package dev.brewkits.krelay.integration

import dev.brewkits.krelay.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Integration tests for queue enqueue behaviour after the refactoring to [enqueueActionUnderLock].
 *
 * Specifically verifies:
 * - FIFO eviction (dispatch without priority) — oldest entry removed on overflow
 * - Priority eviction (dispatchWithPriority) — lowest-priority entry removed on overflow
 * - Priority sort — queued actions replayed highest-priority first
 * - Boundary: queue exactly at maxQueueSize, queue at maxQueueSize + 1
 * - Expiry pruning happens before overflow check
 * - Behaviour is identical on singleton and instance APIs
 */
class EnqueueBehaviorIntegrationTest {

    interface WorkFeature : RelayFeature {
        fun run(label: String)
    }

    class RecordingImpl : WorkFeature {
        val calls = mutableListOf<String>()
        override fun run(label: String) { calls.add(label) }
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("EnqueueBehaviorScope")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    // ── FIFO eviction (dispatch without priority) ─────────────────────────

    @Test
    fun fifoEviction_oldestDroppedOnOverflow() {
        instance.maxQueueSize = 3

        instance.dispatch<WorkFeature> { it.run("first") }
        instance.dispatch<WorkFeature> { it.run("second") }
        instance.dispatch<WorkFeature> { it.run("third") }
        // Queue full — next dispatch should drop "first"
        instance.dispatch<WorkFeature> { it.run("fourth") }

        assertEquals(3, instance.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)

        // "first" was dropped; second/third/fourth replayed in insertion order
        assertEquals(listOf("second", "third", "fourth"), impl.calls)
    }

    @Test
    fun fifoEviction_multipleOverflows_keepsOnlyNewest() {
        instance.maxQueueSize = 2

        (1..10).forEach { i -> instance.dispatch<WorkFeature> { feature -> feature.run("item$i") } }

        assertEquals(2, instance.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)

        // Only the last 2 survive
        assertEquals(listOf("item9", "item10"), impl.calls)
    }

    // ── Priority eviction (dispatchWithPriority) ──────────────────────────

    @Test
    fun priorityEviction_lowestPriorityDroppedOnOverflow() {
        instance.maxQueueSize = 2

        instance.dispatchWithPriority<WorkFeature>(ActionPriority.HIGH) { it.run("high") }
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.LOW) { it.run("low") }
        // Queue full (2/2). Next: CRITICAL — should evict LOW
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.CRITICAL) { it.run("critical") }

        assertEquals(2, instance.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)

        // LOW was evicted; replayed highest priority first
        assertTrue("critical" in impl.calls)
        assertTrue("high" in impl.calls)
        assertFalse("low" in impl.calls)
    }

    @Test
    fun priorityEviction_highestPriorityAlwaysSurvives() {
        instance.maxQueueSize = 1

        instance.dispatchWithPriority<WorkFeature>(ActionPriority.NORMAL) { it.run("normal") }
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.CRITICAL) { it.run("critical") }

        assertEquals(1, instance.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)
        assertEquals(listOf("critical"), impl.calls)
    }

    // ── Priority sort order ───────────────────────────────────────────────

    @Test
    fun prioritySort_replayedInDescendingOrder() {
        // Dispatch low → normal → high → critical in FIFO order
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.LOW) { it.run("L") }
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.NORMAL) { it.run("N") }
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.HIGH) { it.run("H") }
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.CRITICAL) { it.run("C") }

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)

        // Replayed: CRITICAL first, then HIGH, NORMAL, LOW
        assertEquals(listOf("C", "H", "N", "L"), impl.calls)
    }

    @Test
    fun mixedDispatch_normalThenPriority_replayOrderCorrect() {
        // Normal dispatch gets priority=0 (LOW), priority dispatch gets priority=100 (HIGH)
        instance.dispatch<WorkFeature> { it.run("normal-0") }
        instance.dispatchWithPriority<WorkFeature>(ActionPriority.HIGH) { it.run("high-100") }
        instance.dispatch<WorkFeature> { it.run("normal-1") }

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)

        // HIGH should come first; the two normal actions keep their relative insertion order
        assertEquals("high-100", impl.calls.first())
    }

    // ── Expiry pruning before overflow check ──────────────────────────────

    @Test
    fun expiry_prunesBeforeOverflowCheck_allowsNewEntries() = runBlocking {
        instance.maxQueueSize = 2
        // isExpired(0L) = (elapsed > 0) — true after ≥ 1 ms has elapsed
        instance.actionExpiryMs = 0L

        instance.dispatch<WorkFeature> { it.run("old-1") }
        instance.dispatch<WorkFeature> { it.run("old-2") }

        // Wait so old-1 / old-2 are genuinely expired (elapsed > 0 ms)
        delay(5)

        // Queue appears full (2/2), but both entries are now expired.
        // enqueueActionUnderLock prunes them first → "fresh" is added without FIFO eviction.
        instance.dispatch<WorkFeature> { it.run("fresh") }

        // Only "fresh" remains (old items were pruned, not FIFO-evicted)
        assertEquals(1, instance.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)
        assertEquals(listOf("fresh"), impl.calls)
    }

    // ── Boundary: exact capacity ──────────────────────────────────────────

    @Test
    fun exactCapacity_noEvictionUntilExceeded() {
        instance.maxQueueSize = 3

        instance.dispatch<WorkFeature> { it.run("a") }
        instance.dispatch<WorkFeature> { it.run("b") }
        instance.dispatch<WorkFeature> { it.run("c") }

        // Exactly at capacity — no eviction yet
        assertEquals(3, instance.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        instance.register<WorkFeature>(impl)
        assertEquals(listOf("a", "b", "c"), impl.calls)
    }

    // ── Singleton API mirrors instance behaviour ───────────────────────────

    @Test
    fun singleton_fifoEviction_matchesInstanceBehaviour() {
        KRelay.maxQueueSize = 2

        KRelay.dispatch<WorkFeature> { it.run("s1") }
        KRelay.dispatch<WorkFeature> { it.run("s2") }
        KRelay.dispatch<WorkFeature> { it.run("s3") }

        assertEquals(2, KRelay.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        KRelay.register<WorkFeature>(impl)
        assertEquals(listOf("s2", "s3"), impl.calls)
    }

    @Test
    fun singleton_priorityEviction_matchesInstanceBehaviour() {
        KRelay.maxQueueSize = 1

        KRelay.dispatchWithPriority<WorkFeature>(ActionPriority.LOW) { it.run("low") }
        KRelay.dispatchWithPriority<WorkFeature>(ActionPriority.CRITICAL) { it.run("critical") }

        assertEquals(1, KRelay.getPendingCount<WorkFeature>())

        val impl = RecordingImpl()
        KRelay.register<WorkFeature>(impl)
        assertEquals(listOf("critical"), impl.calls)
    }
}

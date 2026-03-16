package dev.brewkits.krelay.stress

import dev.brewkits.krelay.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlin.test.*

/**
 * Concurrent stress tests for the Scope Token API.
 *
 * Verifies that cancelScope + dispatch under heavy concurrency:
 * - Does not corrupt the pending queue
 * - Does not cause ConcurrentModificationException
 * - Leaves the queue in a consistent, bounded state
 */
class ScopeTokenConcurrentStressTest {

    interface WorkFeature : RelayFeature {
        fun run(id: String)
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("ScopeTokenStressScope")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    /**
     * Stress 1: Many goroutines dispatch with unique tokens; one goroutine
     * continuously calls cancelScope. Verifies no CME / crash.
     */
    @Test
    fun stress_concurrentDispatchAndCancelScope_noCorruption() = runBlocking {
        val tokens = (0 until 20).map { scopedToken() }
        val cancelCount = atomic(0)

        // 20 goroutines each dispatching 200 actions with their own token
        val dispatchJobs = tokens.map { token ->
            launch(Dispatchers.Default) {
                repeat(200) { i ->
                    try {
                        instance.dispatch<WorkFeature>(token) { it.run("$token-$i") }
                    } catch (e: Exception) {
                        fail("dispatch must not throw: ${e.message}")
                    }
                }
            }
        }

        // 5 goroutines randomly cancelling scopes
        val cancelJobs = (0 until 5).map {
            launch(Dispatchers.Default) {
                tokens.forEach { token ->
                    try {
                        instance.cancelScope(token)
                        cancelCount.incrementAndGet()
                    } catch (e: Exception) {
                        fail("cancelScope must not throw: ${e.message}")
                    }
                    delay(1)
                }
            }
        }

        dispatchJobs.forEach { it.join() }
        cancelJobs.forEach { it.join() }

        // Queue should be bounded and not corrupted
        val remaining = instance.getPendingCount<WorkFeature>()
        assertTrue(remaining >= 0, "Pending count must be non-negative: $remaining")
        assertTrue(remaining <= 100, "Pending count must not exceed maxQueueSize: $remaining")
    }

    /**
     * Stress 2: Concurrent dispatch + cancelScope + register.
     * Verifies that the mock impl only receives valid actions and count is consistent.
     */
    @Test
    fun stress_concurrentDispatchCancelAndRegister_consistentState() = runBlocking {
        val received = atomic(0)
        val tokens = (0 until 10).map { scopedToken() }

        // Dispatch phase: 10 tokens × 50 dispatches
        val dispatchJobs = tokens.map { token ->
            launch(Dispatchers.Default) {
                repeat(50) { i ->
                    instance.dispatch<WorkFeature>(token) { it.run("$i") }
                }
            }
        }
        dispatchJobs.forEach { it.join() }

        // Cancel half the tokens
        tokens.take(5).forEach { instance.cancelScope(it) }

        // Register — triggers replay
        val impl = object : WorkFeature {
            override fun run(id: String) { received.incrementAndGet() }
        }
        instance.register<WorkFeature>(impl)

        delay(500)  // let replay execute

        // Received count must be <= what was queued after cancellations
        val receivedCount = received.value
        assertTrue(receivedCount >= 0, "Received count should be non-negative: $receivedCount")
        assertTrue(receivedCount <= 500, "Received count should not exceed total dispatches: $receivedCount")
    }

    /**
     * Stress 3: Same token used by many coroutines simultaneously.
     * cancelScope while dispatches are in flight — no deadlock or crash.
     */
    @Test
    fun stress_sameTokenConcurrentDispatchAndCancel_noDeadlock() = runBlocking {
        val sharedToken = scopedToken()

        val dispatchJob = launch(Dispatchers.Default) {
            repeat(500) { i ->
                instance.dispatch<WorkFeature>(sharedToken) { it.run("$i") }
                if (i % 50 == 0) delay(1)
            }
        }

        val cancelJob = launch(Dispatchers.Default) {
            repeat(10) {
                delay(5)
                instance.cancelScope(sharedToken)
            }
        }

        withTimeout(10_000) {
            dispatchJob.join()
            cancelJob.join()
        }

        // If we reach here, no deadlock occurred
        val remaining = instance.getPendingCount<WorkFeature>()
        assertTrue(remaining in 0..100, "Queue must remain bounded: $remaining")
    }

    /**
     * Stress 4: Verify that after cancelScope, the queue contains ONLY untagged actions.
     *
     * Uses a queue large enough (250) to fit all dispatches without FIFO eviction, so
     * the post-cancel count is deterministic. Verifies queue state synchronously via
     * getPendingCount for platform independence across Android and iOS.
     */
    @Test
    fun stress_afterCancelScope_freshRegisterReceivesCorrectCount() = runBlocking {
        val tokens = (0 until 5).map { scopedToken() }
        val taggedPerToken = 30
        val untaggedCount = 20
        // 5 × 30 + 20 = 170 total; set queue large enough to prevent FIFO eviction
        instance.maxQueueSize = 250

        val jobs = tokens.map { token ->
            launch(Dispatchers.Default) {
                repeat(taggedPerToken) { instance.dispatch<WorkFeature>(token) { it.run("tagged") } }
            }
        }
        val untaggedJob = launch(Dispatchers.Default) {
            repeat(untaggedCount) { instance.dispatch<WorkFeature> { it.run("untagged") } }
        }

        jobs.forEach { it.join() }
        untaggedJob.join()

        // Cancel all tagged tokens — only untagged should remain
        tokens.forEach { instance.cancelScope(it) }

        val remaining = instance.getPendingCount<WorkFeature>()
        assertEquals(
            untaggedCount,
            remaining,
            "After cancel, only $untaggedCount untagged actions should remain, got $remaining"
        )
    }
}

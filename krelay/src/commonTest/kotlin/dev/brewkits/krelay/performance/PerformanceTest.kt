package dev.brewkits.krelay.performance

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Coarse performance guards. Budgets are deliberately generous (CI machines, iOS simulator,
 * Kotlin/Wasm) — they catch algorithmic regressions (e.g. O(n²) queueing), not micro-variance.
 */
class PerformanceTest {

    interface CounterFeature : RelayFeature {
        fun hit()
    }

    class Counter : CounterFeature {
        var count = 0
        override fun hit() { count++ }
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelayMetrics.reset()
        KRelayMetrics.enabled = false
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
        KRelay.clearInstanceRegistry()
        KRelayMetrics.reset()
        KRelayMetrics.enabled = false
    }

    private inline fun elapsedMs(block: () -> Unit): Long {
        val start = currentTimeMillis()
        block()
        return currentTimeMillis() - start
    }

    @Test
    fun testImmediateDispatchThroughput() {
        val instance = KRelay.create("PerfDispatch")
        val counter = Counter()
        instance.register<CounterFeature>(counter)

        val ms = elapsedMs { repeat(50_000) { instance.dispatch<CounterFeature> { it.hit() } } }

        assertEquals(50_000, counter.count)
        assertTrue(ms < 15_000, "50k immediate dispatches took ${ms}ms")
        instance.reset()
    }

    @Test
    fun testQueueAndReplayScale() {
        val instance = KRelay.builder("PerfReplay").maxQueueSize(20_000).build()
        val counter = Counter()

        val queueMs = elapsedMs { repeat(20_000) { instance.dispatch<CounterFeature> { it.hit() } } }
        assertEquals(20_000, instance.getPendingCount<CounterFeature>())

        val replayMs = elapsedMs { instance.register<CounterFeature>(counter) }

        assertEquals(20_000, counter.count)
        assertTrue(queueMs < 15_000, "queueing 20k actions took ${queueMs}ms")
        assertTrue(replayMs < 15_000, "replaying 20k actions took ${replayMs}ms")
        instance.reset()
    }

    @Test
    fun testPriorityInsertionScalesWithMixedPriorities() {
        val instance = KRelay.builder("PerfPriority").maxQueueSize(20_000).build()
        val priorities = ActionPriority.values()

        val ms = elapsedMs {
            repeat(20_000) { i ->
                instance.dispatchWithPriority<CounterFeature>(priorities[i % priorities.size]) { it.hit() }
            }
        }

        assertEquals(20_000, instance.getPendingCount<CounterFeature>())
        assertTrue(ms < 15_000, "20k priority inserts took ${ms}ms")
        instance.reset()
    }

    @Test
    fun testFloodAgainstFullQueueStaysBoundedAndFast() {
        val instance = KRelay.builder("PerfFlood").maxQueueSize(100).build()

        val ms = elapsedMs { repeat(100_000) { instance.dispatch<CounterFeature> { it.hit() } } }

        assertEquals(100, instance.getPendingCount<CounterFeature>())
        assertTrue(ms < 20_000, "100k dispatches against a full queue took ${ms}ms")
        instance.reset()
    }

    @Test
    fun testRegisterUnregisterChurnDoesNotAccumulateState() {
        val instance = KRelay.create("PerfChurn")

        val ms = elapsedMs {
            repeat(10_000) {
                val counter = Counter()
                instance.register<CounterFeature>(counter)
                instance.unregister<CounterFeature>()
            }
        }

        assertEquals(0, instance.getPendingCount<CounterFeature>())
        assertFalse(instance.isRegistered<CounterFeature>())
        assertEquals(0, instance.getRegisteredFeaturesCount())
        assertTrue(ms < 15_000, "10k register/unregister cycles took ${ms}ms")
        instance.reset()
    }

    @Test
    fun testManyInstancesCreateAndRemove() {
        val ms = elapsedMs {
            repeat(1_000) { i -> KRelay.create("Perf-$i") }
            repeat(1_000) { i -> KRelay.removeInstance("Perf-$i") }
        }

        assertTrue(ms < 15_000, "1000 instance create/remove took ${ms}ms")
    }

    @Test
    fun testMetricsAndReporterOverheadIsBounded() {
        KRelayMetrics.enabled = true
        var events = 0
        val reporter = KRelayMetricsReporter { events++ }
        KRelayMetrics.addReporter(reporter)
        val instance = KRelay.create("PerfMetrics")
        instance.register<CounterFeature>(Counter())

        val ms = elapsedMs { repeat(50_000) { instance.dispatch<CounterFeature> { it.hit() } } }

        KRelayMetrics.removeReporter(reporter)
        assertEquals(50_000, events)
        assertEquals(50_000L, KRelayMetrics.getDispatchCount(CounterFeature::class))
        assertTrue(ms < 20_000, "50k dispatches with metrics+reporter took ${ms}ms")
        instance.reset()
    }

    @Test
    fun testPersistedCommandSerializationThroughput() {
        val payload = "x".repeat(1_000)
        val ms = elapsedMs {
            repeat(20_000) { i ->
                val restored = PersistedCommand.deserialize(PersistedCommand("action-$i", payload, 1L, 50).serialize())
                assertNotNull(restored)
            }
        }
        assertTrue(ms < 15_000, "20k serialize/deserialize round trips took ${ms}ms")
    }
}

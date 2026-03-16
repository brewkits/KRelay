package dev.brewkits.krelay.demo

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Interactive demo of KRelayMetrics.
 *
 * Run these tests to see how metrics are recorded through the dispatch pipeline.
 * Each scenario shows one aspect of the metrics system.
 */
class MetricsDemo {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigateTo(screen: String)
    }

    class AndroidToast : ToastFeature {
        override fun show(message: String) = println("🍞 $message")
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelayMetrics.reset()
        KRelayMetrics.enabled = true
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("MetricsDemo")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
        KRelayMetrics.reset()
        KRelayMetrics.enabled = false
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo1_immediateDispatch_recordsDispatch() {
        println("\n${"=".repeat(60)}")
        println("DEMO 1: Immediate Dispatch → recordDispatch()")
        println("=".repeat(60))

        val toast = AndroidToast()
        instance.register<ToastFeature>(toast)

        println("\n📤 Dispatching 3 actions to registered ToastFeature...")
        repeat(3) { i ->
            instance.dispatch<ToastFeature> { it.show("Message $i") }
        }

        val dispatches = KRelayMetrics.getDispatchCount(ToastFeature::class)
        println("\n📊 Metrics:")
        println("  - Dispatches (immediate): $dispatches")
        println("  - Queued: ${KRelayMetrics.getQueueCount(ToastFeature::class)}")

        assertEquals(3, dispatches)
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo2_queuedDispatch_recordsQueue() {
        println("\n${"=".repeat(60)}")
        println("DEMO 2: Queued Dispatch → recordQueue()")
        println("=".repeat(60))

        println("\n📤 Dispatching 5 actions (no impl registered — all go to queue)...")
        repeat(5) { i ->
            instance.dispatch<ToastFeature> { it.show("Queued $i") }
        }

        val queued = KRelayMetrics.getQueueCount(ToastFeature::class)
        println("\n📊 Metrics:")
        println("  - Dispatches (immediate): ${KRelayMetrics.getDispatchCount(ToastFeature::class)}")
        println("  - Queued: $queued")
        println("  - In-memory queue size: ${instance.getPendingCount<ToastFeature>()}")

        assertEquals(5, queued)
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo3_replayOnRegister_recordsReplay() {
        println("\n${"=".repeat(60)}")
        println("DEMO 3: Register After Queue → recordReplay()")
        println("=".repeat(60))

        println("\n📤 Queuing 4 actions...")
        repeat(4) { i ->
            instance.dispatch<ToastFeature> { it.show("Pending $i") }
        }

        println("\n📝 Registering implementation → triggers replay...")
        instance.register<ToastFeature>(AndroidToast())

        val replayed = KRelayMetrics.getReplayCount(ToastFeature::class)
        println("\n📊 Metrics:")
        println("  - Queued: ${KRelayMetrics.getQueueCount(ToastFeature::class)}")
        println("  - Replayed: $replayed")

        assertEquals(4, replayed)
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo4_fullPipeline_allMetricsRecorded() {
        println("\n${"=".repeat(60)}")
        println("DEMO 4: Full Pipeline — All Metric Types")
        println("=".repeat(60))

        println("\n📤 Phase 1: Queue 3 Toast actions (no impl)")
        repeat(3) { instance.dispatch<ToastFeature> { it.show("queued $it") } }

        println("\n📝 Phase 2: Register Toast → replays 3")
        instance.register<ToastFeature>(AndroidToast())

        println("\n📤 Phase 3: 2 more immediate dispatches")
        repeat(2) { instance.dispatch<ToastFeature> { it.show("immediate $it") } }

        println("\n📤 Phase 4: 2 queued Nav dispatches (no impl)")
        repeat(2) { instance.dispatch<NavFeature> { it.navigateTo("screen$it") } }

        println("\n📤 Phase 5: Priority dispatch to Toast (immediate)")
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.HIGH) { it.show("prio") }

        println("\n📊 Full Metrics Report:")
        KRelayMetrics.printReport()

        // Assertions
        assertEquals(3L, KRelayMetrics.getQueueCount(ToastFeature::class))
        assertEquals(3L, KRelayMetrics.getReplayCount(ToastFeature::class))
        assertEquals(3L, KRelayMetrics.getDispatchCount(ToastFeature::class)) // 2 immediate + 1 priority
        assertEquals(2L, KRelayMetrics.getQueueCount(NavFeature::class))
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo5_metricsDisabled_nothingRecorded() {
        println("\n${"=".repeat(60)}")
        println("DEMO 5: Metrics Disabled (default) → Zero Overhead")
        println("=".repeat(60))

        KRelayMetrics.enabled = false
        println("\n⚙️  KRelayMetrics.enabled = false (default production setting)")

        instance.dispatch<ToastFeature> { it.show("invisible to metrics") }
        instance.register<ToastFeature>(AndroidToast())
        instance.dispatch<ToastFeature> { it.show("still invisible") }

        val dispatches = KRelayMetrics.getDispatchCount(ToastFeature::class)
        val queued = KRelayMetrics.getQueueCount(ToastFeature::class)
        val replayed = KRelayMetrics.getReplayCount(ToastFeature::class)

        println("\n📊 Metrics (should all be 0 since disabled):")
        println("  - Dispatches: $dispatches")
        println("  - Queued: $queued")
        println("  - Replayed: $replayed")

        assertEquals(0L, dispatches)
        assertEquals(0L, queued)
        assertEquals(0L, replayed)
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo6_metricsEnabled_optIn_collectsData() {
        println("\n${"=".repeat(60)}")
        println("DEMO 6: Enable Metrics via KRelay.metricsEnabled")
        println("=".repeat(60))

        KRelayMetrics.enabled = false
        KRelayMetrics.reset()

        println("\n⚙️  Opt-in: KRelay.metricsEnabled = true")
        KRelay.metricsEnabled = true

        KRelay.dispatch<ToastFeature> { it.show("singleton-queued") }
        KRelay.register<ToastFeature>(AndroidToast())
        KRelay.dispatch<ToastFeature> { it.show("singleton-immediate") }

        println("\n📊 Singleton metrics for ToastFeature:")
        val m = KRelay.getMetrics<ToastFeature>()
        println("  - dispatches: ${m["dispatches"]}")
        println("  - queued: ${m["queued"]}")
        println("  - replayed: ${m["replayed"]}")

        assertEquals(1L, m["queued"])
        assertEquals(1L, m["replayed"])
        assertEquals(1L, m["dispatches"])
    }
}

package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Integration tests verifying that KRelayMetrics is wired into the dispatch pipeline.
 *
 * Covers:
 * - dispatch() immediate: recordDispatch incremented
 * - dispatch() queued:    recordQueue incremented
 * - register() replay:   recordReplay incremented
 * - dispatchWithPriority() wiring (singleton + instance)
 */
class MetricsIntegrationTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    class MockToast : ToastFeature {
        override fun show(message: String) {}
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelayMetrics.reset()
        KRelayMetrics.enabled = true
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("MetricsIntegrationScope")
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

    // ── dispatch immediate ─────────────────────────────────────────────

    @Test
    fun testDispatch_immediate_recordsDispatch() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        instance.dispatch<ToastFeature> { it.show("hello") }

        assertEquals(1, KRelayMetrics.getDispatchCount(ToastFeature::class))
        assertEquals(0, KRelayMetrics.getQueueCount(ToastFeature::class))
    }

    @Test
    fun testDispatch_queued_recordsQueue() {
        // No impl registered → goes to queue
        instance.dispatch<ToastFeature> { it.show("queued") }

        assertEquals(0, KRelayMetrics.getDispatchCount(ToastFeature::class))
        assertEquals(1, KRelayMetrics.getQueueCount(ToastFeature::class))
    }

    // ── register replay ────────────────────────────────────────────────

    @Test
    fun testRegister_replaysQueuedActions_recordsReplay() {
        // Queue 3 actions
        instance.dispatch<ToastFeature> { it.show("1") }
        instance.dispatch<ToastFeature> { it.show("2") }
        instance.dispatch<ToastFeature> { it.show("3") }
        assertEquals(3, KRelayMetrics.getQueueCount(ToastFeature::class))

        // Register → triggers replay
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        assertEquals(3, KRelayMetrics.getReplayCount(ToastFeature::class))
    }

    @Test
    fun testRegister_noQueue_zeroReplay() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        assertEquals(0, KRelayMetrics.getReplayCount(ToastFeature::class))
    }

    // ── dispatchWithPriority ───────────────────────────────────────────

    @Test
    fun testDispatchWithPriority_immediate_recordsDispatch() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        instance.dispatchWithPriority<ToastFeature>(ActionPriority.HIGH) { it.show("prio") }

        assertEquals(1, KRelayMetrics.getDispatchCount(ToastFeature::class))
    }

    @Test
    fun testDispatchWithPriority_queued_recordsQueue() {
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) { it.show("prio") }

        assertEquals(1, KRelayMetrics.getQueueCount(ToastFeature::class))
    }

    // ── singleton API ──────────────────────────────────────────────────

    @Test
    fun testSingleton_dispatch_immediate_recordsDispatch() {
        val mock = MockToast()
        KRelay.register<ToastFeature>(mock)

        KRelay.dispatch<ToastFeature> { it.show("singleton") }

        assertEquals(1, KRelayMetrics.getDispatchCount(ToastFeature::class))
    }

    @Test
    fun testSingleton_dispatch_queued_recordsQueue() {
        KRelay.dispatch<ToastFeature> { it.show("singleton queued") }

        assertEquals(1, KRelayMetrics.getQueueCount(ToastFeature::class))
    }

    // ── metrics disabled ───────────────────────────────────────────────

    @Test
    fun testMetricsDisabled_nothingRecorded() {
        KRelayMetrics.enabled = false

        instance.dispatch<ToastFeature> { it.show("invisible") }

        assertEquals(0, KRelayMetrics.getQueueCount(ToastFeature::class))
    }
}

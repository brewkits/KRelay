package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests for [KRelayMetricsReporter]: event delivery, scope attribution, enable flag, isolation.
 */
class MetricsReporterTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    class MockToast : ToastFeature {
        override fun show(message: String) {}
    }

    private val events = mutableListOf<KRelayMetricEvent>()
    private val reporter = KRelayMetricsReporter { events.add(it) }
    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelayMetrics.reset()
        KRelayMetrics.enabled = true
        KRelayMetrics.addReporter(reporter)
        KRelay.reset()
        instance = KRelay.create("ReporterScope")
    }

    @AfterTest
    fun tearDown() {
        KRelayMetrics.removeReporter(reporter)
        instance.reset()
        KRelay.clearInstanceRegistry()
        KRelayMetrics.reset()
        KRelayMetrics.enabled = false
    }

    @Test
    fun testQueueThenReplay_reportsEventsWithScope() {
        instance.dispatch<ToastFeature> { it.show("a") }
        instance.dispatch<ToastFeature> { it.show("b") }
        instance.register<ToastFeature>(MockToast())

        val queued = events.filter { it.type == KRelayMetricType.QUEUE }
        val replayed = events.single { it.type == KRelayMetricType.REPLAY }
        assertEquals(2, queued.size)
        assertEquals(2, replayed.count)
        assertEquals("ToastFeature", replayed.featureName)
        assertEquals("ReporterScope", replayed.scopeName)
    }

    @Test
    fun testImmediateDispatch_reportsDispatchEvent() {
        instance.register<ToastFeature>(MockToast())
        instance.dispatch<ToastFeature> { it.show("now") }

        val dispatch = events.single { it.type == KRelayMetricType.DISPATCH }
        assertEquals(1, dispatch.count)
        assertEquals("ReporterScope", dispatch.scopeName)
    }

    @Test
    fun testInstanceMetrics_areAttributedToScope() {
        instance.register<ToastFeature>(MockToast())
        instance.dispatch<ToastFeature> { it.show("x") }

        val scoped = KRelayMetrics.getInstanceMetrics("ReporterScope")
        assertEquals(1L, scoped["ToastFeature"]?.get("dispatches"))
    }

    @Test
    fun testClear_reportsClearEvent() {
        instance.dispatch<ToastFeature> { it.show("x") }
        instance.clearQueue(ToastFeature::class)

        assertEquals(1, events.single { it.type == KRelayMetricType.CLEAR }.count)
    }

    @Test
    fun testDisabledMetrics_deliverNoEvents() {
        KRelayMetrics.enabled = false
        instance.dispatch<ToastFeature> { it.show("x") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun testRemovedReporter_stopsReceiving() {
        KRelayMetrics.removeReporter(reporter)
        instance.dispatch<ToastFeature> { it.show("x") }

        assertTrue(events.isEmpty())
    }

    @Test
    fun testAddingSameReporterTwice_deliversOnce() {
        KRelayMetrics.addReporter(reporter)
        instance.dispatch<ToastFeature> { it.show("x") }

        assertEquals(1, events.count { it.type == KRelayMetricType.QUEUE })
    }

    @Test
    fun testThrowingReporter_doesNotBreakDispatch() {
        val bad = KRelayMetricsReporter { error("reporter failure") }
        KRelayMetrics.addReporter(bad)
        try {
            instance.dispatch<ToastFeature> { it.show("x") }
            assertEquals(1, KRelayMetrics.getQueueCount(ToastFeature::class))
            assertEquals(1, events.count { it.type == KRelayMetricType.QUEUE })
        } finally {
            KRelayMetrics.removeReporter(bad)
        }
    }
}

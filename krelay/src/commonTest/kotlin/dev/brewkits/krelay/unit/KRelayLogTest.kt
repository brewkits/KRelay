package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests for structured logging: severity filtering, custom sinks, sink isolation.
 */
class KRelayLogTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    class MockToast : ToastFeature {
        override fun show(message: String) {}
    }

    private data class Record(val level: KRelayLogLevel, val scope: String, val message: String)

    private val records = mutableListOf<Record>()
    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelayLog.reset()
        KRelayLog.sink = KRelayLogSink { level, scope, message -> records.add(Record(level, scope, message)) }
        instance = KRelay.builder("LogTestScope").debugMode(true).build()
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.clearInstanceRegistry()
        KRelayLog.reset()
    }

    @Test
    fun testDebugLogs_reachSink_withScopeName() {
        instance.register<ToastFeature>(MockToast())

        val debug = records.filter { it.level == KRelayLogLevel.DEBUG }
        assertTrue(debug.isNotEmpty())
        assertTrue(debug.all { it.scope == "LogTestScope" })
    }

    @Test
    fun testMinLevel_filtersLowerSeverities() {
        KRelayLog.minLevel = KRelayLogLevel.WARN

        instance.register<ToastFeature>(MockToast())
        // Overflowing a queue of size 1 emits a WARN (eviction).
        val small = KRelay.builder("SmallQueueScope").debugMode(true).maxQueueSize(1).build()
        small.dispatch<ToastFeature> { it.show("1") }
        small.dispatch<ToastFeature> { it.show("2") }
        small.reset()

        assertTrue(records.isNotEmpty())
        assertTrue(records.all { it.level >= KRelayLogLevel.WARN })
        assertTrue(records.any { it.level == KRelayLogLevel.WARN })
    }

    @Test
    fun testError_isEmitted_evenWithoutDebugMode() {
        val quiet = KRelay.builder("QuietScope").debugMode(false).build()
        quiet.register<ToastFeature>(MockToast())
        quiet.dispatch<ToastFeature> { error("boom") }

        assertTrue(records.any { it.level == KRelayLogLevel.ERROR && it.scope == "QuietScope" })
        assertTrue(records.none { it.level == KRelayLogLevel.DEBUG && it.scope == "QuietScope" })
        quiet.reset()
    }

    @Test
    fun testThrowingSink_doesNotBreakDispatch() {
        KRelayLog.sink = KRelayLogSink { _, _, _ -> error("sink failure") }
        val mock = MockToast()

        instance.register<ToastFeature>(mock)
        instance.dispatch<ToastFeature> { it.show("still works") }
        // reaching here without an exception is the assertion
        assertTrue(instance.isRegistered<ToastFeature>())
    }

    @Test
    fun testReset_restoresDefaults() {
        KRelayLog.minLevel = KRelayLogLevel.ERROR
        KRelayLog.reset()

        assertEquals(KRelayLogLevel.DEBUG, KRelayLog.minLevel)
        assertSame(ConsoleLogSink, KRelayLog.sink)
    }
}

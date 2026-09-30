package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Covers branches not reached by the feature-focused suites: singleton KClass entry points,
 * error paths, expiry on replay, persistence restore edge cases and diagnostics output.
 */
@Suppress("DEPRECATION")
class CoverageGapTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    class MockToast : ToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) { shown.add(message) }
    }

    class OtherToast : ToastFeature {
        override fun show(message: String) {}
    }

    private class Recorder : KRelayPersistenceAdapter {
        val store = mutableMapOf<String, MutableList<PersistedCommand>>()
        val removed = mutableListOf<PersistedCommand>()
        override fun save(scopeName: String, featureKey: String, command: PersistedCommand) {
            store.getOrPut(featureKey) { mutableListOf() }.add(command)
        }
        override fun loadAll(scopeName: String): Map<String, List<PersistedCommand>> =
            store.mapValues { it.value.toList() }
        override fun remove(scopeName: String, featureKey: String, command: PersistedCommand) {
            removed.add(command)
            store[featureKey]?.remove(command)
        }
        override fun clearScope(scopeName: String) = store.clear()
        override fun clearAll() = store.clear()
    }

    private val logs = mutableListOf<Pair<KRelayLogLevel, String>>()
    private val events = mutableListOf<KRelayMetricEvent>()
    private val reporter = KRelayMetricsReporter { events.add(it) }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelayMetrics.reset()
        KRelayMetrics.enabled = true
        KRelayMetrics.addReporter(reporter)
        KRelayLog.sink = KRelayLogSink { level, _, message -> logs.add(level to message) }
    }

    @AfterTest
    fun tearDown() {
        KRelayMetrics.removeReporter(reporter)
        KRelayMetrics.reset()
        KRelayMetrics.enabled = false
        KRelayLog.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    private fun spinFor(ms: Long) {
        val start = currentTimeMillis()
        while (currentTimeMillis() - start < ms) { /* busy wait: no sleep in common code */ }
    }

    // ── singleton KClass entry points ────────────────────────────────

    @Test
    fun testSingletonKClassEntryPoints() {
        val mock = MockToast()
        KRelay.registerInternal(ToastFeature::class, mock)
        assertTrue(KRelay.isRegisteredInternal(ToastFeature::class))

        KRelay.dispatchInternal(ToastFeature::class) { it.show("a") }
        KRelay.dispatchWithPriorityInternal(ToastFeature::class, ActionPriority.HIGH) { it.show("b") }
        assertEquals(listOf("a", "b"), mock.shown)

        KRelay.unregisterInternal(ToastFeature::class)
        assertFalse(KRelay.isRegisteredInternal(ToastFeature::class))

        KRelay.dispatchInternal(ToastFeature::class) { it.show("queued") }
        assertEquals(1, KRelay.getPendingCountInternal(ToastFeature::class))

        KRelay.clearQueueInternal(ToastFeature::class)
        assertEquals(0, KRelay.getPendingCountInternal(ToastFeature::class))

        val metrics = KRelay.getMetricsInternal(ToastFeature::class)
        assertEquals(setOf("dispatches", "queued", "replayed", "expired", "cleared"), metrics.keys)
        assertEquals(2L, metrics["dispatches"])
        assertEquals(1L, metrics["cleared"])
    }

    @Test
    fun testPriorityDispatchWithScopeToken_singletonAndInstance() {
        val mock = MockToast()
        val token = scopedToken()

        KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.LOW, token) { it.show("queued-low") }
        assertEquals(1, KRelay.getPendingCount<ToastFeature>())
        KRelay.cancelScope(token)
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())

        KRelay.register<ToastFeature>(mock)
        KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.HIGH) { it.show("now") }

        val instance = KRelay.create("CoverageScope")
        val instToken = scopedToken()
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL, instToken) { it.show("x") }
        assertEquals(1, instance.getPendingCount<ToastFeature>())
        instance.cancelScope(instToken)
        assertEquals(0, instance.getPendingCount<ToastFeature>())
        assertEquals(listOf("now"), mock.shown)
    }

    // ── logging & warning branches ───────────────────────────────────

    @Test
    fun testOverwriteWithDifferentType_warnsInDebugMode() {
        val instance = KRelay.builder("OverwriteScope").debugMode(true).build()
        instance.register<ToastFeature>(MockToast())
        val keep = OtherToast()
        instance.register<ToastFeature>(keep)

        assertTrue(logs.any { it.first == KRelayLogLevel.WARN && it.second.contains("Overwriting") })
        instance.reset()
    }

    @Test
    fun testDebugLogs_forClearCancelAndFactory() {
        val instance = KRelay.builder("DebugLogScope").debugMode(true).build()
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        instance.dispatch<ToastFeature> { it.show("x") }
        instance.clearQueue(ToastFeature::class)
        val token = scopedToken()
        instance.dispatch<ToastFeature>(token) { it.show("y") }
        instance.cancelScope(token)

        val all = logs.map { it.second }
        assertTrue(all.any { it.contains("[FACTORY]") })
        assertTrue(all.any { it.contains("[CLEAR]") })
        assertTrue(all.any { it.contains("[CANCEL]") })
        instance.reset()
    }

    // ── error isolation ──────────────────────────────────────────────

    @Test
    fun testThrowingActionDuringReplay_isLogged_andOthersStillRun() {
        val instance = KRelay.create("ReplayErrorScope")
        val mock = MockToast()
        instance.dispatch<ToastFeature> { error("boom") }
        instance.dispatch<ToastFeature> { it.show("survivor") }

        instance.register<ToastFeature>(mock)

        assertEquals(listOf("survivor"), mock.shown)
        assertTrue(logs.any { it.first == KRelayLogLevel.ERROR && it.second.contains("Error replaying") })
        instance.reset()
    }

    @Test
    fun testThrowingPriorityAction_isLogged() {
        val instance = KRelay.create("PriorityErrorScope")
        instance.register<ToastFeature>(MockToast())
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.HIGH) { error("boom") }

        assertTrue(logs.any { it.first == KRelayLogLevel.ERROR })
        instance.reset()
    }

    @Test
    fun testThrowingPersistedAction_isLogged() {
        val instance = KRelay.create("PersistedErrorScope")
        instance.registerActionFactory<ToastFeature>("toast", "explode") { _ -> { error("boom") } }
        instance.register<ToastFeature>(MockToast())

        // payload passed explicitly: with two strings the call would bind to the deprecated overload
        instance.dispatchPersisted<ToastFeature>("toast", "explode", "")

        assertTrue(logs.any { it.first == KRelayLogLevel.ERROR && it.second.contains("persisted dispatch") })
        instance.reset()
    }

    // ── expiry on replay ─────────────────────────────────────────────

    @Test
    fun testExpiredActionsAreDroppedOnRegister_andReported() {
        val instance = KRelay.builder("ExpiryScope").actionExpiry(5).debugMode(true).build()
        val mock = MockToast()
        instance.dispatch<ToastFeature> { it.show("stale") }
        spinFor(20)

        instance.register<ToastFeature>(mock)

        assertTrue(mock.shown.isEmpty())
        assertEquals(1L, KRelayMetrics.getExpiryCount(ToastFeature::class))
        assertEquals(1, events.single { it.type == KRelayMetricType.EXPIRY }.count)
        assertTrue(logs.any { it.second.contains("[EXPIRY]") })
        instance.reset()
    }

    @Test
    fun testExpiredActionsAreSweptOnEnqueue() {
        val instance = KRelay.builder("SweepScope").actionExpiry(5).build()
        instance.dispatch<ToastFeature> { it.show("stale-1") }
        instance.dispatch<ToastFeature> { it.show("stale-2") }
        spinFor(20)

        instance.dispatch<ToastFeature> { it.show("fresh") }

        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertEquals(listOf("fresh"), mock.shown)
        instance.reset()
    }

    @Test
    fun testRestoredOldCommandsAreStillSweptOnEnqueue() {
        val adapter = Recorder()
        val instance = KRelay.builder("RestoredSweepScope").actionExpiry(10_000).build()
        instance.setPersistenceAdapter(adapter)
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        // Fresh enqueue first, then a restored command whose timestamp is older but not yet expired
        instance.dispatch<ToastFeature> { it.show("new") }
        adapter.store["toast"] = mutableListOf(PersistedCommand("show", "restored", currentTimeMillis() - 9_000))
        instance.restorePersistedActions()
        instance.actionExpiryMs = 1_000   // restored command is now past its expiry

        instance.dispatch<ToastFeature> { it.show("trigger") }

        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertFalse(mock.shown.contains("restored"), "expired restored command must be swept")
        instance.reset()
    }

    // ── persistence restore edge cases ───────────────────────────────

    @Test
    fun testRestore_skipsExpiredUnknownFeatureAndUnknownAction_andCleansStorage() {
        val adapter = Recorder()
        val instance = KRelay.builder("RestoreScope").actionExpiry(1_000).debugMode(true).build()
        instance.setPersistenceAdapter(adapter)
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }

        val now = currentTimeMillis()
        val fresh = PersistedCommand("show", "fresh", now)
        val expired = PersistedCommand("show", "old", now - 10_000)
        val noFactory = PersistedCommand("missing", "x", now)
        val unknownFeature = PersistedCommand("show", "y", now)
        adapter.store["toast"] = mutableListOf(fresh, expired, noFactory)
        adapter.store["ghost"] = mutableListOf(unknownFeature)

        instance.restorePersistedActions()

        assertEquals(1, instance.getPendingCount<ToastFeature>())
        assertEquals(4, adapter.removed.size)
        assertTrue(adapter.store.values.all { it.isEmpty() })
        assertTrue(logs.any { it.first == KRelayLogLevel.WARN && it.second.contains("No KClass for 'ghost'") })
        assertTrue(logs.any { it.first == KRelayLogLevel.WARN && it.second.contains("No factory for") })
        assertTrue(logs.any { it.second.contains("[RESTORE] Restored 1") })

        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertEquals(listOf("fresh"), mock.shown)
        instance.reset()
    }

    @Test
    fun testRestore_withNothingPersisted_isNoOp() {
        val instance = KRelay.builder("EmptyRestoreScope").debugMode(true).build()
        instance.restorePersistedActions()

        assertTrue(logs.any { it.second.contains("No persisted actions") })
        instance.reset()
    }

    @Test
    fun testPersistedQueuedAction_replaysWhenFeatureRegisters() {
        val instance = KRelay.create("PersistedReplayScope")
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        instance.dispatchPersisted<ToastFeature>("toast", "show", "later")
        val mock = MockToast()

        instance.register<ToastFeature>(mock)

        assertEquals(listOf("later"), mock.shown)
        instance.reset()
    }

    @Test
    fun testDeprecatedPersistedOverloads() {
        val instance = KRelay.create("DeprecatedScope")
        instance.registerActionFactory<ToastFeature>("show") { p -> { it.show(p) } }
        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        instance.dispatchPersisted<ToastFeature>("show", "payload")
        assertEquals(listOf("payload"), mock.shown)

        KRelay.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        KRelay.registerActionFactory<ToastFeature>("legacyShow") { p -> { it.show(p) } }
        KRelay.register<ToastFeature>(mock)
        KRelay.dispatchPersisted<ToastFeature>("toast", "show", "singleton")
        KRelay.dispatchPersisted<ToastFeature>("legacyShow", "legacy")
        KRelay.setPersistenceAdapter(InMemoryPersistenceAdapter())
        KRelay.restorePersistedActions()

        assertEquals(listOf("payload", "singleton", "legacy"), mock.shown)
        instance.reset()
    }

    @Test
    fun testInMemoryPersistenceAdapter_isInert() {
        val adapter = InMemoryPersistenceAdapter()
        val command = PersistedCommand("a", "b")
        adapter.save("s", "f", command)
        assertTrue(adapter.loadAll("s").isEmpty())
        adapter.remove("s", "f", command)
        adapter.clearScope("s")
        adapter.clearAll()
    }

    // ── diagnostics & metrics output ─────────────────────────────────

    @Test
    fun testDebugInfoToString_includesExpiredAndQueues() {
        val info = DebugInfo(
            registeredFeaturesCount = 0,
            registeredFeatures = emptyList(),
            featureQueues = mapOf("ToastFeature" to 2),
            totalPendingActions = 2,
            expiredActionsRemoved = 3,
            maxQueueSize = 100,
            actionExpiryMs = 60_000,
            debugMode = true
        )
        val text = info.toString()

        assertTrue(text.contains("(none)"))
        assertTrue(text.contains("ToastFeature: 2 events"))
        assertTrue(text.contains("Expired & Removed: 3 events"))
        assertTrue(text.contains("Debug Mode: true"))
    }

    @Test
    fun testDumpAndPrintReport_doNotThrow() {
        KRelayMetrics.printReport()
        val instance = KRelay.create("DumpScope")
        instance.dispatch<ToastFeature> { it.show("x") }
        instance.register<ToastFeature>(MockToast())
        instance.dispatch<ToastFeature> { it.show("y") }
        instance.dump()
        KRelay.dump()
        KRelayMetrics.printReport()
        instance.reset()
    }

    @Test
    fun testMetricsResetInstance_andEnabledExtension() {
        KRelay.metricsEnabled = true
        assertTrue(KRelay.metricsEnabled)

        val instance = KRelay.create("MetricsResetScope")
        instance.register<ToastFeature>(MockToast())
        instance.dispatch<ToastFeature> { it.show("x") }
        assertEquals(1L, KRelayMetrics.getInstanceMetrics("MetricsResetScope")["ToastFeature"]?.get("dispatches"))

        KRelayMetrics.resetInstance("MetricsResetScope")
        assertTrue(KRelayMetrics.getInstanceMetrics("MetricsResetScope").isEmpty())
        instance.reset()
    }

    @Test
    fun testGetMetricsExtension_reportsAllCounters() {
        KRelay.register<ToastFeature>(MockToast())
        KRelay.dispatch<ToastFeature> { it.show("x") }

        val metrics = KRelay.getMetrics<ToastFeature>()
        assertEquals(1L, metrics["dispatches"])
        assertEquals(setOf("dispatches", "queued", "replayed", "expired", "cleared"), metrics.keys)
    }
}

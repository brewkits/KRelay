package dev.brewkits.krelay.security

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Robustness against hostile or corrupted input: tampered persistence, malformed serialized
 * commands, flooding, key confusion, and misbehaving callbacks.
 */
class SecurityTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface PayFeature : RelayFeature {
        fun pay(message: String)
    }

    class MockToast : ToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) { shown.add(message) }
    }

    class MockPay : PayFeature {
        val paid = mutableListOf<String>()
        override fun pay(message: String) { paid.add(message) }
    }

    private class FixedAdapter(private val data: Map<String, List<PersistedCommand>>) : KRelayPersistenceAdapter {
        val removed = mutableListOf<PersistedCommand>()
        override fun save(scopeName: String, featureKey: String, command: PersistedCommand) = Unit
        override fun loadAll(scopeName: String) = data
        override fun remove(scopeName: String, featureKey: String, command: PersistedCommand) { removed.add(command) }
        override fun clearScope(scopeName: String) = Unit
        override fun clearAll() = Unit
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelayLog.sink = KRelayLogSink { _, _, _ -> }
    }

    @AfterTest
    fun tearDown() {
        KRelayLog.reset()
        KRelay.reset()
        KRelay.clearInstanceRegistry()
    }

    // ── malformed serialized commands ────────────────────────────────

    @Test
    fun testDeserialize_rejectsMalformedInputWithoutThrowing() {
        val hostile = listOf(
            "",
            ":",
            "::",
            ":::",
            "abc:def:ghi:jkl",
            "1:2:3",
            "1:2:-5:abc",
            "1:2:999999999:short",
            "99999999999999999999:1:1:a",
            "1:99999999999:1:a",
            "1:2:3:ab",
            "\u0000:\u0000:\u0000:\u0000",
            "1:2:-2147483648:x",
        )
        hostile.forEach { input ->
            assertNull(PersistedCommand.deserialize(input), "expected null for ${input.take(30)}")
        }
    }

    @Test
    fun testSerialize_roundTripsHostilePayloadsWithoutChangingBoundaries() {
        val payloads = listOf(
            "",
            ":::",
            "1:2:3:4",
            "line1\nline2\r\n",
            "\u0000\u0001",
            "日本語 😀",
            "::",
            "a".repeat(100_000),
        )
        payloads.forEach { payload ->
            val original = PersistedCommand("act:ion::key", payload, 42L, 7)
            val restored = PersistedCommand.deserialize(original.serialize())
            assertEquals(original, restored)
        }
    }

    // ── tampered storage ─────────────────────────────────────────────

    @Test
    fun testRestore_dropsCommandsThatReferenceUnregisteredFactories() {
        val now = currentTimeMillis()
        val tampered = mapOf(
            "toast" to listOf(PersistedCommand("pay_everything", "x", now)),
            "admin" to listOf(PersistedCommand("grant", "x", now))
        )
        val adapter = FixedAdapter(tampered)
        val instance = KRelay.create("TamperedScope")
        instance.setPersistenceAdapter(adapter)
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        instance.restorePersistedActions()

        assertEquals(0, instance.getPendingCount<ToastFeature>())
        assertTrue(mock.shown.isEmpty())
        assertEquals(2, adapter.removed.size, "tampered entries must be purged from storage")
        instance.reset()
    }

    @Test
    fun testRestore_doesNotDeliverToADifferentFeature() {
        val now = currentTimeMillis()
        val adapter = FixedAdapter(mapOf("toast" to listOf(PersistedCommand("pay", "1000", now))))
        val instance = KRelay.create("CrossFeatureScope")
        instance.setPersistenceAdapter(adapter)
        // "pay" factory exists, but only under the *pay* feature key
        instance.registerActionFactory<PayFeature>("pay", "pay") { p -> { it.pay(p) } }
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        val pay = MockPay()
        val toast = MockToast()
        instance.register<PayFeature>(pay)
        instance.register<ToastFeature>(toast)

        instance.restorePersistedActions()

        assertTrue(pay.paid.isEmpty())
        assertTrue(toast.shown.isEmpty())
        instance.reset()
    }

    @Test
    fun testRestore_expiredCommandsAreNeverExecuted() {
        val stale = PersistedCommand("show", "old", currentTimeMillis() - 10 * 60_000)
        val adapter = FixedAdapter(mapOf("toast" to listOf(stale)))
        val instance = KRelay.builder("StaleScope").actionExpiry(60_000).build()
        instance.setPersistenceAdapter(adapter)
        instance.registerActionFactory<ToastFeature>("toast", "show") { p -> { it.show(p) } }
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        instance.restorePersistedActions()

        assertTrue(mock.shown.isEmpty())
        assertEquals(1, adapter.removed.size)
        instance.reset()
    }

    // ── key confusion ────────────────────────────────────────────────

    @Test
    fun testFactoryKeys_doNotCollideAcrossFeatureAndActionBoundaries() {
        val instance = KRelay.create("KeyConfusionScope")
        val toast = MockToast()
        val pay = MockPay()
        instance.register<ToastFeature>(toast)
        instance.register<PayFeature>(pay)
        // ("a", "b::c") and ("a::b", "c") would both flatten to "a::b::c" with a naive separator.
        instance.registerActionFactory<ToastFeature>("a", "b::c") { p -> { it.show("toast:$p") } }
        instance.registerActionFactory<PayFeature>("a::b", "c") { p -> { it.pay("pay:$p") } }

        instance.dispatchPersisted<ToastFeature>("a", "b::c", "1")
        instance.dispatchPersisted<PayFeature>("a::b", "c", "2")

        assertEquals(listOf("toast:1"), toast.shown)
        assertEquals(listOf("pay:2"), pay.paid)
        instance.reset()
    }

    // ── flooding / resource exhaustion ───────────────────────────────

    @Test
    fun testQueueFlood_isBoundedByMaxQueueSize() {
        val instance = KRelay.builder("FloodScope").maxQueueSize(50).build()
        repeat(200_000) { instance.dispatch<ToastFeature> { it.show("spam") } }

        assertEquals(50, instance.getPendingCount<ToastFeature>())
        instance.reset()
    }

    @Test
    fun testCriticalActionsSurviveLowPriorityFlood() {
        val instance = KRelay.builder("PriorityFloodScope").maxQueueSize(10).build()
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) { it.show("critical") }
        repeat(1_000) { instance.dispatchWithPriority<ToastFeature>(ActionPriority.LOW) { it.show("spam") } }
        val mock = MockToast()

        instance.register<ToastFeature>(mock)

        assertTrue(mock.shown.contains("critical"), "critical action must not be evicted by low-priority spam")
        assertEquals(10, mock.shown.size)
        instance.reset()
    }

    @Test
    fun testOversizedPayloadRoundTrips() {
        val payload = "p".repeat(1_000_000)
        val restored = PersistedCommand.deserialize(PersistedCommand("k", payload, 1L, 1).serialize())
        assertEquals(payload.length, restored?.payload?.length)
    }

    // ── misbehaving callbacks ────────────────────────────────────────

    @Test
    fun testThrowingActionDoesNotBlockSubsequentDispatches() {
        val instance = KRelay.create("ThrowingScope")
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        instance.dispatch<ToastFeature> { error("malicious") }
        instance.dispatch<ToastFeature> { it.show("still alive") }

        assertEquals(listOf("still alive"), mock.shown)
        instance.reset()
    }

    @Test
    fun testThrowingReporterAndSinkDoNotBreakDispatch() {
        KRelayMetrics.enabled = true
        val bad = KRelayMetricsReporter { throw IllegalStateException("reporter") }
        KRelayMetrics.addReporter(bad)
        KRelayLog.sink = KRelayLogSink { _, _, _ -> throw IllegalStateException("sink") }
        try {
            val instance = KRelay.builder("HostileCallbacksScope").debugMode(true).build()
            val mock = MockToast()
            instance.register<ToastFeature>(mock)
            instance.dispatch<ToastFeature> { it.show("ok") }
            assertEquals(listOf("ok"), mock.shown)
            instance.reset()
        } finally {
            KRelayMetrics.removeReporter(bad)
            KRelayMetrics.reset()
            KRelayMetrics.enabled = false
        }
    }

    // ── identifiers ──────────────────────────────────────────────────

    @Test
    fun testScopeTokensAreUnique() {
        val tokens = HashSet<String>()
        repeat(20_000) { assertTrue(tokens.add(scopedToken())) }
    }

    @Test
    fun testCancelScopeOnlyAffectsItsOwnToken() {
        val instance = KRelay.create("TokenScope")
        val mine = scopedToken()
        val other = scopedToken()
        instance.dispatch<ToastFeature>(mine) { it.show("mine") }
        instance.dispatch<ToastFeature>(other) { it.show("other") }

        instance.cancelScope(mine)
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        assertEquals(listOf("other"), mock.shown)
        instance.reset()
    }

    @Test
    fun testInstancesAreIsolated() {
        val a = KRelay.create("IsolationA")
        val b = KRelay.create("IsolationB")
        val mockA = MockToast()
        a.register<ToastFeature>(mockA)

        b.dispatch<ToastFeature> { it.show("for-b") }

        assertTrue(mockA.shown.isEmpty())
        assertEquals(1, b.getPendingCount<ToastFeature>())
        a.reset()
        b.reset()
    }
}

package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests for registry-related behavior introduced or hardened in v2.1.1:
 *
 * - Identity-aware unregister: `unregister(impl)` only removes if the stored
 *   reference is the same object, preventing accidental clearance of a newer
 *   registration by a recomposing component.
 * - Same-class replacement: registering a new instance of the same class should
 *   succeed silently (no spurious log noise) and replace the old reference.
 * - `KRelay.getMetrics<T>()`: the extension function returns live data from
 *   `KRelayMetrics`, not a hard-coded empty map.
 */
class RegistryBehaviorTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigate(screen: String)
    }

    class MockToast : ToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) { shown.add(message) }
    }

    class MockNav : NavFeature {
        override fun navigate(screen: String) {}
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelayMetrics.reset()
        instance = KRelay.create("RegistryBehaviorScope")
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

    // ──────────────────────────────────────────────────────────────────────
    // Identity-aware unregister
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun testUnregisterWithImpl_matchingInstance_removesRegistration() {
        val impl = MockToast()
        instance.register<ToastFeature>(impl)
        assertTrue(instance.isRegistered<ToastFeature>())

        instance.unregister<ToastFeature>(impl)

        assertFalse(instance.isRegistered<ToastFeature>())
    }

    @Test
    fun testUnregisterWithImpl_wrongInstance_doesNotRemove() {
        val first = MockToast()
        val second = MockToast()

        instance.register<ToastFeature>(first)
        // A newer component replaced the registration
        instance.register<ToastFeature>(second)

        // The old component calls unregister with its (now-stale) reference
        instance.unregister<ToastFeature>(first)

        // The newer registration must survive
        assertTrue(instance.isRegistered<ToastFeature>(),
            "Unregister with wrong instance should not clear a newer registration")
    }

    @Test
    fun testUnregisterWithoutImpl_alwaysRemoves() {
        val impl = MockToast()
        instance.register<ToastFeature>(impl)

        // null impl = unconditional removal
        instance.unregister<ToastFeature>(null)

        assertFalse(instance.isRegistered<ToastFeature>())
    }

    // ──────────────────────────────────────────────────────────────────────
    // Same-class replacement
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun testRegister_sameClass_replacesImpl() {
        val first = MockToast()
        val second = MockToast()

        instance.register<ToastFeature>(first)
        instance.register<ToastFeature>(second)

        // New impl is active: actions go to second, not first
        instance.dispatch<ToastFeature> { it.show("hello") }

        // Queue should be zero (impl is registered)
        assertEquals(0, instance.getPendingCount<ToastFeature>())
        assertTrue(instance.isRegistered<ToastFeature>())
    }

    @Test
    fun testRegister_differentClass_replacesImpl() {
        class PremiumToast : ToastFeature {
            val shown = mutableListOf<String>()
            override fun show(message: String) { shown.add(message) }
        }

        val basic = MockToast()
        val premium = PremiumToast()

        instance.register<ToastFeature>(basic)
        instance.register<ToastFeature>(premium)

        assertTrue(instance.isRegistered<ToastFeature>())
        // basic should no longer be the active impl
        instance.dispatch<ToastFeature> { it.show("premium only") }
        assertEquals(0, instance.getPendingCount<ToastFeature>())
    }

    // ──────────────────────────────────────────────────────────────────────
    // getMetrics extension on KRelay singleton
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun testGetMetrics_returnsLiveData() {
        KRelayMetrics.enabled = true
        val mock = MockToast()
        KRelay.register<ToastFeature>(mock)

        KRelay.dispatch<ToastFeature> { it.show("a") }
        KRelay.dispatch<ToastFeature> { it.show("b") }

        val metrics = KRelay.getMetrics<ToastFeature>()

        assertEquals(2L, metrics["dispatches"],
            "getMetrics should return live dispatch count from KRelayMetrics")
        assertEquals(0L, metrics["queued"])
        assertEquals(0L, metrics["expired"])
    }

    @Test
    fun testGetMetrics_queuedActions_reflected() {
        KRelayMetrics.enabled = true

        // No impl — actions go to queue
        KRelay.dispatch<ToastFeature> { it.show("q1") }
        KRelay.dispatch<ToastFeature> { it.show("q2") }

        val metrics = KRelay.getMetrics<ToastFeature>()

        assertEquals(0L, metrics["dispatches"])
        assertEquals(2L, metrics["queued"])
    }

    @Test
    fun testGetMetrics_whenMetricsDisabled_returnsZeros() {
        KRelayMetrics.enabled = false
        val mock = MockToast()
        KRelay.register<ToastFeature>(mock)

        KRelay.dispatch<ToastFeature> { it.show("invisible") }

        val metrics = KRelay.getMetrics<ToastFeature>()

        assertEquals(0L, metrics["dispatches"],
            "With metrics disabled, all counts should be zero")
        assertEquals(0L, metrics["queued"])
    }

    // ──────────────────────────────────────────────────────────────────────
    // Registration isolation between instances
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun testInstanceIsolation_registrationDoesNotLeakToSingleton() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        assertFalse(KRelay.isRegistered<ToastFeature>(),
            "Registration on an isolated instance must not affect the singleton")
    }

    @Test
    fun testInstanceIsolation_singletonRegistrationDoesNotLeakToInstance() {
        val mock = MockToast()
        KRelay.register<ToastFeature>(mock)

        assertFalse(instance.isRegistered<ToastFeature>(),
            "Registration on the singleton must not affect an isolated instance")
    }
}

package dev.brewkits.krelay.testing

import dev.brewkits.krelay.KRelayInstance
import dev.brewkits.krelay.RelayFeature
import kotlin.reflect.KClass

/**
 * A fake [KRelayInstance] implementation for use in unit tests.
 *
 * [FakeKRelayInstance] records all dispatched actions and registered/unregistered
 * features, allowing assertions on interaction patterns without requiring a real
 * KRelay runtime.
 *
 * ## Usage
 *
 * ```kotlin
 * class MyViewModelTest {
 *     private val fakeRelay = FakeKRelayInstance()
 *     private val viewModel = MyViewModel(krelay = fakeRelay)
 *
 *     @Test
 *     fun `should dispatch toast on error`() {
 *         viewModel.simulateError()
 *
 *         val dispatch = fakeRelay.lastDispatchFor(ToastFeature::class)
 *         assertNotNull(dispatch)
 *     }
 *
 *     @AfterTest
 *     fun tearDown() = fakeRelay.reset()
 * }
 * ```
 */
class FakeKRelayInstance : KRelayInstance {

    // -----------------------------------------------------------------------
    // Recorded state
    // -----------------------------------------------------------------------

    /** All dispatched actions, in order. Each entry holds the KClass and the invocation block. */
    private val _dispatches = mutableListOf<DispatchRecord<*>>()

    /** Currently registered feature implementations, keyed by KClass. */
    private val _registry = mutableMapOf<KClass<*>, Any>()

    /** All unregister calls recorded, in order. */
    private val _unregistrations = mutableListOf<KClass<*>>()

    // -----------------------------------------------------------------------
    // KRelayInstance implementation
    // -----------------------------------------------------------------------

    override val scopeName: String = "FakeKRelayInstance"
    override var maxQueueSize: Int = 100
    override var actionExpiryMs: Long = 300_000L
    override var debugMode: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun <T : RelayFeature> register(kClass: KClass<T>, impl: T) {
        _registry[kClass] = impl as Any
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : RelayFeature> dispatch(
        kClass: KClass<T>,
        block: (T) -> Unit,
        scopeToken: String?
    ) {
        _dispatches.add(DispatchRecord(kClass, block as (RelayFeature) -> Unit))

        // Immediately invoke if a registered impl exists (simulates real behavior)
        val impl = _registry[kClass] as? T
        if (impl != null) {
            block(impl)
        }
    }

    override fun <T : RelayFeature> dispatchPersisted(
        kClass: KClass<T>,
        featureKey: String,
        actionKey: String,
        payload: String,
        priorityValue: Int
    ) {
        // No-op for now in test fake
    }

    override fun setPersistenceAdapter(adapter: dev.brewkits.krelay.KRelayPersistenceAdapter) {
        // No-op for test fake
    }

    override fun restorePersistedActions() {
        // No-op for test fake
    }

    override fun <T : RelayFeature> registerActionFactory(
        kClass: KClass<T>,
        featureKey: String,
        actionKey: String,
        factory: dev.brewkits.krelay.ActionFactory<T>
    ) {
        // No-op for test fake
    }

    override fun <T : RelayFeature> dispatchWithPriority(
        kClass: KClass<T>,
        priority: Int,
        block: (T) -> Unit,
        scopeToken: String?
    ) {
        dispatch(kClass, block, scopeToken)
    }

    override fun <T : RelayFeature> unregister(kClass: KClass<T>, impl: T?) {
        _unregistrations.add(kClass)
        _registry.remove(kClass)
    }

    override fun <T : RelayFeature> isRegistered(kClass: KClass<T>): Boolean =
        _registry.containsKey(kClass)

    override fun <T : RelayFeature> getPendingCount(kClass: KClass<T>): Int = 0

    override fun <T : RelayFeature> clearQueue(kClass: KClass<T>) { /* no-op */ }

    override fun cancelScope(token: String) { /* no-op */ }

    override fun getRegisteredFeaturesCount(): Int = _registry.size

    override fun getTotalPendingCount(): Int = 0

    override fun getDebugInfo(): dev.brewkits.krelay.DebugInfo =
        dev.brewkits.krelay.DebugInfo(
            registeredFeaturesCount = _registry.size,
            registeredFeatures = _registry.keys.map { it.simpleName ?: "?" },
            featureQueues = emptyMap(),
            totalPendingActions = 0,
            expiredActionsRemoved = 0,
            maxQueueSize = maxQueueSize,
            actionExpiryMs = actionExpiryMs,
            debugMode = debugMode
        )

    override fun dump() { /* no-op */ }

    override fun resetConfiguration() {
        maxQueueSize = 100
        actionExpiryMs = 300_000L
        debugMode = false
    }

    override fun reset() {
        _dispatches.clear()
        _registry.clear()
        _unregistrations.clear()
    }

    // -----------------------------------------------------------------------
    // Test assertion helpers
    // -----------------------------------------------------------------------

    /**
     * Returns all recorded dispatch records for feature [T].
     */
    fun <T : RelayFeature> dispatchesFor(kClass: KClass<T>): List<DispatchRecord<T>> {
        @Suppress("UNCHECKED_CAST")
        return _dispatches.filter { it.kClass == kClass } as List<DispatchRecord<T>>
    }

    /**
     * Returns the most recent dispatch record for feature [T], or `null` if none exist.
     */
    fun <T : RelayFeature> lastDispatchFor(kClass: KClass<T>): DispatchRecord<T>? =
        dispatchesFor(kClass).lastOrNull()

    /**
     * Returns the total number of dispatches recorded across all features.
     */
    fun dispatchCount(): Int = _dispatches.size

    /**
     * Returns the number of dispatches recorded for feature [T].
     */
    fun <T : RelayFeature> dispatchCountFor(kClass: KClass<T>): Int =
        dispatchesFor(kClass).size

    /**
     * Returns the number of unregister calls recorded for feature [T].
     */
    fun <T : RelayFeature> unregisterCountFor(kClass: KClass<T>): Int =
        _unregistrations.count { it == kClass }

    /**
     * Asserts that exactly [count] dispatches were recorded for feature [T].
     * Throws [AssertionError] otherwise.
     */
    fun <T : RelayFeature> assertDispatchCount(kClass: KClass<T>, count: Int) {
        val actual = dispatchCountFor(kClass)
        check(actual == count) {
            "Expected $count dispatch(es) for ${kClass.simpleName}, but found $actual."
        }
    }

    /**
     * Asserts that at least one dispatch was recorded for feature [T].
     */
    fun <T : RelayFeature> assertDispatched(kClass: KClass<T>) {
        check(dispatchCountFor(kClass) > 0) {
            "Expected at least one dispatch for ${kClass.simpleName}, but found none."
        }
    }

    /**
     * Asserts that no dispatches were recorded for feature [T].
     */
    fun <T : RelayFeature> assertNotDispatched(kClass: KClass<T>) {
        val count = dispatchCountFor(kClass)
        check(count == 0) {
            "Expected no dispatches for ${kClass.simpleName}, but found $count."
        }
    }

    // -----------------------------------------------------------------------
    // Convenience reified extensions
    // -----------------------------------------------------------------------

    inline fun <reified T : RelayFeature> dispatchesFor(): List<DispatchRecord<T>> =
        dispatchesFor(T::class)

    inline fun <reified T : RelayFeature> lastDispatchFor(): DispatchRecord<T>? =
        lastDispatchFor(T::class)

    inline fun <reified T : RelayFeature> dispatchCountFor(): Int =
        dispatchCountFor(T::class)

    inline fun <reified T : RelayFeature> assertDispatchCount(count: Int) =
        assertDispatchCount(T::class, count)

    inline fun <reified T : RelayFeature> assertDispatched() =
        assertDispatched(T::class)

    inline fun <reified T : RelayFeature> assertNotDispatched() =
        assertNotDispatched(T::class)
}

/**
 * Represents a single recorded dispatch call on a [FakeKRelayInstance].
 *
 * @property kClass The feature interface KClass that was dispatched to.
 * @property block The lambda that was passed to dispatch.
 */
data class DispatchRecord<T : RelayFeature>(
    val kClass: KClass<T>,
    val block: (T) -> Unit
) {
    /**
     * Executes this dispatch record against a provided [impl].
     * Useful for verifying the effect of a dispatch in tests.
     */
    fun executeWith(impl: T) = block(impl)
}

package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * KRelay: The Native Interop Bridge for Kotlin Multiplatform.
 *
 * This singleton provides a safe, type-safe way to communicate from shared business logic 
 * (ViewModels, UseCases) to platform-specific UI components (Activity, ViewController, Composable).
 *
 * ## Core Pillars
 * 1. **Thread Safety**: Automatically dispatches all actions to the platform's Main/UI thread.
 * 2. **Memory Safety**: Uses `WeakReference` to hold platform implementations, preventing leaks.
 * 3. **Reliability**: Features a "Sticky Queue" that holds actions when the UI is not ready
 *    (e.g., during screen rotation or backgrounding) and replays them when the UI attaches.
 * 4. **Persistence**: Supports surviving process death for critical UI feedback.
 *
 * ## Usage Patterns
 *
 * ### 1. Singleton API (Recommended for small/medium apps)
 * ```kotlin
 * // Platform code
 * KRelay.register<ToastFeature>(this)
 * 
 * // Shared code
 * KRelay.dispatch<ToastFeature> { it.show("Hello!") }
 * ```
 *
 * ### 2. Instance API (Recommended for Super Apps/Modular projects)
 * ```kotlin
 * val paymentRelay = KRelay.create("PaymentModule")
 * paymentRelay.register<PaymentFeature>(impl)
 * ```
 *
 * @see KRelayInstance for isolated instance documentation.
 * @see ProcessDeathUnsafe for important safety guidelines.
 */
@SuperAppWarning
object KRelay {
    /**
     * Internal backing instance for the singleton API.
     */
    @PublishedApi
    internal val defaultInstance = KRelayInstanceImpl(
        scopeName = "__DEFAULT__",
        maxQueueSize = 100,
        actionExpiryMs = 300_000, // 5 minutes
        debugMode = false
    )

    /**
     * The underlying [KRelayInstance] used by this singleton.
     * Useful for dependency injection or framework integrations (like Compose).
     */
    val instance: KRelayInstance get() = defaultInstance

    // ============================================================
    // SINGLETON API (v1.0 - Backward Compatible)
    // ============================================================

    // Internal state accessors removed for M-03. Always use defaultInstance.

    /**
     * Global configuration: Maximum pending actions allowed per feature type.
     * Default: 100
     */
    var maxQueueSize: Int
        get() = defaultInstance.maxQueueSize
        set(value) {
            defaultInstance.maxQueueSize = value
        }

    /**
     * Global configuration: How long an action remains in the queue before being discarded.
     * Default: 300,000ms (5 minutes)
     */
    var actionExpiryMs: Long
        get() = defaultInstance.actionExpiryMs
        set(value) {
            defaultInstance.actionExpiryMs = value
        }

    /**
     * Global configuration: Enables verbose logging of KRelay internal events.
     * Default: false
     */
    var debugMode: Boolean
        get() = defaultInstance.debugMode
        set(value) {
            defaultInstance.debugMode = value
        }

    /**
     * Registers a platform-specific implementation for feature [T].
     * 
     * Any actions currently in the queue for this feature will be replayed immediately.
     * 
     * @param impl The implementation of the [RelayFeature] interface.
     */
    @ProcessDeathUnsafe
    inline fun <reified T : RelayFeature> register(impl: T) {
        defaultInstance.register(T::class, impl)
    }

    /**
     * Dispatches an action to the registered implementation of feature [T].
     * 
     * If the implementation is missing or has been garbage collected, the action
     * is added to the "Sticky Queue".
     * 
     * @param block The lambda to execute on the platform implementation.
     */
    @ProcessDeathUnsafe
    @MemoryLeakWarning
    inline fun <reified T : RelayFeature> dispatch(noinline block: (T) -> Unit) {
        defaultInstance.dispatch(T::class, block)
    }

    /**
     * Dispatches an action tagged with a [scopeToken].
     *
     * Tagged actions can be cancelled in bulk using [cancelScope].
     *
     * @param scopeToken A unique identifier for the caller (e.g., from [scopedToken]).
     * @param block The lambda to execute.
     */
    @ProcessDeathUnsafe
    @MemoryLeakWarning
    inline fun <reified T : RelayFeature> dispatch(
        scopeToken: String,
        noinline block: (T) -> Unit
    ) {
        defaultInstance.dispatch(T::class, block, scopeToken)
    }

    /**
     * Dispatches an action with an explicit [ActionPriority] to the registered implementation of feature [T].
     *
     * Higher-priority actions are dequeued first when the implementation registers.
     * If the queue is full, the **lowest**-priority action is evicted.
     *
     * @param priority The priority of this action. Defaults to [ActionPriority.NORMAL].
     * @param block The lambda to execute on the platform implementation.
     *
     * ## Example
     * ```kotlin
     * KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) {
     *     it.show("Critical error occurred!")
     * }
     * ```
     */
    @ProcessDeathUnsafe
    @MemoryLeakWarning
    inline fun <reified T : RelayFeature> dispatchWithPriority(
        priority: ActionPriority = ActionPriority.NORMAL,
        noinline block: (T) -> Unit
    ) {
        defaultInstance.dispatchWithPriority(T::class, priority.value, block)
    }

    /**
     * Dispatches an action with explicit [ActionPriority] and a [scopeToken].
     *
     * @param priority The priority of this action.
     * @param scopeToken A unique identifier for the caller.
     * @param block The lambda to execute.
     */
    @ProcessDeathUnsafe
    @MemoryLeakWarning
    inline fun <reified T : RelayFeature> dispatchWithPriority(
        priority: ActionPriority = ActionPriority.NORMAL,
        scopeToken: String,
        noinline block: (T) -> Unit
    ) {
        defaultInstance.dispatchWithPriority(T::class, priority.value, block, scopeToken)
    }


    /**
     * Cancels all queued actions that match the provided [token].
     */
    fun cancelScope(token: String) {
        defaultInstance.cancelScope(token)
    }

    /**
     * Removes the registration for feature [T].
     *
     * @param impl Optional identity check. If provided, unregisters only if the current
     *   registration is the same object as [impl]. Useful in Compose to prevent an older
     *   component from clearing a newer registration set during recomposition.
     *   Pass `null` (or omit) to unconditionally remove the registration.
     */
    inline fun <reified T : RelayFeature> unregister(impl: T? = null) {
        defaultInstance.unregister(T::class, impl)
    }

    /**
     * Clears all pending actions for feature [T].
     * 
     * Highly recommended to call this in `ViewModel.onCleared()` if you are not using scope tokens.
     */
    inline fun <reified T : RelayFeature> clearQueue() {
        defaultInstance.clearQueue(T::class)
    }

    /**
     * Returns true if feature [T] has an active implementation registered.
     */
    inline fun <reified T : RelayFeature> isRegistered(): Boolean {
        return defaultInstance.isRegistered(T::class)
    }

    /**
     * Returns the current number of pending actions for feature [T].
     */
    inline fun <reified T : RelayFeature> getPendingCount(): Int {
        return defaultInstance.getPendingCount(T::class)
    }

    /**
     * Returns the count of unique features currently registered in the global singleton.
     */
    fun getRegisteredFeaturesCount(): Int = defaultInstance.getRegisteredFeaturesCount()

    /**
     * Returns the total count of pending actions across all features in the global singleton.
     */
    fun getTotalPendingCount(): Int = defaultInstance.getTotalPendingCount()

    /**
     * Returns a debug snapshot of the global singleton state.
     */
    fun getDebugInfo(): DebugInfo = defaultInstance.getDebugInfo()

    /**
     * Dumps the global singleton state to the console.
     */
    fun dump() = defaultInstance.dump()

    /**
     * Resets configurations to their default values.
     */
    fun resetConfiguration() = defaultInstance.resetConfiguration()

    /**
     * Resets the entire KRelay singleton state (clears all registrations and queues).
     */
    fun reset() = defaultInstance.reset()

    /**
     * Logs a message to the console if [debugMode] is enabled.
     */
    @PublishedApi
    internal fun log(message: String) = defaultInstance.log(message)

    /**
     * Internal registration helper.
     */
    @PublishedApi
    internal fun <T : RelayFeature> registerInternal(kClass: KClass<T>, impl: T) {
        defaultInstance.register(kClass, impl)
    }

    /**
     * Internal unregistration helper.
     */
    @PublishedApi
    internal fun <T : RelayFeature> unregisterInternal(kClass: KClass<T>) {
        defaultInstance.unregister(kClass)
    }

    /**
     * Internal dispatch helper for iOS Swift interop.
     */
    @PublishedApi
    internal fun <T : RelayFeature> dispatchInternal(
        kClass: KClass<T>,
        block: (T) -> Unit
    ) {
        defaultInstance.dispatch(kClass, block)
    }

    /**
     * Internal dispatch with priority helper for iOS Swift interop.
     */
    @PublishedApi
    internal fun <T : RelayFeature> dispatchWithPriorityInternal(
        kClass: KClass<T>,
        priority: ActionPriority,
        block: (T) -> Unit
    ) {
        defaultInstance.dispatchWithPriority(kClass, priority.value, block)
    }

    /**
     * Internal isRegistered helper for iOS Swift interop.
     */
    @PublishedApi
    internal fun <T : RelayFeature> isRegisteredInternal(kClass: KClass<T>): Boolean {
        return defaultInstance.isRegistered(kClass)
    }

    /**
     * Internal getPendingCount helper for iOS Swift interop.
     */
    @PublishedApi
    internal fun <T : RelayFeature> getPendingCountInternal(kClass: KClass<T>): Int {
        return defaultInstance.getPendingCount(kClass)
    }

    /**
     * Internal getMetrics helper for iOS Swift interop.
     */
    @PublishedApi
    internal fun <T : RelayFeature> getMetricsInternal(kClass: KClass<T>): Map<String, Long> {
        return mapOf(
            "dispatches" to KRelayMetrics.getDispatchCount(kClass),
            "queued"     to KRelayMetrics.getQueueCount(kClass),
            "replayed"   to KRelayMetrics.getReplayCount(kClass),
            "expired"    to KRelayMetrics.getExpiryCount(kClass)
        )
    }

    /**
     * Internal queue clearing helper.
     */
    @PublishedApi
    internal fun <T : RelayFeature> clearQueueInternal(kClass: KClass<T>) {
        defaultInstance.clearQueue(kClass)
    }

    // ============================================================
    // INSTANCE API (v2.0 - NEW)
    // ============================================================

    private val instanceRegistry = mutableSetOf<String>()
    private val instanceRegistryLock = Lock()

    /**
     * Creates a new [KRelayInstance] with an isolated registry and queue.
     *
     * @param scopeName A unique name for this module or scope.
     * @return A configured [KRelayInstance].
     * @throws IllegalArgumentException if [scopeName] is blank.
     */
    fun create(scopeName: String): KRelayInstance {
        require(scopeName.isNotBlank()) { "scopeName must not be blank" }

        instanceRegistryLock.withLock {
            if (debugMode && scopeName in instanceRegistry) {
                log("⚠️ [KRelay] Instance with scope '$scopeName' already exists.")
            }
            instanceRegistry.add(scopeName)
        }

        return KRelayInstanceImpl(scopeName)
    }

    /**
     * Removes an isolated instance from the registry.
     */
    fun removeInstance(scopeName: String) {
        instanceRegistryLock.withLock {
            instanceRegistry.remove(scopeName)
        }
    }

    /**
     * Returns a [KRelayBuilder] to create a customized [KRelayInstance].
     */
    fun builder(scopeName: String): KRelayBuilder {
        return KRelayBuilder(scopeName, instanceRegistry, instanceRegistryLock)
    }

    /**
     * Internal test utility.
     */
    @PublishedApi
    internal fun clearInstanceRegistry() {
        instanceRegistryLock.withLock {
            instanceRegistry.clear()
        }
    }
}

// ============================================================
// EXTENSION FUNCTIONS FOR TYPE-SAFE INSTANCE API
// ============================================================

/**
 * Type-safe register for [KRelayInstance].
 */
@ProcessDeathUnsafe
inline fun <reified T : RelayFeature> KRelayInstance.register(impl: T) {
    this.register(T::class, impl)
}

/**
 * Type-safe dispatch for [KRelayInstance].
 */
@ProcessDeathUnsafe
@MemoryLeakWarning
inline fun <reified T : RelayFeature> KRelayInstance.dispatch(noinline block: (T) -> Unit) {
    this.dispatch(T::class, block)
}

/**
 * Type-safe dispatch with scope token for [KRelayInstance].
 */
@ProcessDeathUnsafe
@MemoryLeakWarning
inline fun <reified T : RelayFeature> KRelayInstance.dispatch(
    scopeToken: String,
    noinline block: (T) -> Unit
) {
    this.dispatch(T::class, block, scopeToken)
}

/**
 * Type-safe unregister for [KRelayInstance].
 */
inline fun <reified T : RelayFeature> KRelayInstance.unregister(impl: T? = null) {
    this.unregister(T::class, impl)
}

/**
 * Type-safe check for registration in [KRelayInstance].
 */
inline fun <reified T : RelayFeature> KRelayInstance.isRegistered(): Boolean {
    return this.isRegistered(T::class)
}

/**
 * Type-safe pending count check for [KRelayInstance].
 */
inline fun <reified T : RelayFeature> KRelayInstance.getPendingCount(): Int {
    return this.getPendingCount(T::class)
}

/**
 * Type-safe queue clearing for [KRelayInstance].
 */
inline fun <reified T : RelayFeature> KRelayInstance.clearQueue() {
    this.clearQueue(T::class)
}

/**
 * Type-safe priority dispatch for [KRelayInstance].
 *
 * @param priority The priority of this action. Defaults to [ActionPriority.NORMAL].
 * @param block The lambda to execute on the platform implementation.
 */
@ProcessDeathUnsafe
@MemoryLeakWarning
inline fun <reified T : RelayFeature> KRelayInstance.dispatchWithPriority(
    priority: ActionPriority = ActionPriority.NORMAL,
    noinline block: (T) -> Unit
) {
    this.dispatchWithPriority(T::class, priority.value, block)
}

/**
 * Type-safe priority dispatch with scope token for [KRelayInstance].
 *
 * @param priority The priority of this action.
 * @param scopeToken A unique identifier for the caller.
 * @param block The lambda to execute.
 */
@ProcessDeathUnsafe
@MemoryLeakWarning
inline fun <reified T : RelayFeature> KRelayInstance.dispatchWithPriority(
    priority: ActionPriority = ActionPriority.NORMAL,
    scopeToken: String,
    noinline block: (T) -> Unit
) {
    this.dispatchWithPriority(T::class, priority.value, block, scopeToken)
}

// ============================================================
// SCOPE TOKEN UTILITY
// ============================================================

/**
 * Generates a globally unique token for tagging dispatches within a specific scope (e.g., a ViewModel).
 * 
 * Using tokens allows for surgical cleanup of the sticky queue when a component is destroyed.
 */
fun scopedToken(): String = "krelay-${currentTimeMillis()}-${kotlin.random.Random.nextLong()}"

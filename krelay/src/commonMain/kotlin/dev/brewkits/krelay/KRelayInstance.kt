package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * KRelay instance for modularized apps (Super Apps, multi-team projects).
 *
 * An instance provides an isolated environment for managing feature registrations
 * and pending queues. This is essential for large-scale applications where
 * different modules or teams must operate independently without interfering
 * with each other's UI bridges.
 *
 * ## Key Benefits of Instances
 * - **Isolation**: Registrations in one instance are not visible to others.
 * - **Modular Configuration**: Different modules can have different queue sizes or expiry times.
 * - **Lifecycle Control**: Individual instances can be reset or cleared independently.
 * - **Testability**: Instances are easy to inject and mock in unit tests.
 *
 * ## Standard Usage (via extension functions)
 * ```kotlin
 * val instance = KRelay.create("PaymentModule")
 * 
 * // 1. Register implementation (typically in Activity/ViewController)
 * instance.register<ToastFeature>(this)
 * 
 * // 2. Dispatch from shared code (typically in ViewModel)
 * instance.dispatch<ToastFeature> { it.show("Processing...") }
 * ```
 */
@ProcessDeathUnsafe
interface KRelayInstance {
    /**
     * Unique name for this instance (used for debugging and persistence scoping).
     */
    val scopeName: String

    /**
     * Maximum number of pending actions allowed per feature type in this instance.
     * When the limit is reached, the oldest (or lowest priority) action is evicted.
     * Default: 100
     */
    var maxQueueSize: Int

    /**
     * How long (in milliseconds) a pending action remains valid in the queue.
     * Expired actions are automatically removed during the next dispatch or registry check.
     * Default: 300,000ms (5 minutes)
     */
    var actionExpiryMs: Long

    /**
     * When enabled, KRelay prints detailed lifecycle logs to the console.
     * Useful for troubleshooting registration timing and queue behavior.
     * Default: false
     */
    var debugMode: Boolean

    /**
     * Registers a platform implementation for a specific [RelayFeature].
     * 
     * If there are any non-expired actions in the queue for this feature,
     * they will be replayed immediately on the Main Thread.
     * 
     * @param kClass The class of the feature interface.
     * @param impl The platform-specific implementation.
     */
    fun <T : RelayFeature> register(kClass: KClass<T>, impl: T)

    /**
     * Dispatches an action to the registered platform implementation.
     * 
     * If the implementation is registered and alive, the action executes immediately on the Main Thread.
     * If no implementation is available, the action is added to a "sticky" queue for later replay.
     * 
     * @param kClass The class of the feature interface.
     * @param block The lambda to execute on the implementation.
     * @param scopeToken Optional identifier to group dispatches for bulk cancellation.
     */
    fun <T : RelayFeature> dispatch(kClass: KClass<T>, block: (T) -> Unit, scopeToken: String? = null)

    /**
     * Dispatches an action with a specific priority level.
     * Higher priority actions are replayed before lower priority ones.
     */
    fun <T : RelayFeature> dispatchWithPriority(
        kClass: KClass<T>,
        priorityValue: Int,
        block: (T) -> Unit
    )

    /**
     * Removes a registration for a feature.
     * 
     * @param kClass The feature type to unregister.
     * @param impl Optional identity check. If provided, unregisters only if current registration matches [impl].
     */
    fun <T : RelayFeature> unregister(kClass: KClass<T>, impl: T? = null)

    /**
     * Returns true if a valid, non-collected implementation is currently registered for [kClass].
     */
    fun <T : RelayFeature> isRegistered(kClass: KClass<T>): Boolean

    /**
     * Returns the number of non-expired actions currently waiting in the queue for [kClass].
     */
    fun <T : RelayFeature> getPendingCount(kClass: KClass<T>): Int

    /**
     * Clears all pending actions for a specific feature type.
     */
    fun <T : RelayFeature> clearQueue(kClass: KClass<T>)

    /**
     * Attaches a persistence engine to this instance.
     * 
     * Once set, [dispatchPersisted] calls will survive application process death.
     */
    fun setPersistenceAdapter(adapter: KRelayPersistenceAdapter)

    /**
     * Registers a factory to reconstruct a lambda action from a serializable payload string.
     * 
     * **ProGuard Note**: Use a stable [featureKey] string (e.g., "auth") instead of relying 
     * on class names to avoid issues when code is obfuscated.
     * 
     * @param kClass Feature interface class.
     * @param featureKey A unique, stable identifier for this feature (survives obfuscation).
     * @param actionKey A unique identifier for the specific action within the feature.
     * @param factory The factory that creates the action block from a string payload.
     */
    fun <T : RelayFeature> registerActionFactory(
        kClass: KClass<T>,
        featureKey: String,
        actionKey: String,
        factory: ActionFactory<T>
    )

    /**
     * Dispatches an action that is saved to persistent storage if no implementation is available.
     * 
     * This method requires a previously registered [ActionFactory] to work after app restarts.
     * 
     * @param featureKey Must match the [featureKey] used in [registerActionFactory].
     * @param actionKey The identifier for the action to reconstruct.
     * @param payload Serializable string data for the action.
     * @param priorityValue Priority for queue management (higher value = higher priority).
     */
    fun <T : RelayFeature> dispatchPersisted(
        kClass: KClass<T>,
        featureKey: String,
        actionKey: String,
        payload: String,
        priorityValue: Int
    )

    /**
     * Loads saved actions from the [KRelayPersistenceAdapter] and adds them to the in-memory queue.
     * 
     * Should be called during app initialization after factories are registered but 
     * before implementations are registered.
     */
    fun restorePersistedActions()

    /**
     * Returns the total number of unique features currently registered.
     */
    fun getRegisteredFeaturesCount(): Int

    /**
     * Returns the sum of all pending actions across all feature types.
     */
    fun getTotalPendingCount(): Int

    /**
     * Returns a snapshot of the current state of this instance for debugging.
     */
    fun getDebugInfo(): DebugInfo

    /**
     * Prints a human-readable summary of the instance state to the console.
     */
    fun dump()

    /**
     * Cancels all queued actions that were dispatched with the specified [token].
     * 
     * This is highly recommended for cleanup in `ViewModel.onCleared()` to prevent
     * potential memory leaks from captured variables in lambdas.
     */
    fun cancelScope(token: String)

    /**
     * Resets configuration (maxQueueSize, expiry, etc.) to default values.
     * Does not affect the registry or existing queues.
     */
    fun resetConfiguration()

    /**
     * Completely wipes this instance: clears all registrations, queues, factories, and persistence.
     */
    fun reset()
}

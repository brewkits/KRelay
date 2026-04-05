package dev.brewkits.krelay

import kotlin.reflect.KClass

// ============================================================
// PERSISTED DISPATCH — Extension API for KRelayInstance
// ============================================================

/**
 * Registers a factory to reconstruct a named action from its persisted [payload].
 *
 * @param featureKey A stable, unique string identifying the feature type. This avoids obfuscation bugs.
 * @param actionKey The key used in [dispatchPersisted]. Should be a simple identifier.
 * @param factory A function that takes the payload string and returns the action lambda.
 */
inline fun <reified T : RelayFeature> KRelayInstance.registerActionFactory(
    featureKey: String,
    actionKey: String,
    noinline factory: ActionFactory<T>
) {
    this.registerActionFactory(T::class, featureKey, actionKey, factory)
}

/**
 * Backward compatibility overload for [registerActionFactory].
 * 
 * ⚠️ **WARNING**: Using this method is risky on Android with ProGuard/R8 enabled, 
 * as the feature key defaults to the class simple name which may be obfuscated.
 */
@Deprecated(
    message = "Use the version with an explicit featureKey to avoid ProGuard obfuscation issues.",
    replaceWith = ReplaceWith("registerActionFactory(\"FIXED_KEY\", actionKey, factory)")
)
inline fun <reified T : RelayFeature> KRelayInstance.registerActionFactory(
    actionKey: String,
    noinline factory: ActionFactory<T>
) {
    val stableKey = T::class.simpleName ?: "Unknown"
    this.registerActionFactory(T::class, stableKey, actionKey, factory)
}

/**
 * Dispatches a named, persistable action that can survive process death.
 */
inline fun <reified T : RelayFeature> KRelayInstance.dispatchPersisted(
    featureKey: String,
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    this.dispatchPersisted(T::class, featureKey, actionKey, payload, priority.value)
}

/**
 * Backward compatibility overload for [dispatchPersisted].
 */
@Deprecated(
    message = "Use the version with an explicit featureKey to avoid ProGuard obfuscation issues.",
    replaceWith = ReplaceWith("dispatchPersisted(\"FIXED_KEY\", actionKey, payload, priority)")
)
inline fun <reified T : RelayFeature> KRelayInstance.dispatchPersisted(
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    val stableKey = T::class.simpleName ?: "Unknown"
    this.dispatchPersisted(T::class, stableKey, actionKey, payload, priority.value)
}

// ============================================================
// SINGLETON WRAPPERS
// ============================================================

/**
 * Sets a [KRelayPersistenceAdapter] on the default singleton instance.
 */
fun KRelay.setPersistenceAdapter(adapter: KRelayPersistenceAdapter) {
    defaultInstance.setPersistenceAdapter(adapter)
}

/**
 * Registers an action factory on the default singleton instance.
 */
inline fun <reified T : RelayFeature> KRelay.registerActionFactory(
    featureKey: String,
    actionKey: String,
    noinline factory: ActionFactory<T>
) {
    defaultInstance.registerActionFactory(T::class, featureKey, actionKey, factory)
}

/**
 * Backward compatibility overload for KRelay.registerActionFactory.
 */
@Deprecated(
    message = "Use the version with an explicit featureKey.",
    replaceWith = ReplaceWith("KRelay.registerActionFactory(\"FIXED_KEY\", actionKey, factory)")
)
inline fun <reified T : RelayFeature> KRelay.registerActionFactory(
    actionKey: String,
    noinline factory: ActionFactory<T>
) {
    val stableKey = T::class.simpleName ?: "Unknown"
    defaultInstance.registerActionFactory(T::class, stableKey, actionKey, factory)
}

/**
 * Dispatches a persisted action on the default singleton instance.
 */
inline fun <reified T : RelayFeature> KRelay.dispatchPersisted(
    featureKey: String,
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    defaultInstance.dispatchPersisted(T::class, featureKey, actionKey, payload, priority.value)
}

/**
 * Backward compatibility overload for KRelay.dispatchPersisted.
 */
@Deprecated(
    message = "Use the version with an explicit featureKey.",
    replaceWith = ReplaceWith("KRelay.dispatchPersisted(\"FIXED_KEY\", actionKey, payload, priority)")
)
inline fun <reified T : RelayFeature> KRelay.dispatchPersisted(
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    val stableKey = T::class.simpleName ?: "Unknown"
    defaultInstance.dispatchPersisted(T::class, stableKey, actionKey, payload, priority.value)
}

/**
 * Restores persisted actions on the default singleton instance.
 */
fun KRelay.restorePersistedActions() {
    defaultInstance.restorePersistedActions()
}

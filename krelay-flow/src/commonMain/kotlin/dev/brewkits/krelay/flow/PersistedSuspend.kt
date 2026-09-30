package dev.brewkits.krelay.flow

import dev.brewkits.krelay.ActionPriority
import dev.brewkits.krelay.KRelayInstance
import dev.brewkits.krelay.RelayFeature
import dev.brewkits.krelay.dispatchPersisted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Suspend variant of [dispatchPersisted] for use from coroutine scopes.
 *
 * The factory lookup, queueing and — when no implementation is registered yet — the persistence
 * write (`SharedPreferences` / `NSUserDefaults` / custom adapter) run on [Dispatchers.Default],
 * so the calling coroutine (typically on `Dispatchers.Main`) is never blocked by storage I/O.
 * The action itself is still delivered on the main thread, exactly as with [dispatchPersisted].
 *
 * Throws `IllegalStateException` if no factory is registered for [featureKey]/[actionKey],
 * like [dispatchPersisted].
 *
 * ```kotlin
 * viewModelScope.launch {
 *     relay.dispatchPersistedSuspend<ToastFeature>("toast", "show", payload = "Saved")
 * }
 * ```
 */
suspend inline fun <reified T : RelayFeature> KRelayInstance.dispatchPersistedSuspend(
    featureKey: String,
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    withContext(Dispatchers.Default) {
        dispatchPersisted<T>(featureKey, actionKey, payload, priority)
    }
}

package dev.brewkits.krelay

/**
 * Describes a pending action that can be persisted across process death.
 *
 * Unlike [QueuedAction] (which holds a lambda), this is a serializable record
 * containing an [actionKey] and [payload] string. A registered [ActionFactory]
 * reconstructs the actual action lambda when needed.
 */
data class PersistedCommand(
    val actionKey: String,
    val payload: String = "",
    val timestampMs: Long = currentTimeMillis(),
    val priority: Int = ActionPriority.DEFAULT.value
) {
    /**
     * Whether this command has expired based on the given expiry duration.
     */
    fun isExpired(expiryMs: Long): Boolean = (currentTimeMillis() - timestampMs) > expiryMs

    /**
     * Serialize to a compact string format.
     * Format: `${timestampMs}:${priority}:${actionKeyLength}:${actionKey}${payload}`
     * This encoding is unambiguous for any actionKey/payload content.
     */
    fun serialize(): String = "$timestampMs:$priority:${actionKey.length}:$actionKey$payload"

    companion object {
        /**
         * Deserialize from the format produced by [serialize].
         * Returns null if the string is malformed.
         */
        fun deserialize(s: String): PersistedCommand? {
            return try {
                val firstColon = s.indexOf(':')
                val secondColon = s.indexOf(':', firstColon + 1)
                val thirdColon = s.indexOf(':', secondColon + 1)
                if (firstColon < 0 || secondColon < 0 || thirdColon < 0) return null

                val timestampMs = s.substring(0, firstColon).toLong()
                val priority = s.substring(firstColon + 1, secondColon).toInt()
                val actionKeyLength = s.substring(secondColon + 1, thirdColon).toInt()
                val rest = s.substring(thirdColon + 1)
                if (rest.length < actionKeyLength) return null

                PersistedCommand(
                    actionKey = rest.substring(0, actionKeyLength),
                    payload = rest.substring(actionKeyLength),
                    timestampMs = timestampMs,
                    priority = priority
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Storage adapter for persisting pending KRelay actions across process death.
 *
 * Implement this with your preferred storage backend:
 * - **Android**: `SharedPreferencesPersistenceAdapter(context)` (built-in) or DataStore
 * - **iOS**: `NSUserDefaultsPersistenceAdapter()` (built-in) or FileManager
 *
 * KRelay ships with two ready-to-use implementations:
 * - [InMemoryPersistenceAdapter] — default, no actual persistence (current behavior)
 * - Android: `SharedPreferencesPersistenceAdapter` in `androidMain`
 * - iOS: `NSUserDefaultsPersistenceAdapter` in `iosMain`
 *
 * ## Setup
 * ```kotlin
 * // Android
 * instance.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(context))
 *
 * // iOS
 * instance.setPersistenceAdapter(NSUserDefaultsPersistenceAdapter())
 * ```
 *
 * ## Key invariant
 * Persistence is cleared when [KRelayInstance.restorePersistedActions] is called.
 * If the app dies again after restoration but before replay, those actions are lost.
 * For guaranteed delivery, use WorkManager (Android) or BackgroundTasks (iOS).
 */
interface KRelayPersistenceAdapter {
    /**
     * Persist a pending command. Called when [KRelayInstance.dispatchPersisted] queues
     * an action (no impl registered).
     */
    fun save(scopeName: String, featureKey: String, command: PersistedCommand)

    /**
     * Load all persisted commands for a scope on app restart.
     * @return Map of featureKey → list of commands.
     */
    fun loadAll(scopeName: String): Map<String, List<PersistedCommand>>

    /**
     * Remove a specific command. Called after it has been moved back to in-memory queue.
     */
    fun remove(scopeName: String, featureKey: String, command: PersistedCommand)

    /**
     * Remove all commands for a scope (e.g. when the module is torn down).
     */
    fun clearScope(scopeName: String)

    /**
     * Remove all persisted commands across all scopes.
     */
    fun clearAll()
}

/**
 * Default in-memory adapter — no actual persistence.
 * This preserves the original KRelay behavior (queue lost on process death).
 *
 * This is the default when no adapter is explicitly set.
 */
class InMemoryPersistenceAdapter : KRelayPersistenceAdapter {
    override fun save(scopeName: String, featureKey: String, command: PersistedCommand) = Unit
    override fun loadAll(scopeName: String): Map<String, List<PersistedCommand>> = emptyMap()
    override fun remove(scopeName: String, featureKey: String, command: PersistedCommand) = Unit
    override fun clearScope(scopeName: String) = Unit
    override fun clearAll() = Unit
}

/**
 * Factory function type: given a [payload] string, produce the action lambda.
 *
 * Example:
 * ```kotlin
 * val factory: ActionFactory<ToastFeature> = { payload ->
 *     { feature -> feature.show(payload) }
 * }
 * ```
 */
typealias ActionFactory<T> = (payload: String) -> (T) -> Unit

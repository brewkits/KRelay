package dev.brewkits.krelay

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of [KRelayPersistenceAdapter] using [SharedPreferences].
 *
 * Stores pending actions in a dedicated SharedPreferences file (`krelay_persistence`).
 * Each scope gets its own key, and each entry is serialized to a compact string.
 *
 * ## Setup
 *
 * In your Application or DI setup:
 * ```kotlin
 * // Singleton
 * KRelay.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(applicationContext))
 *
 * // Per-instance
 * val relayInstance = KRelay.create("CheckoutModule")
 * relayInstance.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(applicationContext))
 * ```
 *
 * ## Startup restoration (e.g. in ViewModel or Application.onCreate)
 * ```kotlin
 * // 1. Register factories
 * relayInstance.registerActionFactory<ToastFeature>("show_toast") { payload ->
 *     { feature -> feature.show(payload) }
 * }
 *
 * // 2. Restore persisted actions into in-memory queue
 * relayInstance.restorePersistedActions()
 *
 * // 3. Register implementations — queued actions will replay
 * relayInstance.register<ToastFeature>(toastImpl)
 * ```
 *
 * @param context Application context (use `applicationContext` to avoid Activity leaks).
 */
class SharedPreferencesPersistenceAdapter(
    context: Context
) : KRelayPersistenceAdapter {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun save(scopeName: String, featureKey: String, command: PersistedCommand) {
        val key = scopeKey(scopeName)
        val existing = prefs.getStringSet(key, emptySet())!!.toMutableSet()
        existing.add(encodeEntry(featureKey, command))
        prefs.edit().putStringSet(key, existing).apply()
    }

    override fun loadAll(scopeName: String): Map<String, List<PersistedCommand>> {
        val key = scopeKey(scopeName)
        val entries = prefs.getStringSet(key, emptySet()) ?: return emptyMap()
        val result = mutableMapOf<String, MutableList<PersistedCommand>>()

        for (entry in entries) {
            val decoded = decodeEntry(entry) ?: continue
            result.getOrPut(decoded.first) { mutableListOf() }.add(decoded.second)
        }
        return result
    }

    override fun remove(scopeName: String, featureKey: String, command: PersistedCommand) {
        val key = scopeKey(scopeName)
        val existing = prefs.getStringSet(key, emptySet())!!.toMutableSet()
        existing.remove(encodeEntry(featureKey, command))
        prefs.edit().putStringSet(key, existing).apply()
    }

    override fun clearScope(scopeName: String) {
        prefs.edit().remove(scopeKey(scopeName)).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    // Entry format: "${featureKey.length}:${featureKey}${command.serialize()}"
    private fun encodeEntry(featureKey: String, command: PersistedCommand): String =
        "${featureKey.length}:$featureKey${command.serialize()}"

    private fun decodeEntry(entry: String): Pair<String, PersistedCommand>? {
        return try {
            val colonIdx = entry.indexOf(':')
            if (colonIdx < 0) return null
            val featureKeyLen = entry.substring(0, colonIdx).toInt()
            val rest = entry.substring(colonIdx + 1)
            if (rest.length < featureKeyLen) return null
            val featureKey = rest.substring(0, featureKeyLen)
            val commandStr = rest.substring(featureKeyLen)
            val command = PersistedCommand.deserialize(commandStr) ?: return null
            featureKey to command
        } catch (e: Exception) {
            null
        }
    }

    private fun scopeKey(scopeName: String) = "krelay_$scopeName"

    companion object {
        private const val PREFS_NAME = "krelay_persistence"
    }
}

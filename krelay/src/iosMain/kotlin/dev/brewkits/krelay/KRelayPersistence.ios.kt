package dev.brewkits.krelay

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of [KRelayPersistenceAdapter] using [NSUserDefaults].
 *
 * Stores pending actions in the standard user defaults under prefixed keys.
 * Each scope gets its own key, and each entry is serialized to a compact string.
 *
 * ## Setup
 *
 * In your app initialization or DI setup:
 * ```swift
 * // Swift — set on Kotlin relay instance
 * relayInstance.setPersistenceAdapter(adapter: NSUserDefaultsPersistenceAdapter())
 * ```
 *
 * Or in shared Kotlin code (e.g. ViewModel):
 * ```kotlin
 * relayInstance.setPersistenceAdapter(NSUserDefaultsPersistenceAdapter())
 * ```
 *
 * ## Startup restoration
 * ```kotlin
 * // 1. Register factories
 * relayInstance.registerActionFactory<ToastFeature>("show_toast") { payload ->
 *     { feature -> feature.show(payload) }
 * }
 *
 * // 2. Restore from NSUserDefaults into in-memory queue
 * relayInstance.restorePersistedActions()
 *
 * // 3. Register implementations — queued actions will replay
 * relayInstance.register<ToastFeature>(toastImpl)
 * ```
 *
 * **Note**: NSUserDefaults is suitable for small amounts of data (e.g. a handful of
 * pending UI commands). For large payloads, consider a custom [KRelayPersistenceAdapter]
 * backed by file storage.
 */
class NSUserDefaultsPersistenceAdapter : KRelayPersistenceAdapter {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun save(scopeName: String, featureKey: String, command: PersistedCommand) {
        val key = scopeKey(scopeName)
        val existing = loadRawEntries(key).toMutableList()
        existing.add(encodeEntry(featureKey, command))
        defaults.setObject(existing, key)
    }

    override fun loadAll(scopeName: String): Map<String, List<PersistedCommand>> {
        val key = scopeKey(scopeName)
        val entries = loadRawEntries(key)
        val result = mutableMapOf<String, MutableList<PersistedCommand>>()

        for (entry in entries) {
            val decoded = decodeEntry(entry) ?: continue
            result.getOrPut(decoded.first) { mutableListOf() }.add(decoded.second)
        }
        return result
    }

    override fun remove(scopeName: String, featureKey: String, command: PersistedCommand) {
        val key = scopeKey(scopeName)
        val existing = loadRawEntries(key).toMutableList()
        existing.remove(encodeEntry(featureKey, command))
        defaults.setObject(existing, key)
    }

    override fun clearScope(scopeName: String) {
        defaults.removeObjectForKey(scopeKey(scopeName))
    }

    override fun clearAll() {
        // Only remove KRelay keys — don't wipe unrelated user defaults
        val allKeys = defaults.dictionaryRepresentation().keys
            .filterIsInstance<String>()
            .filter { it.startsWith(KEY_PREFIX) }
        allKeys.forEach { defaults.removeObjectForKey(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadRawEntries(key: String): List<String> {
        return (defaults.arrayForKey(key) as? List<String>) ?: emptyList()
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

    private fun scopeKey(scopeName: String) = "${KEY_PREFIX}$scopeName"

    companion object {
        private const val KEY_PREFIX = "krelay_"
    }
}

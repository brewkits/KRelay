package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * Composable key combining scope name and feature KClass for per-instance metrics.
 */
private data class ScopedMetricKey(val scopeName: String, val kClass: KClass<*>)

/**
 * Performance metrics and monitoring for KRelay.
 *
 * Tracks:
 * - Dispatch counts per feature (globally and per-instance)
 * - Queue statistics
 * - Replay performance
 * - Expiry events
 * - Manual clear events (separate from expiry)
 *
 * Thread-safe: all read/write operations are protected by an internal lock.
 *
 * ## Per-Instance Metrics (v2.1)
 * In Super App scenarios with multiple `KRelayInstance` objects, metrics are tracked
 * both globally (keyed by `KClass`) and per-instance (keyed by `scopeName + KClass`).
 * Use [getInstanceMetrics] to retrieve metrics for a specific instance scope.
 */
object KRelayMetrics {
    private val metricsLock = Lock()

    // Global counters (backward compatible)
    private val dispatchCounts = mutableMapOf<KClass<*>, Long>()
    private val queueCounts = mutableMapOf<KClass<*>, Long>()
    private val replayCounts = mutableMapOf<KClass<*>, Long>()
    private val expiryCounts = mutableMapOf<KClass<*>, Long>()
    private val clearCounts = mutableMapOf<KClass<*>, Long>()

    // Per-instance counters
    private val scopedDispatchCounts = mutableMapOf<ScopedMetricKey, Long>()
    private val scopedQueueCounts = mutableMapOf<ScopedMetricKey, Long>()
    private val scopedReplayCounts = mutableMapOf<ScopedMetricKey, Long>()
    private val scopedExpiryCounts = mutableMapOf<ScopedMetricKey, Long>()
    private val scopedClearCounts = mutableMapOf<ScopedMetricKey, Long>()

    /**
     * Whether metrics collection is enabled.
     * Default: false (opt-in to avoid overhead in production).
     *
     * Enable via [KRelay.metricsEnabled] or directly:
     * ```kotlin
     * KRelayMetrics.enabled = true
     * ```
     */
    var enabled: Boolean = false

    /**
     * Records a dispatch event. No-op if [enabled] is false.
     */
    internal fun recordDispatch(kClass: KClass<*>, scopeName: String = "") {
        if (!enabled) return
        metricsLock.withLock {
            dispatchCounts[kClass] = (dispatchCounts[kClass] ?: 0) + 1
            if (scopeName.isNotEmpty()) {
                val key = ScopedMetricKey(scopeName, kClass)
                scopedDispatchCounts[key] = (scopedDispatchCounts[key] ?: 0) + 1
            }
        }
    }

    /**
     * Records a queue event. No-op if [enabled] is false.
     */
    internal fun recordQueue(kClass: KClass<*>, scopeName: String = "") {
        if (!enabled) return
        metricsLock.withLock {
            queueCounts[kClass] = (queueCounts[kClass] ?: 0) + 1
            if (scopeName.isNotEmpty()) {
                val key = ScopedMetricKey(scopeName, kClass)
                scopedQueueCounts[key] = (scopedQueueCounts[key] ?: 0) + 1
            }
        }
    }

    /**
     * Records a replay event. No-op if [enabled] is false.
     */
    internal fun recordReplay(kClass: KClass<*>, count: Int, scopeName: String = "") {
        if (!enabled) return
        metricsLock.withLock {
            replayCounts[kClass] = (replayCounts[kClass] ?: 0) + count
            if (scopeName.isNotEmpty()) {
                val key = ScopedMetricKey(scopeName, kClass)
                scopedReplayCounts[key] = (scopedReplayCounts[key] ?: 0) + count
            }
        }
    }

    /**
     * Records an expiry event. No-op if [enabled] is false.
     */
    internal fun recordExpiry(kClass: KClass<*>, count: Int, scopeName: String = "") {
        if (!enabled) return
        metricsLock.withLock {
            expiryCounts[kClass] = (expiryCounts[kClass] ?: 0) + count
            if (scopeName.isNotEmpty()) {
                val key = ScopedMetricKey(scopeName, kClass)
                scopedExpiryCounts[key] = (scopedExpiryCounts[key] ?: 0) + count
            }
        }
    }

    /**
     * Records a manual clear event (distinct from expiry). No-op if [enabled] is false.
     */
    internal fun recordClear(kClass: KClass<*>, count: Int, scopeName: String = "") {
        if (!enabled) return
        metricsLock.withLock {
            clearCounts[kClass] = (clearCounts[kClass] ?: 0) + count
            if (scopeName.isNotEmpty()) {
                val key = ScopedMetricKey(scopeName, kClass)
                scopedClearCounts[key] = (scopedClearCounts[key] ?: 0) + count
            }
        }
    }

    /**
     * Gets total dispatch count for a feature (global, across all instances).
     */
    fun getDispatchCount(kClass: KClass<*>): Long = metricsLock.withLock { dispatchCounts[kClass] ?: 0 }

    /**
     * Gets total queue count for a feature (global, across all instances).
     */
    fun getQueueCount(kClass: KClass<*>): Long = metricsLock.withLock { queueCounts[kClass] ?: 0 }

    /**
     * Gets total replay count for a feature (global, across all instances).
     */
    fun getReplayCount(kClass: KClass<*>): Long = metricsLock.withLock { replayCounts[kClass] ?: 0 }

    /**
     * Gets total expiry count for a feature (global, across all instances).
     */
    fun getExpiryCount(kClass: KClass<*>): Long = metricsLock.withLock { expiryCounts[kClass] ?: 0 }

    /**
     * Gets total manual clear count for a feature (global, across all instances).
     */
    fun getClearCount(kClass: KClass<*>): Long = metricsLock.withLock { clearCounts[kClass] ?: 0 }

    /**
     * Gets all metrics as a summary map (global, across all instances).
     */
    fun getAllMetrics(): Map<String, Map<String, Long>> {
        return metricsLock.withLock {
            val allKeys = (dispatchCounts.keys + queueCounts.keys + replayCounts.keys +
                    expiryCounts.keys + clearCounts.keys).distinct()
            allKeys.associate { kClass ->
                kClass.simpleName.orEmpty() to mapOf(
                    "dispatches" to (dispatchCounts[kClass] ?: 0),
                    "queued" to (queueCounts[kClass] ?: 0),
                    "replayed" to (replayCounts[kClass] ?: 0),
                    "expired" to (expiryCounts[kClass] ?: 0),
                    "cleared" to (clearCounts[kClass] ?: 0)
                )
            }
        }
    }

    /**
     * Gets metrics for a specific instance scope.
     *
     * ```kotlin
     * val ridesMetrics = KRelayMetrics.getInstanceMetrics("Rides")
     * // Returns: { "ToastFeature" -> { "dispatches" -> 5, ... } }
     * ```
     */
    fun getInstanceMetrics(scopeName: String): Map<String, Map<String, Long>> {
        return metricsLock.withLock {
            val allScopedKeys = (scopedDispatchCounts.keys + scopedQueueCounts.keys +
                    scopedReplayCounts.keys + scopedExpiryCounts.keys + scopedClearCounts.keys)
                .filter { it.scopeName == scopeName }
                .map { it.kClass }
                .distinct()

            allScopedKeys.associate { kClass ->
                val key = ScopedMetricKey(scopeName, kClass)
                kClass.simpleName.orEmpty() to mapOf(
                    "dispatches" to (scopedDispatchCounts[key] ?: 0),
                    "queued" to (scopedQueueCounts[key] ?: 0),
                    "replayed" to (scopedReplayCounts[key] ?: 0),
                    "expired" to (scopedExpiryCounts[key] ?: 0),
                    "cleared" to (scopedClearCounts[key] ?: 0)
                )
            }
        }
    }

    /**
     * Prints a formatted metrics report.
     */
    fun printReport() {
        println("\n" + "=".repeat(60))
        println("KRelay Metrics Report")
        println("=".repeat(60))

        val metrics = getAllMetrics()
        if (metrics.isEmpty()) {
            println("No metrics recorded yet.")
        } else {
            metrics.forEach { (feature, stats) ->
                println("\n$feature:")
                println("  Dispatches: ${stats["dispatches"]}")
                println("  Queued:     ${stats["queued"]}")
                println("  Replayed:   ${stats["replayed"]}")
                println("  Expired:    ${stats["expired"]}")
                println("  Cleared:    ${stats["cleared"]}")
            }
        }

        println("=".repeat(60) + "\n")
    }

    /**
     * Resets all metrics (global and per-instance).
     */
    fun reset() {
        metricsLock.withLock {
            dispatchCounts.clear()
            queueCounts.clear()
            replayCounts.clear()
            expiryCounts.clear()
            clearCounts.clear()
            scopedDispatchCounts.clear()
            scopedQueueCounts.clear()
            scopedReplayCounts.clear()
            scopedExpiryCounts.clear()
            scopedClearCounts.clear()
        }
    }

    /**
     * Resets metrics for a specific instance scope only.
     */
    fun resetInstance(scopeName: String) {
        metricsLock.withLock {
            scopedDispatchCounts.keys.removeAll { it.scopeName == scopeName }
            scopedQueueCounts.keys.removeAll { it.scopeName == scopeName }
            scopedReplayCounts.keys.removeAll { it.scopeName == scopeName }
            scopedExpiryCounts.keys.removeAll { it.scopeName == scopeName }
            scopedClearCounts.keys.removeAll { it.scopeName == scopeName }
        }
    }
}

/**
 * Extension to enable/disable metrics tracking on KRelay singleton.
 *
 * ```kotlin
 * KRelay.metricsEnabled = true
 * // ... use the app ...
 * KRelayMetrics.printReport()
 * ```
 */
var KRelay.metricsEnabled: Boolean
    get() = KRelayMetrics.enabled
    set(value) {
        KRelayMetrics.enabled = value
    }

/**
 * Extension to get metrics for a specific feature from the singleton.
 */
inline fun <reified T : RelayFeature> KRelay.getMetrics(): Map<String, Long> {
    return mapOf(
        "dispatches" to KRelayMetrics.getDispatchCount(T::class),
        "queued" to KRelayMetrics.getQueueCount(T::class),
        "replayed" to KRelayMetrics.getReplayCount(T::class),
        "expired" to KRelayMetrics.getExpiryCount(T::class),
        "cleared" to KRelayMetrics.getClearCount(T::class)
    )
}


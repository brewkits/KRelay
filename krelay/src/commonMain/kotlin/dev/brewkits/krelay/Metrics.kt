package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * Performance metrics and monitoring for KRelay.
 *
 * Tracks:
 * - Dispatch counts per feature
 * - Queue statistics
 * - Replay performance
 * - Expiry events
 *
 * Thread-safe: all read/write operations are protected by an internal lock.
 */
object KRelayMetrics {
    private val metricsLock = Lock()
    private val dispatchCounts = mutableMapOf<KClass<*>, Long>()
    private val queueCounts = mutableMapOf<KClass<*>, Long>()
    private val replayCounts = mutableMapOf<KClass<*>, Long>()
    private val expiryCounts = mutableMapOf<KClass<*>, Long>()

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
    internal fun recordDispatch(kClass: KClass<*>) {
        if (!enabled) return
        metricsLock.withLock {
            dispatchCounts[kClass] = (dispatchCounts[kClass] ?: 0) + 1
        }
    }

    /**
     * Records a queue event. No-op if [enabled] is false.
     */
    internal fun recordQueue(kClass: KClass<*>) {
        if (!enabled) return
        metricsLock.withLock {
            queueCounts[kClass] = (queueCounts[kClass] ?: 0) + 1
        }
    }

    /**
     * Records a replay event. No-op if [enabled] is false.
     */
    internal fun recordReplay(kClass: KClass<*>, count: Int) {
        if (!enabled) return
        metricsLock.withLock {
            replayCounts[kClass] = (replayCounts[kClass] ?: 0) + count
        }
    }

    /**
     * Records an expiry event. No-op if [enabled] is false.
     */
    internal fun recordExpiry(kClass: KClass<*>, count: Int) {
        if (!enabled) return
        metricsLock.withLock {
            expiryCounts[kClass] = (expiryCounts[kClass] ?: 0) + count
        }
    }

    /**
     * Gets total dispatch count for a feature.
     */
    fun getDispatchCount(kClass: KClass<*>): Long = metricsLock.withLock { dispatchCounts[kClass] ?: 0 }

    /**
     * Gets total queue count for a feature.
     */
    fun getQueueCount(kClass: KClass<*>): Long = metricsLock.withLock { queueCounts[kClass] ?: 0 }

    /**
     * Gets total replay count for a feature.
     */
    fun getReplayCount(kClass: KClass<*>): Long = metricsLock.withLock { replayCounts[kClass] ?: 0 }

    /**
     * Gets total expiry count for a feature.
     */
    fun getExpiryCount(kClass: KClass<*>): Long = metricsLock.withLock { expiryCounts[kClass] ?: 0 }

    /**
     * Gets all metrics as a summary map.
     */
    fun getAllMetrics(): Map<String, Map<String, Long>> {
        return metricsLock.withLock {
            val allKeys = (dispatchCounts.keys + queueCounts.keys + replayCounts.keys + expiryCounts.keys).distinct()
            allKeys.associate { kClass ->
                kClass.simpleName.orEmpty() to mapOf(
                    "dispatches" to (dispatchCounts[kClass] ?: 0),
                    "queued" to (queueCounts[kClass] ?: 0),
                    "replayed" to (replayCounts[kClass] ?: 0),
                    "expired" to (expiryCounts[kClass] ?: 0)
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
            }
        }

        println("=".repeat(60) + "\n")
    }

    /**
     * Resets all metrics.
     */
    fun reset() {
        metricsLock.withLock {
            dispatchCounts.clear()
            queueCounts.clear()
            replayCounts.clear()
            expiryCounts.clear()
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
        "expired" to KRelayMetrics.getExpiryCount(T::class)
    )
}

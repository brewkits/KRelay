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
 */
object KRelayMetrics {
    private val dispatchCounts = mutableMapOf<KClass<*>, Long>()
    private val queueCounts = mutableMapOf<KClass<*>, Long>()
    private val replayCounts = mutableMapOf<KClass<*>, Long>()
    private val expiryCounts = mutableMapOf<KClass<*>, Long>()

    /**
     * Records a dispatch event.
     */
    internal fun recordDispatch(kClass: KClass<*>) {
        dispatchCounts[kClass] = (dispatchCounts[kClass] ?: 0) + 1
    }

    /**
     * Records a queue event.
     */
    internal fun recordQueue(kClass: KClass<*>) {
        queueCounts[kClass] = (queueCounts[kClass] ?: 0) + 1
    }

    /**
     * Records a replay event.
     */
    internal fun recordReplay(kClass: KClass<*>, count: Int) {
        replayCounts[kClass] = (replayCounts[kClass] ?: 0) + count
    }

    /**
     * Records an expiry event.
     */
    internal fun recordExpiry(kClass: KClass<*>, count: Int) {
        expiryCounts[kClass] = (expiryCounts[kClass] ?: 0) + count
    }

    /**
     * Gets total dispatch count for a feature.
     */
    fun getDispatchCount(kClass: KClass<*>): Long = dispatchCounts[kClass] ?: 0

    /**
     * Gets total queue count for a feature.
     */
    fun getQueueCount(kClass: KClass<*>): Long = queueCounts[kClass] ?: 0

    /**
     * Gets total replay count for a feature.
     */
    fun getReplayCount(kClass: KClass<*>): Long = replayCounts[kClass] ?: 0

    /**
     * Gets total expiry count for a feature.
     */
    fun getExpiryCount(kClass: KClass<*>): Long = expiryCounts[kClass] ?: 0

    /**
     * Gets all metrics as a summary map.
     */
    fun getAllMetrics(): Map<String, Map<String, Long>> {
        val allKeys = (dispatchCounts.keys + queueCounts.keys + replayCounts.keys + expiryCounts.keys).distinct()

        return allKeys.associate { kClass ->
            kClass.simpleName.orEmpty() to mapOf(
                "dispatches" to getDispatchCount(kClass),
                "queued" to getQueueCount(kClass),
                "replayed" to getReplayCount(kClass),
                "expired" to getExpiryCount(kClass)
            )
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
        dispatchCounts.clear()
        queueCounts.clear()
        replayCounts.clear()
        expiryCounts.clear()
    }
}

/**
 * Extension to enable/disable metrics tracking on KRelay.
 */
var KRelay.metricsEnabled: Boolean
    get() = KRelayMetrics.isEnabled
    set(value) {
        KRelayMetrics.isEnabled = value
    }

/**
 * Extension to get metrics for a specific feature.
 */
inline fun <reified T : RelayFeature> KRelay.getMetrics(): Map<String, Long> {
    return mapOf(
        "dispatches" to KRelayMetrics.getDispatchCount(T::class),
        "queued" to KRelayMetrics.getQueueCount(T::class),
        "replayed" to KRelayMetrics.getReplayCount(T::class),
        "expired" to KRelayMetrics.getExpiryCount(T::class)
    )
}

/**
 * Internal flag to enable/disable metrics.
 */
private var KRelayMetrics.isEnabled: Boolean
    get() = _metricsEnabled
    set(value) {
        _metricsEnabled = value
    }

private var _metricsEnabled: Boolean = false

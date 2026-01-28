package dev.brewkits.krelay

/**
 * Debug information snapshot of KRelay's current state.
 *
 * Use [KRelay.getDebugInfo] to obtain this snapshot, or [KRelay.dump] to print it.
 *
 * @property registeredFeaturesCount Number of features with alive implementations
 * @property registeredFeatures List of registered feature names
 * @property featureQueues Map of feature name to pending action count
 * @property totalPendingActions Total number of pending actions across all features
 * @property expiredActionsRemoved Number of expired actions removed during this snapshot
 * @property maxQueueSize Current max queue size configuration
 * @property actionExpiryMs Current action expiry time in milliseconds
 * @property debugMode Current debug mode status
 */
data class DebugInfo(
    val registeredFeaturesCount: Int,
    val registeredFeatures: List<String>,
    val featureQueues: Map<String, Int>,
    val totalPendingActions: Int,
    val expiredActionsRemoved: Int,
    val maxQueueSize: Int,
    val actionExpiryMs: Long,
    val debugMode: Boolean
) {
    /**
     * Returns a formatted string representation of the debug info.
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("=== KRelay Debug Info ===")
        sb.appendLine("Registered Features: $registeredFeaturesCount")

        if (registeredFeatures.isNotEmpty()) {
            registeredFeatures.forEach { featureName ->
                sb.appendLine("  - $featureName (alive)")
            }
        } else {
            sb.appendLine("  (none)")
        }

        sb.appendLine()
        sb.appendLine("Pending Actions by Feature:")
        if (featureQueues.isNotEmpty()) {
            featureQueues.forEach { (featureName, count) ->
                sb.appendLine("  - $featureName: $count events")
            }
        } else {
            sb.appendLine("  (none)")
        }

        sb.appendLine()
        sb.appendLine("Total Pending: $totalPendingActions events")
        if (expiredActionsRemoved > 0) {
            sb.appendLine("Expired & Removed: $expiredActionsRemoved events")
        }

        sb.appendLine()
        sb.appendLine("Configuration:")
        sb.appendLine("  - Max Queue Size: $maxQueueSize")
        sb.appendLine("  - Action Expiry: ${actionExpiryMs}ms (${actionExpiryMs / 60000.0} min)")
        sb.appendLine("  - Debug Mode: $debugMode")
        sb.appendLine("========================")

        return sb.toString()
    }
}

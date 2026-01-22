package dev.brewkits.krelay

/**
 * Wrapper for queued actions with timestamp tracking.
 *
 * This allows KRelay to:
 * - Track when actions were queued
 * - Expire old actions automatically
 * - Prioritize actions (future enhancement)
 */
@PublishedApi
internal data class QueuedAction(
    val action: (Any) -> Unit,
    val timestampMs: Long = currentTimeMillis(),
    val priority: Int = 0  // Future: 0 = normal, higher = more important
) {
    /**
     * Checks if this action has expired based on the given expiry duration.
     */
    fun isExpired(expiryMs: Long): Boolean {
        return (currentTimeMillis() - timestampMs) > expiryMs
    }
}

/**
 * Platform-agnostic current time in milliseconds.
 */
@PublishedApi
internal expect fun currentTimeMillis(): Long

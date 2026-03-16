package dev.brewkits.krelay

/**
 * Wrapper for queued actions with timestamp tracking.
 *
 * This allows KRelay to:
 * - Track when actions were queued
 * - Expire old actions automatically
 * - Prioritize actions during replay (higher value = replayed first)
 * - Tag actions with a scope token for selective cancellation
 */
@PublishedApi
internal data class QueuedAction(
    val action: (Any) -> Unit,
    val timestampMs: Long = currentTimeMillis(),
    val priority: Int = 0,
    val scopeToken: String? = null
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

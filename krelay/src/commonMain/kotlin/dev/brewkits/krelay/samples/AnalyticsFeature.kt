package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Analytics feature for tracking events and user behavior.
 *
 * Use Case: Track analytics events from shared business logic without platform coupling.
 *
 * Example:
 * ```kotlin
 * KRelay.dispatch<AnalyticsFeature> {
 *     it.track("button_clicked", mapOf(
 *         "screen" to "home",
 *         "button_id" to "login"
 *     ))
 * }
 * ```
 *
 * Platform Implementation:
 * - Can integrate with Firebase Analytics, Mixpanel, Amplitude, etc.
 * - Android/iOS SDKs wrapped in this interface
 *
 * Note: Use this for non-critical analytics only.
 * For critical analytics, use a persistent queue solution.
 */
interface AnalyticsFeature : RelayFeature {

    /**
     * Track a simple event.
     * @param eventName Name of the event (e.g., "button_clicked")
     */
    fun track(eventName: String)

    /**
     * Track an event with parameters.
     * @param eventName Name of the event
     * @param parameters Event parameters as key-value pairs
     */
    fun track(eventName: String, parameters: Map<String, Any>)

    /**
     * Set user property.
     * @param key Property key (e.g., "user_type")
     * @param value Property value (e.g., "premium")
     */
    fun setUserProperty(key: String, value: String)

    /**
     * Set user ID for tracking.
     * @param userId Unique user identifier
     */
    fun setUserId(userId: String)

    /**
     * Track a screen view.
     * @param screenName Name of the screen
     * @param screenClass Class name of the screen (optional)
     */
    fun trackScreen(screenName: String, screenClass: String? = null)
}

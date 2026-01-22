package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Sample feature: In-app notifications.
 *
 * This demonstrates a more complex feature that might need to show
 * rich notifications with titles, actions, and custom styling.
 *
 * Platform implementations:
 * - Android: Use NotificationCompat.Builder or custom in-app banner
 * - iOS: Use UNUserNotificationCenter or custom banner view
 */
interface NotificationBridge : RelayFeature {
    /**
     * Shows an in-app notification banner.
     *
     * @param title The notification title
     * @param message The notification message
     * @param duration Duration in seconds (0 = until dismissed)
     */
    fun showInAppNotification(
        title: String,
        message: String,
        duration: Int = 3
    )

    /**
     * Shows a system notification (requires permissions).
     *
     * @param title The notification title
     * @param message The notification message
     */
    fun showSystemNotification(
        title: String,
        message: String
    )
}

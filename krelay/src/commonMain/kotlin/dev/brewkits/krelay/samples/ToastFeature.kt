package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Sample feature: Toast/Snackbar notifications.
 *
 * This demonstrates the most common use case for KRelay:
 * showing simple UI notifications from shared business logic.
 *
 * Platform implementations:
 * - Android: Use Toast.makeText() or Snackbar
 * - iOS: Use UIAlertController or custom toast view
 */
interface ToastFeature : RelayFeature {
    /**
     * Shows a short toast message.
     * @param message The message to display
     */
    fun showShort(message: String)

    /**
     * Shows a long toast message.
     * @param message The message to display
     */
    fun showLong(message: String)
}

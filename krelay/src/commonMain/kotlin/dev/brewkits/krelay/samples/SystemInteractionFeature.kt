package dev.brewkits.krelay.samples

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature

/**
 * System interaction feature for native platform capabilities like
 * In-App Review and App Update checking.
 *
 * Use Case: Trigger native system dialogs from shared code without
 * coupling to platform-specific APIs.
 *
 * Example:
 * ```kotlin
 * // After user completes an order
 * KRelay.dispatch<SystemInteractionFeature> {
 *     it.requestInAppReview()
 * }
 *
 * // On app startup
 * KRelay.dispatch<SystemInteractionFeature> {
 *     it.checkForAppUpdates { updateAvailable ->
 *         if (updateAvailable) showUpdatePrompt()
 *     }
 * }
 * ```
 *
 * Platform Implementation:
 * - Android: Uses Google Play Core Library (ReviewManager, AppUpdateManager)
 * - iOS: Uses StoreKit (SKStoreReviewController, App Store API)
 *
 * Benefits with KRelay:
 * - ViewModel decides WHEN to request review (business logic)
 * - UI handles HOW to show review (platform implementation)
 * - Separates business logic from system APIs
 * - Easy to mock for testing (no actual review dialogs in tests)
 */
interface SystemInteractionFeature : RelayFeature {

    /**
     * Request in-app review dialog.
     *
     * Shows the native 5-star rating dialog without leaving the app.
     * The OS controls when the dialog actually appears (may not show every time).
     *
     * Best practices:
     * - Call after positive user experiences (completed order, finished task)
     * - Don't call too frequently (OS will throttle)
     * - Don't tie to a button (violates App Store guidelines)
     *
     * Platform behavior:
     * - Android: Shows Google Play in-app review dialog
     * - iOS: Shows App Store rating dialog (SKStoreReviewController)
     */
    fun requestInAppReview()

    /**
     * Check if app update is available.
     *
     * @param callback Called with true if update is available
     */
    fun checkForAppUpdates(callback: (Boolean) -> Unit = {})

    /**
     * Prompt user to update app.
     *
     * @param flexible If true, allows user to continue using app while downloading
     * @param callback Called with true if user initiated update
     */
    fun promptUpdate(flexible: Boolean = true, callback: (Boolean) -> Unit = {})

    /**
     * Open app in system settings.
     *
     * Useful for directing users to enable permissions.
     */
    fun openAppSettings()

    /**
     * Open app store page for this app.
     *
     * @param forReview If true, opens directly to review section
     */
    fun openAppStorePage(forReview: Boolean = false)

    /**
     * Share app with others.
     *
     * Opens native share sheet with app store link.
     */
    fun shareApp()
}

/**
 * Example ViewModel using SystemInteractionFeature.
 *
 * Shows when and how to trigger system interactions.
 */
class SystemInteractionDemoViewModel {

    /**
     * Request review after user completes positive action.
     *
     * This is the RECOMMENDED pattern:
     * - User completes an order successfully
     * - They're in a positive mood
     * - Good time to ask for review
     */
    fun onOrderCompleted(orderId: String) {
        // Business logic
        KRelay.dispatch<AnalyticsFeature> {
            it.track("order_completed", mapOf("order_id" to orderId))
        }

        KRelay.dispatch<ToastFeature> {
            it.showLong("Order #$orderId confirmed!")
        }

        // Request review (fire-and-forget)
        // ViewModel decides WHEN, UI decides HOW
        KRelay.dispatch<SystemInteractionFeature> {
            it.requestInAppReview()
        }
    }

    /**
     * Request review after user achieves milestone.
     */
    fun onMilestoneReached(milestone: String) {
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<NotificationBridge> {
            it.showInAppNotification(
                title = "Achievement Unlocked!",
                message = milestone
            )
        }

        // Good time to ask for review
        KRelay.dispatch<SystemInteractionFeature> {
            it.requestInAppReview()
        }
    }

    /**
     * Check for updates on app startup.
     */
    fun onAppStarted() {
        KRelay.dispatch<SystemInteractionFeature> {
            it.checkForAppUpdates { updateAvailable ->
                if (updateAvailable) {
                    showUpdateAvailableMessage()
                }
            }
        }
    }

    /**
     * Force update check from settings screen.
     */
    fun checkForUpdates() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Checking for updates...")
        }

        KRelay.dispatch<SystemInteractionFeature> {
            it.checkForAppUpdates { updateAvailable ->
                if (updateAvailable) {
                    promptUserToUpdate()
                } else {
                    KRelay.dispatch<ToastFeature> {
                        it.showShort("You're on the latest version!")
                    }
                }
            }
        }
    }

    /**
     * Handle permission denied - redirect to settings.
     */
    fun onPermissionPermanentlyDenied(permission: String) {
        KRelay.dispatch<ToastFeature> {
            it.showLong("$permission permission required. Open settings?")
        }

        // Give user option to open settings
        // (In real app, show dialog first)
        KRelay.dispatch<SystemInteractionFeature> {
            it.openAppSettings()
        }
    }

    /**
     * User wants to rate app manually.
     */
    fun onRateAppClicked() {
        // Open App Store directly to review page
        KRelay.dispatch<SystemInteractionFeature> {
            it.openAppStorePage(forReview = true)
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("rate_app_clicked")
        }
    }

    /**
     * User wants to share app.
     */
    fun onShareAppClicked() {
        KRelay.dispatch<SystemInteractionFeature> {
            it.shareApp()
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("share_app_clicked")
        }
    }

    // Helper functions
    private fun showUpdateAvailableMessage() {
        KRelay.dispatch<NotificationBridge> {
            it.showInAppNotification(
                title = "Update Available",
                message = "A new version is available. Tap to update.",
                duration = 10
            )
        }
    }

    private fun promptUserToUpdate() {
        KRelay.dispatch<SystemInteractionFeature> {
            it.promptUpdate(flexible = true) { accepted ->
                if (accepted) {
                    KRelay.dispatch<ToastFeature> {
                        it.showShort("Update started")
                    }
                }
            }
        }
    }
}

/**
 * Advanced example: Smart review request with conditions.
 *
 * Shows how to implement intelligent review prompting.
 *
 * Note: In real app, use kotlinx-datetime for cross-platform time handling.
 * This is simplified for demo purposes.
 */
class SmartReviewViewModel {
    private var orderCount = 0

    fun onOrderCompleted() {
        orderCount++

        // Only request review if user completed 5+ orders (engaged user)
        // In real app, also check time since last review using kotlinx-datetime
        if (orderCount >= 5) {
            KRelay.dispatch<SystemInteractionFeature> {
                it.requestInAppReview()
            }
            orderCount = 0 // Reset counter
        }
    }
}

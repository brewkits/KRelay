package dev.brewkits.krelay.samples

import dev.brewkits.krelay.KRelay

/**
 * Demo ViewModel showing comprehensive KRelay usage from shared code.
 *
 * This class demonstrates all recommended use cases:
 * 1. Toast/Snackbar/Alert Messages
 * 2. Navigation Commands
 * 3. Permission Requests
 * 4. Haptic Feedback
 * 5. Simple Analytics
 * 6. In-App Notifications
 *
 * Key Principles:
 * - Fire-and-Forget: Just dispatch, don't wait
 * - No Memory Leaks: WeakReference handles cleanup
 * - Sticky Queue: Commands survive rotation/lifecycle
 * - Always Main Thread: Safe for UI operations
 */
class DemoViewModel {

    // ============================================================================
    // Use Case 1: Toast/Snackbar/Alert Messages
    // ============================================================================

    /**
     * Show simple feedback after data loading.
     * Perfect for: Background operations, API calls, database queries
     */
    fun onDataLoaded(itemCount: Int) {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📦 [DemoViewModel] onDataLoaded() called")
        println("   → Business Logic: Data fetch completed")
        println("   → Items loaded: $itemCount")
        println("   → Need to show user feedback")

        println("\n📤 [DemoViewModel] Dispatching toast via KRelay...")
        println("   → Calling: KRelay.dispatch<ToastFeature> { it.showShort(...) }")

        KRelay.dispatch<ToastFeature> {
            it.showShort("Loaded $itemCount items!")
        }

        println("   → Toast dispatch completed")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    /**
     * Show error message.
     * Demonstrates: Sticky Queue - if app is backgrounded, toast shows on return
     */
    fun onError(error: String) {
        KRelay.dispatch<ToastFeature> {
            it.showLong("Error: $error")
        }
    }

    /**
     * Show success confirmation.
     */
    fun onOperationSuccess(operation: String) {
        KRelay.dispatch<ToastFeature> {
            it.showShort("$operation completed successfully!")
        }
    }

    // ============================================================================
    // Use Case 2: Navigation Commands
    // ============================================================================

    /**
     * Navigate after login.
     * Perfect for: Post-authentication navigation, workflow completion
     */
    fun onLoginSuccess() {
        // Show feedback
        KRelay.dispatch<ToastFeature> {
            it.showLong("Welcome back!")
        }

        // Navigate to home
        KRelay.dispatch<NavigationFeature> {
            it.navigateTo("home")
        }

        // Track analytics
        KRelay.dispatch<AnalyticsFeature> {
            it.track("login_success")
        }
    }

    /**
     * Navigate to details screen.
     */
    fun openItemDetails(itemId: String) {
        KRelay.dispatch<NavigationFeature> {
            it.navigateTo("details/$itemId")
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("item_opened", mapOf("item_id" to itemId))
        }
    }

    /**
     * Navigate back after error.
     */
    fun onCriticalError(message: String) {
        KRelay.dispatch<ToastFeature> {
            it.showLong(message)
        }

        KRelay.dispatch<NavigationFeature> {
            it.navigateBack()
        }
    }

    // ============================================================================
    // Use Case 3: Permission Requests
    // ============================================================================

    /**
     * Request camera permission before taking photo.
     * Perfect for: Feature gating, permission flows
     */
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> {
            it.requestCamera { granted ->
                if (granted) {
                    startCamera()
                } else {
                    showPermissionDenied("Camera")
                }
            }
        }
    }

    /**
     * Request location permission before showing map.
     */
    fun showUserLocation() {
        KRelay.dispatch<PermissionFeature> {
            it.requestLocation { granted ->
                if (granted) {
                    loadMap()
                } else {
                    showPermissionDenied("Location")
                }
            }
        }
    }

    /**
     * Check permission before feature use.
     */
    fun recordAudio() {
        KRelay.dispatch<PermissionFeature> {
            if (it.isCameraGranted()) {
                startRecording()
            } else {
                it.requestMicrophone { granted ->
                    if (granted) startRecording()
                }
            }
        }
    }

    // ============================================================================
    // Use Case 4: Haptic Feedback
    // ============================================================================

    /**
     * Trigger haptic on button press.
     * Perfect for: Button clicks, selections, confirmations
     */
    fun onButtonPressed() {
        KRelay.dispatch<HapticFeature> {
            it.impact(HapticStyle.LIGHT)
        }
    }

    /**
     * Trigger success haptic after operation.
     */
    fun onPaymentSuccess() {
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<ToastFeature> {
            it.showShort("Payment successful!")
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("payment_completed")
        }
    }

    /**
     * Trigger error haptic on validation failure.
     */
    fun onValidationError(field: String) {
        KRelay.dispatch<HapticFeature> {
            it.error()
        }

        KRelay.dispatch<ToastFeature> {
            it.showShort("$field is required")
        }
    }

    /**
     * Trigger selection haptic for picker.
     */
    fun onPickerValueChanged() {
        KRelay.dispatch<HapticFeature> {
            it.selection()
        }
    }

    // ============================================================================
    // Use Case 5: Simple Analytics
    // ============================================================================

    /**
     * Track screen view.
     * Perfect for: Navigation tracking, screen analytics
     */
    fun onScreenViewed(screenName: String) {
        KRelay.dispatch<AnalyticsFeature> {
            it.trackScreen(screenName)
        }
    }

    /**
     * Track user action.
     */
    fun onUserAction(action: String, parameters: Map<String, Any> = emptyMap()) {
        KRelay.dispatch<AnalyticsFeature> {
            it.track(action, parameters)
        }
    }

    /**
     * Track purchase event.
     */
    fun onPurchaseCompleted(productId: String, price: Double) {
        KRelay.dispatch<AnalyticsFeature> {
            it.track("purchase_completed", mapOf(
                "product_id" to productId,
                "price" to price,
                "currency" to "USD"
            ))
        }
    }

    /**
     * Set user properties.
     */
    fun onUserTypeChanged(userType: String) {
        KRelay.dispatch<AnalyticsFeature> {
            it.setUserProperty("user_type", userType)
        }
    }

    // ============================================================================
    // Use Case 6: In-App Notifications
    // ============================================================================

    /**
     * Show notification after background sync.
     * Perfect for: Background operations, updates, alerts
     */
    fun onSyncCompleted(itemsUpdated: Int) {
        KRelay.dispatch<NotificationBridge> {
            it.showInAppNotification(
                title = "Sync Complete",
                message = "$itemsUpdated items updated",
                duration = 5
            )
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("sync_completed", mapOf("items" to itemsUpdated))
        }
    }

    /**
     * Show important alert.
     */
    fun onImportantUpdate(message: String) {
        KRelay.dispatch<NotificationBridge> {
            it.showInAppNotification(
                title = "Important Update",
                message = message,
                duration = 10
            )
        }
    }

    // ============================================================================
    // Complex Workflows
    // ============================================================================

    /**
     * Complete checkout flow with multiple features.
     * Demonstrates: Coordinating multiple features in sequence
     */
    fun completeCheckout(orderId: String, amount: Double) {
        // Haptic feedback
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        // Show confirmation
        KRelay.dispatch<ToastFeature> {
            it.showLong("Order #$orderId confirmed!")
        }

        // Track purchase
        KRelay.dispatch<AnalyticsFeature> {
            it.track("checkout_completed", mapOf(
                "order_id" to orderId,
                "amount" to amount
            ))
        }

        // Navigate to success screen
        KRelay.dispatch<NavigationFeature> {
            it.navigateTo("order-success/$orderId")
        }

        // Show notification
        KRelay.dispatch<NotificationBridge> {
            it.showInAppNotification(
                title = "Order Confirmed",
                message = "Your order #$orderId is being processed"
            )
        }
    }

    /**
     * Handle long-running operation with progress updates.
     * Demonstrates: Sticky Queue - all commands queued and replayed
     */
    suspend fun performLongOperation() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Starting operation...")
        }

        // Simulate work (network, database, etc.)
        // kotlinx.coroutines.delay(1000)  // Commented out to avoid dependency

        KRelay.dispatch<HapticFeature> {
            it.impact(HapticStyle.LIGHT)
        }

        KRelay.dispatch<ToastFeature> {
            it.showShort("Processing...")
        }

        // kotlinx.coroutines.delay(1000)  // Commented out to avoid dependency

        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<NotificationBridge> {
            it.showInAppNotification(
                title = "Operation Complete",
                message = "All tasks finished successfully"
            )
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("long_operation_completed")
        }
    }

    // ============================================================================
    // Helper Functions (Private - just for demo)
    // ============================================================================

    private fun startCamera() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Camera started")
        }
    }

    private fun loadMap() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Loading map...")
        }
    }

    private fun startRecording() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Recording started")
        }
    }

    private fun showPermissionDenied(permission: String) {
        KRelay.dispatch<ToastFeature> {
            it.showLong("$permission permission denied")
        }

        KRelay.dispatch<HapticFeature> {
            it.error()
        }
    }
}


package dev.brewkits.krelay.samples

import dev.brewkits.krelay.KRelay

/**
 * Comprehensive demo showing KRelay integration with popular KMP libraries.
 *
 * This ViewModel demonstrates how KRelay acts as "The Glue Code Standard" to
 * cleanly integrate with specialized libraries without tight coupling.
 *
 * Featured Integrations:
 * 1. Permission Management (Moko Permissions)
 * 2. Biometric Authentication (Moko Biometry / BiometricPrompt)
 * 3. In-App Review & Updates (Play Core / StoreKit)
 * 4. Media Picking (Peekaboo / Image Picker)
 *
 * Key Benefits:
 * - ViewModel has ZERO dependencies on platform libraries
 * - No memory leaks (no Activity/Context/Controller references)
 * - Easy to test (mock implementations)
 * - Clean architecture maintained
 */
class IntegrationsViewModel {

    // ============================================================================
    // Integration 1: Permission Management (Moko Permissions)
    // ============================================================================

    /**
     * Moko Permissions integration example.
     *
     * Problem: Moko PermissionsController needs to be bound to Activity/ViewController.
     *          Passing it to ViewModel causes memory leaks.
     *
     * Solution: ViewModel dispatches permission request via KRelay.
     *          UI layer implements using Moko PermissionsController.
     *
     * Result: ViewModel is clean, testable, and leak-free!
     */
    fun requestCameraPermission() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📸 [Integration Demo] Requesting camera permission")
        println("   → ViewModel: Dispatching permission request")
        println("   → Platform: Will use Moko PermissionsController")
        println("   → No coupling: ViewModel has no Moko dependency!")

        KRelay.dispatch<PermissionFeature> {
            it.requestCamera { granted ->
                if (granted) {
                    println("   ✅ Permission granted")
                    onCameraPermissionGranted()
                } else {
                    println("   ❌ Permission denied")
                    onCameraPermissionDenied()
                }
            }
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    private fun onCameraPermissionGranted() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Camera permission granted!")
        }

        KRelay.dispatch<HapticFeature> {
            it.success()
        }
    }

    private fun onCameraPermissionDenied() {
        KRelay.dispatch<ToastFeature> {
            it.showLong("Camera permission is required")
        }

        KRelay.dispatch<HapticFeature> {
            it.error()
        }
    }

    /**
     * Check permission before taking action.
     */
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> { permission ->
            if (permission.isCameraGranted()) {
                // Already granted - proceed directly
                openCamera()
            } else {
                // Request permission first
                permission.requestCamera { granted ->
                    if (granted) openCamera()
                }
            }
        }
    }

    // ============================================================================
    // Integration 2: Biometric Authentication (Moko Biometry)
    // ============================================================================

    /**
     * Moko Biometry integration example.
     *
     * Problem: BiometryManager requires Activity context and lifecycle.
     *          FaceID/TouchID dialogs are UI-layer concerns.
     *
     * Solution: ViewModel requests authentication via KRelay.
     *          UI layer implements using Moko BiometryManager.
     *
     * Result: Business logic (when to authenticate) separated from
     *         implementation (how to authenticate).
     */
    fun authenticateWithBiometrics() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔐 [Integration Demo] Requesting biometric auth")
        println("   → ViewModel: Decides WHEN to authenticate")
        println("   → Platform: Implements HOW (Moko BiometryManager)")
        println("   → No coupling: ViewModel doesn't hold BiometryManager!")

        KRelay.dispatch<BiometricFeature> {
            it.authenticate(
                title = "Verify Your Identity",
                subtitle = "Use biometrics to access secure features",
                onSuccess = {
                    println("   ✅ Authentication successful")
                    onBiometricSuccess()
                },
                onError = { error ->
                    println("   ❌ Authentication failed: $error")
                    onBiometricError(error)
                }
            )
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    private fun onBiometricSuccess() {
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<ToastFeature> {
            it.showShort("Authentication successful!")
        }

        // Navigate to secure area
        KRelay.dispatch<NavigationFeature> {
            it.navigateTo("secure-vault")
        }
    }

    private fun onBiometricError(error: String) {
        KRelay.dispatch<HapticFeature> {
            it.error()
        }

        KRelay.dispatch<ToastFeature> {
            it.showLong("Authentication failed: $error")
        }
    }

    /**
     * Protect payment with biometrics.
     */
    fun confirmPayment(amount: Double) {
        KRelay.dispatch<BiometricFeature> { biometric ->
            // Check availability first
            if (!biometric.isAvailable()) {
                KRelay.dispatch<ToastFeature> {
                    it.showShort("Biometrics not available")
                }
                return@dispatch
            }

            // Request authentication
            biometric.authenticate(
                title = "Confirm Payment",
                subtitle = "Authorize payment of \$$amount",
                onSuccess = {
                    processPayment(amount)
                }
            )
        }
    }

    private fun processPayment(amount: Double) {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Payment of \$$amount processed")
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("payment_completed", mapOf("amount" to amount))
        }
    }

    // ============================================================================
    // Integration 3: In-App Review & Updates (Play Core / StoreKit)
    // ============================================================================

    /**
     * In-App Review integration example.
     *
     * Problem: ReviewManager (Android) and SKStoreReviewController (iOS)
     *          require Activity/ViewController context.
     *
     * Solution: ViewModel decides WHEN to request review (business logic).
     *          UI layer decides HOW to show review (platform implementation).
     *
     * Result: Business logic separated from platform APIs.
     *         Easy to test - no actual review dialogs in unit tests!
     */
    fun requestAppReview() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("⭐ [Integration Demo] Requesting in-app review")
        println("   → ViewModel: Decides WHEN to ask for review")
        println("   → Platform: Shows native review dialog")
        println("   → Android: Uses Play Core ReviewManager")
        println("   → iOS: Uses SKStoreReviewController")

        KRelay.dispatch<SystemInteractionFeature> {
            it.requestInAppReview()
        }

        println("   → Review request dispatched")
        println("   → OS controls if/when dialog shows")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    /**
     * Request review after positive user action.
     *
     * This is the RECOMMENDED pattern!
     */
    fun onOrderCompleted(orderId: String, amount: Double) {
        // Show success feedback
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<ToastFeature> {
            it.showLong("Order #$orderId confirmed!")
        }

        // Track analytics
        KRelay.dispatch<AnalyticsFeature> {
            it.track("order_completed", mapOf(
                "order_id" to orderId,
                "amount" to amount
            ))
        }

        // User is happy - good time to request review!
        KRelay.dispatch<SystemInteractionFeature> {
            it.requestInAppReview()
        }
    }

    /**
     * Check for app updates.
     */
    fun checkForUpdates() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔄 [Integration Demo] Checking for updates")

        KRelay.dispatch<SystemInteractionFeature> {
            it.checkForAppUpdates { updateAvailable ->
                if (updateAvailable) {
                    println("   ✅ Update available")
                    KRelay.dispatch<ToastFeature> {
                        it.showLong("New version available!")
                    }
                } else {
                    println("   ℹ️ Already on latest version")
                    KRelay.dispatch<ToastFeature> {
                        it.showShort("You're on the latest version!")
                    }
                }
            }
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    /**
     * Open app settings.
     */
    fun openSettings() {
        KRelay.dispatch<SystemInteractionFeature> {
            it.openAppSettings()
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("settings_opened")
        }
    }

    // ============================================================================
    // Integration 4: Media Picking (Peekaboo / Image Picker)
    // ============================================================================

    /**
     * Peekaboo integration example.
     *
     * Problem: rememberImagePickerLauncher is a Compose function.
     *          ViewModel can't call Compose functions.
     *
     * Solution: ViewModel triggers picker via KRelay.
     *          UI layer registers Peekaboo launcher as implementation.
     *
     * Result: ViewModel controls WHEN to pick image.
     *         UI controls HOW to show picker.
     */
    fun pickProfilePicture() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🖼️ [Integration Demo] Picking image from gallery")
        println("   → ViewModel: Triggers image picker")
        println("   → Platform: Uses Peekaboo/ImagePicker")
        println("   → No coupling: ViewModel doesn't call Compose functions!")

        KRelay.dispatch<MediaFeature> {
            it.pickImageFromGallery { imageData ->
                if (imageData != null) {
                    println("   ✅ Image selected: ${imageData.size} bytes")
                    onImagePicked(imageData)
                } else {
                    println("   ℹ️ User cancelled")
                    onImagePickCancelled()
                }
            }
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    private fun onImagePicked(imageData: ByteArray) {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Image selected: ${imageData.size} bytes")
        }

        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        // Upload to server
        KRelay.dispatch<AnalyticsFeature> {
            it.track("profile_picture_updated", mapOf("size" to imageData.size))
        }
    }

    private fun onImagePickCancelled() {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Image selection cancelled")
        }
    }

    /**
     * Capture photo with camera.
     */
    fun capturePhoto() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📷 [Integration Demo] Capturing photo")
        println("   → Step 1: Request camera permission")

        // First request permission
        KRelay.dispatch<PermissionFeature> { permission ->
            permission.requestCamera { granted ->
                if (!granted) {
                    println("   ❌ Permission denied")
                    KRelay.dispatch<ToastFeature> {
                        it.showLong("Camera permission required")
                    }
                    return@requestCamera
                }

                println("   ✅ Permission granted")
                println("   → Step 2: Open camera")

                // Then open camera
                KRelay.dispatch<MediaFeature> { media ->
                    media.capturePhoto { imageData ->
                        if (imageData != null) {
                            println("   ✅ Photo captured: ${imageData.size} bytes")
                            onPhotoCaptured(imageData)
                        }
                    }
                }
            }
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    private fun onPhotoCaptured(imageData: ByteArray) {
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<ToastFeature> {
            it.showShort("Photo captured!")
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("photo_captured", mapOf("size" to imageData.size))
        }
    }

    /**
     * Upload multiple photos.
     */
    fun uploadMultiplePhotos() {
        KRelay.dispatch<MediaFeature> {
            it.pickMultipleImages(maxCount = 5) { images ->
                if (images.isNotEmpty()) {
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Uploading ${images.size} photos...")
                    }

                    KRelay.dispatch<AnalyticsFeature> { analytics ->
                        analytics.track("photos_uploaded", mapOf("count" to images.size))
                    }
                }
            }
        }
    }

    // ============================================================================
    // Complex Workflow: Combining Multiple Integrations
    // ============================================================================

    /**
     * Complete user onboarding flow.
     *
     * Demonstrates coordinating multiple integrations:
     * 1. Request permissions
     * 2. Pick profile picture
     * 3. Setup biometric auth
     * 4. Navigate to home
     */
    fun completeOnboarding() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🚀 [Integration Demo] Complete onboarding flow")
        println("   → Multi-step workflow using multiple integrations")

        // Step 1: Request camera permission for profile picture
        KRelay.dispatch<PermissionFeature> { permission ->
            permission.requestCamera { cameraGranted ->
                println("   → Step 1: Camera permission: $cameraGranted")

                // Step 2: Pick profile picture
                KRelay.dispatch<MediaFeature> { media ->
                    media.pickImageFromGallery { imageData ->
                        if (imageData != null) {
                            println("   → Step 2: Profile picture selected")

                            // Step 3: Setup biometric auth
                            KRelay.dispatch<BiometricFeature> { biometric ->
                                if (biometric.isAvailable()) {
                                    biometric.authenticateSimple("Setup Biometrics") { success ->
                                        println("   → Step 3: Biometrics setup: $success")
                                        completeOnboardingSuccess()
                                    }
                                } else {
                                    completeOnboardingSuccess()
                                }
                            }
                        }
                    }
                }
            }
        }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    private fun completeOnboardingSuccess() {
        // Step 4: Show success and navigate
        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        KRelay.dispatch<ToastFeature> {
            it.showLong("Welcome! Setup complete")
        }

        KRelay.dispatch<NavigationFeature> {
            it.navigateTo("home")
        }

        // Track completion
        KRelay.dispatch<AnalyticsFeature> {
            it.track("onboarding_completed")
        }
    }

    private fun openCamera() {
        KRelay.dispatch<MediaFeature> {
            it.capturePhoto { imageData ->
                if (imageData != null) {
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Photo captured!")
                    }
                }
            }
        }
    }
}

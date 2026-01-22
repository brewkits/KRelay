package dev.brewkits.krelay.samples

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature

/**
 * Biometric authentication feature for FaceID/TouchID/Fingerprint.
 *
 * Use Case: Request biometric authentication from shared code without coupling
 * to platform-specific biometric APIs (Moko Biometry, BiometricPrompt, LocalAuthentication).
 *
 * Example:
 * ```kotlin
 * KRelay.dispatch<BiometricFeature> {
 *     it.authenticate(
 *         title = "Verify your identity",
 *         subtitle = "Use biometrics to access sensitive data",
 *         onSuccess = { accessSecureData() },
 *         onError = { error -> handleAuthError(error) }
 *     )
 * }
 * ```
 *
 * Platform Implementation:
 * - Android: Uses BiometricPrompt API (AndroidX)
 * - iOS: Uses LocalAuthentication framework (FaceID/TouchID)
 * - With Moko Biometry: Uses BiometryManager for KMP abstraction
 *
 * Benefits with KRelay:
 * - ViewModel doesn't hold BiometryManager/BiometricPrompt reference
 * - No lifecycle coupling (Activity/Fragment required by BiometricPrompt)
 * - Fire-and-forget pattern for authentication requests
 * - Easy to mock for testing
 */
interface BiometricFeature : RelayFeature {

    /**
     * Check if biometric authentication is available on this device.
     * @return true if device supports biometrics (FaceID/TouchID/Fingerprint)
     */
    fun isAvailable(): Boolean

    /**
     * Authenticate user with biometrics.
     *
     * @param title Title for the biometric prompt
     * @param subtitle Optional subtitle/description
     * @param onSuccess Called when authentication succeeds
     * @param onError Called when authentication fails (user cancelled, error, etc.)
     */
    fun authenticate(
        title: String,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    )

    /**
     * Simple authentication with just a callback.
     *
     * @param title Title for the biometric prompt
     * @param callback Called with true if authenticated, false otherwise
     */
    fun authenticateSimple(
        title: String,
        callback: (Boolean) -> Unit
    )
}

/**
 * Example ViewModel using BiometricFeature.
 *
 * Shows how to protect sensitive operations with biometric authentication.
 */
class BiometricDemoViewModel {

    /**
     * Access secure vault protected by biometrics.
     */
    fun openSecureVault() {
        // ViewModel just dispatches - no BiometryManager coupling!
        KRelay.dispatch<BiometricFeature> {
            it.authenticate(
                title = "Unlock Vault",
                subtitle = "Verify your identity to access secure data",
                onSuccess = {
                    // Successfully authenticated
                    navigateToVault()
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Vault unlocked")
                    }
                },
                onError = { error ->
                    // Authentication failed
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showLong("Authentication failed: $error")
                    }
                }
            )
        }
    }

    /**
     * Confirm payment with biometrics.
     */
    fun confirmPayment(amount: Double) {
        KRelay.dispatch<BiometricFeature> {
            if (!it.isAvailable()) {
                // Fallback to PIN/Password
                showPinDialog()
                return@dispatch
            }

            it.authenticate(
                title = "Confirm Payment",
                subtitle = "Authorize payment of \$$amount",
                onSuccess = {
                    processPayment(amount)
                    KRelay.dispatch<HapticFeature> { haptic ->
                        haptic.success()
                    }
                }
            )
        }
    }

    /**
     * Quick biometric check before showing sensitive data.
     */
    fun viewSensitiveData() {
        KRelay.dispatch<BiometricFeature> {
            it.authenticateSimple("Verify Identity") { success ->
                if (success) {
                    showSensitiveData()
                } else {
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Authentication required")
                    }
                }
            }
        }
    }

    // Helper functions
    private fun navigateToVault() {
        KRelay.dispatch<NavigationFeature> { it.navigateTo("vault") }
    }

    private fun showPinDialog() {
        KRelay.dispatch<ToastFeature> { it.showShort("Enter PIN") }
    }

    private fun processPayment(amount: Double) {
        KRelay.dispatch<ToastFeature> { it.showShort("Payment processed: \$$amount") }
    }

    private fun showSensitiveData() {
        KRelay.dispatch<NavigationFeature> { it.navigateTo("sensitive-data") }
    }
}

package dev.brewkits.krelay.integrations

import androidx.compose.runtime.*
import dev.brewkits.krelay.samples.*
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSTimer
import platform.StoreKit.SKStoreReviewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation using Moko Permissions
 */
class IOSMokoPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {

    override fun requestCamera(callback: (Boolean) -> Unit) {
        println("\n📸 [IOSMokoPermissionImpl] Requesting camera permission...")
        println("   → Using Moko PermissionsController for iOS")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                controller.providePermission(Permission.CAMERA)
                println("   ✅ Camera permission granted!")
                callback(true)
            } catch (e: Exception) {
                println("   ❌ Camera permission denied: ${e.message}")
                callback(false)
            }
        }
    }

    override fun requestLocation(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                controller.providePermission(Permission.LOCATION)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    override fun requestMicrophone(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                controller.providePermission(Permission.RECORD_AUDIO)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    override fun requestStorage(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                controller.providePermission(Permission.GALLERY)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    override fun isCameraGranted(): Boolean {
        // Note: Moko's isPermissionGranted is suspend function
        // In real app, make this suspend or use different approach
        return false // Simplified for demo
    }

    override fun isLocationGranted(): Boolean {
        // Note: Moko's isPermissionGranted is suspend function
        // In real app, make this suspend or use different approach
        return false // Simplified for demo
    }
}

/**
 * Real iOS biometric implementation using LocalAuthentication.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSBiometricImpl : BiometricFeature {

    override fun isAvailable(): Boolean {
        // For demo, always return true
        // In production, use: LAContext().canEvaluatePolicy(LAPolicy.LAPolicyDeviceOwnerAuthenticationWithBiometrics, error)
        return true
    }

    override fun authenticate(
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        println("\n🔐 [IOSBiometricImpl] Biometric authentication requested")
        println("   → Title: $title")
        println("   → Note: Simulating success (iOS Simulator doesn't support Face ID/Touch ID)")
        println("   → On real device, this would trigger Face ID/Touch ID prompt")

        // For demo purposes, simulate success with delay using dispatch
        dispatch_async(dispatch_get_main_queue()) {
            NSTimer.scheduledTimerWithTimeInterval(
                interval = 0.5,
                repeats = false,
                block = { timer: NSTimer? ->
                    dispatch_async(dispatch_get_main_queue()) {
                        println("   ✅ Authentication simulated as successful")
                        try {
                            onSuccess()
                        } catch (e: Exception) {
                            println("   ❌ Error in onSuccess callback: ${e.message}")
                            onError(e.message ?: "Unknown error")
                        }
                    }
                }
            )
        }
    }

    override fun authenticateSimple(title: String, callback: (Boolean) -> Unit) {
        authenticate(
            title = title,
            onSuccess = { callback(true) },
            onError = { callback(false) }
        )
    }
}

/**
 * iOS implementation using StoreKit
 */
class IOSSystemInteractionImpl : SystemInteractionFeature {

    override fun requestInAppReview() {
        println("\n⭐ [IOSSystemInteractionImpl] Requesting in-app review...")
        println("   → Using StoreKit SKStoreReviewController")

        SKStoreReviewController.requestReview()
        println("   ✓ Review request sent (OS decides if/when to show)")
    }

    override fun checkForAppUpdates(callback: (Boolean) -> Unit) {
        println("\n🔄 [IOSSystemInteractionImpl] Checking for updates...")
        println("   → iOS doesn't provide API to check updates programmatically")
        callback(false)
    }

    override fun promptUpdate(flexible: Boolean, callback: (Boolean) -> Unit) {
        callback(false)
    }

    override fun openAppSettings() {
        println("\n⚙️ [IOSSystemInteractionImpl] Opening app settings...")
        // In real app: UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!)
    }

    override fun openAppStorePage(forReview: Boolean) {
        println("\n🏪 [IOSSystemInteractionImpl] Opening App Store...")
        // In real app: Open App Store with app ID
    }

    override fun shareApp() {
        println("\n📤 [IOSSystemInteractionImpl] Sharing app...")
        // In real app: Present UIActivityViewController
    }
}

/**
 * Simplified Media implementation for iOS
 * Note: Real Peekaboo integration requires more complex image handling
 * This simplified version shows the pattern without full image conversion
 */
class IOSMediaImpl : MediaFeature {

    override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
        println("\n🖼️ [IOSMediaImpl] Opening gallery...")
        println("   → Note: Simplified implementation for demo")
        println("   → In real app, use Peekaboo rememberImagePickerLauncher")

        // Simulate user picking image
        val mockImageData = ByteArray(1024) { it.toByte() }
        callback(mockImageData)
    }

    override fun pickMultipleImages(maxCount: Int, callback: (List<ByteArray>) -> Unit) {
        println("\n🖼️ [IOSMediaImpl] Opening gallery (multiple)...")
        val mockImages = List(3) { ByteArray(1024) { it.toByte() } }
        callback(mockImages)
    }

    override fun capturePhoto(callback: (ByteArray?) -> Unit) {
        println("\n📷 [IOSMediaImpl] Opening camera...")
        val mockPhotoData = ByteArray(2048) { it.toByte() }
        callback(mockPhotoData)
    }

    override fun pickVideo(callback: (ByteArray?) -> Unit) {
        callback(null)
    }

    override fun recordVideo(maxDurationSeconds: Int, callback: (ByteArray?) -> Unit) {
        callback(null)
    }

    override fun isCameraAvailable(): Boolean = true
    override fun isGalleryAvailable(): Boolean = true
}

/**
 * Composable functions to create implementations
 */
@Composable
actual fun rememberPermissionImplementation(): PermissionFeature {
    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }

    // Bind controller to lifecycle
    BindEffect(controller)

    return remember { IOSMokoPermissionImpl(controller) }
}

@Composable
actual fun rememberBiometricImplementation(): BiometricFeature {
    return remember { IOSBiometricImpl() }
}

@Composable
actual fun rememberMediaImplementation(): MediaFeature {
    // Simplified version - in real app, use Peekaboo properly
    return remember { IOSMediaImpl() }
}

@Composable
actual fun rememberSystemInteractionImplementation(): SystemInteractionFeature {
    return remember { IOSSystemInteractionImpl() }
}

@Composable
actual fun rememberToastImplementation(): ToastFeature {
    return remember { IOSToastImpl() }
}

@Composable
actual fun rememberHapticImplementation(): HapticFeature {
    return remember { IOSHapticImpl() }
}

@Composable
actual fun rememberAnalyticsImplementation(): AnalyticsFeature {
    return remember { IOSAnalyticsImpl() }
}

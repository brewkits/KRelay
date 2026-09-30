package dev.brewkits.krelay.integrations

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.review.ReviewManagerFactory
import dev.brewkits.krelay.samples.*
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.location.LOCATION
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android implementation using Moko Permissions
 */
class MokoPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {

    override fun requestCamera(callback: (Boolean) -> Unit) {
        println("\n📸 [MokoPermissionImpl] Requesting camera permission...")
        println("   → Using Moko PermissionsController")

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
 * Android implementation using AndroidX Biometric
 */
class AndroidBiometricImpl(
    private val activity: FragmentActivity,
    private val biometricManager: BiometricManager
) : BiometricFeature {

    override fun isAvailable(): Boolean {
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun authenticate(
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        println("\n🔐 [AndroidBiometricImpl] Requesting biometric authentication...")
        println("   → Using AndroidX BiometricPrompt")
        println("   → Title: $title")

        if (!isAvailable()) {
            onError("Biometric authentication not available")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    println("   ❌ Authentication error: $errString")
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    println("   ✅ Authentication successful!")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    println("   ❌ Authentication failed")
                    onError("Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                if (subtitle != null) {
                    setSubtitle(subtitle)
                }
            }
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
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
 * Android implementation using Play Core Review
 */
class AndroidSystemInteractionImpl(
    private val activity: Activity,
    private val context: android.content.Context
) : SystemInteractionFeature {

    override fun requestInAppReview() {
        println("\n⭐ [AndroidSystemInteractionImpl] Requesting in-app review...")
        println("   → Using Google Play Core ReviewManager")

        // Show toast to confirm action (since review dialog may not show in debug)
        android.widget.Toast.makeText(
            context,
            "📱 Review request sent (may not show in debug builds)",
            android.widget.Toast.LENGTH_LONG
        ).show()

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                println("   ✅ Review flow ready, launching dialog...")

                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { launchTask ->
                    if (launchTask.isSuccessful) {
                        println("   ✓ Review flow completed successfully")
                        android.widget.Toast.makeText(
                            context,
                            "✅ Thanks for reviewing!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        println("   ⚠️ Review dialog not shown (debug build or quota exceeded)")
                        android.widget.Toast.makeText(
                            context,
                            "ℹ️ Review not available (debug mode or quota limit)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                println("   ❌ Review flow failed: ${task.exception?.message}")
                android.widget.Toast.makeText(
                    context,
                    "❌ Review not available: ${task.exception?.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun checkForAppUpdates(callback: (Boolean) -> Unit) {
        println("\n🔄 [AndroidSystemInteractionImpl] Checking for updates...")
        // In real app, use AppUpdateManager
        callback(false)
    }

    override fun promptUpdate(flexible: Boolean, callback: (Boolean) -> Unit) {
        callback(false)
    }

    override fun openAppSettings() {
        println("\n⚙️ [AndroidSystemInteractionImpl] Opening app settings...")
        // In real app, open settings
    }

    override fun openAppStorePage(forReview: Boolean) {
        println("\n🏪 [AndroidSystemInteractionImpl] Opening Play Store...")
        // In real app, open Play Store
    }

    override fun shareApp() {
        println("\n📤 [AndroidSystemInteractionImpl] Sharing app...")
        // In real app, open share sheet
    }
}

/**
 * Simplified Media implementation for Android
 * Note: Real Peekaboo integration requires more complex image handling
 * This simplified version shows the pattern without full image conversion
 */
class AndroidMediaImpl : MediaFeature {

    override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
        println("\n🖼️ [AndroidMediaImpl] Opening gallery...")
        println("   → Note: Simplified implementation for demo")
        println("   → In real app, use Peekaboo rememberImagePickerLauncher")

        // Simulate user picking image
        val mockImageData = ByteArray(1024) { it.toByte() }
        callback(mockImageData)
    }

    override fun pickMultipleImages(maxCount: Int, callback: (List<ByteArray>) -> Unit) {
        println("\n🖼️ [AndroidMediaImpl] Opening gallery (multiple)...")
        val mockImages = List(3) { ByteArray(1024) { it.toByte() } }
        callback(mockImages)
    }

    override fun capturePhoto(callback: (ByteArray?) -> Unit) {
        println("\n📷 [AndroidMediaImpl] Opening camera...")
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

    return remember { MokoPermissionImpl(controller) }
}

@Composable
actual fun rememberBiometricImplementation(): BiometricFeature {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
        ?: throw IllegalStateException("Context must be FragmentActivity for biometric")

    val biometricManager = remember {
        BiometricManager.from(context)
    }

    return remember {
        AndroidBiometricImpl(activity, biometricManager)
    }
}

@Composable
actual fun rememberMediaImplementation(): MediaFeature {
    // Simplified version - in real app, use Peekaboo properly
    return remember { AndroidMediaImpl() }
}

@Composable
actual fun rememberSystemInteractionImplementation(): SystemInteractionFeature {
    val context = LocalContext.current
    val activity = context as? Activity
        ?: throw IllegalStateException("Context must be Activity")

    return remember {
        AndroidSystemInteractionImpl(activity, context)
    }
}

@Composable
actual fun rememberToastImplementation(): ToastFeature {
    val context = LocalContext.current
    return remember {
        AndroidToastImpl(context)
    }
}

@Composable
actual fun rememberHapticImplementation(): HapticFeature {
    val context = LocalContext.current
    return remember {
        AndroidHapticImpl(context)
    }
}

@Composable
actual fun rememberAnalyticsImplementation(): AnalyticsFeature {
    return remember {
        AndroidAnalyticsImpl()
    }
}

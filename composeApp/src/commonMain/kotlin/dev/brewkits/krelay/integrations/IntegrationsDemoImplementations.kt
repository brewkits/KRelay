package dev.brewkits.krelay.integrations

import dev.brewkits.krelay.samples.*

/**
 * Mock implementations for Integrations Demo.
 *
 * These demonstrate how to integrate KRelay with:
 * 1. Permission management (Moko Permissions pattern)
 * 2. Biometric auth (Moko Biometry pattern)
 * 3. System interactions (In-App Review, Updates)
 * 4. Media picking (Peekaboo pattern)
 *
 * In a real app, these would use actual platform libraries.
 */

/**
 * Mock Permission implementation simulating Moko Permissions.
 *
 * Real implementation would use:
 * - Android: ActivityCompat.requestPermissions / Moko PermissionsController
 * - iOS: AVCaptureDevice.requestAccess / Moko PermissionsController
 */
class MockPermissionImpl : PermissionFeature {
    override fun requestCamera(callback: (Boolean) -> Unit) {
        println("\n📸 [PermissionFeature] KRelay dispatched requestCamera()")
        println("   ┌─ Platform: Mock (simulating Moko Permissions)")
        println("   ├─ Permission: CAMERA")
        println("   ├─ In real app: Uses PermissionsController.providePermission()")
        println("   │")
        println("   │  Real Android implementation:")
        println("   │  ────────────────────────────")
        println("   │  class MokoPermissionImpl(")
        println("   │      private val controller: PermissionsController")
        println("   │  ) : PermissionFeature {")
        println("   │      override fun requestCamera(callback: (Boolean) -> Unit) {")
        println("   │          MainScope().launch {")
        println("   │              try {")
        println("   │                  controller.providePermission(Permission.CAMERA)")
        println("   │                  callback(true)")
        println("   │              } catch (e: Exception) {")
        println("   │                  callback(false)")
        println("   │              }")
        println("   │          }")
        println("   │      }")
        println("   │  }")
        println("   │")
        println("   └─ Simulating user GRANTED permission")
        println("   ✓ Callback invoked with: true\n")

        // Simulate user granting permission
        callback(true)
    }

    override fun requestLocation(callback: (Boolean) -> Unit) {
        println("\n📍 [PermissionFeature] requestLocation() - Simulating GRANTED")
        callback(true)
    }

    override fun requestMicrophone(callback: (Boolean) -> Unit) {
        println("\n🎤 [PermissionFeature] requestMicrophone() - Simulating GRANTED")
        callback(true)
    }

    override fun requestStorage(callback: (Boolean) -> Unit) {
        println("\n💾 [PermissionFeature] requestStorage() - Simulating GRANTED")
        callback(true)
    }

    override fun isCameraGranted(): Boolean = true
    override fun isLocationGranted(): Boolean = true
}

/**
 * Mock Biometric implementation simulating Moko Biometry.
 *
 * Real implementation would use:
 * - Android: BiometricPrompt (AndroidX)
 * - iOS: LocalAuthentication framework
 * - KMP: Moko Biometry BiometryManager
 */
class MockBiometricImpl : BiometricFeature {
    override fun isAvailable(): Boolean = true

    override fun authenticate(
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        println("\n🔐 [BiometricFeature] KRelay dispatched authenticate()")
        println("   ┌─ Platform: Mock (simulating Moko Biometry)")
        println("   ├─ Title: \"$title\"")
        println("   ├─ Subtitle: \"$subtitle\"")
        println("   ├─ In real app: Uses BiometryManager or BiometricPrompt")
        println("   │")
        println("   │  Real implementation with Moko Biometry:")
        println("   │  ─────────────────────────────────────────")
        println("   │  class BiometricImpl(")
        println("   │      private val manager: BiometryManager")
        println("   │  ) : BiometricFeature {")
        println("   │      override fun authenticate(...) {")
        println("   │          MainScope().launch {")
        println("   │              try {")
        println("   │                  val available = manager.checkBiometry()")
        println("   │                  if (available && manager.resolveBiometry(title)) {")
        println("   │                      onSuccess()")
        println("   │                  } else {")
        println("   │                      onError(\"Biometry not available\")")
        println("   │                  }")
        println("   │              } catch (e: Exception) {")
        println("   │                  onError(e.message ?: \"Unknown error\")")
        println("   │              }")
        println("   │          }")
        println("   │      }")
        println("   │  }")
        println("   │")
        println("   └─ Simulating SUCCESS authentication")
        println("   ✓ User authenticated via FaceID/TouchID/Fingerprint\n")

        // Simulate successful authentication
        onSuccess()
    }

    override fun authenticateSimple(title: String, callback: (Boolean) -> Unit) {
        println("\n🔐 [BiometricFeature] authenticateSimple() - Simulating SUCCESS")
        callback(true)
    }
}

/**
 * Mock System Interaction implementation.
 *
 * Real implementation would use:
 * - Android: Play Core Library (ReviewManager, AppUpdateManager)
 * - iOS: StoreKit (SKStoreReviewController)
 */
class MockSystemInteractionImpl : SystemInteractionFeature {
    override fun requestInAppReview() {
        println("\n⭐ [SystemInteractionFeature] KRelay dispatched requestInAppReview()")
        println("   ┌─ Platform: Mock (simulating Play Core / StoreKit)")
        println("   ├─ Action: Request 5-star rating dialog")
        println("   ├─ In real app: Shows native review dialog")
        println("   │")
        println("   │  Real Android implementation (Play Core):")
        println("   │  ─────────────────────────────────────────")
        println("   │  class AndroidSystemInteraction(")
        println("   │      private val activity: Activity")
        println("   │  ) : SystemInteractionFeature {")
        println("   │      override fun requestInAppReview() {")
        println("   │          val manager = ReviewManagerFactory.create(activity)")
        println("   │          val request = manager.requestReviewFlow()")
        println("   │          request.addOnCompleteListener { task ->")
        println("   │              if (task.isSuccessful) {")
        println("   │                  manager.launchReviewFlow(activity, task.result)")
        println("   │              }")
        println("   │          }")
        println("   │      }")
        println("   │  }")
        println("   │")
        println("   │  Real iOS implementation (StoreKit):")
        println("   │  ──────────────────────────────────")
        println("   │  class IOSSystemInteraction : SystemInteractionFeature {")
        println("   │      override fun requestInAppReview() {")
        println("   │          SKStoreReviewController.requestReview()")
        println("   │      }")
        println("   │  }")
        println("   │")
        println("   └─ Dialog would appear asking user to rate app")
        println("   ✓ In-app review dialog would be shown\n")
    }

    override fun checkForAppUpdates(callback: (Boolean) -> Unit) {
        println("\n🔄 [SystemInteractionFeature] checkForAppUpdates()")
        println("   → Simulating: No update available")
        callback(false)
    }

    override fun promptUpdate(flexible: Boolean, callback: (Boolean) -> Unit) {
        println("\n🔄 [SystemInteractionFeature] promptUpdate(flexible=$flexible)")
        callback(false)
    }

    override fun openAppSettings() {
        println("\n⚙️ [SystemInteractionFeature] openAppSettings()")
        println("   → Would open: System Settings > App > Permissions")
    }

    override fun openAppStorePage(forReview: Boolean) {
        println("\n🏪 [SystemInteractionFeature] openAppStorePage(forReview=$forReview)")
        println("   → Would open: Google Play / App Store page")
    }

    override fun shareApp() {
        println("\n📤 [SystemInteractionFeature] shareApp()")
        println("   → Would show: Native share sheet with app link")
    }
}

/**
 * Mock Media implementation simulating Peekaboo.
 *
 * Real implementation would use:
 * - Peekaboo library: rememberImagePickerLauncher
 * - Android: ActivityResultContracts.PickVisualMedia
 * - iOS: UIImagePickerController / PHPickerViewController
 */
class MockMediaImpl : MediaFeature {
    override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
        println("\n🖼️ [MediaFeature] KRelay dispatched pickImageFromGallery()")
        println("   ┌─ Platform: Mock (simulating Peekaboo)")
        println("   ├─ Action: Open photo gallery picker")
        println("   ├─ In real app: Uses Peekaboo ImagePicker or platform picker")
        println("   │")
        println("   │  Real implementation with Peekaboo:")
        println("   │  ────────────────────────────────────")
        println("   │  // In UI layer (Compose)")
        println("   │  val singleImagePicker = rememberImagePickerLauncher(")
        println("   │      selectionMode = SelectionMode.Single,")
        println("   │      scope = scope,")
        println("   │      onResult = { images ->")
        println("   │          val imageData = images.firstOrNull()?.toByteArray()")
        println("   │          currentCallback?.invoke(imageData)")
        println("   │      }")
        println("   │  )")
        println("   │")
        println("   │  LaunchedEffect(Unit) {")
        println("   │      KRelay.register(object : MediaFeature {")
        println("   │          override fun pickImageFromGallery(callback) {")
        println("   │              currentCallback = callback")
        println("   │              singleImagePicker.launch()")
        println("   │          }")
        println("   │      })")
        println("   │  }")
        println("   │")
        println("   └─ Simulating user picked image (1024 bytes)")
        println("   ✓ Callback invoked with mock image data\n")

        // Simulate user picking an image
        val mockImageData = ByteArray(1024) { it.toByte() } // Fake 1KB image
        callback(mockImageData)
    }

    override fun pickMultipleImages(maxCount: Int, callback: (List<ByteArray>) -> Unit) {
        println("\n🖼️ [MediaFeature] pickMultipleImages(maxCount=$maxCount)")
        println("   → Simulating: User picked 3 images")

        val mockImages = List(3) { ByteArray(1024) { it.toByte() } }
        callback(mockImages)
    }

    override fun capturePhoto(callback: (ByteArray?) -> Unit) {
        println("\n📷 [MediaFeature] KRelay dispatched capturePhoto()")
        println("   ┌─ Platform: Mock (simulating Camera)")
        println("   ├─ Action: Open camera to take photo")
        println("   ├─ In real app: Uses platform camera API")
        println("   └─ Simulating: User took photo (2048 bytes)")
        println("   ✓ Callback invoked with mock photo data\n")

        val mockPhotoData = ByteArray(2048) { it.toByte() } // Fake 2KB photo
        callback(mockPhotoData)
    }

    override fun pickVideo(callback: (ByteArray?) -> Unit) {
        println("\n🎥 [MediaFeature] pickVideo()")
        callback(ByteArray(4096) { it.toByte() })
    }

    override fun recordVideo(maxDurationSeconds: Int, callback: (ByteArray?) -> Unit) {
        println("\n🎥 [MediaFeature] recordVideo(maxDuration=${maxDurationSeconds}s)")
        callback(ByteArray(4096) { it.toByte() })
    }

    override fun isCameraAvailable(): Boolean = true
    override fun isGalleryAvailable(): Boolean = true
}

/**
 * Mock implementations for supporting features (Toast, Haptic, etc.)
 */
class MockToastImpl : ToastFeature {
    override fun showShort(message: String) {
        println("🍞 [Toast] $message")
    }

    override fun showLong(message: String) {
        println("🍞 [Toast] $message (long)")
    }
}

class MockHapticImpl : HapticFeature {
    override fun vibrate(durationMs: Long) {
        println("📳 [Haptic] Vibrate: ${durationMs}ms")
    }

    override fun impact(style: HapticStyle) {
        println("📳 [Haptic] Impact: $style")
    }

    override fun success() {
        println("📳 [Haptic] Success feedback")
    }

    override fun error() {
        println("📳 [Haptic] Error feedback")
    }

    override fun warning() {
        println("📳 [Haptic] Warning feedback")
    }

    override fun selection() {
        println("📳 [Haptic] Selection feedback")
    }
}

class MockNavigationImpl : NavigationFeature {
    override fun navigateTo(route: String, params: Map<String, String>) {
        println("🧭 [Navigation] Navigate to: $route")
    }

    override fun navigateBack() {
        println("🧭 [Navigation] Navigate back")
    }

    override fun navigateToRoot() {
        println("🧭 [Navigation] Navigate to root")
    }
}

class MockAnalyticsImpl : AnalyticsFeature {
    override fun track(eventName: String) {
        println("📊 [Analytics] Track: $eventName")
    }

    override fun track(eventName: String, parameters: Map<String, Any>) {
        println("📊 [Analytics] Track: $eventName with ${parameters.size} params")
    }

    override fun setUserProperty(key: String, value: String) {
        println("📊 [Analytics] Set property: $key = $value")
    }

    override fun setUserId(userId: String) {
        println("📊 [Analytics] Set user ID: $userId")
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        println("📊 [Analytics] Track screen: $screenName")
    }
}

# KRelay Integration Best Practices

## Overview

KRelay is **"The Glue Code Standard"** for Kotlin Multiplatform. It bridges the gap between your shared business logic and platform-specific implementations, allowing you to use specialized libraries without coupling your ViewModels to them.

This document provides best practices for integrating KRelay with popular KMP libraries.

---

## Core Principle: Separation of Concerns

**The Golden Rule:**
- **ViewModel decides WHEN** (business logic)
- **UI/Platform decides HOW** (implementation)
- **KRelay connects them** (glue code)

```kotlin
// ❌ BAD: ViewModel tightly coupled to platform library
class BadViewModel(private val biometryManager: BiometryManager) {
    fun authenticate() {
        // Problem: ViewModel holds platform reference
        // - Memory leak risk
        // - Hard to test
        // - Can't use in common code easily
        MainScope().launch {
            biometryManager.authenticate(...)
        }
    }
}

// ✅ GOOD: ViewModel uses KRelay
class GoodViewModel {
    fun authenticate() {
        // ViewModel just dispatches - no platform coupling!
        KRelay.dispatch<BiometricFeature> {
            it.authenticate(
                title = "Verify Identity",
                onSuccess = { handleSuccess() }
            )
        }
    }
}
```

---

## Integration 1: Navigation Libraries

### Voyager / Decompose / Appyx

**Problem:** Navigation libraries require Navigator/Router references that are created at UI level.

**Solution:** Define navigation interface, dispatch from ViewModel, implement in UI.

### Example: Voyager Integration

#### Step 1: Define Navigation Interface (Common)

```kotlin
interface NavigationFeature : RelayFeature {
    fun navigateTo(screen: Screen)
    fun navigateBack()
    fun navigateToRoot()
}
```

#### Step 2: Implement in UI Layer

```kotlin
@Composable
fun MyScreen() {
    val navigator = LocalNavigator.currentOrThrow

    // Register KRelay implementation
    LaunchedEffect(Unit) {
        KRelay.register(object : NavigationFeature {
            override fun navigateTo(screen: Screen) {
                navigator.push(screen)
            }
            override fun navigateBack() {
                navigator.pop()
            }
            override fun navigateToRoot() {
                navigator.popAll()
            }
        })
    }

    // ... rest of UI
}
```

#### Step 3: Use in ViewModel (Clean!)

```kotlin
class MyViewModel {
    fun onLoginSuccess() {
        // Business logic
        saveUserSession()
        trackAnalytics()

        // Navigate - no Voyager dependency!
        KRelay.dispatch<NavigationFeature> {
            it.navigateTo(HomeScreen)
        }
    }
}
```

### Benefits
✅ ViewModel has ZERO Voyager dependencies
✅ Easy to test (mock NavigationFeature)
✅ Easy to switch navigation libraries
✅ No memory leaks from holding Navigator references

---

## Integration 2: Permission Management

### Moko Permissions

**Problem:** `PermissionsController` needs to be bound to Activity/ViewController lifecycle.

**Solution:** ViewModel requests permission via KRelay, UI implements using Moko.

### Example: Moko Permissions Integration

#### Step 1: Define Permission Interface (Common)

```kotlin
interface PermissionFeature : RelayFeature {
    fun requestCamera(onGranted: () -> Unit, onDenied: () -> Unit = {})
    fun requestLocation(callback: (Boolean) -> Unit)
    fun isCameraGranted(): Boolean
}
```

#### Step 2: Implement using Moko (Platform)

##### Android Implementation

```kotlin
class MokoPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {
    override fun requestCamera(onGranted: () -> Unit, onDenied: () -> Unit) {
        MainScope().launch {
            try {
                controller.providePermission(Permission.CAMERA)
                onGranted()
            } catch (e: Exception) {
                onDenied()
            }
        }
    }

    override fun isCameraGranted(): Boolean {
        return controller.isPermissionGranted(Permission.CAMERA)
    }
}
```

##### iOS Implementation

```kotlin
class MokoPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {
    // Same implementation - Moko abstracts platform differences!
    override fun requestCamera(onGranted: () -> Unit, onDenied: () -> Unit) {
        MainScope().launch {
            try {
                controller.providePermission(Permission.CAMERA)
                onGranted()
            } catch (e: Exception) {
                onDenied()
            }
        }
    }
}
```

#### Step 3: Register in UI (Activity/ViewController)

```kotlin
// Android Activity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsController = PermissionsController(
            applicationContext = applicationContext
        ).apply {
            bind(lifecycle, supportFragmentManager)
        }

        // Register with KRelay
        KRelay.register<PermissionFeature>(
            MokoPermissionImpl(permissionsController)
        )

        setContent {
            MyApp()
        }
    }
}
```

#### Step 4: Use in ViewModel (Clean!)

```kotlin
class CameraViewModel {
    fun onTakePhotoClicked() {
        KRelay.dispatch<PermissionFeature> {
            if (it.isCameraGranted()) {
                // Already granted
                openCamera()
            } else {
                // Request permission
                it.requestCamera(
                    onGranted = { openCamera() },
                    onDenied = { showPermissionError() }
                )
            }
        }
    }

    private fun openCamera() {
        // Open camera logic
    }
}
```

### Benefits
✅ ViewModel doesn't hold `PermissionsController` reference
✅ No memory leaks from Activity/Fragment references
✅ Easy to test (mock permission responses)
✅ Works seamlessly with Moko's lifecycle binding

---

## Integration 3: Biometric Authentication

### Moko Biometry / BiometricPrompt

**Problem:** Biometric authentication requires Activity context and shows UI dialogs.

**Solution:** ViewModel triggers authentication, UI implements using platform biometric APIs.

### Example: Moko Biometry Integration

#### Step 1: Define Biometric Interface (Common)

```kotlin
interface BiometricFeature : RelayFeature {
    fun isAvailable(): Boolean
    fun authenticate(
        title: String,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    )
}
```

#### Step 2: Implement using Moko Biometry (Platform)

```kotlin
class MokoBiometricImpl(
    private val biometryManager: BiometryManager
) : BiometricFeature {

    override fun isAvailable(): Boolean {
        return biometryManager.isBiometryAvailable()
    }

    override fun authenticate(
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        MainScope().launch {
            try {
                val isAvailable = biometryManager.checkBiometryAuthentication()
                if (!isAvailable) {
                    onError("Biometry not available")
                    return@launch
                }

                val result = biometryManager.requestBiometryAuthentication(
                    requestTitle = title,
                    requestReason = subtitle ?: ""
                )

                if (result) {
                    onSuccess()
                } else {
                    onError("Authentication failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
```

#### Step 3: Register in UI

```kotlin
@Composable
fun App() {
    // Create BiometryManager (lifecycle-aware)
    val biometryManager = remember {
        BiometryManagerFactory().createBiometryManager()
    }

    // Register with KRelay
    LaunchedEffect(Unit) {
        KRelay.register<BiometricFeature>(
            MokoBiometricImpl(biometryManager)
        )
    }

    // ... app content
}
```

#### Step 4: Use in ViewModel (Clean!)

```kotlin
class PaymentViewModel {
    fun confirmPayment(amount: Double) {
        KRelay.dispatch<BiometricFeature> { biometric ->
            if (!biometric.isAvailable()) {
                // Fallback to PIN
                showPinDialog()
                return@dispatch
            }

            biometric.authenticate(
                title = "Confirm Payment",
                subtitle = "Authorize payment of \$$amount",
                onSuccess = {
                    processPayment(amount)
                    KRelay.dispatch<HapticFeature> { it.success() }
                },
                onError = { error ->
                    showError("Authentication failed: $error")
                }
            )
        }
    }
}
```

### Benefits
✅ ViewModel doesn't hold BiometryManager reference
✅ No lifecycle coupling
✅ Business logic (when to authenticate) separated from UI (how to authenticate)
✅ Easy to test without showing actual biometric dialogs

---

## Integration 4: In-App Review & Updates

### Google Play Core / StoreKit

**Problem:** Review/Update APIs require Activity/ViewController context.

**Solution:** ViewModel decides when to request review, platform shows native dialog.

### Example: In-App Review Integration

#### Step 1: Define System Interaction Interface (Common)

```kotlin
interface SystemInteractionFeature : RelayFeature {
    fun requestInAppReview()
    fun checkForAppUpdates(callback: (Boolean) -> Unit)
    fun openAppSettings()
}
```

#### Step 2: Implement for Android (Play Core)

```kotlin
class AndroidSystemInteraction(
    private val activity: Activity
) : SystemInteractionFeature {

    override fun requestInAppReview() {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }

    override fun checkForAppUpdates(callback: (Boolean) -> Unit) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val updateAvailable = appUpdateInfo.updateAvailability() ==
                UpdateAvailability.UPDATE_AVAILABLE
            callback(updateAvailable)
        }
    }
}
```

#### Step 3: Implement for iOS (StoreKit)

```kotlin
class IOSSystemInteraction : SystemInteractionFeature {
    override fun requestInAppReview() {
        SKStoreReviewController.requestReview()
    }

    override fun checkForAppUpdates(callback: (Boolean) -> Unit) {
        // iOS doesn't have API to check updates programmatically
        callback(false)
    }
}
```

#### Step 4: Use in ViewModel (Clean!)

```kotlin
class OrderViewModel {
    fun onOrderCompleted(orderId: String, amount: Double) {
        // Business logic
        saveOrder(orderId)

        // Show success feedback
        KRelay.dispatch<ToastFeature> {
            it.showLong("Order #$orderId confirmed!")
        }

        KRelay.dispatch<HapticFeature> {
            it.success()
        }

        // User is happy - good time to request review!
        KRelay.dispatch<SystemInteractionFeature> {
            it.requestInAppReview()
        }

        // Track analytics
        KRelay.dispatch<AnalyticsFeature> {
            it.track("order_completed", mapOf(
                "order_id" to orderId,
                "amount" to amount
            ))
        }
    }
}
```

### Best Practices for In-App Review

**DO:**
- ✅ Request review after POSITIVE user experiences (completed order, achievement unlocked)
- ✅ Use "fire-and-forget" pattern (don't wait for result)
- ✅ Let the OS decide if/when to show the dialog
- ✅ Track when you requested review (don't spam)

**DON'T:**
- ❌ Request review on app launch
- ❌ Request review after errors or negative experiences
- ❌ Tie review to a button (violates App Store guidelines)
- ❌ Request review too frequently (OS will throttle)

### Smart Review Request Pattern

```kotlin
class SmartReviewViewModel {
    private var positiveActionsCount = 0
    private var lastReviewRequestTime = 0L

    fun onPositiveAction() {
        positiveActionsCount++

        // Only request review if:
        // 1. User completed 5+ positive actions (engaged user)
        // 2. Haven't requested review in last 60 days
        val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        if (positiveActionsCount >= 5 &&
            (now - lastReviewRequestTime) > sixtyDaysMs) {

            KRelay.dispatch<SystemInteractionFeature> {
                it.requestInAppReview()
            }

            lastReviewRequestTime = now
            positiveActionsCount = 0
        }
    }
}
```

---

## Integration 5: Media Picking

### Peekaboo / Compose ImagePicker

**Problem:** `rememberImagePickerLauncher` is a Compose function, can't be called from ViewModel.

**Solution:** ViewModel triggers picker, UI implements using Peekaboo launcher.

### Example: Peekaboo Integration

#### Step 1: Define Media Interface (Common)

```kotlin
interface MediaFeature : RelayFeature {
    fun pickImageFromGallery(callback: (ByteArray?) -> Unit)
    fun capturePhoto(callback: (ByteArray?) -> Unit)
    fun pickMultipleImages(maxCount: Int = 5, callback: (List<ByteArray>) -> Unit)
}
```

#### Step 2: Implement using Peekaboo (UI Layer)

```kotlin
@Composable
fun RegisterMediaPicker() {
    // Peekaboo's image picker launcher
    val singleImagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = rememberCoroutineScope(),
        onResult = { images ->
            val imageData = images.firstOrNull()?.toByteArray()
            currentCallback?.invoke(imageData)
        }
    )

    val multipleImagesPicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Multiple(maxSelection = 10),
        scope = rememberCoroutineScope(),
        onResult = { images ->
            val imagesData = images.map { it.toByteArray() }
            currentMultipleCallback?.invoke(imagesData)
        }
    )

    // Register KRelay implementation
    LaunchedEffect(Unit) {
        KRelay.register(object : MediaFeature {
            override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
                currentCallback = callback
                singleImagePicker.launch()
            }

            override fun capturePhoto(callback: (ByteArray?) -> Unit) {
                currentCallback = callback
                cameraPicker.launch()
            }

            override fun pickMultipleImages(
                maxCount: Int,
                callback: (List<ByteArray>) -> Unit
            ) {
                currentMultipleCallback = callback
                multipleImagesPicker.launch()
            }
        })
    }
}

// Helper to hold callbacks (in real app, use proper state management)
private var currentCallback: ((ByteArray?) -> Unit)? = null
private var currentMultipleCallback: ((List<ByteArray>) -> Unit)? = null
```

#### Step 3: Use in ViewModel (Clean!)

```kotlin
class ProfileViewModel {
    fun updateAvatar() {
        // ViewModel just dispatches - no Peekaboo coupling!
        KRelay.dispatch<MediaFeature> {
            it.pickImageFromGallery { imageData ->
                if (imageData != null) {
                    uploadAvatar(imageData)
                } else {
                    // User cancelled
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Avatar update cancelled")
                    }
                }
            }
        }
    }

    fun captureProfilePhoto() {
        // First check permission, then capture
        KRelay.dispatch<PermissionFeature> { permission ->
            permission.requestCamera(
                onGranted = {
                    KRelay.dispatch<MediaFeature> { media ->
                        media.capturePhoto { imageData ->
                            if (imageData != null) {
                                uploadAvatar(imageData)
                            }
                        }
                    }
                }
            )
        }
    }
}
```

### Benefits
✅ ViewModel doesn't call Compose functions
✅ No coupling to Peekaboo library
✅ Easy to test (mock image selection)
✅ Can switch image picker libraries easily

---

## Testing Best Practices

### Unit Testing ViewModels

KRelay makes ViewModels extremely easy to test because they have no platform dependencies.

```kotlin
class ProfileViewModelTest {

    @Test
    fun `updateAvatar should upload image when user picks one`() {
        // Arrange
        val viewModel = ProfileViewModel()
        var uploadedImage: ByteArray? = null

        // Mock MediaFeature
        KRelay.register(object : MediaFeature {
            override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
                // Simulate user picking image
                callback(ByteArray(1024) { it.toByte() })
            }
        })

        // Mock upload
        viewModel.onImageUploaded = { image ->
            uploadedImage = image
        }

        // Act
        viewModel.updateAvatar()

        // Assert
        assertNotNull(uploadedImage)
        assertEquals(1024, uploadedImage?.size)
    }

    @Test
    fun `updateAvatar should handle cancellation gracefully`() {
        // Arrange
        val viewModel = ProfileViewModel()

        // Mock user cancelling
        KRelay.register(object : MediaFeature {
            override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
                callback(null) // User cancelled
            }
        })

        // Act
        viewModel.updateAvatar()

        // Assert
        // Should not crash, should show cancellation message
        // (verify via ToastFeature mock)
    }
}
```

---

## Common Patterns

### Pattern 1: Fire-and-Forget

Use for one-way actions with no response needed.

```kotlin
fun onUserAction() {
    // Fire-and-forget
    KRelay.dispatch<AnalyticsFeature> {
        it.track("button_clicked")
    }

    KRelay.dispatch<HapticFeature> {
        it.impact(HapticStyle.LIGHT)
    }
}
```

### Pattern 2: Callback Pattern

Use when you need a response from the platform.

```kotlin
fun requestPermissionAndOpenCamera() {
    KRelay.dispatch<PermissionFeature> { permission ->
        permission.requestCamera { granted ->
            if (granted) {
                openCamera()
            } else {
                showPermissionDenied()
            }
        }
    }
}
```

### Pattern 3: Chaining Multiple Features

Coordinate multiple platform features in a workflow.

```kotlin
fun completeOnboarding() {
    // Step 1: Request permission
    KRelay.dispatch<PermissionFeature> { permission ->
        permission.requestCamera { cameraGranted ->

            // Step 2: Pick profile picture
            KRelay.dispatch<MediaFeature> { media ->
                media.pickImageFromGallery { imageData ->
                    if (imageData != null) {

                        // Step 3: Setup biometrics
                        KRelay.dispatch<BiometricFeature> { biometric ->
                            if (biometric.isAvailable()) {
                                biometric.authenticateSimple("Setup") { success ->

                                    // Step 4: Navigate to home
                                    KRelay.dispatch<NavigationFeature> { nav ->
                                        nav.navigateTo(HomeScreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### Pattern 4: Conditional Feature Usage

Check availability before using feature.

```kotlin
fun authenticateUser() {
    KRelay.dispatch<BiometricFeature> { biometric ->
        if (biometric.isAvailable()) {
            // Device supports biometrics
            biometric.authenticate(...)
        } else {
            // Fallback to PIN/Password
            showPinDialog()
        }
    }
}
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Shared Code (Common)                      │
│                                                               │
│  ┌─────────────────┐         ┌──────────────────────┐       │
│  │  ViewModel      │         │  RelayFeature        │       │
│  │  (Business      │────────▶│  Interfaces          │       │
│  │   Logic)        │         │  (Contracts)         │       │
│  └─────────────────┘         └──────────────────────┘       │
│         │                              ▲                     │
│         │                              │                     │
│         ▼                              │                     │
│   KRelay.dispatch<Feature>()          │                     │
└─────────────────────────────────────────────────────────────┘
                                         │
         ┌───────────────────────────────┴──────────────┐
         │                                               │
         ▼                                               ▼
┌─────────────────────┐                   ┌──────────────────────┐
│   Android Platform  │                   │    iOS Platform      │
│                     │                   │                      │
│  ┌──────────────┐   │                   │  ┌────────────────┐  │
│  │ Moko Perms   │   │                   │  │ Moko Perms     │  │
│  │ Controller   │   │                   │  │ Controller     │  │
│  └──────────────┘   │                   │  └────────────────┘  │
│  ┌──────────────┐   │                   │  ┌────────────────┐  │
│  │ BiometricPr  │   │                   │  │ LocalAuth      │  │
│  │ ompt API     │   │                   │  │ Framework      │  │
│  └──────────────┘   │                   │  └────────────────┘  │
│  ┌──────────────┐   │                   │  ┌────────────────┐  │
│  │ Play Core    │   │                   │  │ StoreKit       │  │
│  │ Library      │   │                   │  │                │  │
│  └──────────────┘   │                   │  └────────────────┘  │
└─────────────────────┘                   └──────────────────────┘
```

---

## Summary

### Why Use KRelay with Third-Party Libraries?

1. **Clean Architecture** - ViewModels stay pure business logic
2. **No Memory Leaks** - No Activity/Context/Controller references in ViewModels
3. **Easy Testing** - Mock platform features without complex setup
4. **Platform Flexibility** - Swap implementations without changing ViewModels
5. **KMP-First** - Designed for Kotlin Multiplatform from the ground up

### The KRelay Promise

> **"KRelay doesn't replace your favorite libraries. KRelay is the remote control that lets your ViewModel use them safely."**

- **Not a replacement** for Voyager, Moko, Peekaboo, etc.
- **Not adding complexity** - removing coupling
- **Not reinventing wheels** - connecting existing wheels

### When to Use KRelay

✅ When ViewModel needs to trigger platform-specific actions
✅ When library requires Activity/Context/lifecycle
✅ When you want clean, testable ViewModels
✅ When building KMP apps with platform integrations

### When NOT to Use KRelay

❌ For simple data models/DTOs
❌ For pure business logic (use directly)
❌ When platform libraries don't require special integration

---

## Demo App

The KRelay demo app includes **REAL implementations** of all these integrations!

### Running the Demo

1. Open the demo app
2. Navigate to "Library Integrations"
3. Toggle between **REAL** and **MOCK** implementations
4. Try the demo buttons to see actual integrations in action

### Real Libraries Used

- ✅ **Moko Permissions** (0.18.0) - Permission management
- ✅ **Moko Biometry** (0.4.0) - Biometric authentication
- ✅ **Peekaboo** (0.5.2) - Image/camera picking
- ✅ **Play Core Review** (2.0.1) - In-app review (Android)
- ✅ **StoreKit** - In-app review (iOS)
- ✅ **AndroidX Biometric** (1.1.0) - Alternative biometric implementation

### What You'll See

**REAL Mode:**
- Actual permission dialogs from the OS
- Real biometric authentication (FaceID/TouchID/Fingerprint)
- Native in-app review dialogs
- Real image pickers from Peekaboo

**MOCK Mode:**
- Console logs showing what would happen
- Detailed implementation code examples
- No actual platform dialogs (for testing)

### Code Location

- Real Android implementations: `composeApp/src/androidMain/.../AndroidIntegrations.kt`
- Real iOS implementations: `composeApp/src/iosMain/.../IOSIntegrations.kt`
- Common setup: `composeApp/src/commonMain/.../RealIntegrations.kt`

---

## Further Reading

- [KRelay README](../README.md)
- [Sample Code](../krelay/src/commonMain/kotlin/dev/brewkits/krelay/samples/)
- [Demo App](../composeApp/)
- [Integration Tests](../krelay/src/commonTest/kotlin/dev/brewkits/krelay/)

---

**Happy Integrating! 🚀**

If you have questions or want to share your integration patterns, please open an issue or discussion on GitHub.

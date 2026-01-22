# KRelay Usage Guide

Complete guide to using KRelay in your Kotlin Multiplatform project with practical examples covering all recommended use cases.

---

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Use Case 1: Toast/Snackbar Messages](#use-case-1-toastsnackbar-messages)
4. [Use Case 2: Navigation Commands](#use-case-2-navigation-commands)
5. [Use Case 3: Permission Requests](#use-case-3-permission-requests)
6. [Use Case 4: Haptic Feedback](#use-case-4-haptic-feedback)
7. [Use Case 5: Simple Analytics](#use-case-5-simple-analytics)
8. [Use Case 6: In-App Notifications](#use-case-6-in-app-notifications)
9. [Complex Workflows](#complex-workflows)
10. [Testing with KRelay](#testing-with-krelay)
11. [Common Patterns](#common-patterns)
12. [Troubleshooting](#troubleshooting)

---

## Installation

### Step 1: Add KRelay Module

```kotlin
// In your shared module's build.gradle.kts
commonMain.dependencies {
    implementation(project(":krelay"))
}
```

### Step 2: Sync Project

```bash
./gradlew sync
```

---

## Quick Start

### Three Steps to Use KRelay:

#### 1. Define Feature Interface (Common Code)

```kotlin
// In commonMain
interface ToastFeature : RelayFeature {
    fun show(message: String)
}
```

#### 2. Use from Shared Code

```kotlin
// In ViewModel, UseCase, Repository, etc.
class MyViewModel {
    fun loadData() {
        KRelay.dispatch<ToastFeature> {
            it.show("Data loaded!")
        }
    }
}
```

#### 3. Implement on Platform

**Android:**
```kotlin
// In androidMain
class AndroidToast(private val context: Context) : ToastFeature {
    override fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// In Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KRelay.register<ToastFeature>(AndroidToast(applicationContext))
    }
}
```

**iOS:**
```swift
// In Swift
class IOSToast: ToastFeature {
    weak var viewController: UIViewController?

    func show(message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        viewController?.present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            alert.dismiss(animated: true)
        }
    }
}

// In SwiftUI
struct ContentView: View {
    var body: some View {
        ComposeView()
            .onAppear {
                let toast = IOSToast(viewController: getViewController())
                KRelay.shared.register(impl: toast)
            }
    }
}
```

---

## Use Case 1: Toast/Snackbar Messages

**Perfect for:** User feedback, operation results, error messages

### Basic Toast

```kotlin
// Show simple message
fun onDataLoaded() {
    KRelay.dispatch<ToastFeature> {
        it.show("Data loaded successfully!")
    }
}
```

### Error Toast

```kotlin
// Show error from try-catch
suspend fun loadUserData() {
    try {
        val user = api.getUser()
        // Process user data
    } catch (e: Exception) {
        KRelay.dispatch<ToastFeature> {
            it.show("Error: ${e.message}")
        }
    }
}
```

### Background Operation Feedback

```kotlin
// From background coroutine
viewModelScope.launch(Dispatchers.IO) {
    val items = repository.fetchItems()

    // This toast shows on main thread automatically
    KRelay.dispatch<ToastFeature> {
        it.show("Loaded ${items.size} items")
    }
}
```

### Platform Implementation

**Android:**
```kotlin
class AndroidToast(private val context: Context) : ToastFeature {
    override fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
```

**Registration:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRelay.register<ToastFeature>(AndroidToast(applicationContext))
}
```

---

## Use Case 2: Navigation Commands

**Perfect for:** Post-operation navigation, workflow completion, deep linking

### Navigate to Screen

```kotlin
fun onLoginSuccess() {
    KRelay.dispatch<NavigationFeature> {
        it.navigateTo("home")
    }
}
```

### Navigate with Parameters

```kotlin
fun openItemDetails(itemId: String) {
    KRelay.dispatch<NavigationFeature> {
        it.navigateTo("details/$itemId")
    }
}
```

### Navigate Back

```kotlin
fun onCancelOperation() {
    KRelay.dispatch<NavigationFeature> {
        it.goBack()
    }
}
```

### Combined: Feedback + Navigation

```kotlin
fun onOrderComplete(orderId: String) {
    // Show toast
    KRelay.dispatch<ToastFeature> {
        it.show("Order #$orderId placed!")
    }

    // Navigate to confirmation
    KRelay.dispatch<NavigationFeature> {
        it.navigateTo("order-confirmation/$orderId")
    }
}
```

### Platform Implementation

**Android (Compose Navigation):**
```kotlin
class AndroidNavigation(
    private val navController: NavHostController
) : NavigationFeature {
    override fun navigateTo(route: String) {
        navController.navigate(route)
    }

    override fun goBack() {
        navController.popBackStack()
    }
}
```

**Registration:**
```kotlin
@Composable
fun App() {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        KRelay.register<NavigationFeature>(AndroidNavigation(navController))
    }

    NavHost(navController, startDestination = "home") {
        // Your navigation graph
    }
}
```

---

## Use Case 3: Permission Requests

**Perfect for:** Camera, location, microphone, storage access

### Request Camera Permission

```kotlin
fun takePicture() {
    KRelay.dispatch<PermissionFeature> {
        it.requestCamera { granted ->
            if (granted) {
                startCamera()
            } else {
                showPermissionDenied()
            }
        }
    }
}
```

### Request Location Permission

```kotlin
fun showUserLocation() {
    KRelay.dispatch<PermissionFeature> {
        it.requestLocation { granted ->
            if (granted) {
                loadMap()
            } else {
                KRelay.dispatch<ToastFeature> {
                    it.show("Location permission required")
                }
            }
        }
    }
}
```

### Check Permission Before Use

```kotlin
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
```

### Platform Implementation

**Android:**
```kotlin
class AndroidPermission(
    private val activity: Activity
) : PermissionFeature {
    private var cameraCallback: ((Boolean) -> Unit)? = null

    override fun requestCamera(callback: (Boolean) -> Unit) {
        if (isCameraGranted()) {
            callback(true)
            return
        }

        cameraCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA
        )
    }

    override fun isCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Handle result in Activity
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            cameraCallback?.invoke(granted)
            cameraCallback = null
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}
```

**Activity Setup:**
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var permissionFeature: AndroidPermission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionFeature = AndroidPermission(this)
        KRelay.register<PermissionFeature>(permissionFeature)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionFeature.onRequestPermissionsResult(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
```

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## Use Case 4: Haptic Feedback

**Perfect for:** Button clicks, confirmations, game interactions

### Impact Feedback

```kotlin
fun onButtonPressed() {
    KRelay.dispatch<HapticFeature> {
        it.impact(HapticStyle.LIGHT)
    }
}
```

### Success/Error Feedback

```kotlin
fun onPaymentSuccess() {
    KRelay.dispatch<HapticFeature> {
        it.success()
    }
}

fun onPaymentError() {
    KRelay.dispatch<HapticFeature> {
        it.error()
    }
}
```

### Selection Feedback (Pickers)

```kotlin
fun onPickerValueChanged() {
    KRelay.dispatch<HapticFeature> {
        it.selection()
    }
}
```

### Platform Implementation

**Android:**
```kotlin
class AndroidHaptic(
    private val context: Context
) : HapticFeature {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun impact(style: HapticStyle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (style) {
                HapticStyle.LIGHT -> VibrationEffect.EFFECT_TICK
                HapticStyle.MEDIUM -> VibrationEffect.EFFECT_CLICK
                HapticStyle.HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
            }
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        }
    }

    override fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        }
    }
}
```

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## Use Case 5: Simple Analytics

**Perfect for:** Event tracking, user behavior, screen views

⚠️ **Note:** Use for non-critical analytics only. For critical events, use persistent queue solutions.

### Track Simple Event

```kotlin
fun onButtonClicked() {
    KRelay.dispatch<AnalyticsFeature> {
        it.track("button_clicked")
    }
}
```

### Track Event with Parameters

```kotlin
fun onPurchase(productId: String, price: Double) {
    KRelay.dispatch<AnalyticsFeature> {
        it.track("purchase_completed", mapOf(
            "product_id" to productId,
            "price" to price,
            "currency" to "USD"
        ))
    }
}
```

### Track Screen View

```kotlin
fun onScreenShown(screenName: String) {
    KRelay.dispatch<AnalyticsFeature> {
        it.trackScreen(screenName)
    }
}
```

### Set User Properties

```kotlin
fun onUserUpgrade(userType: String) {
    KRelay.dispatch<AnalyticsFeature> {
        it.setUserProperty("user_type", userType)
    }
}
```

### Platform Implementation

**Android (Firebase Analytics Example):**
```kotlin
class FirebaseAnalyticsFeature(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsFeature {
    override fun track(eventName: String, parameters: Map<String, Any>) {
        val bundle = Bundle()
        parameters.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    override fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        })
    }
}
```

---

## Use Case 6: In-App Notifications

**Perfect for:** Background sync updates, important alerts, banners

### Show Notification

```kotlin
fun onSyncComplete(itemsUpdated: Int) {
    KRelay.dispatch<NotificationBridge> {
        it.showInAppNotification(
            title = "Sync Complete",
            message = "$itemsUpdated items updated",
            duration = 5
        )
    }
}
```

### Important Alert

```kotlin
fun onImportantUpdate(message: String) {
    KRelay.dispatch<NotificationBridge> {
        it.showInAppNotification(
            title = "Important Update",
            message = message,
            duration = 10
        )
    }
}
```

---

## Complex Workflows

### Checkout Flow

```kotlin
fun completeCheckout(orderId: String, amount: Double) {
    // Haptic feedback
    KRelay.dispatch<HapticFeature> {
        it.success()
    }

    // Show confirmation
    KRelay.dispatch<ToastFeature> {
        it.show("Order #$orderId confirmed!")
    }

    // Track analytics
    KRelay.dispatch<AnalyticsFeature> {
        it.track("checkout_completed", mapOf(
            "order_id" to orderId,
            "amount" to amount
        ))
    }

    // Navigate
    KRelay.dispatch<NavigationFeature> {
        it.navigateTo("order-success/$orderId")
    }

    // Show notification
    KRelay.dispatch<NotificationBridge> {
        it.showInAppNotification(
            title = "Order Confirmed",
            message = "Your order is being processed"
        )
    }
}
```

### Long Operation with Progress

```kotlin
suspend fun performLongOperation() {
    // Start
    KRelay.dispatch<ToastFeature> {
        it.show("Starting operation...")
    }

    // Work
    withContext(Dispatchers.IO) {
        // Heavy processing
        delay(2000)
    }

    // Progress update
    KRelay.dispatch<HapticFeature> {
        it.impact(HapticStyle.LIGHT)
    }

    KRelay.dispatch<ToastFeature> {
        it.show("Processing...")
    }

    // More work
    withContext(Dispatchers.IO) {
        delay(2000)
    }

    // Complete
    KRelay.dispatch<HapticFeature> {
        it.success()
    }

    KRelay.dispatch<NotificationBridge> {
        it.showInAppNotification(
            title = "Complete",
            message = "Operation finished successfully"
        )
    }
}
```

---

## Testing with KRelay

### Reset Before Each Test

```kotlin
class MyViewModelTest {
    @BeforeTest
    fun setup() {
        KRelay.reset()
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }
}
```

### Test Queue Behavior

```kotlin
@Test
fun testQueueing() {
    // Dispatch before registration
    viewModel.loadData()

    // Verify queued
    assertEquals(1, KRelay.getPendingCount<ToastFeature>())

    // Register
    val mock = MockToast()
    KRelay.register<ToastFeature>(mock)

    // Verify replayed
    assertEquals(0, KRelay.getPendingCount<ToastFeature>())
}
```

### Mock Implementation

```kotlin
class MockToast : ToastFeature {
    val messages = mutableListOf<String>()

    override fun show(message: String) {
        messages.add(message)
    }
}
```

---

## Common Patterns

### Pattern 1: Error Handling

```kotlin
suspend fun fetchData() {
    try {
        val data = api.getData()
        processData(data)

        KRelay.dispatch<ToastFeature> {
            it.show("Data loaded successfully")
        }
    } catch (e: NetworkException) {
        KRelay.dispatch<ToastFeature> {
            it.show("Network error: ${e.message}")
        }
        KRelay.dispatch<HapticFeature> {
            it.error()
        }
    } catch (e: Exception) {
        KRelay.dispatch<ToastFeature> {
            it.show("Unexpected error")
        }
    }
}
```

### Pattern 2: Permission → Action

```kotlin
fun startFeature() {
    KRelay.dispatch<PermissionFeature> {
        it.requestCamera { granted ->
            if (granted) {
                executeFeature()
            } else {
                showPermissionExplanation()
            }
        }
    }
}

private fun showPermissionExplanation() {
    KRelay.dispatch<ToastFeature> {
        it.show("Camera permission is required for this feature")
    }
    KRelay.dispatch<HapticFeature> {
        it.warning()
    }
}
```

### Pattern 3: Background Work → UI Feedback

```kotlin
fun syncData() {
    viewModelScope.launch(Dispatchers.IO) {
        // Heavy work on IO thread
        val items = repository.syncFromServer()

        // UI feedback on main thread (automatic)
        KRelay.dispatch<ToastFeature> {
            it.show("Synced ${items.size} items")
        }

        KRelay.dispatch<AnalyticsFeature> {
            it.track("sync_completed", mapOf("count" to items.size))
        }
    }
}
```

---

## Troubleshooting

### Issue: Actions Not Executing

**Check Registration:**
```kotlin
if (!KRelay.isRegistered<ToastFeature>()) {
    // Feature not registered yet
    // Commands are queued, will replay when registered
}
```

### Issue: Queue Growing Too Large

**Check Pending Count:**
```kotlin
val pending = KRelay.getPendingCount<ToastFeature>()
if (pending > 10) {
    // You may have forgotten to register the feature
}
```

### Issue: Memory Leaks Suspected

**Use Application Context:**
```kotlin
// ✅ Good - Application context (long-lived)
KRelay.register<ToastFeature>(AndroidToast(applicationContext))

// ❌ Avoid - Activity context (might leak)
KRelay.register<ToastFeature>(AndroidToast(this))
```

### Issue: Commands Lost After Process Death

**This is expected behavior** - lambdas can't be serialized.

For critical operations, use:
- `WorkManager` (Android background work)
- `SavedStateHandle` (UI state)
- `Room/DataStore` (data persistence)

---

## Best Practices

✅ **DO:**
- Use for UI commands (Toast, Navigation, Haptic)
- Dispatch from any thread (KRelay handles main thread)
- Use Application context for long-lived features
- Reset KRelay in test setup/teardown

❌ **DON'T:**
- Use for critical transactions (use WorkManager)
- Use for state management (use StateFlow)
- Use for heavy processing (use Dispatchers.IO)
- Expect return values (use expect/actual instead)

---

## Summary

KRelay is perfect for:
1. ✅ **Toast/Snackbar** - User feedback
2. ✅ **Navigation** - Screen transitions
3. ✅ **Permissions** - Platform permission requests
4. ✅ **Haptics** - Tactile feedback
5. ✅ **Analytics** - Simple event tracking
6. ✅ **Notifications** - In-app alerts

**Golden Rule:** Use KRelay for one-way, fire-and-forget UI commands from shared code. For everything else, use specialized tools.

---

**Need more help?** Check out:
- [README.md](../README.md) - Overview and philosophy
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical details
- [TESTING.md](TESTING.md) - Testing guide
- [Demo Code](../krelay/src/commonMain/kotlin/dev/brewkits/krelay/samples/) - Sample implementations

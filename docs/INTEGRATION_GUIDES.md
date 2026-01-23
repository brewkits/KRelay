# KRelay Integration Guides

> **Quick reference** for integrating popular KMP libraries with KRelay

---

## 🎯 Integration Pattern (Universal)

All KRelay integrations follow the same 4-step pattern:

```kotlin
// Step 1: Define contract (commonMain)
interface YourFeature : RelayFeature {
    fun doSomething(param: String, callback: (Result) -> Unit)
}

// Step 2: Use from ViewModel (commonMain)
class YourViewModel {
    fun action() {
        KRelay.dispatch<YourFeature> { feature ->
            feature.doSomething("param") { result ->
                // Handle result
            }
        }
    }
}

// Step 3: Implement with library (androidMain/iosMain)
class LibraryIntegration(private val library: LibraryAPI) : YourFeature {
    override fun doSomething(param: String, callback: (Result) -> Unit) {
        library.apiCall(param, callback)
    }
}

// Step 4: Register (Activity/ViewController/App root)
KRelay.register<YourFeature>(LibraryIntegration(libraryInstance))
```

---

## 📱 Library-Specific Guides

### 1. Moko Permissions

**Library**: [moko-permissions](https://github.com/icerockdev/moko-permissions)
**Use Case**: Request runtime permissions (Camera, Location, etc.)
**Challenge**: PermissionsController needs Activity binding

#### Integration Code

```kotlin
// commonMain/PermissionFeature.kt
interface PermissionFeature : RelayFeature {
    fun requestCamera(onResult: (Boolean) -> Unit)
    fun requestLocation(onResult: (Boolean) -> Unit)
    fun requestMicrophone(onResult: (Boolean) -> Unit)
}

// commonMain/CameraViewModel.kt
class CameraViewModel {
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> { perm ->
            perm.requestCamera { granted ->
                if (granted) {
                    // Start camera
                } else {
                    // Show rationale
                }
            }
        }
    }
}

// androidMain/MokoPermissionImpl.kt
class MokoPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {

    override fun requestCamera(onResult: (Boolean) -> Unit) {
        controller.providePermission(Permission.CAMERA) { result ->
            onResult(result)
        }
    }

    override fun requestLocation(onResult: (Boolean) -> Unit) {
        controller.providePermission(Permission.LOCATION) { result ->
            onResult(result)
        }
    }

    override fun requestMicrophone(onResult: (Boolean) -> Unit) {
        controller.providePermission(Permission.RECORD_AUDIO) { result ->
            onResult(result)
        }
    }
}

// androidMain/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val controller = PermissionsController(
            applicationContext = applicationContext
        )

        KRelay.register<PermissionFeature>(MokoPermissionImpl(controller))

        setContent { App() }
    }
}
```

#### iOS Integration

```kotlin
// iosMain/MokoPermissionImpl.kt
class IOSPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {

    override fun requestCamera(onResult: (Boolean) -> Unit) {
        controller.providePermission(Permission.CAMERA) { result ->
            onResult(result)
        }
    }

    // Similar for other permissions
}

// In SwiftUI
struct ContentView: View {
    init() {
        let controller = PermissionsController()
        KRelay.shared.register(impl: IOSPermissionImpl(controller: controller))
    }
}
```

**Benefits:**
- ✅ ViewModel testable without Moko
- ✅ No Activity reference in ViewModel
- ✅ Can swap for other permission library

---

### 2. Moko Biometry

**Library**: [moko-biometry](https://github.com/icerockdev/moko-biometry)
**Use Case**: Biometric authentication (fingerprint, Face ID)
**Challenge**: BiometryAuthenticator requires lifecycle

#### Integration Code

```kotlin
// commonMain/BiometricFeature.kt
interface BiometricFeature : RelayFeature {
    fun authenticate(
        title: String,
        subtitle: String,
        onResult: (success: Boolean, error: String?) -> Unit
    )

    fun isAvailable(): Boolean
}

// commonMain/PaymentViewModel.kt
class PaymentViewModel {
    fun confirmPayment(amount: Double) {
        KRelay.dispatch<BiometricFeature> { bio ->
            if (!bio.isAvailable()) {
                // Fall back to PIN/Password
                return@dispatch
            }

            bio.authenticate(
                title = "Confirm Payment",
                subtitle = "Pay $${amount}"
            ) { success, error ->
                if (success) {
                    processPayment(amount)
                } else {
                    showError(error)
                }
            }
        }
    }
}

// androidMain/MokoBiometryImpl.kt
class MokoBiometryImpl(
    private val authenticator: BiometryAuthenticator
) : BiometricFeature {

    override fun authenticate(
        title: String,
        subtitle: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        authenticator.authenticate(
            requestTitle = title,
            requestReason = subtitle,
            failureButtonText = "Cancel"
        ) { result ->
            when (result) {
                is BiometryAuthenticator.Result.Success -> {
                    onResult(true, null)
                }
                is BiometryAuthenticator.Result.Failed -> {
                    onResult(false, result.message)
                }
                is BiometryAuthenticator.Result.Cancelled -> {
                    onResult(false, "Cancelled by user")
                }
            }
        }
    }

    override fun isAvailable(): Boolean {
        return authenticator.checkBiometryAuthentication()
    }
}

// androidMain/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authenticator = BiometryAuthenticator(
            context = applicationContext
        )

        KRelay.register<BiometricFeature>(MokoBiometryImpl(authenticator))

        setContent { App() }
    }
}
```

**Benefits:**
- ✅ ViewModel doesn't know about biometry implementation
- ✅ Easy to mock for testing
- ✅ Can swap Moko for BiometricPrompt or LocalAuthentication

---

### 3. Voyager Navigation

**Library**: [Voyager](https://github.com/adrielcafe/voyager)
**Use Case**: Type-safe navigation
**Challenge**: Navigator needs Compose context

#### Integration Code

```kotlin
// commonMain/NavigationFeature.kt
interface NavigationFeature : RelayFeature {
    fun goToHome()
    fun goToProfile(userId: String)
    fun goToSettings()
    fun goBack()
}

// commonMain/LoginViewModel.kt
class LoginViewModel {
    fun onLoginSuccess(user: User) {
        // Business logic
        saveUserSession(user)

        // Navigate
        KRelay.dispatch<NavigationFeature> { nav ->
            nav.goToHome()
        }
    }

    fun onViewProfile(userId: String) {
        KRelay.dispatch<NavigationFeature> { nav ->
            nav.goToProfile(userId)
        }
    }
}

// Platform code (works on both Android/iOS)
class VoyagerNavigationImpl(
    private val navigator: Navigator
) : NavigationFeature {

    override fun goToHome() {
        navigator.push(HomeScreen())
    }

    override fun goToProfile(userId: String) {
        navigator.push(ProfileScreen(userId))
    }

    override fun goToSettings() {
        navigator.push(SettingsScreen())
    }

    override fun goBack() {
        navigator.pop()
    }
}

// In Compose (Android/iOS shared)
@Composable
fun App() {
    Navigator(LoginScreen()) { navigator ->
        // Register KRelay implementation when Navigator is ready
        LaunchedEffect(navigator) {
            val navImpl = VoyagerNavigationImpl(navigator)
            KRelay.register<NavigationFeature>(navImpl)
        }

        CurrentScreen()
    }
}
```

**Advanced: Multiple Navigation Stacks**

```kotlin
// For Super Apps with multiple navigation stacks
interface RideNavFeature : RelayFeature {
    fun goToBookRide()
    fun goToRideHistory()
}

interface FoodNavFeature : RelayFeature {
    fun goToRestaurants()
    fun goToOrderHistory()
}

// Each module has its own Navigator + KRelay implementation
```

**Benefits:**
- ✅ ViewModel has zero Voyager dependency
- ✅ Easy to swap Voyager for Decompose or Compose Navigation
- ✅ Navigation logic separated from business logic
- ✅ Testable without navigation framework

---

### 4. Decompose Navigation

**Library**: [Decompose](https://github.com/arkivanov/Decompose)
**Use Case**: Component-based navigation
**Challenge**: ComponentContext in shared code

#### Integration Code

```kotlin
// commonMain/NavigationFeature.kt (same as Voyager)
interface NavigationFeature : RelayFeature {
    fun goToHome()
    fun goToDetails(itemId: String)
    fun goBack()
}

// Decompose Component
class RootComponent(
    componentContext: ComponentContext,
    private val navigateToHome: () -> Unit,
    private val navigateToDetails: (String) -> Unit,
    private val navigateBack: () -> Unit
) : ComponentContext by componentContext, NavigationFeature {

    override fun goToHome() = navigateToHome()
    override fun goToDetails(itemId: String) = navigateToDetails(itemId)
    override fun goBack() = navigateBack()

    init {
        // Register this component as NavigationFeature
        KRelay.register<NavigationFeature>(this)
    }
}

// ViewModel (same as Voyager example - no changes needed!)
class MyViewModel {
    fun onItemClick(itemId: String) {
        KRelay.dispatch<NavigationFeature> { nav ->
            nav.goToDetails(itemId)
        }
    }
}
```

**Benefits:**
- ✅ Same NavigationFeature interface works for Voyager AND Decompose
- ✅ ViewModel code unchanged when switching navigation libraries

---

### 5. Peekaboo Media Picker

**Library**: [Peekaboo](https://github.com/onseok/peekaboo)
**Use Case**: Image/Video picking
**Challenge**: rememberImagePickerLauncher is Compose-only

#### Integration Code

```kotlin
// commonMain/MediaFeature.kt
interface MediaFeature : RelayFeature {
    fun pickImage(onResult: (ByteArray?) -> Unit)
    fun pickVideo(onResult: (ByteArray?) -> Unit)
    fun capturePhoto(onResult: (ByteArray?) -> Unit)
}

// commonMain/ProfileViewModel.kt
class ProfileViewModel {
    fun updateProfilePicture() {
        KRelay.dispatch<MediaFeature> { media ->
            media.pickImage { imageData ->
                if (imageData != null) {
                    uploadToServer(imageData)
                    updateUI(imageData)
                }
            }
        }
    }
}

// In Composable (Android/iOS shared)
@Composable
fun SetupMediaFeature() {
    val scope = rememberCoroutineScope()

    // Peekaboo launcher for images
    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { bytes ->
            currentImageCallback?.invoke(bytes.firstOrNull())
        }
    )

    // Peekaboo launcher for camera
    val cameraPicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { bytes ->
            currentCameraCallback?.invoke(bytes.firstOrNull())
        }
    )

    // Create implementation
    val impl = remember {
        object : MediaFeature {
            var currentImageCallback: ((ByteArray?) -> Unit)? = null
            var currentCameraCallback: ((ByteArray?) -> Unit)? = null

            override fun pickImage(onResult: (ByteArray?) -> Unit) {
                currentImageCallback = onResult
                imagePicker.launch()
            }

            override fun pickVideo(onResult: (ByteArray?) -> Unit) {
                // Similar implementation
            }

            override fun capturePhoto(onResult: (ByteArray?) -> Unit) {
                currentCameraCallback = onResult
                cameraPicker.launch()
            }
        }
    }

    // Register when ready
    LaunchedEffect(impl) {
        KRelay.register<MediaFeature>(impl)
    }
}

// Use in App root
@Composable
fun App() {
    SetupMediaFeature()  // Register once at app root

    // Rest of your app
    MyScreen()
}
```

**Benefits:**
- ✅ ViewModel doesn't know about Peekaboo
- ✅ Can swap for native image pickers
- ✅ Business logic stays platform-agnostic

---

### 6. Play Core (Android) / StoreKit (iOS)

**Library**: Play Core Review API / StoreKit
**Use Case**: In-app review prompts
**Challenge**: ReviewManager needs Activity context

#### Integration Code

```kotlin
// commonMain/ReviewFeature.kt
interface ReviewFeature : RelayFeature {
    fun requestReview()
    fun checkForUpdates()
}

// commonMain/CheckoutViewModel.kt
class CheckoutViewModel {
    fun onOrderCompleted(orderId: String, amount: Double) {
        // Business logic
        saveOrder(orderId, amount)

        // Request review if eligible
        if (shouldAskForReview()) {
            KRelay.dispatch<ReviewFeature> { review ->
                review.requestReview()
            }
        }
    }

    private fun shouldAskForReview(): Boolean {
        // Logic: after 3rd successful order, hasn't reviewed, etc.
        return true
    }
}

// androidMain/PlayCoreReviewImpl.kt
class PlayCoreReviewImpl(
    private val activity: Activity
) : ReviewFeature {

    private val reviewManager = ReviewManagerFactory.create(activity)

    override fun requestReview() {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }

    override fun checkForUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        // Check for updates...
    }
}

// androidMain/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KRelay.register<ReviewFeature>(PlayCoreReviewImpl(this))

        setContent { App() }
    }
}
```

#### iOS StoreKit Implementation

```swift
// iOS/StoreKitReviewImpl.swift
import StoreKit

class StoreKitReviewImpl: ReviewFeature {
    func requestReview() {
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
            SKStoreReviewController.requestReview(in: scene)
        }
    }

    func checkForUpdates() {
        // StoreKit 2 update check
    }
}

// Register in SwiftUI
struct ContentView: View {
    init() {
        KRelay.shared.register(impl: StoreKitReviewImpl())
    }
}
```

**Benefits:**
- ✅ ViewModel decides WHEN to ask for review (business logic)
- ✅ Platform implementation decides HOW (Play Core vs StoreKit)
- ✅ Easy A/B testing (change review trigger logic)

---

### 7. Firebase Analytics

**Use Case**: Track events from ViewModels
**Challenge**: FirebaseAnalytics needs platform initialization

#### Integration Code

```kotlin
// commonMain/AnalyticsFeature.kt
interface AnalyticsFeature : RelayFeature {
    fun trackEvent(name: String, params: Map<String, Any> = emptyMap())
    fun trackScreenView(screenName: String)
    fun setUserId(userId: String)
}

// commonMain/ProductViewModel.kt
class ProductViewModel {
    fun onProductViewed(productId: String) {
        KRelay.dispatch<AnalyticsFeature> { analytics ->
            analytics.trackEvent("product_viewed", mapOf(
                "product_id" to productId,
                "source" to "search"
            ))
        }
    }

    fun onPurchaseCompleted(orderId: String, amount: Double) {
        KRelay.dispatch<AnalyticsFeature> { analytics ->
            analytics.trackEvent("purchase", mapOf(
                "order_id" to orderId,
                "amount" to amount,
                "currency" to "USD"
            ))
        }
    }
}

// androidMain/FirebaseAnalyticsImpl.kt
class FirebaseAnalyticsImpl(
    private val analytics: FirebaseAnalytics
) : AnalyticsFeature {

    override fun trackEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        analytics.logEvent(name, bundle)
    }

    override fun trackScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }

    override fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }
}

// androidMain/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val analytics = Firebase.analytics
        KRelay.register<AnalyticsFeature>(FirebaseAnalyticsImpl(analytics))

        setContent { App() }
    }
}
```

**Benefits:**
- ✅ ViewModel doesn't depend on Firebase SDK
- ✅ Easy to switch to Mixpanel, Amplitude, etc.
- ✅ Testable with mock analytics

---

## 🧪 Testing Integrations

### Unit Testing ViewModels

```kotlin
class MyViewModelTest {
    @BeforeTest
    fun setup() {
        KRelay.reset()  // Clean slate

        // Register mock implementation
        KRelay.register<PermissionFeature>(MockPermissionFeature())
    }

    @Test
    fun `when takePicture called, should request camera permission`() {
        val mockPerm = MockPermissionFeature()
        KRelay.register<PermissionFeature>(mockPerm)

        val viewModel = CameraViewModel()
        viewModel.takePicture()

        assertTrue(mockPerm.cameraRequested)
    }
}

class MockPermissionFeature : PermissionFeature {
    var cameraRequested = false

    override fun requestCamera(onResult: (Boolean) -> Unit) {
        cameraRequested = true
        onResult(true)  // Simulate granted
    }
}
```

---

## 📚 Integration Checklist

When integrating a new library:

- [ ] **Step 1**: Define `RelayFeature` interface in `commonMain`
- [ ] **Step 2**: Use `KRelay.dispatch<>()` from ViewModel
- [ ] **Step 3**: Implement interface in `androidMain`/`iosMain`
- [ ] **Step 4**: Register implementation at app root (Activity/ViewController)
- [ ] **Step 5**: Write mock implementation for testing
- [ ] **Step 6**: Test ViewModel with mock (no library dependency)

---

## 🎯 Common Patterns

### Pattern 1: Permission + Action

```kotlin
fun doProtectedAction() {
    KRelay.dispatch<PermissionFeature> { perm ->
        perm.requestCamera { granted ->
            if (granted) {
                // Permission granted, now do action
                KRelay.dispatch<MediaFeature> { media ->
                    media.capturePhoto { photo ->
                        // Handle photo
                    }
                }
            }
        }
    }
}
```

### Pattern 2: Chaining Multiple Dispatches

```kotlin
fun completeOnboarding() {
    // 1. Request permission
    KRelay.dispatch<PermissionFeature> { perm ->
        perm.requestCamera { granted ->
            if (!granted) return@requestCamera

            // 2. Take profile picture
            KRelay.dispatch<MediaFeature> { media ->
                media.capturePhoto { photo ->
                    if (photo == null) return@capturePhoto

                    // 3. Navigate to next screen
                    KRelay.dispatch<NavigationFeature> { nav ->
                        nav.goToHome()
                    }
                }
            }
        }
    }
}
```

---

## 🚀 Next Steps

1. **Browse Demo App**: See [IntegrationsDemo.kt](../composeApp/src/commonMain/kotlin/dev/brewkits/krelay/integrations/IntegrationsDemo.kt) for working examples
2. **Read Positioning**: Understand [why KRelay is The Glue Code Standard](./POSITIONING.md)
3. **Explore Anti-Patterns**: Learn [when NOT to use KRelay](./ANTI_PATTERNS.md)

---

**Can't find your library?** Open an issue and we'll create an integration guide!

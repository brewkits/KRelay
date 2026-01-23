# KRelay: The Glue Code Standard for Kotlin Multiplatform

> **Position Statement**: KRelay is the de facto standard for connecting KMP libraries and platform APIs to shared business logic, enabling clean architecture without memory leaks or lifecycle complexity.

---

## The Problem: KMP Libraries Need "Last Mile Integration"

### Current State of KMP Ecosystem

Kotlin Multiplatform has mature libraries for many domains:
- **Moko Permissions**: Permission management
- **Moko Biometry**: Biometric authentication
- **Voyager**: Navigation
- **Peekaboo**: Media/Camera picking
- **Play Core / StoreKit**: In-app review, updates
- **And 100+ others...**

**But there's a gap**: These libraries need Activity/ViewController binding, but your ViewModels are pure Kotlin in `commonMain`.

### The "Last Mile Problem"

```kotlin
// ❌ Problem: How does ViewModel trigger platform-specific operations?

// In commonMain (ViewModel)
class ProfileViewModel {
    fun uploadProfilePicture() {
        // Need to show image picker...
        // But Peekaboo's rememberImagePickerLauncher() requires Compose context!
        // Cannot call from here!
    }

    fun requestCameraPermission() {
        // Need to request permission...
        // But Moko's PermissionsController requires Activity binding!
        // Cannot call from here!
    }
}
```

**Traditional "Solutions" (All have issues):**

1. **expect/actual functions** → Violates Single Responsibility Principle
2. **Callback injection** → Causes memory leaks with Activity references
3. **SharedFlow + LaunchedEffect** → Boilerplate, rotation issues, event replay bugs
4. **Direct library integration in UI** → Business logic scattered across layers

---

## KRelay's Position: The Glue Code Standard

### What is "Glue Code"?

**Glue Code** = The thin layer that connects:
- **KMP libraries** (Moko, Voyager, Peekaboo, etc.) ← Platform-specific
- **Shared business logic** (ViewModels, UseCases) ← Pure Kotlin in commonMain

**KRelay is designed specifically for this "last mile":**

```kotlin
// ✅ Solution: KRelay as the standard glue layer

// 1. Define contract (commonMain)
interface MediaFeature : RelayFeature {
    fun pickImage(onResult: (ByteArray?) -> Unit)
}

// 2. ViewModel dispatches (commonMain) - Zero library dependencies!
class ProfileViewModel {
    fun uploadProfilePicture() {
        KRelay.dispatch<MediaFeature> { media ->
            media.pickImage { imageData ->
                if (imageData != null) {
                    uploadToServer(imageData)
                }
            }
        }
    }
}

// 3. Platform implements with Peekaboo (androidMain/iosMain)
class PeekabooMediaImpl : MediaFeature {
    private val imagePicker = rememberImagePickerLauncher { bytes ->
        // Handle result
    }

    override fun pickImage(onResult: (ByteArray?) -> Unit) {
        imagePicker.launch()
    }
}

// 4. Wire at app root
KRelay.register<MediaFeature>(PeekabooMediaImpl())
```

**Result:**
- ✅ ViewModel has **zero** Peekaboo dependency
- ✅ Business logic stays in commonMain
- ✅ No memory leaks (WeakRef)
- ✅ No rotation bugs (Sticky Queue)
- ✅ Easy testing (mock MediaFeature)

---

## Why KRelay is the Standard

### 1. Designed for Library Integration

KRelay's architecture specifically solves library integration problems:

| Library Challenge | KRelay Solution |
|-------------------|-----------------|
| **Requires Activity/ViewController** | WeakRef prevents leaks, UI registers on lifecycle |
| **Compose-only API** (e.g., Peekaboo) | ViewModel dispatches, UI implements with Compose |
| **Needs lifecycle awareness** | Sticky Queue queues calls before UI ready |
| **Multiple callbacks** | RelayFeature interface can have multiple methods |
| **Testing nightmare** | Mock the interface, not the library |

### 2. Real-World Integration Examples

#### Integration 1: Moko Permissions

```kotlin
// Problem: PermissionsController needs Activity binding
// ViewModel cannot access Activity

// Solution with KRelay:

// commonMain - Contract
interface PermissionFeature : RelayFeature {
    fun requestCamera(onResult: (Boolean) -> Unit)
}

// commonMain - ViewModel (zero Moko dependency!)
class CameraViewModel {
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> { perm ->
            perm.requestCamera { granted ->
                if (granted) startCamera()
            }
        }
    }
}

// androidMain - Moko integration
class MokoPermissionImpl(
    private val controller: PermissionsController
) : PermissionFeature {
    override fun requestCamera(onResult: (Boolean) -> Unit) {
        controller.providePermission(Permission.CAMERA) { result ->
            onResult(result)
        }
    }
}

// Wire in Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = PermissionsController(
            applicationContext = applicationContext
        )
        KRelay.register<PermissionFeature>(MokoPermissionImpl(controller))
    }
}
```

**Benefits:**
- ✅ ViewModel testable without Moko
- ✅ Can swap Moko for other permission library
- ✅ Activity lifecycle managed automatically
- ✅ Business logic in commonMain, platform integration in androidMain

#### Integration 2: Voyager Navigation

```kotlin
// Problem: Navigator needs Compose context
// ViewModel shouldn't depend on navigation framework

// Solution with KRelay:

// commonMain - Contract
interface NavFeature : RelayFeature {
    fun goToProfile(userId: String)
    fun goBack()
}

// commonMain - ViewModel (zero Voyager dependency!)
class LoginViewModel {
    fun onLoginSuccess(user: User) {
        KRelay.dispatch<NavFeature> { nav ->
            nav.goToProfile(user.id)
        }
    }
}

// Voyager integration
class VoyagerNavImpl(
    private val navigator: Navigator
) : NavFeature {
    override fun goToProfile(userId: String) {
        navigator.push(ProfileScreen(userId))
    }

    override fun goBack() {
        navigator.pop()
    }
}

// Wire in App root
@Composable
fun App() {
    Navigator(LoginScreen()) { navigator ->
        LaunchedEffect(navigator) {
            KRelay.register<NavFeature>(VoyagerNavImpl(navigator))
        }
        CurrentScreen()
    }
}
```

**Benefits:**
- ✅ ViewModel can be unit tested without Voyager
- ✅ Switch to Decompose/Compose Navigation without touching ViewModels
- ✅ Navigation logic separated from business logic

#### Integration 3: Peekaboo Media Picking

```kotlin
// Problem: rememberImagePickerLauncher() is Compose-only
// ViewModel cannot call Compose functions

// Solution with KRelay:

// commonMain - Contract
interface MediaFeature : RelayFeature {
    fun pickImage(onResult: (ByteArray?) -> Unit)
    fun capturePhoto(onResult: (ByteArray?) -> Unit)
}

// commonMain - ViewModel (zero Peekaboo dependency!)
class ProfileViewModel {
    fun updateProfilePicture() {
        KRelay.dispatch<MediaFeature> { media ->
            media.pickImage { imageData ->
                if (imageData != null) {
                    uploadToServer(imageData)
                }
            }
        }
    }
}

// Peekaboo integration (in Composable)
@Composable
fun SetupMediaFeature() {
    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = rememberCoroutineScope(),
        onResult = { bytes ->
            currentCallback?.invoke(bytes.firstOrNull())
        }
    )

    val impl = remember {
        object : MediaFeature {
            override fun pickImage(onResult: (ByteArray?) -> Unit) {
                currentCallback = onResult
                imagePicker.launch()
            }

            override fun capturePhoto(onResult: (ByteArray?) -> Unit) {
                currentCallback = onResult
                imagePicker.launch()
            }
        }
    }

    LaunchedEffect(impl) {
        KRelay.register<MediaFeature>(impl)
    }
}
```

**Benefits:**
- ✅ ViewModel doesn't know about Peekaboo
- ✅ Can swap Peekaboo for native APIs
- ✅ Business logic stays platform-agnostic

#### Integration 4: Moko Biometry

```kotlin
// Problem: BiometryAuthenticator requires lifecycle
// ViewModel shouldn't depend on platform biometry APIs

// Solution with KRelay:

// commonMain - Contract
interface BiometricFeature : RelayFeature {
    fun authenticate(
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit
    )
}

// commonMain - ViewModel (zero Moko Biometry dependency!)
class PaymentViewModel {
    fun confirmPayment(amount: Double) {
        KRelay.dispatch<BiometricFeature> { bio ->
            bio.authenticate(
                title = "Confirm Payment",
                subtitle = "Pay $${amount}",
                onResult = { success ->
                    if (success) processPayment(amount)
                }
            )
        }
    }
}

// Moko Biometry integration
class MokoBiometryImpl(
    private val authenticator: BiometryAuthenticator
) : BiometricFeature {
    override fun authenticate(
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit
    ) {
        authenticator.authenticate(
            requestTitle = title,
            requestReason = subtitle,
            failureButtonText = "Cancel"
        ) { result ->
            onResult(result.isSuccess)
        }
    }
}
```

**Benefits:**
- ✅ ViewModel testable without biometry hardware
- ✅ Can swap Moko Biometry for BiometricPrompt or LocalAuthentication
- ✅ Business logic independent of auth mechanism

---

## Comparison: KRelay vs Alternatives

### Alternative 1: Direct Library Usage in ViewModel

```kotlin
// ❌ Problem: ViewModel depends on platform library
class ProfileViewModel(
    private val permissionsController: PermissionsController // Android-specific!
) {
    fun takePicture() {
        permissionsController.providePermission(Permission.CAMERA) { granted ->
            if (granted) startCamera()
        }
    }
}

// Issues:
// - Cannot compile in commonMain (PermissionsController is Android-only)
// - Cannot unit test without Moko mocks
// - Hard to swap libraries
// - Violates Clean Architecture
```

### Alternative 2: expect/actual Functions

```kotlin
// ❌ Problem: Boilerplate, not scalable
// commonMain
expect fun requestCameraPermission(onResult: (Boolean) -> Unit)

// androidMain
actual fun requestCameraPermission(onResult: (Boolean) -> Unit) {
    // Need Activity context - where to get it?
    // Need to store callback - memory leak?
}

// Issues:
// - Need expect/actual for EVERY platform operation
// - Cannot manage lifecycle properly
// - Memory leaks if not careful
// - Doesn't scale to 100+ operations
```

### Alternative 3: SharedFlow + LaunchedEffect

```kotlin
// ❌ Problem: Complex, rotation bugs
// ViewModel
class ProfileViewModel {
    private val _events = MutableSharedFlow<ProfileEvent>()
    val events = _events.asSharedFlow()

    fun takePicture() {
        viewModelScope.launch {
            _events.emit(ProfileEvent.RequestCamera)
        }
    }
}

// UI
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val events = viewModel.events.collectAsState(null)

    LaunchedEffect(events.value) {
        when (val event = events.value) {
            is ProfileEvent.RequestCamera -> {
                // Request permission...
                // But event fires multiple times on rotation!
                // Need consumed tracking...
            }
        }
    }
}

// Issues:
// - Events replay on rotation (need manual tracking)
// - Boilerplate (MutableSharedFlow + collectAsState + LaunchedEffect)
// - Hard to test
// - Race conditions with multiple collectors
```

### ✅ Alternative 4: KRelay (Recommended)

```kotlin
// ✅ Solution: Clean, testable, scalable

// ViewModel
class ProfileViewModel {
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> { perm ->
            perm.requestCamera { granted ->
                if (granted) startCamera()
            }
        }
    }
}

// Platform integration (separate file)
class MokoPermissionImpl(controller: PermissionsController) : PermissionFeature {
    override fun requestCamera(onResult: (Boolean) -> Unit) {
        controller.providePermission(Permission.CAMERA, onResult)
    }
}

// Benefits:
// - Single dispatch() call
// - No rotation bugs (Sticky Queue)
// - Easy testing (mock PermissionFeature)
// - Scales to 100+ features
// - No memory leaks (WeakRef)
```

---

## The KRelay Ecosystem: Glue Layer for Everything

KRelay is becoming the standard glue layer for the entire KMP ecosystem:

```
┌────────────────────────────────────────────────────────────┐
│                  Your App (commonMain)                     │
│                                                            │
│  ViewModels, UseCases, Repositories                        │
│         ↓                                                  │
│    KRelay.dispatch<Feature>()   ← Single integration point│
└────────────────────────────────────────────────────────────┘
                        ↓
┌────────────────────────────────────────────────────────────┐
│                KRelay (Glue Layer)                         │
│                                                            │
│  • WeakRef registry (no leaks)                             │
│  • Sticky Queue (no lost commands)                         │
│  • Main thread dispatch (thread-safe)                      │
│  • Type-safe interfaces                                    │
└────────────────────────────────────────────────────────────┘
                        ↓
┌────────────────────────────────────────────────────────────┐
│            KMP Libraries (Platform-Specific)               │
│                                                            │
│  Moko Permissions  │  Voyager  │  Peekaboo  │  Play Core  │
│  Moko Biometry    │  Decompose │  Koin      │  StoreKit   │
│  Moko Resources   │  PreCompose│  Napier    │  Firebase   │
└────────────────────────────────────────────────────────────┘
```

### Supported Libraries (Examples)

| Library Category | Examples | KRelay Integration |
|------------------|----------|-------------------|
| **Navigation** | Voyager, Decompose, PreCompose, Compose Nav | ✅ NavFeature |
| **Permissions** | Moko Permissions, Accompanist | ✅ PermissionFeature |
| **Biometry** | Moko Biometry, BiometricPrompt, LocalAuth | ✅ BiometricFeature |
| **Media** | Peekaboo, ImagePicker | ✅ MediaFeature |
| **In-App Review** | Play Core, StoreKit | ✅ ReviewFeature |
| **Analytics** | Firebase, Mixpanel, custom | ✅ AnalyticsFeature |
| **Logging** | Napier, Kermit | ✅ LogFeature |
| **Haptics** | Native vibration APIs | ✅ HapticFeature |
| **Notifications** | Native notification APIs | ✅ NotificationFeature |

---

## Adoption Strategy: Becoming the Standard

### Phase 1: Core Value Proposition (Current)

**Message**: "KRelay solves memory leaks and lost commands for native interop"

**Target**: Individual developers building KMP apps

**Evidence**:
- ✅ 1.0 released with production-ready core
- ✅ Demo app shows real integrations (Moko, Voyager, Peekaboo)
- ✅ Documentation covers anti-patterns and best practices

### Phase 2: Ecosystem Positioning (Next)

**Message**: "KRelay is The Glue Code Standard for KMP libraries"

**Target**: KMP library authors, architecture teams, community influencers

**Actions**:
1. **Create Integration Guides** for popular libraries:
   - "KRelay + Moko Permissions"
   - "KRelay + Voyager"
   - "KRelay + Peekaboo"
   - "KRelay + Decompose"

2. **Community Engagement**:
   - Blog posts: "Why KMP Libraries Need a Glue Layer"
   - Conference talks: KotlinConf, Droidcon
   - YouTube tutorials: Real integration examples

3. **Library Author Outreach**:
   - Provide KRelay integration examples in library docs
   - "Recommended with KRelay" badges
   - Partner with Moko, Voyager, Peekaboo teams

### Phase 3: Industry Standard (Long-term)

**Message**: "If you're building KMP, you're using KRelay"

**Target**: Enterprise teams, consultancies, bootcamps

**Evidence**:
- Major apps using KRelay (case studies)
- KMP courses teaching KRelay as default pattern
- Job descriptions mentioning KRelay
- StackOverflow answers recommending KRelay

---

## Marketing Narrative

### Tagline Options

1. **"The Glue Code Standard for Kotlin Multiplatform"** ← Primary
2. "Connect Any KMP Library to Shared Business Logic"
3. "Zero Boilerplate, Zero Leaks, Zero Hassle"
4. "The Missing Link in Your KMP Architecture"

### Elevator Pitch (30 seconds)

> "KRelay is the standard way to connect KMP libraries like Moko, Voyager, and Peekaboo to your shared business logic. It solves the 'last mile problem': your ViewModels are in commonMain, but libraries need platform-specific binding. KRelay provides a clean, type-safe bridge with zero memory leaks, no rotation bugs, and easy testing. It's the glue layer every KMP app needs."

### Key Messages by Audience

#### For Individual Developers
- **Pain**: "Tired of memory leaks and rotation bugs when integrating platform libraries?"
- **Solution**: "KRelay handles lifecycle, threading, and queuing automatically."
- **Proof**: "See our demo app integrating 5+ libraries with zero boilerplate."

#### For Library Authors
- **Pain**: "Your library requires Activity/ViewController, but users want to call from ViewModels."
- **Solution**: "Recommend KRelay as the integration pattern in your docs."
- **Benefit**: "Users will find your library easier to integrate, leading to more adoption."

#### For Enterprise/Architecture Teams
- **Pain**: "Multiple teams, multiple KMP libraries, messy integration code everywhere."
- **Solution**: "Standardize on KRelay as the glue layer across all teams."
- **Benefit**: "Consistent architecture, easier code reviews, better testing."

---

## Competitive Positioning

### What KRelay Is NOT

- ❌ **Not a DI framework** (use Koin/Hilt for that)
- ❌ **Not state management** (use StateFlow/Redux)
- ❌ **Not a navigation library** (use Voyager/Decompose)
- ❌ **Not for background work** (use WorkManager)

### What KRelay IS

- ✅ **The glue layer** between your ViewModels and platform-specific libraries
- ✅ **The standard pattern** for lifecycle-aware platform dispatch
- ✅ **The simple solution** to memory leaks and lost commands

**Analogy**: KRelay is like Express.js middleware for web apps - it's not the framework, it's the glue that makes everything work together cleanly.

---

## Success Metrics

### Phase 1 Metrics (Individual Adoption)
- GitHub stars: 500+ (validate product-market fit)
- Demo app clones: Track interest
- Documentation views: Monitor learning curve
- Issues reporting integration problems: Identify friction

### Phase 2 Metrics (Ecosystem Recognition)
- Blog posts/tutorials mentioning KRelay: 10+
- Libraries recommending KRelay: 3+ (Moko, Voyager, etc.)
- Conference talks featuring KRelay: 2+
- StackOverflow answers using KRelay: 20+

### Phase 3 Metrics (Industry Standard)
- Production apps using KRelay: 50+
- Job postings mentioning KRelay: 5+
- KMP courses teaching KRelay: 3+
- Enterprise case studies: 2+

---

## Call to Action

### For Library Authors

> "Your library is powerful, but integration is hard. Show users how to integrate with KRelay in your documentation. We'll provide the integration guide template and promote your library in our ecosystem showcase."

**Contact**: [Your email/Twitter/Discord]

### For Community Contributors

> "Help us create integration guides for your favorite KMP libraries. Submit a PR with a working example and we'll feature it in our documentation."

**Contribute**: [GitHub Issues with "integration-guide" label]

### For Users

> "Already using KRelay? Share your experience in a blog post or tweet. We'll feature your story and help other developers learn from your architecture."

**Share**: Tag #KRelay #KotlinMultiplatform

---

## Conclusion

**KRelay is positioning itself as THE standard glue layer for Kotlin Multiplatform apps.**

Just as:
- **Retrofit** became the standard HTTP client for Android
- **Koin** became the standard DI for KMP
- **Compose** became the standard UI framework

**KRelay aims to be the standard platform integration layer for KMP.**

Every KMP library that needs Activity/ViewController binding should be integrated via KRelay. This creates:
- ✅ Consistent architecture across apps
- ✅ Easier learning curve for developers
- ✅ Better testability for everyone
- ✅ Cleaner, more maintainable codebases

**The future of KMP is modular. KRelay is the glue that holds it together.**

---

**Join the movement. Build the standard. Become the glue.**

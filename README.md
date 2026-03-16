# KRelay

![KRelay Cover](rrelay.png)

> **The Glue Code Standard for Kotlin Multiplatform**
>
> Safe, leak-free bridge between shared code and platform-specific APIs

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg?style=flat&logo=kotlin)](http://kotlinlang.org)
[![Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-orange.svg?style=flat)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/dev.brewkits/krelay.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/dev.brewkits/krelay)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-zero-success.svg)](https://github.com/brewkits/krelay/blob/main/krelay/build.gradle.kts)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

---

## What is KRelay?

KRelay is a lightweight bridge that connects your shared Kotlin code to platform-specific implementations (Android/iOS) without memory leaks or lifecycle complexity. It offers a simple, type-safe API for one-way, fire-and-forget UI commands.

**v2.0 introduces a powerful instance-based API**, perfect for dependency injection and large-scale "Super Apps," while remaining fully backward-compatible with the original singleton.

**Use Cases**:
- **Singleton**: Simple, zero-config for small to medium apps.
- **Instances**: DI-friendly, isolated for large modular apps.

```kotlin
// ✅ Singleton (Existing projects)
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Welcome!") }
    }
}

// ✅ Instance-based (DI / Super Apps)
class RideViewModel(private val krelay: KRelayInstance) {
    fun onBookingConfirmed() {
        krelay.dispatch<ToastFeature> { it.show("Ride booked!") }
    }
}
```

---

## What's New in v2.0.0 - Instance API for Super Apps 🚀

KRelay v2.0 introduces a powerful **instance-based API**, designed for scalability, dependency injection, and large-scale applications ("Super Apps"), while preserving **100% backward compatibility** with the simple singleton API.

### 1. Instance-Based API
- ✅ **Create Isolated Instances**: `KRelay.create("MyModuleScope")`
- ✅ **Solves Super App Problem**: No more feature name conflicts between independent modules.
- ✅ **DI-Friendly**: Inject `KRelayInstance` into your ViewModels, UseCases, and repositories.
- ✅ **Full Isolation**: Each instance has its own registry, queue, and configuration.

```kotlin
// Before (v1.x): Global singleton could cause conflicts
// ⚠️ Ride module and Food module might conflict on `ToastFeature`
KRelay.register<ToastFeature>(RideToastImpl())
KRelay.register<ToastFeature>(FoodToastImpl()) // Overwrites the first one!

// After (v2.0): Fully isolated instances
val rideKRelay = KRelay.create("Rides")
val foodKRelay = KRelay.create("Food")

rideKRelay.register<ToastFeature>(RideToastImpl()) // No conflict
foodKRelay.register<ToastFeature>(FoodToastImpl()) // No conflict
```

### 2. Configurable Instances
- ✅ **Builder Pattern**: `KRelay.builder("MyScope").maxQueueSize(50).build()`
- ✅ **Per-Instance Settings**: Customize queue size, action expiry, and debug mode for each module.

### 3. Full Backward Compatibility
- ✅ **No Breaking Changes**: All existing code using `KRelay.dispatch` works exactly as before.
- ✅ **Easy Migration**: Adopt the new instance API incrementally, where it makes sense.
- ✅ The global `KRelay` object now transparently uses a default instance.

**Recommendation**: All new projects, especially those using DI (Koin/Hilt) or with a multi-module architecture, should use the new instance-based API. Existing projects can upgrade without any changes.

---

## Memory Management Best Practices

### Lambda Capture Warning

KRelay queues lambdas that may capture variables. Follow these rules to avoid leaks:

**✅ DO: Capture primitives and data**
```kotlin
// Singleton
val message = viewModel.successMessage
KRelay.dispatch<ToastFeature> { it.show(message) }

// Instance
val krelay: KRelayInstance = get() // from DI
krelay.dispatch<ToastFeature> { it.show(message) }
```

**❌ DON'T: Capture ViewModels or Contexts**
```kotlin
// BAD: Captures entire viewModel
KRelay.dispatch<ToastFeature> { it.show(viewModel.data) }
```

**🔧 CLEANUP: Use clearQueue() in onCleared()**
```kotlin
// Singleton Usage
class MyViewModel : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        KRelay.clearQueue<ToastFeature>()
    }
}

// Instance Usage (with DI)
class MyViewModel(private val krelay: KRelayInstance) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        krelay.clearQueue<ToastFeature>()
    }
}
```

### Built-in Protections

Each KRelay instance includes three passive safety mechanisms:

1. **actionExpiryMs** (default: 5 min): Old actions auto-expire.
2. **maxQueueSize** (default: 100): Oldest actions are dropped when the queue is full.
3. **WeakReference**: Platform implementations are weakly referenced and auto-released.

For 99% of use cases (Toast, Navigation, Permissions), these are sufficient. These settings can be configured per-instance using the `KRelay.builder()`.

---

## Why KRelay?

### Problem 1: Memory Leaks from Strong References

**Without KRelay:**
```kotlin
// ❌ DIY approach - Memory leak!
object MyBridge {
    var activity: Activity? = null  // Forgot to clear → LEAK
}
```

**With KRelay:**
```kotlin
// ✅ Automatic WeakReference - Zero leaks
override fun onCreate(savedInstanceState: Bundle?) {
    KRelay.register<ToastFeature>(AndroidToast(this))
    // Auto-cleanup when Activity destroyed
}
```

### Problem 2: Missed Commands During Lifecycle Changes

**Without KRelay:**
```kotlin
// ❌ Command missed if Activity not ready
viewModelScope.launch {
    val data = load()
    nativeBridge.showToast("Done") // Activity not created yet - event lost!
}
```

**With KRelay:**
```kotlin
// ✅ Sticky Queue - Commands preserved
viewModelScope.launch {
    val data = load()
    KRelay.dispatch<ToastFeature> { it.show("Done") }
    // Queued if Activity not ready → Auto-replays when ready
}
```

### Problem 3: Poor Testability & DI

**Without KRelay:**
```kotlin
// ❌ ViewModel coupled to a specific Navigator
class LoginViewModel(private val navigator: Navigator) {
    fun onLoginSuccess() {
        navigator.push(HomeScreen())
    }
}
// - Hard to test (requires a Navigator mock)
// - Can't switch navigation libraries easily
```

**With KRelay (v2.0):**
```kotlin
// ✅ ViewModel is pure, depends only on the KRelay contract
class LoginViewModel(private val krelay: KRelayInstance) {
    fun onLoginSuccess() {
        krelay.dispatch<NavigationFeature> { it.goToHome() }
    }
}

// - Easy testing: pass in a mock instance
// - DI-friendly: inject the correct instance
// - Switch Voyager → Decompose without touching the ViewModel
```

---

## Quick Start

### Installation

```kotlin
// In your shared module's build.gradle.kts
commonMain.dependencies {
    implementation("dev.brewkits:krelay:2.0.0")
}
```

### Basic Usage

**Step 1: Define Feature Contract (commonMain)**
This is the shared contract between your business logic and platform UI.

```kotlin
interface ToastFeature : RelayFeature {
    fun show(message: String)
}
```

---

#### **Option A: Singleton Usage (Simple)**
Perfect for single-module apps or maintaining backward compatibility.

**Step 2A: Use from Shared Code**

```kotlin
// ViewModel uses the global KRelay object
class LoginViewModel {
    fun onLoginSuccess() {
        // The @SuperAppWarning reminds you that this is a global singleton
        KRelay.dispatch<ToastFeature> { it.show("Welcome back!") }
    }
}
```

**Step 3A: Implement and Register on Platform**

```kotlin
// Android (in Activity)
class AndroidToast(private val context: Context) : ToastFeature { /*...*/ }

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRelay.register<ToastFeature>(AndroidToast(applicationContext))
}

// iOS (in UIViewController)
class IOSToast: ToastFeature { /*...*/ }

override func viewDidLoad() {
    super.viewDidLoad()
    KRelay.shared.register(impl: IOSToast(viewController: self))
}
```

---

#### **Option B: Instance Usage (DI & Super Apps)**
The recommended approach for new, multi-module, or DI-based projects.

**Step 2B: Create & Inject Instance**
Create a shared instance for your module or screen. Here, we use Koin as an example.

```kotlin
// In a Koin module (e.g., RideModule.kt)
val rideModule = module {
    single { KRelay.create("Rides") } // Create a scoped instance
    viewModel { RideViewModel(krelay = get()) }
}

// ViewModel receives the instance via constructor
class RideViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onBookingConfirmed() {
        krelay.dispatch<ToastFeature> { it.show("Ride booked!") }
    }
}
```

**Step 3B: Implement and Register on Platform**
The implementation is the same, but you register it with the specific instance.

```kotlin
// Android (in Activity)
val rideKRelay: KRelayInstance by inject() // from Koin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    rideKRelay.register<ToastFeature>(AndroidToast(applicationContext))
}

// iOS (in UIViewController)
let rideKRelay: KRelayInstance = koin.get() // from Koin
override func viewDidLoad() {
    super.viewDidLoad()
    rideKRelay.register(impl: IOSToast(viewController: self))
}
```

> **⚠️ Important Warnings:**
> - `@ProcessDeathUnsafe`: The queue is in-memory and lost on process death. This is safe for UI feedback (Toasts, Navigation), but not for critical data (payments).
> - `@SuperAppWarning`: This reminds you that the global `KRelay` object is a singleton. For modular apps, **use the instance-based API (Option B)** to prevent conflicts.
>
> See [Managing Warnings](docs/MANAGING_WARNINGS.md) to suppress at the module level.

---

## Key Features

### 📦 Instance-Based API (New in v2.0)
- **Super App Ready**: Create isolated `KRelayInstance`s for each module, preventing conflicts.
- **DI Friendly**: Inject instances into ViewModels and services.
- **Configurable**: Each instance can have its own queue size, expiry, and debug settings.

### 🛡️ Memory Safety
- **Automatic WeakReference** prevents Activity/ViewController leaks.
- No manual cleanup needed for 99% of use cases.

### 🔄 Sticky Queue
- Commands are never lost during configuration changes (e.g., screen rotation).
- Auto-replays queued commands when a platform implementation becomes available.

### 🧵 Thread Safety
- All commands execute on the Main/UI thread automatically.
- **Reentrant locks** on both platforms (Android & iOS) ensure safe concurrent access.
- **Stress-tested** with 100k+ concurrent operations.

### 🔌 Library Integration
- Decouples ViewModels from navigation libraries like Voyager, Decompose, and Compose Navigation.
- Integrates cleanly with permission handlers (Moko Permissions), image pickers (Peekaboo), and more.

### 🧪 Testability
- **Singleton**: `KRelay.reset()` provides a clean state for each test.
- **Instances**: Pass a mock `KRelayInstance` directly to your ViewModel for even easier and more explicit testing.
- No complex mocking libraries needed.

### ⚡ Performance
- Zero overhead when dispatching from the main thread.
- Efficient queue management and minimal memory footprint.

### 🔍 Diagnostic Tools
- **`dump()`**: A visual printout of the current state (registered features, queue depth).
- **`getDebugInfo()`**: Programmatic access to all diagnostic data.
- Real-time monitoring of registered features and queue depth.

---

## Core API

The Core API is consistent across the singleton and instances.

### Singleton API (Backward Compatible)
For quick setup or existing projects. All calls are delegated to a default instance.

```kotlin
// Register a feature on the default instance
KRelay.register<ToastFeature>(AndroidToast(context))

// Dispatch an action on the default instance
KRelay.dispatch<ToastFeature> { it.show("Hello from singleton!") }
```

### Instance API (New in v2.0)
For dependency injection, multi-module apps, and testability.

```kotlin
// Create a new, isolated instance
val rideKRelay = KRelay.create("Rides")

// Or, create a configured instance
val foodKRelay = KRelay.builder("Food")
    .maxQueueSize(20)
    .build()

// Register a feature on a specific instance
rideKRelay.register<ToastFeature>(RideToastImpl())

// Dispatch an action on that instance
rideKRelay.dispatch<ToastFeature> { it.show("Your ride is here!") }
```

### Common Functions
These functions are available on both the `KRelay` singleton and any `KRelayInstance`.

**Utility Functions:**
```kotlin
// On singleton
KRelay.isRegistered<ToastFeature>()
KRelay.getPendingCount<ToastFeature>()
KRelay.clearQueue<ToastFeature>()
KRelay.reset() // Resets the default instance

// On instance
val myRelay: KRelayInstance = get()
myRelay.isRegistered<ToastFeature>()
myRelay.getPendingCount<ToastFeature>()
myRelay.clearQueue<ToastFeature>()
myRelay.reset() // Resets only this instance
```

**Diagnostic Functions:**
```kotlin
// On singleton
KRelay.dump()
KRelay.getDebugInfo()

// On instance
val myRelay: KRelayInstance = get()
myRelay.dump()
myRelay.getDebugInfo()
```

---

## When to Use KRelay

### ✅ Perfect For (Recommended)

- **Navigation**: `KRelay.dispatch<NavFeature> { it.goToHome() }`
- **Toast/Snackbar**: Show user feedback
- **Permissions**: Request camera/location
- **Haptics/Sound**: Trigger vibration/audio
- **Analytics**: Fire-and-forget events
- **Notifications**: In-app banners

### ❌ Do NOT Use For

- **Return Values**: Use `expect/actual` instead
- **State Management**: Use `StateFlow`
- **Heavy Processing**: Use `Dispatchers.IO`
- **Database Ops**: Use Room/SQLite directly
- **Critical Transactions**: Use WorkManager
- **Network Requests**: Use Repository pattern

**Golden Rule**: KRelay is for **one-way, fire-and-forget UI commands**. If you need a return value or guaranteed execution after process death, use different tools.

---

## Important Limitations

### 1. Queue NOT Persistent (Process Death)

Lambda functions **cannot survive process death** (OS kills app).

**Impact:**
- ✅ **Safe**: Toast, Navigation, Haptics (UI feedback - acceptable to lose)
- ❌ **Dangerous**: Payments, Uploads, Critical Analytics (use WorkManager)

**Why?** Lambdas can't be serialized. When OS kills your app, the queue is cleared.

See [@ProcessDeathUnsafe](krelay/src/commonMain/kotlin/dev/brewkits/krelay/ProcessDeathUnsafe.kt) and [Anti-Patterns Guide](docs/ANTI_PATTERNS.md) for details.

### 2. Singleton vs. Instance API

KRelay provides two APIs, and choosing the right one is important.

**Singleton API (`KRelay.dispatch`)**
- **Pros**: Zero setup, easy to use, great for simple apps.
- **Cons**: Can cause feature conflicts in large, multi-module "Super Apps" if two modules use the same feature interface.
- **Use When**: Your app is a single module, or you are certain feature names will not conflict.

**Instance API (`KRelay.create(...)`)**
- **Pros**: Full isolation between modules, DI-friendly, configurable per-instance. **This is the solution for Super Apps.**
- **Cons**: Requires a small amount of setup (creating and providing the instance).
- **Use When**: Building a multi-module app, using dependency injection, or needing different configurations for different parts of your app.

See the `@SuperAppWarning` annotation and the "Quick Start" guide for examples of each.

---

## Documentation

### 📚 Guides
- **[Integration Guides](docs/INTEGRATION_GUIDES.md)** - Voyager, Moko, Peekaboo, Decompose
- **[Anti-Patterns](docs/ANTI_PATTERNS.md)** - What NOT to do (Super App examples)
- **[Testing Guide](docs/TESTING.md)** - How to test KRelay-based code
- **[Managing Warnings](docs/MANAGING_WARNINGS.md)** - Suppress `@OptIn` at module level

### 🏗️ Technical
- **[Architecture](docs/ARCHITECTURE.md)** - Deep dive into internals
- **[API Reference](docs/QUICK_REFERENCE.md)** - Complete API documentation
- **[ADR: Singleton Trade-offs](docs/adr/0001-singleton-and-serialization-tradeoffs.md)** - Design decisions

### 🎯 Understanding KRelay
- **[Positioning](docs/POSITIONING.md)** - Why KRelay exists (The Glue Code Standard)
- **[Roadmap](ROADMAP.md)** - Future development plans (Desktop, Web, v2.0)

---

## FAQ

### Q: Isn't this just EventBus? I remember the nightmare on Android...

**A:** We understand the PTSD! 😅 But KRelay is fundamentally different:

| Aspect | Old EventBus | KRelay |
|--------|-------------|--------|
| **Scope** | Global pub/sub across all components | **Strictly Shared ViewModel → Platform** (one direction) |
| **Memory Safety** | Manual lifecycle management → leaks everywhere | **Automatic WeakReference** - leak-free by design |
| **Direction** | Any-to-Any (spaghetti) | **Unidirectional** (ViewModel → View only) |
| **Discovery** | Events hidden in random places | **Type-safe interfaces** - clear contracts |
| **Use Case** | General messaging (wrong tool) | **KMP "Last Mile" problem** (right tool) |

**Key difference**: EventBus was used for component-to-component communication (wrong pattern). KRelay is for **ViewModel-to-Platform** bridge only (the missing piece in KMP).

---

### Q: How does KRelay v2.0 work with DI (Koin/Hilt)?

**A:** KRelay v2.0 is designed to integrate seamlessly with Dependency Injection frameworks. The new instance-based API allows you to register `KRelayInstance`s as providers in your DI graph and inject them where needed.

**KRelay complements DI** by solving the specific problem of bridging to **lifecycle-aware, Activity/UIViewController-scoped** UI actions (like navigation, dialogs, permissions) without leaking platform contexts into your ViewModels.

**Modern DI Approach (with KRelay v2.0):**
```kotlin
// 1. Provide a KRelay instance in your Koin/Hilt module
val appModule = module {
    single { KRelay.create("AppScope") } // Create an instance
    viewModel { LoginViewModel(krelay = get()) }
}

// 2. Inject the instance into your ViewModel
class LoginViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onLoginSuccess() {
        // ViewModel is pure and easily testable
        krelay.dispatch<NavigationFeature> { it.goToHome() }
    }
}

// 3. Register the implementation at the UI layer
class MyActivity : AppCompatActivity() {
    private val krelay: KRelayInstance by inject() // Inject the same instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        krelay.register<NavigationFeature>(AndroidNavigation(this))
    }
}
```

**When to use what:**
- **DI (Koin/Hilt)**: For managing the lifecycle of your dependencies, including repositories, use cases, and `KRelayInstance`s.
- **KRelay**: As the clean, lifecycle-safe bridge for dispatching commands from your DI-managed components to the UI layer.

---

### Q: Can't I just use `LaunchedEffect` + `SharedFlow`? Why add another library?

**A:** Absolutely! `LaunchedEffect` is lifecycle-aware and doesn't leak. KRelay solves two **different** problems:

**1. Boilerplate Reduction**

**Without KRelay:**
```kotlin
// ViewModel
class LoginViewModel {
    private val _navEvents = MutableSharedFlow<NavEvent>()
    val navEvents = _navEvents.asSharedFlow()

    fun onSuccess() {
        viewModelScope.launch {
            _navEvents.emit(NavEvent.GoHome)
        }
    }
}

// Every screen needs this collector
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is NavEvent.GoHome -> navigator.push(HomeScreen())
                // ... handle all events
            }
        }
    }
}
```

**With KRelay:**
```kotlin
// ViewModel
class LoginViewModel {
    fun onSuccess() {
        KRelay.dispatch<NavFeature> { it.goToHome() }
    }
}

// One-time registration in MainActivity
override fun onCreate(savedInstanceState: Bundle?) {
    KRelay.register<NavFeature>(VoyagerNav(navigator))
}
```

**2. Missed Events During Rotation**

If you dispatch an event **during rotation** (between old Activity destroy → new Activity create), `LaunchedEffect` isn't running yet → **event lost**.

KRelay's **Sticky Queue** catches these events and replays them when the new Activity is ready.

**Trade-off**: If you only have 1-2 features and prefer explicit Flow collectors, stick with `LaunchedEffect`. If you have many platform actions (Toast, Nav, Permissions, Haptics), KRelay reduces boilerplate significantly.

---

## Testing

KRelay is designed for testability. The v2.0 instance API makes testing even cleaner.

### Testing with the Singleton API
If you use the `KRelay` singleton, you can use `KRelay.reset()` to ensure a clean state between tests.

```kotlin
class LoginViewModelTest {
    @BeforeTest
    fun setup() {
        KRelay.reset() // Clears the default instance's registry and queue
    }

    @Test
    fun `when login success, dispatches toast and nav commands`() {
        // Arrange: Register mock implementations on the global object
        val mockToast = MockToast()
        val mockNav = MockNav()
        KRelay.register<ToastFeature>(mockToast)
        KRelay.register<NavigationFeature>(mockNav)
        
        val viewModel = LoginViewModel() // Assumes ViewModel uses KRelay singleton

        // Act
        viewModel.onLoginSuccess()

        // Assert
        assertEquals("Welcome back!", mockToast.lastMessage)
        assertTrue(mockNav.navigatedToHome)
    }
}
```

### Testing with the Instance API (Recommended)
This is the modern, recommended approach. It avoids global state and makes dependencies explicit.

```kotlin
class RideViewModelTest {
    private lateinit var mockRelay: KRelayInstance
    private lateinit var viewModel: RideViewModel

    @BeforeTest
    fun setup() {
        // Create a fresh instance for each test
        mockRelay = KRelay.create("TestScope")
        viewModel = RideViewModel(krelay = mockRelay)
    }

    @Test
    fun `when booking confirmed, dispatches confirmation toast`() {
        // Arrange: Register a mock feature on the instance
        val mockToast = MockToast()
        mockRelay.register<ToastFeature>(mockToast)

        // Act
        viewModel.onBookingConfirmed()

        // Assert
        assertEquals("Ride booked!", mockToast.lastMessage)
    }
}
```

**Shared Mock Implementations:**
```kotlin
// A simple mock used in the tests above
class MockToast : ToastFeature {
    var lastMessage: String? = null
    override fun show(message: String) {
        lastMessage = message
    }
}

class MockNav : NavigationFeature {
    var navigatedToHome: Boolean = false
    override fun goToHome() {
        navigatedToHome = true
    }
}
```

Run tests:
```bash
./gradlew :krelay:testDebugUnitTest        # Android
./gradlew :krelay:iosSimulatorArm64Test    # iOS Simulator
```

---

## Demo App

The project includes a demo app showcasing real integrations:

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**Features:**
- Basic Demo: Core KRelay features
- Voyager Integration: Real navigation library integration

See `composeApp/src/commonMain/kotlin/dev/brewkits/krelay/` for complete examples.

---

## Compatibility

### Version Matrix

| KRelay | Kotlin | KMP | AGP | Android minSdk | iOS min |
|--------|--------|-----|-----|----------------|---------|
| 2.0.0  | 2.3.x  | 2.3.x | 8.x | 24 | 14.0 |
| 1.1.0  | 2.0.x  | 2.0.x | 8.x | 23 | 13.0 |
| 1.0.0  | 1.9.x  | 1.9.x | 7.x | 21 | 13.0 |

### API Compatibility

| KRelay | Singleton API | Instance API | Priority Dispatch |
|--------|--------------|--------------|-------------------|
| 2.0.x  | ✅ Full      | ✅ Full      | ✅ Singleton + Instance |
| 1.1.x  | ✅ Full      | ❌           | ✅ Singleton only |
| 1.0.x  | ✅ Full      | ❌           | ❌ |

### Platforms Supported

| Platform | v1.0 | v1.1 | v2.0 |
|----------|------|------|------|
| Android (arm64, x86_64) | ✅ | ✅ | ✅ |
| iOS arm64 (device) | ✅ | ✅ | ✅ |
| iOS arm64 (simulator) | ✅ | ✅ | ✅ |
| iOS x64 (simulator) | ✅ | ✅ | ✅ |
| JVM (unit tests) | ✅ | ✅ | ✅ |

---

## Philosophy: Do One Thing Well

KRelay follows Unix philosophy - it has **one responsibility**:

> Guarantee safe, leak-free dispatch of UI commands from shared code to platform.

**What KRelay Is:**
- ✅ A messenger for one-way UI commands
- ✅ Fire-and-forget pattern
- ✅ Lifecycle-aware bridge

**What KRelay Is NOT:**
- ❌ RPC framework (no request-response)
- ❌ State management (use StateFlow)
- ❌ Background worker (use WorkManager)
- ❌ DI framework (use Koin/Hilt)

By staying focused, KRelay remains simple, reliable, and maintainable.

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

```
Copyright 2026 Brewkits

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## ⭐ Star Us on GitHub!

If KRelay saves you time, please give us a star!

It helps other developers discover this project.

---

[⬆️ Back to Top](#krelay)

---

Made with ❤️ by **Nguyễn Tuấn Việt** at [Brewkits](https://brewkits.dev)

**Support:** datacenter111@gmail.com • **Community:** [GitHub Issues](https://github.com/brewkits/krelay/issues)

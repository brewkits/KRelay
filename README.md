# KRelay

> **The Glue Code Standard for Kotlin Multiplatform**
>
> Safe, leak-free bridge between shared code and platform-specific APIs

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg?style=flat&logo=kotlin)](http://kotlinlang.org)
[![Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-orange.svg?style=flat)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/dev.brewkits/krelay.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/dev.brewkits/krelay)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-zero-success.svg)](https://github.com/brewkits/krelay/blob/main/krelay/build.gradle.kts)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

---

## What is KRelay?

KRelay is a lightweight bridge that connects your shared Kotlin code to platform-specific implementations (Android/iOS) without memory leaks or lifecycle complexity.

**Perfect for**: Toast/Snackbar, Navigation, Permissions, Haptics, Notifications - any one-way UI command from shared ViewModels.

**Key insight**: Integrating KMP libraries (Voyager, Moko, Peekaboo) into shared ViewModels traditionally requires complex patterns. KRelay provides a simple, type-safe bridge.

```kotlin
// Shared ViewModel - Zero platform dependencies
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Welcome!") }
        KRelay.dispatch<NavigationFeature> { it.goToHome() }
    }
}
```

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

### Problem 3: Complex Integration with KMP Libraries

**Without KRelay:**
```kotlin
// ❌ ViewModel coupled to Voyager Navigator
class LoginViewModel(private val navigator: Navigator) {
    fun onLoginSuccess() {
        navigator.push(HomeScreen())
    }
}
// - Hard to test (need Navigator mock)
// - Can't switch navigation libraries
```

**With KRelay:**
```kotlin
// ✅ ViewModel stays pure Kotlin
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<NavigationFeature> { it.goToHome() }
    }
}
// - Easy testing with simple mock
// - Switch Voyager → Decompose without touching ViewModel
```

---

## Quick Start

### Installation

**Option 1: Maven Central (Recommended for published library)**

```kotlin
// In your shared module's build.gradle.kts
commonMain.dependencies {
    implementation("dev.brewkits:krelay:1.0.1")
}
```

**Option 2: Local Project Reference**

```kotlin
// In your shared module's build.gradle.kts
commonMain.dependencies {
    implementation(project(":krelay"))
}
```

### Basic Usage

**Step 1: Define Feature Contract (commonMain)**

```kotlin
interface ToastFeature : RelayFeature {
    fun show(message: String)
}
```

**Step 2: Use from Shared Code**

```kotlin
class LoginViewModel {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onLoginSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Welcome back!") }
    }
}
```

> **⚠️ Important Warnings:**
> - `@ProcessDeathUnsafe`: Queue is lost on process death (safe for UI feedback, NOT for payments)
> - `@SuperAppWarning`: Global singleton (use feature namespacing in large apps)
>
> See [Managing Warnings](docs/MANAGING_WARNINGS.md) to suppress at module level.

**Step 3: Implement on Android**

```kotlin
class AndroidToast(private val context: Context) : ToastFeature {
    override fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// In Activity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRelay.register<ToastFeature>(AndroidToast(applicationContext))
}
```

**Step 4: Implement on iOS**

```swift
class IOSToast: ToastFeature {
    weak var viewController: UIViewController?

    func show(message: String) {
        let alert = UIAlertController(title: nil, message: message,
                                     preferredStyle: .alert)
        viewController?.present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            alert.dismiss(animated: true)
        }
    }
}

// Register
KRelay.shared.register(impl: IOSToast(viewController: controller))
```

---

## Key Features

### 🛡️ Memory Safety
- **Automatic WeakReference** prevents Activity/ViewController leaks
- No manual cleanup needed
- Proven zero-leak in production apps

### 📦 Sticky Queue
- Commands never lost during rotation/lifecycle changes
- Auto-replay when platform implementation registers
- Configurable queue size and expiry

### 🧵 Thread Safety
- All commands execute on Main/UI thread automatically
- Lock-based concurrency control
- No `CalledFromWrongThreadException`

### 🔌 Library Integration
- Works with Voyager, Decompose, Compose Navigation
- Integrates Moko Permissions, Peekaboo, Play Core
- Clean decoupling from platform libraries

### 🧪 Testability
- Simple mock implementations for tests
- No mocking library needed
- Easy to verify dispatched commands

### ⚡ Performance
- Zero overhead when on main thread
- Efficient queue management
- Minimal memory footprint

---

## Core API

### Register Implementation
```kotlin
KRelay.register<ToastFeature>(AndroidToast(context))
```

### Dispatch Command
```kotlin
KRelay.dispatch<ToastFeature> { it.show("Hello") }
```

### Utility Functions
```kotlin
KRelay.isRegistered<ToastFeature>()        // Check if registered
KRelay.getPendingCount<ToastFeature>()     // Count queued actions
KRelay.unregister<ToastFeature>()          // Manual unregister (optional)
KRelay.clearQueue<ToastFeature>()          // Clear pending actions
KRelay.reset()                              // Clear all (for testing)
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

### 2. Global Singleton

KRelay uses `object KRelay` singleton pattern.

**Impact:**
- ✅ **Perfect for**: Single-module apps, small-medium projects
- ⚠️ **Caution**: Super Apps (Grab/Gojek style) - use Feature Namespacing

**Workaround for large apps:**
```kotlin
// Namespace your features
interface ModuleAToastFeature : RelayFeature { ... }
interface ModuleBToastFeature : RelayFeature { ... }
```

See [@SuperAppWarning](krelay/src/commonMain/kotlin/dev/brewkits/krelay/SuperAppWarning.kt) for guidance.

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

### Q: Why not just use DI (Koin/Hilt) to inject platform helpers?

**A:** DI is perfect for `ApplicationContext`-scoped helpers (file paths, toasts). **KRelay complements DI, doesn't replace it.**

The problem arises with **Activity-scoped actions** (Navigation, Dialogs, Permissions):

**DI Approach:**
```kotlin
// ❌ Can't inject Activity into ViewModel - memory leak!
class LoginViewModel(private val activity: Activity) // LEAK!

// ⚠️ Need manual WeakReference + attach/detach boilerplate
interface ViewAttached { fun attach(activity: Activity) }
```

**KRelay Approach:**
```kotlin
// ✅ ViewModel stays pure - no Activity reference
class LoginViewModel {
    fun onSuccess() {
        KRelay.dispatch<NavFeature> { it.goToHome() }
    }
}

// Activity registers itself automatically
override fun onCreate(savedInstanceState: Bundle?) {
    KRelay.register<NavFeature>(AndroidNav(this))
}
```

**When to use what:**
- **DI**: For stateless helpers, repositories, use cases (ApplicationContext-scoped)
- **KRelay**: For Activity-scoped UI actions (Navigation, Permissions, Dialogs)

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

```kotlin
class LoginViewModelTest {
    @BeforeTest
    fun setup() {
        KRelay.reset() // Clean state
    }

    @Test
    fun `when login success should show toast and navigate`() {
        // Arrange: Register mock implementations
        val mockToast = MockToast()
        val mockNav = MockNav()
        KRelay.register<ToastFeature>(mockToast)
        KRelay.register<NavigationFeature>(mockNav)

        // Act: Trigger login
        viewModel.onLoginSuccess(testUser)

        // Assert: Verify commands dispatched
        assertEquals("Welcome back!", mockToast.lastMessage)
        assertTrue(mockNav.navigatedToHome)
    }
}

class MockToast : ToastFeature {
    var lastMessage: String? = null
    override fun show(message: String) { lastMessage = message }
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

# ⚡ KRelay

![KRelay Cover](rrelay.png)

> **The missing piece in Kotlin Multiplatform.**
> Call Toasts, navigate screens, request permissions — anything native — directly from your shared ViewModel. No leaks. No crashes. No boilerplate.

[![Maven Central](https://img.shields.io/maven-central/v/dev.brewkits/krelay.svg?label=Maven%20Central&color=brightgreen)](https://central.sonatype.com/artifact/dev.brewkits/krelay)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.x-blue.svg?style=flat&logo=kotlin)](http://kotlinlang.org)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-orange.svg?style=flat)](https://kotlinlang.org/docs/multiplatform.html)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-zero-success.svg)](krelay/build.gradle.kts)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

---

## 🛑 Sound familiar?

You've written a clean, shared `ViewModel`. Then you need to show a permission dialog, navigate to the next screen, or open an image picker. And you hit the wall:

```kotlin
class ProfileViewModel : ViewModel() {
    fun updateAvatar() {
        // ❌ Can't pass Activity — memory leak waiting to happen
        // ❌ Can't pass UIViewController — platform dependency in shared code
        // ❌ SharedFlow loses events during screen rotation
        // ❌ expect/actual is overkill for a one-liner
        // 😤 So... what do you do?
    }
}
```

This is the **"Last Mile" problem** of KMP. Your business logic is clean and shared — but the moment you need to trigger something native, you're stuck choosing between leaks, boilerplate, or coupling.

---

## ✅ KRelay solves it in 3 steps

**Step 1 — Define a shared contract (`commonMain`)**

```kotlin
interface MediaFeature : RelayFeature {
    fun pickImage()
}
```

**Step 2 — Dispatch from your ViewModel**

```kotlin
class ProfileViewModel : ViewModel() {
    fun updateAvatar() {
        KRelay.dispatch<MediaFeature> { it.pickImage() }
        // ✅ Zero platform deps  ✅ Zero leaks  ✅ Queued if UI isn't ready yet
    }
}
```

**Step 3 — Register the real implementation on each platform**

```kotlin
// Android
KRelay.register<MediaFeature>(PeekabooMediaImpl(activity))

// iOS (Swift)
KRelay.shared.register(impl: IOSMediaImpl())
```

**That's it.** KRelay handles lifecycle safety, main-thread dispatch, queue management, and cleanup automatically.

---

## Why developers choose KRelay

### 🛡️ Zero memory leaks — by design

Implementations are held as `WeakReference`. When your Activity or UIViewController is destroyed, KRelay releases it automatically. No `null` checks. No `onDestroy` cleanup for 99% of use cases.

### 🔄 Events survive screen rotation

Commands dispatched while the UI isn't ready are queued and **automatically replayed** when a new implementation registers. Your user rotated the screen mid-API-call? The navigation event still arrives.

### 🧵 Always runs on the Main Thread

Dispatch from any background coroutine. KRelay guarantees UI code always executes on the Main Thread — Android Looper and iOS GCD both handled.

---

## Works with your stack

KRelay is the glue layer — it integrates with whatever libraries you already use, keeping your ViewModels free of framework dependencies:

| Category | Works with |
|----------|-----------|
| 🧭 Navigation | [Voyager](docs/INTEGRATION_GUIDES.md), [Decompose](docs/INTEGRATION_GUIDES.md), Navigation Compose |
| 📷 Media | [Peekaboo](docs/INTEGRATION_GUIDES.md) image/camera picker |
| 🔐 Permissions | [Moko Permissions](docs/INTEGRATION_GUIDES.md) |
| 🔒 Biometrics | [Moko Biometry](docs/INTEGRATION_GUIDES.md) |
| ⭐ Reviews | Play Core (Android), StoreKit (iOS) |
| 💉 DI | Koin, Hilt — inject `KRelayInstance` into ViewModels |
| 🎨 Compose | Built-in `KRelayEffect<T>` and `rememberKRelayImpl<T>` helpers |

**Your ViewModels stay pure** — zero direct dependencies on Voyager, Decompose, Moko, or any platform library.

→ See [Integration Guides](docs/INTEGRATION_GUIDES.md) for step-by-step examples.

---

## Quick Start

### Installation

```kotlin
// shared module build.gradle.kts
commonMain.dependencies {
    implementation("dev.brewkits:krelay:2.1.0")
}
```

### Option A — Singleton (simple apps)

Perfect for single-module apps or getting started fast.

```kotlin
// 1. Define the contract (commonMain)
interface ToastFeature : RelayFeature {
    fun show(message: String)
}

// 2. Dispatch from shared ViewModel
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Welcome back!") }
    }
}

// 3A. Register on Android
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRelay.register<ToastFeature>(object : ToastFeature {
        override fun show(message: String) =
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    })
}

// 3B. Register on iOS (Swift)
override func viewDidLoad() {
    super.viewDidLoad()
    KRelay.shared.register(impl: IOSToast(viewController: self))
}
```

### Option B — Instance API (DI & multi-module)

The recommended approach for new projects, Koin/Hilt, and modular "Super Apps." Each module gets its own isolated instance — no conflicts between modules.

```kotlin
// Koin module setup
val rideModule = module {
    single { KRelay.create("Rides") }        // isolated instance
    viewModel { RideViewModel(krelay = get()) }
}

// ViewModel — pure, no framework deps
class RideViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onBookingConfirmed() {
        krelay.dispatch<ToastFeature> { it.show("Ride booked!") }
    }
}

// Android Activity
val rideKRelay: KRelayInstance by inject()
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    rideKRelay.register<ToastFeature>(AndroidToast(applicationContext))
}
```

> **Compose Multiplatform users:** Use the built-in `KRelayEffect<T>` helper for zero-boilerplate, lifecycle-scoped registration:
> ```kotlin
> KRelayEffect<ToastFeature> { AndroidToastImpl(context) }
> // auto-unregisters when the composable leaves
> ```
> See [Compose Integration Guide](docs/COMPOSE_INTEGRATION.md).

> **⚠️ Warnings:** `@ProcessDeathUnsafe` and `@SuperAppWarning` are compile-time reminders.
> See [Managing Warnings](docs/MANAGING_WARNINGS.md) to suppress them at module level.

---

## ❌ When NOT to use KRelay

KRelay is for **one-way, fire-and-forget UI commands**. Be honest with yourself:

| Use Case | Better Tool |
|----------|-------------|
| Need a return value | `expect/actual` or `suspend fun` |
| State management | `StateFlow` / `MutableStateFlow` |
| Critical data — payments, uploads | `WorkManager` / background services |
| Database operations | Room / SQLDelight |
| Network requests | Repository + Ktor |
| Heavy background work | `Dispatchers.IO` |

**Golden Rule**: If you need a return value or guaranteed persistence across process death, use a different tool.

---

## Core API

The API is identical on the singleton and on any instance.

### Singleton

```kotlin
KRelay.register<ToastFeature>(AndroidToast(context))
KRelay.dispatch<ToastFeature> { it.show("Hello!") }
KRelay.unregister<ToastFeature>()
KRelay.isRegistered<ToastFeature>()
KRelay.getPendingCount<ToastFeature>()
KRelay.clearQueue<ToastFeature>()
KRelay.reset()   // clear registry + queue
KRelay.dump()    // print debug state
```

### Instance API

```kotlin
val krelay = KRelay.create("MyScope")           // isolated instance
// or
val krelay = KRelay.builder("MyScope")
    .maxQueueSize(50)
    .actionExpiryMs(30_000)
    .build()

krelay.register<ToastFeature>(impl)
krelay.dispatch<ToastFeature> { it.show("Hello!") }
krelay.reset()
krelay.dump()
```

### Scope Token API — fine-grained cleanup

```kotlin
class MyViewModel : ViewModel() {
    private val token = KRelay.scopedToken()

    fun doWork() {
        KRelay.dispatch<WorkFeature>(token) { it.run("task") }
    }

    override fun onCleared() {
        KRelay.cancelScope(token)  // removes only this ViewModel's queued actions
    }
}
```

---

## Memory Management

### Lambda capture rules

```kotlin
// ✅ DO: capture primitives and data
val message = viewModel.successMessage
KRelay.dispatch<ToastFeature> { it.show(message) }

// ❌ DON'T: capture ViewModels or Contexts
KRelay.dispatch<ToastFeature> { it.show(viewModel.data) }  // captures viewModel!
```

### Built-in protections (passive — always active)

| Protection | Default | Effect |
|-----------|---------|--------|
| `actionExpiryMs` | 5 min | Old queued actions auto-expire |
| `maxQueueSize` | 100 | Oldest actions dropped when queue fills |
| `WeakReference` | Always | Platform impls released on GC automatically |

These are sufficient for 99% of use cases. Customize per-instance with `KRelay.builder()`.

---

## Testing

### Singleton API

```kotlin
@BeforeTest
fun setup() {
    KRelay.reset()  // clean state for each test
}

@Test
fun `login success dispatches toast and navigation`() {
    val mockToast = MockToast()
    KRelay.register<ToastFeature>(mockToast)

    LoginViewModel().onLoginSuccess()

    assertEquals("Welcome back!", mockToast.lastMessage)
}
```

### Instance API (recommended — explicit, no global state)

```kotlin
@BeforeTest
fun setup() {
    mockRelay = KRelay.create("TestScope")
    viewModel = RideViewModel(krelay = mockRelay)
}

@Test
fun `booking confirmed dispatches toast`() {
    val mockToast = MockToast()
    mockRelay.register<ToastFeature>(mockToast)

    viewModel.onBookingConfirmed()

    assertEquals("Ride booked!", mockToast.lastMessage)
}
```

```kotlin
// Simple mocks — no mocking libraries needed
class MockToast : ToastFeature {
    var lastMessage: String? = null
    override fun show(message: String) { lastMessage = message }
}
```

Run tests:

```bash
./gradlew :krelay:testDebugUnitTest        # JVM (fast)
./gradlew :krelay:iosSimulatorArm64Test    # iOS Simulator
./gradlew :krelay:connectedDebugAndroidTest  # Real Android device
```

**237 unit tests** · **19 instrumented tests** · Tested on JVM, iOS Simulator (arm64), and real Android device (Pixel 6 Pro, Android 16).

---

## FAQ

### Q: Isn't this just EventBus? I remember the nightmare...

**A:** KRelay is fundamentally different:

| Aspect | Old EventBus | KRelay |
|--------|-------------|--------|
| **Direction** | Any-to-Any (spaghetti) | **Unidirectional**: ViewModel → Platform only |
| **Memory** | Manual lifecycle → leaks everywhere | **Automatic WeakReference** — leak-free by design |
| **Contracts** | Stringly-typed events hidden anywhere | **Type-safe interfaces** — explicit, discoverable |
| **Scope** | Global pub/sub | **Strictly ViewModel → UI layer** |
| **Purpose** | General messaging (wrong tool) | **KMP "Last Mile" bridge** (right tool) |

### Q: Can't I just use `LaunchedEffect` + `SharedFlow`?

**A:** Yes, and for 1–2 simple cases that's fine. KRelay shines when you have many platform actions and need:

1. **Less boilerplate** — no `MutableSharedFlow` per feature, no `collect {}` per screen
2. **Rotation safety** — `LaunchedEffect` stops collecting between `onDestroy` and `onCreate`; KRelay's sticky queue covers the gap

```kotlin
// Without KRelay: boilerplate per feature, per screen
class LoginViewModel {
    private val _navEvents = MutableSharedFlow<NavEvent>()
    val navEvents = _navEvents.asSharedFlow()
    fun onSuccess() { viewModelScope.launch { _navEvents.emit(NavEvent.GoHome) } }
}

@Composable
fun LoginScreen(vm: LoginViewModel) {
    LaunchedEffect(Unit) { vm.navEvents.collect { when(it) { ... } } }
}

// With KRelay: register once, dispatch anywhere
class LoginViewModel {
    fun onSuccess() { KRelay.dispatch<NavFeature> { it.goToHome() } }
}
```

### Q: How does it work with DI (Koin/Hilt)?

**A:** Create a `KRelayInstance` as a scoped singleton in your DI module and inject it into both the ViewModel (dispatch) and the UI layer (register):

```kotlin
// Koin
val appModule = module {
    single { KRelay.create("AppScope") }
    viewModel { LoginViewModel(krelay = get()) }
}

// ViewModel
class LoginViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onLoginSuccess() { krelay.dispatch<NavigationFeature> { it.goToHome() } }
}

// Activity
class MyActivity : AppCompatActivity() {
    private val krelay: KRelayInstance by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        krelay.register<NavigationFeature>(AndroidNavigation(this))
    }
}
```

---

## Demo App

The repo includes a runnable demo covering all major features:

```bash
./gradlew :composeApp:installDebug
```

| Demo | What it shows |
|------|--------------|
| Basic | Core dispatch, queue, WeakRef behavior |
| Voyager Integration | Navigation across screens without Voyager in ViewModel |
| Decompose Integration | Component-based navigation, same pattern |
| Library Integrations | Moko Permissions, Biometry, Peekaboo, In-app Review |
| Super App Demo | Multiple isolated `KRelayInstance`s, no conflicts |

---

## Compatibility

### Version Matrix

| KRelay | Kotlin | KMP | AGP | Android minSdk | iOS min |
|--------|--------|-----|-----|----------------|---------|
| 2.1.0  | 2.3.x  | 2.3.x | 8.x | 24 | 14.0 |
| 2.0.0  | 2.3.x  | 2.3.x | 8.x | 24 | 14.0 |
| 1.1.0  | 2.0.x  | 2.0.x | 8.x | 23 | 13.0 |
| 1.0.0  | 1.9.x  | 1.9.x | 7.x | 21 | 13.0 |

### API Compatibility

| KRelay | Singleton | Instance API | Priority Dispatch | Compose Helpers | Persistent Dispatch |
|--------|-----------|--------------|-------------------|-----------------|---------------------|
| 2.1.x  | ✅        | ✅           | ✅ Both           | ✅ `KRelayEffect`, `rememberKRelayImpl` | ✅ |
| 2.0.x  | ✅        | ✅           | ✅ Both           | ❌              | ✅                  |
| 1.1.x  | ✅        | ❌           | ✅ Singleton      | ❌              | ❌                  |
| 1.0.x  | ✅        | ❌           | ❌               | ❌              | ❌                  |

### Platforms

| Platform | v1.0 | v1.1 | v2.0 | v2.1 |
|----------|:----:|:----:|:----:|:----:|
| Android (arm64, x86_64) | ✅ | ✅ | ✅ | ✅ |
| iOS arm64 (device) | ✅ | ✅ | ✅ | ✅ |
| iOS arm64 (simulator) | ✅ | ✅ | ✅ | ✅ |
| iOS x64 (simulator) | ✅ | ✅ | ✅ | ✅ |
| JVM (unit tests) | ✅ | ✅ | ✅ | ✅ |

---

## What's New

<details>
<summary><strong>v2.1.0 — Compose Integration & Hardening</strong></summary>

- Built-in `KRelayEffect<T>` and `rememberKRelayImpl<T>` Compose helpers
- `KRelay.instance` public property for cross-module access
- Persistent dispatch with `SharedPreferencesPersistenceAdapter` (Android) and `NSUserDefaultsPersistenceAdapter` (iOS)
- Scope Token API: `scopedToken()` / `cancelScope(token)`
- 237 unit tests + 19 instrumented tests — all passing
- Voyager demo fixed (Voyager 1.1.0-beta03, no more lifecycle crashes)
- Android 15+ 16KB page alignment compatibility
- `KRelayMetrics` wiring fixed; iOS KClass bridging fixed

See [CHANGELOG.md](CHANGELOG.md) and [RELEASE_NOTES_2.1.0.md](RELEASE_NOTES_2.1.0.md) for full details.

</details>

<details>
<summary><strong>v2.0.0 — Instance API for Super Apps</strong></summary>

- `KRelay.create("ScopeName")` — create isolated instances per module
- `KRelay.builder(...)` — configure queue size, expiry, debug mode per instance
- DI-friendly: inject `KRelayInstance` into ViewModels
- 100% backward compatible with v1.x

See [CHANGELOG.md](CHANGELOG.md) for full details.

</details>

---

## Documentation

### Guides
- **[Integration Guides](docs/INTEGRATION_GUIDES.md)** — Voyager, Decompose, Moko, Peekaboo
- **[Compose Integration](docs/COMPOSE_INTEGRATION.md)** — `KRelayEffect`, `rememberKRelayImpl`, Navigation patterns
- **[SwiftUI Integration](docs/SWIFTUI_INTEGRATION.md)** — iOS-specific patterns, XCTest
- **[Lifecycle Guide](docs/LIFECYCLE.md)** — Android (Activity/Fragment/Compose) and iOS (UIViewController/SwiftUI)
- **[DI Integration](docs/DI_INTEGRATION.md)** — Koin and Hilt setup
- **[Testing Guide](docs/TESTING.md)** — Best practices for testing KRelay-based code
- **[Anti-Patterns](docs/ANTI_PATTERNS.md)** — What NOT to do
- **[Managing Warnings](docs/MANAGING_WARNINGS.md)** — Suppress `@OptIn` at module level

### Technical
- **[Architecture](docs/ARCHITECTURE.md)** — Internals deep dive
- **[API Reference](docs/QUICK_REFERENCE.md)** — Full API cheat sheet
- **[Migration to v2.0](docs/MIGRATION_V2.md)** — From v1.x

---

## Philosophy

KRelay does **one thing**:

> Guarantee safe, leak-free dispatch of UI commands from shared code to platform — on any thread, across any lifecycle.

It is not a state manager, not an RPC framework, not a DI framework. By staying focused, it stays simple, reliable, and easy to delete if you ever outgrow it.

---

## Contributing

Contributions welcome! Please submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## License

```
Copyright 2026 Brewkits

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

[⬆️ Back to Top](#-krelay)

Made with ❤️ by **Nguyễn Tuấn Việt** at [Brewkits](https://brewkits.dev)

**Support:** datacenter111@gmail.com · **Issues:** [GitHub Issues](https://github.com/brewkits/krelay/issues)

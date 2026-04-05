<div align="center">

<img src="https://raw.githubusercontent.com/brewkits/krelay/main/assets/logo.svg" width="96" alt="KRelay logo" />

# KRelay

### Type-safe native interop bridge for Kotlin Multiplatform.

Dispatch UI commands (Toast, Navigation, Permissions) from shared ViewModels to Android and iOS — leak-free, rotation-safe, always on the Main Thread.

[![Maven Central](https://img.shields.io/maven-central/v/dev.brewkits/krelay.svg?label=Maven%20Central&color=brightgreen)](https://central.sonatype.com/artifact/dev.brewkits/krelay)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.x-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-zero-brightgreen.svg)](krelay/build.gradle.kts)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

</div>

---

## The problem

Shared ViewModels can't safely touch platform APIs. Every approach has a catch:

| Approach | Problem |
|---|---|
| Pass `Activity` / `UIViewController` | Memory leak |
| `SharedFlow` + `collect {}` | Events lost on rotation |
| `expect/actual` | Wires up a whole file for a one-liner |
| `LiveData` / `StateFlow` as event | Complex, miss-able |

KRelay is none of the above. It is a **typed bridge**: the ViewModel signals an intent, the platform fulfills it.

---

## Install

```kotlin
// shared/build.gradle.kts
commonMain.dependencies {
    implementation("dev.brewkits:krelay:2.1.1")
    implementation("dev.brewkits:krelay-compose:2.1.1") // Compose helpers (optional)
}
```

---

## Quickstart

**1. Define a contract in `commonMain`**

```kotlin
interface ToastFeature : RelayFeature {
    fun show(message: String)
}
```

**2. Dispatch from your ViewModel**

```kotlin
class LoginViewModel : ViewModel() {
    fun onLoginSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Welcome back!") }
        // Zero platform imports. Zero leaks. Queued if the UI isn't ready yet.
    }
}
```

**3. Register the platform implementation**

```kotlin
// Android — Activity or Composable
KRelay.register<ToastFeature>(object : ToastFeature {
    override fun show(message: String) =
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
})
```

```swift
// iOS — Swift
let toastClass = KRelayKClassHelpersKt.toastFeatureKClass()
KRelayIosHelperKt.registerFeature(
    instance: KRelay.shared.instance,
    kClass:   toastClass,
    impl:     IOSToast(viewController: self)
)
```

That's all the wiring needed. KRelay routes the call to the Main Thread, replays it if the UI wasn't ready, and releases the implementation when it's GC'd.

---

## How it works

```
ViewModel                KRelay                   Platform
─────────────────────────────────────────────────────────────
dispatch<Toast> { ... } ──► impl registered?
                             ├── yes: runOnMain { block(impl) }
                             └── no:  sticky queue ──► replay on register()
```

Three guarantees, always active:

- **WeakReference registry** — implementations are never strongly held; no `onDestroy` cleanup needed for 99% of cases.
- **Sticky queue** — actions dispatched before registration are held and replayed automatically. Screen rotation, async init, cold start — all covered.
- **Main Thread dispatch** — regardless of which thread `dispatch` is called from, the block executes on Android's `Looper.mainLooper()` / iOS's GCD main queue.

---

## Core API

The API is identical on the global singleton and on any isolated instance.

```kotlin
// Registration
KRelay.register<ToastFeature>(impl)
KRelay.unregister<ToastFeature>()          // unconditional
KRelay.unregister<ToastFeature>(impl)      // identity-safe (won't clear a newer registration)
KRelay.isRegistered<ToastFeature>()

// Dispatch
KRelay.dispatch<ToastFeature> { it.show("Hello") }
KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) { it.show("Error!") }

// Queue management
KRelay.getPendingCount<ToastFeature>()
KRelay.clearQueue<ToastFeature>()

// Scope tokens — cancel queued actions by caller identity
val token = scopedToken()
KRelay.dispatch<ToastFeature>(token) { it.show("...") }
KRelay.cancelScope(token)              // in ViewModel.onCleared()

// Debug
KRelay.dump()
KRelay.debugMode = true
```

### Priority dispatch

When multiple actions queue up before an implementation registers, higher-priority actions replay first. On overflow, the lowest-priority action is evicted (not just the oldest).

```kotlin
KRelay.dispatchWithPriority<NavFeature>(ActionPriority.HIGH)     { it.goToHome() }
KRelay.dispatchWithPriority<NavFeature>(ActionPriority.CRITICAL) { it.showError("Timeout") }
// ActionPriority: LOW(0)  NORMAL(50)  HIGH(100)  CRITICAL(1000)
```

### Persistent dispatch

Survives process death. The action is saved to `SharedPreferences` (Android) or `NSUserDefaults` (iOS) and restored on next launch.

```kotlin
// Register a factory to reconstruct the action from its payload
instance.registerActionFactory<ToastFeature>("toast", "show") { payload ->
    { feature -> feature.show(payload) }
}

// Dispatch — persisted to disk if no impl is available
instance.dispatchPersisted<ToastFeature>("toast", "show", "Payment received")

// On app restart — restores actions into the in-memory queue
instance.restorePersistedActions()
```

> Use an explicit string `featureKey` (not the class name) — class names can be obfuscated by ProGuard/R8.

---

## Instance API — modular apps and DI

The singleton is fine for small apps. For multi-module projects or Koin/Hilt injection, create isolated instances:

```kotlin
// Each module owns its registry — no cross-module interference
val rideKRelay  = KRelay.create("Rides")
val foodKRelay  = KRelay.create("Food")

// Or with custom settings via builder
val krelay = KRelay.builder("Payment")
    .maxQueueSize(50)
    .actionExpiry(60_000L)
    .debugMode(BuildConfig.DEBUG)
    .build()
```

Inject into ViewModels via Koin:

```kotlin
val appModule = module {
    single { KRelay.create("AppScope") }
    viewModel { LoginViewModel(krelay = get()) }
}

class LoginViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onSuccess() { krelay.dispatch<NavFeature> { it.goToHome() } }
}
```

---

## Compose Multiplatform

Add `krelay-compose` and use the built-in helpers:

```kotlin
// Registers when composition enters, unregisters when it leaves
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    KRelayEffect<ToastFeature> {
        object : ToastFeature {
            override fun show(message: String) =
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    // ...
}
```

```kotlin
// When you need to use the implementation in the same composable
@Composable
fun HomeScreen() {
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    rememberKRelayImpl<ToastFeature> {
        object : ToastFeature {
            override fun show(message: String) {
                scope.launch { snackbarState.showSnackbar(message) }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarState) }) { ... }
}
```

Both helpers accept an optional `instance` parameter for the Instance API:

```kotlin
KRelayEffect<ToastFeature>(instance = myKRelayInstance) { ... }
```

> **Manual `DisposableEffect`?** Always hoist the implementation into `remember {}`. Without it, Kotlin/Native's GC can collect the object before the first dispatch.
>
> See [Compose Integration Guide](docs/COMPOSE_INTEGRATION.md) for full patterns including Navigation Compose and Voyager.

---

## Testing

No mocking library required. Inject a real `KRelayInstance` and register plain Kotlin objects.

```kotlin
private lateinit var krelay: KRelayInstance
private lateinit var viewModel: LoginViewModel

@BeforeTest
fun setup() {
    krelay = KRelay.create("TestScope")
    viewModel = LoginViewModel(krelay = krelay)
}

@AfterTest
fun tearDown() {
    krelay.reset()
}

@Test
fun `login success shows toast and navigates`() {
    val toast = MockToast()
    val nav   = MockNav()
    krelay.register<ToastFeature>(toast)
    krelay.register<NavFeature>(nav)

    viewModel.onLoginSuccess()

    assertEquals("Welcome back!", toast.lastMessage)
    assertEquals("home", nav.lastDestination)
}

class MockToast : ToastFeature {
    var lastMessage: String? = null
    override fun show(message: String) { lastMessage = message }
}

class MockNav : NavFeature {
    var lastDestination: String? = null
    override fun navigateTo(screen: String) { lastDestination = screen }
}
```

Run the test suite:

```bash
./gradlew :krelay:test                           # JVM (fast)
./gradlew :krelay:iosSimulatorArm64Test          # iOS Simulator
./gradlew :krelay:connectedDebugAndroidTest      # Real Android device
```

---

## Memory safety

By default, three passive protections apply to every queued action:

| Protection | Default | Behaviour |
|---|---|---|
| `WeakReference` | Always on | Platform impls released when GC'd — no `onDestroy` cleanup needed |
| `actionExpiryMs` | 5 min | Queued actions expire and are dropped automatically |
| `maxQueueSize` | 100 | When full, lowest-priority (or oldest) action is evicted |

For granular control, use scope tokens to cancel only the actions queued by a specific ViewModel:

```kotlin
class MyViewModel : ViewModel() {
    private val token = scopedToken()

    fun doWork() = KRelay.dispatch<WorkFeature>(token) { it.run() }

    override fun onCleared() = KRelay.cancelScope(token)
}
```

---

## When not to use KRelay

KRelay is for **one-way, fire-and-forget UI commands**. For anything else, use the right tool:

| Scenario | Better alternative |
|---|---|
| Need a return value | `suspend fun` + `expect/actual` |
| Reactive UI state | `StateFlow` / `MutableStateFlow` |
| Critical side-effects (payment, upload) | `WorkManager` / background service |
| Database | Room / SQLDelight |
| Network | Ktor + Repository |

---

## Integrations

KRelay is framework-agnostic. It connects to whatever navigation, media, or permission library you already use — ViewModels stay clean of all framework imports.

| Category | Library |
|---|---|
| Navigation | Voyager · Decompose · Navigation Compose |
| Media | Peekaboo (image/camera picker) |
| Permissions | Moko Permissions |
| Biometrics | Moko Biometry |
| Reviews | Play Core · StoreKit |
| DI | Koin · Hilt |

See [Integration Guides](docs/INTEGRATION_GUIDES.md) for step-by-step examples.

---

## Compatibility

| KRelay | Kotlin | AGP | Android minSdk | iOS |
|---|---|---|---|---|
| 2.1.x | 2.1.x | 8.x | 24 | 14.0+ |
| 2.0.x | 2.1.x | 8.x | 24 | 14.0+ |
| 1.1.x | 2.0.x | 8.x | 23 | 13.0+ |
| 1.0.x | 1.9.x | 7.x | 21 | 13.0+ |

**Platforms:** Android arm64 · Android x86\_64 · iOS arm64 (device) · iOS arm64 (simulator) · iOS x64 (simulator)

---

## What's New

<details>
<summary><strong>v2.1.1 — Hardened & Standardized</strong></summary>

- **Atomic dispatch** — the impl lookup, queue insertion, and persistence decision happen inside a single lock, closing the TOCTOU window that could strand an action indefinitely.
- **`krelay-compose` artifact** — `KRelayEffect<T>` and `rememberKRelayImpl<T>` published as `dev.brewkits:krelay-compose:2.1.1`, separate from the zero-dependency core.
- **ProGuard/R8-safe persistence** — `registerActionFactory` and `dispatchPersisted` now require an explicit stable `featureKey` string. Old overloads deprecated with `replaceWith` guidance.
- **Identity-aware `unregister`** — `unregister(impl)` only removes the registration if the stored reference matches, preventing a recomposing Compose component from clearing a newer registration.
- **Thread-safe metrics** — all `KRelayMetrics` operations are now lock-protected.
- **iOS registration validation** — `registerFeature` validates interface conformance at runtime; crashes in debug, warns in release.
- **Priority eviction** — queue overflow now evicts the lowest-priority action, not the oldest FIFO item.

</details>

<details>
<summary><strong>v2.1.0 — Compose, Persistence & Scope Tokens</strong></summary>

- `KRelayEffect<T>` and `rememberKRelayImpl<T>` Compose helpers
- Persistent dispatch with `dispatchPersisted<T>()` — survives process death
- `SharedPreferencesPersistenceAdapter` (Android) and `NSUserDefaultsPersistenceAdapter` (iOS)
- Scope Token API: `scopedToken()` + `cancelScope(token)` for fine-grained ViewModel cleanup
- `dispatchWithPriority` available on instances (was singleton-only)
- `resetConfiguration()` without clearing the registry or queue

</details>

<details>
<summary><strong>v2.0.0 — Instance API for Super Apps</strong></summary>

- `KRelay.create("ScopeName")` — isolated instances per module
- `KRelay.builder(...)` — configure queue, expiry, and debug mode per instance
- DI-friendly: `KRelayInstance` is an interface, injectable via Koin or Hilt
- 100% backward compatible with v1.x

</details>

---

## Documentation

| Guide | Description |
|---|---|
| [Compose Integration](docs/COMPOSE_INTEGRATION.md) | `KRelayEffect`, `rememberKRelayImpl`, Navigation Compose, Voyager |
| [SwiftUI Integration](docs/SWIFTUI_INTEGRATION.md) | iOS-specific patterns, XCTest |
| [Integration Guides](docs/INTEGRATION_GUIDES.md) | Voyager, Decompose, Moko, Peekaboo, DI |
| [Lifecycle Guide](docs/LIFECYCLE.md) | Activity · Fragment · UIViewController · SwiftUI |
| [Testing Guide](docs/TESTING.md) | Patterns, mocks, instrumented tests |
| [Anti-Patterns](docs/ANTI_PATTERNS.md) | What not to do and why |
| [Architecture](docs/ARCHITECTURE.md) | Internals deep dive |
| [API Reference](docs/QUICK_REFERENCE.md) | Full API cheat sheet |
| [Managing Warnings](docs/MANAGING_WARNINGS.md) | Suppress `@OptIn` at module level |
| [Migration to v2.0](docs/MIGRATION_V2.md) | Upgrading from v1.x |

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

<div align="center">

Made with care by **Nguyễn Tuấn Việt** · [Brewkits](https://brewkits.dev)

[Issues](https://github.com/brewkits/krelay/issues) · [Changelog](CHANGELOG.md) · datacenter111@gmail.com

</div>

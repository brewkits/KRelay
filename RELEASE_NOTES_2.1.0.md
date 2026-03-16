# KRelay v2.1.0 Release Notes

**Release Date**: 2026-03-16
**Type**: Minor release — fully backward compatible with v2.0 and v1.x

---

## Highlights

### Compose Multiplatform Integration (Built-in)

Two new composable helpers ship in `dev.brewkits.krelay.compose`:

- **`KRelayEffect<T>`** — registers a feature implementation scoped to the composition; auto-unregisters on dispose.
- **`rememberKRelayImpl<T>`** — same as `KRelayEffect` but returns the implementation for further use.

Both accept an optional `instance` parameter for use with the Instance API.

```kotlin
// Zero-boilerplate registration
KRelayEffect<ToastFeature> {
    object : ToastFeature {
        override fun show(message: String) =
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
```

A new **`KRelay.instance`** public property exposes the default `KRelayInstance` for cross-module access, fixing the `internal` visibility issue when `KRelayCompose.kt` lives in a different Gradle module.

---

### Persistent Dispatch

New `dispatchPersisted<T>()` API survives process death via the `ActionFactory` pattern (serializable by design — no lambda capture):

- `KRelayPersistenceAdapter` interface for pluggable storage backends
- `SharedPreferencesPersistenceAdapter` for Android
- `NSUserDefaultsPersistenceAdapter` for iOS
- `PersistedCommand` with length-prefix serialization format (handles all special characters)

---

### Scope Token API

Tag queued actions with a caller-identity token. Cancel all tagged actions without touching other pending actions for the same feature — ideal for `ViewModel.onCleared()`:

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

### Quality & Testing

- **237 unit tests** — all pass on JVM and iOS Simulator (Arm64)
- **19 instrumented tests** — all pass on real Android device (Pixel 6 Pro, Android 16)
- Stress tests rewritten for KMP compatibility (iOS GCD async-safe — verify queue state synchronously)
- `resetConfiguration()` on `KRelayInstance` and `KRelay` for isolated test setup

---

## Bug Fixes

| Fix | Details |
|-----|---------|
| `KRelayMetrics` not wired | `recordDispatch/Queue/Replay()` now fire correctly from all dispatch paths |
| `metricsEnabled` flag ignored | `if (!enabled) return` guard added to each `record*` method |
| iOS KClass bridging broken | `KRelayIosHelperKt.getKClass(obj:)` used during `register(_:)`; all iOS operations now find correct interface key |
| Voyager demo lifecycle crash | Upgraded to Voyager 1.1.0-beta03; replaced `LaunchedEffect` + detached scope with `DisposableEffect` + `rememberCoroutineScope()` |
| Android 15+ 16KB page alignment | `android.allow_non_16k_pages=true` opt-in for apps using Peekaboo 0.5.2 (`libimage_processing_util_jni.so` is 4KB-aligned) |
| Duplicate registration debug log | Warning emitted when `register<T>()` overwrites an existing live implementation |
| Test config pollution | `DiagnosticDemo` tests now restore `actionExpiryMs`/`maxQueueSize` in `@AfterTest` |

---

## New Documentation

- `docs/COMPOSE_INTEGRATION.md` — updated with `KRelayEffect`, `rememberKRelayImpl`, `KRelay.instance`
- `docs/LIFECYCLE.md` — Android (Activity/Fragment/Compose) and iOS (UIViewController/SwiftUI) lifecycle best practices
- `docs/SWIFTUI_INTEGRATION.md` — SwiftUI patterns, `@Observable`, NavigationStack, XCTest
- `samples/KRelayFlowAdapter.kt` — Kotlin coroutines/Flow integration patterns

---

## Installation

```kotlin
// commonMain
implementation("dev.brewkits:krelay:2.1.0")
```

---

## Migration from v2.0.0

No changes required. All existing code works without modification.

Optional improvements:
- Replace manual `DisposableEffect` registration blocks with `KRelayEffect<T>` or `rememberKRelayImpl<T>`
- Use `KRelay.instance` instead of `KRelay.defaultInstance` if you were accessing it across modules
- Add `scopedToken()` / `cancelScope()` in ViewModels for fine-grained queue cleanup

---

## Compatibility

| Kotlin | KMP | AGP | Android minSdk | iOS min |
|--------|-----|-----|----------------|---------|
| 2.3.x  | 2.3.x | 8.x | 24 | 14.0 |

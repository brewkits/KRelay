# Changelog

All notable changes to KRelay will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

---

## [2.1.1] - 2026-09-05

### Added
- **`KRelay.removeInstance(scopeName)`**: New public API to explicitly remove a named instance from the global registry. Called automatically by `KRelayInstanceImpl.reset()` to eliminate memory leaks in Super App / Micro-frontend architectures where instances are created and destroyed with feature module lifecycles.
- **Per-instance scoped metrics**: `KRelayMetrics` now accepts a `scopeName` parameter on all `record*` methods, enabling precise per-module monitoring in multi-instance deployments.
- **Thread-safe `setPersistenceAdapter`**: Wrapping the adapter assignment inside `lock.withLock` ensures atomic replacement with no data race when swapping adapters at runtime.
- **`KRelayEffect` / `rememberKRelayImpl` `keys` parameter**: Both Compose helpers now accept a `vararg keys: Any?` argument (analogous to `remember` keys) so the implementation is automatically unregistered and re-registered whenever a captured dependency changes during recomposition.
- **UI instrumented tests for `krelay-compose`**: New `KRelayComposeTest` in `androidInstrumentedTest` verifies `KRelayEffect` lifecycle (register on enter, unregister on dispose) and `rememberKRelayImpl` key-driven re-registration.

### Fixed
- **Android `SharedPreferences` `StringSet` mutation bug (Critical)**: `getStringSet` returns an internal mutable reference; mutating it causes undefined behavior per the Android documentation. Migrated persistence storage to a `JSONArray`-encoded single String, eliminating data loss and ordering issues in `dispatchPersisted`.
- **TOCTOU race in `reset()`**: Captured the `_persistenceAdapter` reference inside `lock.withLock` before performing I/O, closing the time-of-check to time-of-use window that could leave a stale adapter in use after `setPersistenceAdapter` was called concurrently.
- **iOS `NSUserDefaults.clearAll()` O(N) scan**: `clearAll` previously iterated the entire `NSUserDefaults` dictionary to find scope-prefixed keys (O(N) over the whole user defaults store). Now tracks scope keys via a dedicated `ALL_SCOPES_KEY` index for O(K) cleanup (K = number of scopes).
- **O(1) queue eviction**: `enqueueActionUnderLock` now removes the lowest-priority item directly by index (`removeAt(lastIndex)`) instead of a linear search, reducing eviction cost from O(N) to O(1).
- **Binary insertion for priority queue**: Incoming actions are inserted at the correct sorted position using `binarySearch` (O(log N)) rather than appending and re-sorting the entire list (O(N log N)).
- **`kotlinSerialization` plugin regression**: The plugin was incorrectly removed in a cleanup pass; it is required by Decompose for `@Serializable` navigation state restoration and has been restored.

### Changed
- **Decompose upgraded to `3.4.0-alpha03`**: Resolves the iOS Native Linker crash (`SkikoKey` missing symbol) introduced by Compose Multiplatform `1.8.0-alpha03` / Decompose `3.2.0` binary incompatibility.
- **`composeApp` namespace**: Corrected to `dev.brewkits.krelay.demo` to avoid package collisions.
- **`composeApp` R8 minification enabled**: `isMinifyEnabled = true` and `isShrinkResources = true` are now active for release builds.
- **`consumer-rules.pro` tightened**: Removed keep rules for the deleted `MainThreadExecutor` class; narrowed `QueuedAction` keep rules to only the fields accessed via reflection.
- **`kotlin.native.cacheKind=none`**: Added to `gradle.properties` as a workaround for the Kotlin 2.1.0 Native compiler `NullPointerException` in `CacheBuilderKt` when linking iOS test binaries against Compose Multiplatform `1.8.0-alpha03`.

---

## [2.1.0-Changes] - 2026-04-06

### Added
- **Hardened Concurrency Safety**: Persisted dispatch is now fully atomic. The decision to enqueue (impl lookup + queue insertion) happens inside a single lock, eliminating the TOCTOU race where `register()` completing between the check and the enqueue would leave an action stranded. Persistence I/O (`save`/`remove`) is intentionally performed *outside* the lock so disk latency cannot block other threads.
- **Compose Module** (`krelay-compose`): `KRelayEffect<T>` and `rememberKRelayImpl<T>` are now published as a standalone `dev.brewkits:krelay-compose` artifact. The module uses `api(project(":krelay"))` so all core types are automatically visible to consumers.
- **Enhanced iOS Registration Safety**: `registerFeature` helper now validates at runtime that the provided implementation conforms to the target interface, crashing early in debug mode and logging a clear warning in release mode. Prevents silent failures from KClass mismatches.
- **ProGuard/R8-safe Persistence API**: `registerActionFactory` and `dispatchPersisted` now require an explicit stable `featureKey: String`. Class simple names can be obfuscated by R8; explicit keys survive minification. Old overloads deprecated with `replaceWith` guidance.
- **Testability via `KRelayInstance` interface**: Persistence and dispatch methods are now declared on the `KRelayInstance` interface, allowing test doubles to implement it directly without casting to `KRelayInstanceImpl`.
- **Binary Compatibility**: Compiled with **Kotlin 2.1.0 (Stable)** for maximum consumer compatibility.

### Fixed
- **Thread-safe `KRelayMetrics`**: All `record*`, `get*`, and `getAllMetrics()` operations are now protected by an internal lock, preventing data races in concurrent apps.
- **`getMetricsInternal` stub**: iOS Swift interop helper now returns real metrics from `KRelayMetrics` instead of an empty map.
- **Priority Eviction Logic**: When the queue is full, KRelay now evicts the **lowest-priority** action (not just the oldest FIFO action), correctly honouring the `ActionPriority` attribute.
- **Compose overwrite warning noise**: `register()` now only logs the overwrite warning when the incoming implementation is a *different class* from the existing one. Same-class replacements (e.g. Activity recreated by the Compose lifecycle) are expected and are no longer logged.
- **`restorePersistedActions` I/O inside lock**: Persistence I/O (`loadAll`/`remove`) is now performed outside the instance lock; all in-memory mutations happen in a single `lock.withLock` block. This removes the risk of disk latency blocking the dispatch path.
- **Identity-aware `unregister`**: Passing an `impl` to `unregister()` now only removes the registration if the stored reference is the same object, preventing a recomposing Compose screen from accidentally clearing a newer registration.
- **`VoyagerDemo` GC bug**: `VoyagerNavigationImpl` is now held by `remember(navigator)` outside the `DisposableEffect`, preventing the Kotlin/Native GC from reclaiming it before the first dispatch.

---

## [2.1.0] - 2026-03-16

### Added
- **`dispatchWithPriority` on `KRelayInstance`**: Priority dispatch is now available on all instances (previously singleton-only). Fixes API inconsistency between singleton and instance APIs.
- **Lifecycle Integration Guide** (`docs/LIFECYCLE.md`): Comprehensive best practices for Android (Activity, Fragment, Compose) and iOS (UIViewController, SwiftUI), including screen rotation behavior and ViewModel `onCleared` patterns.
- **Flow/Coroutines Adapter** (`samples/KRelayFlowAdapter.kt`): Documentation and patterns for integrating KRelay with Kotlin coroutines and Flow.
- **CI/CD via GitHub Actions** (`.github/workflows/ci.yml`): Automated build, test (Android JVM + iOS Simulator), and snapshot publishing pipeline.
- **Version compatibility matrix** in README: Clear table of KRelay versions vs Kotlin, KMP, AGP, and platform support.
- **Dokka API documentation**: `./gradlew :krelay:dokkaHtml` generates HTML docs to `docs/api/`. Configured with source links to GitHub.
- **Persistent Dispatch** (`KRelayPersistence.kt`, `PersistedDispatch.kt`): New `dispatchPersisted<T>()` API that survives process death. Uses named actions + `ActionFactory` pattern (serializable by design — no lambda capture). Supports `restorePersistedActions()` on app restart.
  - `KRelayPersistenceAdapter` interface for pluggable storage backends
  - `InMemoryPersistenceAdapter` (default, no-op persistence)
  - `SharedPreferencesPersistenceAdapter` for Android (`SharedPreferences`-backed)
  - `NSUserDefaultsPersistenceAdapter` for iOS (`NSUserDefaults`-backed)
  - `PersistedCommand` with length-prefix serialization format (handles all special characters unambiguously)
- **Compose Multiplatform integration** (`composeApp/.../KRelayCompose.kt`): `KRelayEffect<T>` composable and `rememberKRelayImpl<T>` helper for lifecycle-safe registration via `DisposableEffect`.
- **Compose Integration Guide** (`docs/COMPOSE_INTEGRATION.md`): Patterns for `DisposableEffect`, `rememberKRelayImpl`, Voyager, Navigation Compose, and SnackbarHostState.
- **SwiftUI Integration Guide** (`docs/SWIFTUI_INTEGRATION.md`): `KRelayEffect` ViewModifier, `@Observable` pattern (iOS 17+), NavigationStack, Sheet/Modal, Permissions, and XCTest patterns.
- **Scope Token API** (`scopeToken`, `cancelScope`, `dispatch(scopeToken, block)`): Selective queue cleanup by caller identity. Tag queued actions from a ViewModel with a token; call `cancelScope(token)` in `onCleared()` to release lambda captures without touching other pending actions for the same feature. `scopedToken()` utility generates a unique, human-readable token per instance.
- **`resetConfiguration()`** on `KRelayInstance` and `KRelay` singleton: Restores `maxQueueSize`, `actionExpiryMs`, and `debugMode` to defaults without touching the registry or pending queue. Useful for isolated test setup.
- **`KRelayIosHelperKt.registerFeature(instance:kClass:impl:)`**: Public Kotlin helper for KMP apps where Kotlin dispatches under the interface KClass and iOS needs to register under the same key. See `KRelayIosHelper.kt` for usage.

### Fixed
- **`KRelayMetrics` not wired to dispatch pipeline**: `recordDispatch()`, `recordQueue()`, and `recordReplay()` were never called from actual dispatch/register code. All three metrics now fire correctly from `dispatchInternal`, `dispatchWithPriorityInternal`, `dispatchPersistedInternal`, and `registerInternal` (replay path). Zero overhead when `KRelayMetrics.enabled = false` (default).
- **`KRelayMetrics.enabled` flag was never respected**: `record*` methods always recorded metrics regardless of the `metricsEnabled` flag. Fixed by adding `if (!enabled) return` guard in each method. `KRelay.metricsEnabled = true` now properly opt-in enables collection.
- **iOS Swift KClass bridging broken**: `KotlinKClass.init()` placeholder in `KRelay+Extensions.swift` created an invalid/empty KClass, causing all iOS register/dispatch operations to silently fail. Fixed by using `KRelayIosHelperKt.getKClass(obj:)` during `register(_:)` and caching the result. All iOS operations now use the correct concrete KClass. KMP pattern (Kotlin dispatches interface KClass, iOS registers) documented with `registerFeature` helper.
- **iOS main thread comment misleading**: Removed the "99% accurate" comment from `MainThreadExecutor.ios.kt`. `NSThread.isMainThread` is the correct and reliable check for all standard iOS use cases.
- **Duplicate registration warning**: Added debug log when `register<T>()` overwrites an existing alive implementation for the same feature type. Helps detect accidental double-registration.
- **Test config pollution**: `DiagnosticDemo` tests modified global `KRelay.actionExpiryMs`/`maxQueueSize` without restoring defaults after each test, causing intermittent failures in unrelated test classes. Fixed with proper `@BeforeTest`/`@AfterTest` lifecycle methods.

### Changed
- `KRelayMetrics` now exposes `enabled: Boolean` as a direct public property (replaces the convoluted private extension property workaround).
- **Code duplication reduced**: Extracted `enqueueActionUnderLock()` helper in `KRelayInstanceImpl` — shared by `dispatchInternal`, `dispatchWithPriorityInternal`, and `dispatchPersistedInternal`. Eliminates ~50 lines of duplicated queue management logic across the three dispatch paths. `KRelay.dispatchWithPriorityInternal` (singleton) now delegates to the same helper.

---

## [2.0.0] - 2026-02-04

### Added

#### Instance API for Super Apps and DI
- **Instance Creation**: New `KRelay.create(scopeName)` factory method for creating isolated instances
- **Builder Pattern**: New `KRelay.builder(scopeName)` for configuring instances with custom settings
- **Full Instance Isolation**: Each instance has independent registry, queue, and lock
- **Per-Instance Configuration**: Customize `maxQueueSize`, `actionExpiryMs`, and `debugMode` per instance
- **DI Integration**: First-class support for dependency injection frameworks (Koin, Hilt)
- **Super App Support**: Enables multiple independent modules without feature name conflicts

#### Developer Experience Improvements
- **Duplicate Scope Name Detection**: Warning logged in debug mode when creating instances with duplicate names
- **Input Validation**: Builder parameters validated at creation time
  - `maxQueueSize` must be > 0
  - `actionExpiryMs` must be > 0
  - `scopeName` must not be blank
- **Clear Error Messages**: Validation errors include actual invalid values for easier debugging

#### Documentation
- New migration guide for v1.x to v2.0 (`docs/MIGRATION_V2.md`)
- Super App architecture example with full implementation (`docs/SUPER_APP_EXAMPLE.md`)
- DI integration guide for Koin and Hilt (`docs/DI_INTEGRATION.md`)
- Comprehensive technical review document (`docs/COMPREHENSIVE_REVIEW_V2.md`)
- v2.0.0 improvements summary (`docs/V2_0_0_IMPROVEMENTS.md`)

#### Testing
- 10 new instance isolation tests
- 5 new validation tests
- All tests pass: 100% (15/15 instance tests)
- Verified backward compatibility: all existing tests pass

### Changed

#### Architecture Improvements
- Refactored `KRelay` singleton to facade pattern, delegating to internal `defaultInstance`
- Extracted core logic into reusable `KRelayInstanceImpl` class
- Improved code organization with clear separation between singleton and instance APIs

#### API Enhancements
- `KRelayInstance` interface with extension functions for type-safe operations
- `register`, `dispatch`, `unregister`, `isRegistered`, `getPendingCount`, `clearQueue` available on instances
- Non-reified methods (`getDebugInfo`, `dump`, `reset`) available directly on interface

### Technical Details

#### Performance
- Instance API overhead: <5% compared to singleton (negligible)
- Memory footprint: ~800 bytes per instance
- Lock granularity: Per-instance locks (no global contention)

#### Thread Safety
- All instance operations protected by per-instance locks
- Proper lock scope to prevent deadlocks
- Verified by stress tests

#### Memory Management
- Weak references prevent memory leaks
- Bounded queues with configurable size limits
- Automatic cleanup of expired actions

### Backward Compatibility

✅ **100% Backward Compatible**

- All existing v1.x code works without modification
- Singleton API delegates to default internal instance
- No breaking changes to public API
- Existing tests pass without changes

### Migration

Migration from v1.x to v2.0 is **optional** and can be done **incrementally**:

1. **No Changes Required**: Existing code continues to work
2. **Recommended for New Projects**: Use instance API with DI
3. **Recommended for Super Apps**: Migrate to instance API for module isolation
4. **Simple Apps**: Can continue using singleton API

See `docs/MIGRATION_V2.md` for detailed migration guide.

### Breaking Changes

None. This release is fully backward compatible with v1.x.

---

## [1.1.0] - 2025-XX-XX

### Added
- Thread safety improvements with atomic operations in stress tests
- Enhanced documentation
- Bug fixes and stability improvements

### Changed
- Improved test coverage

---

## [1.0.0] - 2024-XX-XX

### Added
- Initial release
- Singleton-based API
- WeakRef registry for automatic memory management
- Sticky queue for action replay
- Thread-safe operations
- Platform-specific main thread dispatch (Android Looper, iOS GCD)
- Debug mode and metrics
- Comprehensive test suite

### Features
- `KRelay.register<T>(impl)` - Register platform implementations
- `KRelay.dispatch<T> { }` - Dispatch actions to platform
- `KRelay.unregister<T>()` - Unregister implementations
- `KRelay.isRegistered<T>()` - Check registration status
- `KRelay.getPendingCount<T>()` - Get queued action count
- `KRelay.clearQueue<T>()` - Clear pending actions
- `KRelay.getDebugInfo()` - Get debug information
- `KRelay.dump()` - Dump state to console
- `KRelay.reset()` - Clear all registrations and queues

---

[Unreleased]: https://github.com/brewkits/KRelay/compare/v2.1.1...HEAD
[2.1.1]: https://github.com/brewkits/KRelay/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/brewkits/KRelay/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/brewkits/KRelay/releases/tag/v2.0.0
[1.1.0]: https://github.com/brewkits/KRelay/releases/tag/v1.1.0
[1.0.0]: https://github.com/brewkits/KRelay/releases/tag/v1.0.0

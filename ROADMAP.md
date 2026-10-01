# KRelay Roadmap

> **Mission**: Become the standard solution for clean, leak-free platform interop in Kotlin Multiplatform projects.

---

## ✅ Released

### v1.0.0 — Initial Release
- Singleton API: `register`, `dispatch`, `unregister`
- WeakReference registry — zero memory leaks
- Sticky queue — events survive screen rotation
- Main-thread dispatch (Android Looper + iOS GCD)
- Thread-safe with per-instance reentrant locks
- Debug tools: `dump()`, `getDebugInfo()`

### v1.1.0 — Hardening
- Thread safety improvements
- Stress-tested with 100k+ concurrent operations
- Enhanced test coverage

### v2.0.0 — Instance API for Super Apps *(Feb 2026)*
- `KRelay.create("ScopeName")` — isolated instances per module
- `KRelay.builder(...)` — configurable queue size, expiry, debug mode
- DI-friendly: inject `KRelayInstance` into ViewModels
- No feature conflicts in multi-module / Super App architectures
- 100% backward compatible with v1.x

### v2.1.0 — Compose Integration & Test Hardening *(Mar 2026)*
- Built-in `KRelayEffect<T>` and `rememberKRelayImpl<T>` Compose helpers
- `KRelay.instance` public property for cross-module access
- Scope Token API: `scopedToken()` / `cancelScope(token)` for per-ViewModel cleanup
- Persistent Dispatch: `dispatchPersisted<T>()` + `SharedPreferencesPersistenceAdapter` + `NSUserDefaultsPersistenceAdapter`
- 237 unit tests (JVM + iOS Simulator) + 19 instrumented tests (real device)
- CI/CD via GitHub Actions
- Fixed: KRelayMetrics wiring, iOS KClass bridging, Voyager lifecycle crash, Android 15+ 16KB page alignment

### v2.1.1 — QA Hardening & Ecosystem Infrastructure *(Sep 2026)*
- **Critical fix**: Android `SharedPreferences` `StringSet` mutation bug (data loss in `dispatchPersisted`)
- **Critical fix**: TOCTOU race condition in `reset()` and `setPersistenceAdapter()`
- **Performance**: `NSUserDefaults.clearAll()` O(N) → O(K); O(1) queue eviction; O(log N) binary insertion
- **New**: `KRelay.removeInstance(scopeName)` — explicit Super App lifecycle management
- **New**: `KRelayEffect` / `rememberKRelayImpl` `vararg keys` for recomposition safety
- **New**: `krelay-testing` artifact — `FakeKRelayInstance`, `KRelayTestRule`, `executeLastDispatch`
- **New**: `krelay-bom` — Bill of Materials for version alignment
- **New**: `KRelay.dispatchWithPriority<T>()` public reified API
- **New**: Binary Compatibility Validator (BCV) integrated; `apiCheck` runs in CI
- **New**: `release.yml` CD workflow triggered by git tags
- **New**: GitHub Issue Templates, `CONTRIBUTING.md`, `SECURITY.md`
- Decompose upgraded to `3.4.0-alpha03` (iOS linker fix)
### v2.2.0 — Desktop, Web & Flow Adapter *(Sep 2026)*
- **JVM Desktop** support: `jvm()` target for Compose Desktop (macOS, Windows, Linux)
- **Kotlin/WasmJs** target for web use-cases
- **`krelay-flow` artifact**: Official Kotlin Coroutines Flow adapter — `relayTo` operator bridges any `Flow` to `KRelayInstance`
- Enhanced stress tests (200 coroutines × 2000 operations)
- CI/CD coverage for all 5 published modules

---

### v2.3.0 — Observability & Ecosystem *(Oct 2026)*
- **Structured logging**: `KRelayLog` with severity levels and pluggable `KRelayLogSink`
- **Metrics reporter**: `KRelayMetricsReporter` for exporting to Firebase Performance, Datadog or custom sinks
- **`dispatchPersistedSuspend`** in `krelay-flow`
- Fixed per-instance metrics attribution and expiry counting
- Kotlin 2.3.21 / Compose Multiplatform 1.10.3

---

## 🔭 Planned

### v2.4.0 — API hygiene, Release Confidence & Adapters *(Q1 2027, tentative)*
Source- and binary-compatible with 2.3.x (guarded by `apiCheck`). Scope is a proposal, not a commitment.

**API hygiene (non-breaking)**
- Raise the deprecated two-argument `dispatchPersisted` / `registerActionFactory` overloads to `DeprecationLevel.ERROR`. A call such as `dispatchPersisted<T>("feature", "action")` silently binds to the deprecated overload instead of the explicit-`featureKey` one (found while writing 2.3.0 tests).
- Deprecate the demo view models bundled in the core artifact (`dev.brewkits.krelay.samples`); they are shipped to every consumer and are the only untested code in `krelay`.

**Quality gates**
- Enforce a Kover coverage floor for `krelay` in CI (core is ~90% on JVM today).
- Run the `krelay-compose` instrumented tests on an emulator in CI.
- Track performance budgets over time (`PerformanceTest` currently only guards against regressions such as the O(n²) enqueue fixed in 2.3.0).

**Release confidence**
- Prove the single-bundle release pipeline (`scripts/build-maven-bundle.sh`) end to end and make the workflow wait for the Central Portal deployment to reach `PUBLISHED`.
- Post-release smoke test: resolve every published artifact and the BOM from Maven Central.

**Demo**
- Resolve the 16 KB page-size warnings in the demo app's transitive native libraries (CameraX image-processing, `androidx.graphics.path`).

**Adapters (opt-in artifacts; core stays dependency-free)**
- First ready-made `KRelayMetricsReporter` / `KRelayLogSink` adapter (Firebase Performance or Datadog — to be decided from user feedback).

### Community-driven
- **Library-specific integration modules**: `krelay-moko`, `krelay-voyager` — contributions welcome

### v3.0.0 — TBD *(2027+)*
Only if the following breaking cleanups are worth a major version; a full migration guide will be provided well in advance:
- Remove the deprecated `dispatchPersisted` / `registerActionFactory` overloads
- Move the bundled `samples` package out of the core artifact

---

## ❌ Non-Goals

These will **never** be added to keep KRelay focused:

| Feature | Why Not |
|---------|---------|
| Suspend function / return values | Use `expect/actual` or `suspend fun` |
| State management | Use `StateFlow` |
| Built-in DI | Use Koin / Hilt / kotlin-inject |
| Background processing | Use `Dispatchers.IO` / WorkManager |
| Two-way communication | Use Repository + callbacks |

---

## Contributing

Issues, PRs, and discussions are welcome at [github.com/brewkits/KRelay](https://github.com/brewkits/KRelay).

---

**Current Version**: v2.3.0 · **Last Updated**: 2026-10-01

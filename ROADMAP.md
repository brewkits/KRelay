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

---

## 🔭 Planned

### v2.2.0 — Desktop & Web *(Q3 2026)*
- JVM Desktop support (Compose Desktop — Windows, macOS, Linux)
- Kotlin/JS and Kotlin/Wasm targets

### v2.3.0 — Observability *(Q4 2026)*
- `KRelayMetrics` dashboard: dispatch rate, queue depth, replay count
- Structured logging with configurable sinks
- Integration with popular monitoring tools

### v3.0.0 — TBD *(2027)*
No breaking changes are currently planned. If a v3 happens, a full migration guide will be provided well in advance.

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

**Current Version**: v2.1.0 · **Last Updated**: 2026-03-16

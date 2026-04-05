# Release Notes - KRelay v2.1.1

> **"Hardened & Standardized"**
> This release focuses on enterprise-grade concurrency safety, ProGuard resilience, and proper library distribution.

## ⚡ What's New in v2.1.1

### 🛡️ Hardened Concurrency & Atomic Dispatch
The dispatch decision (impl lookup + queue insertion) is now a single atomic operation inside one lock. Previously a narrow TOCTOU window existed: `register()` completing between the impl-check and the enqueue would leave an action stranded forever. This is now impossible.

Persistence I/O (`save`/`remove`) is performed *outside* the lock so disk latency never blocks other threads — only the fast in-memory decision is locked.

### 📦 Proper Module Distribution
KRelay is now officially split into two optimized artifacts:
1. `krelay` (Core): **Zero-Dependency**, extremely lightweight.
2. `krelay-compose` (New): Built-in `KRelayEffect` and `rememberKRelayImpl` for Compose Multiplatform users.

The `krelay-compose` artifact uses `api(krelay)` so core types are automatically visible to your project — no extra import required.

### 🔒 ProGuard/R8-Safe Persistence
`registerActionFactory` and `dispatchPersisted` now accept an explicit `featureKey: String`. Class simple names can be stripped or renamed by R8 minification; explicit string keys survive obfuscation unchanged. Old overloads are deprecated with `replaceWith` guidance.

### 🍎 iOS Registration Safety
`registerFeature` now validates at runtime that the implementation conforms to the target interface. In debug mode it crashes immediately (fail-fast); in release mode it logs a clear warning. Prevents silent dispatch failures from KClass mismatches in Swift interop.

### 🧩 Binary Compatibility
Compiled with **Kotlin 2.1.0 (Stable)** for maximum consumer compatibility.

---

## 🛠 Installation

```kotlin
// shared module build.gradle.kts
commonMain.dependencies {
    // The Core library (Always needed)
    implementation("dev.brewkits:krelay:2.1.1")
    
    // The Compose helpers (Optional)
    implementation("dev.brewkits:krelay-compose:2.1.1")
}
```

---

## 🏗 Migration from v2.1.0

1. **Update version** to `2.1.1`.
2. **Compose Users**: If you were manually copying `KRelayCompose.kt`, you can now delete it and simply add the `krelay-compose` dependency.
3. **Android Users**: Ensure you are using Kotlin 2.1.0 or higher.

---

## 🧪 Verification Status

- ✅ **237 Unit Tests**: Passed (JVM & iOS Simulator)
- ✅ **19 Instrumented Tests**: Passed (Android Physical Device)
- ✅ **Stress Tests**: 100,000 concurrent operations verified for Lock integrity.
- ✅ **Demo App**: Fully functional with the new module structure.

---

[⬆️ Back to Top](#-krelay-v211)

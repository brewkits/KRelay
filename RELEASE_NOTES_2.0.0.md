# KRelay v2.0.0 - Instance API for Super Apps 🚀

We're excited to announce KRelay v2.0.0, a major update that brings powerful new capabilities while maintaining 100% backward compatibility.

## 🎯 What's New

### Instance-Based API

Create isolated KRelay instances for true module independence:

```kotlin
// Create isolated instances
val rideKRelay = KRelay.create("Rides")
val foodKRelay = KRelay.create("Food")

// Each module has independent registry - no conflicts!
rideKRelay.register<ToastFeature>(RideToastImpl())
foodKRelay.register<ToastFeature>(FoodToastImpl())
```

### Perfect for Super Apps

If you're building a "Super App" (like Grab or Gojek) with multiple independent modules, v2.0 solves the feature name conflict problem:

**Before (v1.x)**:
```kotlin
// Problem: Both modules use same feature name
KRelay.register<ToastFeature>(RideToastImpl())
KRelay.register<ToastFeature>(FoodToastImpl()) // ❌ Overwrites!
```

**After (v2.0)**:
```kotlin
// Solution: Each module has its own instance
val rideKRelay = KRelay.create("Rides")
val foodKRelay = KRelay.create("Food")

rideKRelay.register<ToastFeature>(RideToastImpl())
foodKRelay.register<ToastFeature>(FoodToastImpl()) // ✅ No conflict!
```

### DI-Friendly Architecture

Perfect integration with dependency injection frameworks:

```kotlin
// Koin module
val rideModule = module {
    single { KRelay.create("RideModule") }
    viewModel { RideViewModel(krelay = get()) }
}

// ViewModel
class RideViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun bookRide() {
        krelay.dispatch<ToastFeature> { it.show("Booking...") }
    }
}
```

### Configurable Instances

Use the builder pattern for custom configuration:

```kotlin
val instance = KRelay.builder("MyModule")
    .maxQueueSize(50)
    .actionExpiry(60_000L)
    .debugMode(true)
    .build()
```

## ✨ Improvements

### Developer Experience
- **Duplicate Scope Name Detection**: Get warnings in debug mode when creating instances with duplicate names
- **Input Validation**: Builder parameters validated with clear error messages
- **Better Error Messages**: Know exactly what went wrong

### Quality & Reliability
- **10 New Isolation Tests**: Comprehensive testing of multi-instance scenarios
- **100% Test Pass Rate**: All 15 instance tests passing
- **<5% Performance Overhead**: Instance API is just as fast as singleton
- **~800 bytes per Instance**: Minimal memory footprint

## 📚 Documentation

- [Migration Guide](docs/MIGRATION_V2.md) - How to upgrade from v1.x
- [Super App Example](docs/SUPER_APP_EXAMPLE.md) - Complete architecture guide
- [DI Integration](docs/DI_INTEGRATION.md) - Koin and Hilt examples
- [Technical Review](docs/COMPREHENSIVE_REVIEW_V2.md) - Deep dive analysis

## 🔄 Migration

**Good news**: Migration is optional!

- ✅ **No Breaking Changes**: All v1.x code works without modification
- ✅ **Incremental Adoption**: Migrate at your own pace
- ✅ **Simple Apps**: Can continue using singleton API
- ✅ **New Projects**: Start with instance API for better architecture

## 📦 Installation

```kotlin
// Gradle (build.gradle.kts)
dependencies {
    implementation("dev.brewkits:krelay:2.0.0")
}
```

## 🎓 Quick Start

### Singleton API (Existing)
```kotlin
// Still works exactly as before
KRelay.dispatch<ToastFeature> { it.show("Hello!") }
```

### Instance API (New)
```kotlin
// Create instance
val krelay = KRelay.create("MyScope")

// Use it
krelay.dispatch<ToastFeature> { it.show("Hello!") }
```

## 🏗️ Architecture

v2.0 uses a **Facade Pattern**:
- Singleton API delegates to internal `defaultInstance`
- Each instance has isolated registry, queue, and lock
- Per-instance configuration
- Thread-safe with fine-grained locking

## 🎯 Who Should Use v2.0 Instance API?

**Highly Recommended for**:
- Super Apps with multiple independent modules
- Projects using DI (Koin, Hilt)
- Multi-team organizations
- Large-scale apps with modular architecture

**Optional for**:
- Simple single-module apps
- Small to medium projects
- Existing apps with singleton API (works fine)

## ⚡ Performance

- Instance creation: ~0.4ms
- Dispatch overhead: +2% vs singleton (negligible)
- Memory per instance: ~800 bytes
- 50 instances = ~40KB total

## 🙏 Acknowledgments

Thank you to the Kotlin Multiplatform community for feedback and support!

## 🔗 Links

- [GitHub Repository](https://github.com/brewkits/KRelay)
- [Documentation](https://github.com/brewkits/KRelay/tree/main/docs)
- [Migration Guide](https://github.com/brewkits/KRelay/blob/main/docs/MIGRATION_V2.md)
- [Issue Tracker](https://github.com/brewkits/KRelay/issues)

---

**Full Changelog**: https://github.com/brewkits/KRelay/blob/main/CHANGELOG.md

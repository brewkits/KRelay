# KRelay Documentation

Complete documentation for KRelay - The Native Interop Bridge for Kotlin Multiplatform.

## 📚 Documentation Index

### Getting Started
- **[Main README](../README.md)** - Overview, quick start, and core features
  - The Three Superpowers of KRelay
  - Installation & setup
  - Quick start guide
  - Use cases & examples

### In-Depth Guides

#### 1. [Architecture Guide](ARCHITECTURE.md)
Deep dive into KRelay's internal design:
- Core components (Registry, Queue, WeakRef)
- Data flow and lifecycle
- Platform implementations (Android, iOS)
- Thread safety mechanisms
- Memory management strategy
- Design decisions and trade-offs
- **Integration Patterns with Navigation Libraries**
  - Voyager integration architecture
  - Decompose integration patterns
  - Compose Navigation integration
  - Testing strategies
  - Migration guide

#### 2. [Integration Guide](INTEGRATION_GUIDE.md)
Step-by-step integration with popular navigation libraries:
- **Voyager Integration**
  - Complete working example
  - ViewModels, Screens, and Navigation setup
  - Testing best practices
- **Decompose Integration**
  - Component-based architecture
  - Lifecycle-aware navigation
  - Child components pattern
- **Compose Navigation Integration**
  - NavController setup
  - Type-safe navigation
  - Deep linking support

#### 3. [Usage Guide](USAGE_GUIDE.md)
Practical patterns and real-world examples:
- Common use cases
- Advanced patterns
- Error handling
- Performance optimization
- Best practices

#### 4. [Testing Guide](TESTING.md)
Comprehensive testing strategies:
- Unit testing ViewModels
- Integration testing
- Mock implementations
- Test scenarios (rotation, background/foreground)

#### 5. [Quick Reference](QUICK_REFERENCE.md)
Cheat sheet for quick lookup:
- API reference
- Common patterns
- Code snippets

## 🎯 Documentation by Use Case

### "I want to integrate KRelay with my navigation library"
→ Read: [Integration Guide](INTEGRATION_GUIDE.md)

**Quick Links:**
- [Voyager Integration](INTEGRATION_GUIDE.md#voyager-integration)
- [Decompose Integration](INTEGRATION_GUIDE.md#decompose-integration)
- [Compose Navigation](INTEGRATION_GUIDE.md#compose-navigation-integration)

### "I want to understand how KRelay works internally"
→ Read: [Architecture Guide](ARCHITECTURE.md)

**Quick Links:**
- [Core Components](ARCHITECTURE.md#core-components)
- [Data Flow](ARCHITECTURE.md#data-flow)
- [Memory Management](ARCHITECTURE.md#memory-management)
- [Thread Safety](ARCHITECTURE.md#thread-safety)

### "I want practical examples and patterns"
→ Read: [Usage Guide](USAGE_GUIDE.md)

**Quick Links:**
- [Toast/Snackbar patterns](USAGE_GUIDE.md#toastsnackbar-patterns)
- [Navigation patterns](USAGE_GUIDE.md#navigation-patterns)
- [Error handling](USAGE_GUIDE.md#error-handling)

### "I want to test my code that uses KRelay"
→ Read: [Testing Guide](TESTING.md)

**Quick Links:**
- [Testing ViewModels](TESTING.md#testing-viewmodels)
- [Mock implementations](TESTING.md#mock-implementations)
- [Integration tests](TESTING.md#integration-tests)

### "I need quick API reference"
→ Read: [Quick Reference](QUICK_REFERENCE.md)

## 🔥 The Three Superpowers (Quick Summary)

### 1️⃣ Native Interop Bridge
Clean, type-safe bridge from shared code to platform-specific features.

**Before:**
```kotlin
// DIY: Manual interface + boilerplate
object MyBridge {
    var activity: Activity? = null  // Memory leak!
}
```

**After with KRelay:**
```kotlin
// Clean, leak-free, automatic lifecycle
KRelay.dispatch<ToastFeature> { it.show("Hello!") }
```

### 2️⃣ The Standard for ViewModel One-off Events
Escape LaunchedEffect hell - fire-and-forget navigation & UI commands.

**Before:**
```kotlin
// SharedFlow + collectAsState + LaunchedEffect = Headache
val events = viewModel.navigationEvents.collectAsState()
LaunchedEffect(events.value) { /* handle events */ }
```

**After with KRelay:**
```kotlin
// Zero boilerplate - just dispatch!
viewModel.onLoginSuccess() // Internally: KRelay.dispatch<NavFeature>()
```

### 3️⃣ Integration with Navigation Libraries
"Ký sinh" strategy - become best friends with Voyager, Decompose, etc.

**Pattern:**
```kotlin
// ViewModel (Pure business logic)
KRelay.dispatch<NavFeature> { it.goToHome() }

// Platform Implementation (Voyager wrapper)
class VoyagerNavImpl(navigator: Navigator) : NavFeature {
    override fun goToHome() = navigator.push(HomeScreen())
}
```

## 📖 Reading Order for New Users

**For Beginners:**
1. Start with [Main README](../README.md) - Understand the "why"
2. Follow [Quick Start](../README.md#quick-start) - Get hands dirty
3. Read [Integration Guide](INTEGRATION_GUIDE.md) - Connect with your nav library
4. Check [Testing Guide](TESTING.md) - Write tests

**For Advanced Users:**
1. [Architecture Guide](ARCHITECTURE.md) - Deep understanding
2. [Usage Guide](USAGE_GUIDE.md) - Advanced patterns
3. [Quick Reference](QUICK_REFERENCE.md) - Bookmark for lookup

## 🧪 Demo Code

All documentation includes working code examples. Find them in:

- **Library Samples**: `krelay/src/commonMain/kotlin/dev/brewkits/krelay/samples/`
  - `VoyagerNavigationFeature.kt` - Navigation feature interface
  - `VoyagerDemoViewModel.kt` - ViewModel example
  - `ToastFeature.kt`, `NotificationBridge.kt`, etc.

- **Test Demos**: `krelay/src/commonTest/kotlin/dev/brewkits/krelay/demo/`
  - `VoyagerIntegrationDemo.kt` - Complete test example
  - `LoginFlowDemo.kt`, `DataSyncDemo.kt`, etc.

- **Demo App**: `composeApp/`
  - Android & iOS demo application
  - Real-world usage patterns

## 🤝 Contributing to Documentation

Found an error or want to improve the docs?

1. **Typos/Errors**: Open an issue or submit a PR
2. **New Examples**: Add to relevant guide with clear comments
3. **New Patterns**: Share in Usage Guide or Integration Guide

## 📞 Getting Help

- **Issues**: [GitHub Issues](https://github.com/yourusername/krelay/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/krelay/discussions)
- **Examples**: Check `samples/` and `demo/` folders

---

**Made with ❤️ for the Kotlin Multiplatform community**

**Version**: 1.0.0
**Last Updated**: 2026-01-22
**Philosophy**: Do One Thing and Do It Well

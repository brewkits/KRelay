# KRelay v1.0.0 - Quick Reference Card

## 🚀 Installation

```kotlin
// In your shared module's build.gradle.kts
commonMain.dependencies {
    implementation(project(":krelay"))
}
```

---

## ⚡ Basic Usage (3 Steps)

### 1. Define Feature (Common)
```kotlin
interface ToastFeature : RelayFeature {
    fun show(message: String)
}
```

### 2. Use from Shared Code
```kotlin
class MyViewModel {
    fun onSuccess() {
        KRelay.dispatch<ToastFeature> {
            it.show("Success!")
        }
    }
}
```

### 3. Implement on Platform

**Android:**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KRelay.register<ToastFeature>(
            object : ToastFeature {
                override fun show(msg: String) {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
```

**iOS (Swift):**
```swift
class ContentView: UIView {
    func setup() {
        KRelay.shared.register(
            impl: object: ToastFeature {
                func show(message: String) {
                    // Show iOS alert
                }
            }
        )
    }
}
```

---

## 🎛️ Configuration

```kotlin
// In Application.onCreate() or setup
KRelay.debugMode = true                    // Enable logging
KRelay.maxQueueSize = 100                  // Max actions per feature
KRelay.actionExpiryMs = 5 * 60 * 1000     // 5 minutes
KRelay.metricsEnabled = true               // Enable metrics
```

---

## 🎯 API Reference

### Core Functions

```kotlin
// Register implementation
KRelay.register<ToastFeature>(impl)

// Dispatch action
KRelay.dispatch<ToastFeature> { it.show("Hello") }

// Check if registered
val isRegistered = KRelay.isRegistered<ToastFeature>()

// Get pending count
val pending = KRelay.getPendingCount<ToastFeature>()

// Clear queue
KRelay.clearQueue<ToastFeature>()

// Unregister
KRelay.unregister<ToastFeature>()

// Reset everything
KRelay.reset()
```

```kotlin
import dev.brewkits.krelay.ActionPriority

// Dispatch with priority
KRelay.dispatchWithPriority<ErrorFeature>(ActionPriority.CRITICAL) {
    it.show("Critical error!")
}

// Priority levels
ActionPriority.LOW       // Analytics, logging
ActionPriority.NORMAL    // Default
ActionPriority.HIGH      // Important notifications
ActionPriority.CRITICAL  // Security alerts
```


## 🎨 Common Patterns

### Pattern 1: Error Handling
```kotlin
class MyUseCase {
    suspend fun execute() {
        try {
            // Business logic
        } catch (e: Exception) {
            KRelay.dispatchWithPriority<ErrorFeature>(ActionPriority.HIGH) {
                it.show("Error: ${e.message}")
            }
        }
    }
}
```

### Pattern 2: Success Feedback
```kotlin
class LoginViewModel {
    suspend fun login(username: String, password: String) {
        val result = authService.login(username, password)

        if (result.isSuccess) {
            KRelay.dispatch<ToastFeature> { it.show("Welcome back!") }
            KRelay.dispatch<NavigationFeature> { it.navigateTo("home") }
        }
    }
}
```

### Pattern 3: Background Operations
```kotlin
class DataSyncService {
    suspend fun syncData() {
        // This runs in background
        val data = api.fetchData()

        // UI update automatically on main thread
        KRelay.dispatch<ToastFeature> {
            it.show("Sync complete: ${data.size} items")
        }
    }
}
```

### Pattern 4: Permission Requests
```kotlin
class CameraViewModel {
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> {
            it.requestCamera { granted ->
                if (granted) startCamera()
            }
        }
    }
}
```

### Pattern 5: Haptic Feedback
```kotlin
class GameViewModel {
    fun onScored() {
        KRelay.dispatch<HapticFeature> {
            it.vibrate(duration = 100)
        }
    }
}
```

### Pattern 6: Simple Analytics
```kotlin
class CheckoutViewModel {
    fun completeOrder() {
        KRelay.dispatch<AnalyticsFeature> {
            it.track("order_completed")
        }
    }
}
```

### Pattern 7: Screen Rotation
```kotlin
// ViewModel dispatches during rotation
class MyViewModel {
    fun onDataLoaded() {
        KRelay.dispatch<ToastFeature> { it.show("Loaded!") }
    }
}

// After rotation, new Activity auto-registers
// Queued toast is shown automatically!
```

---

## ❌ Anti-Patterns to Avoid

### ❌ Don't: Synchronous Return Values
```kotlin
// WRONG: Can't get return value
fun getBattery(): Int {
    var level = 0
    KRelay.dispatch<BatteryFeature> { level = it.level }
    return level // Returns 0, not actual level!
}

// CORRECT: Use expect/actual or callbacks
expect fun getBatteryLevel(): Int
```

### ❌ Don't: State Management
```kotlin
// WRONG: Don't use KRelay for state
KRelay.dispatch<StateFeature> { it.update(newState) }

// CORRECT: Use StateFlow
val uiState = MutableStateFlow(UiState())
```

### ❌ Don't: Heavy Processing
```kotlin
// WRONG: Main thread blocks UI!
KRelay.dispatch<ProcessingFeature> { it.processLargeFile() }

// CORRECT: Use Dispatchers.IO
viewModelScope.launch(Dispatchers.IO) {
    processLargeFile()
    withContext(Main) {
        KRelay.dispatch<ToastFeature> { it.show("Done!") }
    }
}
```

### ❌ Don't: Critical Operations
```kotlin
// WRONG: Lost on process death
KRelay.dispatch<PaymentFeature> { it.sendPayment() }

// CORRECT: Use WorkManager
val work = OneTimeWorkRequest<PaymentWorker>()
WorkManager.enqueue(work)
```

---

## ⚙️ Advanced Configuration

### Queue Policies

```kotlin
// Aggressive cleanup (low memory devices)
KRelay.maxQueueSize = 20
KRelay.actionExpiryMs = 60 * 1000  // 1 minute

// Conservative (important actions)
KRelay.maxQueueSize = 200
KRelay.actionExpiryMs = 10 * 60 * 1000  // 10 minutes

// Immediate discard (no queueing)
KRelay.maxQueueSize = 0

// Unlimited (v1.0.0 behavior - not recommended)
KRelay.maxQueueSize = Int.MAX_VALUE
```

### Debug Configuration

```kotlin
// Development
if (BuildConfig.DEBUG) {
    KRelay.debugMode = true
    KRelay.metricsEnabled = true
}

// Production
KRelay.debugMode = false
KRelay.metricsEnabled = true  // For analytics
```

---

## 🔍 Debugging

### Check KRelay State

```kotlin
// Is feature registered?
if (!KRelay.isRegistered<ToastFeature>()) {
    println("ToastFeature not registered!")
}

// How many actions queued?
val pending = KRelay.getPendingCount<ToastFeature>()
if (pending > 10) {
    println("Warning: $pending actions queued")
}

// View metrics
val metrics = KRelay.getMetrics<ToastFeature>()
println("Dispatches: ${metrics["dispatches"]}")
println("Expired: ${metrics["expired"]}")
```

### Common Issues

**Issue:** Actions not executing
```kotlin
// Solution 1: Check if registered
assertTrue(KRelay.isRegistered<ToastFeature>())

// Solution 2: Check queue
val pending = KRelay.getPendingCount<ToastFeature>()
if (pending > 0) {
    // Actions queued, waiting for register
    KRelay.register<ToastFeature>(impl)
}
```

**Issue:** Queue growing too large
```kotlin
// Solution: Reduce limits
KRelay.maxQueueSize = 20
KRelay.actionExpiryMs = 60 * 1000

// Or: Clear queue
KRelay.clearQueue<ToastFeature>()
```

**Issue:** Memory leak suspected
```kotlin
// Solution: Check metrics
val metrics = KRelay.getMetrics<ToastFeature>()
if (metrics["queued"]!! > metrics["replayed"]!!) {
    // More queued than replayed - potential issue
    // Check if you're calling register()
}
```

---

## 📱 Platform-Specific Tips

### Android

```kotlin
// Best practice: Register in Activity/Fragment
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Good: Application context (long-lived)
        KRelay.register<ToastFeature>(AndroidToast(applicationContext))

        // ⚠️ Avoid: Activity context (might leak)
        // KRelay.register<ToastFeature>(AndroidToast(this))
    }
}

// Optional: Explicit cleanup
override fun onDestroy() {
    super.onDestroy()
    KRelay.unregister<ToastFeature>()  // Though WeakRef handles this
}
```

### iOS

```swift
// Register in View lifecycle
struct ContentView: View {
    var body: some View {
        ComposeView()
            .onAppear {
                let impl = IOSToastFeature(viewController: getVC())
                KRelay.shared.register(impl: impl)
            }
    }
}

// Or in UIViewController
class MyViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        KRelay.shared.register(impl: IOSToastFeature(viewController: self))
    }
}
```

---

## ⚡ Performance Tips

1. **Use Priority for Critical Actions**
   ```kotlin
   KRelay.dispatchWithPriority<ErrorFeature>(ActionPriority.CRITICAL) {
       it.show("Payment failed!")
   }
   ```

2. **Configure Queue Limits**
   ```kotlin
   KRelay.maxQueueSize = 50  // Prevent unbounded growth
   ```

3. **Enable Metrics in Debug Only**
   ```kotlin
   KRelay.metricsEnabled = BuildConfig.DEBUG
   ```

4. **Clear Queues When Navigating Away**
   ```kotlin
   override fun onPause() {
       super.onPause()
       KRelay.clearQueue<TransientFeature>()  // Don't replay when returning
   }
   ```

---

## 📊 Cheat Sheet

| Task | Code |
|------|------|
| Register | `KRelay.register<T>(impl)` |
| Dispatch | `KRelay.dispatch<T> { ... }` |
| Dispatch with priority | `KRelay.dispatchWithPriority<T>(priority) { ... }` |
| Check registered | `KRelay.isRegistered<T>()` |
| Get pending count | `KRelay.getPendingCount<T>()` |
| Clear queue | `KRelay.clearQueue<T>()` |
| Unregister | `KRelay.unregister<T>()` |
| Reset all | `KRelay.reset()` |
| Get metrics | `KRelay.getMetrics<T>()` |
| Print metrics | `KRelayMetrics.printReport()` |

---

## 🎯 Key Concepts

### 🔥 Two Killer Features (Why KRelay Beats DIY)

**1. 🛡️ No Memory Leaks (Automatic WeakReference)**
- DIY solutions forget to clear strong references → OutOfMemoryError
- KRelay uses WeakReference internally → Auto-cleanup
- Activities/ViewControllers properly garbage collected
- Zero manual lifecycle management needed

**2. 📦 Sticky Queue (Never Lose Commands)**
- DIY solutions drop commands when UI isn't ready
- KRelay queues commands and auto-replays when UI registers
- Survives screen rotation, cold start, background→foreground
- Commands execute even if dispatched before Activity created

### Other Features

3. **🧵 Thread Safety**: All actions execute on UI thread automatically
4. **🎯 Fire-and-Forget**: Just dispatch, don't worry about lifecycle
5. **✅ Type-Safe**: Compile-time checking, no string keys

---

## ⚠️ Important Limitations

### 🔴 NO Process Death Survival

Queue does **NOT** survive app process death. Lambdas cannot be serialized.

**✅ Good Use Cases:**
- UI commands (Toast, Navigation, Dialog)
- Screen refresh triggers
- Non-critical analytics

**❌ Bad Use Cases:**
- Critical transactions (payments, orders)
- Important data operations
- Operations requiring guaranteed execution

**Use Instead:**
- **WorkManager** - Critical background work
- **SavedStateHandle** - UI state persistence
- **Room/DataStore** - Data persistence

### 🟡 Singleton Model

Global `object KRelay` - simple but has trade-offs:
- **Pro**: Zero configuration, global access
- **Con**: Testing requires `reset()`, hard to isolate in Super Apps

**Workaround**: Use feature namespacing for module isolation

### 🟢 When to Use KRelay

**Perfect For:**
- Native interop from Kotlin Multiplatform
- UI commands and feedback
- Fire-and-forget operations
- Screen rotation handling

**Not For:**
- Business logic requiring guaranteed execution
- Critical data operations
- Operations needing process death survival

---

## 📚 More Resources

- **README.md** - Complete guide with examples
- **ARCHITECTURE.md** - Internal design and implementation details
- **TESTING.md** - Testing guide and best practices

---

**Version:** 1.0.0
**Status:** Production Ready ✅
**License:** MIT
**Last Updated:** 2026-01-22

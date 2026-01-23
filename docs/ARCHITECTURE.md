# KRelay Architecture

This document provides a deep dive into KRelay's internal architecture and design decisions.

## Table of Contents

1. [Overview](#overview)
2. [Core Components](#core-components)
3. [Data Flow](#data-flow)
4. [Platform Implementations](#platform-implementations)
5. [Roadmap](#roadmap)
6. [Thread Safety](#thread-safety)
7. [Memory Management](#memory-management)
8. [Queue & Replay Mechanism](#queue--replay-mechanism)
9. [Design Decisions](#design-decisions)

## Overview

KRelay is built on three fundamental pillars:

```
┌─────────────────────────────────────────┐
│         KRelay Architecture             │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────────────────────────────┐   │
│  │  1. Safe Dispatch                │   │
│  │     (Thread Management)          │   │
│  └──────────────────────────────────┘   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │  2. Weak Registry                │   │
│  │     (Memory Safety)              │   │
│  └──────────────────────────────────┘   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │  3. Sticky Queue                 │   │
│  │     (Reliability)                │   │
│  └──────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

## Core Components

### 1. KRelay Object (Singleton)

The central orchestrator that manages all registrations, dispatches, and queues.

```kotlin
object KRelay {
    // Registry: KClass -> WeakRef<Implementation>
    private val registry: MutableMap<KClass<*>, WeakRef<Any>>

    // Pending Queue: KClass -> List of pending actions
    private val pendingQueue: MutableMap<KClass<*>, MutableList<(Any) -> Unit>>

    // Thread synchronization lock
    private val lock: Any
}
```

**Responsibilities:**
- Maintain weak references to platform implementations
- Queue actions when implementations are missing
- Replay queued actions when implementations become available
- Provide thread-safe operations

### 2. RelayFeature Interface

Marker interface that all feature interfaces must extend.

```kotlin
interface RelayFeature
```

**Purpose:**
- Type safety boundary
- Clear contract for platform implementations
- Enables generic type constraints

### 3. WeakRef (Platform-Specific)

Platform-agnostic weak reference wrapper.

**Common Interface:**
```kotlin
expect class WeakRef<T : Any>(referred: T) {
    fun get(): T?
    fun clear()
}
```

**Platform Implementations:**
- **Android**: Uses `java.lang.ref.WeakReference`
- **iOS**: Uses Kotlin Native `WeakReference`

### 4. MainThreadExecutor (Platform-Specific)

Platform-agnostic main thread dispatcher.

**Common Interface:**
```kotlin
expect fun runOnMain(block: () -> Unit)
expect fun isMainThread(): Boolean
```

**Platform Implementations:**
- **Android**: Uses `Handler(Looper.getMainLooper())`
- **iOS**: Uses `dispatch_async(dispatch_get_main_queue())`

## Data Flow

### Scenario 1: Dispatch When Implementation Exists

```
┌──────────────────┐
│  Shared Code     │
│  (Any Thread)    │
└────────┬─────────┘
         │
         │ 1. KRelay.dispatch<Feature> { ... }
         ▼
┌──────────────────────────────────┐
│  KRelay.dispatch()               │
│  • Check registry for Feature    │
│  • Found: WeakRef.get() != null  │
└────────┬─────────────────────────┘
         │
         │ 2. runOnMain { block(impl) }
         ▼
┌──────────────────┐
│  Main Thread     │
│  Execute block   │
└────────┬─────────┘
         │
         │ 3. block(implementation)
         ▼
┌──────────────────┐
│  Platform Impl   │
│  (Activity/VC)   │
└──────────────────┘
```

### Scenario 2: Dispatch When Implementation Missing (Queue)

```
┌──────────────────┐
│  Shared Code     │
│  (Background)    │
└────────┬─────────┘
         │
         │ 1. KRelay.dispatch<Feature> { ... }
         ▼
┌──────────────────────────────────┐
│  KRelay.dispatch()               │
│  • Check registry for Feature    │
│  • Missing: WeakRef.get() == null│
└────────┬─────────────────────────┘
         │
         │ 2. Add to pendingQueue
         ▼
┌──────────────────────────────────┐
│  Pending Queue                   │
│  Feature -> [action1, action2]   │
└──────────────────────────────────┘
```

### Scenario 3: Register Implementation (Replay)

```
┌──────────────────┐
│  Platform Code   │
│  (onCreate/init) │
└────────┬─────────┘
         │
         │ 1. KRelay.register<Feature>(impl)
         ▼
┌──────────────────────────────────┐
│  KRelay.register()               │
│  • Store WeakRef(impl)           │
│  • Check pendingQueue            │
│  • Found: [action1, action2]     │
└────────┬─────────────────────────┘
         │
         │ 2. Replay all actions
         ▼
┌──────────────────┐
│  Main Thread     │
│  action1(impl)   │
│  action2(impl)   │
└────────┬─────────┘
         │
         │ 3. Clear queue
         ▼
┌──────────────────────────────────┐
│  Pending Queue (empty)           │
└──────────────────────────────────┘
```

## Platform Implementations

### Android Implementation

#### WeakRef
```kotlin
// Uses java.lang.ref.WeakReference
actual class WeakRef<T : Any>(referred: T) {
    private val weakReference = JavaWeakReference(referred)
    actual fun get(): T? = weakReference.get()
    actual fun clear() = weakReference.clear()
}
```

**Lifecycle Integration:**
- References to Activities/Fragments are automatically cleared by GC
- No manual cleanup needed in most cases
- `onDestroy()` hook available for explicit cleanup if needed

#### MainThreadExecutor
```kotlin
// Uses Android Looper/Handler
actual fun runOnMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block() // Already on main thread
    } else {
        Handler(Looper.getMainLooper()).post(block)
    }
}
```

**Optimization:**
- Skips posting if already on main thread
- Uses lazy-initialized Handler instance
- Zero overhead for main thread calls

### iOS Implementation

#### WeakRef
```kotlin
// Uses Kotlin Native WeakReference
actual class WeakRef<T : Any>(referred: T) {
    private val weakReference = NativeWeakReference(referred)
    actual fun get(): T? = weakReference.get()
    actual fun clear() = weakReference.clear()
}
```

**Lifecycle Integration:**
- Automatic cleanup when ViewController is deallocated
- Works with SwiftUI and UIKit
- No ARC conflicts

#### MainThreadExecutor
```kotlin
// Uses GCD (Grand Central Dispatch)
actual fun runOnMain(block: () -> Unit) {
    if (NSThread.isMainThread) {
        block() // Already on main thread
    } else {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }
}
```

**Optimization:**
- Direct execution on main thread
- GCD handles queue management
- Compatible with Objective-C interop

## Roadmap

KRelay follows a strategic development plan focused on reliability, expansion, and enterprise readiness. See [ROADMAP.md](ROADMAP.md) for detailed plans.

### Strategic Phases

**Phase 1: Launch & Education** (Months 1-2)
- Maven Central publishing
- Community education content
- Visual proof demos
- "The Glue Code Standard" messaging

**Phase 2: Expansion** (Months 3-6) - v1.1, v1.2
- Desktop/Web platform support (JVM, Wasm/JS)
- Debugging tools and logging
- Performance optimization

**Phase 3: Enterprise Ready** (6+ months) - v2.0
- Instance-based API for modularization
- Dependency Injection support (Koin/Hilt)
- Super App architecture patterns

For complete roadmap details, see [ROADMAP.md](ROADMAP.md).

## Thread Safety

### Synchronization Strategy (v1.0.0 - Production Ready)

KRelay uses platform-specific thread locks for true thread safety.

**Lock Implementation (Common):**
```kotlin
expect class Lock() {
    fun <T> withLock(block: () -> T): T
}
```

**Android (ReentrantLock):**
```kotlin
actual class Lock {
    private val lock = ReentrantLock()
    actual fun <T> withLock(block: () -> T): T =
        lock.kotlinWithLock(block)
}
```

**iOS (pthread_mutex):**
```kotlin
actual class Lock {
    private val mutex: pthread_mutex_t = nativeHeap.alloc()

    init {
        pthread_mutex_init(mutex.ptr, null)
    }

    actual fun <T> withLock(block: () -> T): T {
        pthread_mutex_lock(mutex.ptr)
        try {
            return block()
        } finally {
            pthread_mutex_unlock(mutex.ptr)
        }
    }
}
```

**Usage in KRelay:**
```kotlin
object KRelay {
    @PublishedApi
    internal val lock = Lock()

    fun <reified T : RelayFeature> dispatch(action: (T) -> Unit) {
        lock.withLock {
            // All operations are thread-safe
        }
    }
}
```

**Critical Sections:**
1. Registry access (get/set) - ✅ Protected
2. Queue modifications (add/remove/clear) - ✅ Protected
3. Replay operations - ✅ Protected
4. Metrics updates - ✅ Protected

**Lock Granularity:**
- Fine-grained locks on individual operations
- Minimal lock holding time
- No nested locks (prevents deadlocks)
- Platform-optimized native locks
- Reentrant (same thread can acquire multiple times)

### Concurrency Scenarios

#### Multiple Threads Dispatching
```kotlin
Thread A: KRelay.dispatch<ToastFeature> { ... }
Thread B: KRelay.dispatch<NavFeature> { ... }
Thread C: KRelay.dispatch<ToastFeature> { ... }
```

**Handling:**
- Each dispatch acquires lock independently
- Queue appends are atomic
- No data races

#### Register During Dispatch
```kotlin
Thread A: KRelay.dispatch<Feature> { ... }  // Queuing
Thread B: KRelay.register<Feature>(impl)     // Replaying
```

**Handling:**
- Lock ensures either queue-then-register or register-then-skip-queue
- No lost actions
- Deterministic ordering

## Memory Management

### Weak References Strategy

```
┌─────────────────────────────────────┐
│  KRelay Registry                    │
│                                     │
│  ToastFeature -> WeakRef ───────┐   │
│                                 │   │
└─────────────────────────────────┼───┘
                                  │
                                  │ weak
                                  ▼
                        ┌──────────────────┐
                        │  Activity/VC     │
                        │  (Strong Ref)    │
                        └──────────────────┘
                                  │
                                  │ GC collects
                                  ▼
                        ┌──────────────────┐
                        │  null            │
                        └──────────────────┘
```

**Benefits:**
- No memory leaks from shared code
- Automatic cleanup on lifecycle events
- No need for manual unregister (but available)

### Queue Memory Management (v1.0.0 - Implemented)

**Problem:** Unbounded queue growth could cause memory leaks

**Solution:** Configurable limits with automatic expiry

```kotlin
// Configuration (v1.0.0)
KRelay.maxQueueSize = 100                  // Max actions per feature
KRelay.actionExpiryMs = 5 * 60 * 1000     // 5 minutes expiry

// QueuedAction wrapper
data class QueuedAction<T>(
    val action: (T) -> Unit,
    val timestamp: Long,
    val priority: ActionPriority
) {
    fun isExpired(expiryMs: Long): Boolean =
        getCurrentTimeMs() - timestamp > expiryMs
}
```

**Benefits:**
- Prevents unbounded memory growth
- Automatic cleanup of stale actions
- Priority-based queue management
- Configurable per application needs

## Queue & Replay Mechanism

### Queue Structure

```kotlin
private val pendingQueue = mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()
```

**Key Properties:**
- Per-feature queues (isolated)
- FIFO ordering within each feature
- Type-erased action wrappers
- In-memory storage

### Replay Algorithm

```kotlin
fun register(impl: T) {
    synchronized(lock) {
        // 1. Store weak reference
        registry[T::class] = WeakRef(impl)

        // 2. Get pending queue
        val queue = pendingQueue[T::class]

        // 3. Replay actions
        if (!queue.isNullOrEmpty()) {
            val actions = queue.toList() // Copy to avoid concurrent modification
            queue.clear()

            runOnMain {
                actions.forEach { action ->
                    try {
                        action(impl)
                    } catch (e: Exception) {
                        log("Error replaying action: ${e.message}")
                    }
                }
            }
        }
    }
}
```

**Features:**
- Atomic queue clearing
- Error handling per action
- Main thread execution guarantee
- Copy-on-iterate (thread-safe)

## Design Decisions

### 1. Why Singleton?

**Decision:** Use `object KRelay` instead of instance-based API

**Rationale:**
- Single global registry makes sense for app-wide features
- Simpler API (`KRelay.dispatch` vs `relay.dispatch`)
- Less boilerplate in shared code
- Matches platform patterns (e.g., `Dispatchers.Main`)

**Trade-offs:**
- Harder to test (mitigated by `reset()` function)
- Global state (acceptable for infrastructure)

### 2. Why Reified Generics?

**Decision:** Use `inline fun <reified T>` for type-safe dispatch

**Rationale:**
- Type safety at compile time
- Better IDE support (autocomplete, refactoring)
- No string-based keys
- KClass available at runtime

**Trade-offs:**
- Not callable from Swift/Objective-C directly
- Requires wrapper functions for iOS (provided)

### 3. Why WeakRef Instead of Lifecycle Observers?

**Decision:** Use WeakReference instead of lifecycle callbacks

**Rationale:**
- Platform-agnostic (works on both Android and iOS)
- No lifecycle coupling in shared code
- Automatic cleanup
- Simpler implementation

**Trade-offs:**
- Less precise cleanup timing
- Requires null checks

### 4. Why Queue Instead of Drop?

**Decision:** Queue actions when implementation missing instead of dropping

**Rationale:**
- Better UX (actions not lost during rotation)
- Supports cold-start scenarios
- Predictable behavior

**Trade-offs:**
- Memory overhead (queue storage)
- Potential stale actions (addressed in v1.1 with expiry)

### 5. Why Always Main Thread?

**Decision:** Always dispatch to main thread

**Rationale:**
- 99% of platform features are UI-related
- Prevents threading bugs
- Consistent behavior across platforms

**Trade-offs:**
- Slight overhead for already-main-thread calls (optimized with check)
- Not suitable for background operations (not the use case)

## Performance Considerations

### Memory Footprint

**Per Feature:**
- WeakRef: ~16 bytes (object header + reference)
- Queue entry: ~32 bytes (lambda wrapper + metadata)

**Typical App:**
- 5-10 features = ~80-160 bytes registry
- 0-20 queued actions = ~0-640 bytes queue
- **Total: < 1KB**

### CPU Overhead

**Per Dispatch:**
1. Map lookup: O(1) ~10-50ns
2. Null check: O(1) ~5ns
3. Main thread post: O(1) ~100ns-1µs

**Total: < 2µs per dispatch** (negligible)

### GC Impact

**Weak References:**
- No GC pressure (references cleared automatically)
- No finalizers (GC-friendly)

**Queue Lambda Wrappers:**
- Short-lived objects
- Young generation collection
- Minimal impact

## Known Limitations & Trade-offs

### 1. Singleton Architecture

#### Design Choice
KRelay uses a global singleton (`object KRelay`) for simplicity and convenience.

#### Trade-offs

**Advantages:**
- Zero-configuration API
- Global access from shared code
- No dependency injection setup
- Matches platform patterns (like `Dispatchers.Main`)

**Limitations:**
- **Enterprise/Super Apps**: In large applications with multiple independent modules, a shared global KRelay can cause:
  - Feature naming conflicts
  - Difficulty isolating module-specific concerns
  - Complex testing scenarios requiring careful reset
- **Testing**: Requires `KRelay.reset()` in test setup/teardown to avoid state pollution
- **Module Isolation**: Hard to test modules in complete isolation

#### Recommendations

**For Large-Scale Apps:**
Consider feature namespacing:
```kotlin
// Module A
interface ModuleAToastFeature : RelayFeature { ... }

// Module B
interface ModuleBToastFeature : RelayFeature { ... }
```

**Future Enhancement (v2.0):**
Instance-based API for Dependency Injection:
```kotlin
class ModuleADI {
    val krelay = KRelay.create("ModuleA")
}
```

### 2. Lambda Serialization & Process Death

#### The Problem

KRelay stores lambda functions in memory. **Lambdas cannot be serialized**, which means:

- Queue does **NOT** survive process death
- When OS kills the app (low memory, user swipes away), all queued actions are lost
- When user reopens the app, queue is empty

#### Technical Explanation

```kotlin
// This lambda is stored in memory only
KRelay.dispatch<ToastFeature> { toast ->
    toast.show("Hello") // Cannot be saved to disk
}
```

Lambdas capture context and contain executable code, which cannot be serialized to persistent storage (SharedPreferences, Room, etc.).

#### Impact Analysis

**✅ Safe Use Cases** (KRelay is designed for):
- **UI Operations**: Toast, Snackbar, Dialog, Navigation
- **Ephemeral Commands**: Screen refresh, UI state updates
- **Non-Critical Events**: Analytics (if loss is acceptable), logging
- **Rotation Handling**: Queue survives Activity recreation (same process)

**❌ Unsafe Use Cases** (DO NOT use KRelay):
- **Critical Transactions**: Banking transfers, payments, orders
- **Important Analytics**: Events that must be tracked
- **Data Operations**: Database writes, network calls that must complete
- **Background Work**: Long-running tasks that need guaranteed execution

#### Example - Wrong Usage

```kotlin
// ❌ WRONG: Critical operation in KRelay
class PaymentViewModel {
    fun processPayment(amount: Double) {
        KRelay.dispatch<PaymentFeature> {
            it.sendPayment(amount) // LOST if process dies!
        }
    }
}
```

**What happens:**
1. User initiates payment
2. Action queued in KRelay
3. OS kills app (low memory)
4. User reopens app
5. **Payment never executed** ❌

#### Example - Correct Usage

```kotlin
// ✅ CORRECT: Use WorkManager for critical operations
class PaymentViewModel(
    private val workManager: WorkManager
) {
    fun processPayment(amount: Double) {
        // Critical operation: Use WorkManager (survives process death)
        val paymentWork = OneTimeWorkRequestBuilder<PaymentWorker>()
            .setInputData(workDataOf("amount" to amount))
            .build()
        workManager.enqueue(paymentWork)

        // UI feedback: Use KRelay (ephemeral)
        KRelay.dispatch<ToastFeature> {
            it.show("Processing payment...")
        }
    }
}
```

#### Alternatives for Critical Operations

| Operation Type | Recommended Solution |
|----------------|---------------------|
| Critical Background Work | **WorkManager** (Android), **Background Tasks** (iOS) |
| UI State Persistence | **SavedStateHandle**, **ViewModel.savedStateHandle** |
| Data Persistence | **Room**, **SQLite**, **DataStore** |
| Guaranteed Event Delivery | **Firebase Analytics**, **Persistent Queue Libraries** |
| Network Operations | **Retrofit** with **WorkManager** for retry |

### 3. In-Memory Queue Bounds

#### Current Limitation (v1.0.0)

- Queue is unbounded (grows indefinitely)
- If implementation never registers, queue keeps growing
- Potential memory leak in edge cases

#### Example Scenario

```kotlin
// App starts, ViewModel initializes before UI
repeat(1000) {
    KRelay.dispatch<ToastFeature> {
        it.show("Message $it")
    }
}
// If ToastFeature never registers → 1000 lambdas in memory
```

#### Planned Solution (v1.1.0)

```kotlin
// Configurable queue limits
KRelay.maxQueueSize = 100
KRelay.actionExpiryMs = 5 * 60 * 1000 // 5 minutes

// Auto-cleanup of old actions
```

### 4. No Built-in Prioritization (v1.0.0)

All actions are processed in FIFO order. Critical actions wait behind non-critical ones.

**Planned for v1.1.0:**
```kotlin
enum class ActionPriority { LOW, NORMAL, HIGH, CRITICAL }
KRelay.dispatchWithPriority<ErrorFeature>(CRITICAL) { ... }
```

### 5. Thread Safety (v1.0.0 Note)

Current implementation uses placeholder synchronization. True thread safety planned for v1.1.0 with platform-specific locks.

**v1.1.0 Enhancement:**
- Android: `ReentrantLock`
- iOS: `pthread_mutex`

## Implemented in v1.0.0

### ✅ Thread Safety with Platform Locks

```kotlin
// Android: ReentrantLock
// iOS: pthread_mutex
lock.withLock {
    // Thread-safe operations
}
```

### ✅ Priority System

```kotlin
enum class ActionPriority(val value: Int) {
    LOW(0),
    NORMAL(50),
    HIGH(100),
    CRITICAL(1000)
}

KRelay.dispatchWithPriority<T>(ActionPriority.CRITICAL) { ... }
```

### ✅ Performance Monitoring

```kotlin
KRelay.metricsEnabled = true
val metrics = KRelay.getMetrics<ToastFeature>()
KRelayMetrics.printReport()
```

### ✅ Queue Management

```kotlin
KRelay.maxQueueSize = 100
KRelay.actionExpiryMs = 5 * 60 * 1000
KRelay.clearQueue<T>()
```

## Use Cases & Application Scenarios

### Ideal Use Cases (Production-Tested)

KRelay excels in scenarios where you need to call platform-specific UI/UX features from shared Kotlin code.

#### 1. Navigation Commands

**Scenario:** ViewModel in shared code needs to navigate after business logic completes.

```kotlin
// Shared ViewModel
class LoginViewModel {
    suspend fun login(email: String, password: String) {
        val result = authRepository.login(email, password)

        when {
            result.isSuccess -> {
                KRelay.dispatch<NavigationFeature> {
                    it.navigateToHome()
                }
            }
            result.needsVerification -> {
                KRelay.dispatch<NavigationFeature> {
                    it.navigateToVerification(email)
                }
            }
        }
    }
}

// Platform implementation
interface NavigationFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToVerification(email: String)
}
```

**Why KRelay?**
- Navigation is inherently platform-specific
- Fire-and-forget pattern (no return value needed)
- Safe to lose on process death (user can navigate again)

#### 2. Toast/Snackbar/Alert Messages

**Scenario:** Show user feedback from background operations.

```kotlin
// Shared UseCase
class SyncDataUseCase {
    suspend fun sync() {
        try {
            val items = api.fetchData()
            database.insertAll(items)

            KRelay.dispatch<ToastFeature> {
                it.show("Synced ${items.size} items")
            }
        } catch (e: Exception) {
            KRelay.dispatch<ToastFeature> {
                it.showError("Sync failed: ${e.message}")
            }
        }
    }
}
```

**Why KRelay?**
- Toast is UI-only, no business logic
- User can see the result when they're back
- Perfect for sticky queue pattern

#### 3. Permission Requests

**Scenario:** Shared code needs platform permissions.

```kotlin
// Shared ViewModel
class CameraViewModel {
    fun takePicture() {
        KRelay.dispatch<PermissionFeature> {
            it.requestCameraPermission { granted ->
                if (granted) startCamera()
                else showPermissionDenied()
            }
        }
    }
}

// Android implementation
class AndroidPermissionFeature(
    private val activity: Activity
) : PermissionFeature {
    override fun requestCameraPermission(callback: (Boolean) -> Unit) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
        // Store callback for result handling
    }
}
```

**Why KRelay?**
- Permission APIs are platform-specific
- Callback-based async pattern works well
- UI thread requirement handled automatically

#### 4. Haptic Feedback / Device Features

**Scenario:** Trigger device haptics, sounds, or other hardware features.

```kotlin
// Shared code
class GameViewModel {
    fun onPlayerScored() {
        score += 10

        // Trigger haptic feedback
        KRelay.dispatch<HapticFeature> {
            it.impact(style = HapticStyle.MEDIUM)
        }

        // Play sound
        KRelay.dispatch<SoundFeature> {
            it.playSuccess()
        }
    }
}
```

**Why KRelay?**
- Hardware features are platform-specific
- Fire-and-forget (no confirmation needed)
- Perfect for immediate feedback

#### 5. Analytics Events (Simple)

**Scenario:** Track user actions for analytics.

```kotlin
// Shared code
class CheckoutViewModel {
    fun completeOrder(orderId: String, amount: Double) {
        KRelay.dispatch<AnalyticsFeature> {
            it.track("order_completed", mapOf(
                "order_id" to orderId,
                "amount" to amount
            ))
        }
    }
}
```

**Why KRelay?**
- Analytics is fire-and-forget
- Losing an event on process death is acceptable for most apps
- For critical analytics, use persistent queue libraries instead

#### 6. Screen Rotation / Configuration Changes

**Scenario:** Preserve UI commands during Activity recreation.

```kotlin
// ViewModel survives rotation
class DataViewModel {
    fun loadData() {
        viewModelScope.launch {
            val data = repository.load()

            // This dispatch happens during rotation
            KRelay.dispatch<ToastFeature> {
                it.show("Loaded ${data.size} items")
            }
        }
    }
}

// Old Activity is destroyed
// KRelay queues the toast
// New Activity is created and registers
// Toast is shown automatically ✅
```

**Why KRelay?**
- Sticky queue preserves commands across rotation
- No need for SavedStateHandle for UI commands
- Automatic replay when new Activity registers

### Anti-Patterns (What NOT to Do)

#### ❌ 1. Synchronous Return Values

**Problem:** KRelay is async and one-way. Cannot return values synchronously.

```kotlin
// ❌ WRONG: This doesn't work
fun getBatteryLevel(): Int {
    var level = 0
    KRelay.dispatch<BatteryFeature> {
        level = it.getBatteryLevel() // Won't work!
    }
    return level // Returns 0, not actual level
}

// ✅ CORRECT: Use expect/actual
expect fun getBatteryLevel(): Int

// Or use callbacks
fun getBatteryLevel(callback: (Int) -> Unit) {
    KRelay.dispatch<BatteryFeature> {
        it.getBatteryLevel { level ->
            callback(level)
        }
    }
}
```

**Why it fails:**
- `runOnMain` is asynchronous
- Lambda executes later on main thread
- Function returns before lambda runs

#### ❌ 2. State Management

**Problem:** KRelay is for commands, not state.

```kotlin
// ❌ WRONG: Using KRelay for state
data class UiState(val loading: Boolean, val items: List<Item>)

class ViewModel {
    fun updateState(newState: UiState) {
        KRelay.dispatch<StateFeature> {
            it.updateUi(newState)
        }
    }
}

// ✅ CORRECT: Use StateFlow
class ViewModel {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadItems() {
        _uiState.update { it.copy(loading = true) }
        // Load data
        _uiState.update { it.copy(loading = false, items = data) }
    }
}
```

**Why StateFlow is better:**
- Supports bi-directional observation
- Handles configuration changes properly
- Type-safe state updates
- Compose/SwiftUI reactive updates

#### ❌ 3. Heavy Background Processing

**Problem:** KRelay executes on main thread.

```kotlin
// ❌ WRONG: Heavy work on main thread
KRelay.dispatch<ProcessingFeature> {
    it.processLargeFile() // Blocks UI! ANR on Android!
}

// ✅ CORRECT: Use Dispatchers.IO
viewModelScope.launch(Dispatchers.IO) {
    processLargeFile()

    // Then notify UI on main thread
    withContext(Dispatchers.Main) {
        KRelay.dispatch<ToastFeature> {
            it.show("Processing complete!")
        }
    }
}
```

**Why it fails:**
- `runOnMain` executes on UI thread
- Heavy work freezes UI
- Android shows ANR dialog
- iOS shows spinning wheel

#### ❌ 4. Request-Response Patterns

**Problem:** KRelay doesn't support request-response flow.

```kotlin
// ❌ WRONG: Trying to get response
suspend fun fetchUserData(): User? {
    var user: User? = null

    KRelay.dispatch<ApiFeature> {
        user = it.getUser() // Doesn't work!
    }

    return user // Always null
}

// ✅ CORRECT: Use Repository pattern
class UserRepository {
    private val api = UserApi()

    suspend fun fetchUser(): User {
        return api.getUser() // Direct call
    }
}
```

**Why it fails:**
- Async execution means function returns before lambda runs
- Cannot wait for KRelay dispatch to complete
- Not designed for request-response

#### ❌ 5. Critical Data Operations

**Problem:** Queue doesn't survive process death.

```kotlin
// ❌ WRONG: Database writes in KRelay
fun saveUser(user: User) {
    KRelay.dispatch<DatabaseFeature> {
        it.insertUser(user) // Lost if process dies!
    }
}

// ✅ CORRECT: Direct database access
suspend fun saveUser(user: User) {
    database.userDao().insert(user)

    // Use KRelay only for UI feedback
    KRelay.dispatch<ToastFeature> {
        it.show("User saved")
    }
}
```

**Why it fails:**
- Process death clears queue
- Critical data is lost
- No transaction guarantees

### Decision Framework

Use this flowchart to decide if KRelay is appropriate:

```
Is it a platform-specific UI/UX feature?
├─ No → Don't use KRelay
│         Use: expect/actual, Repository, ViewModel
│
└─ Yes → Does it need to return a value immediately?
    ├─ Yes → Don't use KRelay
    │         Use: expect/actual, suspend functions
    │
    └─ No → Is it critical business logic?
        ├─ Yes → Don't use KRelay
        │         Use: WorkManager, Room, DataStore
        │
        └─ No → ✅ Use KRelay!
                  Examples: Toast, Navigation, Haptic
```

### Comparison with Alternatives

| Feature | KRelay | expect/actual | StateFlow | WorkManager |
|---------|--------|---------------|-----------|-------------|
| **Platform Calls** | ✅ Excellent | ✅ Good | ❌ No | ❌ No |
| **Return Values** | ❌ No | ✅ Yes | ✅ Yes | ⚠️ Async only |
| **State Management** | ❌ No | ❌ No | ✅ Excellent | ❌ No |
| **Guaranteed Execution** | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| **Process Death Survival** | ❌ No | ✅ Yes | ⚠️ Depends | ✅ Yes |
| **Queue/Replay** | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes |
| **Setup Complexity** | ✅ Minimal | ⚠️ Medium | ⚠️ Medium | ⚠️ High |
| **Best For** | UI Commands | Platform APIs | UI State | Background Work |

## Integration Patterns with Navigation Libraries

### Philosophy: "Ký sinh" Strategy

KRelay doesn't replace navigation libraries—it **enhances** them by providing a clean bridge between business logic and platform navigation.

**The Pattern:**
```
┌─────────────────────────────────────────┐
│     Shared Business Logic               │
│     (ViewModels, UseCases)              │
│                                         │
│     KRelay.dispatch<NavFeature>()       │
└─────────────┬───────────────────────────┘
              │ Fire & Forget
              ▼
┌─────────────────────────────────────────┐
│          KRelay Bridge                  │
│    (Type-safe, Lifecycle-aware)         │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│    Platform Navigation Library          │
│   (Voyager, Decompose, NavController)   │
│                                         │
│   Manages: Stack, Backstack, Routing    │
└─────────────────────────────────────────┘
```

### Voyager Integration Architecture

**Component Responsibilities:**

1. **NavigationFeature Interface (Common)**
   - Defines navigation contract
   - Platform-agnostic
   - Versioned alongside ViewModels

2. **ViewModel (Common)**
   - Pure business logic
   - No Navigator reference
   - Testable without navigation library

3. **VoyagerNavigationFeature (Platform)**
   - Wraps Voyager's Navigator
   - Translates KRelay commands → Voyager calls
   - Lifecycle-bound to Navigator

4. **Voyager (Platform)**
   - Handles actual navigation
   - Manages screen stack
   - Provides CurrentScreen composable

**Example Architecture:**

```kotlin
// Layer 1: Business Logic (commonMain)
class LoginViewModel {
    fun onLoginSuccess() {
        // No platform dependencies!
        KRelay.dispatch<NavigationFeature> {
            it.navigateToHome()
        }
    }
}

// Layer 2: Navigation Contract (commonMain)
interface NavigationFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToProfile(userId: String)
}

// Layer 3: Platform Bridge (androidMain/iosMain)
class VoyagerNavigationFeature(
    private val navigator: Navigator
) : NavigationFeature {
    override fun navigateToHome() {
        // Translate to Voyager API
        navigator.push(HomeScreen())
    }

    override fun navigateToProfile(userId: String) {
        navigator.push(ProfileScreen(userId))
    }
}

// Layer 4: Composition Root (Platform UI)
@Composable
fun App() {
    Navigator(LoginScreen()) { navigator ->
        // Wire everything together
        LaunchedEffect(navigator) {
            KRelay.register(VoyagerNavigationFeature(navigator))
        }
        CurrentScreen()
    }
}
```

**Lifecycle Flow:**

```
1. App Starts
   ├─→ Voyager Navigator created
   ├─→ VoyagerNavigationFeature wraps Navigator
   └─→ KRelay.register(VoyagerNavigationFeature)

2. User Interacts
   ├─→ Button clicked
   ├─→ ViewModel.onLoginSuccess() called
   └─→ KRelay.dispatch<NavigationFeature> { it.navigateToHome() }

3. KRelay Processes
   ├─→ Check registry for NavigationFeature
   ├─→ Found: VoyagerNavigationFeature instance
   ├─→ runOnMain { navigateToHome() }
   └─→ VoyagerNavigationFeature.navigateToHome() executed

4. Voyager Navigates
   ├─→ navigator.push(HomeScreen())
   └─→ UI updates to show HomeScreen
```

### Decompose Integration Architecture

Decompose uses a component-based architecture. KRelay integrates at the component level:

```kotlin
// Root Component implements NavigationFeature
class RootComponent(
    componentContext: ComponentContext,
    private val onNavigateToHome: () -> Unit
) : ComponentContext by componentContext, NavigationFeature {

    init {
        // Component registers itself as the navigation implementation
        KRelay.register<NavigationFeature>(this)
    }

    override fun navigateToHome() {
        onNavigateToHome()
    }

    // Component lifecycle automatically manages KRelay registration
    override fun onDestroy() {
        super.onDestroy()
        KRelay.unregister<NavigationFeature>()
    }
}
```

**Benefits:**
- Component lifecycle = KRelay lifecycle
- Type-safe navigation
- Testable components

### Compose Navigation Integration

For Jetpack Compose Navigation or Compose Multiplatform Navigation:

```kotlin
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // Register navigation implementation
    LaunchedEffect(navController) {
        KRelay.register(ComposeNavigationFeature(navController))
    }

    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("profile/{userId}") { ProfileScreen() }
    }
}

class ComposeNavigationFeature(
    private val navController: NavHostController
) : NavigationFeature {
    override fun navigateToHome() {
        navController.navigate("home") {
            popUpTo("login") { inclusive = true }
        }
    }
}
```

### Testing Strategy

**Without KRelay (Tight Coupling):**

```kotlin
// ❌ ViewModel depends on Navigator - Hard to test
class LoginViewModel(private val navigator: Navigator) {
    fun onLoginSuccess() {
        navigator.push(HomeScreen())
    }
}

// Test requires mocking Navigator
class LoginViewModelTest {
    @Test
    fun test() {
        val mockNavigator = mockk<Navigator>()
        val viewModel = LoginViewModel(mockNavigator)
        viewModel.onLoginSuccess()
        verify { mockNavigator.push(any<HomeScreen>()) }
    }
}
```

**With KRelay (Decoupled):**

```kotlin
// ✅ ViewModel has zero dependencies - Easy to test
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<NavigationFeature> { it.navigateToHome() }
    }
}

// Simple test with mock NavigationFeature
class LoginViewModelTest {
    @Test
    fun `login success should navigate to home`() {
        val mockNav = MockNavigationFeature()
        KRelay.register<NavigationFeature>(mockNav)

        viewModel.onLoginSuccess()

        assertTrue(mockNav.navigatedToHome)
    }
}

class MockNavigationFeature : NavigationFeature {
    var navigatedToHome = false
    override fun navigateToHome() { navigatedToHome = true }
}
```

**Benefits:**
1. No mocking libraries needed
2. Fast tests (no framework overhead)
3. Clear test intent
4. Easy to verify navigation calls

### Migration Strategy

**Migrating existing code to use KRelay:**

**Step 1: Extract Navigation Interface**

```kotlin
// Before: ViewModel has Navigator dependency
class LoginViewModel(private val navigator: Navigator) {
    fun onLoginSuccess() {
        navigator.push(HomeScreen())
    }
}

// After: Create navigation contract
interface NavigationFeature : RelayFeature {
    fun navigateToHome()
}
```

**Step 2: Update ViewModel**

```kotlin
// Remove Navigator dependency
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<NavigationFeature> {
            it.navigateToHome()
        }
    }
}
```

**Step 3: Create Platform Implementation**

```kotlin
class VoyagerNavigationFeature(private val navigator: Navigator) : NavigationFeature {
    override fun navigateToHome() {
        navigator.push(HomeScreen())
    }
}
```

**Step 4: Register at App Root**

```kotlin
@Composable
fun App() {
    Navigator(LoginScreen()) { navigator ->
        LaunchedEffect(navigator) {
            KRelay.register(VoyagerNavigationFeature(navigator))
        }
        CurrentScreen()
    }
}
```

### Comparison: Direct vs KRelay Integration

| Aspect | Direct Navigator Dependency | KRelay Integration |
|--------|----------------------------|-------------------|
| **ViewModel Dependencies** | Requires Navigator injection | Zero dependencies |
| **Testing Complexity** | Requires mocking Navigator | Simple mock interface |
| **Platform Coupling** | Tight coupling to nav library | Zero coupling |
| **Library Migration** | Rewrite all ViewModels | Only rewrite implementation |
| **Code in ViewModel** | Navigation logic mixed with business logic | Pure business logic |
| **Type Safety** | Depends on nav library | Type-safe interface |

### Advanced Patterns

#### Multi-Feature Navigation

For complex apps with multiple features:

```kotlin
interface AuthNavigationFeature : RelayFeature {
    fun navigateToLogin()
    fun navigateToSignup()
}

interface MainNavigationFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToProfile()
}

// Register both
KRelay.register<AuthNavigationFeature>(AuthNavImpl(navigator))
KRelay.register<MainNavigationFeature>(MainNavImpl(navigator))
```

#### Deep Linking Integration

```kotlin
class DeepLinkHandler(private val navigator: Navigator) : NavigationFeature {
    override fun navigateToProfile(userId: String) {
        // Handle deep link
        navigator.push(ProfileScreen(userId))
    }
}

// Deep link triggers navigation via KRelay
fun handleDeepLink(url: String) {
    val userId = extractUserId(url)
    KRelay.dispatch<NavigationFeature> {
        it.navigateToProfile(userId)
    }
}
```

## Design Philosophy: Unix Principles

### "Do One Thing and Do It Well"

KRelay follows the Unix philosophy religiously:

**One Responsibility:**
> Guarantee safe, leak-free dispatch of UI commands from Kotlin shared code to native platforms.

**Why So Focused?**

History teaches us that libraries attempting to solve every problem end up solving none well:

- **EventBus (GreenRobot)**: Started as simple event dispatch → Became everything (state, RPC, async) → Unmaintainable spaghetti code → Abandoned for cleaner alternatives
- **RxJava**: Tried to be state + async + events → Learning curve too steep → Coroutines won with simpler focus

**KRelay's Strength:** It does **ONE** thing perfectly. Don't dilute it.

## Future Enhancements (Aligned with Philosophy)

### v1.1.0: Performance & Reliability
- [ ] Platform-specific thread locks (ReentrantLock, pthread_mutex)
- [ ] Queue size limits and action expiry
- [ ] Action priorities (LOW, NORMAL, HIGH, CRITICAL)
- [ ] Performance metrics & monitoring

### v1.2.0: Platform Expansion
- [ ] Desktop (JVM) support
- [ ] Web/JS support
- [ ] Enhanced debugging tools

### v2.0.0: Advanced Configuration
- [ ] One-time vs Sticky events configuration
- [ ] Custom error handling strategies

## Non-Goals (By Design)

These features will **NEVER** be added to KRelay. They violate our core philosophy and would turn a focused tool into a bloated framework.

### ❌ Suspend Function Support / Return Values

**Why NOT:**
- Breaks "Fire-and-Forget" pattern
- Turns messenger into RPC framework
- Adds complexity: timeouts, cancellation, blocking
- Violates single responsibility

**The Trap:**
```kotlin
// ❌ This looks convenient but is WRONG philosophy
val confirmed = KRelay.dispatchSuspend<DialogFeature, Boolean> {
    it.showConfirmDialog("Delete?")
}
```

**Why It's Wrong:**
- Now devs must handle: "What if timeout?", "What if cancelled?", "What if UI not ready?"
- Lost the simplicity of fire-and-forget
- Better solution exists: `expect/actual`

**Right Way:**
```kotlin
// ✅ Use expect/actual for return values
expect suspend fun showConfirmDialog(title: String): Boolean

// Android
actual suspend fun showConfirmDialog(title: String): Boolean {
    return suspendCoroutine { continuation ->
        AlertDialog.Builder(context)
            .setTitle(title)
            .setPositiveButton("OK") { _, _ -> continuation.resume(true) }
            .setNegativeButton("Cancel") { _, _ -> continuation.resume(false) }
            .show()
    }
}
```

### ❌ State Management

**Why NOT:**
- StateFlow exists and does it better
- State ≠ Events (fundamental difference)
- Would compete with proven solutions

**The Trap:**
```kotlin
// ❌ WRONG: Using KRelay for state
data class UiState(val loading: Boolean, val items: List<Item>)
KRelay.dispatch<StateFeature> { it.updateState(newState) }
```

**Right Way:**
```kotlin
// ✅ Use StateFlow
class ViewModel {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
}
```

### ❌ Background Processing

**Why NOT:**
- Violates "Always Main Thread" guarantee
- Trust is KRelay's core value: "Code in this block is UI-safe"
- Breaking that trust breaks everything

**The Trap:**
```kotlin
// ❌ WRONG: Heavy work in KRelay
KRelay.dispatch<ProcessingFeature> {
    it.processLargeFile() // Blocks UI!
}
```

**Right Way:**
```kotlin
// ✅ Use Dispatchers.IO for heavy work
viewModelScope.launch(Dispatchers.IO) {
    processLargeFile()
    withContext(Dispatchers.Main) {
        KRelay.dispatch<ToastFeature> { it.show("Done!") }
    }
}
```

### ❌ Persistent Queue (Process Death Survival)

**Why NOT:**
- Lambdas can't be serialized (technical limitation)
- Adds massive complexity (serialization, deserialization, versioning)
- Better solutions exist (WorkManager, SavedStateHandle)

**The Trap:**
```kotlin
// ❌ This can't work - lambdas aren't serializable
KRelay.enablePersistence()
```

**Right Way:**
```kotlin
// ✅ Use WorkManager for critical tasks
val work = OneTimeWorkRequest<PaymentWorker>()
WorkManager.enqueue(work)

// ✅ Use SavedStateHandle for UI state
class ViewModel(private val savedState: SavedStateHandle) {
    var selectedTab by savedState.saveable { 0 }
}
```

### ❌ Built-in Dependency Injection

**Why NOT:**
- Not KRelay's scope
- Koin, Hilt already solve this
- Would balloon codebase 10x

**Right Way:**
```kotlin
// ✅ Use existing DI frameworks
class MyViewModel(
    private val repository: Repository // Injected by Koin/Hilt
) {
    fun showSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Success") }
    }
}
```

## The Cost of Feature Creep

**What happens if we ignore these Non-Goals?**

### Scenario: Adding Suspend Support

**Code Before (Clean):**
```kotlin
// KRelay.kt - Simple, 200 lines
inline fun <reified T : RelayFeature> dispatch(action: (T) -> Unit) {
    lock.withLock {
        val impl = registry[T::class]?.get()
        if (impl != null) {
            runOnMain { action(impl) }
        } else {
            queue.add(action)
        }
    }
}
```

**Code After (Bloated):**
```kotlin
// KRelay.kt - Complex, 800+ lines
suspend inline fun <reified T : RelayFeature, R> dispatchSuspend(
    timeout: Duration = 30.seconds,
    action: suspend (T) -> R
): Result<R> = withTimeout(timeout) {
    suspendCancellableCoroutine { continuation ->
        lock.withLock {
            val impl = registry[T::class]?.get()
            if (impl == null) {
                continuation.resume(Result.failure(NotRegisteredException()))
                return@withLock
            }
            // Now need: continuation storage, timeout handling, cancellation,
            // thread coordination, error handling, memory management...
        }
    }
}

// Plus: ContinuationManager, TimeoutHandler, ExceptionMapper...
// Result: 600+ lines of complexity for one feature
```

**Maintenance Nightmare:**
- Every change risks breaking suspend behavior
- Tests become 10x more complex
- New contributors can't understand codebase
- Bug reports spike

### Real-World Example: EventBus's Decline

**Year 2015:** EventBus is simple, focused
```java
// Simple API
EventBus.getDefault().post(new MessageEvent("Hello"));
```

**Year 2017:** Feature creep begins
- Added: Sticky events, priorities, thread modes, error handling

**Year 2020:** Unmaintainable
- Codebase: 3000+ lines
- Issues: Debugging nightmares, callback hell, memory leaks
- Developers: Fled to RxJava/Coroutines

**Year 2023:** Abandoned
- New projects don't use it
- Legacy projects stuck with it
- Lesson: Focus or die

## KRelay's Promise

**We Will:**
- ✅ Keep the codebase simple (<500 lines core)
- ✅ Maintain "Fire-and-Forget" guarantee
- ✅ Stay focused on UI dispatch
- ✅ Provide clear boundaries (what we do vs don't do)

**We Won't:**
- ❌ Add features that violate Unix philosophy
- ❌ Compete with specialized tools (StateFlow, WorkManager)
- ❌ Sacrifice simplicity for "convenience"
- ❌ Fall into the Feature Creep trap

**Why This Matters:**

When you use KRelay, you can trust:
1. It will never break your app with complexity
2. It will always be simple to understand
3. It will do **one thing perfectly**
4. Alternative solutions exist for other needs (and we'll point you to them)

---

**Last Updated:** 2026-01-22
**Version:** 1.0.0 (Production Ready)
**Philosophy:** Do One Thing and Do It Well

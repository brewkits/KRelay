package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * KRelay: The Native Interop Bridge for KMP
 *
 * Core singleton that manages:
 * 1. Safe Dispatch: Automatically switches to main thread
 * 2. Weak Registry: Holds platform implementations without memory leaks
 * 3. Sticky Queue: Queues actions when UI is not ready, replays when it becomes available
 *
 * ## v2.0 Update: Singleton + Instance API
 *
 * **Singleton API** (v1.0 - fully backward compatible):
 * ```kotlin
 * KRelay.register<ToastFeature>(impl)
 * KRelay.dispatch<ToastFeature> { it.show("Hello") }
 * ```
 *
 * **Instance API** (v2.0 - for Super Apps):
 * ```kotlin
 * val rideKRelay = KRelay.create("RideModule")
 * rideKRelay.register<ToastFeature>(impl)
 * // Note: register/dispatch on instance require inline wrapper
 * ```
 *
 * See [KRelayInstance] for when to use instances vs singleton.
 *
 * ## ⚠️ CRITICAL WARNINGS
 *
 * ### 1. Process Death: Queue is NOT Persistent
 * Lambda functions in the queue **cannot survive process death** (OS kills your app).
 * - ✅ **Safe**: Toast, Navigation, Haptics (UI feedback)
 * - ❌ **NEVER**: Payments, File Uploads, Critical Analytics
 * - 🔧 **Use Instead**: WorkManager for guaranteed execution
 *
 * ### 2. Singleton in Super Apps (v2.0 Solution Available)
 * Global singleton may conflict in apps with multiple independent modules.
 * - ✅ **Safe**: Single-module apps, small-medium projects
 * - ⚠️ **v1.0 Workaround**: Feature Namespacing (e.g., RideModuleToastFeature)
 * - ✅ **v2.0 Solution**: Use `KRelay.create("ModuleName")` for isolated instances
 *
 * See @ProcessDeathUnsafe and @SuperAppWarning for detailed guidance.
 *
 * Usage:
 * ```kotlin
 * // In shared code (ViewModel, UseCase, etc.)
 * @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
 * KRelay.dispatch<ToastFeature> { it.show("Hello!") }
 *
 * // In platform code (Activity, ViewController)
 * @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
 * KRelay.register(this as ToastFeature)
 * ```
 */
@SuperAppWarning
object KRelay {
    /**
     * Default instance used by singleton API (internal).
     * All singleton methods delegate to this instance.
     */
    @PublishedApi
    internal val defaultInstance = KRelayInstanceImpl(
        scopeName = "__DEFAULT__",
        maxQueueSize = 100,
        actionExpiryMs = 5 * 60 * 1000,
        debugMode = false
    )

    // ============================================================
    // SINGLETON API (v1.0 - Backward Compatible)
    // ============================================================

    /**
     * Thread-safe lock for all operations.
     * Exposed for backward compatibility with existing code.
     */
    @PublishedApi
    internal val lock: Lock
        get() = defaultInstance.lock

    /**
     * Registry: KClass -> WeakRef to platform implementation.
     * Exposed for backward compatibility with existing code.
     */
    @PublishedApi
    internal val registry: MutableMap<KClass<*>, WeakRef<Any>>
        get() = defaultInstance.registry

    /**
     * Sticky Queue: KClass -> List of pending actions with timestamps.
     * Exposed for backward compatibility with existing code.
     */
    @PublishedApi
    internal val pendingQueue: MutableMap<KClass<*>, MutableList<QueuedAction>>
        get() = defaultInstance.pendingQueue

    /**
     * Queue configuration: Maximum actions per feature type.
     * Default: 100
     */
    var maxQueueSize: Int
        get() = defaultInstance.maxQueueSize
        set(value) {
            defaultInstance.maxQueueSize = value
        }

    /**
     * Queue configuration: Action expiry time in milliseconds.
     * Default: 5 minutes (300,000ms)
     */
    var actionExpiryMs: Long
        get() = defaultInstance.actionExpiryMs
        set(value) {
            defaultInstance.actionExpiryMs = value
        }

    /**
     * Debug mode flag.
     * Default: false
     */
    var debugMode: Boolean
        get() = defaultInstance.debugMode
        set(value) {
            defaultInstance.debugMode = value
        }

    /**
     * Registers a platform implementation for a feature.
     *
     * This should be called from platform code (Activity onCreate, ViewController init, etc.)
     * When registered, any pending actions in the queue will be immediately replayed.
     *
     * ⚠️ **IMPORTANT**: Queued actions are lost on process death (OS kills app).
     * See @ProcessDeathUnsafe for safe vs dangerous use cases.
     *
     * @param impl The platform implementation of a RelayFeature
     */
    @ProcessDeathUnsafe
    inline fun <reified T : RelayFeature> register(impl: T) {
        defaultInstance.registerInternal(T::class, impl)
    }

    /**
     * Dispatches an action to the platform implementation.
     *
     * Thread Safety: Automatically switches to main thread before executing.
     * Memory Safety: Uses weak references to prevent memory leaks.
     * Reliability: Queues action if implementation is not available yet.
     * Queue Management: Enforces size limits and expires old actions.
     *
     * ⚠️ **CRITICAL WARNING 1**: Queue is lost on process death (OS kills app).
     * ⚠️ **CRITICAL WARNING 2**: Lambda captures can cause memory leaks.
     *
     * ## Process Death Risk
     * See @ProcessDeathUnsafe for safe vs dangerous use cases.
     *
     * **Safe Use Cases (UI feedback - acceptable to lose):**
     * - Toast/Snackbar notifications
     * - Navigation commands
     * - Haptic feedback / Vibration
     * - Permission requests
     * - In-app notifications
     *
     * **DANGEROUS Use Cases (NEVER use KRelay for these):**
     * - Banking/Payment transactions → Use WorkManager
     * - File uploads → Use UploadWorker
     * - Critical analytics → Use persistent queue
     * - Database writes → Use Room/SQLite directly
     *
     * ## Memory Leak Risk
     * See @MemoryLeakWarning for lambda capture best practices.
     *
     * **TL;DR Safe Pattern**:
     * ```kotlin
     * // ✅ Good: Capture primitives only
     * val message = viewModel.data
     * KRelay.dispatch<ToastFeature> { it.show(message) }
     *
     * // ❌ Bad: Captures entire ViewModel
     * KRelay.dispatch<ToastFeature> { it.show(viewModel.data) }
     * ```
     *
     * @param block The action to execute on the platform implementation
     */
    @ProcessDeathUnsafe
    @MemoryLeakWarning
    inline fun <reified T : RelayFeature> dispatch(noinline block: (T) -> Unit) {
        defaultInstance.dispatchInternal(T::class, block)
    }

    /**
     * Unregisters an implementation.
     *
     * Usually not needed as WeakRef will be cleared automatically when the object is GC'd.
     * However, can be useful for explicit cleanup in some scenarios.
     */
    inline fun <reified T : RelayFeature> unregister() {
        defaultInstance.unregisterInternal(T::class)
    }

    /**
     * Clears the pending queue for a specific feature type.
     *
     * **IMPORTANT**: Use this to prevent Lambda Capture Leaks.
     *
     * ### Problem:
     * When you queue an action like:
     * ```kotlin
     * KRelay.dispatch<ToastFeature> { it.show(viewModel.data) }
     * ```
     * The lambda captures `viewModel` and any surrounding context.
     * If the queue holds this lambda for too long (e.g., user backgrounds the app
     * and never returns to that screen), it causes a memory leak.
     *
     * ### Solution:
     * Call this in ViewModel's `onCleared()` to explicitly release queued lambdas:
     * ```kotlin
     * override fun onCleared() {
     *     super.onCleared()
     *     KRelay.clearQueue<ToastFeature>()
     * }
     * ```
     *
     * **Note**: Actions already have an expiry time (`actionExpiryMs`), but this
     * provides manual control for immediate cleanup.
     */
    inline fun <reified T : RelayFeature> clearQueue() {
        defaultInstance.clearQueueInternal(T::class)
    }

    /**
     * Checks if an implementation is currently registered and alive.
     */
    inline fun <reified T : RelayFeature> isRegistered(): Boolean {
        return defaultInstance.isRegisteredInternal(T::class)
    }

    /**
     * Gets the number of pending actions for a feature type.
     * Automatically removes expired actions before counting.
     */
    inline fun <reified T : RelayFeature> getPendingCount(): Int {
        return defaultInstance.getPendingCountInternal(T::class)
    }

    /**
     * Gets the number of currently registered features.
     * Only counts features with alive implementations (not GC'd).
     */
    fun getRegisteredFeaturesCount(): Int = defaultInstance.getRegisteredFeaturesCount()

    /**
     * Gets the total number of pending actions across all features.
     * Automatically removes expired actions before counting.
     */
    fun getTotalPendingCount(): Int = defaultInstance.getTotalPendingCount()

    /**
     * Gets detailed debug information about KRelay's current state.
     *
     * @return DebugInfo object containing:
     *   - Number of registered features
     *   - List of registered feature names
     *   - Pending actions per feature
     *   - Total pending actions
     *   - Configuration settings
     */
    fun getDebugInfo(): DebugInfo = defaultInstance.getDebugInfo()

    /**
     * Dumps KRelay's current state to console for debugging.
     *
     * Output includes:
     * - Number of registered features and their names
     * - Pending actions per feature
     * - Total pending actions
     * - Configuration settings
     *
     * Example output:
     * ```
     * === KRelay Debug Dump ===
     * Registered Features: 3
     *   - ToastFeature (alive)
     *   - NavigationFeature (alive)
     *   - PermissionFeature (alive)
     *
     * Pending Actions by Feature:
     *   - ToastFeature: 2 events
     *   - PermissionFeature: 5 events
     *
     * Total Pending: 7 events
     * Expired & Removed: 2 events
     *
     * Configuration:
     *   - Max Queue Size: 100
     *   - Action Expiry: 300000ms (5.0 min)
     *   - Debug Mode: true
     * ========================
     * ```
     */
    fun dump() = defaultInstance.dump()

    /**
     * Clears all registrations and pending queues.
     * Useful for testing or complete reset scenarios.
     * Thread-safe operation.
     */
    fun reset() = defaultInstance.reset()

    /**
     * Internal logging function.
     * Exposed for backward compatibility.
     */
    @PublishedApi
    internal fun log(message: String) = defaultInstance.log(message)

    /**
     * Internal registration logic.
     * Exposed for backward compatibility with inline functions.
     */
    @PublishedApi
    internal fun <T : RelayFeature> registerInternal(kClass: KClass<T>, impl: T) {
        defaultInstance.registerInternal(kClass, impl)
    }

    /**
     * Internal unregister logic.
     * Exposed for backward compatibility with inline functions.
     */
    @PublishedApi
    internal fun <T : RelayFeature> unregisterInternal(kClass: KClass<T>) {
        defaultInstance.unregisterInternal(kClass)
    }

    /**
     * Internal clear queue logic.
     * Exposed for backward compatibility with inline functions.
     */
    @PublishedApi
    internal fun <T : RelayFeature> clearQueueInternal(kClass: KClass<T>) {
        defaultInstance.clearQueueInternal(kClass)
    }

    // ============================================================
    // INSTANCE API (v2.0 - NEW)
    // ============================================================

    /**
     * Registry of created instance scope names for duplicate detection.
     * Thread-safe access via instanceRegistryLock.
     */
    private val instanceRegistry = mutableSetOf<String>()
    private val instanceRegistryLock = Lock()

    /**
     * Creates a new KRelay instance with the given scope name.
     *
     * **Use Cases**:
     * - Super Apps with independent modules
     * - Multi-team projects requiring isolation
     * - Dependency Injection architectures
     *
     * **Example**:
     * ```kotlin
     * val rideKRelay = KRelay.create("RideModule")
     * val foodKRelay = KRelay.create("FoodModule")
     * // Each module has isolated registry
     * ```
     *
     * **Duplicate Scope Name Warning** (v2.0.1):
     * If `debugMode` is enabled and an instance with the same scope name already exists,
     * a warning will be logged. While technically allowed, duplicate scope names can
     * make debug logs confusing.
     *
     * **Note**: Instance methods like `register()` and `dispatch()` are not reified,
     * so you'll need to use extension functions or wrappers for type-safe calls.
     *
     * @param scopeName Unique identifier for this instance (used in debug logs, must not be blank)
     * @return New KRelayInstance with default configuration
     * @throws IllegalArgumentException if scopeName is blank
     */
    fun create(scopeName: String): KRelayInstance {
        // Validate scope name (v2.0.1)
        require(scopeName.isNotBlank()) { "scopeName must not be blank" }

        // Check for duplicate scope name (v2.0.1)
        instanceRegistryLock.withLock {
            if (debugMode && scopeName in instanceRegistry) {
                log("⚠️ [KRelay] Instance with scope '$scopeName' already exists. " +
                    "Consider using unique names to avoid confusion in debug logs.")
            }
            instanceRegistry.add(scopeName)
        }

        return KRelayInstanceImpl(scopeName)
    }

    /**
     * Creates a builder for configuring a KRelay instance.
     *
     * **Example**:
     * ```kotlin
     * val instance = KRelay.builder("MyModule")
     *     .maxQueueSize(50)
     *     .actionExpiry(60_000L)
     *     .debugMode(true)
     *     .build()
     * ```
     *
     * **Duplicate Scope Name Warning** (v2.0.1):
     * If `debugMode` is enabled and an instance with the same scope name already exists,
     * a warning will be logged when `build()` is called.
     */
    fun builder(scopeName: String): KRelayBuilder {
        return KRelayBuilder(scopeName, instanceRegistry, instanceRegistryLock)
    }

    /**
     * Clears the instance registry.
     * Internal method for testing purposes only.
     */
    @PublishedApi
    internal fun clearInstanceRegistry() {
        instanceRegistryLock.withLock {
            instanceRegistry.clear()
        }
    }
}

// ============================================================
// EXTENSION FUNCTIONS FOR TYPE-SAFE INSTANCE API
// ============================================================

/**
 * Type-safe register for KRelayInstance.
 *
 * Usage:
 * ```kotlin
 * val instance = KRelay.create("MyModule")
 * instance.register<ToastFeature>(myImpl)
 * ```
 */
@ProcessDeathUnsafe
inline fun <reified T : RelayFeature> KRelayInstance.register(impl: T) {
    if (this is KRelayInstanceImpl) {
        this.registerInternal(T::class, impl)
    } else {
        throw UnsupportedOperationException("Custom KRelayInstance implementations must override register()")
    }
}

/**
 * Type-safe dispatch for KRelayInstance.
 *
 * Usage:
 * ```kotlin
 * val instance = KRelay.create("MyModule")
 * instance.dispatch<ToastFeature> { it.show("Hello") }
 * ```
 */
@ProcessDeathUnsafe
@MemoryLeakWarning
inline fun <reified T : RelayFeature> KRelayInstance.dispatch(noinline block: (T) -> Unit) {
    if (this is KRelayInstanceImpl) {
        this.dispatchInternal(T::class, block)
    } else {
        throw UnsupportedOperationException("Custom KRelayInstance implementations must override dispatch()")
    }
}

/**
 * Type-safe unregister for KRelayInstance.
 */
inline fun <reified T : RelayFeature> KRelayInstance.unregister() {
    if (this is KRelayInstanceImpl) {
        this.unregisterInternal(T::class)
    } else {
        throw UnsupportedOperationException("Custom KRelayInstance implementations must override unregister()")
    }
}

/**
 * Type-safe isRegistered for KRelayInstance.
 */
inline fun <reified T : RelayFeature> KRelayInstance.isRegistered(): Boolean {
    return if (this is KRelayInstanceImpl) {
        this.isRegisteredInternal(T::class)
    } else {
        throw UnsupportedOperationException("Custom KRelayInstance implementations must override isRegistered()")
    }
}

/**
 * Type-safe getPendingCount for KRelayInstance.
 */
inline fun <reified T : RelayFeature> KRelayInstance.getPendingCount(): Int {
    return if (this is KRelayInstanceImpl) {
        this.getPendingCountInternal(T::class)
    } else {
        throw UnsupportedOperationException("Custom KRelayInstance implementations must override getPendingCount()")
    }
}

/**
 * Type-safe clearQueue for KRelayInstance.
 */
inline fun <reified T : RelayFeature> KRelayInstance.clearQueue() {
    if (this is KRelayInstanceImpl) {
        this.clearQueueInternal(T::class)
    } else {
        throw UnsupportedOperationException("Custom KRelayInstance implementations must override clearQueue()")
    }
}

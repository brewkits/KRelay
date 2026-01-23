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
 * ## ⚠️ CRITICAL WARNINGS
 *
 * ### 1. Process Death: Queue is NOT Persistent
 * Lambda functions in the queue **cannot survive process death** (OS kills your app).
 * - ✅ **Safe**: Toast, Navigation, Haptics (UI feedback)
 * - ❌ **NEVER**: Payments, File Uploads, Critical Analytics
 * - 🔧 **Use Instead**: WorkManager for guaranteed execution
 *
 * ### 2. Singleton in Super Apps
 * Global singleton may conflict in apps with multiple independent modules.
 * - ✅ **Safe**: Single-module apps, small-medium projects
 * - ⚠️ **Caution**: Super Apps (Grab/Gojek style) - use Feature Namespacing
 * - 🔜 **Future**: v2.0 will add instance-based KRelay for DI
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
    // Thread-safe lock for all operations
    @PublishedApi
    internal val lock = Lock()

    // Registry: KClass -> WeakRef to platform implementation
    @PublishedApi
    internal val registry = mutableMapOf<KClass<*>, WeakRef<Any>>()

    // Sticky Queue: KClass -> List of pending actions with timestamps
    @PublishedApi
    internal val pendingQueue = mutableMapOf<KClass<*>, MutableList<QueuedAction>>()

    // Queue configuration
    var maxQueueSize: Int = 100  // Maximum actions per feature type
    var actionExpiryMs: Long = 5 * 60 * 1000  // 5 minutes default

    // Debug mode flag
    var debugMode: Boolean = false

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
        registerInternal(T::class, impl)
    }

    /**
     * Internal registration logic with thread safety and expiry handling.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : RelayFeature> registerInternal(kClass: KClass<T>, impl: T) {
        val actionsToReplay = lock.withLock {
            if (debugMode) {
                log("📝 Registering ${kClass.simpleName}")
            }

            // Save weak reference
            registry[kClass] = WeakRef(impl as Any)

            // Check if there are pending actions
            val queue = pendingQueue[kClass]
            if (!queue.isNullOrEmpty()) {
                // Filter out expired actions
                val validActions = queue.filter { !it.isExpired(actionExpiryMs) }
                val expiredCount = queue.size - validActions.size

                if (expiredCount > 0 && debugMode) {
                    log("⏰ Removed $expiredCount expired action(s) for ${kClass.simpleName}")
                }

                queue.clear()

                if (debugMode && validActions.isNotEmpty()) {
                    log("🔄 Replaying ${validActions.size} pending action(s) for ${kClass.simpleName}")
                }

                validActions.toList() // Copy to avoid concurrent modification
            } else {
                emptyList()
            }
        }

        // Replay all valid actions on main thread (outside lock)
        if (actionsToReplay.isNotEmpty()) {
            runOnMain {
                actionsToReplay.forEach { queuedAction ->
                    try {
                        queuedAction.action(impl)
                    } catch (e: Exception) {
                        log("❌ Error replaying action for ${kClass.simpleName}: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Dispatches an action to the platform implementation.
     *
     * Thread Safety: Automatically switches to main thread before executing.
     * Memory Safety: Uses weak references to prevent memory leaks.
     * Reliability: Queues action if implementation is not available yet.
     * Queue Management: Enforces size limits and expires old actions.
     *
     * ⚠️ **CRITICAL WARNING**: Queue is lost on process death (OS kills app).
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
     * See @ProcessDeathUnsafe annotation for complete guidance.
     *
     * @param block The action to execute on the platform implementation
     */
    @ProcessDeathUnsafe
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : RelayFeature> dispatch(noinline block: (T) -> Unit) {
        val kClass = T::class

        val impl = lock.withLock {
            registry[kClass]?.get() as? T
        }

        if (impl != null) {
            // Case A: Implementation is alive -> Execute on main thread
            if (debugMode) {
                log("✅ Dispatching to ${kClass.simpleName}")
            }

            runOnMain {
                try {
                    block(impl)
                } catch (e: Exception) {
                    log("❌ Error executing action for ${kClass.simpleName}: ${e.message}")
                }
            }
        } else {
            // Case B: Implementation is dead/missing -> Queue for later
            lock.withLock {
                if (debugMode) {
                    log("⏸️  Implementation missing for ${kClass.simpleName}. Queuing action...")
                }

                val actionWrapper: (Any) -> Unit = { instance ->
                    block(instance as T)
                }

                val queue = pendingQueue.getOrPut(kClass) { mutableListOf() }

                // Remove expired actions before adding new one
                queue.removeAll { it.isExpired(actionExpiryMs) }

                // Check queue size limit
                if (queue.size >= maxQueueSize) {
                    // Remove oldest action (FIFO)
                    queue.removeAt(0)
                    if (debugMode) {
                        log("⚠️  Queue full for ${kClass.simpleName}. Removed oldest action.")
                    }
                }

                // Add new action with timestamp
                queue.add(QueuedAction(actionWrapper))
            }
        }
    }

    /**
     * Unregisters an implementation.
     *
     * Usually not needed as WeakRef will be cleared automatically when the object is GC'd.
     * However, can be useful for explicit cleanup in some scenarios.
     */
    inline fun <reified T : RelayFeature> unregister() {
        unregisterInternal(T::class)
    }

    /**
     * Internal unregister logic with thread safety.
     */
    @PublishedApi
    internal fun <T : RelayFeature> unregisterInternal(kClass: KClass<T>) {
        lock.withLock {
            if (debugMode) {
                log("🗑️  Unregistering ${kClass.simpleName}")
            }
            registry[kClass]?.clear()
            registry.remove(kClass)
        }
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
        clearQueueInternal(T::class)
    }

    /**
     * Internal clear queue logic with thread safety.
     */
    @PublishedApi
    internal fun <T : RelayFeature> clearQueueInternal(kClass: KClass<T>) {
        lock.withLock {
            val count = pendingQueue[kClass]?.size ?: 0
            pendingQueue.remove(kClass)
            if (debugMode) {
                log("🧹 Cleared queue for ${kClass.simpleName} ($count actions removed)")
            }
            KRelayMetrics.recordExpiry(kClass, count)
        }
    }

    /**
     * Checks if an implementation is currently registered and alive.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : RelayFeature> isRegistered(): Boolean {
        return lock.withLock {
            val weakRef = registry[T::class]
            weakRef?.get() != null
        }
    }

    /**
     * Gets the number of pending actions for a feature type.
     * Automatically removes expired actions before counting.
     */
    inline fun <reified T : RelayFeature> getPendingCount(): Int {
        return lock.withLock {
            val queue = pendingQueue[T::class]
            if (queue != null) {
                // Remove expired actions
                queue.removeAll { it.isExpired(actionExpiryMs) }
                queue.size
            } else {
                0
            }
        }
    }

    /**
     * Clears all registrations and pending queues.
     * Useful for testing or complete reset scenarios.
     * Thread-safe operation.
     */
    fun reset() {
        lock.withLock {
            if (debugMode) {
                log("🔄 Resetting KRelay - clearing all registrations and queues")
            }
            registry.values.forEach { it.clear() }
            registry.clear()
            pendingQueue.clear()
        }
    }

    /**
     * Internal logging function.
     */
    @PublishedApi
    internal fun log(message: String) {
        println("[KRelay] $message")
    }
}

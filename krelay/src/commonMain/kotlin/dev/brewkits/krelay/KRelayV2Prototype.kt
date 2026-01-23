package dev.brewkits.krelay

/**
 * PROTOTYPE FOR v2.0: Instance-Based KRelay API
 *
 * This file contains experimental code for KRelay v2.0 architecture.
 * It demonstrates how instance-based KRelay would work for Super Apps and DI scenarios.
 *
 * ## Goals for v2.0
 * 1. Support multiple KRelay instances for module isolation
 * 2. Enable Dependency Injection (DI) patterns
 * 3. Maintain backward compatibility with v1.0 singleton
 * 4. Provide factory methods for instance creation
 *
 * ## Usage Examples (Planned v2.0 API)
 *
 * ### Example 1: Module Isolation in Super Apps
 * ```kotlin
 * // Ride Module
 * val rideKRelay = KRelay.create("RideModule")
 * rideKRelay.register<ToastFeature>(RideToastImpl())
 * rideKRelay.dispatch<ToastFeature> { it.show("Ride booked!") }
 *
 * // Food Module
 * val foodKRelay = KRelay.create("FoodModule")
 * foodKRelay.register<ToastFeature>(FoodToastImpl())
 * foodKRelay.dispatch<ToastFeature> { it.show("Order placed!") }
 *
 * // Each module has isolated registry - no conflicts!
 * ```
 *
 * ### Example 2: Dependency Injection
 * ```kotlin
 * // Koin module definition
 * val rideModule = module {
 *     single { KRelay.create("RideModule") }
 *     viewModel { RideViewModel(kRelay = get()) }
 * }
 *
 * // ViewModel with injected KRelay
 * class RideViewModel(private val kRelay: KRelayInstance) : ViewModel() {
 *     fun bookRide() {
 *         kRelay.dispatch<ToastFeature> { it.show("Booking...") }
 *     }
 * }
 * ```
 *
 * ### Example 3: Testing with Isolated Instances
 * ```kotlin
 * @Test
 * fun testRideBooking() {
 *     val testKRelay = KRelay.create("TestInstance")
 *     val mockToast = MockToastFeature()
 *     testKRelay.register(mockToast)
 *
 *     val viewModel = RideViewModel(testKRelay)
 *     viewModel.bookRide()
 *
 *     assertTrue(mockToast.wasShowCalled)
 *     // No need for KRelay.reset() - instance is isolated
 * }
 * ```
 *
 * ## Status
 * - ⏳ **Not Yet Implemented**: This is prototype/design code only
 * - 🎯 **Target Release**: v2.0.0
 * - 💡 **Feedback Welcome**: Try the API design, report issues
 *
 * @see [ADR-0001](../../docs/adr/0001-singleton-and-serialization-tradeoffs.md)
 */

/**
 * Interface representing an instance of KRelay with isolated registry and queue.
 *
 * This is the core abstraction for v2.0 instance-based API.
 *
 * @property scopeName The name/identifier of this KRelay instance
 */
interface KRelayInstance {
    val scopeName: String

    /**
     * Registers a platform implementation for this instance.
     *
     * @param impl The platform implementation of a RelayFeature
     */
    fun <T : RelayFeature> register(impl: T)

    /**
     * Dispatches an action to the platform implementation in this instance.
     *
     * @param block The action to execute on the platform implementation
     */
    fun <T : RelayFeature> dispatch(block: (T) -> Unit)

    /**
     * Unregisters an implementation from this instance.
     */
    fun <T : RelayFeature> unregister(kClass: kotlin.reflect.KClass<T>)

    /**
     * Checks if an implementation is registered in this instance.
     */
    fun <T : RelayFeature> isRegistered(kClass: kotlin.reflect.KClass<T>): Boolean

    /**
     * Gets the number of pending actions for a feature type in this instance.
     */
    fun <T : RelayFeature> getPendingCount(kClass: kotlin.reflect.KClass<T>): Int

    /**
     * Clears the pending queue for a specific feature type in this instance.
     */
    fun <T : RelayFeature> clearQueue(kClass: kotlin.reflect.KClass<T>)

    /**
     * Resets this instance - clears all registrations and queues.
     */
    fun reset()
}

/**
 * PROTOTYPE: Internal implementation of KRelayInstance.
 *
 * Each instance maintains its own:
 * - Registry: Map of feature types to implementations
 * - Queue: Map of feature types to pending actions
 * - Configuration: maxQueueSize, actionExpiryMs, debugMode
 *
 * Thread-safety is ensured using a per-instance lock.
 */
/*
// COMMENTED OUT - This is prototype code, not yet production-ready

internal class KRelayInstanceImpl(
    override val scopeName: String
) : KRelayInstance {

    // Thread-safe lock for this instance
    private val lock = Lock()

    // Instance-specific registry and queue
    private val registry = mutableMapOf<KClass<*>, WeakRef<Any>>()
    private val pendingQueue = mutableMapOf<KClass<*>, MutableList<QueuedAction>>()

    // Instance-specific configuration (can be customized per instance in future)
    var maxQueueSize: Int = 100
    var actionExpiryMs: Long = 5 * 60 * 1000
    var debugMode: Boolean = false

    override fun <T : RelayFeature> register(impl: T) {
        val kClass = impl::class as KClass<T>
        registerInternal(kClass, impl)
    }

    private fun <T : RelayFeature> registerInternal(kClass: KClass<T>, impl: T) {
        val actionsToReplay = lock.withLock {
            if (debugMode) {
                log("[$scopeName] 📝 Registering ${kClass.simpleName}")
            }

            registry[kClass] = WeakRef(impl as Any)

            val queue = pendingQueue[kClass]
            if (!queue.isNullOrEmpty()) {
                val validActions = queue.filter { !it.isExpired(actionExpiryMs) }
                val expiredCount = queue.size - validActions.size

                if (expiredCount > 0 && debugMode) {
                    log("[$scopeName] ⏰ Removed $expiredCount expired action(s)")
                }

                queue.clear()

                if (debugMode && validActions.isNotEmpty()) {
                    log("[$scopeName] 🔄 Replaying ${validActions.size} pending action(s)")
                }

                validActions.toList()
            } else {
                emptyList()
            }
        }

        if (actionsToReplay.isNotEmpty()) {
            runOnMain {
                actionsToReplay.forEach { queuedAction ->
                    try {
                        queuedAction.action(impl)
                    } catch (e: Exception) {
                        log("[$scopeName] ❌ Error replaying action: ${e.message}")
                    }
                }
            }
        }
    }

    override fun <T : RelayFeature> dispatch(block: (T) -> Unit) {
        // Implementation similar to KRelay.dispatch()
        // See KRelay.kt for reference implementation
    }

    override fun <T : RelayFeature> unregister(kClass: KClass<T>) {
        lock.withLock {
            if (debugMode) {
                log("[$scopeName] 🗑️  Unregistering ${kClass.simpleName}")
            }
            registry[kClass]?.clear()
            registry.remove(kClass)
        }
    }

    override fun <T : RelayFeature> isRegistered(kClass: KClass<T>): Boolean {
        return lock.withLock {
            registry[kClass]?.get() != null
        }
    }

    override fun <T : RelayFeature> getPendingCount(kClass: KClass<T>): Int {
        return lock.withLock {
            val queue = pendingQueue[kClass]
            if (queue != null) {
                queue.removeAll { it.isExpired(actionExpiryMs) }
                queue.size
            } else {
                0
            }
        }
    }

    override fun <T : RelayFeature> clearQueue(kClass: KClass<T>) {
        lock.withLock {
            val count = pendingQueue[kClass]?.size ?: 0
            pendingQueue.remove(kClass)
            if (debugMode) {
                log("[$scopeName] 🧹 Cleared queue ($count actions removed)")
            }
        }
    }

    override fun reset() {
        lock.withLock {
            if (debugMode) {
                log("[$scopeName] 🔄 Resetting instance")
            }
            registry.values.forEach { it.clear() }
            registry.clear()
            pendingQueue.clear()
        }
    }

    private fun log(message: String) {
        println("[KRelay] $message")
    }
}
*/

/**
 * PROTOTYPE: Factory methods for creating KRelay instances.
 *
 * These would be extension functions on the existing KRelay object,
 * providing backward compatibility while adding new capabilities.
 */

/*
// COMMENTED OUT - Prototype extension functions for KRelay v2.0

// Extension on existing KRelay object for backward compatibility
fun KRelay.create(scopeName: String): KRelayInstance {
    // Could be enhanced with:
    // - Instance pooling/reuse
    // - Parent-child relationships (scoped instances)
    // - Configuration inheritance
    return KRelayInstanceImpl(scopeName)
}

// Alternative API: Builder pattern for advanced configuration
class KRelayBuilder internal constructor(private val scopeName: String) {
    private var maxQueueSize: Int = 100
    private var actionExpiryMs: Long = 5 * 60 * 1000
    private var debugMode: Boolean = false

    fun maxQueueSize(size: Int) = apply { this.maxQueueSize = size }
    fun actionExpiry(ms: Long) = apply { this.actionExpiryMs = ms }
    fun debugMode(enabled: Boolean) = apply { this.debugMode = enabled }

    fun build(): KRelayInstance {
        val instance = KRelayInstanceImpl(scopeName)
        instance.maxQueueSize = maxQueueSize
        instance.actionExpiryMs = actionExpiryMs
        instance.debugMode = debugMode
        return instance
    }
}

fun KRelay.builder(scopeName: String): KRelayBuilder {
    return KRelayBuilder(scopeName)
}

// Usage example:
// val instance = KRelay.builder("MyModule")
//     .maxQueueSize(50)
//     .actionExpiry(60_000)
//     .debugMode(true)
//     .build()
*/

/**
 * ## Migration Guide: v1.0 → v2.0 (Draft)
 *
 * ### Option 1: Keep Using Singleton (Zero Changes)
 * ```kotlin
 * // v1.0 code continues to work
 * KRelay.dispatch<ToastFeature> { it.show("Hello") }
 * KRelay.register<ToastFeature>(impl)
 * ```
 *
 * ### Option 2: Migrate to Instance-Based (Gradual)
 * ```kotlin
 * // Step 1: Create instance
 * val appKRelay = KRelay.create("MainApp")
 *
 * // Step 2: Replace KRelay.dispatch() with instance.dispatch()
 * // Before:
 * KRelay.dispatch<ToastFeature> { it.show("Hello") }
 * // After:
 * appKRelay.dispatch<ToastFeature> { it.show("Hello") }
 *
 * // Step 3: Use DI to inject instance into ViewModels
 * class MyViewModel(private val kRelay: KRelayInstance) {
 *     fun doSomething() {
 *         kRelay.dispatch<ToastFeature> { it.show("Done") }
 *     }
 * }
 * ```
 *
 * ### Option 3: Super App with Multiple Modules
 * ```kotlin
 * // Each module creates its own instance
 * object RideModule {
 *     val kRelay = KRelay.create("RideModule")
 *
 *     fun init(context: Context) {
 *         kRelay.register<ToastFeature>(RideToastImpl(context))
 *     }
 * }
 *
 * object FoodModule {
 *     val kRelay = KRelay.create("FoodModule")
 *
 *     fun init(context: Context) {
 *         kRelay.register<ToastFeature>(FoodToastImpl(context))
 *     }
 * }
 *
 * // ViewModels use module-specific instance
 * class RideViewModel : ViewModel() {
 *     fun bookRide() {
 *         RideModule.kRelay.dispatch<ToastFeature> {
 *             it.show("Ride booked!")
 *         }
 *     }
 * }
 * ```
 *
 * ## Open Questions for v2.0 Design
 *
 * 1. **Instance Lifecycle Management**
 *    - Should instances be automatically garbage collected?
 *    - Should we provide a `KRelay.dispose(instance)` method?
 *
 * 2. **Configuration Inheritance**
 *    - Should child instances inherit parent configuration?
 *    - Should we support instance hierarchies?
 *
 * 3. **Thread Safety**
 *    - Current per-instance lock is sufficient?
 *    - Any cross-instance synchronization needed?
 *
 * 4. **Metrics and Monitoring**
 *    - Should each instance have isolated metrics?
 *    - Should we provide a global `KRelay.getAllInstances()` API?
 *
 * 5. **Backward Compatibility**
 *    - Should singleton KRelay be an alias to a "default" instance?
 *    - Or should it remain a separate implementation?
 *
 * ## Feedback
 *
 * Please provide feedback on this design:
 * - Does the API feel natural?
 * - Are there missing use cases?
 * - Any concerns about performance or complexity?
 *
 * Open an issue: https://github.com/brewkits/krelay/issues
 */

/**
 * ## Implementation Roadmap (Draft)
 *
 * ### Phase 1: Core Instance API (v2.0-alpha)
 * - [ ] Implement KRelayInstanceImpl
 * - [ ] Add KRelay.create() factory method
 * - [ ] Write unit tests for instance isolation
 * - [ ] Update documentation
 *
 * ### Phase 2: Builder Pattern (v2.0-beta)
 * - [ ] Implement KRelayBuilder
 * - [ ] Support custom configuration per instance
 * - [ ] Add validation for configuration values
 *
 * ### Phase 3: DI Integration Examples (v2.0-rc)
 * - [ ] Provide Koin integration example
 * - [ ] Provide Hilt/Dagger integration example
 * - [ ] Document best practices for DI
 *
 * ### Phase 4: Stable Release (v2.0.0)
 * - [ ] Performance benchmarks (compare singleton vs instance)
 * - [ ] Migration guide with real-world examples
 * - [ ] Community feedback integration
 * - [ ] Production testing in Super App scenarios
 */

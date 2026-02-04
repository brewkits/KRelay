package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * Internal implementation of KRelayInstance.
 *
 * This class contains the same core logic as the KRelay singleton,
 * but operating on instance-level registry and queue.
 *
 * Each instance has:
 * - Isolated registry (WeakRef map)
 * - Isolated pending queue
 * - Isolated lock for thread safety
 * - Independent configuration
 */
@PublishedApi
internal class KRelayInstanceImpl(
    override val scopeName: String,
    override var maxQueueSize: Int = 100,
    override var actionExpiryMs: Long = 5 * 60 * 1000,
    override var debugMode: Boolean = false
) : KRelayInstance {

    // Instance-level lock
    @PublishedApi
    internal val lock = Lock()

    // Instance-level registry
    @PublishedApi
    internal val registry = mutableMapOf<KClass<*>, WeakRef<Any>>()

    // Instance-level pending queue
    @PublishedApi
    internal val pendingQueue = mutableMapOf<KClass<*>, MutableList<QueuedAction>>()

    // Note: register() is provided as extension function in KRelay.kt

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

    // Note: dispatch() is provided as extension function in KRelay.kt

    /**
     * Internal dispatch logic with type information.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : RelayFeature> dispatchInternal(kClass: KClass<T>, block: (T) -> Unit) {
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

    // Note: unregister() is provided as extension function in KRelay.kt

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

    // Note: isRegistered() is provided as extension function in KRelay.kt

    /**
     * Internal isRegistered logic.
     */
    @PublishedApi
    internal fun <T : RelayFeature> isRegisteredInternal(kClass: KClass<T>): Boolean {
        return lock.withLock {
            val weakRef = registry[kClass]
            weakRef?.get() != null
        }
    }

    // Note: getPendingCount() is provided as extension function in KRelay.kt

    /**
     * Internal getPendingCount logic.
     */
    @PublishedApi
    internal fun <T : RelayFeature> getPendingCountInternal(kClass: KClass<T>): Int {
        return lock.withLock {
            val queue = pendingQueue[kClass]
            if (queue != null) {
                // Remove expired actions
                queue.removeAll { it.isExpired(actionExpiryMs) }
                queue.size
            } else {
                0
            }
        }
    }

    // Note: clearQueue() is provided as extension function in KRelay.kt

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
     * Gets the number of currently registered features.
     */
    override fun getRegisteredFeaturesCount(): Int {
        return lock.withLock {
            registry.count { it.value.get() != null }
        }
    }

    /**
     * Gets the total number of pending actions across all features.
     */
    override fun getTotalPendingCount(): Int {
        return lock.withLock {
            var total = 0
            pendingQueue.forEach { (_, queue) ->
                queue.removeAll { it.isExpired(actionExpiryMs) }
                total += queue.size
            }
            total
        }
    }

    /**
     * Gets detailed debug information about this instance's state.
     */
    override fun getDebugInfo(): DebugInfo {
        return lock.withLock {
            val registeredFeatures = mutableListOf<String>()
            val featureQueues = mutableMapOf<String, Int>()
            var totalPending = 0
            var expiredCount = 0

            // Collect registered features (alive only)
            registry.forEach { (kClass, weakRef) ->
                if (weakRef.get() != null) {
                    registeredFeatures.add(kClass.simpleName ?: "Unknown")
                }
            }

            // Collect queue info and cleanup expired
            pendingQueue.forEach { (kClass, queue) ->
                val beforeSize = queue.size
                queue.removeAll { it.isExpired(actionExpiryMs) }
                val afterSize = queue.size

                expiredCount += (beforeSize - afterSize)

                if (afterSize > 0) {
                    featureQueues[kClass.simpleName ?: "Unknown"] = afterSize
                    totalPending += afterSize
                }
            }

            DebugInfo(
                registeredFeaturesCount = registeredFeatures.size,
                registeredFeatures = registeredFeatures,
                featureQueues = featureQueues,
                totalPendingActions = totalPending,
                expiredActionsRemoved = expiredCount,
                maxQueueSize = maxQueueSize,
                actionExpiryMs = actionExpiryMs,
                debugMode = debugMode
            )
        }
    }

    /**
     * Dumps this instance's state to console for debugging.
     */
    override fun dump() {
        val info = getDebugInfo()

        println("=== KRelay Instance Debug Dump [$scopeName] ===")
        println("Registered Features: ${info.registeredFeaturesCount}")
        if (info.registeredFeatures.isNotEmpty()) {
            info.registeredFeatures.forEach { featureName ->
                println("  - $featureName (alive)")
            }
        } else {
            println("  (none)")
        }

        println()
        println("Pending Actions by Feature:")
        if (info.featureQueues.isNotEmpty()) {
            info.featureQueues.forEach { (featureName, count) ->
                println("  - $featureName: $count events")
            }
        } else {
            println("  (none)")
        }

        println()
        println("Total Pending: ${info.totalPendingActions} events")
        if (info.expiredActionsRemoved > 0) {
            println("Expired & Removed: ${info.expiredActionsRemoved} events")
        }

        println()
        println("Configuration:")
        println("  - Max Queue Size: ${info.maxQueueSize}")
        println("  - Action Expiry: ${info.actionExpiryMs}ms (${info.actionExpiryMs / 60000.0} min)")
        println("  - Debug Mode: ${info.debugMode}")
        println("================================================")
    }

    /**
     * Clears all registrations and pending queues for this instance.
     */
    override fun reset() {
        lock.withLock {
            if (debugMode) {
                log("🔄 Resetting KRelay instance [$scopeName] - clearing all registrations and queues")
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
        println("[$scopeName] $message")
    }
}

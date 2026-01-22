package dev.brewkits.krelay

/**
 * Action priority levels for queue management.
 *
 * When multiple actions are queued, higher priority actions
 * will be executed first during replay.
 */
enum class ActionPriority(val value: Int) {
    /**
     * Low priority - executed last.
     * Use for: Analytics, logging, non-critical updates
     */
    LOW(0),

    /**
     * Normal priority - default.
     * Use for: Regular UI updates, standard notifications
     */
    NORMAL(50),

    /**
     * High priority - executed before normal.
     * Use for: Important notifications, navigation commands
     */
    HIGH(100),

    /**
     * Critical priority - executed first.
     * Use for: Error dialogs, critical user feedback, security alerts
     */
    CRITICAL(1000);

    companion object {
        /**
         * Default priority for all actions.
         */
        val DEFAULT = NORMAL
    }
}

/**
 * Dispatches an action with a specific priority.
 *
 * Higher priority actions will be replayed first when the implementation
 * becomes available.
 *
 * @param priority The priority level for this action
 * @param block The action to execute
 */
inline fun <reified T : RelayFeature> KRelay.dispatchWithPriority(
    priority: ActionPriority,
    noinline block: (T) -> Unit
) {
    dispatchWithPriorityInternal(T::class, priority.value, block)
}

/**
 * Internal implementation of priority dispatch.
 */
@PublishedApi
internal fun <T : RelayFeature> KRelay.dispatchWithPriorityInternal(
    kClass: kotlin.reflect.KClass<T>,
    priorityValue: Int,
    block: (T) -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    val impl = lock.withLock {
        registry[kClass]?.get() as? T
    }

    if (impl != null) {
        // Case A: Implementation is alive -> Execute on main thread
        if (debugMode) {
            log("✅ Dispatching to ${kClass.simpleName} with priority $priorityValue")
        }

        runOnMain {
            try {
                block(impl)
            } catch (e: Exception) {
                log("❌ Error executing action for ${kClass.simpleName}: ${e.message}")
            }
        }
    } else {
        // Case B: Implementation is dead/missing -> Queue with priority
        lock.withLock {
            if (debugMode) {
                log("⏸️  Implementation missing for ${kClass.simpleName}. Queuing action with priority $priorityValue...")
            }

            val actionWrapper: (Any) -> Unit = { instance ->
                block(instance as T)
            }

            val queue = pendingQueue.getOrPut(kClass) { mutableListOf() }

            // Remove expired actions before adding new one
            queue.removeAll { it.isExpired(actionExpiryMs) }

            // Check queue size limit
            if (queue.size >= maxQueueSize) {
                // Remove lowest priority action
                val lowestPriorityIndex = queue.indices.minByOrNull { queue[it].priority } ?: 0
                queue.removeAt(lowestPriorityIndex)
                if (debugMode) {
                    log("⚠️  Queue full for ${kClass.simpleName}. Removed lowest priority action.")
                }
            }

            // Add new action with timestamp and priority
            queue.add(QueuedAction(actionWrapper, priority = priorityValue))

            // Sort queue by priority (highest first)
            queue.sortByDescending { it.priority }
        }
    }
}

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
 * Dispatches an action with a specific priority on a [KRelayInstance].
 *
 * Higher priority actions will be replayed first when the implementation becomes available.
 * This method is consistent with [KRelay.dispatchWithPriority] for the singleton API.
 *
 * @param priority The priority level for this action
 * @param block The action to execute
 */
@ProcessDeathUnsafe
@MemoryLeakWarning
inline fun <reified T : RelayFeature> KRelayInstance.dispatchWithPriority(
    priority: ActionPriority,
    noinline block: (T) -> Unit
) {
    if (this is KRelayInstanceImpl) {
        this.dispatchWithPriorityInternal(T::class, priority.value, block)
    } else {
        throw UnsupportedOperationException(
            "Custom KRelayInstance implementations must override dispatchWithPriority()"
        )
    }
}

/**
 * Internal implementation of priority dispatch (singleton).
 * Delegates queue management to [defaultInstance.enqueueActionUnderLock].
 */
@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : RelayFeature> KRelay.dispatchWithPriorityInternal(
    kClass: kotlin.reflect.KClass<T>,
    priorityValue: Int,
    block: (T) -> Unit
) {
    val impl = lock.withLock {
        registry[kClass]?.get() as? T
    }

    if (impl != null) {
        if (debugMode) log("✅ Dispatching to ${kClass.simpleName} with priority $priorityValue")
        KRelayMetrics.recordDispatch(kClass)
        runOnMain {
            try {
                block(impl)
            } catch (e: Exception) {
                log("❌ Error executing action for ${kClass.simpleName}: ${e.message}")
            }
        }
    } else {
        lock.withLock {
            if (debugMode) log("⏸️  Implementation missing for ${kClass.simpleName}. Queuing action with priority $priorityValue...")
            defaultInstance.enqueueActionUnderLock(
                kClass,
                QueuedAction(action = { instance -> block(instance as T) }, priority = priorityValue),
                evictByPriority = true
            )
        }
        KRelayMetrics.recordQueue(kClass)
    }
}

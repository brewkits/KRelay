package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * Internal implementation of [KRelayInstance].
 *
 * This class contains the core logic for registration management, action queueing,
 * and persistence orchestration. It is designed to be thread-safe across all 
 * supported KMP platforms using platform-specific [Lock] implementations.
 *
 * @property scopeName Unique name for this instance, used for logging and persistence.
 * @property maxQueueSize Limit on the number of actions per feature type.
 * @property actionExpiryMs Duration after which queued actions are discarded.
 * @property debugMode Enables detailed internal logging.
 */
@PublishedApi
internal class KRelayInstanceImpl(
    override val scopeName: String,
    override var maxQueueSize: Int = 100,
    override var actionExpiryMs: Long = 300_000,
    override var debugMode: Boolean = false
) : KRelayInstance {

    /**
     * Platform-agnostic reentrant lock.
     */
    @PublishedApi
    internal val lock = Lock()

    /**
     * Thread-safe registry holding [WeakRef]s to platform implementations.
     */
    @PublishedApi
    internal val registry = mutableMapOf<KClass<*>, WeakRef<Any>>()

    /**
     * The "Sticky Queue" holding actions awaiting registration.
     */
    @PublishedApi
    internal val pendingQueue = mutableMapOf<KClass<*>, MutableList<QueuedAction>>()

    /**
     * The current persistence engine. Defaults to an in-memory mock.
     */
    @PublishedApi
    internal var _persistenceAdapter: KRelayPersistenceAdapter = InMemoryPersistenceAdapter()

    /**
     * Map of feature-action keys to their reconstruction factories.
     */
    @PublishedApi
    internal val actionFactories = mutableMapOf<String, ActionFactory<*>>()

    /**
     * Reverse mapping from stable string keys to [KClass] objects.
     */
    @PublishedApi
    internal val featureKeyToKClass = mutableMapOf<String, KClass<*>>()

    /**
     * Registers an implementation and triggers immediate replay of queued actions.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : RelayFeature> register(kClass: KClass<T>, impl: T) {
        val actionsToReplay = lock.withLock {
            if (debugMode) {
                val existing = registry[kClass]?.get()
                if (existing != null && existing !== impl) {
                    // Only warn when the *type* of the new impl differs from the existing one.
                    // Same-class replacement (e.g. Activity recreated by Compose lifecycle) is
                    // expected and not a developer mistake — suppress to avoid log noise.
                    if (existing::class != impl::class) {
                        log("[WARN] Overwriting ${kClass.simpleName}: replacing ${existing::class.simpleName} with ${impl::class.simpleName}. " +
                            "If unintentional, check that only one component registers this feature at a time.")
                    }
                }
                log("[REG] Registering ${kClass.simpleName}")
            }

            registry[kClass] = WeakRef(impl as Any)

            val queue = pendingQueue[kClass]
            if (!queue.isNullOrEmpty()) {
                val validActions = queue.filter { !it.isExpired(actionExpiryMs) }
                val expiredCount = queue.size - validActions.size

                if (expiredCount > 0 && debugMode) {
                    log("[EXPIRY] Removed $expiredCount expired action(s) for ${kClass.simpleName}")
                }

                queue.clear()

                if (validActions.isNotEmpty()) {
                    if (debugMode) {
                        log("[REPLAY] Replaying ${validActions.size} pending action(s) for ${kClass.simpleName}")
                    }
                    KRelayMetrics.recordReplay(kClass, validActions.size)
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
                        logError("Error replaying action for ${kClass.simpleName}: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Core dispatch logic: executes immediately or queues.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : RelayFeature> dispatch(
        kClass: KClass<T>,
        block: (T) -> Unit,
        scopeToken: String?
    ) {
        val impl: T? = lock.withLock {
            val found = registry[kClass]?.get() as? T
            if (found == null) {
                if (debugMode) log("[QUEUE] Implementation missing for ${kClass.simpleName}. Queuing action...")
                enqueueActionUnderLock(
                    kClass,
                    QueuedAction(action = { instance -> block(instance as T) }, scopeToken = scopeToken)
                )
            } else {
                if (debugMode) log("[DISPATCH] Dispatching to ${kClass.simpleName}")
            }
            found
        }

        if (impl != null) {
            KRelayMetrics.recordDispatch(kClass)
            runOnMain {
                try {
                    block(impl)
                } catch (e: Exception) {
                    logError("Error executing action for ${kClass.simpleName}: ${e.message}")
                }
            }
        } else {
            KRelayMetrics.recordQueue(kClass)
        }
    }

    /**
     * Dispatches an action with a specific priority level.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : RelayFeature> dispatchWithPriority(
        kClass: KClass<T>,
        priorityValue: Int,
        block: (T) -> Unit
    ) {
        val impl: T? = lock.withLock {
            val found = registry[kClass]?.get() as? T
            if (found == null) {
                if (debugMode) log("[QUEUE] Implementation missing for ${kClass.simpleName}. Queuing with priority $priorityValue...")
                enqueueActionUnderLock(
                    kClass,
                    QueuedAction(action = { instance -> block(instance as T) }, priority = priorityValue),
                    evictByPriority = true
                )
            } else {
                if (debugMode) log("[DISPATCH] Dispatching to ${kClass.simpleName} with priority $priorityValue")
            }
            found
        }

        if (impl != null) {
            KRelayMetrics.recordDispatch(kClass)
            runOnMain {
                try {
                    block(impl)
                } catch (e: Exception) {
                    logError("Error executing action for ${kClass.simpleName}: ${e.message}")
                }
            }
        } else {
            KRelayMetrics.recordQueue(kClass)
        }
    }

    /**
     * Adds an action to the queue while enforcing limits and performing eviction.
     */
    @PublishedApi
    internal fun enqueueActionUnderLock(
        kClass: KClass<*>,
        action: QueuedAction,
        evictByPriority: Boolean = false
    ) {
        val queue = pendingQueue.getOrPut(kClass) { mutableListOf() }
        queue.removeAll { it.isExpired(actionExpiryMs) }

        if (queue.size >= maxQueueSize) {
            if (evictByPriority && queue.isNotEmpty()) {
                // Queue is sorted descending by priority, so the last element is the lowest priority — O(1)
                queue.removeAt(queue.lastIndex)
            } else if (queue.isNotEmpty()) {
                queue.removeAt(0)
            }
            
            if (debugMode) {
                log("[WARN] Queue full for ${kClass.simpleName}. Evicted ${if (evictByPriority) "lowest-priority" else "oldest"} action.")
            }
        }

        if (evictByPriority) {
            // Binary insertion to maintain descending priority order — O(log n) vs O(n log n) for full sort
            val insertIndex = queue.binarySearch { action.priority.compareTo(it.priority) }
                .let { if (it < 0) -(it + 1) else it }
            queue.add(insertIndex, action)
        } else {
            queue.add(action)
        }
    }

    /**
     * Unregisters an implementation with identity check.
     */
    override fun <T : RelayFeature> unregister(kClass: KClass<T>, impl: T?) {
        lock.withLock {
            if (impl == null || registry[kClass]?.get() === impl) {
                if (debugMode) {
                    log("[UNREG] Unregistering ${kClass.simpleName}")
                }
                registry[kClass]?.clear()
                registry.remove(kClass)
            }
        }
    }

    /**
     * Checks if a feature is currently registered and alive.
     */
    override fun <T : RelayFeature> isRegistered(kClass: KClass<T>): Boolean {
        return lock.withLock {
            val weakRef = registry[kClass]
            weakRef?.get() != null
        }
    }

    /**
     * Returns the current size of the pending queue for a feature.
     */
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

    /**
     * Clears the pending queue for a feature.
     */
    override fun <T : RelayFeature> clearQueue(kClass: KClass<T>) {
        lock.withLock {
            val count = pendingQueue[kClass]?.size ?: 0
            pendingQueue.remove(kClass)
            if (debugMode) {
                log("[CLEAR] Cleared queue for ${kClass.simpleName} ($count actions removed)")
            }
            KRelayMetrics.recordClear(kClass, count)
        }
    }

    override fun getRegisteredFeaturesCount(): Int {
        return lock.withLock {
            registry.count { it.value.get() != null }
        }
    }

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

    override fun getDebugInfo(): DebugInfo {
        return lock.withLock {
            val registeredFeatures = mutableListOf<String>()
            val featureQueues = mutableMapOf<String, Int>()
            var totalPending = 0
            var expiredCount = 0

            registry.forEach { (kClass, weakRef) ->
                if (weakRef.get() != null) {
                    registeredFeatures.add(kClass.simpleName ?: "Unknown")
                }
            }

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

    override fun cancelScope(token: String) {
        lock.withLock {
            var cancelled = 0
            pendingQueue.values.forEach { queue ->
                val before = queue.size
                queue.removeAll { it.scopeToken == token }
                cancelled += before - queue.size
            }
            if (debugMode) {
                log("[CANCEL] Cancelled $cancelled queued action(s) for scope token '$token'")
            }
        }
    }

    override fun resetConfiguration() {
        lock.withLock {
            maxQueueSize = 100
            actionExpiryMs = 300_000
            debugMode = false
        }
    }

    override fun reset() {
        // Capture adapter reference inside lock to avoid TOCTOU race with setPersistenceAdapter()
        val adapter = lock.withLock {
            if (debugMode) {
                log("[RESET] Resetting KRelay instance [$scopeName] - clearing all registrations and queues")
            }
            registry.clear()
            pendingQueue.clear()
            actionFactories.clear()
            featureKeyToKClass.clear()
            _persistenceAdapter
        }
        // I/O outside lock — uses the adapter reference captured atomically above
        adapter.clearScope(scopeName)
        KRelay.removeInstance(scopeName)
    }

    override fun setPersistenceAdapter(adapter: KRelayPersistenceAdapter) {
        lock.withLock {
            this._persistenceAdapter = adapter
        }
    }

    override fun <T : RelayFeature> registerActionFactory(
        kClass: KClass<T>,
        featureKey: String,
        actionKey: String,
        factory: ActionFactory<T>
    ) {
        lock.withLock {
            featureKeyToKClass[featureKey] = kClass
            @Suppress("UNCHECKED_CAST")
            actionFactories["$featureKey::$actionKey"] = factory as ActionFactory<*>
            if (debugMode) {
                log("[FACTORY] Registered factory for $featureKey::$actionKey")
            }
        }
    }

    /**
     * Atomic check-and-enqueue for persisted dispatch.
     *
     * Factory lookup, impl check, and enqueue all happen inside a single lock to avoid
     * the same TOCTOU race as [dispatch]: `register()` completing between check and enqueue
     * would otherwise leave the action stranded in the queue indefinitely.
     *
     * Persistence I/O (`save`) is intentionally performed *outside* the lock so that
     * disk latency does not block other threads that need the lock.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : RelayFeature> dispatchPersisted(
        kClass: KClass<T>,
        featureKey: String,
        actionKey: String,
        payload: String,
        priorityValue: Int
    ) {
        val factoryKey = "$featureKey::$actionKey"

        // Resolve factory and reconstruct block before acquiring the main lock.
        // factory() only produces a lambda — no I/O, safe to call outside lock.
        val factory = lock.withLock { actionFactories[factoryKey] } as? ActionFactory<T>
            ?: error(
                "No factory registered for '$factoryKey'. " +
                "Call instance.registerActionFactory<$featureKey>(\"$actionKey\") { payload -> { feature -> ... } } first."
            )
        val block = factory(payload)

        // Atomic check-and-enqueue: impl lookup + optional queue insertion in one lock
        var needsPersist = false
        val command = PersistedCommand(actionKey, payload, currentTimeMillis(), priorityValue)

        val impl: T? = lock.withLock {
            val found = registry[kClass]?.get() as? T
            if (found == null) {
                if (debugMode) log("[QUEUE] Queuing persisted action $featureKey::$actionKey (payload: $payload)")
                enqueueActionUnderLock(
                    kClass,
                    QueuedAction(
                        action = { instance -> block(instance as T) },
                        timestampMs = command.timestampMs,
                        priority = command.priority
                    ),
                    evictByPriority = true
                )
                needsPersist = true
            } else {
                if (debugMode) log("[DISPATCH] Persisted dispatch (immediate) $featureKey::$actionKey")
            }
            found
        }

        if (impl != null) {
            KRelayMetrics.recordDispatch(kClass)
            runOnMain {
                try {
                    block(impl)
                } catch (e: Exception) {
                    logError("Error in persisted dispatch for $featureKey::$actionKey — ${e.message}")
                }
            }
        } else {
            // I/O outside lock — disk latency does not affect the locked dispatch path
            if (needsPersist) {
                _persistenceAdapter.save(scopeName, featureKey, command)
                if (debugMode) log("[PERSIST] Persisted $featureKey::$actionKey to storage")
            }
            KRelayMetrics.recordQueue(kClass)
        }
    }

    /**
     * Restores persisted actions from storage into the in-memory queue.
     *
     * Design goals:
     * 1. **No I/O inside lock**: `loadAll` and `remove` calls are never made while holding
     *    the instance lock, so disk latency cannot block the main thread's dispatch path.
     * 2. **Single lock acquisition**: all in-memory mutations (factory lookups + enqueue)
     *    happen inside one `lock.withLock` block, eliminating repeated lock/unlock overhead
     *    and reducing TOCTOU windows to zero.
     * 3. **Remove-after-enqueue**: persistence entries are deleted only after they are
     *    successfully enqueued; if the process dies between enqueue and remove the command
     *    will be restored again on the next start (safe duplicate replay rather than loss).
     *
     * **Important**: call this method from a background thread (e.g. a coroutine on
     * `Dispatchers.IO`) — `loadAll` performs disk I/O and can block for several
     * milliseconds on a cold start.
     */
    override fun restorePersistedActions() {
        // Capture adapter reference inside lock to avoid TOCTOU race with setPersistenceAdapter()
        val adapter = lock.withLock { _persistenceAdapter }

        // Step 1: I/O outside lock — load everything from disk first
        val persistedMap = adapter.loadAll(scopeName)
        if (persistedMap.isEmpty()) {
            if (debugMode) log("[RESTORE] No persisted actions to restore for scope '$scopeName'")
            return
        }

        val totalCount = persistedMap.values.sumOf { it.size }
        if (debugMode) log("[RESTORE] Restoring $totalCount persisted action(s) for scope '$scopeName'")

        // Step 2: Collect the I/O outcome lists so we can remove from disk after unlocking
        data class EnqueuedEntry(val featureKey: String, val command: PersistedCommand)

        val toRemove = mutableListOf<EnqueuedEntry>()
        var restoredCount = 0
        var skippedExpired = 0
        var skippedNoFactory = 0

        // Step 3: Single lock acquisition — all in-memory work happens here
        lock.withLock {
            persistedMap.forEach featureLoop@{ (featureKey, commands) ->
                val kClass = featureKeyToKClass[featureKey]
                if (kClass == null) {
                    if (debugMode) log("[WARN] No KClass for '$featureKey'. Register factory before restorePersistedActions().")
                    skippedNoFactory += commands.size
                    commands.forEach { toRemove.add(EnqueuedEntry(featureKey, it)) }
                    return@featureLoop
                }

                commands.forEach commandLoop@{ command ->
                    if (command.isExpired(actionExpiryMs)) {
                        skippedExpired++
                        toRemove.add(EnqueuedEntry(featureKey, command))
                        return@commandLoop
                    }

                    val factoryKey = "$featureKey::${command.actionKey}"
                    @Suppress("UNCHECKED_CAST")
                    val factory = actionFactories[factoryKey] as? ActionFactory<Any>
                    if (factory == null) {
                        if (debugMode) log("[WARN] No factory for '$factoryKey'. Skipping restored action.")
                        skippedNoFactory++
                        toRemove.add(EnqueuedEntry(featureKey, command))
                        return@commandLoop
                    }

                    // Reconstruct action and enqueue — all in-memory, no I/O
                    val block = factory(command.payload)
                    val queue = pendingQueue.getOrPut(kClass) { mutableListOf() }
                    val queuedAction = QueuedAction({ instance -> block(instance) }, command.timestampMs, command.priority)
                    // Binary insertion to maintain descending priority order
                    val insertIndex = queue.binarySearch { queuedAction.priority.compareTo(it.priority) }
                        .let { if (it < 0) -(it + 1) else it }
                    queue.add(insertIndex, queuedAction)

                    toRemove.add(EnqueuedEntry(featureKey, command))
                    restoredCount++
                }
            }
        }

        // Step 4: I/O outside lock — delete entries from disk now that they are safely in memory
        toRemove.forEach { (featureKey, command) ->
            adapter.remove(scopeName, featureKey, command)
        }

        if (debugMode) {
            log("[RESTORE] Restored $restoredCount action(s). Skipped: $skippedExpired expired, $skippedNoFactory no-factory.")
        }
    }

    /**
     * Internal logging function for debug messages.
     * Call sites MUST guard with `if (debugMode)` before calling.
     */
    @PublishedApi
    internal fun log(message: String) {
        println("[KRelay][$scopeName] $message")
    }

    /**
     * Internal logging function for error messages.
     * Always prints regardless of [debugMode] — errors must never be silently swallowed.
     */
    @PublishedApi
    internal fun logError(message: String) {
        println("[KRelay][$scopeName][ERROR] $message")
    }
}

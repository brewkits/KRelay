package dev.brewkits.krelay

import kotlin.reflect.KClass

// ============================================================
// PERSISTED DISPATCH — Extension API for KRelayInstance
// ============================================================

/**
 * Sets a [KRelayPersistenceAdapter] on this instance, enabling [dispatchPersisted]
 * to survive process death.
 *
 * Call this early in your app lifecycle, before dispatching any persisted actions:
 * ```kotlin
 * // Android: Application.onCreate()
 * relayInstance.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(this))
 *
 * // iOS: App init
 * relayInstance.setPersistenceAdapter(NSUserDefaultsPersistenceAdapter())
 * ```
 */
fun KRelayInstance.setPersistenceAdapter(adapter: KRelayPersistenceAdapter) {
    if (this is KRelayInstanceImpl) {
        this.persistenceAdapter = adapter
    } else {
        throw UnsupportedOperationException(
            "Custom KRelayInstance implementations must handle setPersistenceAdapter()"
        )
    }
}

/**
 * Registers a factory to reconstruct a named action from its persisted [payload].
 *
 * **Must be called before [restorePersistedActions].**
 *
 * Example:
 * ```kotlin
 * instance.registerActionFactory<ToastFeature>("show_toast") { payload ->
 *     { feature -> feature.show(payload) }
 * }
 *
 * instance.registerActionFactory<NavigationFeature>("go_home") { _ ->
 *     { feature -> feature.navigateTo("home") }
 * }
 * ```
 *
 * @param actionKey The key used in [dispatchPersisted]. Should be a simple identifier.
 * @param factory A function that takes the payload string and returns the action lambda.
 */
inline fun <reified T : RelayFeature> KRelayInstance.registerActionFactory(
    actionKey: String,
    noinline factory: ActionFactory<T>
) {
    if (this is KRelayInstanceImpl) {
        this.registerActionFactoryInternal(T::class, actionKey, factory)
    } else {
        throw UnsupportedOperationException(
            "Custom KRelayInstance implementations must handle registerActionFactory()"
        )
    }
}

/**
 * Dispatches a named, persistable action that can survive process death.
 *
 * Unlike [dispatch] (which captures a lambda), this method stores an [actionKey] +
 * [payload] that can be serialized to disk. A registered [ActionFactory] reconstructs
 * the action lambda on restoration.
 *
 * ## Lifecycle
 * 1. Register factory: `instance.registerActionFactory<T>("key") { payload -> { feature -> ... } }`
 * 2. Dispatch: `instance.dispatchPersisted<T>("key", "my payload")`
 * 3. If no impl: queued in memory AND persisted to disk
 * 4. On process death: queue lost, but disk entry survives
 * 5. On restart: call `instance.restorePersistedActions()` to restore to queue
 * 6. Register impl: queued actions replayed as normal
 *
 * ## Example
 * ```kotlin
 * // ViewModel init
 * relayInstance.registerActionFactory<ToastFeature>("welcome") { payload ->
 *     { feature -> feature.show(payload) }
 * }
 *
 * // On some event (safe across process death)
 * relayInstance.dispatchPersisted<ToastFeature>("welcome", "Hello, $userName!")
 * ```
 *
 * @param actionKey Identifier for this action type. Must match a registered factory key.
 * @param payload Serializable string data passed to the factory. Default: empty string.
 * @param priority Queue priority if this action is queued. Default: [ActionPriority.NORMAL].
 * @throws IllegalStateException if no factory is registered for [actionKey].
 */
inline fun <reified T : RelayFeature> KRelayInstance.dispatchPersisted(
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    if (this is KRelayInstanceImpl) {
        this.dispatchPersistedInternal(T::class, actionKey, payload, priority)
    } else {
        throw UnsupportedOperationException(
            "Custom KRelayInstance implementations must handle dispatchPersisted()"
        )
    }
}

/**
 * Restores persisted actions from storage back into the in-memory queue.
 *
 * Call this **on every app start**, after registering action factories but before
 * registering feature implementations:
 *
 * ```kotlin
 * // Startup order:
 * // 1. Set persistence adapter (once)
 * instance.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(context))
 *
 * // 2. Register factories (before restore)
 * instance.registerActionFactory<ToastFeature>("show_toast") { payload ->
 *     { feature -> feature.show(payload) }
 * }
 *
 * // 3. Restore persisted actions → adds back to in-memory queue
 * instance.restorePersistedActions()
 *
 * // 4. Register implementations (triggers replay of queued actions)
 * instance.register<ToastFeature>(this)
 * ```
 *
 * **Note:** Commands are cleared from storage immediately upon restoration.
 * If the app dies again before replay, those commands are lost.
 */
fun KRelayInstance.restorePersistedActions() {
    if (this is KRelayInstanceImpl) {
        this.restorePersistedActionsInternal()
    } else {
        throw UnsupportedOperationException(
            "Custom KRelayInstance implementations must handle restorePersistedActions()"
        )
    }
}

// ============================================================
// SINGLETON WRAPPERS
// ============================================================

/**
 * Sets a [KRelayPersistenceAdapter] on the default singleton instance.
 */
fun KRelay.setPersistenceAdapter(adapter: KRelayPersistenceAdapter) {
    defaultInstance.setPersistenceAdapter(adapter)
}

/**
 * Registers an action factory on the default singleton instance.
 * See [KRelayInstance.registerActionFactory] for full documentation.
 */
inline fun <reified T : RelayFeature> KRelay.registerActionFactory(
    actionKey: String,
    noinline factory: ActionFactory<T>
) {
    defaultInstance.registerActionFactory(actionKey, factory)
}

/**
 * Dispatches a persisted action on the default singleton instance.
 * See [KRelayInstance.dispatchPersisted] for full documentation.
 */
inline fun <reified T : RelayFeature> KRelay.dispatchPersisted(
    actionKey: String,
    payload: String = "",
    priority: ActionPriority = ActionPriority.DEFAULT
) {
    defaultInstance.dispatchPersisted<T>(actionKey, payload, priority)
}

/**
 * Restores persisted actions on the default singleton instance.
 * See [KRelayInstance.restorePersistedActions] for full documentation.
 */
fun KRelay.restorePersistedActions() {
    defaultInstance.restorePersistedActions()
}

// ============================================================
// INTERNAL IMPLEMENTATION METHODS (added to KRelayInstanceImpl)
// ============================================================

/**
 * Internal: Register action factory with class-to-key mapping.
 */
@PublishedApi
internal fun <T : RelayFeature> KRelayInstanceImpl.registerActionFactoryInternal(
    kClass: KClass<T>,
    actionKey: String,
    factory: ActionFactory<T>
) {
    val featureName = kClass.simpleName ?: return  // extract before withLock (return not allowed inside non-inline lambda)
    lock.withLock {
        featureKeyToKClass[featureName] = kClass
        @Suppress("UNCHECKED_CAST")
        actionFactories["$featureName::$actionKey"] = factory as ActionFactory<*>
        if (debugMode) {
            log("🏭 Registered factory for $featureName::$actionKey")
        }
    }
}

/**
 * Internal: Dispatch a named persisted action.
 */
@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : RelayFeature> KRelayInstanceImpl.dispatchPersistedInternal(
    kClass: KClass<T>,
    actionKey: String,
    payload: String,
    priority: ActionPriority
) {
    val featureName = kClass.simpleName ?: "Unknown"
    val factoryKey = "$featureName::$actionKey"

    val factory = lock.withLock { actionFactories[factoryKey] } as? ActionFactory<T>
        ?: error(
            "No factory registered for '$factoryKey'. " +
            "Call instance.registerActionFactory<$featureName>(\"$actionKey\") { payload -> { feature -> ... } } first."
        )

    val block = factory(payload)

    val impl = lock.withLock { registry[kClass]?.get() as? T }

    if (impl != null) {
        // Execute immediately — no need to persist
        if (debugMode) log("✅ Persisted dispatch (immediate) $featureName::$actionKey")
        KRelayMetrics.recordDispatch(kClass)
        runOnMain {
            try {
                block(impl)
            } catch (e: Exception) {
                log("❌ Error in persisted dispatch for $featureName::$actionKey — ${e.message}")
            }
        }
    } else {
        // Queue in memory + persist to disk
        val command = PersistedCommand(actionKey, payload, currentTimeMillis(), priority.value)

        lock.withLock {
            if (debugMode) log("⏸️  Queuing persisted action $featureName::$actionKey (payload: $payload)")
            enqueueActionUnderLock(
                kClass,
                QueuedAction(
                    action = { instance -> block(instance as T) },
                    timestampMs = command.timestampMs,
                    priority = command.priority
                ),
                evictByPriority = true
            )
        }
        KRelayMetrics.recordQueue(kClass)

        // Persist for process-death survival
        persistenceAdapter.save(scopeName, featureName, command)
        if (debugMode) log("💾 Persisted $featureName::$actionKey to storage")
    }
}

/**
 * Internal: Restore persisted actions from storage into in-memory queue.
 */
internal fun KRelayInstanceImpl.restorePersistedActionsInternal() {
    val persistedMap = persistenceAdapter.loadAll(scopeName)
    if (persistedMap.isEmpty()) {
        if (debugMode) log("📂 No persisted actions to restore for scope '$scopeName'")
        return
    }

    val totalCount = persistedMap.values.sumOf { it.size }
    if (debugMode) log("📂 Restoring $totalCount persisted action(s) for scope '$scopeName'")

    var restoredCount = 0
    var skippedExpired = 0
    var skippedNoFactory = 0

    persistedMap.forEach { (featureKey, commands) ->
        val kClass = lock.withLock { featureKeyToKClass[featureKey] }
        if (kClass == null) {
            if (debugMode) log("⚠️  No KClass for feature '$featureKey'. Register factory before restorePersistedActions().")
            skippedNoFactory += commands.size
            commands.forEach { persistenceAdapter.remove(scopeName, featureKey, it) }
            return@forEach
        }

        commands.forEach { command ->
            // Always remove from persistence (now in-memory)
            persistenceAdapter.remove(scopeName, featureKey, command)

            if (command.isExpired(actionExpiryMs)) {
                skippedExpired++
                return@forEach
            }

            val factoryKey = "$featureKey::${command.actionKey}"
            val factory = lock.withLock { actionFactories[factoryKey] }
            if (factory == null) {
                if (debugMode) log("⚠️  No factory for '$factoryKey'. Skipping restored action.")
                skippedNoFactory++
                return@forEach
            }

            // Reconstruct action and add to in-memory queue
            lock.withLock {
                @Suppress("UNCHECKED_CAST")
                val typedFactory = factory as ActionFactory<Any>
                val block = typedFactory(command.payload)
                val actionWrapper: (Any) -> Unit = { instance -> block(instance) }

                val queue = pendingQueue.getOrPut(kClass) { mutableListOf() }
                queue.add(QueuedAction(actionWrapper, command.timestampMs, command.priority))
                queue.sortByDescending { it.priority }
            }
            restoredCount++
        }
    }

    if (debugMode) {
        log("✅ Restored $restoredCount action(s). Skipped: $skippedExpired expired, $skippedNoFactory no-factory.")
    }
}

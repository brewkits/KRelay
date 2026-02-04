package dev.brewkits.krelay

/**
 * Builder for creating configured KRelay instances.
 *
 * Example:
 * ```kotlin
 * val instance = KRelay.builder("RideModule")
 *     .maxQueueSize(50)
 *     .actionExpiry(60_000L)
 *     .debugMode(true)
 *     .build()
 * ```
 */
class KRelayBuilder internal constructor(
    private val scopeName: String,
    private val instanceRegistry: MutableSet<String>,
    private val instanceRegistryLock: Lock
) {
    init {
        // Validate scope name (v2.0.1)
        require(scopeName.isNotBlank()) { "scopeName must not be blank" }
    }

    private var maxQueueSize: Int = 100
    private var actionExpiryMs: Long = 5 * 60 * 1000
    private var debugMode: Boolean = false

    /**
     * Sets maximum queue size per feature type.
     * Default: 100
     *
     * @param size Maximum number of pending actions per feature (must be > 0)
     * @return This builder for chaining
     * @throws IllegalArgumentException if size <= 0
     */
    fun maxQueueSize(size: Int): KRelayBuilder {
        require(size > 0) { "maxQueueSize must be greater than 0, got: $size" }
        this.maxQueueSize = size
        return this
    }

    /**
     * Sets action expiry time in milliseconds.
     * Default: 5 minutes (300,000ms)
     *
     * @param ms Expiry time in milliseconds (must be > 0)
     * @return This builder for chaining
     * @throws IllegalArgumentException if ms <= 0
     */
    fun actionExpiry(ms: Long): KRelayBuilder {
        require(ms > 0) { "actionExpiryMs must be greater than 0, got: $ms" }
        this.actionExpiryMs = ms
        return this
    }

    /**
     * Enables or disables debug mode.
     * Default: false
     *
     * When enabled, KRelay will print detailed logs about:
     * - Registration/unregistration events
     * - Dispatch operations
     * - Queue operations
     * - Expired actions
     *
     * @param enabled true to enable debug mode, false to disable
     * @return This builder for chaining
     */
    fun debugMode(enabled: Boolean): KRelayBuilder {
        this.debugMode = enabled
        return this
    }

    /**
     * Builds the KRelay instance with configured settings.
     *
     * **Duplicate Scope Name Warning** (v2.0.1):
     * If `debugMode` is enabled and an instance with the same scope name already exists,
     * a warning will be logged.
     *
     * @return Configured KRelayInstance
     */
    fun build(): KRelayInstance {
        // Check for duplicate scope name (v2.0.1)
        instanceRegistryLock.withLock {
            if (debugMode && scopeName in instanceRegistry) {
                println("⚠️ [KRelay] Instance with scope '$scopeName' already exists. " +
                    "Consider using unique names to avoid confusion in debug logs.")
            }
            instanceRegistry.add(scopeName)
        }

        return KRelayInstanceImpl(
            scopeName = scopeName,
            maxQueueSize = maxQueueSize,
            actionExpiryMs = actionExpiryMs,
            debugMode = debugMode
        )
    }
}

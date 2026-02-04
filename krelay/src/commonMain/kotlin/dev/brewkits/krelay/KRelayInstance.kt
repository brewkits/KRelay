package dev.brewkits.krelay

/**
 * KRelay instance for modularized apps (Super Apps, multi-team projects).
 *
 * ## When to Use Instances vs Singleton
 *
 * **Use Singleton (KRelay object)**:
 * - Single-module apps
 * - Small-medium projects
 * - Zero-config simplicity
 *
 * **Use Instances (KRelayInstance)**:
 * - Super Apps (Grab/Gojek style) with independent modules
 * - Multi-team projects requiring isolation
 * - DI-based architecture (inject KRelayInstance)
 * - Per-module configuration (different queue sizes, expiry times)
 *
 * ## Example: Super App with Multiple Modules
 *
 * ```kotlin
 * // Ride Module
 * val rideKRelay = KRelay.create("RideModule")
 * rideKRelay.register<ToastFeature>(RideToastImpl())
 *
 * // Food Module (independent)
 * val foodKRelay = KRelay.create("FoodModule")
 * foodKRelay.register<ToastFeature>(FoodToastImpl())
 *
 * // No conflicts! Each module has isolated registry
 * ```
 *
 * ## Example: Dependency Injection
 *
 * ```kotlin
 * // Koin module
 * val rideModule = module {
 *     single { KRelay.create("RideModule") }
 *     viewModel { RideViewModel(kRelay = get()) }
 * }
 *
 * // ViewModel
 * class RideViewModel(private val kRelay: KRelayInstance) : ViewModel() {
 *     fun bookRide() {
 *         kRelay.dispatch<ToastFeature> { it.show("Booking...") }
 *     }
 * }
 * ```
 */
@ProcessDeathUnsafe
interface KRelayInstance {
    /**
     * Unique name for this instance (used for debugging).
     */
    val scopeName: String

    /**
     * Configuration: Maximum queue size per feature type.
     * Default: 100
     */
    var maxQueueSize: Int

    /**
     * Configuration: Action expiry time in milliseconds.
     * Default: 5 minutes (300,000ms)
     */
    var actionExpiryMs: Long

    /**
     * Configuration: Debug mode for this instance.
     * Default: false
     */
    var debugMode: Boolean

    // Note: Type-safe methods (register, dispatch, unregister, isRegistered, getPendingCount, clearQueue)
    // are provided as extension functions in KRelay.kt because they require reified type parameters

    /**
     * Gets registered features count.
     *
     * @return Number of registered features
     */
    fun getRegisteredFeaturesCount(): Int

    /**
     * Gets total pending actions across all features.
     *
     * @return Total pending count
     */
    fun getTotalPendingCount(): Int

    /**
     * Gets debug information about this instance.
     *
     * @return DebugInfo containing instance state
     */
    fun getDebugInfo(): DebugInfo

    /**
     * Dumps debug information to console.
     */
    fun dump()

    /**
     * Resets this instance (clears all registrations and queues).
     */
    fun reset()
}

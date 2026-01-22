package dev.brewkits.krelay

/**
 * Marker interface for all KRelay features.
 *
 * Any service that needs to be called from shared Kotlin code
 * and implemented in platform-specific UI layer should extend this interface.
 *
 * Example:
 * ```kotlin
 * interface ToastFeature : RelayFeature {
 *     fun show(message: String)
 * }
 * ```
 */
interface RelayFeature

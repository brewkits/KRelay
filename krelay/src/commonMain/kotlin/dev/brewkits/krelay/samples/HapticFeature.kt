package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Haptic feedback feature for triggering device vibrations and haptic effects.
 *
 * Use Case: Provide tactile feedback for user interactions from shared code.
 *
 * Example:
 * ```kotlin
 * // Light impact for button press
 * KRelay.dispatch<HapticFeature> {
 *     it.impact(HapticStyle.LIGHT)
 * }
 *
 * // Success feedback
 * KRelay.dispatch<HapticFeature> {
 *     it.success()
 * }
 * ```
 *
 * Platform Implementation:
 * - Android: Uses Vibrator/VibrationEffect
 * - iOS: Uses UIImpactFeedbackGenerator/UINotificationFeedbackGenerator
 */
interface HapticFeature : RelayFeature {

    /**
     * Trigger a simple vibration.
     * @param durationMs Vibration duration in milliseconds
     */
    fun vibrate(durationMs: Long = 100)

    /**
     * Trigger an impact haptic feedback.
     * @param style Haptic style (LIGHT, MEDIUM, HEAVY)
     */
    fun impact(style: HapticStyle = HapticStyle.MEDIUM)

    /**
     * Trigger a selection haptic (for UI selections like picker).
     */
    fun selection()

    /**
     * Trigger a success haptic notification.
     */
    fun success()

    /**
     * Trigger a warning haptic notification.
     */
    fun warning()

    /**
     * Trigger an error haptic notification.
     */
    fun error()
}

/**
 * Haptic feedback styles.
 */
enum class HapticStyle {
    /**
     * Light impact - for small UI interactions.
     */
    LIGHT,

    /**
     * Medium impact - default for most interactions.
     */
    MEDIUM,

    /**
     * Heavy impact - for significant actions.
     */
    HEAVY
}

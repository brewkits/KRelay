package dev.brewkits.krelay.integrations

import dev.brewkits.krelay.samples.HapticFeature
import dev.brewkits.krelay.samples.HapticStyle
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Real iOS Haptic implementation using UIFeedbackGenerator.
 *
 * NOTE: Haptic feedback doesn't work on iOS Simulator.
 * This implementation safely handles simulator environment.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSHapticImpl : HapticFeature {

    private val isSimulator: Boolean by lazy {
        // Check if running on simulator
        platform.Foundation.NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
    }

    override fun vibrate(durationMs: Long) {
        if (isSimulator) {
            println("📳 [IOSHapticImpl] Vibrate ${durationMs}ms (simulator - no haptic)")
            return
        }
        // iOS doesn't support custom duration vibration
        // Use impact feedback as alternative
        impact(HapticStyle.MEDIUM)
    }

    override fun impact(style: HapticStyle) {
        if (isSimulator) {
            println("📳 [IOSHapticImpl] Impact: $style (simulator - no haptic)")
            return
        }

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val generator = when (style) {
                    HapticStyle.LIGHT -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
                    HapticStyle.MEDIUM -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
                    HapticStyle.HEAVY -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                }
                generator.prepare()
                generator.impactOccurred()
            } catch (e: Exception) {
                println("⚠️ [IOSHapticImpl] Impact error: ${e.message}")
            }
        }
    }

    override fun success() {
        if (isSimulator) {
            println("📳 [IOSHapticImpl] Success feedback (simulator - no haptic)")
            return
        }

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            } catch (e: Exception) {
                println("⚠️ [IOSHapticImpl] Success error: ${e.message}")
            }
        }
    }

    override fun error() {
        if (isSimulator) {
            println("📳 [IOSHapticImpl] Error feedback (simulator - no haptic)")
            return
        }

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
            } catch (e: Exception) {
                println("⚠️ [IOSHapticImpl] Error error: ${e.message}")
            }
        }
    }

    override fun warning() {
        if (isSimulator) {
            println("📳 [IOSHapticImpl] Warning feedback (simulator - no haptic)")
            return
        }

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            } catch (e: Exception) {
                println("⚠️ [IOSHapticImpl] Warning error: ${e.message}")
            }
        }
    }

    override fun selection() {
        if (isSimulator) {
            println("📳 [IOSHapticImpl] Selection feedback (simulator - no haptic)")
            return
        }

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val generator = UISelectionFeedbackGenerator()
                generator.prepare()
                generator.selectionChanged()
            } catch (e: Exception) {
                println("⚠️ [IOSHapticImpl] Selection error: ${e.message}")
            }
        }
    }
}

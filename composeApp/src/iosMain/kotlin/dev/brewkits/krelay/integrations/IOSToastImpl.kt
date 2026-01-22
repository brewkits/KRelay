package dev.brewkits.krelay.integrations

import dev.brewkits.krelay.samples.ToastFeature
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTimer
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Real iOS Toast implementation.
 *
 * On iOS Simulator: Just logs to console (UIAlertController can be flaky)
 * On Real Device: Shows UIAlertController that auto-dismisses
 */
@OptIn(ExperimentalForeignApi::class)
class IOSToastImpl : ToastFeature {

    private val isSimulator: Boolean by lazy {
        platform.Foundation.NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
    }

    override fun showShort(message: String) {
        if (isSimulator) {
            println("🍞 [Toast] SHORT: $message")
        } else {
            showToast(message, duration = 2.0)
        }
    }

    override fun showLong(message: String) {
        if (isSimulator) {
            println("🍞 [Toast] LONG: $message")
        } else {
            showToast(message, duration = 3.5)
        }
    }

    private fun showToast(message: String, duration: Double) {
        // Only used on real device
        dispatch_async(dispatch_get_main_queue()) {
            try {
                @Suppress("DEPRECATION")
                val keyWindow = UIApplication.sharedApplication.keyWindow
                val rootViewController = keyWindow?.rootViewController

                if (rootViewController == null) {
                    println("🍞 [Toast] $message (no view controller)")
                    return@dispatch_async
                }

                var topController = rootViewController
                while (topController?.presentedViewController != null) {
                    topController = topController.presentedViewController
                }

                if (topController == null) {
                    println("🍞 [Toast] $message (no top controller)")
                    return@dispatch_async
                }

                val alert = UIAlertController.alertControllerWithTitle(
                    title = null,
                    message = message,
                    preferredStyle = UIAlertControllerStyleAlert
                )

                topController.presentViewController(
                    viewControllerToPresent = alert,
                    animated = true,
                    completion = {
                        NSTimer.scheduledTimerWithTimeInterval(
                            interval = duration,
                            repeats = false,
                            block = { timer: NSTimer? ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    alert.dismissViewControllerAnimated(true, completion = null)
                                }
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                println("🍞 [Toast] $message (error: ${e.message})")
            }
        }
    }
}

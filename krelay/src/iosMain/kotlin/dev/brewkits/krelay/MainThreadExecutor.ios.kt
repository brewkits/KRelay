package dev.brewkits.krelay

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.Foundation.NSThread

/**
 * iOS implementation of main thread executor using GCD (Grand Central Dispatch).
 *
 * This ensures that UI operations are always executed on the iOS Main queue,
 * preventing UI-related crashes from background threads.
 */
actual fun runOnMain(block: () -> Unit) {
    // NSThread.isMainThread is the correct and reliable way to check if the current
    // execution context is on the iOS main thread. It returns true whenever the current
    // thread is the main thread, regardless of which GCD queue dispatched the work.
    // This covers all standard use cases: UIKit callbacks, GCD main queue blocks, etc.
    if (NSThread.isMainThread) {
        // Already on main thread — execute synchronously to avoid unnecessary async overhead
        block()
    } else {
        // Off main thread — dispatch asynchronously to the main queue
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }
}

actual fun isMainThread(): Boolean {
    return NSThread.isMainThread
}

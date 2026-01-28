package dev.brewkits.krelay

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync
import platform.Foundation.NSThread

/**
 * iOS implementation of main thread executor using GCD (Grand Central Dispatch).
 *
 * This ensures that UI operations are always executed on the iOS Main queue,
 * preventing UI-related crashes from background threads.
 */
actual fun runOnMain(block: () -> Unit) {
    // Note: NSThread.isMainThread is 99% accurate for typical use cases.
    // In rare GCD edge cases, dispatch_async may be called unnecessarily.
    // For v1.1.0, this tradeoff is acceptable for performance.
    // Future: Consider dispatch_queue_get_label check in v1.2.0.
    if (NSThread.isMainThread) {
        // Already on main thread, execute immediately
        block()
    } else {
        // Dispatch to main queue asynchronously
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }
}

actual fun isMainThread(): Boolean {
    return NSThread.isMainThread
}

package dev.brewkits.krelay

/**
 * Platform-agnostic main thread executor.
 *
 * Ensures that the given block is executed on the platform's main/UI thread.
 *
 * Platform implementations:
 * - Android: Uses Handler(Looper.getMainLooper()).post {}
 * - iOS: Uses dispatch_async(dispatch_get_main_queue()) {}
 *
 * @param block The code to execute on the main thread
 */
expect fun runOnMain(block: () -> Unit)

/**
 * Checks if the current thread is the main/UI thread.
 *
 * Platform implementations:
 * - Android: Checks Looper.getMainLooper() == Looper.myLooper()
 * - iOS: Checks if on main dispatch queue
 *
 * @return true if current thread is main thread, false otherwise
 */
expect fun isMainThread(): Boolean

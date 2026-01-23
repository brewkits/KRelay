package dev.brewkits.krelay

import android.os.Handler
import android.os.Looper

/**
 * Android implementation of main thread executor using Handler and Looper.
 *
 * This ensures that UI operations are always executed on the Android Main/UI thread,
 * preventing "CalledFromWrongThreadException" crashes.
 *
 * For unit tests (JVM environment), falls back to synchronous execution.
 */

private val mainHandler by lazy {
    try {
        val looper = Looper.getMainLooper()
        if (looper != null) {
            Handler(looper)
        } else {
            null // Unit test environment - no main looper
        }
    } catch (e: RuntimeException) {
        // Unit test environment - Looper.getMainLooper() not mocked
        null
    }
}

actual fun runOnMain(block: () -> Unit) {
    try {
        val looper = Looper.getMainLooper()
        if (looper == null) {
            // Unit test environment - execute synchronously
            block()
        } else if (Looper.myLooper() == looper) {
            // Already on main thread, execute immediately
            block()
        } else {
            // Post to main thread
            mainHandler?.post(block) ?: block()
        }
    } catch (e: RuntimeException) {
        // Unit test environment - Looper not available, execute synchronously
        block()
    }
}

actual fun isMainThread(): Boolean {
    return try {
        Looper.myLooper() == Looper.getMainLooper()
    } catch (e: RuntimeException) {
        // Unit test environment - assume we're on main thread
        true
    }
}

package dev.brewkits.krelay

import android.os.Handler
import android.os.Looper

/**
 * Android implementation of main thread executor using Handler and Looper.
 *
 * This ensures that UI operations are always executed on the Android Main/UI thread,
 * preventing "CalledFromWrongThreadException" crashes.
 */

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

actual fun runOnMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        // Already on main thread, execute immediately
        block()
    } else {
        // Post to main thread
        mainHandler.post(block)
    }
}

actual fun isMainThread(): Boolean {
    return Looper.myLooper() == Looper.getMainLooper()
}

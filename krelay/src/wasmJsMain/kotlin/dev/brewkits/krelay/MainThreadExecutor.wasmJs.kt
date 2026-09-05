package dev.brewkits.krelay

/**
 * WasmJS runs in a single-threaded JavaScript environment.
 * Everything is the main thread.
 */
actual fun isMainThread(): Boolean = true

actual fun runOnMain(block: () -> Unit) {
    block()
}

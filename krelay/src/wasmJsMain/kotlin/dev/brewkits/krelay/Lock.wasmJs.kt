package dev.brewkits.krelay

/**
 * WasmJS is single-threaded, so locking is a no-op.
 */
actual class Lock actual constructor() {
    actual inline fun <T> withLock(block: () -> T): T {
        return block()
    }
}

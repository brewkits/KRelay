package dev.brewkits.krelay

import platform.Foundation.NSRecursiveLock

/**
 * iOS implementation of Lock using NSRecursiveLock.
 *
 * Why NSRecursiveLock over pthread_mutex?
 * 1. **Memory Safety**: ARC handles cleanup automatically (no manual free() needed)
 * 2. **Reentrant Support**: Same thread can acquire lock multiple times without deadlock
 * 3. **Simplicity**: Pure object-oriented API vs C-style pthread
 * 4. **Performance**: Sufficient for UI-thread synchronization tasks
 *
 * This is critical for future instance-based KRelay (v2.0) where multiple
 * RelayHub instances may be created and destroyed during app lifecycle.
 */
actual class Lock {
    private val lock = NSRecursiveLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}

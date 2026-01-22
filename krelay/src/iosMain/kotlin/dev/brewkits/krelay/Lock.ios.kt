package dev.brewkits.krelay

import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS implementation of Lock using pthread_mutex_t.
 *
 * pthread_mutex provides:
 * - POSIX-compliant mutex
 * - Low-level thread synchronization
 * - Efficient native implementation
 */
@OptIn(ExperimentalForeignApi::class)
actual class Lock {
    private val mutex: pthread_mutex_t = nativeHeap.alloc()

    init {
        pthread_mutex_init(mutex.ptr, null)
    }

    actual fun <T> withLock(block: () -> T): T {
        pthread_mutex_lock(mutex.ptr)
        try {
            return block()
        } finally {
            pthread_mutex_unlock(mutex.ptr)
        }
    }

    /**
     * Cleanup mutex resources.
     * Note: In Kotlin/Native, this will be called by the GC.
     */
    @Suppress("unused")
    fun destroy() {
        pthread_mutex_destroy(mutex.ptr)
        nativeHeap.free(mutex)
    }
}

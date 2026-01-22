package dev.brewkits.krelay

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock as kotlinWithLock

/**
 * Android implementation of Lock using ReentrantLock.
 *
 * ReentrantLock provides:
 * - Mutual exclusion (only one thread can hold the lock)
 * - Reentrant behavior (same thread can acquire multiple times)
 * - Fair ordering option (FIFO for waiting threads)
 */
actual class Lock {
    private val lock = ReentrantLock()

    actual fun <T> withLock(block: () -> T): T {
        return lock.kotlinWithLock(block)
    }
}

package dev.brewkits.krelay

/**
 * Platform-agnostic lock for thread-safe operations.
 *
 * This provides a consistent locking API across all platforms:
 * - Android/JVM: Uses ReentrantLock
 * - iOS/Native: Uses platform.posix.pthread_mutex_t
 */
expect class Lock() {
    /**
     * Executes the given block while holding the lock.
     */
    fun <T> withLock(block: () -> T): T
}

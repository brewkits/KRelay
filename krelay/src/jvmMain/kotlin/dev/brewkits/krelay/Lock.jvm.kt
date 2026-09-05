package dev.brewkits.krelay

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

actual class Lock actual constructor() {
    @PublishedApi
    internal val lock = ReentrantLock()

    actual inline fun <T> withLock(block: () -> T): T {
        return lock.withLock(block)
    }
}

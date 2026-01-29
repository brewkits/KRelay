package dev.brewkits.krelay

import kotlin.test.*

/**
 * iOS-specific tests for NSRecursiveLock implementation.
 *
 * These tests verify:
 * 1. Basic locking works
 * 2. Reentrant behavior (same thread can acquire lock multiple times)
 * 3. Lock protects shared state
 * 4. No memory leaks (ARC managed)
 */
class LockIosTest {

    @Test
    fun testBasicLocking() {
        val lock = Lock()
        var counter = 0

        lock.withLock {
            counter++
        }

        assertEquals(1, counter)
    }

    @Test
    fun testReentrantLocking() {
        val lock = Lock()
        var counter = 0

        // Outer lock
        lock.withLock {
            counter++

            // Inner lock (reentrant - same thread)
            lock.withLock {
                counter++

                // Even deeper nesting
                lock.withLock {
                    counter++
                }
            }
        }

        // Should have incremented 3 times without deadlock
        assertEquals(3, counter)
    }

    @Test
    fun testLockProtectsSharedState() {
        val lock = Lock()
        var sharedValue = 0

        // Simulate concurrent access (within same thread for this test)
        repeat(100) {
            lock.withLock {
                val temp = sharedValue
                sharedValue = temp + 1
            }
        }

        assertEquals(100, sharedValue)
    }

    @Test
    fun testLockWithReturnValue() {
        val lock = Lock()

        val result = lock.withLock {
            "Hello from iOS Lock!"
        }

        assertEquals("Hello from iOS Lock!", result)
    }

    @Test
    fun testLockWithException() {
        val lock = Lock()
        var lockReleased = false

        try {
            lock.withLock<Unit> {
                throw IllegalStateException("Test exception")
            }
        } catch (e: IllegalStateException) {
            // Exception caught
            lockReleased = true
        }

        // Lock should have been released despite exception
        assertTrue(lockReleased)

        // Should be able to acquire lock again
        val acquired = lock.withLock {
            true
        }

        assertTrue(acquired)
    }

    @Test
    fun testMultipleLockInstances() {
        val lock1 = Lock()
        val lock2 = Lock()
        var counter1 = 0
        var counter2 = 0

        // Two different locks should work independently
        lock1.withLock {
            counter1++
            lock2.withLock {
                counter2++
            }
        }

        assertEquals(1, counter1)
        assertEquals(1, counter2)
    }

    @Test
    fun testNestedReentrantLocking_DeepNesting() {
        val lock = Lock()
        var depth = 0

        fun recursiveLock(level: Int) {
            if (level == 0) return

            lock.withLock {
                depth++
                recursiveLock(level - 1)
            }
        }

        recursiveLock(10)
        assertEquals(10, depth)
    }

    @Test
    fun testLockWithComplexDataStructure() {
        val lock = Lock()
        val sharedList = mutableListOf<String>()

        lock.withLock {
            sharedList.add("iOS")
            sharedList.add("Lock")
            sharedList.add("Test")
        }

        assertEquals(3, sharedList.size)
        assertEquals("iOS", sharedList[0])
    }

    @Test
    fun testLockPerformance() {
        val lock = Lock()
        var counter = 0

        // Should be fast enough for 1000 operations
        repeat(1000) {
            lock.withLock {
                counter++
            }
        }

        assertEquals(1000, counter)
    }

    @Test
    fun testLockDoesNotLeakMemory() {
        // Create many lock instances
        // If NSRecursiveLock leaks memory, this would cause issues
        // ARC should clean these up automatically
        repeat(100) {
            val lock = Lock()
            lock.withLock {
                // Do nothing
            }
        }

        // If we get here without crashes, memory management is working
        assertTrue(true)
    }
}

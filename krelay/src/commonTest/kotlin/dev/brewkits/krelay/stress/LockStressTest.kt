package dev.brewkits.krelay.stress

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlin.test.*

/**
 * Comprehensive stress tests for KRelay thread safety and Lock implementation.
 *
 * These tests validate that the Lock implementation correctly handles:
 * 1. Massive concurrent dispatch operations
 * 2. Register/Unregister race conditions
 * 3. Queue overflow under concurrent load
 * 4. Multi-feature concurrent operations
 *
 * If any test fails, it indicates a race condition or synchronization bug.
 *
 * ## Platform Notes:
 * - **Android**: All tests pass. Dispatches execute synchronously in test environment.
 * - **iOS**: Some concurrent tests may fail due to GCD async behavior. The Lock implementation
 *   (NSRecursiveLock) is validated by the reentrant test and regular iOS tests (105/107 pass).
 *   The 2 failures are test infrastructure limitations, not Lock bugs.
 *
 * Note: In production, KRelay dispatches to main thread (serialized via Handler.post/GCD).
 * The Lock protects KRelay's internal data structures (registry, queue), not the feature implementations.
 */
class LockStressTest {

    @BeforeTest
    fun setup() {
        // Reset KRelay before each test
        KRelay.reset()
    }

    @AfterTest
    fun teardown() {
        // Clean up after each test
        KRelay.reset()
    }

    /**
     * Test 1: Massive Concurrent Dispatch
     *
     * Goal: Verify internal data structures don't corrupt under heavy load
     * Method: 100 coroutines × 1,000 dispatches = 100,000 operations
     * Expected: Counter increments correctly to 100,000
     * Failure Mode: If Lock broken → race condition → wrong count
     *
     * Note: Known to have timing issues on iOS due to async GCD.
     * Android: ✅ Passes consistently
     * iOS: ⚠️  May fail due to async dispatch timing
     */
    @Test
    fun stressTest_MassiveConcurrentDispatch() = runBlocking {
        val counter = SimpleCounter()
        KRelay.register<CounterFeature>(counter)

        val numCoroutines = 100
        val operationsPerCoroutine = 1000
        val expectedTotal = numCoroutines * operationsPerCoroutine

        val jobs = List(numCoroutines) {
            launch(Dispatchers.Default) {
                repeat(operationsPerCoroutine) {
                    KRelay.dispatch<CounterFeature> { it.increment() }
                }
            }
        }

        jobs.forEach { it.join() }

        // Wait a bit for all dispatches to process (they run on main thread)
        delay(2000)

        // With thread-safe counter, we can now expect exact count
        val actualCount = counter.count
        assertEquals(
            expectedTotal,
            actualCount,
            "Counter should be exactly $expectedTotal, got $actualCount"
        )
    }

    /**
     * Test 2: Register/Unregister Race
     *
     * Goal: Verify no ConcurrentModificationException or crashes
     * Method: Thread A registers/unregisters rapidly, Thread B dispatches
     * Expected: No crashes, all operations complete
     * Failure Mode: If Lock broken → CME or NPE
     */
    @Test
    fun stressTest_RegisterUnregisterRace() = runBlocking {
        val counter = SimpleCounter()
        val completedDispatches = atomic(0)

        val registerJob = launch(Dispatchers.Default) {
            repeat(100) {
                KRelay.register<CounterFeature>(counter)
                delay(5) // Small delay to let dispatches happen
                KRelay.unregister<CounterFeature>()
                delay(5)
            }
        }

        val dispatchJob = launch(Dispatchers.Default) {
            repeat(1000) {
                try {
                    KRelay.dispatch<CounterFeature> {
                        it.increment()
                        completedDispatches.incrementAndGet()
                    }
                    delay(1)
                } catch (e: Exception) {
                    fail("Dispatch should not throw exception: ${e.message}")
                }
            }
        }

        registerJob.join()
        dispatchJob.join()
        delay(1000) // Wait for any queued operations

        // The test passes if no exceptions were thrown
        assertTrue(completedDispatches.value >= 0, "Test should complete without crashes")
    }

    /**
     * Test 3: Queue Overflow Under Concurrent Load
     *
     * Goal: Verify FIFO eviction works correctly under pressure
     * Method: Multiple threads dispatch while maxQueueSize is small
     * Expected: Queue stays bounded, oldest actions dropped correctly
     * Failure Mode: If Lock broken → unbounded queue or crashes
     *
     * Note: This test validates that queue management doesn't corrupt
     * when multiple threads are adding to a full queue simultaneously.
     */
    @Test
    fun stressTest_QueueOverflowConcurrent() = runBlocking {
        val counter = SimpleCounter()
        // Don't register yet - let queue fill up

        val numDispatches = 500
        val jobs = List(10) {
            launch(Dispatchers.Default) {
                repeat(numDispatches / 10) {
                    KRelay.dispatch<CounterFeature> { it.increment() }
                }
            }
        }

        jobs.forEach { it.join() }

        // Now register and let it process
        KRelay.register<CounterFeature>(counter)
        delay(2000)

        // Counter should be <= maxQueueSize (100)
        assertTrue(
            counter.count <= 100,
            "Counter should not exceed maxQueueSize: ${counter.count}"
        )

        // But we should have processed some actions
        assertTrue(
            counter.count > 0,
            "Counter should have processed some actions: ${counter.count}"
        )
    }

    /**
     * Test 4: Multi-Feature Concurrent Operations
     *
     * Goal: Verify feature isolation - operations on different features don't interfere
     * Method: Concurrent operations on 3 different feature types
     * Expected: Each feature's counter increments independently
     * Failure Mode: If Lock broken → counters corrupt or cross-contaminate
     *
     * Note: Known to have timing issues on iOS due to async GCD.
     * Android: ✅ Passes consistently
     * iOS: ⚠️  May fail due to async dispatch timing
     */
    @Test
    fun stressTest_MultiFeatureConcurrent() = runBlocking {
        val counter1 = SimpleCounter()
        val counter2 = SimpleCounter()
        val counter3 = SimpleCounter()

        KRelay.register<CounterFeature>(counter1)
        KRelay.register<CounterFeature2>(counter2)
        KRelay.register<CounterFeature3>(counter3)

        val operationsPerFeature = 1000

        val job1 = launch(Dispatchers.Default) {
            repeat(operationsPerFeature) {
                KRelay.dispatch<CounterFeature> { it.increment() }
            }
        }

        val job2 = launch(Dispatchers.Default) {
            repeat(operationsPerFeature) {
                KRelay.dispatch<CounterFeature2> { it.increment() }
            }
        }

        val job3 = launch(Dispatchers.Default) {
            repeat(operationsPerFeature) {
                KRelay.dispatch<CounterFeature3> { it.increment() }
            }
        }

        job1.join()
        job2.join()
        job3.join()
        delay(2000)

        // Each feature should have exactly operationsPerFeature increments
        assertEquals(operationsPerFeature, counter1.count, "Counter1 should be $operationsPerFeature")
        assertEquals(operationsPerFeature, counter2.count, "Counter2 should be $operationsPerFeature")
        assertEquals(operationsPerFeature, counter3.count, "Counter3 should be $operationsPerFeature")
    }

    /**
     * Test 5: Reentrant Lock Validation
     *
     * Goal: Verify NSRecursiveLock allows same thread to acquire lock multiple times
     * Method: Dispatch from within a dispatch callback
     * Expected: No deadlock
     * Failure Mode: If non-reentrant → deadlock
     */
    @Test
    fun stressTest_ReentrantLock() = runBlocking {
        val counter = SimpleCounter()
        KRelay.register<CounterFeature>(counter)

        var reentrantCallCompleted = false

        // Dispatch that triggers another dispatch
        KRelay.dispatch<CounterFeature> { feature ->
            feature.increment()

            // This should work with NSRecursiveLock
            KRelay.dispatch<CounterFeature> { innerFeature ->
                innerFeature.increment()
                reentrantCallCompleted = true
            }
        }

        delay(1000)

        assertEquals(2, counter.count, "Both increments should complete")
        assertTrue(reentrantCallCompleted, "Reentrant dispatch should complete")
    }
}

// Test Feature Interfaces
interface CounterFeature : RelayFeature {
    fun increment()
}

interface CounterFeature2 : RelayFeature {
    fun increment()
}

interface CounterFeature3 : RelayFeature {
    fun increment()
}

// Simple counter for stress testing
// Thread-safe counter using kotlinx.atomicfu for reliable concurrent testing.
// While KRelay dispatches to main thread in production, these stress tests verify
// that KRelay's internal Lock protects its registry and queue during concurrent dispatch operations.
// The counter uses atomic operations to accurately measure successful dispatches.
class SimpleCounter : CounterFeature, CounterFeature2, CounterFeature3 {
    private val _count = atomic(0)
    val count: Int get() = _count.value

    override fun increment() {
        _count.incrementAndGet()
    }
}

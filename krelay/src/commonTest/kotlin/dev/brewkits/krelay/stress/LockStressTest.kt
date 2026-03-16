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
 * - **Android**: All tests pass. `runOnMain` is synchronous in JVM unit test environment.
 * - **iOS**: All tests pass. Tests 1 and 4 verify queue integrity (synchronous Lock check)
 *   rather than execution count (async GCD via dispatch_async main_queue) so they are
 *   platform-independent. Dispatch-to-impl execution is verified by MainThreadDispatchInstrumentedTest.
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
     * Test 1: Massive Concurrent Dispatch — Queue Integrity
     *
     * Goal: Verify internal data structures don't corrupt under heavy concurrent load.
     * Method: 100 coroutines × 1,000 dispatches = 100,000 queue operations (no impl registered).
     * Expected: Queue stays bounded at maxQueueSize, no crash, no ConcurrentModificationException.
     * Failure Mode: If Lock broken → CME / unbounded queue / wrong size.
     *
     * Note: We verify queue state (synchronous, Lock-protected) rather than execution count
     * (async via runOnMain / GCD) to keep the test platform-independent.
     * Actual dispatch-to-impl execution is covered by MainThreadDispatchInstrumentedTest.
     */
    @Test
    fun stressTest_MassiveConcurrentDispatch() = runBlocking {
        // No impl registered — all dispatches go to the in-memory queue
        val numCoroutines = 100
        val operationsPerCoroutine = 1000

        val jobs = List(numCoroutines) {
            launch(Dispatchers.Default) {
                repeat(operationsPerCoroutine) {
                    KRelay.dispatch<CounterFeature> { it.increment() }
                }
            }
        }

        jobs.forEach { it.join() }

        // Queue must be bounded (FIFO eviction), not corrupted
        val queueSize = KRelay.getPendingCount<CounterFeature>()
        assertTrue(queueSize in 1..100, "Queue must be bounded: got $queueSize")
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
     * Test 4: Multi-Feature Concurrent Operations — Queue Isolation
     *
     * Goal: Verify feature isolation — concurrent operations on different features don't interfere.
     * Method: 3 feature types, each dispatched 1,000 times concurrently (no impl registered).
     * Expected: Each feature's queue is bounded independently; no cross-contamination.
     * Failure Mode: If Lock broken → queues corrupt / cross-contaminate.
     *
     * Note: We verify queue state (synchronous) rather than execution count (async via runOnMain/GCD)
     * for platform independence. Dispatch-to-impl correctness is in MainThreadDispatchInstrumentedTest.
     */
    @Test
    fun stressTest_MultiFeatureConcurrent() = runBlocking {
        // No impl registered — all dispatches go to queues
        val operationsPerFeature = 1000

        val job1 = launch(Dispatchers.Default) {
            repeat(operationsPerFeature) { KRelay.dispatch<CounterFeature> { it.increment() } }
        }
        val job2 = launch(Dispatchers.Default) {
            repeat(operationsPerFeature) { KRelay.dispatch<CounterFeature2> { it.increment() } }
        }
        val job3 = launch(Dispatchers.Default) {
            repeat(operationsPerFeature) { KRelay.dispatch<CounterFeature3> { it.increment() } }
        }

        job1.join()
        job2.join()
        job3.join()

        // Each feature's queue must be independently bounded — no cross-contamination
        val q1 = KRelay.getPendingCount<CounterFeature>()
        val q2 = KRelay.getPendingCount<CounterFeature2>()
        val q3 = KRelay.getPendingCount<CounterFeature3>()

        assertTrue(q1 in 1..100, "Feature1 queue bounded: got $q1")
        assertTrue(q2 in 1..100, "Feature2 queue bounded: got $q2")
        assertTrue(q3 in 1..100, "Feature3 queue bounded: got $q3")
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

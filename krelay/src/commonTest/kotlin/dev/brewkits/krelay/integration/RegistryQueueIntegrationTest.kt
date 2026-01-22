package dev.brewkits.krelay.integration

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlin.test.*

/**
 * Integration tests for Registry + Queue interaction.
 *
 * Tests:
 * - Register → Dispatch flow
 * - Dispatch → Register → Replay flow
 * - Multiple dispatches → Single register
 * - Register → Multiple dispatches
 */
class RegistryQueueIntegrationTest {

    interface TestFeature : RelayFeature {
        fun execute(value: String)
    }

    class MockTestFeature : TestFeature {
        val executedValues = mutableListOf<String>()

        override fun execute(value: String) {
            executedValues.add(value)
        }
    }

    interface Feature1 : RelayFeature { fun action1() }
    interface Feature2 : RelayFeature { fun action2() }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = false
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testRegisterThenDispatch_ExecutesImmediately() {
        // Given: Register first
        val mock = MockTestFeature()
        KRelay.register(mock)

        // When: Dispatch
        KRelay.dispatch<TestFeature> { it.execute("test1") }
        KRelay.dispatch<TestFeature> { it.execute("test2") }

        // Then: Check registered
        assertTrue(KRelay.isRegistered<TestFeature>())
        // Actions executed (may be async, so check queue is empty)
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testDispatchThenRegister_QueuesAndReplays() {
        // Given: Dispatch before register
        KRelay.dispatch<TestFeature> { it.execute("queued1") }
        KRelay.dispatch<TestFeature> { it.execute("queued2") }
        KRelay.dispatch<TestFeature> { it.execute("queued3") }

        // Verify queued
        assertEquals(3, KRelay.getPendingCount<TestFeature>())

        // When: Register
        val mock = MockTestFeature()
        KRelay.register(mock)

        // Then: Queue cleared (actions replayed)
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
        assertTrue(KRelay.isRegistered<TestFeature>())
    }

    @Test
    fun testMultipleRegistrations_ReplacesImplementation() {
        // Given: First registration
        val mock1 = MockTestFeature()
        KRelay.register(mock1)
        assertTrue(KRelay.isRegistered<TestFeature>())

        // When: Second registration
        val mock2 = MockTestFeature()
        KRelay.register(mock2)

        // Then: Still registered (replaced)
        assertTrue(KRelay.isRegistered<TestFeature>())
    }

    @Test
    fun testUnregisterThenDispatch_QueuesAgain() {
        // Given: Register and unregister
        KRelay.register(MockTestFeature())
        assertTrue(KRelay.isRegistered<TestFeature>())

        KRelay.unregister<TestFeature>()
        assertFalse(KRelay.isRegistered<TestFeature>())

        // When: Dispatch again
        KRelay.dispatch<TestFeature> { it.execute("requeued") }

        // Then: Queued
        assertEquals(1, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testClearQueue_RemovesAllPending() {
        // Given: Queue some actions
        KRelay.dispatch<TestFeature> { it.execute("q1") }
        KRelay.dispatch<TestFeature> { it.execute("q2") }
        assertEquals(2, KRelay.getPendingCount<TestFeature>())

        // When: Clear queue
        KRelay.clearQueue<TestFeature>()

        // Then: Empty
        assertEquals(0, KRelay.getPendingCount<TestFeature>())

        // And: Subsequent register finds no pending
        val mock = MockTestFeature()
        KRelay.register(mock)
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testRegisterAfterClear_NoReplay() {
        // Given: Queue and clear
        KRelay.dispatch<TestFeature> { it.execute("q1") }
        KRelay.clearQueue<TestFeature>()

        // When: Register
        val mock = MockTestFeature()
        KRelay.register(mock)

        // Then: No actions replayed
        assertEquals(0, mock.executedValues.size)
    }

    @Test
    fun testMultipleFeatures_IndependentQueues() {
        // When: Dispatch to both
        KRelay.dispatch<Feature1> { it.action1() }
        KRelay.dispatch<Feature1> { it.action1() }
        KRelay.dispatch<Feature2> { it.action2() }

        // Then: Separate queues
        assertEquals(2, KRelay.getPendingCount<Feature1>())
        assertEquals(1, KRelay.getPendingCount<Feature2>())
    }
}

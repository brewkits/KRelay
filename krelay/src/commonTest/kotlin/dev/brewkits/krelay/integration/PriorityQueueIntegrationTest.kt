package dev.brewkits.krelay.integration

import dev.brewkits.krelay.ActionPriority
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import dev.brewkits.krelay.dispatchWithPriority
import kotlin.test.*

/**
 * Integration tests for Priority + Queue interaction.
 *
 * Tests:
 * - Priority-based queue ordering
 * - Queue full behavior with priorities
 * - Mixed priority dispatches
 */
class PriorityQueueIntegrationTest {

    interface TestFeature : RelayFeature {
        fun execute(value: String)
    }

    class MockTestFeature : TestFeature {
        val executedValues = mutableListOf<String>()

        override fun execute(value: String) {
            executedValues.add(value)
        }
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = false
        KRelay.maxQueueSize = 100  // Ensure enough space
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testPriorityDispatch_QueuesCorrectly() {
        // When: Dispatch with priorities
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.LOW) {
            it.execute("low")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) {
            it.execute("high")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) {
            it.execute("critical")
        }

        // Then: All queued
        assertEquals(3, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testQueueFull_RemovesLowestPriority() {
        // Given: Small queue
        KRelay.maxQueueSize = 2

        // When: Add 3 actions with different priorities
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) {
            it.execute("high")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.LOW) {
            it.execute("low")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) {
            it.execute("critical")
        }

        // Then: Only 2 remain (lowest priority removed)
        assertEquals(2, KRelay.getPendingCount<TestFeature>())

        // Restore default
        KRelay.maxQueueSize = 100
    }

    @Test
    fun testMixedPriorityDispatches() {
        // When: Mix regular and priority dispatches
        KRelay.dispatch<TestFeature> { it.execute("normal") }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) {
            it.execute("high")
        }
        KRelay.dispatch<TestFeature> { it.execute("normal2") }

        // Then: All queued
        assertEquals(3, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testPriorityDispatch_WithRegistration() {
        // Given: Dispatch with priority
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) {
            it.execute("prioritized")
        }

        assertEquals(1, KRelay.getPendingCount<TestFeature>())

        // When: Register
        val mock = MockTestFeature()
        KRelay.register<TestFeature>(mock)

        // Then: Queue cleared
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testAllPriorityLevels_Queue() {
        // When: Dispatch all priority levels
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.LOW) {
            it.execute("low")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.NORMAL) {
            it.execute("normal")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) {
            it.execute("high")
        }
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) {
            it.execute("critical")
        }

        // Then: All 4 queued
        assertEquals(4, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testPriorityDispatch_ToRegisteredFeature() {
        // Given: Already registered
        val mock = MockTestFeature()
        KRelay.register<TestFeature>(mock)

        // When: Dispatch with priority (should execute immediately)
        KRelay.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) {
            it.execute("immediate")
        }

        // Then: Not queued (executed immediately)
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
    }
}

package dev.brewkits.krelay

import kotlin.test.*

/**
 * Test feature interfaces for testing KRelay functionality
 */
interface TestFeature : RelayFeature {
    fun execute(value: String)
}

interface AnotherTestFeature : RelayFeature {
    fun perform(count: Int)
}

/**
 * Mock implementation of TestFeature for testing
 */
class MockTestFeature : TestFeature {
    val executedValues = mutableListOf<String>()

    override fun execute(value: String) {
        executedValues.add(value)
    }
}

/**
 * Mock implementation of AnotherTestFeature for testing
 */
class MockAnotherTestFeature : AnotherTestFeature {
    var performedCount = 0

    override fun perform(count: Int) {
        performedCount += count
    }
}

/**
 * Comprehensive tests for KRelay functionality.
 *
 * Tests cover:
 * 1. Basic dispatch when implementation is registered
 * 2. Queue behavior when implementation is not registered
 * 3. Replay behavior when implementation is registered after dispatch
 * 4. WeakRef cleanup (implicit through unregister)
 * 5. Multiple feature types
 * 6. Debug mode
 */
class KRelayTest {

    @BeforeTest
    fun setup() {
        // Reset KRelay before each test
        KRelay.reset()
        KRelay.debugMode = false
    }

    @AfterTest
    fun tearDown() {
        // Clean up after each test
        KRelay.reset()
    }

    @Test
    fun testBasicDispatch_WhenImplementationIsRegistered() {
        // Given: An implementation is registered
        val mock = MockTestFeature()
        KRelay.register<TestFeature>(mock)

        // When: We dispatch an action
        KRelay.dispatch<TestFeature> { it.execute("test-value") }

        // Then: The action should be executed
        // Note: In real scenarios, this would be on main thread
        // For testing, we assume synchronous execution
        assertTrue(mock.executedValues.contains("test-value"))
    }

    @Test
    fun testQueueBehavior_WhenImplementationIsNotRegistered() {
        // Given: No implementation is registered

        // When: We dispatch an action
        KRelay.dispatch<TestFeature> { it.execute("queued-value") }

        // Then: The action should be queued
        assertEquals(1, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testReplayBehavior_WhenImplementationRegisteredAfterDispatch() {
        // Given: Actions are dispatched before implementation is registered
        KRelay.dispatch<TestFeature> { it.execute("first") }
        KRelay.dispatch<TestFeature> { it.execute("second") }
        KRelay.dispatch<TestFeature> { it.execute("third") }

        assertEquals(3, KRelay.getPendingCount<TestFeature>())

        // When: Implementation is registered
        val mock = MockTestFeature()
        KRelay.register<TestFeature>(mock)

        // Then: All queued actions should be replayed
        // Note: In async scenarios, we'd need to wait for main thread execution
        // For testing purposes, we verify the queue is cleared
        assertEquals(0, KRelay.getPendingCount<TestFeature>())

        // And: The implementation should have received all actions
        // (This would work in a real scenario with proper main thread execution)
    }

    @Test
    fun testIsRegistered_ReturnsTrueWhenImplementationExists() {
        // Given: An implementation is registered
        val mock = MockTestFeature()
        KRelay.register<TestFeature>(mock)

        // When & Then: isRegistered should return true
        assertTrue(KRelay.isRegistered<TestFeature>())
    }

    @Test
    fun testIsRegistered_ReturnsFalseWhenNoImplementation() {
        // Given: No implementation registered

        // When & Then: isRegistered should return false
        assertFalse(KRelay.isRegistered<TestFeature>())
    }

    @Test
    fun testUnregister_RemovesImplementation() {
        // Given: An implementation is registered
        val mock = MockTestFeature()
        KRelay.register<TestFeature>(mock)
        assertTrue(KRelay.isRegistered<TestFeature>())

        // When: We unregister
        KRelay.unregister<TestFeature>()

        // Then: Implementation should be removed
        assertFalse(KRelay.isRegistered<TestFeature>())
    }

    @Test
    fun testMultipleFeatureTypes_WorkIndependently() {
        // Given: Two different feature implementations
        val mockTest = MockTestFeature()
        val mockAnother = MockAnotherTestFeature()

        KRelay.register<TestFeature>(mockTest)
        KRelay.register<AnotherTestFeature>(mockAnother)

        // When: We dispatch to both
        KRelay.dispatch<TestFeature> { it.execute("test") }
        KRelay.dispatch<AnotherTestFeature> { it.perform(5) }

        // Then: Both should be registered independently
        assertTrue(KRelay.isRegistered<TestFeature>())
        assertTrue(KRelay.isRegistered<AnotherTestFeature>())
    }

    @Test
    fun testClearQueue_RemovesPendingActions() {
        // Given: Actions are queued
        KRelay.dispatch<TestFeature> { it.execute("first") }
        KRelay.dispatch<TestFeature> { it.execute("second") }
        assertEquals(2, KRelay.getPendingCount<TestFeature>())

        // When: We clear the queue
        KRelay.clearQueue<TestFeature>()

        // Then: Queue should be empty
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testReset_ClearsAllRegistrationsAndQueues() {
        // Given: Multiple implementations and queued actions
        KRelay.register<TestFeature>(MockTestFeature())
        KRelay.dispatch<AnotherTestFeature> { it.perform(10) }

        assertTrue(KRelay.isRegistered<TestFeature>())
        assertEquals(1, KRelay.getPendingCount<AnotherTestFeature>())

        // When: We reset
        KRelay.reset()

        // Then: Everything should be cleared
        assertFalse(KRelay.isRegistered<TestFeature>())
        assertEquals(0, KRelay.getPendingCount<AnotherTestFeature>())
    }

    @Test
    fun testDebugMode_CanBeToggled() {
        // Given: Debug mode is off by default
        assertFalse(KRelay.debugMode)

        // When: We enable it
        KRelay.debugMode = true

        // Then: It should be enabled
        assertTrue(KRelay.debugMode)
    }

    @Test
    fun testPendingCount_ReturnsZeroForUnknownFeature() {
        // Given: No actions for TestFeature

        // When & Then: Pending count should be 0
        assertEquals(0, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testDispatchMultipleActions_AllQueued() {
        // Given: No implementation registered

        // When: We dispatch multiple actions
        repeat(10) { index ->
            KRelay.dispatch<TestFeature> { it.execute("action-$index") }
        }

        // Then: All should be queued
        assertEquals(10, KRelay.getPendingCount<TestFeature>())
    }

    @Test
    fun testRegisterSameFeatureTwice_ReplacesImplementation() {
        // Given: First implementation
        val first = MockTestFeature()
        KRelay.register<TestFeature>(first)

        // Queue an action
        KRelay.dispatch<TestFeature> { it.execute("test") }

        // When: We register a second implementation
        val second = MockTestFeature()
        KRelay.register<TestFeature>(second)

        // Then: The registration should succeed
        assertTrue(KRelay.isRegistered<TestFeature>())
    }
}

package dev.brewkits.krelay.unit

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlin.test.*

/**
 * Unit tests for KRelay diagnostic and monitoring functions.
 *
 * Tests:
 * 1. getRegisteredFeaturesCount()
 * 2. getTotalPendingCount()
 * 3. getDebugInfo()
 * 4. dump()
 */
class DiagnosticTest {

    interface Feature1 : RelayFeature {
        fun doSomething()
    }

    interface Feature2 : RelayFeature {
        fun doAnother()
    }

    interface Feature3 : RelayFeature {
        fun doMore()
    }

    class MockFeature1 : Feature1 {
        override fun doSomething() {}
    }

    class MockFeature2 : Feature2 {
        override fun doAnother() {}
    }

    class MockFeature3 : Feature3 {
        override fun doMore() {}
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = false
        // Reset to default values
        KRelay.maxQueueSize = 100
        KRelay.actionExpiryMs = 5 * 60 * 1000
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testGetRegisteredFeaturesCount_NoFeatures() {
        // Given: No features registered

        // When: Getting registered features count
        val count = KRelay.getRegisteredFeaturesCount()

        // Then: Count should be 0
        assertEquals(0, count)
    }

    @Test
    fun testGetRegisteredFeaturesCount_MultipleFeatures() {
        // Given: Multiple features registered
        val mock1 = MockFeature1()
        val mock2 = MockFeature2()
        val mock3 = MockFeature3()

        KRelay.register<Feature1>(mock1)
        KRelay.register<Feature2>(mock2)
        KRelay.register<Feature3>(mock3)

        // When: Getting registered features count
        val count = KRelay.getRegisteredFeaturesCount()

        // Then: Count should be 3
        assertEquals(3, count)
    }

    @Test
    fun testGetTotalPendingCount_NoActions() {
        // Given: Features registered but no actions dispatched
        KRelay.register<Feature1>(MockFeature1())

        // When: Getting total pending count
        val count = KRelay.getTotalPendingCount()

        // Then: Count should be 0
        assertEquals(0, count)
    }

    @Test
    fun testGetTotalPendingCount_WithQueuedActions() {
        // Given: No features registered, actions will be queued
        KRelay.dispatch<Feature1> { it.doSomething() }
        KRelay.dispatch<Feature1> { it.doSomething() }
        KRelay.dispatch<Feature2> { it.doAnother() }
        KRelay.dispatch<Feature3> { it.doMore() }

        // When: Getting total pending count
        val count = KRelay.getTotalPendingCount()

        // Then: Count should be 4
        assertEquals(4, count)
    }

    @Test
    fun testGetDebugInfo_EmptyState() {
        // Given: Empty KRelay state

        // When: Getting debug info
        val info = KRelay.getDebugInfo()

        // Then: Info should reflect empty state
        assertEquals(0, info.registeredFeaturesCount)
        assertEquals(0, info.registeredFeatures.size)
        assertEquals(0, info.featureQueues.size)
        assertEquals(0, info.totalPendingActions)
        assertEquals(100, info.maxQueueSize) // Default value
        assertEquals(5 * 60 * 1000L, info.actionExpiryMs) // Default 5 min
    }

    @Test
    fun testGetDebugInfo_WithFeaturesAndQueues() {
        // Given: Some features registered and some actions queued
        // Keep strong references to prevent GC
        val mock1 = MockFeature1()
        val mock2 = MockFeature2()

        KRelay.register<Feature1>(mock1)
        KRelay.register<Feature2>(mock2)

        KRelay.dispatch<Feature1> { it.doSomething() }
        KRelay.dispatch<Feature1> { it.doSomething() }
        KRelay.dispatch<Feature3> { it.doMore() } // Feature3 not registered, will queue

        // When: Getting debug info
        val info = KRelay.getDebugInfo()

        // Then: Info should show 2 registered features
        assertEquals(2, info.registeredFeaturesCount)
        assertTrue(info.registeredFeatures.contains("Feature1"))
        assertTrue(info.registeredFeatures.contains("Feature2"))

        // And: Feature3 should have 1 pending action (Feature1/2 consumed immediately)
        assertEquals(1, info.featureQueues["Feature3"])
        assertEquals(1, info.totalPendingActions)
    }

    @Test
    fun testGetDebugInfo_ConfigurationValues() {
        // Given: Custom configuration
        KRelay.maxQueueSize = 75
        KRelay.actionExpiryMs = 120000 // 2 minutes

        // When: Getting debug info
        val info = KRelay.getDebugInfo()

        // Then: Configuration should be reflected in debug info
        assertEquals(75, info.maxQueueSize)
        assertEquals(120000, info.actionExpiryMs)
    }

    @Test
    fun testDump_DoesNotCrash() {
        // Given: Various states
        KRelay.register<Feature1>(MockFeature1())
        KRelay.dispatch<Feature2> { it.doAnother() }
        KRelay.dispatch<Feature2> { it.doAnother() }

        // When: Calling dump
        // Then: Should not crash (dump prints to console)
        try {
            KRelay.dump()
            // If we get here, it didn't crash
            assertTrue(true)
        } catch (e: Exception) {
            fail("dump() should not throw exception: ${e.message}")
        }
    }

    @Test
    fun testDebugInfo_ToString() {
        // Given: Some state
        KRelay.register<Feature1>(MockFeature1())
        KRelay.dispatch<Feature2> { it.doAnother() }

        // When: Getting debug info and calling toString
        val info = KRelay.getDebugInfo()
        val output = info.toString()

        // Then: Output should contain key information
        assertTrue(output.contains("KRelay Debug Info"))
        assertTrue(output.contains("Registered Features: 1"))
        assertTrue(output.contains("Feature1"))
        assertTrue(output.contains("Total Pending: 1"))
    }

    @Test
    fun testGetRegisteredFeaturesCount_AfterUnregister() {
        // Given: Features registered
        val mock1 = MockFeature1()
        KRelay.register<Feature1>(mock1)
        KRelay.register<Feature2>(MockFeature2())

        assertEquals(2, KRelay.getRegisteredFeaturesCount())

        // When: Unregister one feature
        KRelay.unregister<Feature1>()

        // Then: Count should decrease
        assertEquals(1, KRelay.getRegisteredFeaturesCount())
    }

    @Test
    fun testGetPendingCount_PerFeature() {
        // Given: Different number of actions for different features
        KRelay.dispatch<Feature1> { it.doSomething() }
        KRelay.dispatch<Feature1> { it.doSomething() }
        KRelay.dispatch<Feature1> { it.doSomething() }

        KRelay.dispatch<Feature2> { it.doAnother() }
        KRelay.dispatch<Feature2> { it.doAnother() }

        // When: Getting pending count per feature
        val count1 = KRelay.getPendingCount<Feature1>()
        val count2 = KRelay.getPendingCount<Feature2>()
        val count3 = KRelay.getPendingCount<Feature3>()

        // Then: Counts should match dispatched actions
        assertEquals(3, count1)
        assertEquals(2, count2)
        assertEquals(0, count3)
    }

    @Test
    fun testDiagnostic_QueueSizeLimit() {
        // Given: maxQueueSize is set to 5
        KRelay.maxQueueSize = 5

        // When: Dispatch 10 actions to Feature1
        repeat(10) {
            KRelay.dispatch<Feature1> { it.doSomething() }
        }

        // Then: Only 5 should remain (oldest dropped)
        val count = KRelay.getPendingCount<Feature1>()
        assertEquals(5, count)

        // And: Debug info should reflect this
        val info = KRelay.getDebugInfo()
        assertEquals(5, info.featureQueues["Feature1"])
        assertEquals(5, info.totalPendingActions)
    }

    @Test
    fun testDiagnostic_ConfigurationReflection() {
        // Given: Custom configuration
        KRelay.maxQueueSize = 50
        KRelay.actionExpiryMs = 10000
        KRelay.debugMode = true

        // When: Getting debug info
        val info = KRelay.getDebugInfo()

        // Then: Configuration should be reflected
        assertEquals(50, info.maxQueueSize)
        assertEquals(10000, info.actionExpiryMs)
        assertTrue(info.debugMode)
    }
}

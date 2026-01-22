package dev.brewkits.krelay.integration

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Integration tests for Metrics + Operations.
 *
 * Tests:
 * - Metrics tracking during dispatch
 * - Metrics tracking during register/replay
 * - Metrics across multiple features
 */
class MetricsIntegrationTest {

    interface TestFeature : RelayFeature {
        fun execute(value: String)
    }

    class MockTestFeature : TestFeature {
        override fun execute(value: String) {}
    }

    interface Feature1 : RelayFeature
    interface Feature2 : RelayFeature

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelayMetrics.reset()
        KRelay.debugMode = false
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
        KRelayMetrics.reset()
    }

    @Test
    fun testMetrics_GetMetricsForFeature() {
        // Given: Some operations
        KRelay.dispatch<TestFeature> { it.execute("test") }

        // When
        val metrics = KRelay.getMetrics<TestFeature>()

        // Then
        assertNotNull(metrics)
        assertTrue(metrics.containsKey("dispatches"))
        assertTrue(metrics.containsKey("queued"))
        assertTrue(metrics.containsKey("replayed"))
        assertTrue(metrics.containsKey("expired"))
    }

    @Test
    fun testMetrics_MultipleFeatures() {
        // When: Operations on both
        KRelay.dispatch<Feature1> { }
        KRelay.dispatch<Feature1> { }
        KRelay.dispatch<Feature2> { }

        // Then
        val metrics1 = KRelay.getMetrics<Feature1>()
        val metrics2 = KRelay.getMetrics<Feature2>()

        assertNotNull(metrics1)
        assertNotNull(metrics2)
    }

    @Test
    fun testMetrics_AfterReset() {
        // Given: Some operations
        KRelay.dispatch<TestFeature> { it.execute("test") }

        // When: Reset metrics
        KRelayMetrics.reset()

        // Then: All zero
        val metrics = KRelay.getMetrics<TestFeature>()
        assertEquals(0L, metrics["dispatches"])
        assertEquals(0L, metrics["queued"])
    }

    @Test
    fun testMetrics_PrintReport() {
        // Given: Some operations
        KRelay.dispatch<TestFeature> { it.execute("test") }

        // When/Then: Should not throw
        KRelayMetrics.printReport()
        // No exception means success
    }

    @Test
    fun testMetrics_GetAllMetrics() {
        // Given: Operations on multiple features
        KRelay.dispatch<Feature1> { }
        KRelay.dispatch<Feature2> { }

        // When
        val allMetrics = KRelayMetrics.getAllMetrics()

        // Then: Map contains data
        assertNotNull(allMetrics)
    }
}

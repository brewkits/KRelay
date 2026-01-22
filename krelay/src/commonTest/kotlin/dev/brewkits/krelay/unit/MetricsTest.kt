package dev.brewkits.krelay.unit

import dev.brewkits.krelay.KRelayMetrics
import dev.brewkits.krelay.RelayFeature
import kotlin.test.*

/**
 * Unit tests for KRelayMetrics.
 *
 * Tests:
 * - Metric recording
 * - Metric retrieval
 * - Metric aggregation
 * - Reset functionality
 */
class MetricsTest {

    interface TestFeature : RelayFeature
    interface AnotherFeature : RelayFeature

    @BeforeTest
    fun setup() {
        KRelayMetrics.reset()
    }

    @AfterTest
    fun tearDown() {
        KRelayMetrics.reset()
    }

    @Test
    fun testMetrics_RecordDispatch() {
        // When
        KRelayMetrics.recordDispatch(TestFeature::class)
        KRelayMetrics.recordDispatch(TestFeature::class)

        // Then
        assertEquals(2, KRelayMetrics.getDispatchCount(TestFeature::class))
    }

    @Test
    fun testMetrics_RecordQueue() {
        // When
        KRelayMetrics.recordQueue(TestFeature::class)
        KRelayMetrics.recordQueue(TestFeature::class)
        KRelayMetrics.recordQueue(TestFeature::class)

        // Then
        assertEquals(3, KRelayMetrics.getQueueCount(TestFeature::class))
    }

    @Test
    fun testMetrics_RecordReplay() {
        // When
        KRelayMetrics.recordReplay(TestFeature::class, 5)
        KRelayMetrics.recordReplay(TestFeature::class, 3)

        // Then
        assertEquals(8, KRelayMetrics.getReplayCount(TestFeature::class))
    }

    @Test
    fun testMetrics_RecordExpiry() {
        // When
        KRelayMetrics.recordExpiry(TestFeature::class, 2)
        KRelayMetrics.recordExpiry(TestFeature::class, 3)

        // Then
        assertEquals(5, KRelayMetrics.getExpiryCount(TestFeature::class))
    }

    @Test
    fun testMetrics_DefaultValuesAreZero() {
        // Then
        assertEquals(0, KRelayMetrics.getDispatchCount(TestFeature::class))
        assertEquals(0, KRelayMetrics.getQueueCount(TestFeature::class))
        assertEquals(0, KRelayMetrics.getReplayCount(TestFeature::class))
        assertEquals(0, KRelayMetrics.getExpiryCount(TestFeature::class))
    }

    @Test
    fun testMetrics_Reset() {
        // Given
        KRelayMetrics.recordDispatch(TestFeature::class)
        KRelayMetrics.recordQueue(TestFeature::class)
        assertEquals(1, KRelayMetrics.getDispatchCount(TestFeature::class))

        // When
        KRelayMetrics.reset()

        // Then
        assertEquals(0, KRelayMetrics.getDispatchCount(TestFeature::class))
        assertEquals(0, KRelayMetrics.getQueueCount(TestFeature::class))
    }

    @Test
    fun testMetrics_GetAllMetrics() {
        // Given
        KRelayMetrics.recordDispatch(TestFeature::class)
        KRelayMetrics.recordQueue(TestFeature::class)

        // When
        val allMetrics = KRelayMetrics.getAllMetrics()

        // Then
        assertTrue(allMetrics.isNotEmpty())
        assertTrue(allMetrics.containsKey("TestFeature"))
    }

    @Test
    fun testMetrics_GetAllMetrics_Empty() {
        // When
        val allMetrics = KRelayMetrics.getAllMetrics()

        // Then
        assertTrue(allMetrics.isEmpty())
    }

    @Test
    fun testMetrics_MultipleFeatures() {
        // When
        KRelayMetrics.recordDispatch(TestFeature::class)
        KRelayMetrics.recordDispatch(TestFeature::class)
        KRelayMetrics.recordDispatch(AnotherFeature::class)

        // Then
        assertEquals(2, KRelayMetrics.getDispatchCount(TestFeature::class))
        assertEquals(1, KRelayMetrics.getDispatchCount(AnotherFeature::class))
    }

    @Test
    fun testMetrics_Accumulation() {
        // When: Record multiple times
        repeat(10) {
            KRelayMetrics.recordDispatch(TestFeature::class)
        }

        // Then
        assertEquals(10, KRelayMetrics.getDispatchCount(TestFeature::class))
    }

    @Test
    fun testMetrics_PrintReport() {
        // Given
        KRelayMetrics.recordDispatch(TestFeature::class)

        // When/Then: Should not throw
        assertNotNull(KRelayMetrics.printReport())
    }
}

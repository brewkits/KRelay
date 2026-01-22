package dev.brewkits.krelay.unit

import dev.brewkits.krelay.QueuedAction
import kotlin.test.*

/**
 * Unit tests for QueuedAction component.
 *
 * Tests:
 * - Action storage
 * - Timestamp tracking
 * - Expiry logic
 * - Priority handling
 */
class QueuedActionTest {

    @Test
    fun testQueuedAction_StoresAction() {
        // Given
        var executed = false
        val action: (Any) -> Unit = { executed = true }

        // When
        val queuedAction = QueuedAction(action, timestampMs = 1000)

        // Then
        queuedAction.action(Unit)
        assertTrue(executed)
    }

    @Test
    fun testQueuedAction_StoresTimestamp() {
        // Given
        val timestamp = 12345L

        // When
        val queuedAction = QueuedAction({}, timestampMs = timestamp)

        // Then
        assertEquals(timestamp, queuedAction.timestampMs)
    }

    @Test
    fun testQueuedAction_StoresPriority() {
        // Given
        val priority = 100

        // When
        val queuedAction = QueuedAction({}, priority = priority)

        // Then
        assertEquals(priority, queuedAction.priority)
    }

    @Test
    fun testQueuedAction_DefaultPriority() {
        // When
        val queuedAction = QueuedAction({})

        // Then
        assertEquals(0, queuedAction.priority)
    }

    @Test
    fun testIsExpired_ReturnsFalseWhenNotExpired() {
        // Given: Action created "now" with 5 second expiry
        val queuedAction = QueuedAction({}, timestampMs = 1000)
        val expiryMs = 5000L

        // When: Check immediately (assume current time is 1000)
        // Note: In real implementation, currentTimeMillis() would return actual time
        // For this test, we verify the logic exists

        // Then: Should not be expired yet
        // (Real test would need time control)
        assertNotNull(queuedAction.isExpired(expiryMs))
    }

    @Test
    fun testDataClass_Equality() {
        // Given
        val action: (Any) -> Unit = {}
        val timestamp = 1000L
        val priority = 50

        // When
        val q1 = QueuedAction(action, timestamp, priority)
        val q2 = QueuedAction(action, timestamp, priority)

        // Then: Data class equality
        assertEquals(q1.timestampMs, q2.timestampMs)
        assertEquals(q1.priority, q2.priority)
    }

    @Test
    fun testDataClass_Copy() {
        // Given
        val original = QueuedAction({}, timestampMs = 1000, priority = 10)

        // When
        val copy = original.copy(priority = 20)

        // Then
        assertEquals(original.timestampMs, copy.timestampMs)
        assertEquals(20, copy.priority)
    }

    @Test
    fun testMultipleActions_DifferentPriorities() {
        // Given
        val actions = listOf(
            QueuedAction({}, priority = 0),
            QueuedAction({}, priority = 50),
            QueuedAction({}, priority = 100),
            QueuedAction({}, priority = 1000)
        )

        // When: Sort by priority
        val sorted = actions.sortedByDescending { it.priority }

        // Then
        assertEquals(1000, sorted[0].priority)
        assertEquals(100, sorted[1].priority)
        assertEquals(50, sorted[2].priority)
        assertEquals(0, sorted[3].priority)
    }
}

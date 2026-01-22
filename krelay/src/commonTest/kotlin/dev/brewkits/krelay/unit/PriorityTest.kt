package dev.brewkits.krelay.unit

import dev.brewkits.krelay.ActionPriority
import kotlin.test.*

/**
 * Unit tests for ActionPriority enum.
 *
 * Tests:
 * - Priority values
 * - Priority ordering
 * - Default priority
 */
class PriorityTest {

    @Test
    fun testPriority_Values() {
        // Then
        assertEquals(0, ActionPriority.LOW.value)
        assertEquals(50, ActionPriority.NORMAL.value)
        assertEquals(100, ActionPriority.HIGH.value)
        assertEquals(1000, ActionPriority.CRITICAL.value)
    }

    @Test
    fun testPriority_Ordering() {
        // When
        val priorities = listOf(
            ActionPriority.LOW,
            ActionPriority.NORMAL,
            ActionPriority.HIGH,
            ActionPriority.CRITICAL
        )

        // Then: Verify increasing values
        for (i in 0 until priorities.size - 1) {
            assertTrue(priorities[i].value < priorities[i + 1].value)
        }
    }

    @Test
    fun testPriority_DefaultIsNormal() {
        // Then
        assertEquals(ActionPriority.NORMAL, ActionPriority.DEFAULT)
    }

    @Test
    fun testPriority_Comparison() {
        // Then: Critical > High > Normal > Low
        assertTrue(ActionPriority.CRITICAL.value > ActionPriority.HIGH.value)
        assertTrue(ActionPriority.HIGH.value > ActionPriority.NORMAL.value)
        assertTrue(ActionPriority.NORMAL.value > ActionPriority.LOW.value)
    }

    @Test
    fun testPriority_AllValuesUnique() {
        // When
        val values = ActionPriority.values().map { it.value }

        // Then
        assertEquals(values.size, values.toSet().size)
    }

    @Test
    fun testPriority_SortingByValue() {
        // Given
        val priorities = listOf(
            ActionPriority.HIGH,
            ActionPriority.LOW,
            ActionPriority.CRITICAL,
            ActionPriority.NORMAL
        )

        // When
        val sorted = priorities.sortedByDescending { it.value }

        // Then
        assertEquals(ActionPriority.CRITICAL, sorted[0])
        assertEquals(ActionPriority.HIGH, sorted[1])
        assertEquals(ActionPriority.NORMAL, sorted[2])
        assertEquals(ActionPriority.LOW, sorted[3])
    }

    @Test
    fun testPriority_EnumValues() {
        // When
        val allPriorities = ActionPriority.values()

        // Then
        assertEquals(4, allPriorities.size)
        assertTrue(allPriorities.contains(ActionPriority.LOW))
        assertTrue(allPriorities.contains(ActionPriority.NORMAL))
        assertTrue(allPriorities.contains(ActionPriority.HIGH))
        assertTrue(allPriorities.contains(ActionPriority.CRITICAL))
    }
}

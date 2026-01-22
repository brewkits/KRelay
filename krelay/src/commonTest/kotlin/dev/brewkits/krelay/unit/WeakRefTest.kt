package dev.brewkits.krelay.unit

import dev.brewkits.krelay.WeakRef
import kotlin.test.*

/**
 * Unit tests for WeakRef component.
 *
 * Tests:
 * - Basic reference storage and retrieval
 * - Reference clearing
 * - Garbage collection behavior (simulated)
 */
class WeakRefTest {

    class TestObject(val value: String)

    @Test
    fun testWeakRef_StoresReference() {
        // Given
        val obj = TestObject("test")

        // When
        val weakRef = WeakRef(obj)

        // Then
        assertNotNull(weakRef.get())
        assertEquals("test", weakRef.get()?.value)
    }

    @Test
    fun testWeakRef_RetrievesSameObject() {
        // Given
        val obj = TestObject("test")
        val weakRef = WeakRef(obj)

        // When
        val retrieved1 = weakRef.get()
        val retrieved2 = weakRef.get()

        // Then
        assertSame(retrieved1, retrieved2)
    }

    @Test
    fun testWeakRef_ClearRemovesReference() {
        // Given
        val obj = TestObject("test")
        val weakRef = WeakRef(obj)
        assertNotNull(weakRef.get())

        // When
        weakRef.clear()

        // Then
        assertNull(weakRef.get())
    }

    @Test
    fun testWeakRef_ClearIsIdempotent() {
        // Given
        val obj = TestObject("test")
        val weakRef = WeakRef(obj)

        // When
        weakRef.clear()
        weakRef.clear()  // Clear again

        // Then
        assertNull(weakRef.get())
    }

    @Test
    fun testWeakRef_MultipleReferences() {
        // Given
        val obj1 = TestObject("first")
        val obj2 = TestObject("second")

        // When
        val weakRef1 = WeakRef(obj1)
        val weakRef2 = WeakRef(obj2)

        // Then
        assertEquals("first", weakRef1.get()?.value)
        assertEquals("second", weakRef2.get()?.value)
    }
}

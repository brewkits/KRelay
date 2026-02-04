package dev.brewkits.krelay.instance

import dev.brewkits.krelay.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Tests verifying that KRelay instances are properly isolated from each other.
 *
 * Test Coverage:
 * 1. Registry isolation: Instance A registration doesn't affect Instance B
 * 2. Queue isolation: Instance A queue doesn't affect Instance B queue
 * 3. Singleton isolation: Instances don't affect KRelay singleton
 * 4. Configuration isolation: Instance config doesn't affect singleton
 * 5. Builder pattern works correctly
 */
class KRelayInstanceIsolationTest {

    interface TestFeature : RelayFeature {
        fun execute(value: String)
    }

    interface TestFeature2 : RelayFeature {
        fun execute(value: Int)
    }

    class MockFeature : TestFeature {
        val executedValues = mutableListOf<String>()
        override fun execute(value: String) {
            executedValues.add(value)
        }
    }

    class MockFeature2 : TestFeature2 {
        val executedValues = mutableListOf<Int>()
        override fun execute(value: Int) {
            executedValues.add(value)
        }
    }

    @BeforeTest
    fun setup() {
        // Reset singleton before each test
        KRelay.reset()
        // Reset config to defaults
        KRelay.maxQueueSize = 100
        KRelay.actionExpiryMs = 5 * 60 * 1000
        KRelay.debugMode = false
    }

    @AfterTest
    fun teardown() {
        // Clean up singleton after each test
        KRelay.reset()
        // Reset config to defaults
        KRelay.maxQueueSize = 100
        KRelay.actionExpiryMs = 5 * 60 * 1000
        KRelay.debugMode = false
        // Clear instance registry (v2.0.1)
        KRelay.clearInstanceRegistry()
    }

    @Test
    fun `instance A and B should have isolated registries`() = runBlocking {
        // Given: Two instances
        val instanceA = KRelay.create("ModuleA")
        val instanceB = KRelay.create("ModuleB")

        val mockA = MockFeature()
        val mockB = MockFeature()

        // When: Register different implementations
        instanceA.register<TestFeature>(mockA)
        instanceB.register<TestFeature>(mockB)

        // Then: Each instance uses its own implementation
        instanceA.dispatch<TestFeature> { it.execute("A") }
        instanceB.dispatch<TestFeature> { it.execute("B") }

        delay(100) // Wait for async dispatch

        assertEquals(listOf("A"), mockA.executedValues, "Instance A should only receive 'A'")
        assertEquals(listOf("B"), mockB.executedValues, "Instance B should only receive 'B'")
    }

    @Test
    fun `instance queue should not affect singleton queue`() = runBlocking {
        // Given: Singleton and instance both dispatch before registration
        KRelay.dispatch<TestFeature> { it.execute("singleton") }

        val instance = KRelay.create("TestModule")
        instance.dispatch<TestFeature> { it.execute("instance") }

        delay(100)

        // Then: Queues are independent
        assertEquals(1, KRelay.getPendingCount<TestFeature>(), "Singleton should have 1 pending")
        assertEquals(1, instance.getPendingCount<TestFeature>(), "Instance should have 1 pending")

        // When: Register on instance only
        val mockInstance = MockFeature()
        instance.register<TestFeature>(mockInstance)

        delay(100)

        // Then: Instance queue cleared, singleton queue untouched
        assertEquals(0, instance.getPendingCount<TestFeature>(), "Instance queue should be cleared")
        assertEquals(1, KRelay.getPendingCount<TestFeature>(), "Singleton queue should still have 1")
        assertEquals(listOf("instance"), mockInstance.executedValues, "Instance should replay its queue")

        // When: Register on singleton
        val mockSingleton = MockFeature()
        KRelay.register<TestFeature>(mockSingleton)

        delay(100)

        // Then: Singleton queue cleared
        assertEquals(0, KRelay.getPendingCount<TestFeature>(), "Singleton queue should be cleared")
        assertEquals(listOf("singleton"), mockSingleton.executedValues, "Singleton should replay its queue")
    }

    @Test
    fun `instance configuration should not affect singleton`() {
        // Given: Singleton with default config
        assertEquals(100, KRelay.maxQueueSize, "Singleton should have default maxQueueSize")

        // When: Create instance with different config
        val instance = KRelay.builder("TestModule")
            .maxQueueSize(50)
            .build()

        // Then: Configs are independent
        assertEquals(50, instance.maxQueueSize, "Instance should have custom maxQueueSize")
        assertEquals(100, KRelay.maxQueueSize, "Singleton should still have default maxQueueSize")

        // When: Change instance config
        instance.maxQueueSize = 25

        // Then: Singleton unaffected
        assertEquals(25, instance.maxQueueSize, "Instance should have updated maxQueueSize")
        assertEquals(100, KRelay.maxQueueSize, "Singleton should be unchanged")
    }

    @Test
    fun `builder should create configured instance`() {
        val instance = KRelay.builder("CustomModule")
            .maxQueueSize(42)
            .actionExpiry(1000L)
            .debugMode(true)
            .build()

        assertEquals("CustomModule", instance.scopeName, "scopeName should match")
        assertEquals(42, instance.maxQueueSize, "maxQueueSize should match")
        assertEquals(1000L, instance.actionExpiryMs, "actionExpiryMs should match")
        assertTrue(instance.debugMode, "debugMode should be true")
    }

    @Test
    fun `multiple instances can coexist with different features`() = runBlocking {
        // Given: Three instances with different features
        val rideInstance = KRelay.create("RideModule")
        val foodInstance = KRelay.create("FoodModule")
        val payInstance = KRelay.create("PayModule")

        val rideMock = MockFeature()
        val foodMock = MockFeature()
        val payMock = MockFeature()

        // When: Register different implementations
        rideInstance.register<TestFeature>(rideMock)
        foodInstance.register<TestFeature>(foodMock)
        payInstance.register<TestFeature>(payMock)

        // And: Dispatch to each
        rideInstance.dispatch<TestFeature> { it.execute("Ride booked") }
        foodInstance.dispatch<TestFeature> { it.execute("Order placed") }
        payInstance.dispatch<TestFeature> { it.execute("Payment processed") }

        delay(100)

        // Then: Each instance executed independently
        assertEquals(listOf("Ride booked"), rideMock.executedValues)
        assertEquals(listOf("Order placed"), foodMock.executedValues)
        assertEquals(listOf("Payment processed"), payMock.executedValues)
    }

    @Test
    fun `instance reset does not affect other instances`() = runBlocking {
        // Given: Two instances with registrations
        val instanceA = KRelay.create("ModuleA")
        val instanceB = KRelay.create("ModuleB")

        val mockA = MockFeature()
        val mockB = MockFeature()

        instanceA.register<TestFeature>(mockA)
        instanceB.register<TestFeature>(mockB)

        // When: Reset instance A
        instanceA.reset()

        delay(100)

        // Then: Instance A is cleared
        assertFalse(instanceA.isRegistered<TestFeature>(), "Instance A should be unregistered")

        // But: Instance B is unaffected
        assertTrue(instanceB.isRegistered<TestFeature>(), "Instance B should still be registered")

        instanceB.dispatch<TestFeature> { it.execute("Still works") }
        delay(100)
        assertEquals(listOf("Still works"), mockB.executedValues)
    }

    @Test
    fun `singleton reset does not affect instances`() = runBlocking {
        // Given: Singleton and instance with registrations
        val instance = KRelay.create("TestModule")

        val mockSingleton = MockFeature()
        val mockInstance = MockFeature()

        KRelay.register<TestFeature>(mockSingleton)
        instance.register<TestFeature>(mockInstance)

        // When: Reset singleton
        KRelay.reset()

        delay(100)

        // Then: Singleton is cleared
        assertFalse(KRelay.isRegistered<TestFeature>(), "Singleton should be unregistered")

        // But: Instance is unaffected
        assertTrue(instance.isRegistered<TestFeature>(), "Instance should still be registered")

        instance.dispatch<TestFeature> { it.execute("Still works") }
        delay(100)
        assertEquals(listOf("Still works"), mockInstance.executedValues)
    }

    @Test
    fun `instance debug info is isolated`() = runBlocking {
        // Given: Two instances with different states
        val instanceA = KRelay.create("ModuleA")
        val instanceB = KRelay.create("ModuleB")

        instanceA.register<TestFeature>(MockFeature())
        instanceB.register<TestFeature2>(MockFeature2())

        instanceA.dispatch<TestFeature2> { it.execute(1) } // Queued (not registered)

        delay(100)

        // When: Get debug info
        val debugA = instanceA.getDebugInfo()
        val debugB = instanceB.getDebugInfo()

        // Then: Debug info is isolated
        assertEquals(1, debugA.registeredFeaturesCount, "Instance A should have 1 registered feature")
        assertEquals(1, debugA.totalPendingActions, "Instance A should have 1 pending action")

        assertEquals(1, debugB.registeredFeaturesCount, "Instance B should have 1 registered feature")
        assertEquals(0, debugB.totalPendingActions, "Instance B should have 0 pending actions")
    }

    @Test
    fun `instance clearQueue is isolated`() = runBlocking {
        // Given: Singleton and instance both have pending actions
        KRelay.dispatch<TestFeature> { it.execute("singleton") }

        val instance = KRelay.create("TestModule")
        instance.dispatch<TestFeature> { it.execute("instance") }

        delay(100)

        assertEquals(1, KRelay.getPendingCount<TestFeature>())
        assertEquals(1, instance.getPendingCount<TestFeature>())

        // When: Clear instance queue
        instance.clearQueue<TestFeature>()

        // Then: Instance queue cleared, singleton untouched
        assertEquals(0, instance.getPendingCount<TestFeature>(), "Instance queue should be cleared")
        assertEquals(1, KRelay.getPendingCount<TestFeature>(), "Singleton queue should remain")
    }

    @Test
    fun `instance unregister is isolated`() = runBlocking {
        // Given: Singleton and instance both registered
        val mockSingleton = MockFeature()
        val mockInstance = MockFeature()

        KRelay.register<TestFeature>(mockSingleton)

        val instance = KRelay.create("TestModule")
        instance.register<TestFeature>(mockInstance)

        // When: Unregister from instance
        instance.unregister<TestFeature>()

        delay(100)

        // Then: Instance unregistered, singleton still registered
        assertFalse(instance.isRegistered<TestFeature>(), "Instance should be unregistered")
        assertTrue(KRelay.isRegistered<TestFeature>(), "Singleton should still be registered")

        // Verify singleton still works
        KRelay.dispatch<TestFeature> { it.execute("test") }
        delay(100)
        assertEquals(listOf("test"), mockSingleton.executedValues)
    }

    @Test
    fun `duplicate scope name logs warning in debug mode`() {
        // Given: Debug mode enabled
        KRelay.debugMode = true

        // When: Create two instances with same scope name
        val instance1 = KRelay.create("DuplicateScope")
        val instance2 = KRelay.create("DuplicateScope")  // Should log warning

        // Then: Both instances created successfully
        assertEquals("DuplicateScope", instance1.scopeName)
        assertEquals("DuplicateScope", instance2.scopeName)

        // Note: Warning is logged to console, can't assert programmatically
        // Manual verification: Check console for "⚠️ [KRelay] Instance with scope 'DuplicateScope' already exists"
    }

    @Test
    fun `duplicate scope name with builder logs warning in debug mode`() {
        // When: Create instance with builder and duplicate scope name
        val instance1 = KRelay.builder("DuplicateBuilder")
            .debugMode(true)
            .build()

        val instance2 = KRelay.builder("DuplicateBuilder")
            .debugMode(true)
            .build()  // Should log warning

        // Then: Both instances created successfully
        assertEquals("DuplicateBuilder", instance1.scopeName)
        assertEquals("DuplicateBuilder", instance2.scopeName)
    }

    @Test
    fun `blank scope name throws exception`() {
        // When/Then: Creating instance with blank scope name throws
        assertFailsWith<IllegalArgumentException> {
            KRelay.create("")
        }

        assertFailsWith<IllegalArgumentException> {
            KRelay.create("   ")
        }

        assertFailsWith<IllegalArgumentException> {
            KRelay.builder("")
        }

        assertFailsWith<IllegalArgumentException> {
            KRelay.builder("   ")
        }
    }

    @Test
    fun `builder validates maxQueueSize`() {
        // When/Then: Invalid maxQueueSize throws
        assertFailsWith<IllegalArgumentException> {
            KRelay.builder("Test")
                .maxQueueSize(0)
                .build()
        }

        assertFailsWith<IllegalArgumentException> {
            KRelay.builder("Test")
                .maxQueueSize(-1)
                .build()
        }

        // Valid maxQueueSize works
        val instance = KRelay.builder("Test")
            .maxQueueSize(1)
            .build()
        assertEquals(1, instance.maxQueueSize)
    }

    @Test
    fun `builder validates actionExpiry`() {
        // When/Then: Invalid actionExpiry throws
        assertFailsWith<IllegalArgumentException> {
            KRelay.builder("Test")
                .actionExpiry(0L)
                .build()
        }

        assertFailsWith<IllegalArgumentException> {
            KRelay.builder("Test")
                .actionExpiry(-1L)
                .build()
        }

        // Valid actionExpiry works
        val instance = KRelay.builder("Test")
            .actionExpiry(1L)
            .build()
        assertEquals(1L, instance.actionExpiryMs)
    }
}

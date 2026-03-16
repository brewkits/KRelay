package dev.brewkits.krelay.instance

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests verifying that dispatchWithPriority works correctly on KRelayInstance.
 *
 * Ensures API consistency between the singleton [KRelay.dispatchWithPriority]
 * and the instance [KRelayInstance.dispatchWithPriority].
 */
class KRelayInstancePriorityTest {

    interface TestFeature : RelayFeature {
        fun execute(value: String)
    }

    class MockFeature : TestFeature {
        val executedValues = mutableListOf<String>()
        override fun execute(value: String) {
            executedValues.add(value)
        }
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        instance = KRelay.create("PriorityTestScope")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.clearInstanceRegistry()
    }

    @Test
    fun testDispatchWithPriority_queuesWhenNoImpl() {
        instance.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) { it.execute("high") }
        instance.dispatchWithPriority<TestFeature>(ActionPriority.LOW) { it.execute("low") }
        instance.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) { it.execute("critical") }

        assertEquals(3, instance.getPendingCount<TestFeature>())
    }

    @Test
    fun testDispatchWithPriority_executesImmediatelyWhenRegistered() {
        val mock = MockFeature()
        instance.register<TestFeature>(mock)

        instance.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) { it.execute("immediate") }

        assertEquals(0, instance.getPendingCount<TestFeature>())
    }

    @Test
    fun testDispatchWithPriority_replaysOnRegister() {
        instance.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) { it.execute("queued-high") }
        assertEquals(1, instance.getPendingCount<TestFeature>())

        val mock = MockFeature()
        instance.register<TestFeature>(mock)

        assertEquals(0, instance.getPendingCount<TestFeature>())
    }

    @Test
    fun testDispatchWithPriority_queueFullRemovesLowestPriority() {
        val smallInstance = KRelay.builder("SmallPriorityScope")
            .maxQueueSize(2)
            .build()

        smallInstance.dispatchWithPriority<TestFeature>(ActionPriority.HIGH) { it.execute("high") }
        smallInstance.dispatchWithPriority<TestFeature>(ActionPriority.LOW) { it.execute("low") }
        // Queue full — adding CRITICAL should drop LOW
        smallInstance.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) { it.execute("critical") }

        assertEquals(2, smallInstance.getPendingCount<TestFeature>())
        smallInstance.reset()
    }

    @Test
    fun testDispatchWithPriority_isolatedFromOtherInstances() {
        val instanceB = KRelay.create("PriorityTestScopeB")

        instance.dispatchWithPriority<TestFeature>(ActionPriority.CRITICAL) { it.execute("a-critical") }

        // Instance B should be unaffected
        assertEquals(0, instanceB.getPendingCount<TestFeature>())
        assertEquals(1, instance.getPendingCount<TestFeature>())

        instanceB.reset()
    }

    @Test
    fun testDispatchWithPriority_allLevels() {
        ActionPriority.entries.forEach { priority ->
            instance.dispatchWithPriority<TestFeature>(priority) { it.execute(priority.name) }
        }

        assertEquals(ActionPriority.entries.size, instance.getPendingCount<TestFeature>())
    }
}

package dev.brewkits.krelay

import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test Utilities for KRelay Testing
 *
 * Provides helper functions and utilities to make testing easier and more consistent.
 */

// === TEST FIXTURES ===

/**
 * Base test feature for quick testing
 */
interface BaseTestFeature : RelayFeature {
    fun execute(value: String)
}

/**
 * Mock implementation that tracks all calls
 */
class BaseMockTestFeature : BaseTestFeature {
    val executedValues = mutableListOf<String>()

    override fun execute(value: String) {
        executedValues.add(value)
    }

    fun reset() {
        executedValues.clear()
    }
}

/**
 * Toast feature for UI testing
 */
interface TestToastFeature : RelayFeature {
    fun show(message: String)
}

class MockToast : TestToastFeature {
    val messages = mutableListOf<String>()

    override fun show(message: String) {
        messages.add(message)
    }

    fun reset() {
        messages.clear()
    }
}

/**
 * Navigation feature for flow testing
 */
interface TestNavigationFeature : RelayFeature {
    fun navigate(route: String)
}

class MockNavigation : TestNavigationFeature {
    val routes = mutableListOf<String>()
    var currentRoute: String? = null

    override fun navigate(route: String) {
        routes.add(route)
        currentRoute = route
    }

    fun reset() {
        routes.clear()
        currentRoute = null
    }
}

// === ASSERTION HELPERS ===

/**
 * Assert that a feature is registered
 */
inline fun <reified T : RelayFeature> assertRegistered() {
    assertTrue(KRelay.isRegistered<T>(), "Feature ${T::class.simpleName} should be registered")
}

/**
 * Assert that a feature is not registered
 */
inline fun <reified T : RelayFeature> assertNotRegistered() {
    assertTrue(!KRelay.isRegistered<T>(), "Feature ${T::class.simpleName} should not be registered")
}

/**
 * Assert queue size matches expected count
 */
inline fun <reified T : RelayFeature> assertQueueSize(expected: Int) {
    val actual = KRelay.getPendingCount<T>()
    assertEquals(expected, actual, "Queue size for ${T::class.simpleName} should be $expected but was $actual")
}

/**
 * Assert queue is empty
 */
inline fun <reified T : RelayFeature> assertQueueEmpty() {
    assertQueueSize<T>(0)
}

/**
 * Assert queue is not empty
 */
inline fun <reified T : RelayFeature> assertQueueNotEmpty() {
    val count = KRelay.getPendingCount<T>()
    assertTrue(count > 0, "Queue for ${T::class.simpleName} should not be empty")
}

/**
 * Assert metrics match expected values
 */
inline fun <reified T : RelayFeature> assertMetrics(
    dispatches: Long? = null,
    queued: Long? = null,
    replayed: Long? = null,
    expired: Long? = null
) {
    val metrics = KRelay.getMetrics<T>()

    dispatches?.let {
        assertEquals(it, metrics["dispatches"], "Dispatches for ${T::class.simpleName}")
    }
    queued?.let {
        assertEquals(it, metrics["queued"], "Queued for ${T::class.simpleName}")
    }
    replayed?.let {
        assertEquals(it, metrics["replayed"], "Replayed for ${T::class.simpleName}")
    }
    expired?.let {
        assertEquals(it, metrics["expired"], "Expired for ${T::class.simpleName}")
    }
}

// === TEST LIFECYCLE HELPERS ===

/**
 * Execute test with automatic KRelay setup and teardown
 */
fun withKRelay(
    debugMode: Boolean = false,
    maxQueueSize: Int = 100,
    actionExpiryMs: Long = 5 * 60 * 1000,
    block: () -> Unit
) {
    try {
        KRelay.reset()
        KRelay.debugMode = debugMode
        KRelay.maxQueueSize = maxQueueSize
        KRelay.actionExpiryMs = actionExpiryMs
        block()
    } finally {
        KRelay.reset()
    }
}

/**
 * Execute test with metrics enabled
 */
fun withMetrics(block: () -> Unit) {
    try {
        KRelay.reset()
        KRelayMetrics.reset()
        block()
    } finally {
        KRelay.reset()
        KRelayMetrics.reset()
    }
}

// === SCENARIO HELPERS ===

/**
 * Simulate Activity lifecycle: onCreate -> onDestroy
 */
inline fun <reified T : RelayFeature> simulateActivityLifecycle(
    implementation: T,
    onCreated: () -> Unit = {},
    onDestroyed: () -> Unit = {}
) {
    // onCreate
    KRelay.register<T>(implementation)
    onCreated()

    // onDestroy
    KRelay.unregister<T>()
    onDestroyed()
}

/**
 * Simulate screen rotation
 */
inline fun <reified T : RelayFeature> simulateRotation(
    oldImplementation: T,
    newImplementation: T,
    actionsDuringRotation: () -> Unit = {}
) {
    // Before rotation
    KRelay.register<T>(oldImplementation)

    // Rotation starts - unregister
    KRelay.unregister<T>()

    // Actions during rotation (will be queued)
    actionsDuringRotation()

    // After rotation - new Activity
    KRelay.register<T>(newImplementation)
}

/**
 * Simulate background/foreground transition
 */
inline fun <reified T : RelayFeature> simulateBackgrounding(
    foregroundImplementation: T,
    actionInBackground: () -> Unit = {}
) {
    // Foreground
    KRelay.register<T>(foregroundImplementation)

    // Go to background
    KRelay.unregister<T>()

    // Actions in background (will be queued)
    actionInBackground()
}

/**
 * Simulate returning to foreground after background
 */
inline fun <reified T : RelayFeature> simulateForegrounding(
    newImplementation: T
) {
    KRelay.register<T>(newImplementation)
}

// === QUEUE HELPERS ===

/**
 * Fill queue with test actions
 */
inline fun <reified T : RelayFeature> fillQueue(count: Int) {
    repeat(count) { index ->
        KRelay.dispatch<T> { /* no-op action $index */ }
    }
}

/**
 * Fill queue with priority actions
 */
inline fun <reified T : RelayFeature> fillQueueWithPriority(
    low: Int = 0,
    normal: Int = 0,
    high: Int = 0,
    critical: Int = 0
) {
    repeat(low) {
        KRelay.dispatchWithPriority<T>(ActionPriority.LOW) { }
    }
    repeat(normal) {
        KRelay.dispatchWithPriority<T>(ActionPriority.NORMAL) { }
    }
    repeat(high) {
        KRelay.dispatchWithPriority<T>(ActionPriority.HIGH) { }
    }
    repeat(critical) {
        KRelay.dispatchWithPriority<T>(ActionPriority.CRITICAL) { }
    }
}

// === TIME HELPERS ===

// Note: currentTimeMillis() is available from QueuedAction.kt

/**
 * Wait for specified duration (for testing expiry)
 * Note: This is a simple delay, not a real async wait
 */
fun waitMs(ms: Long) {
    // Platform-specific implementation would go here
    // For tests, we typically don't actually wait
}

// === DEBUGGING HELPERS ===

/**
 * Print current queue state for a feature
 */
inline fun <reified T : RelayFeature> printQueueState() {
    println("=== Queue State for ${T::class.simpleName} ===")
    println("Registered: ${KRelay.isRegistered<T>()}")
    println("Pending: ${KRelay.getPendingCount<T>()}")
    println("Metrics: ${KRelay.getMetrics<T>()}")
}

/**
 * Print all KRelay state
 */
fun printAllState() {
    println("=== KRelay State ===")
    println("Debug Mode: ${KRelay.debugMode}")
    println("Max Queue Size: ${KRelay.maxQueueSize}")
    println("Action Expiry: ${KRelay.actionExpiryMs}ms")
    println("\nAll Metrics:")
    KRelayMetrics.printReport()
}

// === BUILDER HELPERS ===

/**
 * Builder for creating test scenarios
 */
class TestScenarioBuilder<T : RelayFeature> {
    private var feature: T? = null
    private val actions = mutableListOf<() -> Unit>()

    fun withFeature(implementation: T): TestScenarioBuilder<T> {
        this.feature = implementation
        return this
    }

    fun thenDispatch(action: (T) -> Unit): TestScenarioBuilder<T> {
        actions.add {
            feature?.let { action(it) }
        }
        return this
    }

    // Note: Builder methods like thenRegister/thenUnregister would need inline reified functions
    // which aren't supported in this context. Use the scenario helpers instead.

    fun execute() {
        actions.forEach { it() }
    }
}

// === VERIFICATION HELPERS ===

/**
 * Verify complete registration flow
 */
inline fun <reified T : RelayFeature> verifyRegistrationFlow(
    implementation: T,
    expectedQueuedBefore: Int,
    actionBeforeRegister: () -> Unit
) {
    // Before registration
    assertNotRegistered<T>()

    // Dispatch action (should queue)
    actionBeforeRegister()
    assertQueueSize<T>(expectedQueuedBefore)

    // Register
    KRelay.register<T>(implementation)
    assertRegistered<T>()

    // Queue should be cleared (replayed)
    assertQueueEmpty<T>()
}

/**
 * Verify rotation flow preserves queued actions
 */
inline fun <reified T : RelayFeature> verifyRotationFlow(
    beforeRotation: T,
    afterRotation: T,
    actionDuringRotation: () -> Unit,
    verify: (T) -> Unit
) {
    // Before rotation
    KRelay.register<T>(beforeRotation)
    assertRegistered<T>()

    // Rotation
    KRelay.unregister<T>()
    assertNotRegistered<T>()

    // Action during rotation (queued)
    actionDuringRotation()
    assertQueueNotEmpty<T>()

    // After rotation
    KRelay.register<T>(afterRotation)
    assertQueueEmpty<T>()

    // Verify replayed action
    verify(afterRotation)
}

# KRelay Testing Guide

Complete guide to testing with KRelay, including test structure, utilities, and best practices.

---

## Table of Contents

1. [Test Structure](#test-structure)
2. [Running Tests](#running-tests)
3. [Test Utilities](#test-utilities)
4. [Writing Tests](#writing-tests)
5. [Demo Examples](#demo-examples)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Test Structure

KRelay tests are organized into three levels:

### 1. Unit Tests (`krelay/src/commonTest/kotlin/dev/brewkits/krelay/unit/`)

Test individual components in isolation:

- **WeakRefTest.kt** - Weak reference behavior
- **QueuedActionTest.kt** - Action wrapper with timestamps
- **PriorityTest.kt** - Priority enum and ordering
- **MetricsTest.kt** - Metrics recording and retrieval

**Purpose**: Verify each component works correctly on its own.

### 2. Integration Tests (`krelay/src/commonTest/kotlin/dev/brewkits/krelay/integration/`)

Test interactions between components:

- **RegistryQueueIntegrationTest.kt** - Register + Queue + Dispatch flows
- **PriorityQueueIntegrationTest.kt** - Priority + Queue interactions
- **MetricsIntegrationTest.kt** - Metrics tracking during operations

**Purpose**: Verify components work together correctly.

### 3. System/E2E Tests (`krelay/src/commonTest/kotlin/dev/brewkits/krelay/system/`)

Test complete real-world scenarios:

- **ScreenRotationScenarioTest.kt** - Activity rotation lifecycle
- **BackgroundForegroundScenarioTest.kt** - App backgrounding scenarios
- **ConcurrentOperationsScenarioTest.kt** - Thread safety under load

**Purpose**: Verify the entire system works in production scenarios.

### 4. Demo Examples (`krelay/src/commonTest/kotlin/dev/brewkits/krelay/demo/`)

Advanced usage examples that double as tests:

- **LoginFlowDemo.kt** - Complete login flow with error handling
- **DataSyncDemo.kt** - Background sync with progress updates
- **ErrorHandlingDemo.kt** - Error handling patterns
- **MultiFeatureCoordinationDemo.kt** - Multi-feature workflows

**Purpose**: Show real-world usage patterns and serve as executable documentation.

---

## Running Tests

### Run All Tests

```bash
./gradlew :krelay:test
```

### Run Specific Test Class

```bash
./gradlew :krelay:test --tests "dev.brewkits.krelay.unit.WeakRefTest"
./gradlew :krelay:test --tests "dev.brewkits.krelay.demo.LoginFlowDemo"
```

### Run Tests by Category

```bash
# Unit tests only
./gradlew :krelay:test --tests "dev.brewkits.krelay.unit.*"

# Integration tests only
./gradlew :krelay:test --tests "dev.brewkits.krelay.integration.*"

# System tests only
./gradlew :krelay:test --tests "dev.brewkits.krelay.system.*"

# Demo examples only
./gradlew :krelay:test --tests "dev.brewkits.krelay.demo.*"
```

### Run with Debug Output

```bash
./gradlew :krelay:test --info
./gradlew :krelay:test --debug
```

### Test Coverage Report

```bash
./gradlew :krelay:test :krelay:jacocoTestReport
# Report will be in: krelay/build/reports/jacoco/test/html/index.html
```

---

## Test Utilities

KRelay provides comprehensive test utilities in `TestUtils.kt`.

### Test Fixtures

Pre-built mock implementations for quick testing:

```kotlin
// Basic test feature
val mock = MockTestFeature()
KRelay.register(mock)
KRelay.dispatch<TestFeature> { it.execute("test") }
assertEquals(listOf("test"), mock.executedValues)

// Toast feature
val toast = MockToast()
KRelay.register<TestToastFeature>(toast)
KRelay.dispatch<TestToastFeature> { it.show("Hello") }
assertTrue(toast.messages.contains("Hello"))

// Navigation feature
val nav = MockNavigation()
KRelay.register<TestNavigationFeature>(nav)
KRelay.dispatch<TestNavigationFeature> { it.navigate("home") }
assertEquals("home", nav.currentRoute)
```

### Assertion Helpers

Simplified assertions for common checks:

```kotlin
// Registration assertions
assertRegistered<ToastFeature>()
assertNotRegistered<ToastFeature>()

// Queue assertions
assertQueueSize<ToastFeature>(3)
assertQueueEmpty<ToastFeature>()
assertQueueNotEmpty<ToastFeature>()

// Metrics assertions
assertMetrics<ToastFeature>(
    dispatches = 5L,
    queued = 2L,
    replayed = 3L
)
```

### Lifecycle Helpers

Simplify test setup/teardown:

```kotlin
// Automatic setup and cleanup
withKRelay(debugMode = true) {
    // Your test code
    KRelay.dispatch<TestFeature> { it.execute("test") }
}
// KRelay.reset() called automatically

// With metrics tracking
withMetrics {
    // Your test code
}
// Metrics reset automatically
```

### Scenario Helpers

Simulate common Android scenarios:

```kotlin
// Activity lifecycle
simulateActivityLifecycle<ToastFeature>(
    implementation = AndroidToast(),
    onCreated = {
        // Actions when created
    },
    onDestroyed = {
        // Actions when destroyed
    }
)

// Screen rotation
val toast1 = AndroidToast()
val toast2 = AndroidToast()
simulateRotation(
    oldImplementation = toast1,
    newImplementation = toast2,
    actionsDuringRotation = {
        KRelay.dispatch<ToastFeature> { it.show("Queued!") }
    }
)

// Background/Foreground
simulateBackgrounding<ToastFeature>(
    foregroundImplementation = toast1,
    actionInBackground = {
        KRelay.dispatch<ToastFeature> { it.show("Background") }
    }
)
simulateForegrounding<ToastFeature>(toast2)
```

### Queue Helpers

Manipulate queues for testing:

```kotlin
// Fill queue with actions
fillQueue<ToastFeature>(count = 10)
assertQueueSize<ToastFeature>(10)

// Fill with priority actions
fillQueueWithPriority<ToastFeature>(
    low = 2,
    normal = 3,
    high = 4,
    critical = 1
)
assertQueueSize<ToastFeature>(10)
```

### Debugging Helpers

Debug test issues:

```kotlin
// Print queue state for a feature
printQueueState<ToastFeature>()
// Output:
// === Queue State for ToastFeature ===
// Registered: true
// Pending: 3
// Metrics: {dispatches=5, queued=3, ...}

// Print all KRelay state
printAllState()
```

### Verification Helpers

End-to-end flow verification:

```kotlin
// Verify complete registration flow
verifyRegistrationFlow<ToastFeature>(
    implementation = AndroidToast(),
    expectedQueuedBefore = 2,
    actionBeforeRegister = {
        KRelay.dispatch<ToastFeature> { it.show("Test 1") }
        KRelay.dispatch<ToastFeature> { it.show("Test 2") }
    }
)

// Verify rotation preserves queued actions
verifyRotationFlow<ToastFeature>(
    beforeRotation = toast1,
    afterRotation = toast2,
    actionDuringRotation = {
        KRelay.dispatch<ToastFeature> { it.show("During rotation") }
    },
    verify = { implementation ->
        assertTrue(implementation.messages.contains("During rotation"))
    }
)
```

---

## Writing Tests

### Basic Test Template

```kotlin
package dev.brewkits.krelay

import kotlin.test.*

class MyFeatureTest {

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = true
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testMyFeature() {
        // Given
        val mock = MockTestFeature()

        // When
        KRelay.register(mock)
        KRelay.dispatch<TestFeature> { it.execute("test") }

        // Then
        assertEquals(listOf("test"), mock.executedValues)
    }
}
```

### Testing Registration Flow

```kotlin
@Test
fun testRegistration() {
    // Before registration
    assertFalse(KRelay.isRegistered<ToastFeature>())

    // Register
    val toast = AndroidToast()
    KRelay.register(toast)

    // After registration
    assertTrue(KRelay.isRegistered<ToastFeature>())

    // Unregister
    KRelay.unregister<ToastFeature>()

    // After unregister
    assertFalse(KRelay.isRegistered<ToastFeature>())
}
```

### Testing Queue Behavior

```kotlin
@Test
fun testQueueing() {
    // Dispatch without registration (queues)
    KRelay.dispatch<ToastFeature> { it.show("Message 1") }
    KRelay.dispatch<ToastFeature> { it.show("Message 2") }

    // Verify queued
    assertEquals(2, KRelay.getPendingCount<ToastFeature>())

    // Register (replays queue)
    val toast = AndroidToast()
    KRelay.register(toast)

    // Verify queue cleared and actions replayed
    assertEquals(0, KRelay.getPendingCount<ToastFeature>())
    assertTrue(toast.messages.contains("Message 1"))
    assertTrue(toast.messages.contains("Message 2"))
}
```

### Testing Priority

```kotlin
@Test
fun testPriority() {
    // Dispatch with different priorities
    KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.LOW) {
        it.show("Low priority")
    }
    KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) {
        it.show("Critical!")
    }

    // All queued
    assertEquals(2, KRelay.getPendingCount<ToastFeature>())

    // Register and verify
    val toast = AndroidToast()
    KRelay.register(toast)

    // Both messages received (order depends on priority)
    assertEquals(2, toast.messages.size)
}
```

### Testing Rotation Scenario

```kotlin
@Test
fun testRotation() {
    // Before rotation
    val toast1 = AndroidToast()
    KRelay.register<ToastFeature>(toast1)

    // Rotation starts (Activity destroyed)
    KRelay.unregister<ToastFeature>()

    // Action during rotation (queued)
    KRelay.dispatch<ToastFeature> { it.show("During rotation") }
    assertEquals(1, KRelay.getPendingCount<ToastFeature>())

    // After rotation (new Activity)
    val toast2 = AndroidToast()
    KRelay.register<ToastFeature>(toast2)

    // Verify replayed on new instance
    assertEquals(0, KRelay.getPendingCount<ToastFeature>())
    assertTrue(toast2.messages.contains("During rotation"))
}
```

### Testing Metrics

```kotlin
@Test
fun testMetrics() {
    // Reset metrics
    KRelayMetrics.reset()

    // Perform operations
    KRelay.dispatch<ToastFeature> { it.show("Test") }
    KRelay.dispatch<ToastFeature> { it.show("Test 2") }

    // Check metrics
    val metrics = KRelay.getMetrics<ToastFeature>()
    assertEquals(2L, metrics["dispatches"])
    assertEquals(2L, metrics["queued"])

    // Register and replay
    KRelay.register<ToastFeature>(AndroidToast())

    // Check updated metrics
    val updatedMetrics = KRelay.getMetrics<ToastFeature>()
    assertEquals(2L, updatedMetrics["replayed"])
}
```

---

## Demo Examples

The demo examples showcase real-world usage patterns.

### Login Flow Demo

**File**: `demo/LoginFlowDemo.kt`

Shows complete login flow with:
- Loading states
- Network calls
- Success/error handling
- Analytics tracking
- Rotation during login

**Key Tests**:
- `demo_SuccessfulLogin()` - Happy path
- `demo_FailedLogin()` - Error handling
- `demo_RotationDuringLogin()` - Rotation scenario

### Data Sync Demo

**File**: `demo/DataSyncDemo.kt`

Shows background sync with:
- Progress updates
- Priority notifications
- Background/foreground transitions
- Database operations

**Key Tests**:
- `demo_SuccessfulSync()` - Complete sync flow
- `demo_BackgroundSync_QueuesNotifications()` - Background scenario
- `demo_PriorityNotifications()` - Priority handling

### Error Handling Demo

**File**: `demo/ErrorHandlingDemo.kt`

Shows error handling patterns:
- Network errors with retry
- Validation errors
- Permission errors
- Analytics error logging

**Key Tests**:
- `demo_NetworkErrorWithRetry()` - Retry mechanism
- `demo_ValidationErrors()` - Form validation
- `demo_ErrorDuringRotation()` - Error + rotation

### Multi-Feature Coordination Demo

**File**: `demo/MultiFeatureCoordinationDemo.kt`

Shows complex workflows:
- Shopping cart checkout
- Multiple features coordinating
- Sequential operations
- Partial registration

**Key Tests**:
- `demo_CompleteCheckoutFlow_Success()` - Happy path
- `demo_RotationDuringPayment()` - Rotation during checkout
- `demo_PartialFeatureRegistration()` - Gradual registration

---

## Best Practices

### 1. Always Reset State

```kotlin
@BeforeTest
fun setup() {
    KRelay.reset()
    KRelayMetrics.reset()
}

@AfterTest
fun tearDown() {
    KRelay.reset()
    KRelayMetrics.reset()
}
```

### 2. Use Descriptive Test Names

```kotlin
// Good
@Test
fun testRotation_PreservesQueuedActions_WhenActivityDestroyed()

// Avoid
@Test
fun test1()
```

### 3. Follow Given-When-Then Pattern

```kotlin
@Test
fun testExample() {
    // Given: Setup state
    val mock = MockTestFeature()
    KRelay.register(mock)

    // When: Perform action
    KRelay.dispatch<TestFeature> { it.execute("test") }

    // Then: Verify result
    assertEquals(listOf("test"), mock.executedValues)
}
```

### 4. Test Both Happy and Error Paths

```kotlin
@Test
fun testSuccess() { /* happy path */ }

@Test
fun testFailure() { /* error path */ }
```

### 5. Use Test Utilities

```kotlin
// Instead of this:
KRelay.reset()
KRelay.debugMode = true
// ... test code ...
KRelay.reset()

// Use this:
withKRelay(debugMode = true) {
    // ... test code ...
}
```

### 6. Test Real Scenarios

Model tests after actual use cases:
- Screen rotations
- Background/foreground transitions
- Network errors
- Permission issues

### 7. Verify Metrics

```kotlin
@Test
fun testWithMetrics() {
    KRelayMetrics.reset()

    // ... operations ...

    assertMetrics<ToastFeature>(
        dispatches = 3L,
        queued = 2L
    )
}
```

### 8. Keep Tests Focused

Each test should verify one specific behavior:

```kotlin
// Good - focused test
@Test
fun testDispatch_QueuesAction_WhenNotRegistered()

// Avoid - testing multiple things
@Test
fun testEverything()
```

---

## Troubleshooting

### Tests Fail Due to Async Behavior

**Problem**: Tests expecting synchronous execution fail because `runOnMain` is async.

**Solution**: Use queue-based tests instead:

```kotlin
// Instead of testing execution directly
@Test
fun testExecution() {
    val mock = MockTestFeature()
    KRelay.register(mock)
    KRelay.dispatch<TestFeature> { it.execute("test") }
    assertEquals(listOf("test"), mock.executedValues) // May fail!
}

// Test queue behavior
@Test
fun testQueuing() {
    KRelay.dispatch<TestFeature> { it.execute("test") }
    assertEquals(1, KRelay.getPendingCount<TestFeature>())

    val mock = MockTestFeature()
    KRelay.register(mock)
    assertEquals(0, KRelay.getPendingCount<TestFeature>()) // Reliable!
}
```

### Metrics Not Updating

**Problem**: Metrics show zero despite operations.

**Solution**: Ensure metrics are not being reset:

```kotlin
@Test
fun testMetrics() {
    KRelayMetrics.reset() // Reset at start

    // Perform operations
    KRelay.dispatch<ToastFeature> { it.show("Test") }

    // Don't call reset again before checking!
    val metrics = KRelay.getMetrics<ToastFeature>()
    assertTrue(metrics["dispatches"]!! > 0)
}
```

### Queue Not Clearing After Registration

**Problem**: Queue count doesn't go to zero after registration.

**Solution**: Check if actions are actually being replayed:

```kotlin
// Verify replay happens
KRelay.dispatch<ToastFeature> { it.show("Test") }
assertEquals(1, KRelay.getPendingCount<ToastFeature>())

KRelay.register<ToastFeature>(AndroidToast())

// Queue should be cleared
assertEquals(0, KRelay.getPendingCount<ToastFeature>())
```

### Tests Interfering With Each Other

**Problem**: Tests pass individually but fail when run together.

**Solution**: Ensure proper cleanup in `@AfterTest`:

```kotlin
@AfterTest
fun tearDown() {
    KRelay.reset()
    KRelayMetrics.reset()
    // Clean any other shared state
}
```

### Priority Not Working

**Problem**: Priority actions not behaving as expected.

**Solution**: Remember priority affects queue order, not immediate execution:

```kotlin
// Priority matters when actions are queued
KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) {
    it.show("Critical")
}

// When registered, critical actions replay first
KRelay.register<ToastFeature>(toast)
```

---

## Test Coverage Goals

### Current Coverage

- **Unit Tests**: 4 files, ~30 test cases
- **Integration Tests**: 3 files, ~20 test cases
- **System Tests**: 3 files, ~20 test cases
- **Demo Examples**: 4 files, ~25 test cases

**Total**: ~95 test cases covering all major functionality

### Coverage by Feature

- ✅ Registration/Unregistration
- ✅ Dispatch (immediate and queued)
- ✅ Queue management
- ✅ Priority system
- ✅ Metrics tracking
- ✅ Weak references
- ✅ Thread safety
- ✅ Action expiry
- ✅ Screen rotation
- ✅ Background/foreground
- ✅ Error handling
- ✅ Multi-feature coordination

---

## Next Steps

### Adding New Tests

1. Determine test level (unit/integration/system/demo)
2. Create test file in appropriate package
3. Follow existing patterns
4. Use test utilities for common operations
5. Verify test passes in isolation and with full suite

### Test-Driven Development

When adding new features:

1. Write test first (it should fail)
2. Implement feature
3. Test should pass
4. Add integration/system tests
5. Create demo example if it's a major feature

### Continuous Integration

Add to CI pipeline:

```bash
# In CI script
./gradlew :krelay:test --continue
./gradlew :krelay:jacocoTestReport

# Fail if coverage < 80%
./gradlew :krelay:jacocoTestCoverageVerification
```

---

## Summary

KRelay provides:
- ✅ Comprehensive test suite (95+ tests)
- ✅ Test utilities for easy testing
- ✅ Demo examples showing real usage
- ✅ Clear testing patterns
- ✅ Full coverage of features

Use this guide to write effective tests and ensure KRelay works perfectly in your app!

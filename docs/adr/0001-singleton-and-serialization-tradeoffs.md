# ADR-0001: Singleton Architecture and Lambda Serialization Trade-offs

**Status:** Accepted (v1.0)
**Date:** 2024-01-23
**Reviewers:** Core Team
**Related Issues:** Architecture Decisions, Super App Support, Process Death Handling

---

## Context

KRelay is designed as a lightweight, zero-config bridge for dispatching UI commands from Kotlin Multiplatform shared code to platform-specific implementations. During development, we faced two critical architectural decisions:

1. **Singleton vs Instance-Based Architecture**: Should KRelay be a global singleton or support multiple instances?
2. **Queue Persistence**: Should the action queue survive Android process death?

These decisions have significant implications for:
- API simplicity vs flexibility
- Use cases in large-scale applications (Super Apps)
- Reliability guarantees
- Implementation complexity

---

## Decision

### 1. Singleton Architecture (Global `object KRelay`)

**Decision:** Use a global singleton for v1.0.

**Rationale:**

#### Pros
- **Zero Configuration**: Developers can use `KRelay.dispatch()` immediately without DI setup
- **Simple Mental Model**: One global registry, accessible anywhere in shared code
- **Reduced Boilerplate**: No need to pass KRelay instances through constructors
- **Perfect for 90% of Use Cases**: Small-to-medium apps, single-module projects
- **Faster Time-to-Market**: Teams can integrate in minutes, not hours

#### Cons
- **Super App Conflicts**: In apps with multiple independent modules (e.g., Grab, Gojek, WeChat), modules share the same singleton registry
  - **Impact**: Module A registering `ToastFeature` overwrites Module B's implementation
  - **Workaround**: Use feature namespacing (`RideModuleToastFeature`, `FoodModuleToastFeature`)
- **Testing Isolation**: Unit tests must call `KRelay.reset()` in `@BeforeTest`/`@AfterTest` to avoid state leakage
- **Multi-Tenant Apps**: Cannot have per-tenant or per-user feature implementations
- **Library Integration**: Libraries using KRelay cannot provide isolated instances to host apps

#### Real-World Example: Super App Conflict

```kotlin
// ❌ Problem: Shared singleton causes overwrites
// Ride Module (Team A)
KRelay.register<ToastFeature>(RideModuleToast(context))

// Food Module (Team B) - Overwrites Team A's implementation!
KRelay.register<ToastFeature>(FoodModuleToast(context))

// Now all Ride toasts use Food's toast style!
```

**Workaround (v1.0):**
```kotlin
// ✅ Solution: Feature namespacing
interface RideModuleToastFeature : RelayFeature {
    fun show(message: String)
}

interface FoodModuleToastFeature : RelayFeature {
    fun show(message: String)
}

// Now modules are isolated
KRelay.register<RideModuleToastFeature>(RideModuleToast(context))
KRelay.register<FoodModuleToastFeature>(FoodModuleToast(context))
```

#### Decision Factors

| Factor | Weight | Singleton Score | Instance-Based Score |
|--------|--------|-----------------|----------------------|
| API Simplicity | HIGH | 10/10 | 5/10 |
| Small App Suitability | HIGH | 10/10 | 7/10 |
| Super App Support | MEDIUM | 4/10 | 10/10 |
| Testing Ease | MEDIUM | 6/10 | 9/10 |
| DI Integration | LOW | 5/10 | 10/10 |
| Implementation Complexity | LOW | 10/10 | 6/10 |
| **Weighted Total** | - | **8.3/10** | **7.5/10** |

**Conclusion:** Singleton wins for v1.0 due to API simplicity and suitability for the majority of use cases.

---

### 2. No Queue Persistence (Lambda Functions Cannot Survive Process Death)

**Decision:** Do NOT persist the action queue to disk. Accept that queued actions are lost on process death.

**Rationale:**

#### Technical Constraints

**Lambda Serialization is NOT Possible:**
```kotlin
// This lambda captures context and cannot be serialized
KRelay.dispatch<ToastFeature> { toast ->
    toast.show(viewModel.userName) // Captures viewModel reference
}

// Attempting to serialize this lambda would require:
// 1. Serializing the lambda bytecode → Impossible with Kotlin/Native
// 2. Serializing captured variables (viewModel, userName) → Requires all objects to be @Serializable
// 3. Deserializing and reconstructing lambda on process restart → No Kotlin API supports this
```

**Why Persistence is Infeasible:**
1. **Kotlin Multiplatform**: No built-in lambda serialization in Kotlin/Native
2. **Captured Variables**: Lambdas capture ViewModels, Activities, arbitrary objects - cannot serialize
3. **Implementation Complexity**: Would require custom bytecode manipulation, massive engineering effort
4. **Limited Value**: If we serialize only data (not lambdas), users might as well use WorkManager

#### Pros of NOT Persisting Queue
- **Simple Implementation**: No disk I/O, no serialization logic
- **Clear Expectations**: Developers know KRelay is for "UI commands", not "critical operations"
- **Performance**: Zero disk overhead
- **Focus on Core Value**: KRelay excels at leak-free, sticky UI dispatch - not guaranteed execution

#### Cons of NOT Persisting Queue
- **Process Death Scenarios**: Queued actions lost when OS kills app
  - Low memory situations
  - User swipes app away
  - Developer Options: "Don't keep activities"
  - Force stop in Settings
  - OS updates
- **Cannot Use for Critical Operations**:
  - Banking transactions
  - File uploads
  - Important analytics
  - Any operation requiring guaranteed execution

#### Real-World Impact Analysis

**Scenario 1: Toast Notification (Acceptable Loss)**
```kotlin
// User logs in, ViewModel dispatches toast
viewModelScope.launch {
    val result = authService.login(user, pass)
    KRelay.dispatch<ToastFeature> { it.show("Login successful!") }
    // If process dies here, toast is lost
    // Impact: User doesn't see confirmation - ACCEPTABLE
    // Login still succeeded (server-side), user can see they're logged in
}
```

**Scenario 2: Payment Transaction (UNACCEPTABLE Loss)**
```kotlin
// ❌ WRONG: Critical operation via KRelay
KRelay.dispatch<PaymentFeature> {
    it.processPayment(1000.0)
    // If process dies, payment is LOST
    // User's money gone, no transaction record - CATASTROPHIC
}

// ✅ CORRECT: Use WorkManager for critical operations
val paymentWork = OneTimeWorkRequestBuilder<PaymentWorker>()
    .setInputData(workDataOf("amount" to 1000.0))
    .build()
WorkManager.getInstance(context).enqueue(paymentWork)
// WorkManager persists to disk, guaranteed execution
```

#### Decision Matrix: Use Cases

| Use Case | KRelay Safe? | If Lost, Impact? | Alternative |
|----------|--------------|------------------|-------------|
| Toast/Snackbar | ✅ Yes | User doesn't see feedback - Minor | None needed |
| Navigation | ✅ Yes | User stays on current screen - Acceptable | None needed |
| Haptic Feedback | ✅ Yes | No vibration - Negligible | None needed |
| Permission Request | ✅ Yes | User can retry - Acceptable | None needed |
| In-app Notification | ✅ Yes | Will refetch on next app open - Acceptable | None needed |
| Simple Analytics | ⚠️ Maybe | Event lost - Consider if acceptable | Persistent queue if critical |
| Banking/Payment | ❌ NEVER | Money lost - CATASTROPHIC | WorkManager |
| File Upload | ❌ NEVER | User data lost - Unacceptable | UploadWorker |
| Critical Analytics | ❌ NEVER | Business metrics inaccurate - Unacceptable | Room DB + WorkManager |
| Database Write | ❌ NEVER | Data lost - Unacceptable | Room/SQLite directly |

#### Considered Alternatives

**Alternative 1: Serialize Action Data (Not Lambdas)**
```kotlin
// Instead of lambdas, serialize commands
sealed class PaymentCommand {
    data class ProcessPayment(val amount: Double) : PaymentCommand()
}

// Save to disk
queueRepository.enqueue(PaymentCommand.ProcessPayment(1000.0))
```

**Why Rejected:**
- Defeats KRelay's purpose: Lambda-based API is the core value proposition
- Forces users to define sealed classes for every command - massive boilerplate
- If we're doing this, users should just use WorkManager directly

**Alternative 2: Warn Users via Annotations**
```kotlin
@ProcessDeathUnsafe // Warns at compile time
KRelay.dispatch<ToastFeature> { it.show("Hello") }
```

**Decision:** Implement this. Educate users via:
- `@ProcessDeathUnsafe` annotation with `@RequiresOptIn`
- Prominent README warnings
- ADR documentation (this document)
- Anti-patterns guide

---

## Consequences

### Positive

1. **Simple, Focused API**: KRelay remains lightweight and easy to use
2. **Clear Boundaries**: "UI commands only" is a clear mental model
3. **Zero Disk Overhead**: No I/O performance impact
4. **Fast Integration**: Developers can start using KRelay in minutes
5. **Educational Value**: Forces developers to think about operation criticality

### Negative

1. **Not Suitable for Critical Operations**: Developers must learn when NOT to use KRelay
2. **Super App Friction**: Feature namespacing workaround is verbose
3. **Testing Boilerplate**: Requires `reset()` calls in tests
4. **Documentation Burden**: Must clearly communicate limitations

### Mitigation Strategies

#### 1. Compile-Time Warnings
```kotlin
@ProcessDeathUnsafe // IDE shows warning
@SuperAppWarning    // IDE shows warning
object KRelay { ... }
```

#### 2. Comprehensive Documentation
- README: Prominent "Important Limitations" section
- ADR: This document
- Anti-patterns guide: Real-world wrong usage examples
- Code comments: Inline warnings in KRelay.kt

#### 3. Future v2.0: Instance-Based KRelay
```kotlin
// Planned API for Super Apps
val rideKRelay = KRelay.create("RideModule")
val foodKRelay = KRelay.create("FoodModule")

class RideViewModel(private val kRelay: KRelayInstance) {
    fun bookRide() {
        kRelay.dispatch<ToastFeature> { it.show("Booking...") }
    }
}
```

**Benefits:**
- Backward compatible (singleton still works)
- Solves Super App conflicts
- DI-friendly for testing
- Optional: users choose singleton or instance-based

---

## Testing Strategy

### 1. Process Death Testing

**Developer Testing:**
```bash
# Enable "Don't keep activities"
adb shell settings put global always_finish_activities 1

# Or manually kill process
adb shell am kill com.yourapp.package
```

**Unit Tests:**
```kotlin
@Test
fun `queue is lost on process death - verify with reset`() {
    KRelay.dispatch<ToastFeature> { it.show("Test") }
    assertEquals(1, KRelay.getPendingCount<ToastFeature>())

    // Simulate process death
    KRelay.reset()

    // Queue is gone
    assertEquals(0, KRelay.getPendingCount<ToastFeature>())
}
```

### 2. Singleton Isolation Testing

```kotlin
class TestA {
    @BeforeTest
    fun setup() {
        KRelay.reset() // Critical: Isolate from other tests
    }

    @Test
    fun myTest() {
        // Now isolated
    }
}
```

---

## Future Roadmap

### v1.1.0 (Near Future)
- [ ] Add `actionExpiryMs` to prevent stale queued actions
- [ ] Add `maxQueueSize` to prevent memory bloat
- [ ] Metrics API: `KRelay.getMetrics()` for monitoring

### v2.0.0 (Long-term)
- [ ] Instance-based KRelay for DI support
- [ ] Backward compatible singleton API
- [ ] Scoped queues (per-module isolation)
- [ ] Optional: Persistent queue plugin (experimental, opt-in)

**Example v2.0 API:**
```kotlin
// Option A: Factory pattern
val kRelay = KRelay.create("MyModule")

// Option B: Constructor injection (DI-friendly)
class MyViewModel(private val kRelay: KRelayInstance)

// Option C: Scoped singleton (per-module)
KRelay.scoped("RideModule").dispatch<ToastFeature> { ... }
```

---

## References

- [KRelay README](../../README.md)
- [Anti-Patterns Guide](../ANTI_PATTERNS.md)
- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
- [SavedStateHandle Guide](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
- Super App Case Studies:
  - [Grab Engineering Blog](https://engineering.grab.com/)
  - [Gojek Tech Blog](https://www.gojek.io/blog)

---

## Review History

| Date | Reviewer | Comments |
|------|----------|----------|
| 2024-01-23 | Core Team | Initial approval for v1.0 |
| 2024-01-23 | Community Feedback | Added Super App concerns, Real-world scenarios |

---

## Appendix: Super App Architecture Patterns

### Pattern 1: Feature Namespacing (Current v1.0)

```kotlin
// Each module defines its own feature interfaces
interface RideModuleToastFeature : RelayFeature
interface FoodModuleToastFeature : RelayFeature
interface PayModuleToastFeature : RelayFeature

// Each module registers independently
KRelay.register<RideModuleToastFeature>(RideToastImpl())
KRelay.register<FoodModuleToastFeature>(FoodToastImpl())
```

**Pros:**
- Works with v1.0 singleton
- Full isolation
- Zero code changes to KRelay core

**Cons:**
- Verbose
- Boilerplate
- Not intuitive

### Pattern 2: Module Wrappers (Current v1.0)

```kotlin
// Wrapper per module
object RideKRelay {
    fun dispatch<T : RelayFeature>(block: (T) -> Unit) {
        KRelay.dispatch(block) // Delegates to global singleton
    }
}

// Usage
RideKRelay.dispatch<ToastFeature> { it.show("Ride booked") }
```

**Pros:**
- Encapsulation
- Easy to migrate to v2.0 later

**Cons:**
- Still shares global registry (no real isolation)
- Misleading API (looks isolated, but isn't)

### Pattern 3: Instance-Based (Planned v2.0)

```kotlin
// True isolation with instances
val rideKRelay = KRelay.create("RideModule")
val foodKRelay = KRelay.create("FoodModule")

rideKRelay.dispatch<ToastFeature> { it.show("Ride booked") }
foodKRelay.dispatch<ToastFeature> { it.show("Order placed") }

// Each instance has independent registry
```

**Pros:**
- True isolation
- Clean API
- DI-friendly
- Testable

**Cons:**
- More complex implementation
- Requires v2.0 architecture changes

---

## Conclusion

The singleton and non-persistent queue decisions are **deliberate trade-offs**:

- **Singleton** optimizes for API simplicity and the 90% use case (small-medium apps)
- **Non-persistent queue** maintains focus on KRelay's core value: leak-free UI dispatch

For the 10% of use cases (Super Apps, critical operations), we provide:
1. Clear documentation via annotations, ADR, and guides
2. Workarounds (feature namespacing)
3. Alternative tools (WorkManager, SavedStateHandle)
4. Future roadmap (v2.0 instance-based API)

These decisions align with KRelay's philosophy: **"Do one thing and do it well."**

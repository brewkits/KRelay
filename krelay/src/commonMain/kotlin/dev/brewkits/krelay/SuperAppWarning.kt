package dev.brewkits.krelay

/**
 * Marks KRelay's Singleton architecture, which has trade-offs in large-scale applications.
 *
 * ## ⚠️ Singleton Architecture: Simplicity vs Scalability
 *
 * KRelay uses a **global singleton** (`object KRelay`), providing zero-config simplicity
 * but with important limitations in large Enterprise/Super Apps.
 *
 * ### Pros (Why Singleton is Good)
 * - ✅ **Zero Configuration**: Just `KRelay.dispatch()` - no DI setup needed
 * - ✅ **Global Access**: Available anywhere in shared code
 * - ✅ **Simple Mental Model**: One registry for all features
 * - ✅ **Perfect for Small-Medium Apps**: Most apps never hit the limitations
 *
 * ### Cons (When Singleton Becomes a Problem)
 *
 * #### 1. Super Apps with Multiple Independent Modules
 * ```kotlin
 * // Problem: All modules share the same KRelay singleton
 * // Module A
 * KRelay.register<ToastFeature>(ModuleAToastImpl())
 *
 * // Module B - OVERWRITES Module A's implementation!
 * KRelay.register<ToastFeature>(ModuleBToastImpl())
 * // Now Module A's toasts use Module B's implementation!
 * ```
 *
 * **Real-World Impact:**
 * - **Grab/Gojek-style Super Apps**: Each mini-app (Ride, Food, Pay) is independent
 * - **Module Isolation**: Modules should not affect each other
 * - **Team Autonomy**: Each team should control their own feature implementations
 *
 * #### 2. Testing Isolation
 * ```kotlin
 * // Test 1
 * @Test
 * fun testFeatureA() {
 *     KRelay.register<ToastFeature>(mockToast)
 *     // Test code...
 *     KRelay.reset() // MUST call reset!
 * }
 *
 * // Test 2 - If forgot reset(), uses Test 1's mock!
 * @Test
 * fun testFeatureB() {
 *     // ⚠️ State leaked from Test 1 if reset() was forgotten
 *     KRelay.register<ToastFeature>(anotherMock)
 * }
 * ```
 *
 * **Problem:** Singleton requires careful cleanup in tests. Forgetting `reset()` causes flaky tests.
 *
 * #### 3. Multi-Tenant Applications
 * ```kotlin
 * // Problem: Cannot have different implementations per user/tenant
 * KRelay.register<PaymentFeature>(paymentImpl)
 * // All users share the same implementation - cannot customize per tenant
 * ```
 *
 * ## Current Workarounds
 *
 * ### Workaround 1: Feature Namespacing (Recommended for Super Apps)
 * ```kotlin
 * // Instead of generic ToastFeature:
 * interface ToastFeature : RelayFeature { ... }
 *
 * // Use module-specific features:
 * interface RideModuleToastFeature : RelayFeature {
 *     fun show(message: String)
 * }
 *
 * interface FoodModuleToastFeature : RelayFeature {
 *     fun show(message: String)
 * }
 *
 * // Now each module has its own implementation:
 * KRelay.register<RideModuleToastFeature>(RideToastImpl())
 * KRelay.register<FoodModuleToastFeature>(FoodToastImpl())
 * ```
 *
 * **Pros:**
 * - ✅ Works with current v1.0 singleton
 * - ✅ Full module isolation
 * - ✅ No code changes needed
 *
 * **Cons:**
 * - ❌ Verbose interface names
 * - ❌ Boilerplate for each module
 * - ❌ Doesn't feel natural
 *
 * ### Workaround 2: Testing with `@BeforeTest` and `@AfterTest`
 * ```kotlin
 * class MyTest {
 *     @BeforeTest
 *     fun setup() {
 *         KRelay.reset() // Always start fresh
 *     }
 *
 *     @AfterTest
 *     fun teardown() {
 *         KRelay.reset() // Clean up for next test
 *     }
 *
 *     @Test
 *     fun myTest() {
 *         // Now fully isolated
 *     }
 * }
 * ```
 *
 * ## Future Solution: Instance-Based KRelay (v2.0 Planned)
 *
 * **Proposed API Design:**
 * ```kotlin
 * // Option 1: Manual instance creation
 * val rideModuleKRelay = KRelay.create("RideModule")
 * rideModuleKRelay.dispatch<ToastFeature> { it.show("Ride booked!") }
 *
 * val foodModuleKRelay = KRelay.create("FoodModule")
 * foodModuleKRelay.dispatch<ToastFeature> { it.show("Order placed!") }
 *
 * // Option 2: DI-friendly
 * class RideViewModel(private val kRelay: KRelayInstance) {
 *     fun bookRide() {
 *         kRelay.dispatch<ToastFeature> { it.show("Booking...") }
 *     }
 * }
 *
 * // DI Setup
 * single { KRelay.create("RideModule") }
 * viewModel { RideViewModel(get()) }
 * ```
 *
 * **Benefits:**
 * - ✅ True module isolation
 * - ✅ Testability with DI
 * - ✅ Multi-tenant support
 * - ✅ Backward compatible (singleton still available)
 *
 * ## Decision Guide
 *
 * | App Type | Use Singleton? | Recommended Approach |
 * |----------|---------------|----------------------|
 * | Small-Medium App | ✅ Yes | Current singleton is perfect |
 * | Single-Module App | ✅ Yes | No concerns |
 * | Super App (Grab/Gojek style) | ⚠️ With Caution | Use Feature Namespacing |
 * | Multi-Tenant SaaS | ❌ Problematic | Wait for v2.0 or fork |
 * | Library (used by others) | ❌ Problematic | Expose DI-friendly API |
 *
 * ## When to Be Concerned
 *
 * Ask yourself:
 * - ❓ Do I have 5+ independent feature modules?
 * - ❓ Do modules need different Toast/Navigation implementations?
 * - ❓ Am I building a white-label app with per-client customization?
 * - ❓ Do I need to mock KRelay in 100+ unit tests?
 *
 * If **YES to 2+**, consider:
 * 1. Use Feature Namespacing workaround
 * 2. Wait for v2.0 instance-based API
 * 3. Contribute to v2.0 development!
 *
 * ## Summary
 *
 * - ✅ **For most apps**: Singleton is simple, reliable, zero-config
 * - ⚠️ **For Super Apps**: Use feature namespacing or wait for v2.0
 * - ❌ **For libraries**: Avoid exposing KRelay singleton to consumers
 * - 🔜 **v2.0 Solution**: Instance-based KRelay with DI support
 *
 * @see [ADR-0001](../../docs/adr/0001-singleton-and-serialization-tradeoffs.md) for detailed analysis
 */
@RequiresOptIn(
    message = "KRelay uses a global singleton. In Super Apps with multiple modules, " +
            "use Feature Namespacing (e.g., RideModuleToastFeature) to avoid conflicts. " +
            "See @SuperAppWarning documentation for workarounds.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SuperAppWarning

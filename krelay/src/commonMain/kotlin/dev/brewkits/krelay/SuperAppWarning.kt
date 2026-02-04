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
 * ## ✅ v2.0 Solution: Instance-Based KRelay (NOW AVAILABLE)
 *
 * **KRelay v2.0 introduces instance-based API for Super Apps:**
 * ```kotlin
 * // Option 1: Manual instance creation
 * val rideModuleKRelay = KRelay.create("RideModule")
 * rideModuleKRelay.register<ToastFeature>(RideToastImpl())
 * rideModuleKRelay.dispatch<ToastFeature> { it.show("Ride booked!") }
 *
 * val foodModuleKRelay = KRelay.create("FoodModule")
 * foodModuleKRelay.register<ToastFeature>(FoodToastImpl())
 * foodModuleKRelay.dispatch<ToastFeature> { it.show("Order placed!") }
 *
 * // Option 2: DI-friendly (recommended for large apps)
 * class RideViewModel(private val kRelay: KRelayInstance) {
 *     fun bookRide() {
 *         kRelay.dispatch<ToastFeature> { it.show("Booking...") }
 *     }
 * }
 *
 * // Koin DI Setup
 * val rideModule = module {
 *     single { KRelay.create("RideModule") }
 *     viewModel { RideViewModel(get()) }
 * }
 *
 * // Option 3: Builder pattern for custom configuration
 * val instance = KRelay.builder("MyModule")
 *     .maxQueueSize(50)
 *     .actionExpiry(60_000L)
 *     .debugMode(true)
 *     .build()
 * ```
 *
 * **Benefits:**
 * - ✅ True module isolation (each instance has separate registry)
 * - ✅ Testability with DI (easy to mock instances)
 * - ✅ Multi-tenant support (different instances per tenant)
 * - ✅ 100% Backward compatible (singleton still works)
 * - ✅ Per-instance configuration (different queue sizes, expiry times)
 *
 * ## Decision Guide
 *
 * | App Type | Use Singleton? | Recommended Approach |
 * |----------|---------------|----------------------|
 * | Small-Medium App | ✅ Yes | Current singleton is perfect |
 * | Single-Module App | ✅ Yes | No concerns |
 * | Super App (Grab/Gojek style) | ✅ v2.0 Instances | Use `KRelay.create("ModuleName")` per module |
 * | Multi-Tenant SaaS | ✅ v2.0 Instances | Use `KRelay.create("Tenant_${id}")` per tenant |
 * | Library (used by others) | ✅ v2.0 Instances | Accept `KRelayInstance` parameter in DI |
 *
 * ## When to Use v2.0 Instances
 *
 * Ask yourself:
 * - ❓ Do I have 5+ independent feature modules?
 * - ❓ Do modules need different Toast/Navigation implementations?
 * - ❓ Am I building a white-label app with per-client customization?
 * - ❓ Do I need to mock KRelay in 100+ unit tests?
 *
 * If **YES to 2+**, use v2.0 instances:
 * 1. ✅ **Preferred**: Use `KRelay.create("ModuleName")` for isolated instances
 * 2. ✅ **DI Integration**: Inject `KRelayInstance` into ViewModels/UseCases
 * 3. ✅ **Testing**: Each test gets its own instance (no `reset()` needed)
 * 4. ⚠️ **Fallback**: Feature Namespacing still works if you prefer singleton
 *
 * ## Summary
 *
 * - ✅ **For most apps**: Singleton is simple, reliable, zero-config
 * - ✅ **For Super Apps**: Use v2.0 instances via `KRelay.create("ModuleName")`
 * - ✅ **For libraries**: Accept `KRelayInstance` parameter in DI (v2.0)
 * - ✅ **For testing**: v2.0 instances eliminate need for `reset()` calls
 * - ✅ **100% Backward Compatible**: Existing singleton code works unchanged
 *
 * @see [ADR-0001](../../docs/adr/0001-singleton-and-serialization-tradeoffs.md) for detailed analysis
 */
@RequiresOptIn(
    message = "KRelay uses a global singleton. For Super Apps, use KRelay.create() for per-module instances. " +
            "See @SuperAppWarning and KRelayInstance documentation for details.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SuperAppWarning

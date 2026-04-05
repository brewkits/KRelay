package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * Helper functions to bridge Kotlin reflection to Swift.
 *
 * Swift cannot directly access Kotlin's `reified` type parameters or `::class`,
 * so we provide explicit functions.
 *
 * ## Pattern A — iOS-only apps (iOS dispatches AND registers)
 *
 * Use the concrete KClass of the implementation consistently:
 * ```swift
 * // Register with the concrete KClass (via getKClass helper)
 * // The Swift extension handles this automatically — just call:
 * KRelay.shared.register(myToastImpl)
 *
 * // Dispatch using the SAME concrete type
 * KRelay.shared.dispatch(MyToastImpl.self) { $0.show("Hello") }
 * ```
 *
 * ## Pattern B — KMP apps (Kotlin dispatches, iOS registers)
 *
 * Kotlin dispatches using the interface KClass (`ToastFeature::class`).
 * iOS must register under the same interface KClass. Export a Kotlin helper:
 *
 * ```kotlin
 * // In your shared Kotlin code:
 * fun toastFeatureClass() = ToastFeature::class
 * ```
 *
 * Then from Swift:
 * ```swift
 * KRelayIosHelperKt.registerFeature(
 *     instance: KRelay.shared.defaultInstance,
 *     kClass:   YourSharedKt.toastFeatureClass(),
 *     impl:     self
 * )
 * ```
 */

/**
 * Gets the KClass for a given object instance (returns the concrete class).
 */
fun getKClass(obj: Any): KClass<*> = obj::class

/**
 * Registers [impl] under the provided [kClass] key on the given [instance].
 *
 * This is the correct helper for **KMP apps** where Kotlin dispatches using the
 * *interface* KClass (`ToastFeature::class`) and iOS needs to register under
 * the same key.
 *
 * Export a Kotlin helper that returns the interface KClass:
 * ```kotlin
 * fun toastFeatureClass() = ToastFeature::class
 * ```
 *
 * Then call from Swift:
 * ```swift
 * KRelayIosHelperKt.registerFeature(
 *     instance: KRelay.shared.defaultInstance,
 *     kClass:   YourSharedKt.toastFeatureClass(),
 *     impl:     self
 * )
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun registerFeature(
    instance: KRelayInstance,
    kClass: KClass<out RelayFeature>,
    impl: RelayFeature
) {
    // Validation: ensure the implementation actually matches the interface
    if (!kClass.isInstance(impl)) {
        val errorMsg = "❌ [KRelay] Registration error: ${impl::class.simpleName} does not implement ${kClass.simpleName}. " +
            "Ensure you are passing the correct interface KClass."
        if (instance.debugMode) {
            error(errorMsg)  // crash early in debug to surface developer mistakes immediately
        } else {
            println("[KRelay] WARNING: $errorMsg")  // always log in release; never silently ignore
        }
        return
    }

    instance.register(kClass as KClass<RelayFeature>, impl)
}

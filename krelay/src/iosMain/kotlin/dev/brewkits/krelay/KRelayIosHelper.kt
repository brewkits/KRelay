package dev.brewkits.krelay

import kotlin.reflect.KClass

/**
 * Helper functions to bridge Kotlin reflection to Swift.
 *
 * Swift cannot directly access Kotlin's `reified` type parameters or `::class`,
 * so we provide explicit functions.
 *
 * Usage in Swift:
 * ```swift
 * // Get KClass from instance
 * let kClass = KRelayIosHelperKt.getKClass(for: myToastImpl)
 * KRelay.shared.registerInternal(impl: myToastImpl, kClass: kClass)
 *
 * // Get KClass from type
 * let kClass = KRelayIosHelperKt.getKClassForType(MyToastFeature.self)
 * KRelay.shared.unregisterInternal(kClass: kClass)
 * ```
 */

/**
 * Gets the KClass for a given object instance.
 */
fun getKClass(obj: Any): KClass<*> = obj::class

/**
 * Gets the KClass for a protocol/interface type (Swift metatype).
 *
 * Note: This requires the type to have at least one implementation.
 * For protocols with no instances, this cannot work due to Swift/Kotlin interop limitations.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> getKClassForType(instance: T): KClass<*> = instance::class

package dev.brewkits.krelay

import dev.brewkits.krelay.samples.NavigationFeature
import dev.brewkits.krelay.samples.ToastFeature
import kotlin.reflect.KClass

/**
 * Kotlin KClass helpers for iOS Swift interop.
 *
 * Swift cannot obtain the KClass of a Kotlin *interface* directly from a Swift metatype
 * (`ToastFeature.self` is a Swift metatype, not a KotlinKClass). These helpers export
 * the interface KClass so that iOS code can register implementations under the correct
 * key — matching the KClass used by Kotlin `dispatch<ToastFeature>` calls.
 *
 * ## Usage in Swift
 * ```swift
 * import ComposeApp
 * import Krelay
 *
 * let kClass = KRelayKClassHelpersKt.toastFeatureKClass()
 * KRelayIosHelperKt.registerFeature(
 *     instance: KRelay.shared.instance,
 *     kClass:   kClass,
 *     impl:     myToastImpl
 * )
 * ```
 */
fun toastFeatureKClass(): KClass<ToastFeature> = ToastFeature::class

fun navigationFeatureKClass(): KClass<NavigationFeature> = NavigationFeature::class

package dev.brewkits.krelay

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference as NativeWeakReference

/**
 * iOS implementation of WeakRef using Kotlin Native WeakReference.
 *
 * This ensures that ViewController implementations won't cause memory leaks
 * when they are destroyed but the shared Kotlin code still holds references.
 */
@OptIn(ExperimentalNativeApi::class)
actual class WeakRef<T : Any> actual constructor(referred: T) {
    private val weakReference = NativeWeakReference(referred)

    actual fun get(): T? = weakReference.get()

    actual fun clear() {
        weakReference.clear()
    }
}

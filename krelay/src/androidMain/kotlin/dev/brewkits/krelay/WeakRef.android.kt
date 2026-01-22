package dev.brewkits.krelay

import java.lang.ref.WeakReference as JavaWeakReference

/**
 * Android implementation of WeakRef using java.lang.ref.WeakReference.
 *
 * This ensures that Activity/Fragment implementations won't cause memory leaks
 * when they are destroyed but the shared Kotlin code still holds references.
 */
actual class WeakRef<T : Any> actual constructor(referred: T) {
    private val weakReference = JavaWeakReference(referred)

    actual fun get(): T? = weakReference.get()

    actual fun clear() = weakReference.clear()
}

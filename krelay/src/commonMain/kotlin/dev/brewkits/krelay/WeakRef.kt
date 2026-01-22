package dev.brewkits.krelay

/**
 * Platform-agnostic weak reference wrapper.
 *
 * This class allows KRelay to hold references to platform implementations
 * without causing memory leaks when Activities/ViewControllers are destroyed.
 *
 * Platform implementations:
 * - Android: Uses java.lang.ref.WeakReference
 * - iOS: Uses Kotlin Native WeakReference
 */
expect class WeakRef<T : Any>(referred: T) {
    /**
     * Returns the referenced object if it's still alive, null otherwise.
     */
    fun get(): T?

    /**
     * Clears the reference.
     */
    fun clear()
}

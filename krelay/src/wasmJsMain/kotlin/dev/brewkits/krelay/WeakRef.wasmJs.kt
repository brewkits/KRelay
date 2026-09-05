package dev.brewkits.krelay

/**
 * For WasmJS, we currently fallback to a strong reference that can be cleared manually,
 * as stable cross-platform WeakRef isn't universally available without compiler flags.
 */
actual class WeakRef<T : Any> actual constructor(referred: T) {
    private var ref: T? = referred

    actual fun get(): T? = ref

    actual fun clear() {
        ref = null
    }
}

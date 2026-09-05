package dev.brewkits.krelay

import java.lang.ref.WeakReference

actual class WeakRef<T : Any> actual constructor(referred: T) {
    private val ref = WeakReference(referred)
    actual fun get(): T? = ref.get()
    actual fun clear() {
        ref.clear()
    }
}

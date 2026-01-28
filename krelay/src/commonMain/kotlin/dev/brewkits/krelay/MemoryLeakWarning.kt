package dev.brewkits.krelay

/**
 * Marks APIs that can cause memory leaks if lambdas capture heavy objects.
 *
 * ## Problem: Lambda Capture Leaks
 *
 * When you dispatch an action:
 * ```kotlin
 * KRelay.dispatch<ToastFeature> { it.show(viewModel.data) }
 * ```
 * The lambda captures `viewModel`. If the queue holds this for too long
 * (e.g., user backgrounds app and never returns), the entire ViewModel
 * and any Context it holds cannot be garbage collected.
 *
 * ## Solutions:
 *
 * ### 1. Use Primitive Captures (Best)
 * ```kotlin
 * // ✅ Good: Capture only the data, not ViewModel
 * val message = viewModel.data
 * KRelay.dispatch<ToastFeature> { it.show(message) }
 * ```
 *
 * ### 2. Call clearQueue() in onCleared()
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     override fun onCleared() {
 *         super.onCleared()
 *         KRelay.clearQueue<ToastFeature>()  // Release lambdas
 *     }
 * }
 * ```
 *
 * ### 3. Trust actionExpiryMs (Default: 5 minutes)
 * Actions expire automatically. For most UI feedback, this is sufficient.
 *
 * ## Not a Problem For:
 * - Short-lived objects (primitives, strings, data classes)
 * - Features with high registration frequency (Activity-based)
 *
 * @see ProcessDeathUnsafe
 */
@RequiresOptIn(
    message = "This API can cause memory leaks if lambdas capture ViewModels or Contexts. " +
              "Capture only primitive data, or call clearQueue() in onCleared().",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class MemoryLeakWarning

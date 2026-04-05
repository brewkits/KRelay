package dev.brewkits.krelay.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import dev.brewkits.krelay.RelayFeature
import dev.brewkits.krelay.register
import dev.brewkits.krelay.unregister

/**
 * Registers a KRelay feature implementation scoped to the composition.
 * Automatically calls [KRelayInstance.unregister] when the composition leaves.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val context = LocalContext.current
 *
 *     KRelayEffect<ToastFeature> {
 *         object : ToastFeature {
 *             override fun show(message: String) =
 *                 Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
 *         }
 *     }
 *
 *     HomeContent()
 * }
 * ```
 *
 * @param instance The KRelayInstance to register on. Defaults to the global [KRelay] singleton.
 * @param factory  Produces the feature implementation. Called once and remembered.
 */
@Composable
inline fun <reified T : RelayFeature> KRelayEffect(
    instance: KRelayInstance = KRelay.instance,
    crossinline factory: () -> T
) {
    val impl = remember { factory() }
    DisposableEffect(impl, instance) {
        instance.register<T>(impl)
        onDispose { instance.unregister<T>(impl) }
    }
}

/**
 * Registers a KRelay feature and returns the implementation for further use.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val snackbarHostState = remember { SnackbarHostState() }
 *     val scope = rememberCoroutineScope()
 *
 *     rememberKRelayImpl<ToastFeature> {
 *         object : ToastFeature {
 *             override fun show(message: String) {
 *                 scope.launch { snackbarHostState.showSnackbar(message) }
 *             }
 *         }
 *     }
 *
 *     Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { ... }
 * }
 * ```
 *
 * @return The remembered implementation instance.
 */
@Composable
inline fun <reified T : RelayFeature> rememberKRelayImpl(
    instance: KRelayInstance = KRelay.instance,
    crossinline factory: () -> T
): T {
    val impl = remember { factory() }
    DisposableEffect(impl, instance) {
        instance.register<T>(impl)
        onDispose { instance.unregister<T>(impl) }
    }
    return impl
}

package dev.brewkits.krelay.samples

/**
 * Optional Flow/Coroutines adapter for KRelay.
 *
 * ## Requirements
 * Add to your module's `build.gradle.kts`:
 * ```kotlin
 * commonMain.dependencies {
 *     implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
 * }
 * ```
 *
 * ## Usage
 *
 * ### Converting a KRelay dispatch to a Flow
 * Instead of fire-and-forget dispatch, expose a SharedFlow from your ViewModel:
 *
 * ```kotlin
 * import kotlinx.coroutines.flow.MutableSharedFlow
 * import kotlinx.coroutines.flow.SharedFlow
 * import kotlinx.coroutines.flow.asSharedFlow
 *
 * class LoginViewModel : ViewModel() {
 *     private val _toastFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
 *     val toastFlow: SharedFlow<String> = _toastFlow.asSharedFlow()
 *
 *     fun onLoginSuccess() {
 *         viewModelScope.launch {
 *             _toastFlow.emit("Welcome!")
 *         }
 *     }
 * }
 * ```
 *
 * Then in your platform layer, collect the flow and bridge to KRelay:
 *
 * ```kotlin
 * // Android Activity
 * lifecycleScope.launch {
 *     repeatOnLifecycle(Lifecycle.State.STARTED) {
 *         viewModel.toastFlow.collect { message ->
 *             KRelay.dispatch<ToastFeature> { it.show(message) }
 *         }
 *     }
 * }
 * ```
 *
 * ### KRelay as a bridge for platform events → shared code
 * For platform → ViewModel direction, use a StateFlow/SharedFlow in the ViewModel
 * and send events from platform code:
 *
 * ```kotlin
 * // In shared ViewModel
 * class PermissionViewModel : ViewModel() {
 *     private val _permissionResult = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
 *     val permissionResult: SharedFlow<Boolean> = _permissionResult.asSharedFlow()
 *
 *     fun onPermissionGranted() {
 *         viewModelScope.launch { _permissionResult.emit(true) }
 *     }
 * }
 *
 * // In platform code (after user grants permission)
 * viewModel.onPermissionGranted()
 * ```
 *
 * ### When to use KRelay vs Flow directly
 *
 * | Scenario | Use |
 * |----------|-----|
 * | ViewModel → platform one-way command | KRelay (sticky queue, weak ref) |
 * | ViewModel → Compose UI state | StateFlow/MutableState |
 * | Platform → ViewModel event | Direct function call or Channel |
 * | ViewModel → ViewModel communication | SharedFlow |
 * | Backpressure-sensitive stream | Channel with BufferOverflow policy |
 *
 * KRelay shines specifically for **ViewModel → native platform bridge** where:
 * - The platform implementation may not be alive when the command is issued
 * - Automatic main thread dispatch is needed
 * - Memory safety (weak refs) is required without manual management
 */
object KRelayFlowAdapterDocs

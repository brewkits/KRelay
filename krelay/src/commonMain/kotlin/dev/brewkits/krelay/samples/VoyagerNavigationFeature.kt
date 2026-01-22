package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Sample Navigation Feature for Voyager Integration.
 *
 * This interface demonstrates how to create a navigation feature
 * that works with Voyager (or any other navigation library).
 *
 * # The Pattern:
 *
 * 1. **Define Feature (commonMain)**: This interface
 * 2. **Use from ViewModel (commonMain)**: `KRelay.dispatch<VoyagerNavigationFeature> { it.goToHome() }`
 * 3. **Implement with Voyager (Platform)**: `class VoyagerNavImpl(navigator: Navigator) : VoyagerNavigationFeature`
 * 4. **Register at App Root**: `KRelay.register(VoyagerNavImpl(navigator))`
 *
 * # Benefits:
 *
 * - **Testable**: ViewModels don't depend on Voyager Navigator
 * - **Flexible**: Switch to Decompose? Just write new implementation
 * - **Clean**: Business logic is decoupled from navigation framework
 *
 * # Example Usage:
 *
 * ```kotlin
 * // ViewModel (commonMain) - Zero dependencies!
 * class LoginViewModel {
 *     fun onLoginSuccess() {
 *         KRelay.dispatch<VoyagerNavigationFeature> {
 *             it.goToHome()
 *         }
 *     }
 * }
 *
 * // Voyager Implementation (Platform code)
 * class VoyagerNavImpl(private val navigator: Navigator) : VoyagerNavigationFeature {
 *     override fun goToHome() {
 *         navigator.replaceAll(HomeScreen())
 *     }
 *
 *     override fun goToProfile(userId: String) {
 *         navigator.push(ProfileScreen(userId))
 *     }
 *
 *     override fun goBack() {
 *         navigator.pop()
 *     }
 * }
 *
 * // App Root (Compose)
 * @Composable
 * fun App() {
 *     Navigator(LoginScreen()) { navigator ->
 *         LaunchedEffect(navigator) {
 *             KRelay.register(VoyagerNavImpl(navigator))
 *         }
 *         CurrentScreen()
 *     }
 * }
 * ```
 */
interface VoyagerNavigationFeature : RelayFeature {
    /**
     * Navigate to home screen, clearing back stack.
     */
    fun goToHome()

    /**
     * Navigate to user profile screen.
     *
     * @param userId The user ID to display
     */
    fun goToProfile(userId: String)

    /**
     * Navigate to settings screen.
     */
    fun goToSettings()

    /**
     * Navigate back (pop current screen).
     */
    fun goBack()

    /**
     * Navigate to login screen, clearing back stack.
     * Useful for logout flow.
     */
    fun goToLogin()
}

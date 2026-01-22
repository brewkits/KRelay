package dev.brewkits.krelay.samples

import dev.brewkits.krelay.KRelay

/**
 * Demo ViewModel showing Voyager integration with KRelay.
 *
 * This ViewModel demonstrates:
 * 1. Zero dependencies on Voyager Navigator
 * 2. Pure business logic
 * 3. Easy to test (no mocking needed)
 *
 * # The Power of Decoupling:
 *
 * **Before KRelay:**
 * ```kotlin
 * class LoginViewModel(private val navigator: Navigator) {
 *     fun onLoginSuccess() {
 *         navigator.push(HomeScreen())
 *     }
 * }
 * ```
 * Problems:
 * - Coupled to Voyager
 * - Hard to test (need to mock Navigator)
 * - Can't reuse if you switch navigation libraries
 *
 * **With KRelay:**
 * ```kotlin
 * class LoginViewModel {
 *     fun onLoginSuccess() {
 *         KRelay.dispatch<VoyagerNavigationFeature> { it.goToHome() }
 *     }
 * }
 * ```
 * Benefits:
 * - Zero dependencies
 * - Easy to test (simple mock interface)
 * - Switch to Decompose? Just rewrite the implementation!
 */
class VoyagerDemoViewModel {

    /**
     * Simulates a successful login flow.
     * In real app, this would call AuthRepository, validate credentials, etc.
     */
    fun onLoginSuccess(username: String) {
        // Business logic here (e.g., save auth token)
        println("✅ Login successful for user: $username")

        // Navigate to home - Fire and Forget!
        KRelay.dispatch<VoyagerNavigationFeature> {
            it.goToHome()
        }

        // No need to:
        // - Hold Navigator reference
        // - Worry about lifecycle
        // - Handle threading
        // KRelay does it all! 🚀
    }

    /**
     * Handles user profile view request.
     */
    fun onViewProfile(userId: String) {
        println("👤 Viewing profile for user: $userId")

        KRelay.dispatch<VoyagerNavigationFeature> {
            it.goToProfile(userId)
        }
    }

    /**
     * Handles settings navigation.
     */
    fun onOpenSettings() {
        println("⚙️ Opening settings...")

        KRelay.dispatch<VoyagerNavigationFeature> {
            it.goToSettings()
        }
    }

    /**
     * Handles back navigation.
     */
    fun onBackPressed() {
        println("⬅️ Going back...")

        KRelay.dispatch<VoyagerNavigationFeature> {
            it.goBack()
        }
    }

    /**
     * Handles logout flow.
     * Clears user data and navigates back to login.
     */
    fun onLogout() {
        // Business logic: Clear auth token, user data, etc.
        println("👋 Logging out...")

        // Navigate to login (clearing back stack)
        KRelay.dispatch<VoyagerNavigationFeature> {
            it.goToLogin()
        }
    }

    /**
     * Example: Complex business logic flow with multiple navigation commands.
     *
     * This demonstrates how KRelay makes complex flows simple:
     * - Load data
     * - Show loading state
     * - On success: navigate to profile
     * - On error: stay on current screen
     */
    suspend fun loadUserDataAndNavigate(userId: String) {
        try {
            println("⏳ Loading user data for $userId...")

            // Simulate network call
            delay(1000)

            // Success: Navigate to profile
            KRelay.dispatch<VoyagerNavigationFeature> {
                it.goToProfile(userId)
            }

            // Also show success toast (optional - requires ToastFeature)
            // KRelay.dispatch<ToastFeature> {
            //     it.show("Profile loaded!")
            // }
        } catch (e: Exception) {
            // Error: Stay on current screen, show error (optional - requires ToastFeature)
            // KRelay.dispatch<ToastFeature> {
            //     it.show("Failed to load profile: ${e.message}")
            // }
        }
    }
}

// Fake delay for demo
private suspend fun delay(ms: Long) {
    // In real app, use kotlinx.coroutines.delay
    println("Simulating ${ms}ms delay...")
}

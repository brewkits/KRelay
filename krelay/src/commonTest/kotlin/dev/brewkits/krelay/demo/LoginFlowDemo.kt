package dev.brewkits.krelay.demo

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Advanced Demo: Complete Login Flow
 *
 * Demonstrates:
 * 1. User interaction triggers ViewModel action
 * 2. ViewModel dispatches loading state
 * 3. Network call (simulated)
 * 4. Success: Show toast + navigate
 * 5. Error: Show error toast
 * 6. Activity rotation during loading (queuing works!)
 */
class LoginFlowDemo {

    // === FEATURES ===

    interface ToastFeature : RelayFeature {
        fun showSuccess(message: String)
        fun showError(message: String)
    }

    interface NavigationFeature : RelayFeature {
        fun navigateToHome()
        fun navigateToForgotPassword()
    }

    interface LoadingFeature : RelayFeature {
        fun showLoading(visible: Boolean)
    }

    interface AnalyticsFeature : RelayFeature {
        fun trackEvent(event: String, properties: Map<String, Any>)
    }

    // === IMPLEMENTATIONS ===

    class AndroidToast : ToastFeature {
        val messages = mutableListOf<Pair<String, String>>() // (type, message)

        override fun showSuccess(message: String) {
            messages.add("success" to message)
        }

        override fun showError(message: String) {
            messages.add("error" to message)
        }
    }

    class AndroidNavigation : NavigationFeature {
        var currentScreen = "login"

        override fun navigateToHome() {
            currentScreen = "home"
        }

        override fun navigateToForgotPassword() {
            currentScreen = "forgot_password"
        }
    }

    class AndroidLoadingDialog : LoadingFeature {
        var isShowing = false

        override fun showLoading(visible: Boolean) {
            isShowing = visible
        }
    }

    class FirebaseAnalytics : AnalyticsFeature {
        val events = mutableListOf<Pair<String, Map<String, Any>>>()

        override fun trackEvent(event: String, properties: Map<String, Any>) {
            events.add(event to properties)
        }
    }

    // === VIEW MODEL ===

    class LoginViewModel {
        fun login(email: String, password: String) {
            // Step 1: Show loading
            KRelay.dispatch<LoadingFeature> { it.showLoading(true) }

            // Step 2: Track analytics
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("login_attempt", mapOf("email" to email))
            }

            // Step 3: Simulate network call (in real app, this is async)
            val success = simulateNetworkCall(email, password)

            // Step 4: Hide loading
            KRelay.dispatch<LoadingFeature> { it.showLoading(false) }

            // Step 5: Handle result
            if (success) {
                KRelay.dispatch<ToastFeature> { it.showSuccess("Login successful!") }
                KRelay.dispatch<NavigationFeature> { it.navigateToHome() }
                KRelay.dispatch<AnalyticsFeature> {
                    it.trackEvent("login_success", mapOf("email" to email))
                }
            } else {
                KRelay.dispatch<ToastFeature> { it.showError("Invalid credentials") }
                KRelay.dispatch<AnalyticsFeature> {
                    it.trackEvent("login_failed", mapOf("email" to email))
                }
            }
        }

        fun forgotPassword() {
            KRelay.dispatch<NavigationFeature> { it.navigateToForgotPassword() }
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("forgot_password_clicked", emptyMap())
            }
        }

        private fun simulateNetworkCall(email: String, password: String): Boolean {
            // Simulate success if email contains "test"
            return email.contains("test")
        }
    }

    // === TESTS (DOUBLES AS DEMO) ===

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = true
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun demo_SuccessfulLogin() {
        // === SETUP: Activity created ===
        val toast = AndroidToast()
        val navigation = AndroidNavigation()
        val loading = AndroidLoadingDialog()
        val analytics = FirebaseAnalytics()

        KRelay.register<ToastFeature>(toast)
        KRelay.register<NavigationFeature>(navigation)
        KRelay.register<LoadingFeature>(loading)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION: User taps Login ===
        val viewModel = LoginViewModel()
        viewModel.login("test@example.com", "password123")

        // === VERIFY: Complete flow executed ===
        assertEquals("home", navigation.currentScreen)
        assertTrue(toast.messages.any { it.first == "success" })
        assertFalse(loading.isShowing)
        assertTrue(analytics.events.any { it.first == "login_success" })
    }

    @Test
    fun demo_FailedLogin() {
        // === SETUP ===
        val toast = AndroidToast()
        val analytics = FirebaseAnalytics()

        KRelay.register<ToastFeature>(toast)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION ===
        val viewModel = LoginViewModel()
        viewModel.login("wrong@example.com", "wrongpass")

        // === VERIFY ===
        assertTrue(toast.messages.any { it.first == "error" })
        assertTrue(analytics.events.any { it.first == "login_failed" })
    }

    @Test
    fun demo_RotationDuringLogin() {
        // === SCENARIO: User rotates device during login ===

        // Step 1: Activity created, user starts login
        val toast1 = AndroidToast()
        KRelay.register<ToastFeature>(toast1)

        val viewModel = LoginViewModel()

        // Step 2: Unregister (simulate rotation)
        KRelay.unregister<ToastFeature>()
        KRelay.unregister<NavigationFeature>()
        KRelay.unregister<LoadingFeature>()
        KRelay.unregister<AnalyticsFeature>()

        // Step 3: Login completes while Activity destroyed (QUEUED!)
        viewModel.login("test@example.com", "password123")

        // Verify queued
        assertTrue(KRelay.getPendingCount<ToastFeature>() > 0)
        assertTrue(KRelay.getPendingCount<NavigationFeature>() > 0)

        // Step 4: New Activity created after rotation
        val toast2 = AndroidToast()
        val navigation2 = AndroidNavigation()
        val loading2 = AndroidLoadingDialog()
        val analytics2 = FirebaseAnalytics()

        KRelay.register<ToastFeature>(toast2)
        KRelay.register<NavigationFeature>(navigation2)
        KRelay.register<LoadingFeature>(loading2)
        KRelay.register<AnalyticsFeature>(analytics2)

        // Step 5: Queued actions replayed on NEW instances
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())
        assertEquals("home", navigation2.currentScreen)
        assertTrue(toast2.messages.any { it.first == "success" })
    }

    @Test
    fun demo_ForgotPasswordFlow() {
        // === SETUP ===
        val navigation = AndroidNavigation()
        val analytics = FirebaseAnalytics()

        KRelay.register<NavigationFeature>(navigation)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION ===
        val viewModel = LoginViewModel()
        viewModel.forgotPassword()

        // === VERIFY ===
        assertEquals("forgot_password", navigation.currentScreen)
        assertTrue(analytics.events.any { it.first == "forgot_password_clicked" })
    }

    @Test
    fun demo_MultipleLoginAttempts() {
        // === SETUP ===
        val toast = AndroidToast()
        val analytics = FirebaseAnalytics()

        KRelay.register<ToastFeature>(toast)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION: User tries 3 times ===
        val viewModel = LoginViewModel()
        viewModel.login("wrong1@example.com", "wrong")
        viewModel.login("wrong2@example.com", "wrong")
        viewModel.login("test@example.com", "correct")

        // === VERIFY: All attempts tracked ===
        val attempts = analytics.events.filter { it.first == "login_attempt" }
        assertEquals(3, attempts.size)

        val failures = analytics.events.filter { it.first == "login_failed" }
        assertEquals(2, failures.size)

        val successes = analytics.events.filter { it.first == "login_success" }
        assertEquals(1, successes.size)
    }
}

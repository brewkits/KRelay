package dev.brewkits.krelay.integration.voyager

import dev.brewkits.krelay.KRelay

/**
 * ViewModel for Voyager integration demo.
 *
 * Key point: This ViewModel has ZERO dependencies on Voyager Navigator.
 * All navigation is done via KRelay.dispatch().
 *
 * Benefits:
 * - Easy to test (no Navigator mocking needed)
 * - Can switch navigation libraries without touching this code
 * - Pure business logic
 */
class VoyagerLoginViewModel {
    fun onLoginSuccess(username: String) {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔐 [LoginViewModel] Login button clicked")
        println("   → Business Logic: Authenticating user '$username'...")

        // Simulate login logic (in real app: network call, token storage, etc.)
        println("   → Business Logic: Authentication successful! ✓")
        println("   → ViewModel has ZERO Voyager dependencies!")

        println("\n📤 [LoginViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<VoyagerNavFeature> { it.navigateToHome() }")
        println("   → This is FIRE-AND-FORGET pattern (no return value)")

        // Navigate to home - Fire and forget!
        KRelay.dispatch<VoyagerNavFeature> {
            it.navigateToHome()
        }

        println("   → KRelay dispatch completed (queued for processing)")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    fun onGotoSignup() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📝 [LoginViewModel] Signup button clicked")
        println("   → Business Logic: User wants to create account")

        println("\n📤 [LoginViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<VoyagerNavFeature> { it.navigateToSignup() }")

        KRelay.dispatch<VoyagerNavFeature> {
            it.navigateToSignup()
        }

        println("   → Dispatch completed, navigation will happen on main thread")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

class VoyagerHomeViewModel {
    fun onViewProfile(userId: String) {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("👤 [HomeViewModel] View Profile button clicked")
        println("   → Business Logic: Fetching profile data for user '$userId'...")
        println("   → Business Logic: Profile data loaded ✓")

        println("\n📤 [HomeViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<VoyagerNavFeature> { it.navigateToProfile('$userId') }")
        println("   → Passing parameter: userId = '$userId'")

        KRelay.dispatch<VoyagerNavFeature> {
            it.navigateToProfile(userId)
        }

        println("   → Navigation dispatched with parameters")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    fun onLogout() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("👋 [HomeViewModel] Logout button clicked")
        println("   → Business Logic: Clearing user session...")
        println("   → Business Logic: Deleting auth tokens...")
        println("   → Business Logic: Cleanup completed ✓")

        println("\n📤 [HomeViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<VoyagerNavFeature> { it.navigateToLogin() }")
        println("   → This will REPLACE entire navigation stack")

        KRelay.dispatch<VoyagerNavFeature> {
            it.navigateToLogin()
        }

        println("   → Logout navigation dispatched")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

class VoyagerProfileViewModel(val userId: String) {
    fun onBack() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("⬅️ [ProfileViewModel] Back button clicked")
        println("   → Current userId: '$userId'")
        println("   → Business Logic: Saving any unsaved changes...")
        println("   → Business Logic: Save completed ✓")

        println("\n📤 [ProfileViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<VoyagerNavFeature> { it.navigateBack() }")
        println("   → This will POP current screen from stack")

        KRelay.dispatch<VoyagerNavFeature> {
            it.navigateBack()
        }

        println("   → Back navigation dispatched")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

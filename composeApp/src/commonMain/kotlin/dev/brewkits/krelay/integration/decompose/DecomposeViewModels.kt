package dev.brewkits.krelay.integration.decompose

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.ProcessDeathUnsafe
import dev.brewkits.krelay.SuperAppWarning

/**
 * ViewModels for Decompose demo.
 *
 * NOTE: These ViewModels have ZERO Decompose dependencies!
 * They only depend on DecomposeNavFeature interface.
 *
 * This demonstrates clean architecture:
 * - Easy to test (mock DecomposeNavFeature)
 * - Easy to swap navigation libraries
 * - Platform-agnostic business logic
 */

class DecomposeLoginViewModel {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onLoginClick() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔐 [DecomposeLoginViewModel] Login button clicked")
        println("   → Business Logic: Authenticating user 'demo_user'...")
        println("   → Business Logic: Authentication successful! ✓")
        println("   → ViewModel has ZERO Decompose dependencies!")
        println("")
        println("📤 [DecomposeLoginViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<DecomposeNavFeature> { it.navigateToHome() }")
        println("   → This is FIRE-AND-FORGET pattern (no return value)")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateToHome() }

        println("   → KRelay dispatch completed (queued for processing)")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onSignupClick() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("✍️ [DecomposeLoginViewModel] Signup button clicked")
        println("   → Navigating to Signup screen...")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateToSignup() }

        println("   → Navigation dispatched")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

class DecomposeHomeViewModel {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onViewProfileClick(userId: String) {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("👤 [DecomposeHomeViewModel] View Profile button clicked")
        println("   → Business Logic: Fetching profile data for user '$userId'...")
        println("   → Business Logic: Profile data loaded ✓")
        println("")
        println("📤 [DecomposeHomeViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<DecomposeNavFeature> { it.navigateToProfile('$userId') }")
        println("   → Passing parameter: userId = '$userId'")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateToProfile(userId) }

        println("   → Navigation dispatched with parameters")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onLogoutClick() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🚪 [DecomposeHomeViewModel] Logout button clicked")
        println("   → Business Logic: Clearing user session...")
        println("   → Business Logic: Session cleared ✓")
        println("")
        println("📤 [DecomposeHomeViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<DecomposeNavFeature> { it.navigateToLogin() }")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateToLogin() }

        println("   → Logout navigation dispatched")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

class DecomposeProfileViewModel(private val userId: String) {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onBackClick() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("⬅️ [DecomposeProfileViewModel] Back button clicked")
        println("   → Current userId: '$userId'")
        println("   → Business Logic: Saving any unsaved changes...")
        println("   → Business Logic: Save completed ✓")
        println("")
        println("📤 [DecomposeProfileViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<DecomposeNavFeature> { it.navigateBack() }")
        println("   → This will POP current screen from stack")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateBack() }

        println("   → Back navigation dispatched")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

class DecomposeSignupViewModel {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onCreateAccountClick() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("✍️ [DecomposeSignupViewModel] Create Account button clicked")
        println("   → Business Logic: Creating new account...")
        println("   → Business Logic: Account created successfully! ✓")
        println("")
        println("📤 [DecomposeSignupViewModel] Dispatching navigation via KRelay...")
        println("   → Calling: KRelay.dispatch<DecomposeNavFeature> { it.navigateToHome() }")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateToHome() }

        println("   → Navigation to Home dispatched (account created)")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun onBackClick() {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("⬅️ [DecomposeSignupViewModel] Back button clicked")
        println("   → Navigating back to Login screen...")

        KRelay.dispatch<DecomposeNavFeature> { it.navigateBack() }

        println("   → Back navigation dispatched")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}

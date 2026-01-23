package dev.brewkits.krelay.integration.decompose

/**
 * Decompose implementation of DecomposeNavFeature.
 *
 * This is the BRIDGE between KRelay and Decompose.
 *
 * Architecture:
 * - ViewModels call: KRelay.dispatch<DecomposeNavFeature> { it.navigateToHome() }
 * - KRelay finds this implementation
 * - This implementation translates to Decompose Component calls
 * - Decompose Router performs actual navigation
 *
 * Key benefits:
 * - ViewModels have ZERO Decompose dependencies
 * - Easy to swap Decompose for another library
 * - Testable with simple mocks
 */
class DecomposeNavigationImpl(
    private val component: DecomposeNavigationComponent
) : DecomposeNavFeature {

    override fun navigateToHome() {
        println("\n🌉 [DecomposeNavigationImpl] KRelay called navigateToHome()")
        println("   ┌─ This is the BRIDGE between KRelay → Decompose")
        println("   ├─ Action: REPLACE CURRENT (clear and go to Home)")
        println("   └─ Calling: component.navigateToHome()")

        component.navigateToHome()

        println("   ✓ Navigation command sent to Decompose")
        println("   ✓ Decompose Router will handle the transition\n")
    }

    override fun navigateToProfile(userId: String) {
        println("\n🌉 [DecomposeNavigationImpl] KRelay called navigateToProfile('$userId')")
        println("   ┌─ This is the BRIDGE between KRelay → Decompose")
        println("   ├─ Action: PUSH (add Profile to stack)")
        println("   ├─ Parameter: userId='$userId'")
        println("   └─ Calling: component.navigateToProfile(userId)")

        component.navigateToProfile(userId)

        println("   ✓ Navigation command sent to Decompose")
        println("   ✓ Profile screen will be pushed onto stack\n")
    }

    override fun navigateBack() {
        println("\n🌉 [DecomposeNavigationImpl] KRelay called navigateBack()")
        println("   ┌─ This is the BRIDGE between KRelay → Decompose")
        println("   ├─ Action: POP (remove top screen)")
        println("   └─ Calling: component.navigateBack()")

        component.navigateBack()

        println("   ✓ Navigation command sent to Decompose")
        println("   ✓ Current screen will be popped from stack\n")
    }

    override fun navigateToLogin() {
        println("\n🌉 [DecomposeNavigationImpl] KRelay called navigateToLogin()")
        println("   ┌─ This is the BRIDGE between KRelay → Decompose")
        println("   ├─ Action: REPLACE CURRENT (logout flow)")
        println("   └─ Calling: component.navigateToLogin()")

        component.navigateToLogin()

        println("   ✓ Navigation command sent to Decompose")
        println("   ✓ Will navigate to Login screen\n")
    }

    override fun navigateToSignup() {
        println("\n🌉 [DecomposeNavigationImpl] KRelay called navigateToSignup()")
        println("   ┌─ This is the BRIDGE between KRelay → Decompose")
        println("   ├─ Action: PUSH (add Signup to stack)")
        println("   └─ Calling: component.navigateToSignup()")

        component.navigateToSignup()

        println("   ✓ Navigation command sent to Decompose")
        println("   ✓ Signup screen will be pushed onto stack\n")
    }
}

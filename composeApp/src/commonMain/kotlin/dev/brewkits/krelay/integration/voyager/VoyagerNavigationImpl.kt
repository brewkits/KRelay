package dev.brewkits.krelay.integration.voyager

import cafe.adriel.voyager.navigator.Navigator

/**
 * Real Voyager implementation of VoyagerNavFeature.
 *
 * This is the ONLY file that knows about Voyager Navigator.
 * It translates KRelay navigation commands into Voyager API calls.
 *
 * Pattern:
 * - ViewModels dispatch to VoyagerNavFeature (interface)
 * - KRelay finds this implementation
 * - This implementation calls Voyager Navigator
 */
class VoyagerNavigationImpl(
    private val navigator: Navigator,
    private val onBackToMenu: () -> Unit
) : VoyagerNavFeature {

    override fun navigateToHome() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToHome()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: REPLACE ALL (clear entire stack)")
        println("   ├─ Creating: HomeScreen(onBackToMenu)")
        println("   └─ Calling: navigator.replaceAll(HomeScreen)")

        // Replace entire stack with HomeScreen
        navigator.replaceAll(HomeScreen(onBackToMenu = onBackToMenu))

        println("   ✓ Navigation completed!")
        println("   ✓ New stack size: ${navigator.size}")
        println("   ✓ Current screen: HomeScreen\n")
    }

    override fun navigateToProfile(userId: String) {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToProfile('$userId')")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: PUSH (add to stack)")
        println("   ├─ Creating: ProfileScreen(userId='$userId', onBackToMenu)")
        println("   └─ Calling: navigator.push(ProfileScreen)")

        // Push ProfileScreen onto stack
        navigator.push(ProfileScreen(userId = userId, onBackToMenu = onBackToMenu))

        println("   ✓ Navigation completed!")
        println("   ✓ New stack size: ${navigator.size}")
        println("   ✓ Current screen: ProfileScreen\n")
    }

    override fun navigateBack() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateBack()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: POP (remove top screen)")
        println("   └─ Calling: navigator.pop()")

        // Pop current screen
        navigator.pop()

        println("   ✓ Navigation completed!")
        println("   ✓ New stack size: ${navigator.size}")
        println("   ✓ Returned to previous screen\n")
    }

    override fun navigateToLogin() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToLogin()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: REPLACE ALL (logout flow)")
        println("   ├─ Creating: LoginScreen(onBackToMenu)")
        println("   └─ Calling: navigator.replaceAll(LoginScreen)")

        // Replace entire stack with LoginScreen
        navigator.replaceAll(LoginScreen(onBackToMenu = onBackToMenu))

        println("   ✓ Navigation completed!")
        println("   ✓ New stack size: ${navigator.size}")
        println("   ✓ Current screen: LoginScreen (user logged out)\n")
    }

    override fun navigateToSignup() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToSignup()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: PUSH (add signup screen)")
        println("   ├─ Creating: SignupScreen(onBackToMenu)")
        println("   └─ Calling: navigator.push(SignupScreen)")

        // Push SignupScreen onto stack
        navigator.push(SignupScreen(onBackToMenu = onBackToMenu))

        println("   ✓ Navigation completed!")
        println("   ✓ New stack size: ${navigator.size}")
        println("   ✓ Current screen: SignupScreen\n")
    }
}

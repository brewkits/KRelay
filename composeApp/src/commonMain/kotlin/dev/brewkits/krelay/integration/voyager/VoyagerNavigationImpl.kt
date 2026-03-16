package dev.brewkits.krelay.integration.voyager

import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
 *
 * The coroutine scope is provided by the composable (rememberCoroutineScope)
 * so it is automatically cancelled when the composition leaves.
 */
class VoyagerNavigationImpl(
    private val navigator: Navigator,
    private val scope: CoroutineScope,
    private val onBackToMenu: () -> Unit
) : VoyagerNavFeature {

    override fun navigateToHome() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToHome()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: replaceAll(HomeScreen)")
        println("   └─ Scheduling navigation in composable scope")

        scope.launch {
            try {
                navigator.replaceAll(HomeScreen(onBackToMenu = onBackToMenu))
                println("   ✓ Navigation completed! Stack size: ${navigator.size}\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
            }
        }
    }

    override fun navigateToProfile(userId: String) {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToProfile('$userId')")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: push(ProfileScreen)")
        println("   └─ Scheduling navigation in composable scope")

        scope.launch {
            try {
                navigator.push(ProfileScreen(userId = userId, onBackToMenu = onBackToMenu))
                println("   ✓ Navigation completed! Stack size: ${navigator.size}\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
            }
        }
    }

    override fun navigateBack() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateBack()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: pop()")
        println("   └─ Scheduling navigation in composable scope")

        scope.launch {
            try {
                navigator.pop()
                println("   ✓ Navigation completed! Stack size: ${navigator.size}\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
            }
        }
    }

    override fun navigateToLogin() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToLogin()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: replaceAll(LoginScreen) — logout flow")
        println("   └─ Scheduling navigation in composable scope")

        scope.launch {
            try {
                navigator.replaceAll(LoginScreen(onBackToMenu = onBackToMenu))
                println("   ✓ Navigation completed! Stack size: ${navigator.size}\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
            }
        }
    }

    override fun navigateToSignup() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToSignup()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: push(SignupScreen)")
        println("   └─ Scheduling navigation in composable scope")

        scope.launch {
            try {
                navigator.push(SignupScreen(onBackToMenu = onBackToMenu))
                println("   ✓ Navigation completed! Stack size: ${navigator.size}\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
            }
        }
    }
}

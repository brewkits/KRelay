package dev.brewkits.krelay.integration.voyager

import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

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
 * Note: Navigation calls use replace() instead of replaceAll() to avoid lifecycle conflicts
 */
class VoyagerNavigationImpl(
    private val navigator: Navigator,
    private val onBackToMenu: () -> Unit
) : VoyagerNavFeature {

    private val navigationScope = CoroutineScope(Dispatchers.Main)

    override fun navigateToHome() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToHome()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: POP ALL + PUSH (workaround for lifecycle issue)")
        println("   ├─ Creating: HomeScreen(onBackToMenu)")
        println("   └─ Scheduling navigation in coroutine scope")

        navigationScope.launch {
            try {
                yield()
                delay(150)

                // Workaround: popAll() then push() to avoid lifecycle conflicts
                navigator.popAll()
                delay(50) // Small gap between operations
                navigator.push(HomeScreen(onBackToMenu = onBackToMenu))

                println("   ✓ Navigation completed!")
                println("   ✓ New stack size: ${navigator.size}")
                println("   ✓ Current screen: HomeScreen\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun navigateToProfile(userId: String) {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToProfile('$userId')")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: PUSH (add to stack)")
        println("   ├─ Creating: ProfileScreen(userId='$userId', onBackToMenu)")
        println("   └─ Scheduling navigation in coroutine scope")

        navigationScope.launch {
            try {
                yield()
                delay(100)
                navigator.push(ProfileScreen(userId = userId, onBackToMenu = onBackToMenu))
                println("   ✓ Navigation completed!")
                println("   ✓ New stack size: ${navigator.size}")
                println("   ✓ Current screen: ProfileScreen\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun navigateBack() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateBack()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: POP (remove top screen)")
        println("   └─ Scheduling navigation in coroutine scope")

        navigationScope.launch {
            try {
                yield()
                delay(100)
                navigator.pop()
                println("   ✓ Navigation completed!")
                println("   ✓ New stack size: ${navigator.size}")
                println("   ✓ Returned to previous screen\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun navigateToLogin() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToLogin()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: POP ALL + PUSH (logout flow)")
        println("   ├─ Creating: LoginScreen(onBackToMenu)")
        println("   └─ Scheduling navigation in coroutine scope")

        navigationScope.launch {
            try {
                yield()
                delay(150)

                navigator.popAll()
                delay(50)
                navigator.push(LoginScreen(onBackToMenu = onBackToMenu))

                println("   ✓ Navigation completed!")
                println("   ✓ New stack size: ${navigator.size}")
                println("   ✓ Current screen: LoginScreen (user logged out)\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun navigateToSignup() {
        println("\n🌉 [VoyagerNavigationImpl] KRelay called navigateToSignup()")
        println("   ┌─ This is the BRIDGE between KRelay → Voyager")
        println("   ├─ Current stack size: ${navigator.size}")
        println("   ├─ Action: PUSH (add signup screen)")
        println("   ├─ Creating: SignupScreen(onBackToMenu)")
        println("   └─ Scheduling navigation in coroutine scope")

        navigationScope.launch {
            try {
                yield()
                delay(100)
                navigator.push(SignupScreen(onBackToMenu = onBackToMenu))
                println("   ✓ Navigation completed!")
                println("   ✓ New stack size: ${navigator.size}")
                println("   ✓ Current screen: SignupScreen\n")
            } catch (e: Exception) {
                println("   ❌ Navigation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

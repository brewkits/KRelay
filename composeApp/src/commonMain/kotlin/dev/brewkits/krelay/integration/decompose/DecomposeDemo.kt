package dev.brewkits.krelay.integration.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.ProcessDeathUnsafe
import dev.brewkits.krelay.SuperAppWarning

/**
 * Decompose Demo Entry Point.
 *
 * This demonstrates the complete KRelay + Decompose integration:
 *
 * Architecture Flow:
 * ┌─────────────────────┐
 * │   DecomposeDemo     │ ← Entry point (THIS FILE)
 * │  (Component setup)  │
 * └──────────┬──────────┘
 *            │ Creates & registers
 *            ↓
 * ┌─────────────────────┐
 * │ DecomposeNavigation │ ← Decompose Root Component
 * │     Component       │   (Manages navigation stack)
 * └──────────┬──────────┘
 *            │ Wrapped by
 *            ↓
 * ┌─────────────────────┐
 * │ DecomposeNavigation │ ← KRelay Bridge Implementation
 * │       Impl          │   (RelayFeature interface)
 * └──────────┬──────────┘
 *            │ Registered in
 *            ↓
 * ┌─────────────────────┐
 * │      KRelay         │ ← Feature dispatcher
 * │  (Feature Registry) │
 * └──────────┬──────────┘
 *            │ Called by
 *            ↓
 * ┌─────────────────────┐
 * │    ViewModels       │ ← Business logic layer
 * │ (Login, Home, etc)  │   (ZERO Decompose dependencies!)
 * └─────────────────────┘
 *
 * Key Benefits:
 * - ViewModels are 100% platform-agnostic
 * - Easy to swap Decompose for another library
 * - Easy to test with mock implementations
 * - Clean separation of concerns
 */
@OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
@Composable
fun DecomposeDemo(onBackClick: () -> Unit) {
    // Create Decompose lifecycle and component context
    val lifecycle = remember { LifecycleRegistry() }
    val componentContext = remember {
        DefaultComponentContext(lifecycle = lifecycle)
    }

    // Create the root navigation component
    val navigationComponent = remember {
        DecomposeNavigationComponent(
            componentContext = componentContext,
            onBackToMenu = onBackClick
        )
    }

    // Create the KRelay bridge implementation
    val navigationImpl = remember {
        DecomposeNavigationImpl(navigationComponent)
    }

    // Register the navigation feature with KRelay
    DisposableEffect(Unit) {
        println("\n╔═══════════════════════════════════════════════════════════════════╗")
        println("║                  🧩 DECOMPOSE DEMO STARTING                        ║")
        println("╚═══════════════════════════════════════════════════════════════════╝")
        println()
        println("📋 [DecomposeDemo] Setting up Decompose + KRelay integration...")
        println("   ┌─ Creating Decompose navigation component")
        println("   ├─ Creating KRelay bridge (DecomposeNavigationImpl)")
        println("   └─ Registering with KRelay feature registry")
        println()

        KRelay.register<DecomposeNavFeature>(navigationImpl)

        println("✅ [DecomposeDemo] Setup complete!")
        println("   ┌─ DecomposeNavFeature registered")
        println("   ├─ ViewModels can now call: KRelay.dispatch<DecomposeNavFeature> { ... }")
        println("   └─ Navigation calls will route through DecomposeNavigationImpl")
        println()
        println("🎬 [DecomposeDemo] Starting at Login screen...")
        println()

        onDispose {
            println("\n╔═══════════════════════════════════════════════════════════════════╗")
            println("║                  🧩 DECOMPOSE DEMO CLEANUP                         ║")
            println("╚═══════════════════════════════════════════════════════════════════╝")
            println()
            println("🧹 [DecomposeDemo] Unregistering DecomposeNavFeature from KRelay...")

            KRelay.unregister<DecomposeNavFeature>()

            println("✅ [DecomposeDemo] Cleanup complete!")
            println("   └─ DecomposeNavFeature unregistered")
            println()
        }
    }

    // Render the navigation stack with animations
    Children(
        stack = navigationComponent.childStack,
        animation = stackAnimation(slide())
    ) { child ->
        when (val instance = child.instance) {
            is DecomposeNavigationComponent.Child.Login -> {
                DecomposeLoginScreen(instance.component)
            }
            is DecomposeNavigationComponent.Child.Home -> {
                DecomposeHomeScreen(instance.component)
            }
            is DecomposeNavigationComponent.Child.Profile -> {
                DecomposeProfileScreen(instance.component)
            }
            is DecomposeNavigationComponent.Child.Signup -> {
                DecomposeSignupScreen(instance.component)
            }
        }
    }
}

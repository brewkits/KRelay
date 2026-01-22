package dev.brewkits.krelay.integration.voyager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import dev.brewkits.krelay.KRelay

/**
 * Voyager Integration Demo
 *
 * This demo shows real integration between KRelay and Voyager navigation library.
 *
 * Architecture:
 * 1. VoyagerNavFeature - Navigation contract (interface)
 * 2. ViewModels - Use KRelay.dispatch() to navigate (zero Voyager dependency)
 * 3. VoyagerNavigationImpl - Bridges KRelay → Voyager Navigator
 * 4. This file - Wires everything together
 *
 * Key Pattern:
 * - Navigator is created by Voyager
 * - VoyagerNavigationImpl wraps Navigator
 * - KRelay.register() connects the bridge
 * - ViewModels dispatch navigation commands via KRelay
 */
@Composable
fun VoyagerDemo(onBackClick: () -> Unit) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Create Voyager Navigator starting at LoginScreen
            Navigator(LoginScreen(onBackToMenu = onBackClick)) { navigator ->
                // Register KRelay navigation bridge
                // This is the magic connection point!
                LaunchedEffect(navigator) {
                    println("\n╔════════════════════════════════════════════════════════════════╗")
                    println("║  🚀 VOYAGER DEMO - KRelay Integration Setup                  ║")
                    println("╚════════════════════════════════════════════════════════════════╝")
                    println("\n🔧 [VoyagerDemo] Initializing KRelay bridge...")
                    println("   Step 1: Creating VoyagerNavigationImpl (the bridge)")
                    val navImpl = VoyagerNavigationImpl(navigator, onBackClick)
                    println("   Step 2: Registering VoyagerNavFeature with KRelay")
                    KRelay.register<VoyagerNavFeature>(navImpl)
                    println("   ✓ Registration complete!")
                    println("\n💡 HOW IT WORKS:")
                    println("   1️⃣  ViewModel calls: KRelay.dispatch<VoyagerNavFeature> { ... }")
                    println("   2️⃣  KRelay finds the registered VoyagerNavigationImpl")
                    println("   3️⃣  VoyagerNavigationImpl translates to Voyager Navigator calls")
                    println("   4️⃣  Voyager Navigator performs actual screen navigation")
                    println("\n✨ ViewModels have ZERO Voyager dependencies!")
                    println("✨ Easy to swap Voyager for Decompose or any other nav library!")
                    println("✨ Testable without mocking Navigator!")
                    println("\n═══════════════════════════════════════════════════════════════════\n")
                }

                // Voyager handles screen transitions
                SlideTransition(navigator)
            }
        }
    }
}

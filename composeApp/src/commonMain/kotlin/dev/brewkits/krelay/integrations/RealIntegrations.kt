package dev.brewkits.krelay.integrations

import androidx.compose.runtime.*
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.samples.*

/**
 * Setup function to register all REAL implementations.
 *
 * This replaces mock implementations with actual library integrations:
 * - Moko Permissions
 * - Moko Biometry
 * - Peekaboo Media Picker
 * - Play Core Review (Android) / StoreKit (iOS)
 */
@Composable
fun SetupRealIntegrations() {
    // Platform-specific implementations will be provided via expect/actual
    val permissionImpl = rememberPermissionImplementation()
    val biometricImpl = rememberBiometricImplementation()
    val mediaImpl = rememberMediaImplementation()
    val systemInteractionImpl = rememberSystemInteractionImplementation()
    val toastImpl = rememberToastImplementation()
    val hapticImpl = rememberHapticImplementation()
    val analyticsImpl = rememberAnalyticsImplementation()
    // KRelay keeps implementations weakly: hold a strong reference for the composition's lifetime
    val navigationImpl = remember { MockNavigationImpl() }

    LaunchedEffect(Unit) {
        println("\n╔════════════════════════════════════════════════════════════════╗")
        println("║  🔌 REAL INTEGRATIONS - Actual Library Setup                  ║")
        println("╚════════════════════════════════════════════════════════════════╝")
        println("\n🔧 [RealIntegrations] Registering REAL implementations...")
        println("   → Using actual Moko Permissions")
        println("   → Using actual Moko Biometry")
        println("   → Using actual Peekaboo Image Picker")
        println("   → Using actual Play Core Review (Android) / StoreKit (iOS)")
        println()

        KRelay.register<PermissionFeature>(permissionImpl)
        println("   ✓ PermissionFeature -> MokoPermissionImpl (REAL)")

        KRelay.register<BiometricFeature>(biometricImpl)
        println("   ✓ BiometricFeature -> MokoBiometricImpl (REAL)")

        KRelay.register<MediaFeature>(mediaImpl)
        println("   ✓ MediaFeature -> PeekabooMediaImpl (REAL)")

        KRelay.register<SystemInteractionFeature>(systemInteractionImpl)
        println("   ✓ SystemInteractionFeature -> Platform specific (REAL)")

        // Supporting features - using REAL implementations
        KRelay.register<ToastFeature>(toastImpl)
        println("   ✓ ToastFeature -> Platform specific (REAL)")

        KRelay.register<HapticFeature>(hapticImpl)
        println("   ✓ HapticFeature -> Platform specific (REAL)")

        KRelay.register<AnalyticsFeature>(analyticsImpl)
        println("   ✓ AnalyticsFeature -> Platform specific (REAL)")

        // Navigation feature - using mock (use VoyagerDemo for real navigation)
        KRelay.register<NavigationFeature>(navigationImpl)
        println("   ✓ NavigationFeature -> Mock (see VoyagerDemo for real navigation)")

        println("\n✨ All REAL integrations registered!")
        println("   Now you can test with actual platform features!")
        println("═══════════════════════════════════════════════════════════════════\n")
    }
}

/**
 * Platform-specific factory functions (expect/actual)
 */
@Composable
expect fun rememberPermissionImplementation(): PermissionFeature

@Composable
expect fun rememberBiometricImplementation(): BiometricFeature

@Composable
expect fun rememberMediaImplementation(): MediaFeature

@Composable
expect fun rememberSystemInteractionImplementation(): SystemInteractionFeature

@Composable
expect fun rememberToastImplementation(): ToastFeature

@Composable
expect fun rememberHapticImplementation(): HapticFeature

@Composable
expect fun rememberAnalyticsImplementation(): AnalyticsFeature

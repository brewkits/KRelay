package dev.brewkits.krelay.demo

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlin.test.Test

/**
 * Interactive demo of KRelay diagnostic features.
 *
 * Run this test to see dump() output and diagnostic info.
 */
class DiagnosticDemo {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavigationFeature : RelayFeature {
        fun navigate(route: String)
    }

    interface PermissionFeature : RelayFeature {
        fun request(permission: String)
    }

    class AndroidToast : ToastFeature {
        override fun show(message: String) {
            println("🍞 Toast: $message")
        }
    }

    class VoyagerNavigation : NavigationFeature {
        override fun navigate(route: String) {
            println("🧭 Navigate to: $route")
        }
    }

    @Test
    fun demoScenario1_EmptyState() {
        println("\n" + "=".repeat(60))
        println("DEMO 1: Empty State")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true

        println("\n📊 Calling KRelay.dump()...")
        KRelay.dump()

        println("\n📊 Using getDebugInfo()...")
        val info = KRelay.getDebugInfo()
        println("Registered Features: ${info.registeredFeaturesCount}")
        println("Total Pending: ${info.totalPendingActions}")
    }

    @Test
    fun demoScenario2_WithRegisteredFeatures() {
        println("\n" + "=".repeat(60))
        println("DEMO 2: With Registered Features")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true

        val toast = AndroidToast()
        val nav = VoyagerNavigation()

        println("\n📝 Registering ToastFeature...")
        KRelay.register<ToastFeature>(toast)

        println("\n📝 Registering NavigationFeature...")
        KRelay.register<NavigationFeature>(nav)

        println("\n📊 Calling KRelay.dump()...")
        KRelay.dump()

        println("\n📊 Individual counts:")
        println("  - getRegisteredFeaturesCount(): ${KRelay.getRegisteredFeaturesCount()}")
        println("  - getTotalPendingCount(): ${KRelay.getTotalPendingCount()}")
    }

    @Test
    fun demoScenario3_WithQueuedActions() {
        println("\n" + "=".repeat(60))
        println("DEMO 3: With Queued Actions (Features Not Registered)")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true

        println("\n📤 Dispatching actions WITHOUT registering features...")
        KRelay.dispatch<ToastFeature> { it.show("Hello World!") }
        KRelay.dispatch<ToastFeature> { it.show("Second toast") }
        KRelay.dispatch<NavigationFeature> { it.navigate("/home") }
        KRelay.dispatch<PermissionFeature> { it.request("CAMERA") }
        KRelay.dispatch<PermissionFeature> { it.request("LOCATION") }

        println("\n📊 Calling KRelay.dump()...")
        KRelay.dump()

        println("\n📊 Per-feature pending counts:")
        println("  - ToastFeature: ${KRelay.getPendingCount<ToastFeature>()} pending")
        println("  - NavigationFeature: ${KRelay.getPendingCount<NavigationFeature>()} pending")
        println("  - PermissionFeature: ${KRelay.getPendingCount<PermissionFeature>()} pending")
    }

    @Test
    fun demoScenario4_MixedState() {
        println("\n" + "=".repeat(60))
        println("DEMO 4: Mixed State (Some Registered, Some Queued)")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true

        // Register ToastFeature
        val toast = AndroidToast()
        println("\n📝 Registering ToastFeature...")
        KRelay.register<ToastFeature>(toast)

        // Dispatch to registered feature (will execute immediately)
        println("\n📤 Dispatching to ToastFeature (registered)...")
        KRelay.dispatch<ToastFeature> { it.show("This will execute now!") }

        // Dispatch to unregistered features (will queue)
        println("\n📤 Dispatching to NavigationFeature (not registered)...")
        KRelay.dispatch<NavigationFeature> { it.navigate("/home") }
        KRelay.dispatch<NavigationFeature> { it.navigate("/profile") }

        println("\n📤 Dispatching to PermissionFeature (not registered)...")
        KRelay.dispatch<PermissionFeature> { it.request("CAMERA") }

        println("\n📊 Calling KRelay.dump()...")
        KRelay.dump()
    }

    @Test
    fun demoScenario5_QueueSizeLimit() {
        println("\n" + "=".repeat(60))
        println("DEMO 5: Queue Size Limit (DROP_OLDEST policy)")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true
        KRelay.maxQueueSize = 5 // Set small limit for demo

        println("\n⚙️  Set maxQueueSize = 5")
        println("\n📤 Dispatching 10 actions to ToastFeature...")

        repeat(10) { i ->
            KRelay.dispatch<ToastFeature> { it.show("Toast #${i + 1}") }
            println("  - Dispatched Toast #${i + 1}")
        }

        println("\n📊 Calling KRelay.dump()...")
        KRelay.dump()

        println("\n✅ Only 5 most recent actions remain (oldest 5 dropped)")
    }

    @Test
    fun demoScenario6_ActionExpiry() {
        println("\n" + "=".repeat(60))
        println("DEMO 6: Action Expiry (TTL)")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true
        KRelay.actionExpiryMs = 0 // Instant expiry for demo

        println("\n⚙️  Set actionExpiryMs = 0ms (instant expiry)")

        println("\n📤 Dispatching 5 actions...")
        repeat(5) { i ->
            KRelay.dispatch<ToastFeature> { it.show("Toast #${i + 1}") }
        }

        println("\n📊 Calling getDebugInfo() triggers cleanup of expired actions:")
        val info = KRelay.getDebugInfo()
        println("Expired & Removed: ${info.expiredActionsRemoved} actions")

        println("\n📊 Final dump (should show 0 pending):")
        KRelay.dump()

        println("\n✅ All actions expired and removed due to 0ms TTL")
    }

    @Test
    fun demoScenario7_CustomConfiguration() {
        println("\n" + "=".repeat(60))
        println("DEMO 7: Custom Configuration")
        println("=".repeat(60))

        KRelay.reset()
        KRelay.debugMode = true

        println("\n⚙️  Configuring custom settings...")
        KRelay.maxQueueSize = 50
        KRelay.actionExpiryMs = 2 * 60 * 1000 // 2 minutes

        println("  - maxQueueSize: 50")
        println("  - actionExpiryMs: 2 minutes (120000ms)")

        println("\n📊 Calling KRelay.dump()...")
        KRelay.dump()

        println("\n📊 Using getDebugInfo() to verify configuration:")
        val info = KRelay.getDebugInfo()
        println("  - maxQueueSize: ${info.maxQueueSize}")
        println("  - actionExpiryMs: ${info.actionExpiryMs}ms = ${info.actionExpiryMs / 60000.0} min")
        println("  - debugMode: ${info.debugMode}")
    }
}

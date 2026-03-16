package dev.brewkits.krelay.demo

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Interactive demo of the Scope Token API.
 *
 * Run these tests to see the output of each scenario.
 * They illustrate real-world ViewModel lifecycle patterns.
 */
class ScopeTokenDemo {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigateTo(screen: String)
    }

    class AndroidToast : ToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) {
            shown.add(message)
            println("🍞 Toast: $message")
        }
    }

    class VoyagerNav : NavFeature {
        val navigated = mutableListOf<String>()
        override fun navigateTo(screen: String) {
            navigated.add(screen)
            println("🧭 Navigate: $screen")
        }
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("ScopeTokenDemo")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo1_basicScopeToken_queueAndCancel() {
        println("\n${"=".repeat(60)}")
        println("DEMO 1: Basic Scope Token — Queue and Cancel")
        println("=".repeat(60))

        val vmToken = scopedToken()
        println("\n🔑 Created token: $vmToken")

        println("\n📤 ViewModel dispatches 2 actions (UI not ready yet)...")
        instance.dispatch<ToastFeature>(vmToken) { it.show("Loading done!") }
        instance.dispatch<NavFeature>(vmToken) { it.navigateTo("dashboard") }
        println("  Queue: Toast=${instance.getPendingCount<ToastFeature>()}, Nav=${instance.getPendingCount<NavFeature>()}")

        println("\n🗑️  ViewModel destroyed — cancelScope(token)...")
        instance.cancelScope(vmToken)
        println("  Queue after cancel: Toast=${instance.getPendingCount<ToastFeature>()}, Nav=${instance.getPendingCount<NavFeature>()}")

        println("\n📝 Activity registers — nothing should replay...")
        val toast = AndroidToast()
        val nav = VoyagerNav()
        instance.register<ToastFeature>(toast)
        instance.register<NavFeature>(nav)

        println("  Toast replayed: ${toast.shown}")
        println("  Nav replayed: ${nav.navigated}")

        assertTrue(toast.shown.isEmpty())
        assertTrue(nav.navigated.isEmpty())
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo2_scopeToken_onlyTaggedCancelled() {
        println("\n${"=".repeat(60)}")
        println("DEMO 2: Selective Cancel — Only Tagged Actions Removed")
        println("=".repeat(60))

        val vmToken = scopedToken()

        println("\n📤 Dispatching mix of tagged and untagged actions...")
        instance.dispatch<ToastFeature>(vmToken) { it.show("[VM] Loading…") }
        instance.dispatch<ToastFeature> { it.show("[System] Connection OK") }
        instance.dispatch<ToastFeature>(vmToken) { it.show("[VM] Done!") }

        println("  Queue: ${instance.getPendingCount<ToastFeature>()} pending")

        println("\n🗑️  ViewModel destroyed (cancel tagged only)...")
        instance.cancelScope(vmToken)
        println("  Queue: ${instance.getPendingCount<ToastFeature>()} pending")

        val toast = AndroidToast()
        instance.register<ToastFeature>(toast)

        println("  Replayed: ${toast.shown}")
        assertEquals(listOf("[System] Connection OK"), toast.shown)
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo3_twoViewModels_independentLifecycles() {
        println("\n${"=".repeat(60)}")
        println("DEMO 3: Two ViewModels — Independent Lifecycles")
        println("=".repeat(60))

        val homeToken = scopedToken()
        val checkoutToken = scopedToken()

        println("\n📤 HomeViewModel queues actions...")
        instance.dispatch<ToastFeature>(homeToken) { it.show("Home loaded") }
        instance.dispatch<NavFeature>(homeToken) { it.navigateTo("home") }

        println("📤 CheckoutViewModel queues actions...")
        instance.dispatch<ToastFeature>(checkoutToken) { it.show("Checkout started") }
        instance.dispatch<NavFeature>(checkoutToken) { it.navigateTo("checkout") }

        println("\n  Total Toast queue: ${instance.getPendingCount<ToastFeature>()}")
        println("  Total Nav queue: ${instance.getPendingCount<NavFeature>()}")

        println("\n🗑️  HomeViewModel destroyed (user presses back)...")
        instance.cancelScope(homeToken)
        println("  Toast remaining: ${instance.getPendingCount<ToastFeature>()}")
        println("  Nav remaining: ${instance.getPendingCount<NavFeature>()}")

        val toast = AndroidToast()
        val nav = VoyagerNav()
        instance.register<ToastFeature>(toast)
        instance.register<NavFeature>(nav)

        println("\n  Toast replayed: ${toast.shown}")
        println("  Nav replayed: ${nav.navigated}")

        assertEquals(listOf("Checkout started"), toast.shown)
        assertEquals(listOf("checkout"), nav.navigated)
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo4_scopedToken_uniqueness() {
        println("\n${"=".repeat(60)}")
        println("DEMO 4: scopedToken() Uniqueness")
        println("=".repeat(60))

        println("\n🔑 Generating 5 tokens:")
        val tokens = (1..5).map { scopedToken() }
        tokens.forEach { println("  $it") }

        println("\n✅ All unique: ${tokens.toSet().size == tokens.size}")
        assertEquals(5, tokens.toSet().size)
        assertTrue(tokens.all { it.startsWith("krelay-") })
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo5_cancelScope_afterExecutionIsNoOp() {
        println("\n${"=".repeat(60)}")
        println("DEMO 5: cancelScope After Immediate Execution Is a No-Op")
        println("=".repeat(60))

        val toast = AndroidToast()
        instance.register<ToastFeature>(toast)

        val token = scopedToken()
        println("\n📤 Dispatch with impl already registered (executes immediately)...")
        instance.dispatch<ToastFeature>(token) { it.show("Executed!") }

        println("  Executed: ${toast.shown}")
        assertEquals(0, instance.getPendingCount<ToastFeature>())

        println("\n🗑️  cancelScope on empty queue (no-op)...")
        instance.cancelScope(token)

        println("  Queue: ${instance.getPendingCount<ToastFeature>()}")
        println("  Toast list unchanged: ${toast.shown}")

        assertEquals(listOf("Executed!"), toast.shown)
        assertEquals(0, instance.getPendingCount<ToastFeature>())
    }

    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun demo6_scopeToken_withPriorityDispatch() {
        println("\n${"=".repeat(60)}")
        println("DEMO 6: Scope Token + Priority Dispatch")
        println("=".repeat(60))

        val vmToken = scopedToken()

        println("\n📤 Mixed priority dispatches (some tagged, some not)...")
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) { it.show("[CRITICAL] Error!") }
        instance.dispatch<ToastFeature>(vmToken) { it.show("[VM] Loading…") }
        instance.dispatchWithPriority<ToastFeature>(ActionPriority.HIGH) { it.show("[HIGH] Warning") }

        println("  Queue: ${instance.getPendingCount<ToastFeature>()} pending")

        println("\n🗑️  VM destroyed — cancel tagged...")
        instance.cancelScope(vmToken)
        println("  Queue: ${instance.getPendingCount<ToastFeature>()} pending")

        val toast = AndroidToast()
        instance.register<ToastFeature>(toast)

        println("  Replayed (by priority order): ${toast.shown}")

        // CRITICAL and HIGH survive; VM's normal action cancelled
        assertEquals(2, toast.shown.size)
        assertEquals("[CRITICAL] Error!", toast.shown[0])
        assertEquals("[HIGH] Warning", toast.shown[1])
    }
}

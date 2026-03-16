package dev.brewkits.krelay.system

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * System scenario tests for the Scope Token API.
 *
 * Simulates real ViewModel lifecycle patterns:
 * - ViewModel queues actions, destroyed before screen ready → cancelScope clears them
 * - Two ViewModels competing for the same feature queue
 * - Screen rotation: ViewModel survives rotation, new Activity registers → actions replayed
 * - Deep back-stack: multiple screens, each with their own ViewModel token
 */
class ScopeTokenViewModelScenarioTest {

    // ── Feature interfaces ─────────────────────────────────────────────────

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigateTo(screen: String)
    }

    interface LoadingFeature : RelayFeature {
        fun setLoading(visible: Boolean)
    }

    // ── Mock implementations ───────────────────────────────────────────────

    class MockActivity : ToastFeature, NavFeature, LoadingFeature {
        val toasts = mutableListOf<String>()
        val navEvents = mutableListOf<String>()
        var loadingState = false

        override fun show(message: String) { toasts.add(message) }
        override fun navigateTo(screen: String) { navEvents.add(screen) }
        override fun setLoading(visible: Boolean) { loadingState = visible }
    }

    // ── Simulated ViewModel ────────────────────────────────────────────────

    inner class HomeViewModel(private val relay: KRelayInstance) {
        val token = scopedToken()

        fun loadData() {
            relay.dispatch<LoadingFeature>(token) { it.setLoading(true) }
            relay.dispatch<ToastFeature>(token) { it.show("Data loaded") }
        }

        fun navigate(screen: String) {
            relay.dispatch<NavFeature>(token) { it.navigateTo(screen) }
        }

        fun onCleared() {
            relay.cancelScope(token)
        }
    }

    inner class CheckoutViewModel(private val relay: KRelayInstance) {
        val token = scopedToken()

        fun startCheckout() {
            relay.dispatch<ToastFeature>(token) { it.show("Checkout started") }
            relay.dispatch<NavFeature>(token) { it.navigateTo("checkout") }
        }

        fun onCleared() {
            relay.cancelScope(token)
        }
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("ViewModelScenarioScope")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    // ── Scenario 1: ViewModel destroyed before Activity ready ─────────────

    @Test
    fun scenario_viewModelDestroyedBeforeActivityReady_actionsNotReplayed() {
        val vm = HomeViewModel(instance)
        vm.loadData()

        // ViewModel destroyed (e.g. user navigated away during loading)
        vm.onCleared()

        assertEquals(0, instance.getPendingCount<ToastFeature>())
        assertEquals(0, instance.getPendingCount<LoadingFeature>())

        // Activity registers — nothing should replay
        val activity = MockActivity()
        instance.register<ToastFeature>(activity)
        instance.register<LoadingFeature>(activity)
        instance.register<NavFeature>(activity)

        assertTrue(activity.toasts.isEmpty())
        assertFalse(activity.loadingState)
    }

    // ── Scenario 2: ViewModel survives, Activity recreated (rotation) ──────

    @Test
    fun scenario_screenRotation_viewModelSurvives_actionsReplayed() {
        val vm = HomeViewModel(instance)
        vm.loadData()

        // Activity1 destroyed (rotation) before VM dispatches finish
        assertEquals(2, instance.getTotalPendingCount())  // loading + toast

        // New Activity registers after rotation
        val activity2 = MockActivity()
        instance.register<ToastFeature>(activity2)
        instance.register<LoadingFeature>(activity2)

        // Actions replayed to new Activity
        assertEquals(listOf("Data loaded"), activity2.toasts)
        assertTrue(activity2.loadingState)

        // ViewModel still alive — cleanup at correct time
        vm.onCleared()
    }

    // ── Scenario 3: Two ViewModels, independent tokens ─────────────────────

    @Test
    fun scenario_twoViewModels_independentCancellation() {
        val homeVm = HomeViewModel(instance)
        val checkoutVm = CheckoutViewModel(instance)

        homeVm.loadData()        // queues 2 actions (loading + toast)
        checkoutVm.startCheckout() // queues 2 actions (toast + nav)

        assertEquals(2, instance.getPendingCount<ToastFeature>())  // home + checkout

        // Home ViewModel destroyed (user presses back)
        homeVm.onCleared()

        // Only checkout toast remains
        assertEquals(1, instance.getPendingCount<ToastFeature>())
        assertEquals(1, instance.getPendingCount<NavFeature>())

        val activity = MockActivity()
        instance.register<ToastFeature>(activity)
        instance.register<NavFeature>(activity)
        instance.register<LoadingFeature>(activity)

        assertEquals(listOf("Checkout started"), activity.toasts)
        assertEquals(listOf("checkout"), activity.navEvents)
        assertFalse(activity.loadingState)  // Home VM was cancelled before loading replayed
    }

    // ── Scenario 4: Re-launch after back-press, fresh ViewModel ──────────────

    @Test
    fun scenario_backPress_newViewModelToken_onlyNewActionsReplayed() {
        // Screen 1: ViewModel A queues actions, user presses back → cancelled
        val vmA = HomeViewModel(instance)
        vmA.loadData()
        vmA.navigate("profile")
        vmA.onCleared()

        // All queue cleared
        assertEquals(0, instance.getTotalPendingCount())

        // Screen 2: User re-enters, new ViewModel with fresh token
        val vmB = HomeViewModel(instance)
        vmB.loadData()

        assertEquals(2, instance.getTotalPendingCount())

        val activity = MockActivity()
        instance.register<ToastFeature>(activity)
        instance.register<LoadingFeature>(activity)
        instance.register<NavFeature>(activity)

        // Only vmB's actions replayed
        assertEquals(listOf("Data loaded"), activity.toasts)
        assertTrue(activity.loadingState)
    }

    // ── Scenario 5: Mixed tagged and untagged dispatches ──────────────────

    @Test
    fun scenario_mixedTaggedAndUntagged_onlyTaggedCancelled() {
        val vm = HomeViewModel(instance)

        // System-level (untagged) dispatch — e.g. from Application class
        instance.dispatch<ToastFeature> { it.show("System message") }

        // ViewModel-level (tagged)
        vm.loadData()

        assertEquals(2, instance.getPendingCount<ToastFeature>())  // system + vm

        // ViewModel destroyed
        vm.onCleared()

        // System message survives
        assertEquals(1, instance.getPendingCount<ToastFeature>())

        val activity = MockActivity()
        instance.register<ToastFeature>(activity)
        assertEquals(listOf("System message"), activity.toasts)
    }

    // ── Scenario 6: Multiple rapid registration cycles (quick rotation) ────

    @Test
    fun scenario_rapidRotations_tokensRespectedAcrossCycles() {
        val vm = HomeViewModel(instance)
        vm.loadData()

        // Rotation 1 — Activity recreated without VM reset
        val activity1 = MockActivity()
        instance.register<ToastFeature>(activity1)
        instance.register<LoadingFeature>(activity1)
        // Actions replayed on activity1
        assertEquals(1, activity1.toasts.size)

        // Unregister (simulating rotation)
        instance.unregister<ToastFeature>()
        instance.unregister<LoadingFeature>()

        // VM dispatches more (queued again since no impl)
        vm.loadData()

        // VM is cleared mid-rotation
        vm.onCleared()

        // New activity registers — nothing should replay (vm was cancelled)
        val activity2 = MockActivity()
        instance.register<ToastFeature>(activity2)
        instance.register<LoadingFeature>(activity2)

        assertTrue(activity2.toasts.isEmpty())
        assertFalse(activity2.loadingState)
    }
}

package dev.brewkits.krelay.integration

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Integration tests for the Scope Token API.
 *
 * Covers dispatch + cancelScope + register (replay) end-to-end scenarios,
 * including interactions with priority dispatch and multiple features.
 */
class ScopeTokenIntegrationTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigateTo(screen: String)
    }

    interface AnalyticsFeature : RelayFeature {
        fun track(event: String)
    }

    class MockToast : ToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) { shown.add(message) }
    }

    class MockNav : NavFeature {
        val navigated = mutableListOf<String>()
        override fun navigateTo(screen: String) { navigated.add(screen) }
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("ScopeTokenIntegration")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    // ── cancelScope then register ─────────────────────────────────────────

    @Test
    fun cancelScope_beforeRegister_cancelledActionsNotReplayed() {
        val vmToken = scopedToken()

        instance.dispatch<ToastFeature>(vmToken) { it.show("vm-cancelled") }
        instance.dispatch<ToastFeature> { it.show("stays") }

        instance.cancelScope(vmToken)

        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        assertEquals(listOf("stays"), mock.shown)
    }

    @Test
    fun cancelScope_afterRegister_doesNotAffectAlreadyExecuted() {
        val token = scopedToken()
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        // Dispatch after register → executes immediately, not queued
        instance.dispatch<ToastFeature>(token) { it.show("already executed") }
        assertEquals(listOf("already executed"), mock.shown)

        // cancelScope on an empty queue is a no-op
        instance.cancelScope(token)
        assertEquals(listOf("already executed"), mock.shown)
    }

    // ── multi-feature cancel ───────────────────────────────────────────────

    @Test
    fun cancelScope_removesFromAllFeatures_acrossInstance() {
        val vmToken = scopedToken()

        instance.dispatch<ToastFeature>(vmToken) { it.show("vm-toast") }
        instance.dispatch<NavFeature>(vmToken) { it.navigateTo("vm-home") }
        instance.dispatch<ToastFeature> { it.show("untagged-toast") }

        assertEquals(2, instance.getPendingCount<ToastFeature>())
        assertEquals(1, instance.getPendingCount<NavFeature>())

        instance.cancelScope(vmToken)

        assertEquals(1, instance.getPendingCount<ToastFeature>())
        assertEquals(0, instance.getPendingCount<NavFeature>())

        val mockToast = MockToast()
        val mockNav = MockNav()
        instance.register<ToastFeature>(mockToast)
        instance.register<NavFeature>(mockNav)

        assertEquals(listOf("untagged-toast"), mockToast.shown)
        assertTrue(mockNav.navigated.isEmpty())
    }

    // ── two ViewModels competing for same feature ─────────────────────────

    @Test
    fun twoViewModels_cancelOne_otherPreserved() {
        val vm1Token = scopedToken()
        val vm2Token = scopedToken()

        // VM1 queues 2 actions, VM2 queues 2 actions
        instance.dispatch<ToastFeature>(vm1Token) { it.show("vm1-a") }
        instance.dispatch<ToastFeature>(vm2Token) { it.show("vm2-a") }
        instance.dispatch<ToastFeature>(vm1Token) { it.show("vm1-b") }
        instance.dispatch<ToastFeature>(vm2Token) { it.show("vm2-b") }

        assertEquals(4, instance.getPendingCount<ToastFeature>())

        // VM1 is destroyed
        instance.cancelScope(vm1Token)

        assertEquals(2, instance.getPendingCount<ToastFeature>())

        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        // Only VM2's actions replayed, in order
        assertEquals(listOf("vm2-a", "vm2-b"), mock.shown)
    }

    // ── scope token + priority ─────────────────────────────────────────────

    @Test
    fun dispatchWithPriority_thenCancelScope_removesOnlyTaggedPriorityActions() {
        val token = scopedToken()

        instance.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) { it.show("untagged-critical") }
        instance.dispatch<ToastFeature>(token) { it.show("tagged-normal") }

        instance.cancelScope(token)

        // Only the CRITICAL untagged action remains
        assertEquals(1, instance.getPendingCount<ToastFeature>())

        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertEquals(listOf("untagged-critical"), mock.shown)
    }

    // ── cancel then dispatch again ─────────────────────────────────────────

    @Test
    fun cancelScope_thenDispatchAgainWithSameToken_newActionsQueued() {
        val token = scopedToken()

        instance.dispatch<ToastFeature>(token) { it.show("first-wave") }
        instance.cancelScope(token)  // cancel first wave

        assertEquals(0, instance.getPendingCount<ToastFeature>())

        // Dispatch again with same token
        instance.dispatch<ToastFeature>(token) { it.show("second-wave") }

        assertEquals(1, instance.getPendingCount<ToastFeature>())

        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertEquals(listOf("second-wave"), mock.shown)
    }

    // ── singleton API ─────────────────────────────────────────────────────

    @Test
    fun singleton_scopeToken_cancelAndReplay() {
        val token = scopedToken()

        KRelay.dispatch<ToastFeature>(token) { it.show("cancelled") }
        KRelay.dispatch<ToastFeature> { it.show("survives") }

        KRelay.cancelScope(token)

        val mock = MockToast()
        KRelay.register<ToastFeature>(mock)

        assertEquals(listOf("survives"), mock.shown)
    }

    // ── high volume cancellation ───────────────────────────────────────────

    @Test
    fun largeQueue_cancelScope_removesAllTaggedEntries() {
        val vmToken = scopedToken()

        // 50 tagged + 50 untagged
        repeat(50) { i ->
            instance.dispatch<ToastFeature>(vmToken) { it.show("vm-$i") }
            instance.dispatch<ToastFeature> { it.show("other-$i") }
        }

        assertEquals(100, instance.getPendingCount<ToastFeature>())

        instance.cancelScope(vmToken)

        assertEquals(50, instance.getPendingCount<ToastFeature>())

        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertEquals(50, mock.shown.size)
        assertTrue(mock.shown.all { it.startsWith("other-") })
    }
}

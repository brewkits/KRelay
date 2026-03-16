package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests for the scopeToken + cancelScope feature.
 *
 * Covers:
 * - dispatch with token queues correctly
 * - cancelScope removes only tagged actions, leaves others
 * - cancelScope across multiple feature types
 * - dispatch with token executes immediately when impl is alive (no queuing)
 * - scopedToken() uniqueness
 * - cancelScope on singleton API
 */
class ScopeTokenTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigateTo(screen: String)
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
        instance = KRelay.create("ScopeTokenTestScope")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.clearInstanceRegistry()
    }

    // ──────────────────────────────────────────────────────────────────
    // Basic queuing with token
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testDispatchWithToken_queuesWhenNoImpl() {
        val token = scopedToken()
        instance.dispatch<ToastFeature>(token) { it.show("Hello") }

        assertEquals(1, instance.getPendingCount<ToastFeature>())
    }

    @Test
    fun testDispatchWithToken_executesImmediatelyWhenImplAlive() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        val token = scopedToken()
        instance.dispatch<ToastFeature>(token) { it.show("Immediate") }

        // Executed immediately, nothing queued
        assertEquals(0, instance.getPendingCount<ToastFeature>())
        assertEquals(listOf("Immediate"), mock.shown)
    }

    // ──────────────────────────────────────────────────────────────────
    // cancelScope — selective removal
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testCancelScope_removesOnlyTaggedActions() {
        val vmToken = scopedToken()
        val otherToken = scopedToken()

        instance.dispatch<ToastFeature>(vmToken) { it.show("from VM") }
        instance.dispatch<ToastFeature>(otherToken) { it.show("from other") }
        instance.dispatch<ToastFeature> { it.show("no token") }

        assertEquals(3, instance.getPendingCount<ToastFeature>())

        instance.cancelScope(vmToken)

        // Only the vmToken action removed — 2 remain
        assertEquals(2, instance.getPendingCount<ToastFeature>())
    }

    @Test
    fun testCancelScope_acrossMultipleFeatureTypes() {
        val token = scopedToken()

        instance.dispatch<ToastFeature>(token) { it.show("toast") }
        instance.dispatch<NavFeature>(token) { it.navigateTo("home") }
        instance.dispatch<ToastFeature> { it.show("untagged toast") }

        assertEquals(2, instance.getPendingCount<ToastFeature>())
        assertEquals(1, instance.getPendingCount<NavFeature>())

        instance.cancelScope(token)

        // Both token-tagged actions removed across different feature types
        assertEquals(1, instance.getPendingCount<ToastFeature>())
        assertEquals(0, instance.getPendingCount<NavFeature>())
    }

    @Test
    fun testCancelScope_unknownToken_noOp() {
        instance.dispatch<ToastFeature> { it.show("stays") }

        instance.cancelScope("nonexistent-token")

        assertEquals(1, instance.getPendingCount<ToastFeature>())
    }

    @Test
    fun testCancelScope_emptyQueue_noOp() {
        instance.cancelScope("any-token")
        assertEquals(0, instance.getPendingCount<ToastFeature>())
    }

    @Test
    fun testCancelScope_doesNotPreventReplayForOtherActions() {
        val token = scopedToken()

        instance.dispatch<ToastFeature>(token) { it.show("cancelled") }
        instance.dispatch<ToastFeature> { it.show("replayed") }

        instance.cancelScope(token)

        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        assertEquals(listOf("replayed"), mock.shown)
    }

    // ──────────────────────────────────────────────────────────────────
    // scopedToken() uniqueness
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testScopedToken_isUnique() {
        val tokens = (1..100).map { scopedToken() }.toSet()
        assertEquals(100, tokens.size)
    }

    @Test
    fun testScopedToken_containsPrefix() {
        val token = scopedToken()
        assertTrue(token.startsWith("krelay-"), "Token should start with 'krelay-': $token")
    }

    // ──────────────────────────────────────────────────────────────────
    // Singleton API
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testSingleton_cancelScope_works() {
        val token = scopedToken()

        KRelay.dispatch<ToastFeature>(token) { it.show("singleton") }
        assertEquals(1, KRelay.getPendingCount<ToastFeature>())

        KRelay.cancelScope(token)
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())
    }
}

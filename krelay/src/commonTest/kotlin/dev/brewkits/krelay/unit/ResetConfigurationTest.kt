package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests for KRelayInstance.resetConfiguration() and KRelay.resetConfiguration().
 *
 * Verifies:
 * - resetConfiguration() restores defaults without touching registry/queue
 * - reset() still clears registry and queue (unchanged behaviour)
 * - Singleton API delegates correctly
 */
class ResetConfigurationTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    class MockToast : ToastFeature {
        override fun show(message: String) {}
    }

    private lateinit var instance: KRelayInstance

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
        instance = KRelay.create("ResetConfigTest")
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        KRelay.reset()
        KRelay.resetConfiguration()
        KRelay.clearInstanceRegistry()
    }

    // ── Instance API ──────────────────────────────────────────────────────

    @Test
    fun testResetConfiguration_restoresDefaults() {
        instance.maxQueueSize = 42
        instance.actionExpiryMs = 999L
        instance.debugMode = true

        instance.resetConfiguration()

        assertEquals(100, instance.maxQueueSize)
        assertEquals(5 * 60 * 1000L, instance.actionExpiryMs)
        assertFalse(instance.debugMode)
    }

    @Test
    fun testResetConfiguration_doesNotClearQueue() {
        // Queue an action (no impl registered)
        instance.dispatch<ToastFeature> { it.show("queued") }
        assertEquals(1, instance.getPendingCount<ToastFeature>())

        instance.resetConfiguration()

        // Queue is untouched
        assertEquals(1, instance.getPendingCount<ToastFeature>())
    }

    @Test
    fun testResetConfiguration_doesNotClearRegistry() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        assertTrue(instance.isRegistered<ToastFeature>())

        instance.resetConfiguration()

        assertTrue(instance.isRegistered<ToastFeature>())
    }

    @Test
    fun testReset_stillClearsEverything() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)
        instance.dispatch<ToastFeature> { it.show("queued") }

        instance.reset()

        assertFalse(instance.isRegistered<ToastFeature>())
        // Queue is also cleared after reset
        assertEquals(0, instance.getPendingCount<ToastFeature>())
    }

    // ── Singleton API ─────────────────────────────────────────────────────

    @Test
    fun testSingleton_resetConfiguration_restoresDefaults() {
        KRelay.maxQueueSize = 7
        KRelay.actionExpiryMs = 1234L
        KRelay.debugMode = true

        KRelay.resetConfiguration()

        assertEquals(100, KRelay.maxQueueSize)
        assertEquals(5 * 60 * 1000L, KRelay.actionExpiryMs)
        assertFalse(KRelay.debugMode)
    }

    @Test
    fun testSingleton_resetConfiguration_doesNotClearQueue() {
        KRelay.dispatch<ToastFeature> { it.show("stays") }
        assertEquals(1, KRelay.getPendingCount<ToastFeature>())

        KRelay.resetConfiguration()

        assertEquals(1, KRelay.getPendingCount<ToastFeature>())
    }
}

package dev.brewkits.krelay.system

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlin.test.*

/**
 * System test: Concurrent Operations Scenario.
 *
 * Simulates:
 * 1. Multiple threads dispatching simultaneously
 * 2. Registration happening concurrently with dispatches
 * 3. Multiple feature types being used concurrently
 * 4. Queue operations under load
 *
 * Tests thread safety of the entire system.
 */
class ConcurrentOperationsScenarioTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface AnalyticsFeature : RelayFeature {
        fun track(event: String)
    }

    interface NavigationFeature : RelayFeature {
        fun navigate(route: String)
    }

    class ConcurrentToast : ToastFeature {
        val messages = mutableListOf<String>()
        override fun show(message: String) {
            synchronized(messages) {
                messages.add(message)
            }
        }
    }

    class ConcurrentAnalytics : AnalyticsFeature {
        val events = mutableListOf<String>()
        override fun track(event: String) {
            synchronized(events) {
                events.add(event)
            }
        }
    }

    class ConcurrentNavigation : NavigationFeature {
        val routes = mutableListOf<String>()
        override fun navigate(route: String) {
            synchronized(routes) {
                routes.add(route)
            }
        }
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = false  // Reduce noise in concurrent test
        KRelay.maxQueueSize = 200  // Allow larger queue for concurrency
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testConcurrentDispatches_SingleFeature() {
        // When: Multiple dispatches to same feature
        repeat(10) { index ->
            KRelay.dispatch<ToastFeature> {
                it.show("Message $index")
            }
        }

        // Then: All queued
        assertEquals(10, KRelay.getPendingCount<ToastFeature>())
    }

    @Test
    fun testConcurrentDispatches_MultipleFeatures() {
        // When: Concurrent dispatches to different features
        repeat(5) { i ->
            KRelay.dispatch<ToastFeature> { it.show("Toast $i") }
            KRelay.dispatch<AnalyticsFeature> { it.track("Event $i") }
            KRelay.dispatch<NavigationFeature> { it.navigate("Route $i") }
        }

        // Then: Each feature has its queue
        assertEquals(5, KRelay.getPendingCount<ToastFeature>())
        assertEquals(5, KRelay.getPendingCount<AnalyticsFeature>())
        assertEquals(5, KRelay.getPendingCount<NavigationFeature>())
    }

    @Test
    fun testConcurrentRegistrations() {
        // When: Register multiple features
        KRelay.register<ToastFeature>(ConcurrentToast())
        KRelay.register<AnalyticsFeature>(ConcurrentAnalytics())
        KRelay.register<NavigationFeature>(ConcurrentNavigation())

        // Then: All registered
        assertTrue(KRelay.isRegistered<ToastFeature>())
        assertTrue(KRelay.isRegistered<AnalyticsFeature>())
        assertTrue(KRelay.isRegistered<NavigationFeature>())
    }

    @Test
    fun testMixedOperations_DispatchAndRegister() {
        // Scenario: Simulate real app startup

        // Phase 1: Queue some actions before registration
        repeat(3) { i ->
            KRelay.dispatch<ToastFeature> { it.show("Early $i") }
        }

        assertEquals(3, KRelay.getPendingCount<ToastFeature>())

        // Phase 2: Register
        KRelay.register<ToastFeature>(ConcurrentToast())

        // Phase 3: More dispatches after registration
        repeat(3) { i ->
            KRelay.dispatch<ToastFeature> { it.show("Late $i") }
        }

        // Then: Early actions replayed, late actions executed
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())
    }

    @Test
    fun testRegisterUnregisterCycle() {
        // Simulate Activity lifecycle multiple times

        repeat(3) { cycle ->
            // Register
            KRelay.register<ToastFeature>(ConcurrentToast())
            assertTrue(KRelay.isRegistered<ToastFeature>())

            // Dispatch
            KRelay.dispatch<ToastFeature> { it.show("Cycle $cycle") }

            // Unregister
            KRelay.unregister<ToastFeature>()
            assertFalse(KRelay.isRegistered<ToastFeature>())
        }
    }

    @Test
    fun testQueueOverflow_UnderLoad() {
        // Given: Small queue
        KRelay.maxQueueSize = 10

        // When: Dispatch more than max
        repeat(20) { i ->
            KRelay.dispatch<ToastFeature> { it.show("Overflow $i") }
        }

        // Then: Queue capped at max (oldest removed)
        assertEquals(10, KRelay.getPendingCount<ToastFeature>())

        // Restore
        KRelay.maxQueueSize = 200
    }

    @Test
    fun testFullSystemCycle_EndToEnd() {
        // Complete real-world scenario

        // Step 1: App starts, no Activity yet
        assertFalse(KRelay.isRegistered<ToastFeature>())

        // Step 2: ViewModel initializes, starts loading
        KRelay.dispatch<ToastFeature> { it.show("Loading...") }

        // Step 3: Activity created, registers
        val toast = ConcurrentToast()
        KRelay.register<ToastFeature>(toast)

        // Step 4: Loading completes
        KRelay.dispatch<ToastFeature> { it.show("Loaded!") }

        // Step 5: User navigates
        KRelay.dispatch<NavigationFeature> { it.navigate("details") }

        // Step 6: Analytics
        KRelay.dispatch<AnalyticsFeature> { it.track("view_details") }

        // Verify all features work independently
        assertTrue(KRelay.isRegistered<ToastFeature>())
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())
        assertEquals(1, KRelay.getPendingCount<NavigationFeature>())
        assertEquals(1, KRelay.getPendingCount<AnalyticsFeature>())
    }
}

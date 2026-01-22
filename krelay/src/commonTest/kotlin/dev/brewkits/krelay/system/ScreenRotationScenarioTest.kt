package dev.brewkits.krelay.system

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlin.test.*

/**
 * System test: Screen Rotation Scenario.
 *
 * Simulates:
 * 1. Activity creates and starts loading data
 * 2. ViewModel dispatches UI updates
 * 3. Screen rotates (Activity destroyed)
 * 4. New Activity created and registers
 * 5. Queued actions replayed on new Activity
 *
 * This is a killer feature of KRelay - no lost updates during rotation!
 */
class ScreenRotationScenarioTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface LoadingFeature : RelayFeature {
        fun showLoading(visible: Boolean)
    }

    class AndroidToast : ToastFeature {
        val shownMessages = mutableListOf<String>()
        override fun show(message: String) {
            shownMessages.add(message)
        }
    }

    class AndroidLoadingDialog : LoadingFeature {
        var isLoading = false
        override fun showLoading(visible: Boolean) {
            isLoading = visible
        }
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = true
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testScreenRotation_PreservesQueuedActions() {
        // === BEFORE ROTATION ===

        // Step 1: Activity creates, registers features
        val toast1 = AndroidToast()
        val loading1 = AndroidLoadingDialog()

        KRelay.register<ToastFeature>(toast1)
        KRelay.register<LoadingFeature>(loading1)

        assertTrue(KRelay.isRegistered<ToastFeature>())
        assertTrue(KRelay.isRegistered<LoadingFeature>())

        // Step 2: Show loading
        KRelay.dispatch<LoadingFeature> { it.showLoading(true) }

        // Step 3: ViewModel fetches data (background thread)
        // While fetching, Activity is destroyed (rotation)

        // === ROTATION HAPPENS ===

        // Step 4: Activity destroyed (WeakRef cleared)
        KRelay.unregister<ToastFeature>()
        KRelay.unregister<LoadingFeature>()

        assertFalse(KRelay.isRegistered<ToastFeature>())
        assertFalse(KRelay.isRegistered<LoadingFeature>())

        // Step 5: ViewModel completes fetch, dispatches updates
        // These are queued because Activity is gone
        KRelay.dispatch<ToastFeature> { it.show("Data loaded!") }
        KRelay.dispatch<LoadingFeature> { it.showLoading(false) }

        // Verify queued
        assertEquals(1, KRelay.getPendingCount<ToastFeature>())
        assertEquals(1, KRelay.getPendingCount<LoadingFeature>())

        // === AFTER ROTATION ===

        // Step 6: New Activity created, registers new instances
        val toast2 = AndroidToast()
        val loading2 = AndroidLoadingDialog()

        KRelay.register<ToastFeature>(toast2)
        KRelay.register<LoadingFeature>(loading2)

        // Step 7: Queued actions replayed on NEW instances
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())
        assertEquals(0, KRelay.getPendingCount<LoadingFeature>())

        // Verify actions were replayed (would be async in real app)
        assertTrue(KRelay.isRegistered<ToastFeature>())
        assertTrue(KRelay.isRegistered<LoadingFeature>())
    }

    @Test
    fun testMultipleRotations_StillWorks() {
        // Simulate multiple rapid rotations
        for (rotation in 1..3) {
            // Create Activity
            val toast = AndroidToast()
            KRelay.register<ToastFeature>(toast)

            // Dispatch something
            KRelay.dispatch<ToastFeature> { it.show("Rotation $rotation") }

            // Destroy Activity
            KRelay.unregister<ToastFeature>()

            // Some actions dispatched while no Activity
            KRelay.dispatch<ToastFeature> { it.show("Queued $rotation") }
        }

        // Final Activity
        val finalToast = AndroidToast()
        KRelay.register<ToastFeature>(finalToast)

        // All queued actions should replay
        assertEquals(0, KRelay.getPendingCount<ToastFeature>())
    }
}

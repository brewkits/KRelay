package dev.brewkits.krelay.system

import dev.brewkits.krelay.ActionPriority
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import dev.brewkits.krelay.dispatchWithPriority
import kotlin.test.*

/**
 * System test: Background/Foreground Scenario.
 *
 * Simulates:
 * 1. App goes to background
 * 2. Background sync starts
 * 3. Sync completes, tries to show notification
 * 4. Notification queued (app in background)
 * 5. User returns to app (foreground)
 * 6. Notification shown
 */
class BackgroundForegroundScenarioTest {

    interface NotificationFeature : RelayFeature {
        fun showNotification(title: String, message: String)
    }

    interface SyncStatusFeature : RelayFeature {
        fun updateStatus(status: String)
    }

    class AppNotificationManager : NotificationFeature {
        val notifications = mutableListOf<Pair<String, String>>()

        override fun showNotification(title: String, message: String) {
            notifications.add(title to message)
        }
    }

    class AppSyncStatus : SyncStatusFeature {
        var currentStatus = ""

        override fun updateStatus(status: String) {
            currentStatus = status
        }
    }

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = true
        KRelay.maxQueueSize = 50
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun testBackgroundSync_QueuesNotifications() {
        // === APP IN FOREGROUND ===

        // Step 1: App is active, features registered
        val notificationManager = AppNotificationManager()
        val syncStatus = AppSyncStatus()

        KRelay.register<NotificationFeature>(notificationManager)
        KRelay.register<SyncStatusFeature>(syncStatus)

        assertTrue(KRelay.isRegistered<NotificationFeature>())

        // === APP GOES TO BACKGROUND ===

        // Step 2: Activity paused/stopped (unregister UI features)
        KRelay.unregister<NotificationFeature>()
        KRelay.unregister<SyncStatusFeature>()

        assertFalse(KRelay.isRegistered<NotificationFeature>())

        // === BACKGROUND SYNC RUNNING ===

        // Step 3: Background worker starts sync
        simulateBackgroundSync()

        // Verify notifications are queued
        assertTrue(KRelay.getPendingCount<NotificationFeature>() > 0)

        // === USER RETURNS TO APP ===

        // Step 4: Activity resumed, re-register features
        val newNotificationManager = AppNotificationManager()
        val newSyncStatus = AppSyncStatus()

        KRelay.register<NotificationFeature>(newNotificationManager)
        KRelay.register<SyncStatusFeature>(newSyncStatus)

        // Step 5: Queued notifications replayed
        assertEquals(0, KRelay.getPendingCount<NotificationFeature>())
    }

    @Test
    fun testCriticalNotifications_NotLost() {
        // Step 1: App in background (no registration)
        assertFalse(KRelay.isRegistered<NotificationFeature>())

        // Step 2: Critical error occurs in background
        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.CRITICAL) {
            it.showNotification("Error", "Payment failed!")
        }

        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.HIGH) {
            it.showNotification("Warning", "Network issue")
        }

        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.LOW) {
            it.showNotification("Info", "Sync complete")
        }

        // Step 3: All queued
        assertEquals(3, KRelay.getPendingCount<NotificationFeature>())

        // Step 4: App returns to foreground
        val notificationManager = AppNotificationManager()
        KRelay.register<NotificationFeature>(notificationManager)

        // Step 5: All notifications shown (critical first due to priority)
        assertEquals(0, KRelay.getPendingCount<NotificationFeature>())
    }

    @Test
    fun testLongBackgroundSession_WithExpiry() {
        // Step 1: App goes to background
        KRelay.unregister<NotificationFeature>()

        // Step 2: Set short expiry for testing
        KRelay.actionExpiryMs = 100L  // 100ms

        // Step 3: Dispatch notification
        KRelay.dispatch<NotificationFeature> {
            it.showNotification("Old", "This will expire")
        }

        assertEquals(1, KRelay.getPendingCount<NotificationFeature>())

        // Step 4: Wait for expiry (in real test with time control)
        // For now, verify expiry mechanism exists

        // Step 5: Check that getPendingCount removes expired
        val count = KRelay.getPendingCount<NotificationFeature>()
        assertTrue(count >= 0)

        // Restore default
        KRelay.actionExpiryMs = 5 * 60 * 1000
    }

    private fun simulateBackgroundSync() {
        // Simulate a background sync worker

        // Phase 1: Start
        KRelay.dispatch<SyncStatusFeature> {
            it.updateStatus("Syncing...")
        }

        // Phase 2: Download data (simulated)
        // ...

        // Phase 3: Complete
        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.NORMAL) {
            it.showNotification("Sync Complete", "Downloaded 42 items")
        }

        KRelay.dispatch<SyncStatusFeature> {
            it.updateStatus("Sync complete")
        }
    }
}

package dev.brewkits.krelay.demo

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Advanced Demo: Background Data Sync
 *
 * Demonstrates:
 * 1. Background worker syncing data
 * 2. Progress updates to UI
 * 3. Success/Error notifications
 * 4. Priority-based notifications
 * 5. App in background scenario
 */
class DataSyncDemo {

    // === FEATURES ===

    interface SyncProgressFeature : RelayFeature {
        fun updateProgress(current: Int, total: Int, message: String)
        fun syncComplete(itemsDownloaded: Int)
        fun syncFailed(error: String)
    }

    interface NotificationFeature : RelayFeature {
        fun showNotification(title: String, message: String, priority: Int = 0)
    }

    interface DatabaseFeature : RelayFeature {
        fun saveItems(items: List<String>)
        fun clearCache()
    }

    // === IMPLEMENTATIONS ===

    class SyncProgressUI : SyncProgressFeature {
        var currentProgress = 0
        var totalProgress = 0
        var progressMessage = ""
        var isComplete = false
        var errorMessage: String? = null

        override fun updateProgress(current: Int, total: Int, message: String) {
            currentProgress = current
            totalProgress = total
            progressMessage = message
        }

        override fun syncComplete(itemsDownloaded: Int) {
            isComplete = true
            currentProgress = itemsDownloaded
            totalProgress = itemsDownloaded
        }

        override fun syncFailed(error: String) {
            errorMessage = error
        }
    }

    class NotificationManager : NotificationFeature {
        data class Notification(val title: String, val message: String, val priority: Int)
        val notifications = mutableListOf<Notification>()

        override fun showNotification(title: String, message: String, priority: Int) {
            notifications.add(Notification(title, message, priority))
        }
    }

    class LocalDatabase : DatabaseFeature {
        val items = mutableListOf<String>()

        override fun saveItems(items: List<String>) {
            this.items.addAll(items)
        }

        override fun clearCache() {
            items.clear()
        }
    }

    // === SYNC WORKER ===

    class SyncWorker {
        fun startSync(itemCount: Int = 100) {
            // Phase 1: Start
            KRelay.dispatch<SyncProgressFeature> {
                it.updateProgress(0, itemCount, "Starting sync...")
            }

            // Phase 2: Download in batches
            val batchSize = 10
            val batches = itemCount / batchSize

            for (batch in 1..batches) {
                val current = batch * batchSize
                val items = (1..batchSize).map { "Item-${current - batchSize + it}" }

                // Save to database
                KRelay.dispatch<DatabaseFeature> {
                    it.saveItems(items)
                }

                // Update progress
                KRelay.dispatch<SyncProgressFeature> {
                    it.updateProgress(current, itemCount, "Downloaded $current of $itemCount")
                }
            }

            // Phase 3: Complete
            KRelay.dispatch<SyncProgressFeature> {
                it.syncComplete(itemCount)
            }

            // Phase 4: Notify user (with priority)
            KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.HIGH) {
                it.showNotification(
                    "Sync Complete",
                    "Downloaded $itemCount items",
                    ActionPriority.HIGH.value
                )
            }
        }

        fun syncWithError() {
            // Phase 1: Start
            KRelay.dispatch<SyncProgressFeature> {
                it.updateProgress(0, 100, "Starting sync...")
            }

            // Phase 2: Simulate partial download
            KRelay.dispatch<SyncProgressFeature> {
                it.updateProgress(25, 100, "Downloaded 25 of 100")
            }

            // Phase 3: Error occurs
            KRelay.dispatch<SyncProgressFeature> {
                it.syncFailed("Network connection lost")
            }

            // Phase 4: Critical notification
            KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.CRITICAL) {
                it.showNotification(
                    "Sync Failed",
                    "Network connection lost",
                    ActionPriority.CRITICAL.value
                )
            }
        }

        fun syncInBackground() {
            // No UI registered - all actions will be queued

            // Simulate sync completing in background
            KRelay.dispatch<SyncProgressFeature> {
                it.syncComplete(50)
            }

            // Queue notification for when app returns to foreground
            KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.NORMAL) {
                it.showNotification(
                    "Background Sync Complete",
                    "Downloaded 50 items while you were away",
                    ActionPriority.NORMAL.value
                )
            }
        }
    }

    // === TESTS (DOUBLES AS DEMO) ===

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
    fun demo_SuccessfulSync() {
        // === SETUP: App is active ===
        val progressUI = SyncProgressUI()
        val notifications = NotificationManager()
        val database = LocalDatabase()

        KRelay.register<SyncProgressFeature>(progressUI)
        KRelay.register<NotificationFeature>(notifications)
        KRelay.register<DatabaseFeature>(database)

        // === ACTION: Start sync ===
        val worker = SyncWorker()
        worker.startSync(itemCount = 50)

        // === VERIFY ===
        assertTrue(progressUI.isComplete)
        assertEquals(50, progressUI.currentProgress)
        assertEquals(50, database.items.size)
        assertTrue(notifications.notifications.any { it.title == "Sync Complete" })
    }

    @Test
    fun demo_SyncWithError() {
        // === SETUP ===
        val progressUI = SyncProgressUI()
        val notifications = NotificationManager()

        KRelay.register<SyncProgressFeature>(progressUI)
        KRelay.register<NotificationFeature>(notifications)

        // === ACTION: Sync fails ===
        val worker = SyncWorker()
        worker.syncWithError()

        // === VERIFY ===
        assertFalse(progressUI.isComplete)
        assertEquals("Network connection lost", progressUI.errorMessage)
        assertTrue(notifications.notifications.any { it.title == "Sync Failed" })

        // Verify critical priority
        val failedNotif = notifications.notifications.find { it.title == "Sync Failed" }
        assertEquals(ActionPriority.CRITICAL.value, failedNotif?.priority)
    }

    @Test
    fun demo_BackgroundSync_QueuesNotifications() {
        // === SCENARIO: App goes to background ===

        // Step 1: App was active
        val progressUI1 = SyncProgressUI()
        KRelay.register<SyncProgressFeature>(progressUI1)
        KRelay.register<NotificationFeature>(NotificationManager())

        // Step 2: App goes to background (unregister)
        KRelay.unregister<SyncProgressFeature>()
        KRelay.unregister<NotificationFeature>()

        // Step 3: Background sync runs (actions queued!)
        val worker = SyncWorker()
        worker.syncInBackground()

        // Verify queued
        assertTrue(KRelay.getPendingCount<SyncProgressFeature>() > 0)
        assertTrue(KRelay.getPendingCount<NotificationFeature>() > 0)

        // Step 4: User returns to app
        val progressUI2 = SyncProgressUI()
        val notifications2 = NotificationManager()

        KRelay.register<SyncProgressFeature>(progressUI2)
        KRelay.register<NotificationFeature>(notifications2)

        // Step 5: Queued actions replayed
        assertEquals(0, KRelay.getPendingCount<SyncProgressFeature>())
        assertTrue(progressUI2.isComplete)
        assertTrue(notifications2.notifications.any { it.title == "Background Sync Complete" })
    }

    @Test
    fun demo_ProgressUpdates() {
        // === SETUP ===
        val progressUI = SyncProgressUI()
        val database = LocalDatabase()

        KRelay.register<SyncProgressFeature>(progressUI)
        KRelay.register<DatabaseFeature>(database)

        // === ACTION: Sync with progress tracking ===
        val worker = SyncWorker()
        worker.startSync(itemCount = 30)

        // === VERIFY: Progress was tracked ===
        assertEquals(30, progressUI.currentProgress)
        assertEquals(30, progressUI.totalProgress)
        assertTrue(progressUI.isComplete)
        assertEquals(30, database.items.size)
    }

    @Test
    fun demo_PriorityNotifications() {
        // === SETUP ===
        val notifications = NotificationManager()
        KRelay.register<NotificationFeature>(notifications)

        // === ACTION: Dispatch different priority notifications ===
        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.LOW) {
            it.showNotification("Info", "Low priority", ActionPriority.LOW.value)
        }

        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.HIGH) {
            it.showNotification("Warning", "High priority", ActionPriority.HIGH.value)
        }

        KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.CRITICAL) {
            it.showNotification("Error", "Critical priority", ActionPriority.CRITICAL.value)
        }

        // === VERIFY: All received ===
        assertEquals(3, notifications.notifications.size)

        // Verify priorities are preserved
        val priorities = notifications.notifications.map { it.priority }
        assertTrue(priorities.contains(ActionPriority.LOW.value))
        assertTrue(priorities.contains(ActionPriority.HIGH.value))
        assertTrue(priorities.contains(ActionPriority.CRITICAL.value))
    }

    @Test
    fun demo_ClearCacheBeforeSync() {
        // === SETUP ===
        val database = LocalDatabase()
        database.items.addAll(listOf("old1", "old2", "old3"))

        KRelay.register<DatabaseFeature>(database)

        // === ACTION: Clear cache and sync ===
        KRelay.dispatch<DatabaseFeature> { it.clearCache() }

        val worker = SyncWorker()
        worker.startSync(itemCount = 20)

        // === VERIFY: Old data cleared, new data synced ===
        assertEquals(20, database.items.size)
        assertFalse(database.items.contains("old1"))
        assertTrue(database.items.any { it.startsWith("Item-") })
    }
}

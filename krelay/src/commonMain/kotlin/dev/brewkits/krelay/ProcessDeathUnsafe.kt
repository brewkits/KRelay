package dev.brewkits.krelay

/**
 * Marks APIs that use KRelay's in-memory queue, which **does not survive process death**.
 *
 * ## ⚠️ Critical Limitation: No Process Death Survival
 *
 * KRelay stores pending actions (lambda functions) in **RAM only**. When Android OS kills
 * your app process (low memory, user swipes away, etc.), all queued actions are **lost forever**.
 *
 * ### Why This Happens
 * - Lambda functions cannot be serialized to disk
 * - When process restarts, KRelay starts with an empty queue
 * - Any actions dispatched before process death are **permanently lost**
 *
 * ## ✅ Safe Use Cases (UI-Only Operations)
 * ```kotlin
 * // GOOD: Showing a toast (if lost, user just doesn't see it)
 * @OptIn(ProcessDeathUnsafe::class)
 * KRelay.dispatch<ToastFeature> {
 *     it.show("Welcome back!")
 * }
 *
 * // GOOD: Navigation (if lost, user stays on current screen)
 * @OptIn(ProcessDeathUnsafe::class)
 * KRelay.dispatch<NavigationFeature> {
 *     it.goToHome()
 * }
 *
 * // GOOD: Haptic feedback (if lost, no vibration - acceptable)
 * @OptIn(ProcessDeathUnsafe::class)
 * KRelay.dispatch<HapticFeature> {
 *     it.vibrate(100)
 * }
 * ```
 *
 * ## ❌ DANGEROUS Use Cases (Critical Operations)
 * ```kotlin
 * // DANGEROUS: Banking transaction
 * KRelay.dispatch<PaymentFeature> {
 *     it.processPayment(1000.0) // ⚠️ WILL BE LOST IF PROCESS DIES!
 * }
 * // FIX: Use WorkManager for guaranteed execution
 * val work = OneTimeWorkRequestBuilder<PaymentWorker>().build()
 * WorkManager.getInstance(context).enqueue(work)
 *
 * // DANGEROUS: Critical analytics event
 * KRelay.dispatch<AnalyticsFeature> {
 *     it.trackPurchase(orderId) // ⚠️ Event lost if process dies
 * }
 * // FIX: Use persistent queue (Room DB + WorkManager)
 * analyticsRepository.queueEvent(PurchaseEvent(orderId))
 *
 * // DANGEROUS: Upload user-generated content
 * KRelay.dispatch<UploadFeature> {
 *     it.uploadPhoto(photoData) // ⚠️ Upload lost if process dies
 * }
 * // FIX: Use UploadWorker with WorkManager
 * ```
 *
 * ## Decision Matrix: Should You Use KRelay?
 *
 * | Operation Type | Use KRelay? | Reason |
 * |----------------|-------------|--------|
 * | Toast/Snackbar | ✅ Yes | If lost, user just doesn't see feedback |
 * | Navigation | ✅ Yes | If lost, user stays on current screen |
 * | Haptics/Sounds | ✅ Yes | If lost, no vibration - acceptable |
 * | Permission Requests | ✅ Yes | User can retry if lost |
 * | In-app Notifications | ✅ Yes | Will be fetched again next time |
 * | Banking/Payment | ❌ NO | **MUST NEVER BE LOST** |
 * | File Upload | ❌ NO | User data must be preserved |
 * | Critical Analytics | ❌ NO | Business metrics must be accurate |
 * | Data Persistence | ❌ NO | Use Room/DataStore |
 *
 * ## Alternatives for Critical Operations
 *
 * **For operations that MUST survive process death:**
 * - **WorkManager** (Android): Guaranteed background execution
 * - **SavedStateHandle**: Preserve ViewModel state across process death
 * - **Room/SQLite**: Persist data to disk
 * - **DataStore**: Persist key-value pairs
 * - **Persistent Event Queue**: Custom queue with disk storage
 *
 * ## Real-World Process Death Scenarios
 *
 * Process death happens more often than you think:
 * 1. **Low Memory**: OS kills background apps to free RAM
 * 2. **User Swipes Away**: Removes app from Recent Apps
 * 3. **Developer Options**: "Don't keep activities" enabled during testing
 * 4. **Force Stop**: User force-stops app in Settings
 * 5. **OS Updates**: System kills all apps during updates
 *
 * ## Testing Process Death
 *
 * To verify your app handles process death correctly:
 * ```bash
 * # Enable "Don't keep activities" in Developer Options
 * adb shell settings put global always_finish_activities 1
 *
 * # Or manually kill process
 * adb shell am kill com.yourapp.package
 *
 * # Then reopen app - KRelay queue will be EMPTY
 * ```
 *
 * ## Summary
 *
 * - ✅ Use KRelay for **UI feedback** that's acceptable to lose
 * - ❌ Never use KRelay for **critical business operations**
 * - 🔄 For critical ops: Use WorkManager, Room, or SavedStateHandle
 *
 * @see [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
 * @see [SavedStateHandle Guide](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
 */
@RequiresOptIn(
    message = "KRelay queue is lost on process death. Only use for UI feedback (toast/navigation). " +
            "For critical operations (payment/upload), use WorkManager. " +
            "See @ProcessDeathUnsafe documentation for details.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ProcessDeathUnsafe

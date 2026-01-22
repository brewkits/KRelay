package dev.brewkits.krelay.platform

import android.content.Context
import android.widget.Toast
import dev.brewkits.krelay.samples.NotificationBridge

/**
 * Android implementation of NotificationBridge.
 *
 * This is a simplified demo implementation using Toast.
 * In production, you would use:
 * - Custom in-app banner views for showInAppNotification
 * - NotificationCompat.Builder for showSystemNotification
 */
class AndroidNotificationBridge(private val context: Context) : NotificationBridge {

    override fun showInAppNotification(title: String, message: String, duration: Int) {
        // Simplified: Show as toast with title + message
        val fullMessage = "$title\n$message"
        Toast.makeText(context, fullMessage, Toast.LENGTH_LONG).show()

        // In production, you might show a custom banner view:
        // - Create a custom View with title, message, dismiss button
        // - Add to WindowManager as overlay
        // - Auto-dismiss after duration seconds
    }

    override fun showSystemNotification(title: String, message: String) {
        // Simplified: Show as toast
        val fullMessage = "🔔 $title\n$message"
        Toast.makeText(context, fullMessage, Toast.LENGTH_LONG).show()

        // In production, you would use NotificationCompat.Builder:
        /*
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        */
    }
}

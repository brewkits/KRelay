package dev.brewkits.krelay.integrations

import android.util.Log
import dev.brewkits.krelay.samples.AnalyticsFeature

/**
 * Real Android Analytics implementation.
 *
 * In production, this would integrate with:
 * - Firebase Analytics
 * - Mixpanel
 * - Amplitude
 * - Google Analytics
 *
 * For demo purposes, this logs to Logcat.
 */
class AndroidAnalyticsImpl : AnalyticsFeature {

    companion object {
        private const val TAG = "Analytics"
    }

    override fun track(eventName: String) {
        Log.d(TAG, "Event tracked: $eventName")
    }

    override fun track(eventName: String, parameters: Map<String, Any>) {
        Log.d(TAG, "Event tracked: $eventName with ${parameters.size} parameters")
        parameters.forEach { (key, value) ->
            Log.d(TAG, "  - $key: $value")
        }
    }

    override fun setUserProperty(key: String, value: String) {
        Log.d(TAG, "User property set: $key = $value")
    }

    override fun setUserId(userId: String) {
        Log.d(TAG, "User ID set: $userId")
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        if (screenClass != null) {
            Log.d(TAG, "Screen view: $screenName ($screenClass)")
        } else {
            Log.d(TAG, "Screen view: $screenName")
        }
    }
}

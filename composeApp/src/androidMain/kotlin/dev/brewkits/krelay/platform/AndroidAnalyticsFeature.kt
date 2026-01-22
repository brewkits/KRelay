package dev.brewkits.krelay.platform

import android.util.Log
import dev.brewkits.krelay.samples.AnalyticsFeature

/**
 * Android implementation of AnalyticsFeature.
 *
 * This is a DEMO implementation that logs to console.
 *
 * In production, replace with actual analytics SDK:
 * - Firebase Analytics: FirebaseAnalytics.getInstance(context)
 * - Mixpanel: MixpanelAPI.getInstance(context, token)
 * - Amplitude: Amplitude.getInstance().initialize(context, apiKey)
 * - etc.
 *
 * Example with Firebase:
 * ```kotlin
 * class FirebaseAnalyticsFeature(
 *     private val firebaseAnalytics: FirebaseAnalytics
 * ) : AnalyticsFeature {
 *     override fun track(eventName: String, parameters: Map<String, Any>) {
 *         val bundle = Bundle()
 *         parameters.forEach { (key, value) ->
 *             when (value) {
 *                 is String -> bundle.putString(key, value)
 *                 is Int -> bundle.putInt(key, value)
 *                 is Long -> bundle.putLong(key, value)
 *                 is Double -> bundle.putDouble(key, value)
 *                 is Boolean -> bundle.putBoolean(key, value)
 *                 else -> bundle.putString(key, value.toString())
 *             }
 *         }
 *         firebaseAnalytics.logEvent(eventName, bundle)
 *     }
 * }
 * ```
 */
class AndroidAnalyticsFeature : AnalyticsFeature {

    override fun track(eventName: String) {
        Log.d(TAG, "Event: $eventName")
        // Replace with: firebaseAnalytics.logEvent(eventName, null)
    }

    override fun track(eventName: String, parameters: Map<String, Any>) {
        Log.d(TAG, "Event: $eventName, Parameters: $parameters")
        // Replace with actual analytics SDK call
    }

    override fun setUserProperty(key: String, value: String) {
        Log.d(TAG, "User Property: $key = $value")
        // Replace with: firebaseAnalytics.setUserProperty(key, value)
    }

    override fun setUserId(userId: String) {
        Log.d(TAG, "User ID: $userId")
        // Replace with: firebaseAnalytics.setUserId(userId)
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        Log.d(TAG, "Screen: $screenName, Class: ${screenClass ?: "N/A"}")
        // Replace with actual analytics SDK call
    }

    companion object {
        private const val TAG = "Analytics"
    }
}

package dev.brewkits.krelay.integrations

import dev.brewkits.krelay.samples.AnalyticsFeature
import platform.Foundation.NSLog

/**
 * Real iOS Analytics implementation.
 *
 * In production, this would integrate with:
 * - Firebase Analytics
 * - Mixpanel
 * - Amplitude
 * - Apple Analytics
 *
 * For demo purposes, this logs to console using println.
 */
class IOSAnalyticsImpl : AnalyticsFeature {

    override fun track(eventName: String) {
        println("📊 [Analytics] Event tracked: $eventName")
    }

    override fun track(eventName: String, parameters: Map<String, Any>) {
        println("📊 [Analytics] Event tracked: $eventName with ${parameters.size} parameters")
        parameters.forEach { (key, value) ->
            println("📊 [Analytics]   - $key: $value")
        }
    }

    override fun setUserProperty(key: String, value: String) {
        println("📊 [Analytics] User property set: $key = $value")
    }

    override fun setUserId(userId: String) {
        println("📊 [Analytics] User ID set: $userId")
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        if (screenClass != null) {
            println("📊 [Analytics] Screen view: $screenName ($screenClass)")
        } else {
            println("📊 [Analytics] Screen view: $screenName")
        }
    }
}

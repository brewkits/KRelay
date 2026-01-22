package dev.brewkits.krelay.basic

import dev.brewkits.krelay.samples.ToastFeature
import dev.brewkits.krelay.samples.NotificationBridge
import dev.brewkits.krelay.samples.NavigationFeature
import dev.brewkits.krelay.samples.AnalyticsFeature

/**
 * Mock implementations for Basic Demo.
 *
 * These are simple console-logging implementations to demonstrate
 * KRelay functionality without requiring platform-specific code.
 */

/**
 * Mock Toast implementation that logs to console
 */
class MockToastImpl : ToastFeature {
    override fun showShort(message: String) {
        println("\n🍞 [ToastFeature] KRelay dispatched showShort()")
        println("   ┌─ Platform: Mock implementation (real app would show Android Toast/iOS Alert)")
        println("   ├─ Duration: SHORT (2 seconds)")
        println("   ├─ Message: \"$message\"")
        println("   └─ In real app: Toast.makeText(context, message, LENGTH_SHORT).show()")
        println("   ✓ Toast would be displayed to user\n")
    }

    override fun showLong(message: String) {
        println("\n🍞 [ToastFeature] KRelay dispatched showLong()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Duration: LONG (3.5 seconds)")
        println("   ├─ Message: \"$message\"")
        println("   └─ In real app: Toast.makeText(context, message, LENGTH_LONG).show()")
        println("   ✓ Toast would be displayed to user\n")
    }
}

/**
 * Mock Notification implementation that logs to console
 */
class MockNotificationImpl : NotificationBridge {
    override fun showInAppNotification(title: String, message: String, duration: Int) {
        println("\n🔔 [NotificationBridge] KRelay dispatched showInAppNotification()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Type: IN-APP notification banner")
        println("   ├─ Title: \"$title\"")
        println("   ├─ Message: \"$message\"")
        println("   ├─ Duration: ${duration} seconds")
        println("   └─ In real app: Show custom banner at top of screen")
        println("   ✓ User would see notification banner\n")
    }

    override fun showSystemNotification(title: String, message: String) {
        println("\n🔔 [NotificationBridge] KRelay dispatched showSystemNotification()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Type: SYSTEM notification (Android notification tray / iOS notification center)")
        println("   ├─ Title: \"$title\"")
        println("   ├─ Message: \"$message\"")
        println("   └─ In real app: NotificationManager.notify() / UNUserNotificationCenter")
        println("   ✓ User would see system notification\n")
    }
}

/**
 * Mock Navigation implementation that logs to console
 */
class MockNavigationImpl : NavigationFeature {
    override fun navigateTo(route: String, params: Map<String, String>) {
        println("\n🧭 [NavigationFeature] KRelay dispatched navigateTo()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Action: Navigate to route")
        println("   ├─ Route: \"$route\"")
        if (params.isNotEmpty()) {
            println("   ├─ Parameters: $params")
        }
        println("   └─ In real app: NavController.navigate(route) / Coordinator.navigate()")
        println("   ✓ User would see new screen: $route\n")
    }

    override fun navigateBack() {
        println("\n🧭 [NavigationFeature] KRelay dispatched navigateBack()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Action: Pop navigation stack")
        println("   └─ In real app: NavController.popBackStack() / NavigationController.popViewController()")
        println("   ✓ User would return to previous screen\n")
    }

    override fun navigateToRoot() {
        println("\n🧭 [NavigationFeature] KRelay dispatched navigateToRoot()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Action: Clear stack and return to root")
        println("   └─ In real app: NavController.popBackStack(ROOT, false)")
        println("   ✓ User would return to home screen\n")
    }
}

/**
 * Mock Analytics implementation that logs to console
 */
class MockAnalyticsImpl : AnalyticsFeature {
    override fun track(eventName: String) {
        println("\n📊 [AnalyticsFeature] KRelay dispatched track()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Event: \"$eventName\"")
        println("   └─ In real app: Firebase.analytics.logEvent(eventName)")
        println("   ✓ Event would be tracked\n")
    }

    override fun track(eventName: String, parameters: Map<String, Any>) {
        println("\n📊 [AnalyticsFeature] KRelay dispatched track() with parameters")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Event: \"$eventName\"")
        println("   ├─ Parameters:")
        parameters.forEach { (key, value) ->
            println("   │  • $key = $value")
        }
        println("   └─ In real app: Firebase.analytics.logEvent(eventName, params)")
        println("   ✓ Event with parameters would be tracked\n")
    }

    override fun setUserProperty(key: String, value: String) {
        println("\n📊 [AnalyticsFeature] KRelay dispatched setUserProperty()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Property: $key")
        println("   ├─ Value: $value")
        println("   └─ In real app: Firebase.analytics.setUserProperty(key, value)")
        println("   ✓ User property would be set\n")
    }

    override fun setUserId(userId: String) {
        println("\n📊 [AnalyticsFeature] KRelay dispatched setUserId()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ User ID: $userId")
        println("   └─ In real app: Firebase.analytics.setUserId(userId)")
        println("   ✓ User ID would be set for tracking\n")
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        println("\n📊 [AnalyticsFeature] KRelay dispatched trackScreen()")
        println("   ┌─ Platform: Mock implementation")
        println("   ├─ Screen: \"$screenName\"")
        if (screenClass != null) {
            println("   ├─ Class: \"$screenClass\"")
        }
        println("   └─ In real app: Firebase.analytics.logScreenView(screenName)")
        println("   ✓ Screen view would be tracked\n")
    }
}

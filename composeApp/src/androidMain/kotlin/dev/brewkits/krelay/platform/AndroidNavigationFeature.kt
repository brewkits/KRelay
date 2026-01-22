package dev.brewkits.krelay.platform

import android.content.Context
import android.util.Log
import android.widget.Toast
import dev.brewkits.krelay.samples.NavigationFeature

/**
 * Android implementation of NavigationFeature.
 *
 * This is a simplified demo implementation that just logs navigation events.
 * In production, you would use:
 * - Jetpack Navigation Compose (NavController)
 * - Fragment transactions
 * - Activity Intent navigation
 */
class AndroidNavigationFeature(private val context: Context) : NavigationFeature {

    override fun navigateTo(route: String, params: Map<String, String>) {
        val paramsStr = if (params.isNotEmpty()) {
            " with params: $params"
        } else {
            ""
        }

        Log.d("AndroidNavigation", "Navigate to: $route$paramsStr")
        Toast.makeText(context, "🧭 Navigate to: $route", Toast.LENGTH_SHORT).show()

        // In production with Compose Navigation:
        /*
        navController.navigate(route) {
            // Navigation options
            launchSingleTop = true
        }
        */
    }

    override fun navigateBack() {
        Log.d("AndroidNavigation", "Navigate back")
        Toast.makeText(context, "⬅️ Navigate back", Toast.LENGTH_SHORT).show()

        // In production:
        // navController.navigateUp()
        // or
        // (context as? Activity)?.onBackPressed()
    }

    override fun navigateToRoot() {
        Log.d("AndroidNavigation", "Navigate to root")
        Toast.makeText(context, "🏠 Navigate to root", Toast.LENGTH_SHORT).show()

        // In production:
        /*
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
        }
        */
    }
}

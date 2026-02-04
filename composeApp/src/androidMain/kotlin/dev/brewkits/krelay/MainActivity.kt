package dev.brewkits.krelay

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.platform.AndroidNavigationFeature
import dev.brewkits.krelay.platform.AndroidNotificationBridge
import dev.brewkits.krelay.platform.AndroidToastFeature
import dev.brewkits.krelay.register
import dev.brewkits.krelay.samples.NavigationFeature
import dev.brewkits.krelay.samples.NotificationBridge
import dev.brewkits.krelay.samples.ToastFeature
import dev.brewkits.krelay.superapp.foodKRelay
import dev.brewkits.krelay.superapp.ridesKRelay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Enable debug mode to see KRelay logs
        KRelay.debugMode = true

        // Register platform implementations for the default singleton
        KRelay.register<ToastFeature>(AndroidToastFeature(applicationContext))
        KRelay.register<NotificationBridge>(AndroidNotificationBridge(applicationContext))
        KRelay.register<NavigationFeature>(AndroidNavigationFeature(applicationContext))

        // Register implementations for the Super App demo instances
        // IMPORTANT: Each instance needs its own platform implementation
        Log.d("MainActivity", "Registering Super App instances...")

        ridesKRelay.debugMode = true
        ridesKRelay.register<ToastFeature>(AndroidToastFeature(applicationContext))
        Log.d("MainActivity", "Rides KRelay registered: ${ridesKRelay.isRegistered<ToastFeature>()}")

        foodKRelay.debugMode = true
        foodKRelay.register<ToastFeature>(AndroidToastFeature(applicationContext))
        Log.d("MainActivity", "Food KRelay registered: ${foodKRelay.isRegistered<ToastFeature>()}")

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optional: Explicitly unregister (though WeakRef will handle this automatically)
        // KRelay.unregister<ToastFeature>()
        // ridesKRelay.unregister<ToastFeature>()
        // foodKRelay.unregister<ToastFeature>()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
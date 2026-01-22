package dev.brewkits.krelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.platform.AndroidNavigationFeature
import dev.brewkits.krelay.platform.AndroidNotificationBridge
import dev.brewkits.krelay.platform.AndroidToastFeature
import dev.brewkits.krelay.samples.NavigationFeature
import dev.brewkits.krelay.samples.NotificationBridge
import dev.brewkits.krelay.samples.ToastFeature

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Enable debug mode to see KRelay logs
        KRelay.debugMode = true

        // Register platform implementations
        // These will be held as weak references and automatically cleaned up
        // when this Activity is destroyed
        KRelay.register<ToastFeature>(AndroidToastFeature(applicationContext))
        KRelay.register<NotificationBridge>(AndroidNotificationBridge(applicationContext))
        KRelay.register<NavigationFeature>(AndroidNavigationFeature(applicationContext))

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optional: Explicitly unregister (though WeakRef will handle this automatically)
        // KRelay.unregister<ToastFeature>()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
package dev.brewkits.krelay.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.brewkits.krelay.integrations.AndroidToastImpl
import dev.brewkits.krelay.samples.ToastFeature

/**
 * Android-specific Toast implementation for BasicDemo.
 * Returns the real Android Toast implementation with Context.
 */
@Composable
actual fun rememberPlatformToastImpl(): ToastFeature {
    val context = LocalContext.current
    return remember { AndroidToastImpl(context.applicationContext) }
}

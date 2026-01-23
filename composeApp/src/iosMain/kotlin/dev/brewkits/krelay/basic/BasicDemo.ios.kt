package dev.brewkits.krelay.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.brewkits.krelay.integrations.IOSToastImpl
import dev.brewkits.krelay.samples.ToastFeature

/**
 * iOS-specific Toast implementation for BasicDemo.
 * Returns the real iOS UIAlertController implementation.
 */
@Composable
actual fun rememberPlatformToastImpl(): ToastFeature {
    return remember { IOSToastImpl() }
}

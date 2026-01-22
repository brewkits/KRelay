package dev.brewkits.krelay

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Main iOS ViewController factory.
 *
 * This creates a Compose UIViewController and returns it to SwiftUI.
 * KRelay implementations should be registered from Swift side after this VC is created.
 */
fun MainViewController(): UIViewController {
    return ComposeUIViewController { App() }
}
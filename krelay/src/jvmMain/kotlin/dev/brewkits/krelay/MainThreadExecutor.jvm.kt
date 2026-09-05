package dev.brewkits.krelay

import javax.swing.SwingUtilities

actual fun isMainThread(): Boolean = SwingUtilities.isEventDispatchThread()

private val isUnitTestEnvironment by lazy {
    try {
        Class.forName("org.junit.Test")
        true
    } catch (e: Throwable) {
        false
    }
}

actual fun runOnMain(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread() || isUnitTestEnvironment) {
        block()
    } else {
        SwingUtilities.invokeLater { block() }
    }
}

package dev.brewkits.krelay

import kotlinx.coroutines.CoroutineScope

actual fun runTestBlocking(block: suspend CoroutineScope.() -> Unit) {
    // Skipped on WasmJs since runBlocking is not supported
}

package dev.brewkits.krelay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun runTestBlocking(block: suspend CoroutineScope.() -> Unit) {
    runBlocking { block() }
}

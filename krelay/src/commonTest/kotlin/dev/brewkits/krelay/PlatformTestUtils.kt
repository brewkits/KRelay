package dev.brewkits.krelay

import kotlinx.coroutines.CoroutineScope

expect fun runTestBlocking(block: suspend CoroutineScope.() -> Unit)

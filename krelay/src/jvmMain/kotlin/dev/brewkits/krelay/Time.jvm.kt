package dev.brewkits.krelay

/**
 * Returns the current system time in milliseconds for JVM targets.
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

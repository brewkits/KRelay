package dev.brewkits.krelay

/**
 * Android implementation using System.currentTimeMillis().
 */
@PublishedApi
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

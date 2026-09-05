package dev.brewkits.krelay

@JsFun("() => Date.now()")
internal external fun dateNow(): Double

internal actual fun currentTimeMillis(): Long = dateNow().toLong()

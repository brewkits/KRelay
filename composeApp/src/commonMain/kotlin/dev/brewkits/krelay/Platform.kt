package dev.brewkits.krelay

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
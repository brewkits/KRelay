package dev.brewkits.krelay

/**
 * Severity of a KRelay log record, ordered from least to most severe.
 */
enum class KRelayLogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * Destination for KRelay log records (Logcat, os_log, Timber, Datadog, a file, ...).
 *
 * Implementations may be called from any thread and must not throw; exceptions are swallowed
 * so a faulty sink can never break a dispatch.
 */
fun interface KRelayLogSink {
    fun log(level: KRelayLogLevel, scopeName: String, message: String)
}

/**
 * Global configuration for KRelay's structured logging.
 *
 * By default records go to the console. DEBUG and most WARN records are only produced when the
 * owning instance has `debugMode` enabled; ERROR records (e.g. an action that threw) are
 * always produced.
 *
 * ```kotlin
 * KRelayLog.minLevel = KRelayLogLevel.WARN
 * KRelayLog.sink = KRelayLogSink { level, scope, message ->
 *     MyLogger.log(level.name, "KRelay[$scope]", message)
 * }
 * ```
 */
object KRelayLog {
    /** Records below this level are dropped before reaching [sink]. Default: [KRelayLogLevel.DEBUG]. */
    @kotlin.concurrent.Volatile
    var minLevel: KRelayLogLevel = KRelayLogLevel.DEBUG

    /** Where records are delivered. Default: [ConsoleLogSink]. */
    @kotlin.concurrent.Volatile
    var sink: KRelayLogSink = ConsoleLogSink

    /** Restores the default console sink and minimum level. */
    fun reset() {
        minLevel = KRelayLogLevel.DEBUG
        sink = ConsoleLogSink
    }

    internal fun emit(level: KRelayLogLevel, scopeName: String, message: String) {
        if (level < minLevel) return
        try {
            sink.log(level, scopeName, message)
        } catch (_: Throwable) {
            // A logging sink must never break dispatching.
        }
    }
}

/**
 * Default [KRelayLogSink]: prints to the console using the pre-2.3.0 format.
 */
object ConsoleLogSink : KRelayLogSink {
    override fun log(level: KRelayLogLevel, scopeName: String, message: String) {
        val line = when (level) {
            KRelayLogLevel.ERROR -> "[KRelay][$scopeName][ERROR] $message"
            KRelayLogLevel.WARN -> "[KRelay][$scopeName][WARN] $message"
            else -> "[KRelay][$scopeName] $message"
        }
        println(line)
    }
}

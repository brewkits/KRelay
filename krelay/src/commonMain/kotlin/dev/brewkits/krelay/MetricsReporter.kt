package dev.brewkits.krelay

/**
 * Kind of event reported to a [KRelayMetricsReporter].
 */
enum class KRelayMetricType { DISPATCH, QUEUE, REPLAY, EXPIRY, CLEAR }

/**
 * A single metrics event.
 *
 * @property type What happened.
 * @property featureName `simpleName` of the feature interface (may be empty for anonymous types).
 * @property scopeName Name of the owning instance, or empty for events recorded without a scope.
 * @property count Number of actions the event covers (1 for dispatch/queue, N for replay/expiry/clear).
 */
data class KRelayMetricEvent(
    val type: KRelayMetricType,
    val featureName: String,
    val scopeName: String,
    val count: Int
)

/**
 * Receives KRelay metrics events so they can be forwarded to Firebase Performance, Datadog,
 * or any custom backend. KRelay ships only this interface — it has no dependency on those SDKs.
 *
 * Register with [KRelayMetrics.addReporter]. Events are only delivered while
 * [KRelayMetrics.enabled] is true. Reporters are invoked synchronously on the thread that
 * recorded the event, and for some events (replay, expiry) while KRelay holds its internal
 * reentrant lock — keep them fast and non-blocking, and hand heavy work to another thread.
 * Exceptions thrown by a reporter are swallowed.
 */
fun interface KRelayMetricsReporter {
    fun onMetric(event: KRelayMetricEvent)
}

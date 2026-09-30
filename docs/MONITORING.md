# Monitoring & Logging (v2.3.0+)

KRelay exposes two small hooks. Neither pulls in a third-party SDK — you forward the data to whichever
backend you use (Firebase Performance, Datadog, Sentry, your own endpoint…).

## Structured logging

```kotlin
// Only surface warnings and errors in production
KRelayLog.minLevel = KRelayLogLevel.WARN

KRelayLog.sink = KRelayLogSink { level, scopeName, message ->
    // Forward to your logger of choice
    MyLogger.log(level.name, tag = "KRelay[$scopeName]", message = message)
}
```

- Levels: `DEBUG`, `INFO`, `WARN`, `ERROR`.
- `DEBUG` and most `WARN` records are only produced when the instance was built with `debugMode(true)`.
  `ERROR` records (for example an action that threw) are always produced.
- Sinks can be called from any thread and must be fast. Exceptions thrown by a sink are swallowed.
- `KRelayLog.reset()` restores the console sink and `DEBUG` level.

## Metrics reporter

Metrics are opt-in:

```kotlin
KRelayMetrics.enabled = true

KRelayMetrics.addReporter { event ->
    // event.type:        DISPATCH | QUEUE | REPLAY | EXPIRY | CLEAR
    // event.featureName: e.g. "ToastFeature"
    // event.scopeName:   instance scope, "" for the default singleton
    // event.count:       number of actions covered by the event
    MyAnalytics.count(
        name = "krelay.${event.type.name.lowercase()}",
        value = event.count,
        tags = mapOf("feature" to event.featureName, "scope" to event.scopeName)
    )
}
```

Guidelines:

- The reporter runs synchronously on the recording thread, and for replay/expiry events while KRelay
  holds its internal (reentrant) lock. Keep it cheap and hand heavy work to another thread or batch it.
- Exceptions thrown by a reporter are swallowed.
- `removeReporter()` detaches it; `KRelayMetrics.reset()` clears counters but keeps reporters.
- Pull-style access is still available: `KRelayMetrics.getAllMetrics()` and
  `KRelayMetrics.getInstanceMetrics(scopeName)`.

`MyLogger` / `MyAnalytics` above are placeholders for your own wrappers around the monitoring SDK.

## Persisting from coroutines

With `krelay-flow`, `dispatchPersistedSuspend` keeps persistence I/O off the calling coroutine:

```kotlin
viewModelScope.launch {
    relay.dispatchPersistedSuspend<ToastFeature>("toast", "show", payload = "Saved")
}
```

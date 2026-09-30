# Migrating to KRelay 2.3.0

2.3.0 is source- and binary-compatible with 2.2.x: no public API was removed or changed
(`apiCheck` only shows additions). What changes for you is the **toolchain KRelay is built with**
and a few behaviours.

## 1. Toolchain requirements

KRelay 2.3.0 is compiled with **Kotlin 2.3.21** and (for `krelay-compose`) **Compose Multiplatform 1.10.3**.

- Kotlin compilers can read libraries built by the next minor version at most, so use **Kotlin 2.2 or newer**
  in your project. Kotlin 2.1.x is not supported by 2.3.0; stay on KRelay 2.2.x if you cannot upgrade yet.
- If you use `krelay-compose`, align your Compose Multiplatform version with 1.10.x
  (the BOM does not manage Compose).
- Android Gradle Plugin and minSdk (24) requirements are unchanged.

```kotlin
commonMain.dependencies {
    api(platform("dev.brewkits:krelay-bom:2.3.0"))
    implementation("dev.brewkits:krelay")
}
```

## 2. Behaviour changes

| Area | Before (2.2.x) | Now (2.3.0) |
|---|---|---|
| `KRelayMetrics.getInstanceMetrics(scope)` | Always empty — events were recorded without the instance scope | Populated per instance |
| Expired actions dropped on replay | Not counted | Counted via `recordExpiry` (`expired` in metrics) |
| Warning log format | `[KRelay][scope] [WARN] …` | `[KRelay][scope][WARN] …` |

If you parse KRelay's console output or assert on metrics in your own tests, adjust accordingly.

## 3. New opt-in APIs

### Structured logging
```kotlin
KRelayLog.minLevel = KRelayLogLevel.WARN
KRelayLog.sink = KRelayLogSink { level, scope, message -> MyLogger.log(level.name, scope, message) }
```

### Metrics reporter
```kotlin
KRelayMetrics.enabled = true
KRelayMetrics.addReporter { event -> MyAnalytics.count("krelay.${event.type}", event.count) }
```

### `dispatchPersistedSuspend` (`krelay-flow`)
```kotlin
viewModelScope.launch {
    relay.dispatchPersistedSuspend<ToastFeature>("toast", "show", payload = "Saved")
}
```

See [Monitoring & Logging](MONITORING.md) for details. Nothing needs to change if you do not use these.

## 4. Publishing / CI (maintainers only)

Releases are published through the Central Portal OSSRH staging API; the legacy
`s01.oss.sonatype.org` endpoint no longer exists. The `maven-central` GitHub environment needs
`OSSRH_USERNAME` / `OSSRH_PASSWORD` (Portal **user token**), `SIGNING_KEY` and `SIGNING_PASSWORD`.

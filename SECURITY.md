# Security Policy

## Supported Versions

We provide security fixes for the following versions:

| Version | Supported |
|---------|-----------|
| 2.x.x (latest) | ✅ Active support |
| 1.x.x | ⚠️ Critical security fixes only |
| < 1.0.0 | ❌ No support |

---

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities as public GitHub Issues.**

To report a vulnerability, send an email to:

**`security@brewkits.dev`**

Please include the following information:

- **Affected version(s)** of KRelay
- **A description of the vulnerability** — what it is, what the impact is
- **Steps to reproduce** — a minimal proof-of-concept if possible
- **Your suggested fix** (optional, but appreciated)

### What to expect

| Timeline | Action |
|---|---|
| **Within 48 hours** | Acknowledgement of your report |
| **Within 7 days** | Initial assessment and severity determination |
| **Within 30 days** | Patch released (for confirmed vulnerabilities) |
| **After patch release** | Public disclosure (coordinated with reporter) |

We will credit you in the release notes unless you request otherwise.

---

## Scope

KRelay is a Kotlin Multiplatform library. Security issues most relevant to this library include:

- **Memory safety** — improper use of references that could lead to data leaks between app components or modules
- **Data leakage** — persisted dispatch data (`dispatchPersisted`) being stored insecurely on-device
- **Thread safety vulnerabilities** — race conditions that could lead to inconsistent or corrupted state

Issues related to **third-party dependencies** of consumer apps are generally out of scope. KRelay's core module has **zero runtime dependencies** beyond the Kotlin standard library.

---

## Security Considerations for Users

### Persistent Dispatch Storage

`dispatchPersisted<T>()` stores serialized command data on-device using the platform's standard key-value store (`SharedPreferences` on Android, `NSUserDefaults` on iOS). 

> [!WARNING]
> Do NOT persist sensitive data (passwords, tokens, PII) via `dispatchPersisted`. This storage is not encrypted. Use the platform's Keychain (iOS) or EncryptedSharedPreferences (Android) for sensitive data.

### WeakReference Registry

KRelay holds registered implementations via `WeakReference`. If a registered object holds sensitive data, it will be garbage-collected normally when no strong reference exists. KRelay does not prevent GC of registered implementations.

### Logging

When `KRelay.debugMode = true`, internal state (feature class names, queue depths) is logged to the platform console (Logcat / os_log). **Do not enable `debugMode` in release builds.**

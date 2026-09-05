# Contributing to KRelay

Thank you for your interest in contributing to KRelay! This document outlines the process for contributing code, documentation, and bug reports.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Ways to Contribute](#ways-to-contribute)
- [Development Setup](#development-setup)
- [Running Tests](#running-tests)
- [Coding Standards](#coding-standards)
- [Commit Message Convention](#commit-message-convention)
- [Pull Request Process](#pull-request-process)
- [Release Process](#release-process)

---

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). By participating, you agree to uphold this standard. Please report unacceptable behavior to `dev@brewkits.dev`.

---

## Ways to Contribute

- **Bug Reports** — Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.yml)
- **Feature Requests** — Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.yml)
- **Documentation fixes** — Typos, corrections, improved examples
- **Integration Guides** — Examples for integrating KRelay with other KMP libraries
- **Code contributions** — Bug fixes, performance improvements, new features from the roadmap

---

## Development Setup

### Prerequisites

| Tool | Minimum Version |
|---|---|
| JDK | 17 (Temurin recommended) |
| Android SDK | API 24+ |
| Xcode | 15+ (for iOS targets, macOS only) |
| Kotlin | 2.1.0 |

### Clone and open

```bash
git clone https://github.com/brewkits/KRelay.git
cd KRelay
```

Open with **Android Studio Meerkat** or **IntelliJ IDEA** (with the Kotlin Multiplatform plugin installed).

### Verify your environment

```bash
# Should print BUILD SUCCESSFUL
./gradlew help --no-daemon
```

---

## Running Tests

### JVM Unit Tests (fast — no device needed)

```bash
./gradlew :krelay:testDebugUnitTest
./gradlew :krelay-compose:testDebugUnitTest
./gradlew :krelay-testing:testDebugUnitTest
```

### iOS Simulator Tests (macOS only)

```bash
./gradlew :krelay:iosSimulatorArm64Test
```

### Android Instrumented Tests (requires emulator or device)

```bash
./gradlew :krelay:connectedDebugAndroidTest
./gradlew :krelay-compose:connectedDebugAndroidTest
```

### Full check

```bash
./gradlew test
```

### Binary Compatibility Check

Before submitting a PR that modifies public API, run:

```bash
./gradlew :krelay:apiCheck :krelay-compose:apiCheck
```

If you intentionally changed the public API, update the API dump:

```bash
./gradlew :krelay:apiDump :krelay-compose:apiDump
```

Then commit the updated `.api` files along with your changes.

---

## Coding Standards

- **Language**: Kotlin only (no Java in the library modules).
- **Style**: Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **KDoc**: All public API must have KDoc. Include `@param`, `@return`, and `@throws` where applicable.
- **No internal comments in Vietnamese** or any non-English language.
- **No `TODO` / `FIXME` comments** in committed code — open an issue instead.
- **Thread safety**: Any shared mutable state **must** be accessed inside `lock.withLock`. Document threading guarantees in KDoc.
- **Memory safety**: Never store strong references to `Activity`, `Fragment`, or `ViewController` in long-lived objects. Use `WeakReference` or the KRelay registration pattern.

---

## Commit Message Convention

KRelay uses [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

**Types:**

| Type | When to use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `perf` | Performance improvement |
| `refactor` | Code change that's neither a feat nor a fix |
| `test` | Adding or fixing tests |
| `docs` | Documentation only |
| `ci` | CI/CD pipeline changes |
| `build` | Build system or dependency changes |
| `chore` | Other maintenance |

**Scopes** (optional): `core`, `compose`, `testing`, `bom`, `ios`, `android`, `ci`, `docs`

**Examples:**

```
feat(core): add removeInstance() API for Super App lifecycle management
fix(android): replace StringSet with JSONArray in SharedPreferencesPersistenceAdapter
perf(core): use binary insertion for priority queue (O(log N) vs O(N log N))
docs(compose): document vararg keys parameter in KRelayEffect
```

---

## Pull Request Process

1. **Fork** the repository and create a branch from `main`:
   ```bash
   git checkout -b fix/my-bug-fix
   ```

2. **Make your changes** following the coding standards above.

3. **Write or update tests** — all bug fixes must include a regression test. New features require unit tests.

4. **Run the full test suite** and `apiCheck` to ensure nothing is broken.

5. **Update documentation** if you changed or added public API.

6. **Open a Pull Request** against `main`. Fill in the PR template completely.

7. **Address review feedback** promptly. PRs with no activity for 14 days may be closed.

### PR Requirements Checklist

- [ ] All tests pass (`./gradlew test`)
- [ ] `apiCheck` passes (or `apiDump` updated for intentional API changes)
- [ ] New public API has KDoc
- [ ] CHANGELOG.md updated under `[Unreleased]`
- [ ] No debugging code or println left in

---

## Release Process

Releases are handled by the maintainers. The process is:

1. Update version in `krelay/build.gradle.kts` and `krelay-compose/build.gradle.kts`
2. Update `CHANGELOG.md` (move `[Unreleased]` to the new version)
3. Run `./gradlew :krelay:apiDump` to capture the current API baseline
4. Commit: `chore: release v2.x.x`
5. Push tag: `git tag v2.x.x && git push origin v2.x.x`
6. The `release.yml` CI/CD workflow takes over automatically

---

## Questions?

Open a [Discussion](https://github.com/brewkits/KRelay/discussions) rather than an Issue for general questions.

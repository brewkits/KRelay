# Managing KRelay Warnings: Balancing Safety and Convenience

> **TL;DR**: Warnings can be suppressed at module/file level to reduce boilerplate. Only use when you understand the risks.

---

## The Problem

KRelay has compile-time warnings to prevent misuse:

```kotlin
// ⚠️ Warning appears on EVERY KRelay call
class MyViewModel {
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
        //    ^^^^^^^^ Warning: Queue lost on process death
    }

    fun navigate() {
        KRelay.dispatch<NavigationFeature> { it.goToHome() }
        //    ^^^^^^^^ Warning again!
    }
}
```

**Problem**: Even for safe use cases (Toast, Navigation), you see warnings everywhere. This creates noise and developer frustration.

---

## Understanding the Three Warnings

KRelay has three opt-in warnings to guide safe usage:

### 1. @ProcessDeathUnsafe
**What it means**: Queue is lost when OS kills the app (process death)

**Safe for**: Toast, Navigation, Haptics, UI feedback
**Dangerous for**: Payments, Uploads, Critical Analytics

See: [Anti-Patterns Guide](./ANTI_PATTERNS.md)

### 2. @SuperAppWarning
**What it means**: Global singleton may cause conflicts in large apps

**Safe for**: Single-module apps, small-medium projects
**Caution for**: Super Apps (Grab/Gojek style) - use feature namespacing

See: [ADR-0001](./adr/0001-singleton-and-serialization-tradeoffs.md)

### 3. @MemoryLeakWarning (v1.1.0+)
**What it means**: Lambdas may capture ViewModels/Contexts causing memory leaks

**Safe when**: Capturing primitives/data only
**Dangerous when**: Capturing entire ViewModels or Android Contexts

**Best Practice**:
```kotlin
// ✅ Good: Capture primitives only
val message = viewModel.data
KRelay.dispatch<ToastFeature> { it.show(message) }

// ❌ Bad: Captures entire viewModel
KRelay.dispatch<ToastFeature> { it.show(viewModel.data) }
```

**Solution**: Call `clearQueue()` in ViewModel's `onCleared()` or capture primitives only.

See: [Main README - Memory Management](../README.md#memory-management-best-practices)

---

## Solutions (Ranked by Convenience)

### ⭐ Option 1: Module-Level Suppression (RECOMMENDED)

Suppress warnings for your entire module if you're confident about your usage.

#### For Shared Module (commonMain)

Add to your **shared module's build.gradle.kts**:

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            languageSettings {
                // Suppress warnings for entire module
                optIn("dev.brewkits.krelay.ProcessDeathUnsafe")
                optIn("dev.brewkits.krelay.SuperAppWarning")
                optIn("dev.brewkits.krelay.MemoryLeakWarning")  // v1.1.0+
            }
        }
    }
}
```

**Effect**: No more warnings in your shared code!

```kotlin
// Now writes like v1.0 - clean!
class MyViewModel {
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
        // No warning!
    }
}
```

**When to use:**
- ✅ Your module ONLY uses KRelay for safe operations (Toast, Navigation, Haptics)
- ✅ You've read the documentation and understand limitations
- ✅ Your team has code review process to catch misuse

**When NOT to use:**
- ❌ You're unsure if all KRelay usage is safe
- ❌ Multiple developers with varying KRelay knowledge
- ❌ No code review process

---

### Option 2: File-Level Suppression

Suppress for a specific file if that file only does safe operations.

```kotlin
// At top of file
@file:OptIn(
    ProcessDeathUnsafe::class,
    SuperAppWarning::class,
    MemoryLeakWarning::class  // v1.1.0+
)

package com.myapp.viewmodels

import dev.brewkits.krelay.*

class LoginViewModel {
    fun onSuccess() {
        KRelay.dispatch<ToastFeature> { it.show("Welcome!") }
        // No warning in this file
    }
}

class ProfileViewModel {
    fun navigate() {
        KRelay.dispatch<NavigationFeature> { it.goToProfile() }
        // No warning in this file
    }
}
```

**When to use:**
- ✅ One file with multiple ViewModels doing safe operations
- ✅ Clear separation: "This file is UI-only commands"

---

### Option 3: Class-Level Suppression

Suppress for a specific class.

```kotlin
@OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
class LoginViewModel {
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
        // No warning
    }

    fun navigate() {
        KRelay.dispatch<NavigationFeature> { it.goToHome() }
        // No warning
    }
}
```

**When to use:**
- ✅ This specific ViewModel only does safe operations
- ✅ You want warnings to appear in OTHER ViewModels

---

### Option 4: Function-Level Opt-In (Most Verbose)

Only suppress at individual function level.

```kotlin
class MyViewModel {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
    }

    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
    fun navigate() {
        KRelay.dispatch<NavigationFeature> { it.goToHome() }
    }
}
```

**When to use:**
- ✅ You want maximum safety (warnings everywhere else)
- ✅ Greenfield project with strict code review

**When NOT to use:**
- ❌ Too verbose for most projects
- ❌ Developer fatigue

---

## Recommendation by App Type

| App Type | Recommended Approach | Reasoning |
|----------|---------------------|-----------|
| **Small-Medium App** | **Module-Level** | You control all code, safe usage is obvious |
| **Large App (Single Team)** | **File-Level** | Separate UI command files from business logic |
| **Super App (Multiple Teams)** | **Class-Level** | Each team can decide per ViewModel |
| **Library/SDK** | **Function-Level** | Maximum safety, warnings guide library users |
| **Startup MVP** | **Module-Level** | Move fast, you know what you're doing |
| **Enterprise (Banking)** | **Class-Level + Code Review** | Safety-critical, need human verification |

---

## Example: Recommended Setup for Typical App

### Step 1: Suppress at Module Level

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            languageSettings {
                optIn("dev.brewkits.krelay.ProcessDeathUnsafe")
                optIn("dev.brewkits.krelay.SuperAppWarning")
                optIn("dev.brewkits.krelay.MemoryLeakWarning")  // v1.1.0+
            }
        }
    }
}
```

### Step 2: Add Lint Rule (Optional)

Create custom lint rule to catch dangerous patterns:

```kotlin
// detekt.yml or custom lint rule
KRelayDangerousUsage:
  active: true
  excludes: []
  patterns:
    - 'dispatch.*Payment'
    - 'dispatch.*Upload'
    - 'dispatch.*Database'
    - 'dispatch.*Critical'
```

### Step 3: Code Review Checklist

Add to PR template:

```markdown
## KRelay Usage Checklist
- [ ] All KRelay.dispatch() calls are for UI feedback only (Toast/Navigation)
- [ ] No KRelay usage for Payments, Uploads, or Critical Analytics
- [ ] Verified alternatives used (WorkManager, Room) for critical ops
```

---

## Comparison: With vs Without Warnings

### Scenario 1: Toast (Safe Operation)

**Without Module-Level Suppression** (Annoying):
```kotlin
@OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)  // Boilerplate
class MyViewModel {
    @OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)  // More boilerplate
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
    }
}
```

**With Module-Level Suppression** (Clean):
```kotlin
class MyViewModel {
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
    }
}
```

**Result**: 5 lines → 3 lines. Much cleaner!

---

### Scenario 2: Mixed Safe/Unsafe Operations

**If you have BOTH safe and unsafe operations in your app:**

```kotlin
// Build config: Module-level suppression DISABLED
// Use class-level opt-in selectively

// Safe ViewModel - Opt-in
@OptIn(ProcessDeathUnsafe::class, SuperAppWarning::class)
class UIViewModel {
    fun showToast() {
        KRelay.dispatch<ToastFeature> { it.show("Hello") }
    }
}

// Payment ViewModel - NO opt-in
class PaymentViewModel {
    fun processPayment(amount: Double) {
        // ⚠️ Warning appears here - GOOD!
        // KRelay.dispatch<PaymentFeature> { it.process(amount) }

        // Correct approach
        val work = OneTimeWorkRequestBuilder<PaymentWorker>().build()
        WorkManager.getInstance(context).enqueue(work)
    }
}
```

**This gives you:**
- ✅ Clean code for safe operations (opt-in)
- ✅ Warnings for dangerous operations (no opt-in)
- ✅ Best of both worlds

---

## Disabling Warnings Completely (NOT RECOMMENDED)

If you absolutely need to disable all warnings project-wide:

```kotlin
// root build.gradle.kts (affects all modules)
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += listOf(
                "-opt-in=dev.brewkits.krelay.ProcessDeathUnsafe",
                "-opt-in=dev.brewkits.krelay.SuperAppWarning",
                "-opt-in=dev.brewkits.krelay.MemoryLeakWarning"  // v1.1.0+
            )
        }
    }
}
```

**⚠️ WARNING**: This disables warnings everywhere, including in dangerous code. Only use if:
- You're 100% confident your team knows when NOT to use KRelay
- You have automated tests catching misuse
- You have strict code review process

---

## IDE Suppressions (Not Recommended)

You can also suppress in IDE:

```kotlin
@Suppress("OPT_IN_USAGE")  // ❌ Not recommended
fun myFunction() {
    KRelay.dispatch<ToastFeature> { it.show("Hello") }
}
```

**Why not recommended:**
- Suppresses ALL opt-in warnings (not just KRelay)
- No compile-time safety
- Hides intentional warnings

---

## Testing: Verify Suppressions Work

After adding module-level suppression:

```bash
# Clean and rebuild
./gradlew clean
./gradlew :shared:compileKotlinMetadata

# Should compile without warnings
# If warnings still appear, check your build config
```

---

## Migration Path: Adding Warnings to Existing Project

If you're upgrading KRelay from v1.0 (no warnings) to v1.1 (with warnings):

### Step 1: See All Warnings First
```bash
./gradlew :shared:compileKotlinMetadata
# Let it fail, see all warning locations
```

### Step 2: Review Each Warning
- Is this usage safe? (Toast, Navigation, Haptics)
- Is this usage dangerous? (Payment, Upload, Critical Analytics)

### Step 3: Fix Dangerous Usage First
```kotlin
// Before (dangerous)
KRelay.dispatch<PaymentFeature> { it.process(amount) }

// After (safe)
val work = OneTimeWorkRequestBuilder<PaymentWorker>().build()
WorkManager.getInstance(context).enqueue(work)
```

### Step 4: Add Module-Level Suppression
```kotlin
// shared/build.gradle.kts
languageSettings {
    optIn("dev.brewkits.krelay.ProcessDeathUnsafe")
    optIn("dev.brewkits.krelay.SuperAppWarning")
    optIn("dev.brewkits.krelay.MemoryLeakWarning")  // v1.1.0+
}
```

### Step 5: Rebuild
```bash
./gradlew :shared:compileKotlinMetadata
# Should succeed now
```

---

## FAQ

### Q: Should I suppress warnings?
**A**: Yes, if you're confident your usage is safe. Most apps only use KRelay for UI feedback (Toast, Navigation), which is completely safe.

### Q: Will suppression affect runtime behavior?
**A**: No. Warnings are compile-time only. Suppression just silences the compiler.

### Q: Can I suppress for some modules but not others?
**A**: Yes! Add `languageSettings { optIn(...) }` only to modules where usage is safe.

### Q: What if I'm unsure?
**A**: Keep warnings enabled. Use class-level `@OptIn` for ViewModels you're confident about.

### Q: What about third-party libraries using KRelay?
**A**: They should NOT suppress warnings. Library users should see warnings and opt-in consciously.

---

## Summary

| Scope | Boilerplate | Safety | Recommendation |
|-------|-------------|--------|----------------|
| Module-Level | None | Low | **✅ Use for typical apps** |
| File-Level | Minimal | Medium | Good for organized codebases |
| Class-Level | Some | High | Good for mixed safe/unsafe code |
| Function-Level | Heavy | Maximum | Only for safety-critical apps |

**For most apps**: Use **Module-Level suppression** + **Code Review** + **Custom Lint Rules**.

**Balance achieved**: Clean code without sacrificing safety! 🎉

---

## Further Reading

- [ProcessDeathUnsafe Annotation](../krelay/src/commonMain/kotlin/dev/brewkits/krelay/ProcessDeathUnsafe.kt)
- [SuperAppWarning Annotation](../krelay/src/commonMain/kotlin/dev/brewkits/krelay/SuperAppWarning.kt)
- [MemoryLeakWarning Annotation](../krelay/src/commonMain/kotlin/dev/brewkits/krelay/MemoryLeakWarning.kt) (v1.1.0+)
- [Anti-Patterns Guide](./ANTI_PATTERNS.md)
- [ADR-0001](./adr/0001-singleton-and-serialization-tradeoffs.md)

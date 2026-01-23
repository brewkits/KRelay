# Known Issues

## Voyager Integration Demo - Lifecycle Crash

**Status:** TEMPORARILY DISABLED
**Affected Component:** Voyager Demo (composeApp)
**Severity:** High (Crashes app)
**KRelay Impact:** None - This is a Voyager library bug

### Problem Description

The Voyager Integration demo crashes when performing multiple navigation operations due to a lifecycle management bug in Voyager's `AndroidScreenLifecycleOwner`.

**Error:**
```
java.lang.IllegalStateException: State is 'DESTROYED' and cannot be moved to `STARTED`
in component cafe.adriel.voyager.androidx.AndroidScreenLifecycleOwner
```

### Root Cause

Voyager's `AndroidScreenLifecycleOwner.emitOnStopEvents()` attempts to transition a lifecycle that is already in `DESTROYED` state back to `STARTED`, which is not allowed by AndroidX Lifecycle.

**Reproduction Steps:**
1. Navigate Login → Home (works)
2. Navigate Home → Profile (works)
3. Navigate Profile → Back (CRASHES)

Or:
1. Navigate Login → Home multiple times (crashes after 2-3 times)

### What We Tried

Multiple workarounds were attempted:

1. ✅ **Used `replace()` instead of `replaceAll()`** - Still crashes
2. ✅ **Added coroutine delays (50ms, 100ms, 150ms)** - Still crashes
3. ✅ **Used `yield()` before navigation** - Still crashes
4. ✅ **Changed dispatcher to `Dispatchers.Main`** - Still crashes
5. ✅ **Used `popAll()` + `delay()` + `push()`** - Still crashes
6. ✅ **Increased delays between operations** - Still crashes

**Conclusion:** This is a fundamental bug in Voyager's lifecycle implementation that cannot be worked around from KRelay's side.

### Impact on KRelay

**Important:** This issue does NOT affect KRelay's core functionality.

- ✅ KRelay dispatch mechanism works perfectly
- ✅ Basic Demo works without issues
- ✅ Integrations Demo works without issues
- ✅ All unit tests pass
- ❌ Only Voyager navigation integration affected

### Alternatives

Users can:

1. **Use other navigation libraries:**
   - Compose Navigation (official)
   - Decompose Navigation
   - Appyx Navigation

2. **Implement custom navigation:**
   - State-based navigation with `MutableState<Screen>`
   - SharedFlow-based navigation events

3. **Wait for Voyager fix:**
   - Track issue: https://github.com/adrielcafe/voyager/issues
   - Voyager is actively maintained

### Example: Alternative Navigation Pattern

Instead of Voyager, you can use state-based navigation:

```kotlin
// Define navigation state
sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    data class Profile(val userId: String) : Screen()
}

// Navigation feature
interface NavigationFeature : RelayFeature {
    fun navigateTo(screen: Screen)
}

// Implementation
class StateNavigationImpl(
    private val navigateTo: (Screen) -> Unit
) : NavigationFeature {
    override fun navigateTo(screen: Screen) {
        navigateTo.invoke(screen)
    }
}

// In Composable
@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    LaunchedEffect(Unit) {
        KRelay.register(StateNavigationImpl { screen ->
            currentScreen = screen
        })
    }

    when (val screen = currentScreen) {
        is Screen.Login -> LoginScreen()
        is Screen.Home -> HomeScreen()
        is Screen.Profile -> ProfileScreen(screen.userId)
    }
}
```

This approach:
- ✅ No lifecycle issues
- ✅ Works perfectly with KRelay
- ✅ Simple and predictable
- ✅ Easy to test

### Timeline

- **2026-01-23:** Issue identified and documented
- **Status:** Voyager demo disabled until library fix
- **Workaround:** Use alternative navigation or Basic/Integrations demos

### For Contributors

If you want to help fix this:

1. Check Voyager's GitHub issues
2. Try newer Voyager versions (current: check gradle libs.versions.toml)
3. Submit PR to Voyager if you find a fix
4. Update KRelay demo when fix is available

### References

- Voyager repository: https://github.com/adrielcafe/voyager
- AndroidX Lifecycle: https://developer.android.com/topic/libraries/architecture/lifecycle
- Related Voyager issues: (search for "AndroidScreenLifecycleOwner")

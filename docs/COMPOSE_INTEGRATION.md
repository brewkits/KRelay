# KRelay + Compose Multiplatform Integration

This guide covers idiomatic patterns for integrating KRelay with Compose Multiplatform.

---

## Core Pattern: `DisposableEffect` Registration

The most idiomatic approach is to use `DisposableEffect` to tie registration/unregistration to the Compose lifecycle:

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current

    // Register feature implementation tied to composition lifecycle
    DisposableEffect(Unit) {
        val toastImpl = object : ToastFeature {
            override fun show(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        KRelay.register<ToastFeature>(toastImpl)

        onDispose {
            KRelay.unregister<ToastFeature>()
        }
    }

    // ... UI content
}
```

---

## Reusable `rememberKRelayImpl` Helper

Extract the pattern into a reusable composable helper:

```kotlin
/**
 * Remembers and registers a KRelay feature implementation.
 * Automatically unregisters when the composition leaves.
 *
 * @param instance The KRelayInstance to register on (default: KRelay singleton)
 * @param factory  Factory to create the feature implementation
 */
@Composable
inline fun <reified T : RelayFeature> rememberKRelayImpl(
    instance: KRelayInstance = KRelay.defaultInstance,
    crossinline factory: @DisallowComposableCalls () -> T
): T {
    val impl = remember { factory() }
    DisposableEffect(impl) {
        instance.register<T>(impl)
        onDispose { instance.unregister<T>() }
    }
    return impl
}

// Usage:
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    rememberKRelayImpl<ToastFeature> {
        object : ToastFeature {
            override fun show(message: String) =
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    // ... UI
}
```

> **Copy this into your project** — KRelay's core library has zero Compose dependencies,
> so this helper lives in your app code or a `krelay-compose` module you create.

---

## With Instance API (DI + Koin)

```kotlin
// Koin module
val appModule = module {
    single { KRelay.create("AppScope") }
    viewModel { HomeViewModel(krelay = get()) }
}

// Composable
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    krelay: KRelayInstance = koinInject()
) {
    val context = LocalContext.current

    DisposableEffect(krelay) {
        val impl = object : ToastFeature {
            override fun show(message: String) =
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        krelay.register<ToastFeature>(impl)
        onDispose { krelay.unregister<ToastFeature>() }
    }

    // ... UI
}
```

---

## Navigation-Aware Registration

When using navigation libraries (Voyager, Decompose, Navigation Compose), register on the screen level to ensure correct lifecycle alignment:

### Voyager

```kotlin
class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        DisposableEffect(Unit) {
            val navImpl = object : NavigationFeature {
                override fun navigateTo(screen: String) {
                    when (screen) {
                        "detail" -> navigator.push(DetailScreen())
                        "back" -> navigator.pop()
                    }
                }
            }
            KRelay.register<NavigationFeature>(navImpl)
            onDispose { KRelay.unregister<NavigationFeature>() }
        }

        HomeContent()
    }
}
```

### Navigation Compose

```kotlin
@Composable
fun AppNavigation(navController: NavController) {
    NavHost(navController, startDestination = "home") {
        composable("home") {
            // Register on NavBackStackEntry lifecycle for proper backstack handling
            DisposableEffect(it) {
                val navImpl = object : NavigationFeature {
                    override fun navigateTo(screen: String) {
                        navController.navigate(screen)
                    }
                }
                KRelay.register<NavigationFeature>(navImpl)
                onDispose { KRelay.unregister<NavigationFeature>() }
            }

            HomeScreen()
        }
    }
}
```

---

## Dialog / Permission Requests

Use a `ManagedKRelayImpl` pattern that holds state for dialog visibility:

```kotlin
@Composable
fun HomeScreen() {
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Register a permission feature impl
    DisposableEffect(Unit) {
        val permImpl = object : PermissionFeature {
            override fun requestCamera() {
                showPermissionDialog = true
            }
        }
        KRelay.register<PermissionFeature>(permImpl)
        onDispose { KRelay.unregister<PermissionFeature>() }
    }

    // Show dialog when triggered by ViewModel
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Camera Permission") },
            text = { Text("This app needs camera access") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    // Request actual permission ...
                }) { Text("Allow") }
            }
        )
    }
}
```

---

## SnackBar / Toast via SnackbarHostState

The recommended Compose-native approach for notifications:

```kotlin
@Composable
fun HomeScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(snackbarHostState) {
        val toastImpl = object : ToastFeature {
            override fun show(message: String) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
        KRelay.register<ToastFeature>(toastImpl)
        onDispose { KRelay.unregister<ToastFeature>() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // ... content
    }
}
```

---

## Testing Composables with KRelay

Use `KRelay.create()` for isolated testing:

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun homeScreen_showsToast_whenViewModelDispatches() {
    val krelay = KRelay.create("TestScope")
    var shownMessage: String? = null

    composeTestRule.setContent {
        DisposableEffect(Unit) {
            krelay.register<ToastFeature>(object : ToastFeature {
                override fun show(message: String) { shownMessage = message }
            })
            onDispose { krelay.unregister<ToastFeature>() }
        }
    }

    krelay.dispatch<ToastFeature> { it.show("Hello Test") }
    composeTestRule.waitForIdle()

    assertEquals("Hello Test", shownMessage)
    krelay.reset()
}
```

---

## Summary: Registration Lifecycle Mapping

| Compose Scope | Use |
|---------------|-----|
| Screen/full composable | `DisposableEffect(Unit)` |
| Shared between tabs | Register at NavGraph level |
| Instance scoped to module | `DisposableEffect(krelayInstance)` |
| Dialog/Sheet content | Register inside dialog composable |
| Multiple screens need same feature | Use Instance API + DI |

# KRelay Lifecycle Integration Guide

This guide covers the correct lifecycle integration for KRelay on both Android and iOS.

---

## The Golden Rule

> **Register** when your UI is ready to receive commands.
> **Unregister** (or rely on WeakRef GC) when it is not.
> **clearQueue** in ViewModel's `onCleared()` to prevent lambda capture leaks.

---

## Android

### Activity

```kotlin
class MainActivity : AppCompatActivity(), ToastFeature, NavigationFeature {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register after the view is created
        KRelay.register<ToastFeature>(this)
        KRelay.register<NavigationFeature>(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optional: WeakRef clears automatically on GC, but explicit unregister
        // is recommended for activities that may be recreated (e.g. rotation)
        KRelay.unregister<ToastFeature>()
        KRelay.unregister<NavigationFeature>()
    }

    // ToastFeature
    override fun show(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // NavigationFeature
    override fun navigateTo(screen: String) {
        // ...
    }
}
```

**Screen Rotation Behavior:**
1. `onDestroy()` — WeakRef clears (old Activity GC'd)
2. `onCreate()` — new Activity registers
3. Any queued actions from ViewModel are **automatically replayed** ✅

### Fragment

```kotlin
class HomeFragment : Fragment(), ToastFeature {

    // Use onStart/onStop to avoid double-registration during backstack
    override fun onStart() {
        super.onStart()
        KRelay.register<ToastFeature>(this)
    }

    override fun onStop() {
        super.onStop()
        KRelay.unregister<ToastFeature>()
    }

    override fun show(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
}
```

> **Why `onStart`/`onStop` for Fragments?**
> Using `onResume`/`onPause` can miss dispatches when the fragment is visible but paused (e.g., dialog on top). Using `onStart`/`onStop` ensures registration aligns with visibility.

### ViewModel (dispatch side)

```kotlin
class LoginViewModel : ViewModel() {

    fun onLoginSuccess(user: User) {
        val welcomeMsg = "Welcome, ${user.name}!"
        KRelay.dispatch<ToastFeature> { it.show(welcomeMsg) }
        KRelay.dispatch<NavigationFeature> { it.navigateTo("home") }
    }

    override fun onCleared() {
        super.onCleared()
        // Prevent lambda capture leaks: clear any pending actions when ViewModel dies
        KRelay.clearQueue<ToastFeature>()
        KRelay.clearQueue<NavigationFeature>()
    }
}
```

### Jetpack Compose

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current

    // Register/unregister tied to composition lifecycle
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

    // ... UI content ...
}
```

---

## iOS

### UIViewController

```swift
class HomeViewController: UIViewController, ToastFeature {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Register after view is created
        KRelayIosHelper.shared.register(feature: ToastFeature.self, impl: self)
    }

    deinit {
        // WeakRef clears automatically when VC is deallocated
        // Explicit unregister is optional but recommended for clarity
        KRelayIosHelper.shared.unregister(feature: ToastFeature.self)
    }

    // ToastFeature implementation
    func show(message: String) {
        // Show a toast/snackbar equivalent
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            alert.dismiss(animated: true)
        }
    }
}
```

### SwiftUI

```swift
struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        ContentView()
            .onAppear {
                KRelayIosHelper.shared.register(feature: ToastFeature.self, impl: ToastHandler())
            }
            .onDisappear {
                KRelayIosHelper.shared.unregister(feature: ToastFeature.self)
            }
    }
}
```

---

## Instance API (v2.0)

When using isolated instances (e.g. with Koin/Hilt), pass the instance to both the ViewModel and the platform layer:

```kotlin
// Shared module setup
val relayInstance = KRelay.create("Checkout")

// Android — inject into Activity
class CheckoutActivity : AppCompatActivity(), PaymentResultFeature {
    // Inject via DI (Hilt/Koin)
    @Inject lateinit var relay: KRelayInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        relay.register<PaymentResultFeature>(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        relay.unregister<PaymentResultFeature>()
    }
}

// ViewModel
class CheckoutViewModel(private val relay: KRelayInstance) : ViewModel() {
    fun onPaymentSuccess() {
        relay.dispatch<PaymentResultFeature> { it.showSuccess() }
    }

    override fun onCleared() {
        super.onCleared()
        relay.clearQueue<PaymentResultFeature>()
    }
}
```

---

## Common Mistakes

| Mistake | Problem | Fix |
|---------|---------|-----|
| Register in `Application.onCreate()` | No UI context available, UI impl will be null | Register in Activity/Fragment `onCreate` |
| Never call `clearQueue()` in `onCleared()` | Lambda capture memory leak | Always call `clearQueue<T>()` for each dispatched feature |
| Register same feature in multiple fragments simultaneously | Second registration logs a warning; first impl is overwritten | Use different feature interfaces or use Instance API for isolation |
| Dispatch from background thread | Thread safety handled, but always verify | KRelay dispatches on main thread automatically — no special handling needed |
| Dispatch after `onDestroy` without registration | Action queued indefinitely | Set reasonable `actionExpiryMs` (default: 5 min) |

---

## Lifecycle State Machine

```
ViewModel Created
  │
  ├─ dispatch("X")          → [queued, no impl]
  │
Activity/VC onCreate
  ├─ register(impl)          → replays "X" on main thread ✅
  │
  ├─ dispatch("Y")          → executes immediately ✅
  │
[Screen Rotation]
  ├─ Activity onDestroy      → WeakRef clears (impl GC'd)
  ├─ dispatch("Z")          → [queued again]
  ├─ Activity onCreate       → register(newImpl) → replays "Z" ✅
  │
ViewModel onCleared
  └─ clearQueue<T>()         → any un-replayed actions released ✅
```

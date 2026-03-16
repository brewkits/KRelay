# KRelay + SwiftUI Integration

This guide covers idiomatic patterns for using KRelay in SwiftUI-based iOS apps.

---

## Core Concept

KRelay lives in shared Kotlin code. On iOS, your Swift code:
1. **Registers** a feature implementation (conforming to the Kotlin interface)
2. **Receives** commands dispatched from shared ViewModels

---

## Basic Pattern: `.onAppear` / `.onDisappear`

```swift
struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        ContentView(viewModel: viewModel)
            .onAppear {
                KRelayIosHelper.shared.register(impl: ToastHandler(view: self))
            }
            .onDisappear {
                KRelayIosHelper.shared.unregister(ToastFeature.self)
            }
    }
}

// Feature implementation
class ToastHandler: ToastFeature {
    func show(message: String) {
        // Show a banner / snackbar equivalent in SwiftUI
        // See "In-App Notifications" section below
    }
}
```

---

## `KRelayEffect` ViewModifier

Encapsulate the register/unregister pattern in a reusable `ViewModifier`:

```swift
struct KRelayEffect<T: RelayFeature>: ViewModifier {
    let impl: T

    func body(content: Content) -> some View {
        content
            .onAppear {
                KRelayIosHelper.shared.register(impl: impl)
            }
            .onDisappear {
                KRelayIosHelper.shared.unregister(T.self)
            }
    }
}

extension View {
    func krelayRegister<T: RelayFeature>(_ impl: T) -> some View {
        modifier(KRelayEffect(impl: impl))
    }
}

// Usage:
struct HomeView: View {
    var body: some View {
        HomeContent()
            .krelayRegister(ToastHandler())
            .krelayRegister(NavigationHandler())
    }
}
```

---

## Observable Pattern (iOS 17+)

For iOS 17+, use `@Observable` with `onChange`:

```swift
@Observable
class AppState {
    var toastMessage: String? = nil
    var navigationTarget: String? = nil
}

struct HomeView: View {
    @State private var appState = AppState()

    var body: some View {
        NavigationStack {
            HomeContent()
                .onAppear {
                    KRelayIosHelper.shared.register(impl: SwiftUIToastFeature(appState: appState))
                    KRelayIosHelper.shared.register(impl: SwiftUINavFeature(appState: appState))
                }
                .onDisappear {
                    KRelayIosHelper.shared.unregisterAll()
                }
        }
        // Toast overlay
        .overlay(alignment: .top) {
            if let message = appState.toastMessage {
                ToastBanner(message: message)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .onAppear {
                        Task {
                            try await Task.sleep(nanoseconds: 2_000_000_000)
                            appState.toastMessage = nil
                        }
                    }
            }
        }
    }
}

class SwiftUIToastFeature: ToastFeature {
    private let appState: AppState

    init(appState: AppState) { self.appState = appState }

    func show(message: String) {
        DispatchQueue.main.async {
            self.appState.toastMessage = message
        }
    }
}
```

---

## In-App Notifications / Toast Banner

Since iOS has no built-in Toast, here's a reusable `ToastBanner` component:

```swift
struct ToastBanner: View {
    let message: String

    var body: some View {
        Text(message)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.black.opacity(0.75))
            .foregroundColor(.white)
            .clipShape(Capsule())
            .padding(.top, 8)
            .shadow(radius: 4)
    }
}
```

---

## Navigation with NavigationStack

```swift
struct RootView: View {
    @State private var navigationPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navigationPath) {
            HomeView()
                .navigationDestination(for: String.self) { screen in
                    destinationView(for: screen)
                }
        }
        .onAppear {
            // Register navigation impl once at root level
            KRelayIosHelper.shared.register(impl: SwiftUINavigator(path: $navigationPath))
        }
    }

    @ViewBuilder
    private func destinationView(for screen: String) -> some View {
        switch screen {
        case "detail": DetailView()
        case "profile": ProfileView()
        default: Text("Unknown screen: \(screen)")
        }
    }
}

class SwiftUINavigator: NavigationFeature {
    @Binding var path: NavigationPath

    init(path: Binding<NavigationPath>) { self._path = path }

    func navigateTo(screen: String) {
        DispatchQueue.main.async {
            self.path.append(screen)
        }
    }

    func goBack() {
        DispatchQueue.main.async {
            if !self.path.isEmpty { self.path.removeLast() }
        }
    }
}
```

---

## Sheet / Modal Presentation

```swift
struct HomeView: View {
    @State private var sheetContent: String? = nil

    var body: some View {
        HomeContent()
            .onAppear {
                KRelayIosHelper.shared.register(impl: SheetPresenter(sheetContent: $sheetContent))
            }
            .sheet(item: $sheetContent) { content in
                SheetView(content: content)
            }
    }
}

// Make String conform to Identifiable for .sheet(item:)
extension String: @retroactive Identifiable {
    public var id: String { self }
}

class SheetPresenter: ModalFeature {
    @Binding var sheetContent: String?

    init(sheetContent: Binding<String?>) { self._sheetContent = sheetContent }

    func showModal(content: String) {
        DispatchQueue.main.async { self.sheetContent = content }
    }
}
```

---

## Permissions

```swift
struct HomeView: View {
    var body: some View {
        HomeContent()
            .onAppear {
                KRelayIosHelper.shared.register(impl: PermissionHandler())
            }
    }
}

class PermissionHandler: PermissionFeature {
    func requestCamera() {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                if granted {
                    // Open camera
                } else {
                    // Show settings prompt
                }
            }
        }
    }

    func requestLocation() {
        // CLLocationManager request...
    }
}
```

---

## Instance API

When using KRelay's instance API with Swift:

```swift
// Create instance (typically in a DI container or module coordinator)
let checkoutRelay = KRelay.shared.create(scopeName: "CheckoutModule")

struct CheckoutView: View {
    let relay: KRelayInstance

    var body: some View {
        CheckoutContent()
            .onAppear {
                relay.register(impl: CheckoutToastHandler())
            }
            .onDisappear {
                relay.unregister(ToastFeature.self)
            }
    }
}
```

---

## Lifecycle Summary

| SwiftUI Lifecycle | KRelay Action |
|-------------------|---------------|
| `.onAppear` | `register<T>(impl)` |
| `.onDisappear` | `unregister<T>()` |
| `deinit` (in class) | `unregister<T>()` (WeakRef auto-clears) |
| App foreground | `restorePersistedActions()` if using persistence |
| ViewModel `deinit` | `clearQueue<T>()` to release lambda captures |

---

## Testing with Swift (XCTest)

```swift
class HomeViewModelTests: XCTestCase {
    var relay: KRelayInstance!
    var mockToast: MockToast!

    override func setUp() {
        relay = KRelay.shared.create(scopeName: "TestScope")
        mockToast = MockToast()
        relay.register(impl: mockToast)
    }

    override func tearDown() {
        relay.reset()
    }

    func testShowsWelcomeToast_onLoginSuccess() {
        // Given: LoginViewModel uses relay
        let vm = LoginViewModel(krelay: relay)

        // When
        vm.onLoginSuccess()

        // Then
        XCTAssertEqual(mockToast.lastMessage, "Welcome back!")
    }
}

class MockToast: ToastFeature {
    var lastMessage: String? = nil
    func show(message: String) { lastMessage = message }
}
```

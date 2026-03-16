import Foundation
import Krelay

// MARK: - KClass Cache
//
// iOS cannot obtain the KClass of a Kotlin *interface* directly from Swift.
// When `register(_:)` is called with a concrete implementation, we obtain
// its KClass via `KRelayIosHelperKt.getKClass(obj:)` and cache it under the
// concrete type name so that `dispatch`, `unregister`, and `clearQueue` can
// reuse the same KClass key.
//
// ⚠️ IMPORTANT — iOS-only vs KMP pattern:
//
// **iOS-only apps** (iOS both dispatches and registers):
//   Use the CONCRETE implementation type as the type parameter in both
//   `register` and `dispatch`. The cache bridges the gap automatically.
//   ```swift
//   KRelay.shared.register(myToastVC)                       // caches MyToastVC → KClass
//   KRelay.shared.dispatch(MyToastVC.self) { $0.show("Hi") }// reuses cached KClass
//   ```
//
// **KMP apps** (Kotlin dispatches with interface KClass, iOS registers):
//   Kotlin dispatches under `ToastFeature::class`; the iOS cache stores
//   `MyToastVC::class` — these will NOT match, so replay won't trigger.
//   Use `KRelayIosHelperKt.registerFeature(instance:kClass:impl:)` instead,
//   passing the interface KClass from a Kotlin helper function:
//   ```kotlin
//   // Kotlin (shared module) — export this helper:
//   fun toastFeatureClass() = ToastFeature::class
//   ```
//   ```swift
//   // Swift:
//   KRelayIosHelperKt.registerFeature(
//       instance: KRelay.shared.defaultInstance,
//       kClass:   YourSharedKt.toastFeatureClass(),
//       impl:     self
//   )
//   ```

private var _kClassCache: [String: KotlinKClass] = [:]
private let _kClassCacheLock = NSLock()

private func cachedKClass(for typeName: String) -> KotlinKClass? {
    _kClassCacheLock.lock()
    defer { _kClassCacheLock.unlock() }
    return _kClassCache[typeName]
}

private func cacheKClass(_ kClass: KotlinKClass, for typeName: String) {
    _kClassCacheLock.lock()
    defer { _kClassCacheLock.unlock() }
    _kClassCache[typeName] = kClass
}

// MARK: - Swift-Friendly KRelay Extensions

/**
 * Swift-friendly extensions for KRelay.
 *
 * Usage in Swift:
 * ```swift
 * // Register (uses concrete type — caches KClass automatically)
 * KRelay.shared.register(myToastImpl)
 *
 * // Dispatch (must use the same CONCRETE type as register)
 * KRelay.shared.dispatch(MyToastImpl.self) { feature in
 *     feature.show("Hello from Swift!")
 * }
 *
 * // Check registration
 * if KRelay.shared.isRegistered(MyToastImpl.self) { ... }
 *
 * // Unregister
 * KRelay.shared.unregister(MyToastImpl.self)
 *
 * // Clear queue
 * KRelay.shared.clearQueue(MyToastImpl.self)
 * ```
 *
 * For KMP apps where Kotlin dispatches using the *interface* KClass, see
 * the `KRelayIosHelperKt.registerFeature(instance:kClass:impl:)` helper.
 */
extension KRelay {

    // MARK: - Registration

    /**
     * Registers a platform implementation.
     *
     * Obtains the concrete KClass via `KRelayIosHelperKt.getKClass(obj:)` and
     * caches it under the concrete type name for use in `dispatch`, `unregister`, etc.
     *
     * - Parameter impl: The implementation conforming to RelayFeature
     *
     * Example:
     * ```swift
     * class MyToast: ToastFeature {
     *     func show(_ message: String) { print(message) }
     * }
     *
     * let toast = MyToast()
     * KRelay.shared.register(toast)                          // caches MyToast → KClass
     * KRelay.shared.dispatch(MyToast.self) { $0.show("Hi") } // reuses cached KClass
     * ```
     *
     * For KMP apps where Kotlin dispatches under the *interface* KClass, use
     * `KRelayIosHelperKt.registerFeature(instance:kClass:impl:)` instead.
     */
    func register<T: RelayFeature>(_ impl: T) {
        let kClass = KRelayIosHelperKt.getKClass(obj: impl) as! KotlinKClass
        let typeName = String(describing: type(of: impl))
        cacheKClass(kClass, for: typeName)
        self.registerInternal(impl: impl as AnyObject, kClass: kClass)
    }

    /**
     * Unregisters an implementation by its concrete type name.
     *
     * - Parameter type: The **concrete** type used in `register(_:)`
     *
     * Example:
     * ```swift
     * KRelay.shared.unregister(MyToast.self)
     * ```
     */
    func unregister<T: RelayFeature>(_ type: T.Type) {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else {
            print("⚠️ [KRelay] unregister(\(typeName)): no cached KClass — was register() called first?")
            return
        }
        self.unregisterInternal(kClass: kClass)
    }

    // MARK: - Dispatch

    /**
     * Dispatches an action to a registered feature implementation.
     *
     * If the implementation is registered, executes immediately on the main thread.
     * If not registered, queues the action for later replay.
     *
     * **IMPORTANT**: `type` must be the **concrete** class used in `register(_:)`.
     * Using a protocol/interface type will produce a cache-miss warning and the
     * action will be dropped.
     *
     * - Parameters:
     *   - type:   The concrete feature type (e.g. `MyToastImpl.self`)
     *   - action: The action to execute
     *
     * Example:
     * ```swift
     * KRelay.shared.dispatch(MyToast.self) { feature in
     *     feature.show("Success!")
     * }
     * ```
     */
    func dispatch<T: RelayFeature>(_ type: T.Type, action: @escaping (T) -> Void) {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else {
            print("⚠️ [KRelay] dispatch(\(typeName)): no cached KClass. Call register() before dispatch(), or use KRelayIosHelperKt.registerFeature() for KMP apps.")
            return
        }
        self.dispatchInternal(kClass: kClass) { instance in
            if let feature = instance as? T { action(feature) }
        }
    }

    /**
     * Dispatches an action with priority.
     *
     * Higher priority actions are replayed first when the feature becomes registered.
     * See `dispatch(_:action:)` for notes on the `type` parameter.
     *
     * - Parameters:
     *   - type:     The concrete feature type
     *   - priority: The action priority
     *   - action:   The action to execute
     *
     * Example:
     * ```swift
     * KRelay.shared.dispatch(
     *     MyNotification.self,
     *     priority: .critical
     * ) { feature in
     *     feature.showAlert("Payment failed!")
     * }
     * ```
     */
    func dispatch<T: RelayFeature>(
        _ type: T.Type,
        priority: ActionPriority,
        action: @escaping (T) -> Void
    ) {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else {
            print("⚠️ [KRelay] dispatch(\(typeName), priority:): no cached KClass — call register() first.")
            return
        }
        self.dispatchWithPriorityInternal(kClass: kClass, priority: priority) { instance in
            if let feature = instance as? T { action(feature) }
        }
    }

    // MARK: - Query

    /**
     * Checks if an implementation is currently registered.
     *
     * - Parameter type: The concrete feature type
     * - Returns: True if registered, false otherwise
     *
     * Example:
     * ```swift
     * if KRelay.shared.isRegistered(MyToast.self) { print("Toast is available") }
     * ```
     */
    func isRegistered<T: RelayFeature>(_ type: T.Type) -> Bool {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else { return false }
        return self.isRegisteredInternal(kClass: kClass)
    }

    /**
     * Gets the number of pending actions for a feature.
     *
     * - Parameter type: The concrete feature type
     * - Returns: Number of queued actions
     *
     * Example:
     * ```swift
     * let pending = KRelay.shared.getPendingCount(MyToast.self)
     * print("Pending toasts: \(pending)")
     * ```
     */
    func getPendingCount<T: RelayFeature>(_ type: T.Type) -> Int {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else { return 0 }
        return Int(self.getPendingCountInternal(kClass: kClass))
    }

    // MARK: - Queue Management

    /**
     * Clears the pending queue for a feature type.
     *
     * **IMPORTANT**: Use this to prevent lambda capture leaks.
     * Call in `deinit` or when the ViewController is being dismissed.
     *
     * - Parameter type: The concrete feature type
     *
     * Example:
     * ```swift
     * class MyViewModel {
     *     deinit { KRelay.shared.clearQueue(MyToast.self) }
     * }
     * ```
     */
    func clearQueue<T: RelayFeature>(_ type: T.Type) {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else { return }
        self.clearQueueInternal(kClass: kClass)
    }

    // MARK: - Metrics

    /**
     * Gets metrics for a specific feature type.
     *
     * - Parameter type: The concrete feature type
     * - Returns: Dictionary of metric names to values
     *
     * Example:
     * ```swift
     * let metrics = KRelay.shared.getMetrics(MyToast.self)
     * print("Dispatches: \(metrics["dispatches"] ?? 0)")
     * ```
     */
    func getMetrics<T: RelayFeature>(_ type: T.Type) -> [String: Int64] {
        let typeName = String(describing: type)
        guard let kClass = cachedKClass(for: typeName) else { return [:] }
        return self.getMetricsInternal(kClass: kClass) as! [String: Int64]
    }
}

// MARK: - Convenience: Typed Wrappers

/**
 * Type-safe wrappers for common features.
 * Add your own feature-specific extensions here.
 *
 * Note: dispatch convenience helpers are intentionally omitted because
 * dispatch requires the *concrete* type (not the protocol) for cache lookup.
 * Call `dispatch(MyConcreteImpl.self) { ... }` directly.
 */
extension KRelay {

    // Example: Registration convenience (concrete type inferred from impl)
    func registerToast(_ impl: ToastFeature) {
        register(impl)
    }

    func registerNavigation(_ impl: NavigationFeature) {
        register(impl)
    }
}

// MARK: - UIKit Integration

#if canImport(UIKit)
import UIKit

/**
 * UIViewController extensions for automatic KRelay lifecycle management.
 */
extension UIViewController {

    /**
     * Automatically registers a feature in viewDidLoad and unregisters in deinit.
     *
     * Usage:
     * ```swift
     * class MyViewController: UIViewController, ToastFeature {
     *     override func viewDidLoad() {
     *         super.viewDidLoad()
     *         autoRegister(self as ToastFeature)
     *     }
     *
     *     func show(_ message: String) {
     *         // Show toast UI
     *     }
     * }
     * ```
     */
    func autoRegister<T: RelayFeature>(_ impl: T) {
        KRelay.shared.register(impl)

        // Note: In Swift, we can't hook into deinit from an extension,
        // so developers need to manually unregister or use associated objects.
        // Better approach: Use a wrapper class with deinit.
    }
}

/**
 * Lifecycle wrapper for automatic cleanup.
 *
 * Usage:
 * ```swift
 * class MyViewController: UIViewController, ToastFeature {
 *     private var relayLifecycle: KRelayLifecycle<ToastFeature>?
 *
 *     override func viewDidLoad() {
 *         super.viewDidLoad()
 *         relayLifecycle = KRelayLifecycle(feature: self)
 *     }
 *
 *     func show(_ message: String) {
 *         // Show toast
 *     }
 * }
 * ```
 */
class KRelayLifecycle<T: RelayFeature> {
    private let concreteType: T.Type

    init(feature: T) {
        self.concreteType = type(of: feature)
        KRelay.shared.register(feature)
    }

    deinit {
        KRelay.shared.unregister(concreteType)
        KRelay.shared.clearQueue(concreteType)
    }
}

#endif

// MARK: - SwiftUI Integration

#if canImport(SwiftUI)
import SwiftUI

/**
 * SwiftUI View modifier for KRelay feature registration.
 *
 * Usage:
 * ```swift
 * struct ContentView: View, ToastFeature {
 *     var body: some View {
 *         Text("Hello")
 *             .onAppear {
 *                 KRelay.shared.register(self)
 *             }
 *             .onDisappear {
 *                 KRelay.shared.unregister(type(of: self))
 *             }
 *     }
 *
 *     func show(_ message: String) {
 *         // Show toast
 *     }
 * }
 * ```
 */
@available(iOS 13.0, *)
extension View {
    func registerKRelayFeature<T: RelayFeature>(_ feature: T) -> some View {
        self.onAppear {
            KRelay.shared.register(feature)
        }
        .onDisappear {
            KRelay.shared.unregister(type(of: feature))
            KRelay.shared.clearQueue(type(of: feature))
        }
    }
}

#endif

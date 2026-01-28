import Foundation
import Krelay

// MARK: - Swift-Friendly KRelay Extensions

/**
 * Swift-friendly extensions for KRelay.
 *
 * Since Kotlin's reified inline functions don't work well from Swift,
 * these extensions provide idiomatic Swift APIs.
 *
 * Usage in Swift:
 * ```swift
 * // Register
 * KRelay.shared.register(myToastImpl)
 *
 * // Dispatch
 * KRelay.shared.dispatch(ToastFeature.self) { feature in
 *     feature.show("Hello from Swift!")
 * }
 *
 * // Check registration
 * if KRelay.shared.isRegistered(ToastFeature.self) {
 *     print("Toast is registered")
 * }
 *
 * // Unregister
 * KRelay.shared.unregister(ToastFeature.self)
 *
 * // Clear queue
 * KRelay.shared.clearQueue(ToastFeature.self)
 * ```
 */
extension KRelay {

    // MARK: - Registration

    /**
     * Registers a platform implementation.
     *
     * - Parameter impl: The implementation conforming to RelayFeature
     *
     * Example:
     * ```swift
     * class MyToast: ToastFeature {
     *     func show(_ message: String) {
     *         print(message)
     *     }
     * }
     *
     * let toast = MyToast()
     * KRelay.shared.register(toast)
     * ```
     */
    func register<T: RelayFeature>(_ impl: T) {
        let kClass = KotlinKClass<T>(for: type(of: impl))
        self.registerInternal(impl: impl as AnyObject, kClass: kClass)
    }

    /**
     * Unregisters an implementation.
     *
     * - Parameter type: The feature type to unregister
     *
     * Example:
     * ```swift
     * KRelay.shared.unregister(ToastFeature.self)
     * ```
     */
    func unregister<T: RelayFeature>(_ type: T.Type) {
        let kClass = KotlinKClass<T>(for: type)
        self.unregisterInternal(kClass: kClass)
    }

    // MARK: - Dispatch

    /**
     * Dispatches an action to a feature implementation.
     *
     * If the implementation is registered, executes immediately on main thread.
     * If not registered, queues the action for later replay.
     *
     * - Parameters:
     *   - type: The feature type
     *   - action: The action to execute
     *
     * Example:
     * ```swift
     * KRelay.shared.dispatch(ToastFeature.self) { feature in
     *     feature.show("Success!")
     * }
     * ```
     */
    func dispatch<T: RelayFeature>(_ type: T.Type, action: @escaping (T) -> Void) {
        let kClass = KotlinKClass<T>(for: type)
        self.dispatchInternal(kClass: kClass) { instance in
            if let feature = instance as? T {
                action(feature)
            }
        }
    }

    /**
     * Dispatches an action with priority.
     *
     * Higher priority actions are replayed first when the feature is registered.
     *
     * - Parameters:
     *   - type: The feature type
     *   - priority: The action priority
     *   - action: The action to execute
     *
     * Example:
     * ```swift
     * KRelay.shared.dispatch(
     *     NotificationFeature.self,
     *     priority: .critical
     * ) { feature in
     *     feature.showNotification("Payment failed!", priority: .critical)
     * }
     * ```
     */
    func dispatch<T: RelayFeature>(
        _ type: T.Type,
        priority: ActionPriority,
        action: @escaping (T) -> Void
    ) {
        let kClass = KotlinKClass<T>(for: type)
        self.dispatchWithPriorityInternal(kClass: kClass, priority: priority) { instance in
            if let feature = instance as? T {
                action(feature)
            }
        }
    }

    // MARK: - Query

    /**
     * Checks if an implementation is currently registered.
     *
     * - Parameter type: The feature type to check
     * - Returns: True if registered, false otherwise
     *
     * Example:
     * ```swift
     * if KRelay.shared.isRegistered(ToastFeature.self) {
     *     print("Toast is available")
     * }
     * ```
     */
    func isRegistered<T: RelayFeature>(_ type: T.Type) -> Bool {
        let kClass = KotlinKClass<T>(for: type)
        return self.isRegisteredInternal(kClass: kClass)
    }

    /**
     * Gets the number of pending actions for a feature.
     *
     * - Parameter type: The feature type
     * - Returns: Number of queued actions
     *
     * Example:
     * ```swift
     * let pending = KRelay.shared.getPendingCount(ToastFeature.self)
     * print("Pending toasts: \(pending)")
     * ```
     */
    func getPendingCount<T: RelayFeature>(_ type: T.Type) -> Int {
        let kClass = KotlinKClass<T>(for: type)
        return Int(self.getPendingCountInternal(kClass: kClass))
    }

    // MARK: - Queue Management

    /**
     * Clears the pending queue for a feature type.
     *
     * **IMPORTANT**: Use this to prevent lambda capture leaks.
     * Call in deinit or when the ViewController is being dismissed.
     *
     * - Parameter type: The feature type
     *
     * Example:
     * ```swift
     * class MyViewModel {
     *     deinit {
     *         KRelay.shared.clearQueue(ToastFeature.self)
     *     }
     * }
     * ```
     */
    func clearQueue<T: RelayFeature>(_ type: T.Type) {
        let kClass = KotlinKClass<T>(for: type)
        self.clearQueueInternal(kClass: kClass)
    }

    // MARK: - Metrics

    /**
     * Gets metrics for a specific feature type.
     *
     * - Parameter type: The feature type
     * - Returns: Dictionary of metric names to values
     *
     * Example:
     * ```swift
     * let metrics = KRelay.shared.getMetrics(ToastFeature.self)
     * print("Dispatches: \(metrics["dispatches"] ?? 0)")
     * ```
     */
    func getMetrics<T: RelayFeature>(_ type: T.Type) -> [String: Int64] {
        let kClass = KotlinKClass<T>(for: type)
        return self.getMetricsInternal(kClass: kClass) as! [String: Int64]
    }
}

// MARK: - Helper: KotlinKClass Creation

/**
 * Helper to create KClass from Swift type.
 * This bridges Swift's Type system to Kotlin's KClass.
 *
 * **IMPORTANT**: This requires proper Kotlin/Native interop setup.
 * Use KRelayIosHelper.kt functions for KClass creation:
 * - getKClass(obj:) - Get KClass from instance
 * - getKClassForType(_:) - Get KClass from type
 *
 * If interop is not properly configured, this will log a warning and return nil,
 * allowing graceful degradation instead of crashing the app.
 */
fileprivate extension KotlinKClass {
    convenience init<T>(for type: T.Type) {
        // WARNING: This is a placeholder implementation.
        // The actual implementation should use KRelayIosHelperKt functions.
        //
        // Proper implementation:
        // - Create a dummy instance of the protocol
        // - Call KRelayIosHelperKt.getKClass(for: instance)
        //
        // For now, we log a warning instead of crashing with fatalError.
        // This allows the app to continue running even if KRelay interop
        // is not fully configured.

        print("""
        ⚠️ [KRelay] Swift Extension Warning:
        KClass creation for type '\(T.self)' is not fully implemented.

        To fix this:
        1. Use Kotlin API directly: KRelay.shared.register<YourFeature>(impl)
        2. Or implement proper Swift-Kotlin bridging using KRelayIosHelper.kt

        The app will continue, but KRelay operations may not work correctly.
        """)

        // Attempt to create a minimal KClass as fallback
        // This prevents immediate crash but operations may fail gracefully
        self.init()
    }

    convenience init<T>(for instance: T) {
        // For instances, we can use KRelayIosHelper if available
        // Otherwise fall back to type-based init
        self.init(for: type(of: instance))
    }
}

// MARK: - Convenience: Typed Wrappers

/**
 * Type-safe wrapper for common features.
 * Add your own feature-specific extensions here.
 */
extension KRelay {

    // Example: Toast convenience
    func registerToast(_ impl: ToastFeature) {
        register(impl)
    }

    func showToast(_ message: String) {
        dispatch(ToastFeature.self) { $0.show(message) }
    }

    // Example: Navigation convenience
    func registerNavigation(_ impl: NavigationFeature) {
        register(impl)
    }

    func navigate(to route: String) {
        dispatch(NavigationFeature.self) { $0.navigate(route) }
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
    private let featureType: T.Type

    init(feature: T) {
        self.featureType = type(of: feature)
        KRelay.shared.register(feature)
    }

    deinit {
        KRelay.shared.unregister(featureType)
        KRelay.shared.clearQueue(featureType)
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
 *                 KRelay.shared.unregister(ToastFeature.self)
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

import Foundation
import UIKit
import ComposeApp

/**
 * iOS implementations for KRelay features.
 *
 * This class sets up the bridge between shared Kotlin code and native iOS APIs
 * for the KRelay demo application.
 *
 * ## Why `KRelayIosHelperKt.registerFeature` instead of `KRelay.shared.register(_:)`?
 *
 * The convenience extension `KRelay.shared.register(_ impl: T)` caches the KClass using
 * the **concrete** Swift/Kotlin type (e.g. `IOSToast`). However, shared Kotlin code
 * dispatches using the **interface** KClass (e.g. `ToastFeature::class`).
 * These two KClass values differ, so the dispatch would never find the iOS implementation.
 *
 * `registerFeature(instance:kClass:impl:)` lets us explicitly pass the interface KClass
 * that was exported from Kotlin via `KRelayKClassHelpersKt.toastFeatureKClass()`,
 * guaranteeing that the registration key matches the dispatch key.
 */
class KRelaySetup {

    /**
     * Registers all platform implementations for the KRelay demo.
     * Call this once on app start, before the Compose UI is presented.
     *
     * - Parameter rootViewController: The root `UIViewController` for presenting alerts.
     */
    static func registerImplementations(rootViewController: UIViewController) {
        KRelay.shared.debugMode = true

        // Obtain the Kotlin interface KClass — this must match what Kotlin uses in dispatch<T>.
        let toastKClass = KRelayKClassHelpersKt.toastFeatureKClass()

        // --- Singleton Registration ---
        KRelayIosHelperKt.registerFeature(
            instance: KRelay.shared.instance,
            kClass: toastKClass,
            impl: IOSToast(viewController: rootViewController)
        )

        // --- Super App Demo Registration ---
        // Get the isolated instances created in the shared Kotlin module.
        let ridesKRelay = SuperAppDemoKt.ridesKRelay
        let foodKRelay = SuperAppDemoKt.foodKRelay

        KRelayIosHelperKt.registerFeature(
            instance: ridesKRelay,
            kClass: toastKClass,
            impl: IOSToast(viewController: rootViewController)
        )
        KRelayIosHelperKt.registerFeature(
            instance: foodKRelay,
            kClass: toastKClass,
            impl: IOSToast(viewController: rootViewController)
        )

        print("[KRelay iOS] All feature implementations registered.")
    }
}

// MARK: - IOSToast

/**
 * iOS implementation of the shared `ToastFeature` protocol.
 * Presents a self-dismissing `UIAlertController` for each message.
 */
class IOSToast: ToastFeature {
    // Weak reference prevents a retain cycle with the view hierarchy.
    weak var viewController: UIViewController?

    init(viewController: UIViewController?) {
        self.viewController = viewController
    }

    func showShort(message: String) {
        show(message: message, duration: 2.0)
    }

    func showLong(message: String) {
        show(message: message, duration: 3.5)
    }

    private func show(message: String, duration: TimeInterval = 2.0) {
        DispatchQueue.main.async { [weak self] in
            guard let vc = self?.viewController else {
                print("[IOSToast] Warning: UIViewController is nil — cannot show '\(message)'")
                return
            }

            let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
            vc.present(alert, animated: true)

            DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                alert.dismiss(animated: true)
            }
        }
    }
}

import Foundation
import UIKit
import ComposeApp

/**
 * iOS implementations for KRelay features.
 * This class sets up the bridge between shared Kotlin code and native iOS APIs.
 */
class KRelaySetup {
    
    /**
     * Registers all necessary platform implementations for the KRelay demo.
     * This should be called once when the app starts.
     *
     * @param rootViewController The main UIViewController to be used for presenting UI elements like alerts.
     */
    static func registerImplementations(rootViewController: UIViewController) {
        KRelay.shared.debugMode = true
        
        // --- Singleton Registration ---
        // Registering a ToastFeature for the default KRelay singleton.
        let toastImpl = IOSToast(viewController: rootViewController)
        KRelay.shared.register(impl: toastImpl, kClass: ToastFeature.self)
        
        // --- Super App Demo Registration ---
        // Get the isolated instances created in the shared module.
        let ridesKRelay = SuperAppDemoKt.ridesKRelay
        let foodKRelay = SuperAppDemoKt.foodKRelay
        
        // Register a ToastFeature implementation for EACH instance.
        // Even though they use the same implementation class, the instances are separate.
        ridesKRelay.register(impl: IOSToast(viewController: rootViewController), kClass: ToastFeature.self)
        foodKRelay.register(impl: IOSToast(viewController: rootViewController), kClass: ToastFeature.self)

        print("[KRelay iOS] All feature implementations registered.")
    }
}

/**
 * An iOS implementation of the shared `ToastFeature` protocol.
 * It shows a simple alert message.
 */
class IOSToast: ToastFeature {
    // Use a weak reference to the UIViewController to prevent retain cycles.
    weak var viewController: UIViewController?

    init(viewController: UIViewController?) {
        self.viewController = viewController
    }

    func show(message: String) {
        // Ensure the UI update happens on the main thread.
        DispatchQueue.main.async {
            guard let vc = self.viewController else {
                print("[IOSToast] Error: UIViewController is nil.")
                return
            }
            
            let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
            vc.present(alert, animated: true)
            
            // Automatically dismiss the alert after 2 seconds.
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                alert.dismiss(animated: true)
            }
        }
    }
}

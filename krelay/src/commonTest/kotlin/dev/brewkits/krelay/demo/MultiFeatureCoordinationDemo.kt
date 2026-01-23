package dev.brewkits.krelay.demo

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Advanced Demo: Multi-Feature Coordination
 *
 * Demonstrates:
 * 1. Coordinating multiple features for complex workflows
 * 2. Shopping cart checkout flow
 * 3. Sequential feature activation
 * 4. Conditional feature usage
 * 5. Feature dependencies
 */
class MultiFeatureCoordinationDemo {

    // === FEATURES ===

    interface CartFeature : RelayFeature {
        fun updateItemCount(count: Int)
        fun showTotal(amount: Double)
        fun clearCart()
    }

    interface PaymentFeature : RelayFeature {
        fun processPayment(amount: Double, method: String)
        fun showPaymentSuccess(transactionId: String)
        fun showPaymentFailed(reason: String)
    }

    interface NavigationFeature : RelayFeature {
        fun navigateTo(screen: String)
        fun goBack()
    }

    interface NotificationFeature : RelayFeature {
        fun showNotification(message: String)
    }

    interface AnalyticsFeature : RelayFeature {
        fun trackEvent(name: String, properties: Map<String, Any>)
    }

    interface LoadingFeature : RelayFeature {
        fun show(message: String)
        fun hide()
    }

    // === IMPLEMENTATIONS ===

    class CartUI : CartFeature {
        var itemCount = 0
        var totalAmount = 0.0
        var isCleared = false

        override fun updateItemCount(count: Int) {
            itemCount = count
        }

        override fun showTotal(amount: Double) {
            totalAmount = amount
        }

        override fun clearCart() {
            isCleared = true
            itemCount = 0
            totalAmount = 0.0
        }
    }

    class PaymentProcessor : PaymentFeature {
        var lastTransactionId: String? = null
        var lastError: String? = null

        override fun processPayment(amount: Double, method: String) {
            // Simulated in tests
        }

        override fun showPaymentSuccess(transactionId: String) {
            lastTransactionId = transactionId
        }

        override fun showPaymentFailed(reason: String) {
            lastError = reason
        }
    }

    class Navigator : NavigationFeature {
        val navigationStack = mutableListOf("home")

        override fun navigateTo(screen: String) {
            navigationStack.add(screen)
        }

        override fun goBack() {
            if (navigationStack.size > 1) {
                navigationStack.removeLast()
            }
        }

        fun currentScreen() = navigationStack.lastOrNull() ?: "home"
    }

    class NotificationCenter : NotificationFeature {
        val notifications = mutableListOf<String>()

        override fun showNotification(message: String) {
            notifications.add(message)
        }
    }

    class Analytics : AnalyticsFeature {
        val events = mutableListOf<Pair<String, Map<String, Any>>>()

        override fun trackEvent(name: String, properties: Map<String, Any>) {
            events.add(name to properties)
        }
    }

    class LoadingDialog : LoadingFeature {
        var isVisible = false
        var message = ""

        override fun show(message: String) {
            isVisible = true
            this.message = message
        }

        override fun hide() {
            isVisible = false
            message = ""
        }
    }

    // === CHECKOUT VIEW MODEL ===

    class CheckoutViewModel {
        private var currentCart = listOf("Item1", "Item2", "Item3")
        private val totalAmount = 99.99

        fun startCheckout() {
            // Step 1: Show loading
            KRelay.dispatch<LoadingFeature> { it.show("Preparing checkout...") }

            // Step 2: Navigate to checkout
            KRelay.dispatch<NavigationFeature> { it.navigateTo("checkout") }

            // Step 3: Update cart display
            KRelay.dispatch<CartFeature> {
                it.updateItemCount(currentCart.size)
                it.showTotal(totalAmount)
            }

            // Step 4: Track analytics
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("checkout_started", mapOf(
                    "item_count" to currentCart.size,
                    "total" to totalAmount
                ))
            }

            // Step 5: Hide loading
            KRelay.dispatch<LoadingFeature> { it.hide() }
        }

        fun processPayment(paymentMethod: String) {
            // Step 1: Show processing
            KRelay.dispatch<LoadingFeature> { it.show("Processing payment...") }

            // Step 2: Track payment attempt
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("payment_attempted", mapOf(
                    "method" to paymentMethod,
                    "amount" to totalAmount
                ))
            }

            // Step 3: Process payment (simulated)
            val success = simulatePayment(paymentMethod)

            // Step 4: Hide loading
            KRelay.dispatch<LoadingFeature> { it.hide() }

            if (success) {
                handlePaymentSuccess()
            } else {
                handlePaymentFailure("Insufficient funds")
            }
        }

        private fun handlePaymentSuccess() {
            val transactionId = "TXN-${kotlin.random.Random.nextLong(100000, 999999)}"

            // Step 1: Show success
            KRelay.dispatchWithPriority<PaymentFeature>(ActionPriority.HIGH) {
                it.showPaymentSuccess(transactionId)
            }

            // Step 2: Clear cart
            KRelay.dispatch<CartFeature> { it.clearCart() }

            // Step 3: Navigate to success screen
            KRelay.dispatch<NavigationFeature> { it.navigateTo("order_success") }

            // Step 4: Show notification
            KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.HIGH) {
                it.showNotification("Order placed successfully!")
            }

            // Step 5: Track success
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("payment_success", mapOf(
                    "transaction_id" to transactionId,
                    "amount" to totalAmount
                ))
            }
        }

        private fun handlePaymentFailure(reason: String) {
            // Step 1: Show error
            KRelay.dispatchWithPriority<PaymentFeature>(ActionPriority.CRITICAL) {
                it.showPaymentFailed(reason)
            }

            // Step 2: Stay on payment screen (no navigation)

            // Step 3: Show notification
            KRelay.dispatchWithPriority<NotificationFeature>(ActionPriority.HIGH) {
                it.showNotification("Payment failed: $reason")
            }

            // Step 4: Track failure
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("payment_failed", mapOf(
                    "reason" to reason,
                    "amount" to totalAmount
                ))
            }
        }

        fun cancelCheckout() {
            // Step 1: Track cancellation
            KRelay.dispatch<AnalyticsFeature> {
                it.trackEvent("checkout_cancelled", emptyMap())
            }

            // Step 2: Go back
            KRelay.dispatch<NavigationFeature> { it.goBack() }

            // Step 3: Notify
            KRelay.dispatch<NotificationFeature> {
                it.showNotification("Checkout cancelled")
            }
        }

        private fun simulatePayment(method: String): Boolean {
            return method == "credit_card"
        }
    }

    // === TESTS (DOUBLES AS DEMO) ===

    @BeforeTest
    fun setup() {
        KRelay.reset()
        KRelay.debugMode = true
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun demo_CompleteCheckoutFlow_Success() {
        // === SETUP: Register all features ===
        val cart = CartUI()
        val payment = PaymentProcessor()
        val navigation = Navigator()
        val notifications = NotificationCenter()
        val analytics = Analytics()
        val loading = LoadingDialog()

        KRelay.register<CartFeature>(cart)
        KRelay.register<PaymentFeature>(payment)
        KRelay.register<NavigationFeature>(navigation)
        KRelay.register<NotificationFeature>(notifications)
        KRelay.register<AnalyticsFeature>(analytics)
        KRelay.register<LoadingFeature>(loading)

        // === ACTION: Complete checkout flow ===
        val viewModel = CheckoutViewModel()

        // Start checkout
        viewModel.startCheckout()

        assertEquals("checkout", navigation.currentScreen())
        assertEquals(3, cart.itemCount)
        assertTrue(analytics.events.any { it.first == "checkout_started" })

        // Process payment
        viewModel.processPayment("credit_card")

        // === VERIFY: Complete flow executed ===
        assertEquals("order_success", navigation.currentScreen())
        assertTrue(cart.isCleared)
        assertNotNull(payment.lastTransactionId)
        assertTrue(notifications.notifications.contains("Order placed successfully!"))
        assertTrue(analytics.events.any { it.first == "payment_success" })
        assertFalse(loading.isVisible)
    }

    @Test
    fun demo_CheckoutFlow_PaymentFailure() {
        // === SETUP ===
        val payment = PaymentProcessor()
        val navigation = Navigator()
        val notifications = NotificationCenter()
        val analytics = Analytics()

        KRelay.register<PaymentFeature>(payment)
        KRelay.register<NavigationFeature>(navigation)
        KRelay.register<NotificationFeature>(notifications)
        KRelay.register<AnalyticsFeature>(analytics)
        KRelay.register<CartFeature>(CartUI())
        KRelay.register<LoadingFeature>(LoadingDialog())

        // === ACTION ===
        val viewModel = CheckoutViewModel()
        viewModel.startCheckout()
        viewModel.processPayment("debit_card") // Will fail

        // === VERIFY: Stayed on checkout screen ===
        assertEquals("checkout", navigation.currentScreen())
        assertNotNull(payment.lastError)
        assertTrue(notifications.notifications.any { it.contains("Payment failed") })
        assertTrue(analytics.events.any { it.first == "payment_failed" })
    }

    @Test
    fun demo_CheckoutCancellation() {
        // === SETUP ===
        val navigation = Navigator()
        val notifications = NotificationCenter()
        val analytics = Analytics()

        KRelay.register<NavigationFeature>(navigation)
        KRelay.register<NotificationFeature>(notifications)
        KRelay.register<AnalyticsFeature>(analytics)
        KRelay.register<CartFeature>(CartUI())
        KRelay.register<LoadingFeature>(LoadingDialog())

        // === ACTION ===
        val viewModel = CheckoutViewModel()
        viewModel.startCheckout()
        assertEquals("checkout", navigation.currentScreen())

        viewModel.cancelCheckout()

        // === VERIFY: Returned to home ===
        assertEquals("home", navigation.currentScreen())
        assertTrue(notifications.notifications.contains("Checkout cancelled"))
        assertTrue(analytics.events.any { it.first == "checkout_cancelled" })
    }

    @Test
    fun demo_RotationDuringPayment() {
        // === SCENARIO: Device rotates during payment processing ===

        // Step 1: Start checkout
        val cart1 = CartUI()
        val loading1 = LoadingDialog()
        KRelay.register<CartFeature>(cart1)
        KRelay.register<LoadingFeature>(loading1)
        KRelay.register<NavigationFeature>(Navigator())
        KRelay.register<AnalyticsFeature>(Analytics())

        val viewModel = CheckoutViewModel()
        viewModel.startCheckout()

        // Step 2: Rotation happens (unregister UI)
        KRelay.unregister<CartFeature>()
        KRelay.unregister<PaymentFeature>()
        KRelay.unregister<NavigationFeature>()
        KRelay.unregister<NotificationFeature>()
        KRelay.unregister<LoadingFeature>()

        // Step 3: Payment completes during rotation
        viewModel.processPayment("credit_card")

        // Verify actions queued
        assertTrue(KRelay.getPendingCount<PaymentFeature>() > 0)
        assertTrue(KRelay.getPendingCount<NavigationFeature>() > 0)

        // Step 4: New Activity after rotation
        val cart2 = CartUI()
        val payment2 = PaymentProcessor()
        val navigation2 = Navigator()
        val notifications2 = NotificationCenter()

        KRelay.register<CartFeature>(cart2)
        KRelay.register<PaymentFeature>(payment2)
        KRelay.register<NavigationFeature>(navigation2)
        KRelay.register<NotificationFeature>(notifications2)
        KRelay.register<LoadingFeature>(LoadingDialog())

        // Step 5: Queued actions replayed
        assertTrue(cart2.isCleared)
        assertNotNull(payment2.lastTransactionId)
        assertTrue(navigation2.navigationStack.contains("order_success"))
        assertTrue(notifications2.notifications.contains("Order placed successfully!"))
    }

    @Test
    fun demo_PartialFeatureRegistration() {
        // === SCENARIO: Only some features registered ===

        // Only register cart and navigation
        val cart = CartUI()
        val navigation = Navigator()

        KRelay.register<CartFeature>(cart)
        KRelay.register<NavigationFeature>(navigation)

        // === ACTION: Start checkout (other features queued) ===
        val viewModel = CheckoutViewModel()
        viewModel.startCheckout()

        // === VERIFY: Cart and navigation work, others queued ===
        assertEquals(3, cart.itemCount)
        assertEquals("checkout", navigation.currentScreen())

        // Analytics and Loading are queued
        assertTrue(KRelay.getPendingCount<AnalyticsFeature>() > 0)
        assertTrue(KRelay.getPendingCount<LoadingFeature>() > 0)

        // === ACTION: Register remaining features ===
        val analytics = Analytics()
        val loading = LoadingDialog()

        KRelay.register<AnalyticsFeature>(analytics)
        KRelay.register<LoadingFeature>(loading)

        // === VERIFY: Queued actions replayed ===
        assertTrue(analytics.events.any { it.first == "checkout_started" })
        assertFalse(loading.isVisible) // Hide was the last action
    }
}

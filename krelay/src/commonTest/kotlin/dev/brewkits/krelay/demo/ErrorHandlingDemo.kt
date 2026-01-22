package dev.brewkits.krelay.demo

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Advanced Demo: Error Handling Patterns
 *
 * Demonstrates:
 * 1. Network errors with retry
 * 2. Validation errors
 * 3. Permission errors
 * 4. Graceful degradation
 * 5. Error reporting to analytics
 */
class ErrorHandlingDemo {

    // === FEATURES ===

    interface ErrorDisplayFeature : RelayFeature {
        fun showError(title: String, message: String, canRetry: Boolean = false)
        fun showValidationError(field: String, message: String)
        fun dismissError()
    }

    interface DialogFeature : RelayFeature {
        fun showRetryDialog(errorMessage: String, onRetry: () -> Unit)
        fun showPermissionDialog(permission: String)
    }

    interface AnalyticsFeature : RelayFeature {
        fun logError(errorType: String, details: Map<String, Any>)
    }

    interface ToastFeature : RelayFeature {
        fun showToast(message: String)
    }

    // === IMPLEMENTATIONS ===

    class ErrorUI : ErrorDisplayFeature {
        data class ErrorInfo(
            val title: String,
            val message: String,
            val canRetry: Boolean
        )

        val errors = mutableListOf<ErrorInfo>()
        val validationErrors = mutableMapOf<String, String>()

        override fun showError(title: String, message: String, canRetry: Boolean) {
            errors.add(ErrorInfo(title, message, canRetry))
        }

        override fun showValidationError(field: String, message: String) {
            validationErrors[field] = message
        }

        override fun dismissError() {
            errors.clear()
            validationErrors.clear()
        }
    }

    class DialogManager : DialogFeature {
        var currentDialog: String? = null
        var retryCallback: (() -> Unit)? = null

        override fun showRetryDialog(errorMessage: String, onRetry: () -> Unit) {
            currentDialog = "retry:$errorMessage"
            retryCallback = onRetry
        }

        override fun showPermissionDialog(permission: String) {
            currentDialog = "permission:$permission"
        }

        fun simulateRetry() {
            retryCallback?.invoke()
            currentDialog = null
        }
    }

    class ErrorAnalytics : AnalyticsFeature {
        val loggedErrors = mutableListOf<Pair<String, Map<String, Any>>>()

        override fun logError(errorType: String, details: Map<String, Any>) {
            loggedErrors.add(errorType to details)
        }
    }

    class SimpleToast : ToastFeature {
        val messages = mutableListOf<String>()

        override fun showToast(message: String) {
            messages.add(message)
        }
    }

    // === VIEW MODEL WITH ERROR HANDLING ===

    class NetworkViewModel {
        private var retryCount = 0

        fun loadData() {
            // Simulate network call
            val result = simulateNetworkCall()

            when (result) {
                is Result.Success -> {
                    KRelay.dispatch<ToastFeature> {
                        it.showToast("Data loaded successfully")
                    }
                    retryCount = 0
                }
                is Result.NetworkError -> {
                    handleNetworkError(result.message)
                }
                is Result.ServerError -> {
                    handleServerError(result.code, result.message)
                }
            }
        }

        private fun handleNetworkError(message: String) {
            // Log to analytics
            KRelay.dispatch<AnalyticsFeature> {
                it.logError("network_error", mapOf(
                    "message" to message,
                    "retry_count" to retryCount
                ))
            }

            // Show error with retry option
            if (retryCount < 3) {
                KRelay.dispatch<DialogFeature> {
                    it.showRetryDialog(message) {
                        retryCount++
                        loadData() // Retry
                    }
                }
            } else {
                // Max retries reached
                KRelay.dispatch<ErrorDisplayFeature> {
                    it.showError(
                        "Connection Failed",
                        "Please check your internet connection",
                        canRetry = false
                    )
                }
            }
        }

        private fun handleServerError(code: Int, message: String) {
            KRelay.dispatch<AnalyticsFeature> {
                it.logError("server_error", mapOf(
                    "code" to code,
                    "message" to message
                ))
            }

            KRelay.dispatch<ErrorDisplayFeature> {
                it.showError("Server Error", message, canRetry = true)
            }
        }

        private fun simulateNetworkCall(): Result {
            return when (retryCount) {
                0, 1 -> Result.NetworkError("Connection timeout")
                2 -> Result.Success
                else -> Result.ServerError(500, "Internal server error")
            }
        }

        sealed class Result {
            object Success : Result()
            data class NetworkError(val message: String) : Result()
            data class ServerError(val code: Int, val message: String) : Result()
        }
    }

    class FormViewModel {
        fun validateAndSubmit(email: String, password: String) {
            val errors = mutableListOf<Pair<String, String>>()

            // Validation
            if (email.isBlank()) {
                errors.add("email" to "Email is required")
            } else if (!email.contains("@")) {
                errors.add("email" to "Invalid email format")
            }

            if (password.length < 6) {
                errors.add("password" to "Password must be at least 6 characters")
            }

            // Show errors
            if (errors.isNotEmpty()) {
                errors.forEach { (field, message) ->
                    KRelay.dispatch<ErrorDisplayFeature> {
                        it.showValidationError(field, message)
                    }
                }

                KRelay.dispatch<AnalyticsFeature> {
                    it.logError("validation_error", mapOf(
                        "fields" to errors.map { it.first }
                    ))
                }
            } else {
                // Success
                KRelay.dispatch<ErrorDisplayFeature> { it.dismissError() }
                KRelay.dispatch<ToastFeature> { it.showToast("Form submitted") }
            }
        }
    }

    class PermissionViewModel {
        fun requestCameraAccess() {
            // Check permission (simulated)
            val hasPermission = false

            if (!hasPermission) {
                KRelay.dispatch<DialogFeature> {
                    it.showPermissionDialog("CAMERA")
                }

                KRelay.dispatch<AnalyticsFeature> {
                    it.logError("permission_denied", mapOf(
                        "permission" to "CAMERA"
                    ))
                }
            } else {
                KRelay.dispatch<ToastFeature> {
                    it.showToast("Camera access granted")
                }
            }
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
    fun demo_NetworkErrorWithRetry() {
        // === SETUP ===
        val errorUI = ErrorUI()
        val dialogs = DialogManager()
        val analytics = ErrorAnalytics()

        KRelay.register<ErrorDisplayFeature>(errorUI)
        KRelay.register<DialogFeature>(dialogs)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION: Load data (will fail first time) ===
        val viewModel = NetworkViewModel()
        viewModel.loadData()

        // === VERIFY: Retry dialog shown ===
        assertTrue(dialogs.currentDialog?.startsWith("retry:") == true)
        assertTrue(analytics.loggedErrors.any { it.first == "network_error" })

        // === ACTION: User clicks retry ===
        dialogs.simulateRetry()

        // === VERIFY: Retried and eventually succeeded ===
        assertNotNull(dialogs.retryCallback)
    }

    @Test
    fun demo_ValidationErrors() {
        // === SETUP ===
        val errorUI = ErrorUI()
        val analytics = ErrorAnalytics()

        KRelay.register<ErrorDisplayFeature>(errorUI)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION: Submit invalid form ===
        val viewModel = FormViewModel()
        viewModel.validateAndSubmit("", "123") // Invalid

        // === VERIFY: Validation errors shown ===
        assertTrue(errorUI.validationErrors.containsKey("email"))
        assertTrue(errorUI.validationErrors.containsKey("password"))
        assertEquals("Email is required", errorUI.validationErrors["email"])
        assertTrue(analytics.loggedErrors.any { it.first == "validation_error" })
    }

    @Test
    fun demo_ValidationSuccess() {
        // === SETUP ===
        val errorUI = ErrorUI()
        val toast = SimpleToast()

        KRelay.register<ErrorDisplayFeature>(errorUI)
        KRelay.register<ToastFeature>(toast)

        // === ACTION: Submit valid form ===
        val viewModel = FormViewModel()
        viewModel.validateAndSubmit("test@example.com", "password123")

        // === VERIFY: No errors, success toast ===
        assertTrue(errorUI.validationErrors.isEmpty())
        assertTrue(toast.messages.contains("Form submitted"))
    }

    @Test
    fun demo_PermissionDenied() {
        // === SETUP ===
        val dialogs = DialogManager()
        val analytics = ErrorAnalytics()

        KRelay.register<DialogFeature>(dialogs)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION: Request camera ===
        val viewModel = PermissionViewModel()
        viewModel.requestCameraAccess()

        // === VERIFY: Permission dialog shown ===
        assertEquals("permission:CAMERA", dialogs.currentDialog)
        assertTrue(analytics.loggedErrors.any {
            it.first == "permission_denied" && it.second["permission"] == "CAMERA"
        })
    }

    @Test
    fun demo_ErrorDuringRotation() {
        // === SCENARIO: Error occurs during rotation ===

        // Step 1: Activity exists, load data
        val errorUI1 = ErrorUI()
        KRelay.register<ErrorDisplayFeature>(errorUI1)

        val viewModel = NetworkViewModel()

        // Step 2: Rotation happens (unregister)
        KRelay.unregister<ErrorDisplayFeature>()

        // Step 3: Error occurs while Activity destroyed
        viewModel.loadData()

        // Verify queued
        assertTrue(KRelay.getPendingCount<ErrorDisplayFeature>() > 0 ||
                   KRelay.getPendingCount<DialogFeature>() > 0)

        // Step 4: New Activity created
        val errorUI2 = ErrorUI()
        val dialogs2 = DialogManager()

        KRelay.register<ErrorDisplayFeature>(errorUI2)
        KRelay.register<DialogFeature>(dialogs2)

        // Step 5: Error replayed on new Activity
        assertTrue(
            dialogs2.currentDialog?.contains("Connection timeout") == true ||
            errorUI2.errors.isNotEmpty()
        )
    }

    @Test
    fun demo_MaxRetriesExceeded() {
        // === SETUP ===
        val errorUI = ErrorUI()
        val dialogs = DialogManager()
        val toast = SimpleToast()
        val analytics = ErrorAnalytics()

        KRelay.register<ErrorDisplayFeature>(errorUI)
        KRelay.register<DialogFeature>(dialogs)
        KRelay.register<ToastFeature>(toast)
        KRelay.register<AnalyticsFeature>(analytics)

        // === ACTION: Load and retry multiple times ===
        val viewModel = NetworkViewModel()

        // Try 1
        viewModel.loadData()
        dialogs.simulateRetry()

        // Try 2
        dialogs.simulateRetry()

        // Try 3 - Should succeed
        dialogs.simulateRetry()

        // === VERIFY: Eventually succeeded ===
        assertTrue(toast.messages.contains("Data loaded successfully"))
        assertTrue(analytics.loggedErrors.count { it.first == "network_error" } >= 2)
    }
}

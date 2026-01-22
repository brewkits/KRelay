package dev.brewkits.krelay.demo

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.samples.VoyagerDemoViewModel
import dev.brewkits.krelay.samples.VoyagerNavigationFeature
import kotlin.test.*

/**
 * Demo test showing how easy it is to test ViewModels with KRelay.
 *
 * # The Problem Without KRelay:
 *
 * ```kotlin
 * class LoginViewModelTest {
 *     @Test
 *     fun testLogin() {
 *         // Need to mock Voyager Navigator - Complex!
 *         val mockNavigator = mockk<Navigator>()
 *         val viewModel = LoginViewModel(mockNavigator)
 *
 *         viewModel.onLoginSuccess()
 *
 *         // Verify with mockk - Fragile!
 *         verify { mockNavigator.push(any<HomeScreen>()) }
 *     }
 * }
 * ```
 *
 * Issues:
 * - Requires mocking library (mockk, mockito)
 * - Coupled to Navigator API
 * - Breaks if Voyager API changes
 * - Hard to debug
 *
 * # The Solution With KRelay:
 *
 * ```kotlin
 * class LoginViewModelTest {
 *     @Test
 *     fun testLogin() {
 *         // Simple mock - No library needed!
 *         val mockNav = MockNavigation()
 *         KRelay.register<VoyagerNavigationFeature>(mockNav)
 *
 *         viewModel.onLoginSuccess()
 *
 *         // Simple assertion
 *         assertTrue(mockNav.wentToHome)
 *     }
 * }
 * ```
 *
 * Benefits:
 * - Zero mocking libraries
 * - Simple, readable tests
 * - Resilient to library changes
 * - Easy to debug
 */
class VoyagerIntegrationDemo {

    private lateinit var viewModel: VoyagerDemoViewModel
    private lateinit var mockNav: MockVoyagerNavigation

    @BeforeTest
    fun setup() {
        // Clean slate
        KRelay.reset()

        // Create mock navigation
        mockNav = MockVoyagerNavigation()
        KRelay.register<VoyagerNavigationFeature>(mockNav)

        // Create ViewModel
        viewModel = VoyagerDemoViewModel()
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun `when login success should navigate to home`() {
        // Given
        val username = "john.doe"

        // When
        viewModel.onLoginSuccess(username)

        // Then
        assertTrue(mockNav.navigatedToHome, "Should navigate to home after login")
        assertNull(mockNav.navigatedToProfileUserId, "Should not navigate to profile")
    }

    @Test
    fun `when view profile should navigate to profile with userId`() {
        // Given
        val userId = "user123"

        // When
        viewModel.onViewProfile(userId)

        // Then
        assertEquals(userId, mockNav.navigatedToProfileUserId, "Should navigate to profile with correct userId")
        assertFalse(mockNav.navigatedToHome, "Should not navigate to home")
    }

    @Test
    fun `when open settings should navigate to settings`() {
        // When
        viewModel.onOpenSettings()

        // Then
        assertTrue(mockNav.navigatedToSettings, "Should navigate to settings")
    }

    @Test
    fun `when back pressed should go back`() {
        // When
        viewModel.onBackPressed()

        // Then
        assertTrue(mockNav.wentBack, "Should go back")
    }

    @Test
    fun `when logout should navigate to login`() {
        // When
        viewModel.onLogout()

        // Then
        assertTrue(mockNav.navigatedToLogin, "Should navigate to login on logout")
    }

    @Test
    fun `multiple navigation commands should all be recorded`() {
        // When
        viewModel.onLoginSuccess("user1")
        viewModel.onViewProfile("user2")
        viewModel.onOpenSettings()
        viewModel.onBackPressed()

        // Then
        assertTrue(mockNav.navigatedToHome, "Should have navigated to home")
        assertEquals("user2", mockNav.navigatedToProfileUserId, "Should have navigated to profile")
        assertTrue(mockNav.navigatedToSettings, "Should have navigated to settings")
        assertTrue(mockNav.wentBack, "Should have gone back")
    }
}

/**
 * Simple mock implementation of VoyagerNavigationFeature.
 *
 * No mocking library needed - just a plain Kotlin class!
 *
 * This is one of KRelay's superpowers:
 * - Tests are simple
 * - Tests are fast
 * - Tests are maintainable
 */
class MockVoyagerNavigation : VoyagerNavigationFeature {
    var navigatedToHome = false
    var navigatedToProfileUserId: String? = null
    var navigatedToSettings = false
    var wentBack = false
    var navigatedToLogin = false

    override fun goToHome() {
        navigatedToHome = true
    }

    override fun goToProfile(userId: String) {
        navigatedToProfileUserId = userId
    }

    override fun goToSettings() {
        navigatedToSettings = true
    }

    override fun goBack() {
        wentBack = true
    }

    override fun goToLogin() {
        navigatedToLogin = true
    }

    /**
     * Helper to reset state between test scenarios.
     */
    fun reset() {
        navigatedToHome = false
        navigatedToProfileUserId = null
        navigatedToSettings = false
        wentBack = false
        navigatedToLogin = false
    }
}

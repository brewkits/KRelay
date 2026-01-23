package dev.brewkits.krelay.integration.decompose

import dev.brewkits.krelay.RelayFeature

/**
 * Navigation feature interface for Decompose integration.
 *
 * This demonstrates KRelay's clean architecture pattern:
 * - ViewModels depend ONLY on this interface (zero Decompose dependency)
 * - Platform code implements this interface using Decompose Router
 * - KRelay bridges between them
 *
 * Benefits:
 * - Easy to test (simple mock implementation)
 * - Easy to swap navigation libraries
 * - ViewModels stay platform-agnostic
 */
interface DecomposeNavFeature : RelayFeature {
    /**
     * Navigate to Home screen (login success flow)
     */
    fun navigateToHome()

    /**
     * Navigate to Profile screen with user data
     */
    fun navigateToProfile(userId: String)

    /**
     * Navigate back (pop current screen)
     */
    fun navigateBack()

    /**
     * Navigate to Login screen (logout flow)
     */
    fun navigateToLogin()

    /**
     * Navigate to Signup screen
     */
    fun navigateToSignup()
}

package dev.brewkits.krelay.integration.voyager

import dev.brewkits.krelay.RelayFeature

/**
 * Navigation contract for Voyager integration demo.
 *
 * This interface demonstrates how to create a navigation feature
 * that KRelay will bridge to Voyager Navigator.
 */
interface VoyagerNavFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToProfile(userId: String)
    fun navigateBack()
    fun navigateToLogin()
    fun navigateToSignup()
}

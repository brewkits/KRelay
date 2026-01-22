package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Sample feature: Navigation commands.
 *
 * This demonstrates how KRelay can handle navigation from shared code.
 * Useful for deep links, navigation after business logic completes, etc.
 *
 * Platform implementations:
 * - Android: Use NavController or Intent-based navigation
 * - iOS: Use UINavigationController or Coordinator pattern
 */
interface NavigationFeature : RelayFeature {
    /**
     * Navigates to a specific screen by route.
     *
     * @param route The route identifier (e.g., "profile", "settings/account")
     * @param params Optional parameters as key-value pairs
     */
    fun navigateTo(route: String, params: Map<String, String> = emptyMap())

    /**
     * Navigates back to the previous screen.
     */
    fun navigateBack()

    /**
     * Navigates to the root/home screen, clearing the back stack.
     */
    fun navigateToRoot()
}

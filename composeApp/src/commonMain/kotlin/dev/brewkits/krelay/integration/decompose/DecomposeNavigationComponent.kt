package dev.brewkits.krelay.integration.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

/**
 * Root Decompose Component for navigation demo.
 *
 * This demonstrates Decompose's Component-based architecture:
 * - Component holds navigation state
 * - Router manages navigation stack
 * - Configuration defines screens (sealed class)
 * - Child components are created for each screen
 */
class DecomposeNavigationComponent(
    componentContext: ComponentContext,
    private val onBackToMenu: () -> Unit
) : ComponentContext by componentContext {

    // Navigation controller
    private val navigation = StackNavigation<Config>()

    // Child stack - holds current screen and navigation state
    val childStack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(), // kotlinx.serialization generated
        initialConfiguration = Config.Login,
        handleBackButton = true,
        childFactory = ::createChild
    )

    /**
     * Screen configurations (sealed class pattern)
     */
    @Serializable
    sealed class Config {
        @Serializable
        data object Login : Config()

        @Serializable
        data object Home : Config()

        @Serializable
        data class Profile(val userId: String) : Config()

        @Serializable
        data object Signup : Config()
    }

    /**
     * Child components (one for each screen)
     */
    sealed class Child {
        data class Login(val component: LoginComponent) : Child()
        data class Home(val component: HomeComponent) : Child()
        data class Profile(val component: ProfileComponent) : Child()
        data class Signup(val component: SignupComponent) : Child()
    }

    /**
     * Create child component for each configuration
     */
    private fun createChild(config: Config, componentContext: ComponentContext): Child {
        return when (config) {
            is Config.Login -> Child.Login(
                LoginComponent(
                    componentContext = componentContext,
                    onBackToMenu = onBackToMenu
                )
            )

            is Config.Home -> Child.Home(
                HomeComponent(
                    componentContext = componentContext,
                    onBackToMenu = onBackToMenu
                )
            )

            is Config.Profile -> Child.Profile(
                ProfileComponent(
                    componentContext = componentContext,
                    userId = config.userId,
                    onBackToMenu = onBackToMenu
                )
            )

            is Config.Signup -> Child.Signup(
                SignupComponent(
                    componentContext = componentContext,
                    onBackToMenu = onBackToMenu
                )
            )
        }
    }

    // Navigation methods called by DecomposeNavigationImpl
    @OptIn(DelicateDecomposeApi::class)
    fun navigateToLogin() {
        navigation.replaceCurrent(Config.Login)
    }

    @OptIn(DelicateDecomposeApi::class)
    fun navigateToHome() {
        navigation.replaceCurrent(Config.Home)
    }

    @OptIn(DelicateDecomposeApi::class)
    fun navigateToProfile(userId: String) {
        navigation.push(Config.Profile(userId))
    }

    @OptIn(DelicateDecomposeApi::class)
    fun navigateToSignup() {
        navigation.push(Config.Signup)
    }

    fun navigateBack() {
        navigation.pop()
    }
}

/**
 * Component for Login screen
 */
class LoginComponent(
    componentContext: ComponentContext,
    val onBackToMenu: () -> Unit
) : ComponentContext by componentContext

/**
 * Component for Home screen
 */
class HomeComponent(
    componentContext: ComponentContext,
    val onBackToMenu: () -> Unit
) : ComponentContext by componentContext

/**
 * Component for Profile screen
 */
class ProfileComponent(
    componentContext: ComponentContext,
    val userId: String,
    val onBackToMenu: () -> Unit
) : ComponentContext by componentContext

/**
 * Component for Signup screen
 */
class SignupComponent(
    componentContext: ComponentContext,
    val onBackToMenu: () -> Unit
) : ComponentContext by componentContext

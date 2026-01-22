# KRelay Integration Guide

This guide shows you how to integrate KRelay with popular Kotlin Multiplatform navigation libraries.

## Table of Contents

1. [Voyager Integration](#voyager-integration)
2. [Decompose Integration](#decompose-integration)
3. [Compose Navigation Integration](#compose-navigation-integration)
4. [Testing](#testing)
5. [Best Practices](#best-practices)

---

## Voyager Integration

[Voyager](https://github.com/adrielcafe/voyager) is a pragmatic multiplatform navigation library built on top of Jetpack Compose.

> **📱 Live Demo:** See a complete working example in the demo app at `composeApp/src/commonMain/kotlin/dev/brewkits/krelay/integration/voyager/`
> Run the demo app and select "Voyager Integration" to see this in action!

### Why KRelay + Voyager?

**The Problem:**
```kotlin
// ❌ Without KRelay: ViewModel depends on Voyager Navigator
class LoginViewModel(private val navigator: Navigator) {
    fun onLoginSuccess() {
        navigator.push(HomeScreen())
    }
}
// Issues:
// - ViewModel is coupled to Voyager
// - Hard to test (need to mock Navigator)
// - Can't reuse ViewModel if you switch navigation libraries
```

**The Solution:**
```kotlin
// ✅ With KRelay: ViewModel is navigation-library-agnostic
class LoginViewModel {
    fun onLoginSuccess() {
        KRelay.dispatch<NavigationFeature> {
            it.navigateToHome()
        }
    }
}
// Benefits:
// - ViewModel has zero dependencies
// - Easy to test (simple mock interface)
// - Switch navigation libraries? Just rewrite the implementation!
```

### Step-by-Step Integration

#### Step 1: Add Dependencies

```kotlin
// commonMain dependencies
commonMain.dependencies {
    implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
    implementation("cafe.adriel.voyager:voyager-transitions:1.0.0")
    implementation(project(":krelay"))
}
```

#### Step 2: Define Navigation Feature (commonMain)

```kotlin
// File: commonMain/kotlin/com/app/navigation/NavigationFeature.kt
package com.app.navigation

import dev.brewkits.krelay.RelayFeature

/**
 * Navigation contract for the entire app.
 * Platform implementations will use Voyager, but ViewModels don't need to know that.
 */
interface NavigationFeature : RelayFeature {
    /**
     * Navigate to home screen, clearing back stack
     */
    fun navigateToHome()

    /**
     * Navigate to profile screen
     */
    fun navigateToProfile(userId: String)

    /**
     * Navigate to settings
     */
    fun navigateToSettings()

    /**
     * Navigate back (pop current screen)
     */
    fun navigateBack()
}
```

#### Step 3: Create Voyager Screens (commonMain)

```kotlin
// File: commonMain/kotlin/com/app/screens/LoginScreen.kt
package com.app.screens

import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.runtime.Composable

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = remember { LoginViewModel() }
        LoginContent(viewModel)
    }
}

@Composable
fun LoginContent(viewModel: LoginViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { viewModel.onLoginSuccess() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}
```

```kotlin
// File: commonMain/kotlin/com/app/screens/HomeScreen.kt
package com.app.screens

import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.runtime.Composable

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = remember { HomeViewModel() }
        HomeContent(viewModel)
    }
}

@Composable
fun HomeContent(viewModel: HomeViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Home!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onViewProfile("user123") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Profile")
        }
    }
}
```

```kotlin
// File: commonMain/kotlin/com/app/screens/ProfileScreen.kt
package com.app.screens

import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.runtime.Composable

data class ProfileScreen(val userId: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel = remember { ProfileViewModel(userId) }
        ProfileContent(viewModel)
    }
}

@Composable
fun ProfileContent(viewModel: ProfileViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("User Profile: ${viewModel.userId}", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onBackClicked() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Back")
        }
    }
}
```

#### Step 4: Create ViewModels (commonMain)

```kotlin
// File: commonMain/kotlin/com/app/viewmodels/LoginViewModel.kt
package com.app.viewmodels

import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

class LoginViewModel {
    fun onLoginSuccess() {
        // Pure business logic - no Navigator dependency!
        println("Login successful, navigating to home...")

        KRelay.dispatch<NavigationFeature> {
            it.navigateToHome()
        }
    }
}
```

```kotlin
// File: commonMain/kotlin/com/app/viewmodels/HomeViewModel.kt
package com.app.viewmodels

import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

class HomeViewModel {
    fun onViewProfile(userId: String) {
        println("Viewing profile for user: $userId")

        KRelay.dispatch<NavigationFeature> {
            it.navigateToProfile(userId)
        }
    }
}
```

```kotlin
// File: commonMain/kotlin/com/app/viewmodels/ProfileViewModel.kt
package com.app.viewmodels

import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

class ProfileViewModel(val userId: String) {
    fun onBackClicked() {
        KRelay.dispatch<NavigationFeature> {
            it.navigateBack()
        }
    }
}
```

#### Step 5: Create Voyager Implementation (Platform Code)

```kotlin
// File: androidMain/kotlin/com/app/navigation/VoyagerNavigationFeature.kt
// (or iosMain/kotlin/com/app/navigation/VoyagerNavigationFeature.kt)
package com.app.navigation

import cafe.adriel.voyager.navigator.Navigator
import com.app.screens.HomeScreen
import com.app.screens.ProfileScreen
import com.app.screens.SettingsScreen

/**
 * Platform implementation of NavigationFeature using Voyager.
 *
 * This class translates KRelay navigation commands into Voyager API calls.
 * It's the only place in the codebase that knows about Voyager.
 */
class VoyagerNavigationFeature(
    private val navigator: Navigator
) : NavigationFeature {

    override fun navigateToHome() {
        // Navigate to home and clear back stack
        navigator.replaceAll(HomeScreen())
    }

    override fun navigateToProfile(userId: String) {
        // Push profile screen onto stack
        navigator.push(ProfileScreen(userId))
    }

    override fun navigateToSettings() {
        navigator.push(SettingsScreen())
    }

    override fun navigateBack() {
        // Pop current screen
        navigator.pop()
    }
}
```

#### Step 6: Register at App Root (commonMain)

```kotlin
// File: commonMain/kotlin/com/app/App.kt
package com.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.app.navigation.NavigationFeature
import com.app.navigation.VoyagerNavigationFeature
import com.app.screens.LoginScreen
import dev.brewkits.krelay.KRelay

@Composable
fun App() {
    // Create Voyager Navigator starting at LoginScreen
    Navigator(LoginScreen()) { navigator ->
        // Register KRelay navigation implementation
        LaunchedEffect(navigator) {
            val navImpl = VoyagerNavigationFeature(navigator)
            KRelay.register<NavigationFeature>(navImpl)
        }

        // Voyager handles screen transitions
        SlideTransition(navigator)
    }
}
```

#### Step 7: Platform Entry Points

**Android:**
```kotlin
// File: androidMain/kotlin/com/app/MainActivity.kt
package com.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
```

**iOS:**
```swift
// File: iosApp/ContentView.swift
import SwiftUI
import ComposeApp

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

```kotlin
// File: iosMain/kotlin/com/app/MainViewController.kt
package com.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        App()
    }
}
```

### Complete Flow Diagram

```
User Action (Login Button Click)
    ↓
LoginViewModel.onLoginSuccess()
    ↓
KRelay.dispatch<NavigationFeature> { it.navigateToHome() }
    ↓
KRelay checks registry
    ↓
Found: VoyagerNavigationFeature instance
    ↓
runOnMain { navigateToHome() }
    ↓
VoyagerNavigationFeature.navigateToHome()
    ↓
navigator.replaceAll(HomeScreen())
    ↓
Voyager transitions to HomeScreen
    ↓
UI shows HomeScreen 🎉
```

---

## Decompose Integration

[Decompose](https://github.com/arkivanov/Decompose) provides lifecycle-aware components for KMP.

### Step-by-Step Integration

#### Step 1: Add Dependencies

```kotlin
commonMain.dependencies {
    implementation("com.arkivanov.decompose:decompose:2.2.0")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:2.2.0")
    implementation(project(":krelay"))
}
```

#### Step 2: Define Navigation Feature

```kotlin
// File: commonMain/kotlin/com/app/navigation/NavigationFeature.kt
interface NavigationFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToDetails(itemId: String)
    fun navigateBack()
}
```

#### Step 3: Create Root Component

```kotlin
// File: commonMain/kotlin/com/app/components/RootComponent.kt
package com.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            initialConfiguration = Config.Home,
            handleBackButton = true,
            childFactory = ::child
        )

    // Navigation implementation
    private val navFeature = object : NavigationFeature {
        override fun navigateToHome() {
            navigation.replaceAll(Config.Home)
        }

        override fun navigateToDetails(itemId: String) {
            navigation.push(Config.Details(itemId))
        }

        override fun navigateBack() {
            navigation.pop()
        }
    }

    init {
        // Register navigation feature
        KRelay.register<NavigationFeature>(navFeature)
    }

    private fun child(config: Config, componentContext: ComponentContext): Child =
        when (config) {
            is Config.Home -> Child.Home(HomeComponent(componentContext))
            is Config.Details -> Child.Details(DetailsComponent(componentContext, config.itemId))
        }

    sealed class Config {
        object Home : Config()
        data class Details(val itemId: String) : Config()
    }

    sealed class Child {
        data class Home(val component: HomeComponent) : Child()
        data class Details(val component: DetailsComponent) : Child()
    }
}
```

#### Step 4: Create Child Components

```kotlin
// File: commonMain/kotlin/com/app/components/HomeComponent.kt
package com.app.components

import com.arkivanov.decompose.ComponentContext
import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

class HomeComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    fun onItemClicked(itemId: String) {
        KRelay.dispatch<NavigationFeature> {
            it.navigateToDetails(itemId)
        }
    }
}
```

```kotlin
// File: commonMain/kotlin/com/app/components/DetailsComponent.kt
package com.app.components

import com.arkivanov.decompose.ComponentContext
import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

class DetailsComponent(
    componentContext: ComponentContext,
    val itemId: String
) : ComponentContext by componentContext {

    fun onBackClicked() {
        KRelay.dispatch<NavigationFeature> {
            it.navigateBack()
        }
    }
}
```

#### Step 5: Create UI with Decompose Extensions

```kotlin
// File: commonMain/kotlin/com/app/App.kt
package com.app

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.app.components.RootComponent
import com.app.ui.DetailsScreen
import com.app.ui.HomeScreen

@Composable
fun App(rootComponent: RootComponent) {
    Children(
        stack = rootComponent.stack,
        animation = stackAnimation(slide())
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Home -> HomeScreen(instance.component)
            is RootComponent.Child.Details -> DetailsScreen(instance.component)
        }
    }
}

@Composable
fun HomeScreen(component: HomeComponent) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Home Screen", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { component.onItemClicked("item123") }) {
            Text("View Item Details")
        }
    }
}

@Composable
fun DetailsScreen(component: DetailsComponent) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Details: ${component.itemId}", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { component.onBackClicked() }) {
            Text("← Back")
        }
    }
}
```

---

## Compose Navigation Integration

For Jetpack Compose Navigation (Android) or Compose Multiplatform Navigation.

### Step-by-Step Integration

#### Step 1: Define Navigation Feature

```kotlin
// File: commonMain/kotlin/com/app/navigation/NavigationFeature.kt
interface NavigationFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToProfile(userId: String)
    fun navigateBack()
}
```

#### Step 2: Create NavController Implementation

```kotlin
// File: androidMain/kotlin/com/app/navigation/ComposeNavigationFeature.kt
package com.app.navigation

import androidx.navigation.NavHostController

class ComposeNavigationFeature(
    private val navController: NavHostController
) : NavigationFeature {

    override fun navigateToHome() {
        navController.navigate("home") {
            popUpTo("login") { inclusive = true }
        }
    }

    override fun navigateToProfile(userId: String) {
        navController.navigate("profile/$userId")
    }

    override fun navigateBack() {
        navController.navigateUp()
    }
}
```

#### Step 3: Setup NavHost

```kotlin
// File: androidMain/kotlin/com/app/App.kt
package com.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.navigation.ComposeNavigationFeature
import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay

@Composable
fun App() {
    val navController = rememberNavController()

    // Register KRelay navigation
    LaunchedEffect(navController) {
        val navImpl = ComposeNavigationFeature(navController)
        KRelay.register<NavigationFeature>(navImpl)
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen()
        }

        composable("home") {
            HomeScreen()
        }

        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(userId)
        }
    }
}
```

---

## Testing

### Testing ViewModels with KRelay

```kotlin
// File: commonTest/kotlin/com/app/viewmodels/LoginViewModelTest.kt
package com.app.viewmodels

import com.app.navigation.NavigationFeature
import dev.brewkits.krelay.KRelay
import kotlin.test.*

class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel
    private lateinit var mockNav: MockNavigationFeature

    @BeforeTest
    fun setup() {
        KRelay.reset()
        mockNav = MockNavigationFeature()
        KRelay.register<NavigationFeature>(mockNav)
        viewModel = LoginViewModel()
    }

    @AfterTest
    fun tearDown() {
        KRelay.reset()
    }

    @Test
    fun `when login success should navigate to home`() {
        // When
        viewModel.onLoginSuccess()

        // Then
        assertTrue(mockNav.navigatedToHome)
    }
}

class MockNavigationFeature : NavigationFeature {
    var navigatedToHome = false
    var navigatedToProfileUserId: String? = null

    override fun navigateToHome() {
        navigatedToHome = true
    }

    override fun navigateToProfile(userId: String) {
        navigatedToProfileUserId = userId
    }

    override fun navigateBack() {}
    override fun navigateToSettings() {}
}
```

---

## Best Practices

### 1. Single Navigation Feature per Module

For large apps, create separate navigation features per module:

```kotlin
// Auth module
interface AuthNavigationFeature : RelayFeature {
    fun navigateToLogin()
    fun navigateToSignup()
}

// Main module
interface MainNavigationFeature : RelayFeature {
    fun navigateToHome()
    fun navigateToProfile()
}

// Register both
KRelay.register<AuthNavigationFeature>(AuthNavImpl(navigator))
KRelay.register<MainNavigationFeature>(MainNavImpl(navigator))
```

### 2. Cleanup in onCleared()

If your ViewModel queues navigation commands, clear them in `onCleared()`:

```kotlin
class MyViewModel : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        KRelay.clearQueue<NavigationFeature>()
    }
}
```

### 3. Deep Link Handling

Integrate deep links with KRelay:

```kotlin
fun handleDeepLink(uri: Uri) {
    when (uri.pathSegments.firstOrNull()) {
        "profile" -> {
            val userId = uri.pathSegments.getOrNull(1)
            KRelay.dispatch<NavigationFeature> {
                it.navigateToProfile(userId ?: "")
            }
        }
        "home" -> {
            KRelay.dispatch<NavigationFeature> {
                it.navigateToHome()
            }
        }
    }
}
```

### 4. Type-Safe Navigation with Sealed Classes

```kotlin
sealed class NavigationCommand {
    object Home : NavigationCommand()
    data class Profile(val userId: String) : NavigationCommand()
    data class Details(val itemId: String) : NavigationCommand()
}

interface NavigationFeature : RelayFeature {
    fun navigate(command: NavigationCommand)
}

// Usage
viewModel.onNavigate(NavigationCommand.Profile("user123"))
```

---

## Summary

| Library | Complexity | KRelay Integration | Best For |
|---------|-----------|-------------------|----------|
| **Voyager** | Low | Simple | Quick prototypes, simple apps |
| **Decompose** | Medium | Component-based | Complex apps, lifecycle management |
| **Compose Navigation** | Medium | Platform-specific | Android-first apps |

**Key Takeaways:**
- KRelay decouples business logic from navigation libraries
- ViewModels become testable without mocking navigation
- Switching navigation libraries only requires rewriting the implementation
- Type-safe navigation with zero boilerplate

---

For more examples, see the [demo app](../composeApp) in this repository.

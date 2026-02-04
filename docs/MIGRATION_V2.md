# KRelay v2.0 Migration Guide

KRelay v2.0 introduces a powerful, instance-based API that is perfect for dependency injection and large-scale, multi-module applications. While v2.0 is **100% backward compatible** with the v1.x singleton API, we strongly recommend migrating to the new instance-based approach to improve testability, maintainability, and scalability.

This guide outlines the steps to migrate your project from the v1.x singleton pattern to the v2.0 instance pattern.

## Core Concept: Singleton vs. Instance

- **v1.x (Singleton)**: All calls went through the global `KRelay` object. This was simple but could lead to conflicts in large apps and was less friendly to DI and testing.
- **v2.0 (Instance)**: You create one or more `KRelayInstance` objects, each with its own isolated scope. This is the recommended approach for all new projects.

The old singleton API still works, but it now delegates to a default, internal instance.

## Migration Steps

Migration is straightforward and can be done incrementally.

### Step 1: Update Dependency
Update your `build.gradle.kts` to the latest version:
```kotlin
commonMain.dependencies {
    // implementation("dev.brewkits:krelay:1.1.0") // Before
    implementation("dev.brewkits:krelay:2.0.0") // After
}
```

### Step 2: Provide a KRelay Instance
Instead of relying on the global singleton, you should now provide a `KRelayInstance` through your dependency injection framework (like Koin or Hilt).

**Example: Using Koin**
In your DI module, create and provide a `KRelayInstance`. For most apps, a single instance is sufficient.

```kotlin
// In your app's Koin module
val appModule = module {
    // Provide a single, app-scoped KRelay instance
    single<KRelayInstance> { KRelay.create("AppScope") }

    // Your ViewModels will now get the instance injected
    viewModel { LoginViewModel(krelay = get()) }
    viewModel { ProfileViewModel(krelay = get()) }
}
```
If you are building a "Super App," you can create multiple instances for different modules (e.g., `KRelay.create("Rides")`, `KRelay.create("Food")`).

### Step 3: Update ViewModels and UseCases
Update your ViewModels and other classes to receive the `KRelayInstance` via their constructor instead of calling the global `KRelay` object.

**Before (v1.x)**
```kotlin
class LoginViewModel : ViewModel() {
    fun onLoginSuccess() {
        KRelay.dispatch<NavigationFeature> { it.goToHome() }
        KRelay.dispatch<ToastFeature> { it.show("Welcome!") }
    }

    override fun onCleared() {
        super.onCleared()
        KRelay.clearQueue<ToastFeature>()
    }
}
```

**After (v2.0)**
```kotlin
// ViewModel now receives the instance via its constructor
class LoginViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onLoginSuccess() {
        // Call dispatch on the injected instance
        krelay.dispatch<NavigationFeature> { it.goToHome() }
        krelay.dispatch<ToastFeature> { it.show("Welcome!") }
    }

    override fun onCleared() {
        super.onCleared()
        // Call clearQueue on the instance
        krelay.clearQueue<ToastFeature>()
    }
}
```
This change makes your ViewModel's dependency explicit and far easier to test.

### Step 4: Update UI Layer (Registration)
In your Activities or ViewControllers, you now need to get the `KRelayInstance` (usually from your DI framework) and register your feature implementations with it.

**Before (v1.x)**
```kotlin
// Android Activity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRelay.register<NavigationFeature>(AndroidNavigation(this))
}
```

**After (v2.0)**
```kotlin
// Android Activity with Koin
class MyActivity : AppCompatActivity() {
    // Inject the same instance that the ViewModels use
    private val krelay: KRelayInstance by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register the feature on the specific instance
        krelay.register<NavigationFeature>(AndroidNavigation(this))
    }
}
```

The process is identical for iOS.

### Step 5: Update Tests
Testing is where the v2.0 instance API truly shines. You no longer need to rely on the global `KRelay.reset()` to manage state between tests. Instead, you can pass a mock or a fresh instance directly to your ViewModel.

**Before (v1.x)**
```kotlin
class LoginViewModelTest {
    @BeforeTest
    fun setup() {
        KRelay.reset() // Relies on global state
    }

    @Test
    fun testLogin() {
        // Arrange
        val mockNav = MockNavigationFeature()
        KRelay.register<NavigationFeature>(mockNav)
        val viewModel = LoginViewModel()

        // Act
        viewModel.onLoginSuccess()

        // Assert
        assertTrue(mockNav.navigatedToHome)
    }
}
```

**After (v2.0)**
```kotlin
class LoginViewModelTest {
    @Test
    fun testLogin() {
        // Arrange
        val mockRelay = KRelay.create("TestScope") // Create a fresh, isolated instance
        val mockNav = MockNavigationFeature()
        mockRelay.register<NavigationFeature>(mockNav)
        
        // Pass the mock instance directly to the ViewModel
        val viewModel = LoginViewModel(krelay = mockRelay)

        // Act
        viewModel.onLoginSuccess()

        // Assert
        assertTrue(mockNav.navigatedToHome)
    }
}
```
This approach eliminates test pollution and makes dependencies explicit.

## Conclusion

Migrating to the KRelay v2.0 instance-based API is a straightforward process that yields significant benefits in code quality, testability, and scalability. While the singleton API remains for backward compatibility, we encourage all developers to adopt the new instance-based pattern for future development.

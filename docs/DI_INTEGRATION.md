# KRelay Dependency Injection Integration

KRelay v2.0's instance-based API is designed to work seamlessly with dependency injection (DI) frameworks like Koin and Hilt. This guide provides examples of how to integrate `KRelayInstance` into your DI setup.

The core idea is to treat `KRelayInstance` as a service that you provide in your DI modules and inject into your ViewModels, UseCases, or Repositories.

## Koin Integration

Koin is a popular and lightweight DI framework for Kotlin.

### 1. Provide KRelayInstance in a Module

In your Koin module, create and provide one or more `KRelayInstance`s. For most applications, a single, app-scoped instance is sufficient.

```kotlin
// in shared/src/commonMain/kotlin/di/AppModule.kt

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.androidx.viewmodel.dsl.viewModelOf

val appModule = module {
    // Provide a single, app-scoped KRelay instance
    single<KRelayInstance> {
        KRelay.builder("AppScope")
            .debugMode(true) // Enable logs for debug builds
            .build()
    }

    // Provide your ViewModels, injecting the KRelayInstance
    viewModel { LoginViewModel(krelay = get()) }
}
```

For "Super Apps", you can define multiple instances for different scopes:
```kotlin
val ridesModule = module {
    single<KRelayInstance>(named("Rides")) { KRelay.create("Rides") }
    viewModel { RideViewModel(krelay = get(named("Rides"))) }
}

val foodModule = module {
    single<KRelayInstance>(named("Food")) { KRelay.create("Food") }
    viewModel { FoodViewModel(krelay = get(named("Food"))) }
}
```

### 2. Inject KRelayInstance into ViewModels

Your ViewModels can now receive the `KRelayInstance` via constructor injection. This makes them easy to test and decouples them from the global singleton.

```kotlin
// in shared/src/commonMain/kotlin/LoginViewModel.kt

class LoginViewModel(private val krelay: KRelayInstance) : ViewModel() {
    fun onLoginSuccess() {
        // Dispatch commands on the injected instance
        krelay.dispatch<NavigationFeature> { it.goToHome() }
        krelay.dispatch<ToastFeature> { it.show("Welcome back!") }
    }
}
```

### 3. Register Implementations in the UI Layer

In your Android Activities or iOS ViewControllers, inject the same `KRelayInstance` and use it to register your platform-specific feature implementations.

**Android (Activity)**
```kotlin
// in android/src/main/java/com/myapp/MyActivity.kt

class MyActivity : AppCompatActivity() {
    // Inject the same instance the ViewModels are using
    private val krelay: KRelayInstance by inject()
    private val viewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register the platform implementation on the specific instance
        krelay.register<NavigationFeature>(AndroidNavigation(this))
        krelay.register<ToastFeature>(AndroidToast(this))
    }
}
```

## Hilt Integration

Hilt is the recommended DI framework for modern Android development. Integrating KRelay follows a similar pattern.

### 1. Provide KRelayInstance with @Provides

In a Hilt module, use the `@Provides` annotation to supply a `KRelayInstance`.

```kotlin
// in android/src/main/java/di/KRelayModule.kt

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KRelayModule {

    @Provides
    @Singleton
    fun provideKRelayInstance(): KRelayInstance {
        return KRelay.builder("AppScope")
            .debugMode(BuildConfig.DEBUG)
            .build()
    }
}
```

### 2. Inject KRelayInstance with @Inject

In your Android ViewModels, use `@HiltViewModel` and `@Inject` to receive the `KRelayInstance`.

```kotlin
// in android/src/main/java/com/myapp/LoginViewModel.kt

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brewkits.krelay.KRelayInstance
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val krelay: KRelayInstance,
    private val authRepository: AuthRepository // Other dependencies
) : ViewModel() {

    fun onLoginSuccess() {
        krelay.dispatch<NavigationFeature> { it.goToHome() }
    }
}
```

### 3. Register Implementations in Activity

In your Activity, inject the `KRelayInstance` and register the implementations as usual.

```kotlin
// in android/src/main/java/com/myapp/MainActivity.kt

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var krelay: KRelayInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        krelay.register<NavigationFeature>(AndroidNavigation(this))
        krelay.register<ToastFeature>(AndroidToast(this))
    }
}
```

By following these patterns, you can leverage the full power of KRelay's v2.0 instance API while maintaining a clean, scalable, and testable architecture.

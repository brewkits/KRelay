# Super App Architecture with KRelay v2.0

This guide demonstrates how to build a "Super App" (like Grab, Gojek, WeChat) using KRelay v2.0 instance API.

## What is a Super App?

A Super App is a mobile application that combines multiple independent "mini-apps" or modules into a single platform:
- **Ride**: Book taxis, bikes, cars
- **Food**: Order food delivery
- **Pay**: Digital wallet, payments
- **Mart**: Grocery shopping

**Key Challenge**: Each module needs **complete isolation** - they shouldn't interfere with each other.

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           Super App Container           │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │  Ride    │  │  Food    │  │  Pay   ││
│  │  Module  │  │  Module  │  │  Module││
│  │          │  │          │  │        ││
│  │ KRelay   │  │ KRelay   │  │ KRelay ││
│  │ Instance │  │ Instance │  │ Instance│
│  └──────────┘  └──────────┘  └────────┘│
│                                         │
└─────────────────────────────────────────┘

Each module has:
- ✅ Isolated KRelayInstance
- ✅ Independent features (Toast, Navigation, etc.)
- ✅ Separate ViewModels, UseCases
- ✅ Own DI scope
```

---

## Project Structure

```
superapp/
├── app/                          # Main container app
│   └── MainActivity.kt           # Tab navigation
│
├── ride-module/                  # Ride mini-app
│   ├── di/
│   │   └── RideModule.kt         # Koin module
│   ├── ui/
│   │   ├── RideActivity.kt       # Platform (Android)
│   │   └── RideViewModel.kt      # Shared ViewModel
│   └── features/
│       ├── ToastFeature.kt       # RelayFeature interface
│       └── NavigationFeature.kt
│
├── food-module/                  # Food mini-app
│   ├── di/
│   │   └── FoodModule.kt
│   ├── ui/
│   │   ├── FoodActivity.kt
│   │   └── FoodViewModel.kt
│   └── features/
│       ├── ToastFeature.kt       # Same interface name, different instance!
│       └── NavigationFeature.kt
│
└── pay-module/                   # Payment mini-app
    └── ...
```

---

## Step-by-Step Implementation

### 1. Define Shared Features (Per Module)

Each module defines its own features. Notice they have **same names** but in **different packages**:

**ride-module/features/ToastFeature.kt**
```kotlin
package com.superapp.ride.features

import dev.brewkits.krelay.RelayFeature

interface ToastFeature : RelayFeature {
    fun show(message: String)
    fun showBookingConfirmation(rideId: String)
}

interface NavigationFeature : RelayFeature {
    fun goToRideDetails(rideId: String)
    fun goBack()
}
```

**food-module/features/ToastFeature.kt**
```kotlin
package com.superapp.food.features

import dev.brewkits.krelay.RelayFeature

interface ToastFeature : RelayFeature {
    fun show(message: String)
    fun showOrderConfirmation(orderId: String)
}

interface NavigationFeature : RelayFeature {
    fun goToOrderDetails(orderId: String)
    fun goBack()
}
```

**Key Point**: Same interface names (ToastFeature), different packages = **No conflicts!**

---

### 2. Setup DI with Isolated KRelay Instances

**ride-module/di/RideModule.kt**
```kotlin
package com.superapp.ride.di

import com.superapp.ride.ui.RideViewModel
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val rideModule = module {
    // Create isolated KRelay instance for Ride module
    single<KRelayInstance>(qualifier = named("Ride")) {
        KRelay.builder("RideModule")
            .maxQueueSize(100)
            .debugMode(true)
            .build()
    }

    // Inject Ride's KRelay into Ride ViewModels
    viewModel {
        RideViewModel(
            kRelay = get(named("Ride")),
            rideRepository = get()
        )
    }
}
```

**food-module/di/FoodModule.kt**
```kotlin
package com.superapp.food.di

import com.superapp.food.ui.FoodViewModel
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val foodModule = module {
    // Create isolated KRelay instance for Food module
    single<KRelayInstance>(qualifier = named("Food")) {
        KRelay.builder("FoodModule")
            .maxQueueSize(50)  // Different config!
            .debugMode(true)
            .build()
    }

    // Inject Food's KRelay into Food ViewModels
    viewModel {
        FoodViewModel(
            kRelay = get(named("Food")),
            foodRepository = get()
        )
    }
}
```

---

### 3. Implement ViewModels (Shared Code)

**ride-module/ui/RideViewModel.kt**
```kotlin
package com.superapp.ride.ui

import androidx.lifecycle.ViewModel
import com.superapp.ride.features.NavigationFeature
import com.superapp.ride.features.ToastFeature
import dev.brewkits.krelay.KRelayInstance

class RideViewModel(
    private val kRelay: KRelayInstance,
    private val rideRepository: RideRepository
) : ViewModel() {

    fun bookRide(pickupLocation: String, dropLocation: String) {
        viewModelScope.launch {
            try {
                val ride = rideRepository.bookRide(pickupLocation, dropLocation)

                // Dispatch to Ride module's KRelay instance
                kRelay.dispatch<ToastFeature> {
                    it.showBookingConfirmation(ride.id)
                }

                kRelay.dispatch<NavigationFeature> {
                    it.goToRideDetails(ride.id)
                }
            } catch (e: Exception) {
                kRelay.dispatch<ToastFeature> {
                    it.show("Booking failed: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up queued actions
        kRelay.clearQueue<ToastFeature>()
        kRelay.clearQueue<NavigationFeature>()
    }
}
```

**food-module/ui/FoodViewModel.kt**
```kotlin
package com.superapp.food.ui

import androidx.lifecycle.ViewModel
import com.superapp.food.features.NavigationFeature
import com.superapp.food.features.ToastFeature
import dev.brewkits.krelay.KRelayInstance

class FoodViewModel(
    private val kRelay: KRelayInstance,
    private val foodRepository: FoodRepository
) : ViewModel() {

    fun placeOrder(restaurantId: String, items: List<FoodItem>) {
        viewModelScope.launch {
            try {
                val order = foodRepository.placeOrder(restaurantId, items)

                // Dispatch to Food module's KRelay instance
                kRelay.dispatch<ToastFeature> {
                    it.showOrderConfirmation(order.id)
                }

                kRelay.dispatch<NavigationFeature> {
                    it.goToOrderDetails(order.id)
                }
            } catch (e: Exception) {
                kRelay.dispatch<ToastFeature> {
                    it.show("Order failed: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        kRelay.clearQueue<ToastFeature>()
        kRelay.clearQueue<NavigationFeature>()
    }
}
```

---

### 4. Implement Platform Code (Android)

**ride-module/ui/RideActivity.kt**
```kotlin
package com.superapp.ride.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.superapp.ride.features.NavigationFeature
import com.superapp.ride.features.ToastFeature
import dev.brewkits.krelay.KRelayInstance
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named

class RideActivity : AppCompatActivity(),
    ToastFeature,
    NavigationFeature {

    // Inject Ride module's KRelay instance
    private val kRelay: KRelayInstance by inject(named("Ride"))

    // Inject Ride ViewModel (which uses same KRelay instance)
    private val viewModel: RideViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride)

        // Register this Activity as the platform implementation
        kRelay.register<ToastFeature>(this)
        kRelay.register<NavigationFeature>(this)

        // Setup UI
        setupBookingButton()
    }

    // === ToastFeature Implementation ===
    override fun show(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showBookingConfirmation(rideId: String) {
        Toast.makeText(
            this,
            "Ride $rideId booked successfully!",
            Toast.LENGTH_LONG
        ).show()
    }

    // === NavigationFeature Implementation ===
    override fun goToRideDetails(rideId: String) {
        startActivity(RideDetailsActivity.newIntent(this, rideId))
    }

    override fun goBack() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroy() {
        // Optional: Explicitly unregister (WeakRef will auto-cleanup anyway)
        kRelay.unregister<ToastFeature>()
        kRelay.unregister<NavigationFeature>()
        super.onDestroy()
    }
}
```

**food-module/ui/FoodActivity.kt**
```kotlin
package com.superapp.food.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.superapp.food.features.NavigationFeature
import com.superapp.food.features.ToastFeature
import dev.brewkits.krelay.KRelayInstance
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named

class FoodActivity : AppCompatActivity(),
    ToastFeature,
    NavigationFeature {

    // Inject Food module's KRelay instance (different from Ride!)
    private val kRelay: KRelayInstance by inject(named("Food"))

    private val viewModel: FoodViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food)

        // Register with Food module's KRelay
        kRelay.register<ToastFeature>(this)
        kRelay.register<NavigationFeature>(this)

        setupOrderButton()
    }

    // === ToastFeature Implementation ===
    override fun show(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showOrderConfirmation(orderId: String) {
        Toast.makeText(
            this,
            "Order $orderId placed successfully!",
            Toast.LENGTH_LONG
        ).show()
    }

    // === NavigationFeature Implementation ===
    override fun goToOrderDetails(orderId: String) {
        startActivity(OrderDetailsActivity.newIntent(this, orderId))
    }

    override fun goBack() {
        onBackPressedDispatcher.onBackPressed()
    }
}
```

---

### 5. Application Setup

**app/SuperApp.kt**
```kotlin
package com.superapp

import android.app.Application
import com.superapp.food.di.foodModule
import com.superapp.pay.di.payModule
import com.superapp.ride.di.rideModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SuperApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin with all module-specific DI modules
        startKoin {
            androidContext(this@SuperApp)
            modules(
                rideModule,   // Has its own KRelay instance
                foodModule,   // Has its own KRelay instance
                payModule     // Has its own KRelay instance
            )
        }
    }
}
```

---

## Key Benefits of This Architecture

### 1. Complete Module Isolation

```kotlin
// Ride Module dispatches
rideKRelay.dispatch<ToastFeature> { it.show("Ride booked!") }

// Food Module dispatches
foodKRelay.dispatch<ToastFeature> { it.show("Order placed!") }

// ✅ NO CONFLICT! Each uses its own ToastFeature implementation
```

### 2. Team Independence

```
Team Ride   → Works on ride-module   → Uses Ride KRelay instance
Team Food   → Works on food-module   → Uses Food KRelay instance
Team Pay    → Works on pay-module    → Uses Pay KRelay instance

✅ No stepping on each other's toes!
✅ No shared global state bugs
✅ Can release modules independently
```

### 3. Easy Testing

```kotlin
@Test
fun `test ride booking in isolation`() {
    // Create test-specific KRelay instance
    val testRelay = KRelay.create("RideTest_${UUID.randomUUID()}")

    // Create mocks
    val mockToast = MockToastFeature()
    val mockNav = MockNavigationFeature()

    // Register mocks with test instance
    testRelay.register<ToastFeature>(mockToast)
    testRelay.register<NavigationFeature>(mockNav)

    // Create ViewModel with test instance
    val viewModel = RideViewModel(
        kRelay = testRelay,
        rideRepository = mockRideRepository
    )

    // Test
    viewModel.bookRide("A", "B")

    // Verify
    assertEquals("Booking confirmed", mockToast.lastMessage)
    assertEquals("ride123", mockNav.lastRideId)

    // ✅ No global state, no cleanup needed!
}
```

### 4. Per-Module Configuration

```kotlin
val rideModule = module {
    single(named("Ride")) {
        KRelay.builder("RideModule")
            .maxQueueSize(200)  // High traffic module
            .actionExpiry(10 * 60 * 1000)  // 10 min
            .debugMode(true)
            .build()
    }
}

val payModule = module {
    single(named("Pay")) {
        KRelay.builder("PayModule")
            .maxQueueSize(50)   // Lower traffic
            .actionExpiry(60 * 1000)  // 1 min (faster expiry for security)
            .debugMode(false)  // Production-ready
            .build()
    }
}
```

---

## Comparison: v1.0 vs v2.0 in Super Apps

### v1.0 Singleton (Problematic)

```kotlin
// ❌ Problem: All modules share global KRelay
object RideModule {
    fun init() {
        KRelay.register<ToastFeature>(RideToastImpl())
    }
}

object FoodModule {
    fun init() {
        // Overwrites Ride's ToastFeature! ❌
        KRelay.register<ToastFeature>(FoodToastImpl())
    }
}

// Workaround: Feature namespacing (ugly)
interface RideModuleToastFeature : RelayFeature { ... }
interface FoodModuleToastFeature : RelayFeature { ... }
```

### v2.0 Instances (Clean)

```kotlin
// ✅ Solution: Each module has isolated instance
object RideModule {
    val kRelay = KRelay.create("Ride")

    fun init() {
        kRelay.register<ToastFeature>(RideToastImpl())
    }
}

object FoodModule {
    val kRelay = KRelay.create("Food")

    fun init() {
        kRelay.register<ToastFeature>(FoodToastImpl())
    }
}

// ✅ No conflicts, clean interfaces, no namespacing needed!
```

---

## Summary

**KRelay v2.0 Instance API enables true Super App architecture**:

✅ **Module Isolation**: Each mini-app has its own KRelay instance
✅ **Team Independence**: No cross-team conflicts
✅ **Clean Interfaces**: No feature namespacing needed
✅ **DI-Friendly**: Easy to inject per-module instances
✅ **Testable**: Each test gets fresh instance
✅ **Configurable**: Per-module settings

**Recommended for**:
- Multi-module apps (2+ independent modules)
- Super Apps (Grab/Gojek style)
- White-label apps (multi-tenant)
- Large teams (multiple teams, one app)

**See Also**:
- [Migration Guide](./MIGRATION_V2.md)
- [DI Integration](./DI_INTEGRATION.md)
- [Instance Isolation Tests](../krelay/src/commonTest/kotlin/dev/brewkits/krelay/instance/KRelayInstanceIsolationTest.kt)

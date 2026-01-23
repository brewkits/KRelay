# KRelay Anti-Patterns: When NOT to Use KRelay

> **TL;DR**: KRelay is for **UI feedback** (toast, navigation, haptics). Never use it for **critical operations** (payments, uploads, persistence).

---

## 🚨 The Golden Rule

**If losing this action would cause:**
- ❌ **Data loss** → DON'T use KRelay
- ❌ **Money loss** → DON'T use KRelay
- ❌ **User frustration** (beyond minor UX) → DON'T use KRelay
- ✅ **Minor inconvenience only** → KRelay is PERFECT

---

## Real-World Anti-Patterns from Super App Experience

### ❌ Anti-Pattern 1: Banking/Payment Transactions

**The Mistake:**
```kotlin
class PaymentViewModel {
    fun processPayment(amount: Double) {
        viewModelScope.launch {
            // Call backend API
            val result = paymentApi.charge(userId, amount)

            if (result.isSuccess) {
                // ❌ WRONG: Critical notification via KRelay
                KRelay.dispatch<NotificationFeature> {
                    it.sendPaymentConfirmation(result.transactionId)
                }
                // If process dies here, confirmation is LOST
                // User's $1000 charged, but no receipt!
            }
        }
    }
}
```

**Why It's Dangerous:**
1. **Process Death Scenario**:
   - Backend charges $1000 successfully
   - Android OS kills app (low memory)
   - KRelay queue is wiped
   - User never receives confirmation notification
   - Customer support nightmare: "Where's my money?"

2. **Real Impact in Production**:
   - User panics, calls support
   - Support team spends 30 minutes verifying transaction
   - Lost trust in app
   - Potential refund requests
   - Bad app store reviews

**The Fix:**
```kotlin
class PaymentViewModel {
    fun processPayment(amount: Double) {
        // ✅ CORRECT: Use WorkManager for guaranteed execution
        val paymentWork = OneTimeWorkRequestBuilder<PaymentWorker>()
            .setInputData(workDataOf(
                "userId" to userId,
                "amount" to amount
            ))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(context).enqueue(paymentWork)

        // ✅ CORRECT: Use KRelay ONLY for UI feedback
        KRelay.dispatch<ToastFeature> {
            it.show("Payment processing...")
        }
    }
}

// PaymentWorker persists to disk, survives process death
class PaymentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val amount = inputData.getDouble("amount", 0.0)
        val result = paymentApi.charge(userId, amount)

        if (result.isSuccess) {
            // Guaranteed to execute, even after process death
            notificationManager.sendPaymentConfirmation(result.transactionId)
            return Result.success()
        } else {
            return Result.retry()
        }
    }
}
```

---

### ❌ Anti-Pattern 2: File Upload (User-Generated Content)

**The Mistake:**
```kotlin
class PhotoViewModel {
    fun uploadPhoto(photoData: ByteArray) {
        viewModelScope.launch {
            // ❌ WRONG: Dispatch upload via KRelay
            KRelay.dispatch<UploadFeature> {
                it.uploadPhotoToCloud(photoData)
                // If process dies, user's photo is LOST
                // User spent 10 minutes editing it!
            }
        }
    }
}
```

**Why It's Dangerous:**
1. **User Data Loss**:
   - User edits photo for 10 minutes
   - Taps "Upload"
   - Backgrounds app to check messages
   - OS kills app (low memory)
   - Photo never uploaded, edits lost forever
   - User rage-quits app

2. **Real-World Super App Scenario (Food Delivery)**:
   - Restaurant uploads dish photo
   - Photo stuck in KRelay queue
   - App killed (user swipes away)
   - Menu has no photo → Customers don't order
   - Restaurant loses revenue

**The Fix:**
```kotlin
class PhotoViewModel {
    fun uploadPhoto(photoUri: Uri) {
        // ✅ CORRECT: Save to local DB first
        viewModelScope.launch {
            // Persist photo locally
            val photoId = photoRepository.savePhotoLocally(photoUri)

            // Enqueue background upload (survives process death)
            val uploadWork = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
                .setInputData(workDataOf("photoId" to photoId))
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueue(uploadWork)

            // ✅ CORRECT: Use KRelay ONLY for UI feedback
            KRelay.dispatch<ToastFeature> {
                it.show("Upload queued")
            }
        }
    }
}
```

---

### ❌ Anti-Pattern 3: Critical Analytics (Business Metrics)

**The Mistake:**
```kotlin
class CheckoutViewModel {
    fun onPurchaseComplete(orderId: String, amount: Double) {
        // ❌ WRONG: Business-critical analytics via KRelay
        KRelay.dispatch<AnalyticsFeature> {
            it.trackPurchase(orderId, amount)
            // If process dies, event is LOST
            // Dashboard shows wrong revenue!
        }
    }
}
```

**Why It's Dangerous:**
1. **Business Impact**:
   - Revenue dashboard is inaccurate
   - CEO makes decisions based on wrong data
   - Marketing campaigns target wrong users
   - Investor reports are incorrect

2. **Super App Example (Ride Booking)**:
   - User completes $50 ride
   - Analytics event queued in KRelay
   - App killed (user force-stops)
   - Event lost → $50 missing from daily revenue report
   - Multiply by 1000 lost events → $50,000 reporting error

**The Fix:**
```kotlin
class CheckoutViewModel {
    fun onPurchaseComplete(orderId: String, amount: Double) {
        // ✅ CORRECT: Persist analytics to local DB
        viewModelScope.launch {
            analyticsRepository.queueEvent(
                PurchaseEvent(orderId, amount, timestamp = now())
            )
            // Background worker syncs to server
        }

        // ✅ CORRECT: Use KRelay for non-critical analytics only
        KRelay.dispatch<AnalyticsFeature> {
            it.trackScreenView("checkout_success")
            // If lost, not a big deal - just a screen view
        }
    }
}

// Background worker syncs queued events
class AnalyticsSyncWorker : CoroutineWorker {
    override suspend fun doWork(): Result {
        val events = analyticsRepository.getPendingEvents()
        analyticsApi.batchUpload(events)
        analyticsRepository.markAsSynced(events)
        return Result.success()
    }
}
```

---

### ❌ Anti-Pattern 4: Super App Module Conflicts

**The Mistake:**
```kotlin
// Module A: Ride Booking (Team Alpha)
class RideModule {
    fun init() {
        KRelay.register<ToastFeature>(RideToastImpl())
        // Team Alpha's toast: Blue color, bottom position
    }
}

// Module B: Food Delivery (Team Beta)
class FoodModule {
    fun init() {
        // ❌ WRONG: Overwrites Team Alpha's implementation!
        KRelay.register<ToastFeature>(FoodToastImpl())
        // Team Beta's toast: Red color, top position
    }
}

// Now ALL toasts in the app (including Ride) are red, top position!
// Team Alpha: "Why are our toasts red? We didn't change anything!"
```

**Why It's Dangerous:**
1. **Team Isolation Broken**:
   - Each team works independently
   - Module B breaks Module A without knowing
   - Integration testing nightmare
   - Finger-pointing in retrospectives

2. **Real Super App Example**:
   - 5 modules: Ride, Food, Pay, Shop, Invest
   - Each team uses same `ToastFeature` interface
   - Last module to call `register()` wins
   - Random toast styles depending on module load order
   - Users confused by inconsistent UI

**The Fix (v1.0 Workaround):**
```kotlin
// ✅ CORRECT: Feature Namespacing
interface RideModuleToastFeature : RelayFeature {
    fun show(message: String)
}

interface FoodModuleToastFeature : RelayFeature {
    fun show(message: String)
}

// Module A
KRelay.register<RideModuleToastFeature>(RideToastImpl())

// Module B
KRelay.register<FoodModuleToastFeature>(FoodToastImpl())

// Now isolated! Each module has its own toast style
```

**The Fix (v2.0 Future API):**
```kotlin
// ✅ BETTER: Instance-based KRelay (coming in v2.0)
val rideKRelay = KRelay.create("RideModule")
val foodKRelay = KRelay.create("FoodModule")

// Module A
rideKRelay.register<ToastFeature>(RideToastImpl())

// Module B
foodKRelay.register<ToastFeature>(FoodToastImpl())

// Truly isolated registries
```

---

### ❌ Anti-Pattern 5: Using KRelay for State Management

**The Mistake:**
```kotlin
// ❌ WRONG: Using KRelay as state container
interface StateFeature : RelayFeature {
    fun updateUserName(name: String)
    fun updateUserAge(age: Int)
}

class ProfileViewModel {
    fun onNameChanged(name: String) {
        KRelay.dispatch<StateFeature> { it.updateUserName(name) }
        // State updates via KRelay - BAD IDEA!
    }
}

@Composable
fun ProfileScreen() {
    var userName by remember { mutableStateOf("") }

    KRelay.register<StateFeature>(object : StateFeature {
        override fun updateUserName(name: String) {
            userName = name // This won't recompose properly!
        }
    })
}
```

**Why It's Dangerous:**
1. **No Reactive Updates**: Compose won't recompose when state changes via KRelay
2. **State Lost on Process Death**: Username disappears when app is killed
3. **Race Conditions**: Multiple dispatches can arrive out of order
4. **Debugging Nightmare**: State changes are invisible in debugger

**The Fix:**
```kotlin
// ✅ CORRECT: Use StateFlow for state management
class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            profileRepository.save(uiState.value)

            // ✅ CORRECT: Use KRelay ONLY for UI feedback
            KRelay.dispatch<ToastFeature> {
                it.show("Profile saved!")
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    // Reactive updates work correctly
    Text(uiState.userName)
}
```

---

### ❌ Anti-Pattern 6: Expecting Return Values

**The Mistake:**
```kotlin
// ❌ WRONG: Trying to get return values from KRelay
class BatteryViewModel {
    fun checkBattery() {
        KRelay.dispatch<BatteryFeature> { battery ->
            val level = battery.getBatteryLevel() // How to get this value back?!
            // Cannot return values from KRelay!
        }
    }
}
```

**Why It Doesn't Work:**
- KRelay is **fire-and-forget**
- Lambda executes asynchronously on main thread
- No way to return values from dispatched actions

**The Fix:**
```kotlin
// ✅ CORRECT: Use expect/actual for platform values
// commonMain
expect fun getBatteryLevel(): Int

// androidMain
actual fun getBatteryLevel(): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

// iosMain
actual fun getBatteryLevel(): Int {
    return (UIDevice.currentDevice.batteryLevel * 100).toInt()
}

// Usage in ViewModel
class BatteryViewModel {
    fun checkBattery() {
        val level = getBatteryLevel() // Synchronous, returns value

        // ✅ CORRECT: Use KRelay for UI feedback only
        KRelay.dispatch<ToastFeature> {
            it.show("Battery: $level%")
        }
    }
}
```

---

### ❌ Anti-Pattern 7: Heavy Background Processing

**The Mistake:**
```kotlin
// ❌ WRONG: Heavy work on main thread via KRelay
class DataViewModel {
    fun processData() {
        KRelay.dispatch<ProcessingFeature> {
            it.processLargeFile() // Blocks UI thread!
            // App freezes, ANR (Application Not Responding)
        }
    }
}
```

**Why It's Dangerous:**
- KRelay **always executes on main thread**
- Heavy work causes UI freezes
- ANR dialog → Bad user experience
- App may be killed by Android

**The Fix:**
```kotlin
// ✅ CORRECT: Use Dispatchers.IO for heavy work
class DataViewModel {
    fun processData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Heavy work on background thread
            val result = processLargeFile()

            // Switch to main thread for UI update
            withContext(Dispatchers.Main) {
                KRelay.dispatch<ToastFeature> {
                    it.show("Processing complete!")
                }
            }
        }
    }
}
```

---

### ❌ Anti-Pattern 8: Database Operations

**The Mistake:**
```kotlin
// ❌ WRONG: Database writes via KRelay
class UserViewModel {
    fun saveUser(user: User) {
        KRelay.dispatch<DatabaseFeature> {
            it.insertUser(user)
            // If process dies, user data is LOST!
        }
    }
}
```

**Why It's Dangerous:**
- Data never persisted if process dies
- User expects "Save" button to persist data
- Data loss = loss of user trust

**The Fix:**
```kotlin
// ✅ CORRECT: Use Room/SQLite directly
class UserViewModel(private val userRepository: UserRepository) {
    fun saveUser(user: User) {
        viewModelScope.launch {
            // Persist to database immediately
            userRepository.insert(user)

            // ✅ CORRECT: Use KRelay ONLY for UI feedback
            KRelay.dispatch<ToastFeature> {
                it.show("User saved!")
            }
        }
    }
}
```

---

## ✅ Good Patterns: When to Use KRelay

### ✅ Pattern 1: Toast/Snackbar Notifications

```kotlin
class LoginViewModel {
    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = authService.login(username, password)

            if (result.isSuccess) {
                // ✅ GOOD: UI feedback - acceptable to lose
                KRelay.dispatch<ToastFeature> {
                    it.show("Welcome back, ${result.user.name}!")
                }
            }
        }
    }
}
```

**Why It's Safe:**
- If toast is lost, user still logged in successfully
- They can see they're logged in (UI updates)
- Missing toast = Minor UX issue, not data loss

---

### ✅ Pattern 2: Navigation

```kotlin
class LoginViewModel {
    fun onLoginSuccess() {
        // ✅ GOOD: Navigation command
        KRelay.dispatch<NavigationFeature> {
            it.goToHome()
        }
    }
}
```

**Why It's Safe:**
- If navigation is lost, user stays on login screen
- They can manually navigate or trigger action again
- No data loss

---

### ✅ Pattern 3: Haptic Feedback

```kotlin
class GameViewModel {
    fun onButtonTap() {
        // ✅ GOOD: Haptic feedback
        KRelay.dispatch<HapticFeature> {
            it.vibrate(50)
        }
    }
}
```

**Why It's Safe:**
- If vibration is lost, user just doesn't feel it
- Completely non-critical
- No impact on functionality

---

### ✅ Pattern 4: Permission Requests

```kotlin
class CameraViewModel {
    fun takePicture() {
        // ✅ GOOD: Request permission
        KRelay.dispatch<PermissionFeature> {
            it.requestCamera { granted ->
                if (granted) startCamera()
            }
        }
    }
}
```

**Why It's Safe:**
- If request is lost, user can tap button again
- Permission dialog will show next time
- No permanent impact

---

## Decision Checklist

Before using KRelay, ask yourself:

| Question | If YES → | If NO → |
|----------|----------|---------|
| Can user retry if action is lost? | ✅ Safe for KRelay | ❌ Use WorkManager |
| Is this just UI feedback? | ✅ Safe for KRelay | ❌ Use proper persistence |
| Would losing this cause data loss? | ❌ DON'T use KRelay | ✅ Safe for KRelay |
| Would losing this cost money? | ❌ DON'T use KRelay | ✅ Safe for KRelay |
| Does this need to survive process death? | ❌ DON'T use KRelay | ✅ Safe for KRelay |
| Is this operation critical for business? | ❌ DON'T use KRelay | ✅ Safe for KRelay |

---

## Summary

### ❌ NEVER Use KRelay For:
1. Banking/Payment transactions
2. File uploads (user-generated content)
3. Critical analytics (revenue, conversions)
4. Database writes
5. Any operation requiring guaranteed execution
6. Return values (use expect/actual)
7. State management (use StateFlow)
8. Heavy background processing

### ✅ ALWAYS Use KRelay For:
1. Toast/Snackbar notifications
2. Navigation commands
3. Haptic feedback / Vibration
4. Permission requests
5. In-app notifications (non-critical)
6. UI refresh triggers
7. Simple analytics (screen views, button taps)

### 🔧 Alternatives
- **Critical Operations**: WorkManager
- **State Management**: StateFlow
- **Return Values**: expect/actual
- **Heavy Work**: Dispatchers.IO
- **Persistence**: Room/DataStore
- **Super App Isolation**: Feature Namespacing (v1.0) or wait for v2.0

---

## Testing Your Usage

Run this checklist on your codebase:

```bash
# Search for dangerous KRelay usage patterns
grep -r "dispatch.*Payment" .
grep -r "dispatch.*Upload" .
grep -r "dispatch.*Database" .
grep -r "dispatch.*Analytics" .

# If found, review if those operations are critical
```

---

## Getting Help

If unsure whether your use case is safe:

1. Check the [ADR](./adr/0001-singleton-and-serialization-tradeoffs.md)
2. Review the `@ProcessDeathUnsafe` annotation documentation
3. Ask yourself: "If this action is lost, what's the worst that happens?"
4. When in doubt, use WorkManager instead

---

**Remember: KRelay is for UI commands, not critical operations. Choose the right tool for the job!**

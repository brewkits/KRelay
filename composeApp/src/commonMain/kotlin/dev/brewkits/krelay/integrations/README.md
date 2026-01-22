# Real Library Integrations

This package contains **REAL** implementations of KRelay features using popular KMP libraries.

## Libraries Used

### 1. Moko Permissions (0.18.0)
- **Purpose**: Cross-platform permission management
- **Features**: Camera, Location, Microphone, Gallery permissions
- **Platform**: Android & iOS
- **Implementation**: `MokoPermissionImpl`, `IOSMokoPermissionImpl`

### 2. Moko Biometry (0.4.0) / AndroidX Biometric (1.1.0)
- **Purpose**: Biometric authentication
- **Features**: FaceID, TouchID, Fingerprint
- **Platform**: Android (AndroidX Biometric), iOS (Moko Biometry + LocalAuthentication)
- **Implementation**: `AndroidBiometricImpl`, `IOSBiometricImpl`

### 3. Peekaboo (0.5.2)
- **Purpose**: Image and camera picking for Compose Multiplatform
- **Features**: Single/multiple image selection, camera capture
- **Platform**: Android & iOS
- **Implementation**: `PeekabooMediaImpl`, `IOSPeekabooMediaImpl`

### 4. Play Core Review (2.0.1) / StoreKit
- **Purpose**: In-app review dialogs
- **Features**: Native 5-star rating dialogs
- **Platform**: Android (Play Core), iOS (StoreKit)
- **Implementation**: `AndroidSystemInteractionImpl`, `IOSSystemInteractionImpl`

---

## Architecture

### Expect/Actual Pattern

```
RealIntegrations.kt (Common)
├── expect fun rememberPermissionImplementation()
├── expect fun rememberBiometricImplementation()
├── expect fun rememberMediaImplementation()
└── expect fun rememberSystemInteractionImplementation()

AndroidIntegrations.kt (androidMain)
├── actual fun rememberPermissionImplementation() → MokoPermissionImpl
├── actual fun rememberBiometricImplementation() → AndroidBiometricImpl
├── actual fun rememberMediaImplementation() → PeekabooMediaImpl
└── actual fun rememberSystemInteractionImplementation() → AndroidSystemInteractionImpl

IOSIntegrations.kt (iosMain)
├── actual fun rememberPermissionImplementation() → IOSMokoPermissionImpl
├── actual fun rememberBiometricImplementation() → IOSBiometricImpl
├── actual fun rememberMediaImplementation() → IOSPeekabooMediaImpl
└── actual fun rememberSystemInteractionImplementation() → IOSSystemInteractionImpl
```

---

## Usage in Demo

### In IntegrationsDemo.kt:

```kotlin
@Composable
fun IntegrationsDemo(onBackClick: () -> Unit) {
    var useRealImplementations by remember { mutableStateOf(true) }

    if (useRealImplementations) {
        // Use REAL library implementations
        SetupRealIntegrations()
    } else {
        // Use MOCK implementations
        SetupMockIntegrations()
    }

    // ... rest of UI
}
```

### SetupRealIntegrations():

```kotlin
@Composable
fun SetupRealIntegrations() {
    // Platform-specific implementations via expect/actual
    val permissionImpl = rememberPermissionImplementation()
    val biometricImpl = rememberBiometricImplementation()
    val mediaImpl = rememberMediaImplementation()
    val systemInteractionImpl = rememberSystemInteractionImplementation()

    LaunchedEffect(Unit) {
        KRelay.register<PermissionFeature>(permissionImpl)
        KRelay.register<BiometricFeature>(biometricImpl)
        KRelay.register<MediaFeature>(mediaImpl)
        KRelay.register<SystemInteractionFeature>(systemInteractionImpl)
    }
}
```

---

## Platform-Specific Details

### Android

#### Permissions (Moko)
- Uses `PermissionsController` bound to Activity lifecycle
- Automatically handles permission dialogs
- Works with both runtime and install-time permissions

#### Biometrics (AndroidX)
- Uses `BiometricPrompt` API
- Supports Fingerprint, Face unlock, and device credentials
- Requires `FragmentActivity` context

#### Media (Peekaboo)
- Uses `rememberImagePickerLauncher`
- Integrates with Android's photo picker
- Handles permissions automatically

#### Review (Play Core)
- Uses `ReviewManager` from Play Core library
- Shows Google Play's in-app review dialog
- OS controls when dialog actually appears

### iOS

#### Permissions (Moko)
- Uses `PermissionsController` bound to ViewController
- Handles iOS permission dialogs
- Works with Info.plist permission descriptions

#### Biometrics (LocalAuthentication)
- Uses `LAContext` from LocalAuthentication framework
- Supports FaceID and TouchID
- Gracefully handles unavailable biometrics

#### Media (Peekaboo)
- Uses `rememberImagePickerLauncher`
- Integrates with iOS photo picker (PHPicker)
- Handles permissions automatically

#### Review (StoreKit)
- Uses `SKStoreReviewController.requestReview()`
- Shows App Store rating dialog
- iOS controls when dialog appears (rate limiting)

---

## Testing

### Unit Tests

Mock implementations are still provided in `IntegrationsDemoImplementations.kt` for testing:

```kotlin
class MyViewModelTest {
    @Test
    fun `test camera permission request`() {
        // Use mock implementation
        KRelay.register<PermissionFeature>(MockPermissionImpl())

        val viewModel = MyViewModel()
        viewModel.requestCamera()

        // Assert behavior without showing actual dialog
    }
}
```

### Integration Tests

Real implementations can be tested on devices/simulators:

1. Run demo app on Android device/emulator
2. Toggle to "REAL" mode
3. Test each feature:
   - Camera permission → Shows actual Android permission dialog
   - Biometric auth → Shows fingerprint/face scanner
   - Image picker → Opens real photo gallery
   - In-app review → Shows Google Play review dialog (if conditions met)

---

## Requirements

### Android

- `minSdk = 24` (Android 7.0+)
- `targetSdk = 36` (latest)
- Permissions in `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
  <uses-feature android:name="android.hardware.fingerprint" android:required="false" />
  ```

### iOS

- `iOS 14.0+` deployment target
- Permissions in `Info.plist`:
  ```xml
  <key>NSCameraUsageDescription</key>
  <string>We need camera access to take photos</string>

  <key>NSPhotoLibraryUsageDescription</key>
  <string>We need photo library access to select images</string>

  <key>NSFaceIDUsageDescription</key>
  <string>We use Face ID to authenticate you</string>
  ```

---

## Troubleshooting

### Moko Permissions

**Issue**: Permission dialog doesn't show
- **Solution**: Ensure `BindEffect(controller)` is called in Compose
- **Solution**: Check Activity/ViewController lifecycle binding

### Biometric Authentication

**Issue**: "Biometric not available" error
- **Android**: Check if device has fingerprint/face unlock enabled
- **iOS**: Check if FaceID/TouchID is configured in device settings

### Peekaboo Image Picker

**Issue**: Picker doesn't open
- **Solution**: Check gallery permissions are granted
- **Solution**: Ensure `rememberImagePickerLauncher` is called in Composition

### In-App Review

**Issue**: Review dialog doesn't appear
- **Android**: Google Play may throttle requests (test quota limits)
- **iOS**: iOS rate-limits review prompts (3 times per 365 days)
- **Solution**: This is expected behavior - OS controls visibility

---

## Adding More Integrations

To add a new library integration:

1. **Define interface** in `krelay/samples/` package:
   ```kotlin
   interface MyFeature : RelayFeature {
       fun doSomething()
   }
   ```

2. **Add dependency** to `build.gradle.kts` and `libs.versions.toml`

3. **Implement for Android** in `androidMain/`:
   ```kotlin
   class AndroidMyFeatureImpl : MyFeature {
       override fun doSomething() {
           // Use Android library
       }
   }
   ```

4. **Implement for iOS** in `iosMain/`:
   ```kotlin
   class IOSMyFeatureImpl : MyFeature {
       override fun doSomething() {
           // Use iOS library
       }
   }
   ```

5. **Add expect/actual** in `RealIntegrations.kt`

6. **Register** in `SetupRealIntegrations()`

---

## Resources

- [Moko Permissions](https://github.com/icerockdev/moko-permissions)
- [Moko Biometry](https://github.com/icerockdev/moko-biometry)
- [Peekaboo](https://github.com/onseok/peekaboo)
- [Play Core](https://developer.android.com/guide/playcore/in-app-review)
- [StoreKit Review](https://developer.apple.com/documentation/storekit/skstorereviewcontroller)

---

**Questions?** Open an issue or check the main [INTEGRATION_BEST_PRACTICES.md](../../../../INTEGRATION_BEST_PRACTICES.md)

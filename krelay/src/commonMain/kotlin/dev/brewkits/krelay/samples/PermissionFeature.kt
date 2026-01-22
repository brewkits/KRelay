package dev.brewkits.krelay.samples

import dev.brewkits.krelay.RelayFeature

/**
 * Permission feature for requesting platform-specific permissions.
 *
 * Use Case: Request permissions from shared code without coupling to platform APIs.
 *
 * Example:
 * ```kotlin
 * KRelay.dispatch<PermissionFeature> {
 *     it.requestCamera { granted ->
 *         if (granted) startCamera()
 *     }
 * }
 * ```
 *
 * Platform Implementation:
 * - Android: Uses ActivityCompat.requestPermissions
 * - iOS: Uses AVCaptureDevice.requestAccess
 */
interface PermissionFeature : RelayFeature {

    /**
     * Request camera permission.
     * @param callback Called with true if granted, false if denied
     */
    fun requestCamera(callback: (Boolean) -> Unit)

    /**
     * Request location permission.
     * @param callback Called with true if granted, false if denied
     */
    fun requestLocation(callback: (Boolean) -> Unit)

    /**
     * Request microphone permission.
     * @param callback Called with true if granted, false if denied
     */
    fun requestMicrophone(callback: (Boolean) -> Unit)

    /**
     * Request storage/photos permission.
     * @param callback Called with true if granted, false if denied
     */
    fun requestStorage(callback: (Boolean) -> Unit)

    /**
     * Check if camera permission is granted.
     * @return true if permission is granted
     */
    fun isCameraGranted(): Boolean

    /**
     * Check if location permission is granted.
     * @return true if permission is granted
     */
    fun isLocationGranted(): Boolean
}

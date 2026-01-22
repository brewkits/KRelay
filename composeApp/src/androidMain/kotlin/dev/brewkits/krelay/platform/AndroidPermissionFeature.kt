package dev.brewkits.krelay.platform

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.brewkits.krelay.samples.PermissionFeature

/**
 * Android implementation of PermissionFeature.
 *
 * Uses ActivityCompat for permission requests.
 *
 * IMPORTANT: This is a simplified demo implementation.
 * In production, you should:
 * - Handle permission rationale (shouldShowRequestPermissionRationale)
 * - Store callbacks properly to handle result in onRequestPermissionsResult
 * - Consider using Activity Result API instead
 */
class AndroidPermissionFeature(
    private val activity: Activity
) : PermissionFeature {

    // In production, use a proper callback storage mechanism
    private var cameraCallback: ((Boolean) -> Unit)? = null
    private var locationCallback: ((Boolean) -> Unit)? = null
    private var microphoneCallback: ((Boolean) -> Unit)? = null
    private var storageCallback: ((Boolean) -> Unit)? = null

    override fun requestCamera(callback: (Boolean) -> Unit) {
        if (isCameraGranted()) {
            callback(true)
            return
        }

        cameraCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA
        )
    }

    override fun requestLocation(callback: (Boolean) -> Unit) {
        if (isLocationGranted()) {
            callback(true)
            return
        }

        locationCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION
        )
    }

    override fun requestMicrophone(callback: (Boolean) -> Unit) {
        microphoneCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_MICROPHONE
        )
    }

    override fun requestStorage(callback: (Boolean) -> Unit) {
        storageCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_STORAGE
        )
    }

    override fun isCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Call this from Activity.onRequestPermissionsResult()
     *
     * Example:
     * ```
     * override fun onRequestPermissionsResult(
     *     requestCode: Int,
     *     permissions: Array<out String>,
     *     grantResults: IntArray
     * ) {
     *     permissionFeature.onRequestPermissionsResult(requestCode, grantResults)
     *     super.onRequestPermissionsResult(requestCode, permissions, grantResults)
     * }
     * ```
     */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        when (requestCode) {
            REQUEST_CAMERA -> {
                cameraCallback?.invoke(granted)
                cameraCallback = null
            }
            REQUEST_LOCATION -> {
                locationCallback?.invoke(granted)
                locationCallback = null
            }
            REQUEST_MICROPHONE -> {
                microphoneCallback?.invoke(granted)
                microphoneCallback = null
            }
            REQUEST_STORAGE -> {
                storageCallback?.invoke(granted)
                storageCallback = null
            }
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
        private const val REQUEST_LOCATION = 1002
        private const val REQUEST_MICROPHONE = 1003
        private const val REQUEST_STORAGE = 1004
    }
}

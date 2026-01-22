package dev.brewkits.krelay.samples

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature

/**
 * Media feature for camera and gallery access.
 *
 * Use Case: Open camera or photo gallery from shared code without coupling
 * to platform-specific image picker libraries (Peekaboo, Compose ImagePicker, etc.)
 *
 * Example:
 * ```kotlin
 * // ViewModel triggers gallery open
 * KRelay.dispatch<MediaFeature> {
 *     it.pickImageFromGallery { imageData ->
 *         if (imageData != null) {
 *             uploadAvatar(imageData)
 *         }
 *     }
 * }
 *
 * // ViewModel triggers camera
 * KRelay.dispatch<MediaFeature> {
 *     it.capturePhoto { imageData ->
 *         if (imageData != null) {
 *             processPhoto(imageData)
 *         }
 *     }
 * }
 * ```
 *
 * Platform Implementation:
 * - Android: Uses ActivityResultContracts (PickVisualMedia, TakePicture)
 * - iOS: Uses UIImagePickerController or PHPickerViewController
 * - KMP: Can use Peekaboo library (https://github.com/onseok/peekaboo)
 *
 * Benefits with KRelay:
 * - ViewModel triggers picker without rememberImagePickerLauncher coupling
 * - UI layer handles Compose state (remember, LaunchedEffect)
 * - Fire-and-forget pattern for media operations
 * - Easy to mock for testing (no actual picker dialogs in tests)
 *
 * Integration with Peekaboo:
 * ```kotlin
 * // In UI layer
 * val singleImagePicker = rememberImagePickerLauncher(
 *     selectionMode = SelectionMode.Single,
 *     onResult = { images -> handlePickedImages(images) }
 * )
 *
 * LaunchedEffect(Unit) {
 *     KRelay.register(object : MediaFeature {
 *         override fun pickImageFromGallery(callback: (ByteArray?) -> Unit) {
 *             currentCallback = callback
 *             singleImagePicker.launch()
 *         }
 *     })
 * }
 * ```
 */
interface MediaFeature : RelayFeature {

    /**
     * Pick single image from gallery.
     *
     * @param callback Called with image data (ByteArray) or null if cancelled
     */
    fun pickImageFromGallery(callback: (ByteArray?) -> Unit)

    /**
     * Pick multiple images from gallery.
     *
     * @param maxCount Maximum number of images to pick
     * @param callback Called with list of image data or empty list if cancelled
     */
    fun pickMultipleImages(maxCount: Int = 5, callback: (List<ByteArray>) -> Unit)

    /**
     * Capture photo using camera.
     *
     * @param callback Called with image data or null if cancelled
     */
    fun capturePhoto(callback: (ByteArray?) -> Unit)

    /**
     * Pick video from gallery.
     *
     * @param callback Called with video data or null if cancelled
     */
    fun pickVideo(callback: (ByteArray?) -> Unit)

    /**
     * Record video using camera.
     *
     * @param maxDurationSeconds Maximum video duration in seconds
     * @param callback Called with video data or null if cancelled
     */
    fun recordVideo(maxDurationSeconds: Int = 60, callback: (ByteArray?) -> Unit)

    /**
     * Check if camera is available.
     */
    fun isCameraAvailable(): Boolean

    /**
     * Check if gallery is available.
     */
    fun isGalleryAvailable(): Boolean
}

/**
 * Result wrapper for media operations with metadata.
 */
data class MediaResult(
    val data: ByteArray,
    val mimeType: String,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

/**
 * Enhanced MediaFeature with metadata support.
 */
interface EnhancedMediaFeature : RelayFeature {
    fun pickImage(callback: (MediaResult?) -> Unit)
    fun capturePhoto(callback: (MediaResult?) -> Unit)
}

/**
 * Example ViewModel using MediaFeature.
 *
 * Shows how to trigger media picking without UI coupling.
 */
class MediaDemoViewModel {

    /**
     * Update user avatar.
     *
     * Classic use case - let user pick profile photo.
     */
    fun updateAvatar() {
        // ViewModel just dispatches - no Peekaboo coupling!
        KRelay.dispatch<MediaFeature> {
            it.pickImageFromGallery { imageData ->
                if (imageData != null) {
                    uploadAvatar(imageData)
                } else {
                    // User cancelled
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Avatar update cancelled")
                    }
                }
            }
        }
    }

    /**
     * Take photo for document verification.
     */
    fun captureDocument() {
        // First check camera permission
        KRelay.dispatch<PermissionFeature> { permission ->
            permission.requestCamera { granted ->
                if (granted) {
                    openCamera()
                } else {
                    showPermissionDenied()
                }
            }
        }
    }

    /**
     * Upload multiple photos to gallery.
     */
    fun uploadPhotosToAlbum() {
        KRelay.dispatch<MediaFeature> {
            it.pickMultipleImages(maxCount = 10) { images ->
                if (images.isNotEmpty()) {
                    uploadPhotos(images)
                    KRelay.dispatch<ToastFeature> { toast ->
                        toast.showShort("Uploading ${images.size} photos...")
                    }
                }
            }
        }
    }

    /**
     * Show choice between camera and gallery.
     *
     * Demonstrates conditional dispatching.
     */
    fun selectPhotoSource(useCamera: Boolean) {
        KRelay.dispatch<MediaFeature> { media ->
            if (useCamera && media.isCameraAvailable()) {
                media.capturePhoto { imageData ->
                    handlePhoto(imageData)
                }
            } else if (media.isGalleryAvailable()) {
                media.pickImageFromGallery { imageData ->
                    handlePhoto(imageData)
                }
            } else {
                KRelay.dispatch<ToastFeature> { toast ->
                    toast.showShort("No camera or gallery available")
                }
            }
        }
    }

    /**
     * Record short video for story/reel.
     */
    fun recordStory() {
        KRelay.dispatch<PermissionFeature> { permission ->
            permission.requestCamera { granted ->
                if (!granted) return@requestCamera

                KRelay.dispatch<MediaFeature> { media ->
                    media.recordVideo(maxDurationSeconds = 30) { videoData ->
                        if (videoData != null) {
                            uploadStory(videoData)
                        }
                    }
                }
            }
        }
    }

    // Helper functions
    private fun openCamera() {
        KRelay.dispatch<MediaFeature> {
            it.capturePhoto { imageData ->
                if (imageData != null) {
                    processDocument(imageData)
                    KRelay.dispatch<HapticFeature> { haptic ->
                        haptic.success()
                    }
                }
            }
        }
    }

    private fun uploadAvatar(imageData: ByteArray) {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Uploading avatar...")
        }

        // Simulate upload
        KRelay.dispatch<AnalyticsFeature> {
            it.track("avatar_uploaded", mapOf("size" to imageData.size))
        }
    }

    private fun uploadPhotos(images: List<ByteArray>) {
        KRelay.dispatch<AnalyticsFeature> {
            it.track("photos_uploaded", mapOf("count" to images.size))
        }
    }

    private fun handlePhoto(imageData: ByteArray?) {
        if (imageData != null) {
            KRelay.dispatch<ToastFeature> {
                it.showShort("Photo selected: ${imageData.size} bytes")
            }
        }
    }

    private fun processDocument(imageData: ByteArray) {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Document captured successfully")
        }
    }

    private fun uploadStory(videoData: ByteArray) {
        KRelay.dispatch<ToastFeature> {
            it.showShort("Uploading story...")
        }
    }

    private fun showPermissionDenied() {
        KRelay.dispatch<ToastFeature> {
            it.showLong("Camera permission required")
        }
    }
}

/**
 * Advanced example: Photo editing workflow.
 *
 * Shows complex multi-step media workflow.
 */
class PhotoEditingViewModel {

    /**
     * Complete photo workflow: Pick -> Edit -> Upload
     */
    fun startPhotoWorkflow() {
        // Step 1: Pick image
        KRelay.dispatch<MediaFeature> { media ->
            media.pickImageFromGallery { imageData ->
                if (imageData == null) return@pickImageFromGallery

                // Step 2: Navigate to editor
                KRelay.dispatch<NavigationFeature> { nav ->
                    nav.navigateTo("photo-editor")
                }

                // Step 3: Track event
                KRelay.dispatch<AnalyticsFeature> { analytics ->
                    analytics.track("photo_workflow_started")
                }
            }
        }
    }

    /**
     * Quick photo upload with progress.
     */
    fun quickUpload() {
        KRelay.dispatch<MediaFeature> { media ->
            media.pickImageFromGallery { imageData ->
                if (imageData == null) return@pickImageFromGallery

                // Show upload progress
                KRelay.dispatch<ToastFeature> {
                    it.showLong("Uploading...")
                }

                // Simulate upload (in real app, call repository)
                // uploadRepository.upload(imageData)

                // Show success
                KRelay.dispatch<HapticFeature> {
                    it.success()
                }

                KRelay.dispatch<NotificationBridge> {
                    it.showInAppNotification(
                        title = "Upload Complete",
                        message = "Photo uploaded successfully"
                    )
                }
            }
        }
    }
}

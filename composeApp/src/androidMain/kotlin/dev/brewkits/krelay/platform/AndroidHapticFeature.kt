package dev.brewkits.krelay.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dev.brewkits.krelay.samples.HapticFeature
import dev.brewkits.krelay.samples.HapticStyle

/**
 * Android implementation of HapticFeature.
 *
 * Uses Vibrator/VibrationEffect for haptic feedback.
 *
 * IMPORTANT:
 * - Requires VIBRATE permission in AndroidManifest.xml
 * - Uses new VibrationEffect API for Android O+ (API 26+)
 * - Falls back to legacy vibrate() for older versions
 */
class AndroidHapticFeature(
    private val context: Context
) : HapticFeature {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    override fun impact(style: HapticStyle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (style) {
                HapticStyle.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticStyle.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticStyle.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            }
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Fallback for API 26-28
            val duration = when (style) {
                HapticStyle.LIGHT -> 10L
                HapticStyle.MEDIUM -> 20L
                HapticStyle.HEAVY -> 50L
            }
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            // Fallback for older versions
            @Suppress("DEPRECATION")
            vibrator.vibrate(when (style) {
                HapticStyle.LIGHT -> 10L
                HapticStyle.MEDIUM -> 20L
                HapticStyle.HEAVY -> 50L
            })
        }
    }

    override fun selection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrate(10)
        }
    }

    override fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 50),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    }

    override fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrate(100)
        }
    }

    override fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 50, 100),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }
}

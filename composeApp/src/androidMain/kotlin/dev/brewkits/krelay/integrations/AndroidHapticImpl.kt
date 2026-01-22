package dev.brewkits.krelay.integrations

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import dev.brewkits.krelay.samples.HapticFeature
import dev.brewkits.krelay.samples.HapticStyle

/**
 * Real Android Haptic implementation using Vibrator API.
 */
class AndroidHapticImpl(
    private val context: Context
) : HapticFeature {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    override fun impact(style: HapticStyle) {
        val duration = when (style) {
            HapticStyle.LIGHT -> 20L
            HapticStyle.MEDIUM -> 40L
            HapticStyle.HEAVY -> 60L
        }
        vibrate(duration)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            vibrate(50)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            vibrate(100)
        }
    }

    override fun warning() {
        vibrate(75)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun selection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrate(10)
        }
    }
}

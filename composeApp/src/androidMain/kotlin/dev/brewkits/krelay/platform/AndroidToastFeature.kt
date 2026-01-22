package dev.brewkits.krelay.platform

import android.content.Context
import android.widget.Toast
import dev.brewkits.krelay.samples.ToastFeature

/**
 * Android implementation of ToastFeature using Android Toast API.
 *
 * This class should be registered in Activity.onCreate() and will automatically
 * be unregistered when the Activity is destroyed (thanks to WeakReference).
 */
class AndroidToastFeature(private val context: Context) : ToastFeature {

    override fun showShort(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLong(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

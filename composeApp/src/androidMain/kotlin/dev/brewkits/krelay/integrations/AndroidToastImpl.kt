package dev.brewkits.krelay.integrations

import android.content.Context
import android.widget.Toast
import dev.brewkits.krelay.samples.ToastFeature

/**
 * Real Android Toast implementation using Android Toast API.
 */
class AndroidToastImpl(
    private val context: Context
) : ToastFeature {

    override fun showShort(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLong(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

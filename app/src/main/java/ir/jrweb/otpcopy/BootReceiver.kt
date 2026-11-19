package ir.jrweb.otpcopy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val prefs = context.getSharedPreferences("jrweb_otp_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_enabled", true)) return

        try {
            val serviceIntent = Intent(context, OtpMonitorService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) { }
    }
}

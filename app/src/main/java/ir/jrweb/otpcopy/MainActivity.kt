package ir.jrweb.otpcopy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("jrweb_otp_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("is_enabled", true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4A148C"))
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "JRweb OTP Copy"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "کپی خودکار کد تایید و رمز دوم + بوق تایید"
            textSize = 14f
            setTextColor(Color.parseColor("#E1BEE7"))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 48)
        }

        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val toggle = Switch(this).apply {
            text = "فعال بودن کپی خودکار  "
            setTextColor(Color.WHITE)
            textSize = 16f
            isChecked = isEnabled
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("is_enabled", isChecked).apply()
                if (isChecked) {
                    startMonitorService()
                } else {
                    stopMonitorService()
                }
            }
        }

        val permBtn = Button(this).apply {
            text = "تایید دسترسی پیامک (SMS)"
            setBackgroundColor(Color.WHITE)
            setTextColor(Color.parseColor("#4A148C"))
            setPadding(32, 24, 32, 24)
            setOnClickListener { requestSmsPermission() }
        }

        val footer = TextView(this).apply {
            text = "طراحی شده توسط JRweb"
            textSize = 13f
            setTextColor(Color.parseColor("#CE93D8"))
            gravity = Gravity.CENTER
            setPadding(0, 80, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(statusText)
        root.addView(toggle)

        val spacer = TextView(this).apply { height = 40 }
        root.addView(spacer)

        root.addView(permBtn)
        root.addView(footer)

        setContentView(root)
        updatePermissionStatus()

        if (isEnabled && hasSmsPermission()) {
            startMonitorService()
        }
    }

    private fun startMonitorService() {
        val intent = Intent(this, OtpMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopMonitorService() {
        val intent = Intent(this, OtpMonitorService::class.java)
        stopService(intent)
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
    }

    private fun updatePermissionStatus() {
        statusText.text = if (hasSmsPermission()) {
            "وضعیت: آماده به کار (سرویس پس‌زمینه فعال است)"
        } else {
            "وضعیت: نیاز به تایید دسترسی پیامک"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionStatus()
        val prefs = getSharedPreferences("jrweb_otp_prefs", Context.MODE_PRIVATE)
        if (hasSmsPermission() && prefs.getBoolean("is_enabled", true)) {
            startMonitorService()
        }
    }
}

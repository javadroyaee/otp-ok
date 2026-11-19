package ir.jrweb.otpcopy

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("jrweb_otp_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_enabled", true)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullMessage = messages.joinToString("") { it.messageBody ?: "" }

        val code = extractOtpCode(fullMessage) ?: return

        Handler(Looper.getMainLooper()).post {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("JRweb OTP", code)
                clipboard.setPrimaryClip(clip)
            } catch (_: Exception) { }

            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                Handler(Looper.getMainLooper()).postDelayed({
                    toneGen.release()
                }, 250)
            } catch (_: Exception) { }

            Toast.makeText(context, "کد $code کپی شد (JRweb)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractOtpCode(message: String): String? {
        val normalized = convertPersianDigitsToEnglish(message)
        val lower = normalized.lowercase()

        val keywords = listOf(
            "رمز", "کد", "تایید", "تائید", "پویا", "یکبار", "ورود",
            "عبور", "کلمه عبور", "پسورد", "پاسورد", "احراز", "فعال",
            "شاپرک", "درگاه",
            "otp", "code", "pass", "password", "passcode",
            "verify", "verification", "pin", "auth", "token", "login", "2fa"
        )
        if (keywords.none { lower.contains(it) }) return null

        val numericRegex = Regex("""(?<![\d.,/\-])\d{4,8}(?![\d.,/\-])""")
        numericRegex.find(normalized)?.value?.let { return it }

        val alphaNumRegex = Regex("""(?<![A-Za-z0-9/\-.])(?=[A-Za-z0-9]{4,8}(?![A-Za-z0-9/\-.]))(?=.*\d)[A-Za-z0-9]{4,8}""")
        return alphaNumRegex.find(normalized)?.value
    }

    private fun convertPersianDigitsToEnglish(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persian[i], ('0' + i))
            result = result.replace(arabic[i], ('0' + i))
        }
        return result
    }
}

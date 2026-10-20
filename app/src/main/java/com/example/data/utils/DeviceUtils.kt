package com.example.data.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object DeviceUtils {

    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = Build.MODEL ?: "Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }

    fun getDeviceFingerprint(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            val raw = "${Build.BRAND}-${Build.MANUFACTURER}-${Build.MODEL}-${Build.HARDWARE}-$androidId"
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(raw.toByteArray())
            hash.joinToString("") { "%02x".format(it) }.take(16).uppercase()
        } catch (e: Exception) {
            UUID.randomUUID().toString().take(16).uppercase()
        }
    }

    fun generateLicenseRequestCode(context: Context, email: String, phone: String): String {
        val fp = getDeviceFingerprint(context)
        val raw = "REQ-TALAL-$fp-$email-$phone"
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.uppercase()
        return "TLPRO-${hash.substring(0, 4)}-${hash.substring(4, 8)}-${hash.substring(8, 12)}"
    }

    fun generateActivationCode(requestCode: String, secretSalt: String = "TALAL_PRO_SECURITY_SALT_2026"): String {
        val cleanReq = requestCode.replace("-", "").trim().uppercase()
        val raw = "$cleanReq-$secretSalt-AUTHORIZED"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.uppercase()
        return "ACT-${hash.substring(0, 4)}-${hash.substring(4, 8)}-${hash.substring(8, 12)}-${hash.substring(12, 16)}"
    }

    fun verifyActivationCode(requestCode: String, inputCode: String): Boolean {
        val cleanInput = inputCode.trim().uppercase()
        if (cleanInput.isBlank()) return false
        val expected = generateActivationCode(requestCode)
        // Also support admin master bypass if needed
        return cleanInput == expected || cleanInput == "TALAL-MASTER-PRO-2026"
    }
}

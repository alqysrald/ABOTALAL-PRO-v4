package com.example.sound

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundEffectManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            Log.e("SoundEffectManager", "ToneGenerator init failed", e)
        }
    }

    fun playBeep() {
        playSuccessBeep()
    }

    fun playCriticalAlertBeep() {
        playAlertSound()
    }

    fun playSuccessBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            vibrate(50)
        } catch (_: Exception) {
            playSystemSound()
        }
    }

    fun playAlertSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 400)
            vibrate(200)
        } catch (_: Exception) {
            playSystemSound()
        }
    }

    fun playErrorSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
            vibrate(300)
        } catch (_: Exception) {
            playSystemSound()
        }
    }

    fun testSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            vibrate(100)
        } catch (_: Exception) {
            playSystemSound()
        }
    }

    private fun playSystemSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r?.play()
        } catch (_: Exception) {}
    }

    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}

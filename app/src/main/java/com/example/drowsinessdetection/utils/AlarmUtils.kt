package com.example.drowsinessdetection.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AlarmUtils {

    private var toneGenerator: ToneGenerator? = null
    private var lastToneTime = 0L
    private var lastVibrateTime = 0L

    private const val TONE_COOLDOWN_MS = 2000L
    private const val TONE_DURATION_MS = 300
    private const val VIBRATE_COOLDOWN_MS = 3000L
    private val VIBRATE_PATTERN_MS = longArrayOf(0L, 300L, 120L, 300L, 120L, 300L)

    fun tickDrowsyAlarm(context: Context) {
        playAlarm()
        vibrateAlarm(context)
    }

    private fun playAlarm() {
        val now = System.currentTimeMillis()
        if (now - lastToneTime < TONE_COOLDOWN_MS) return

        lastToneTime = now

        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            }
            toneGenerator?.startTone(
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                TONE_DURATION_MS
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAlarm() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            toneGenerator = null
            lastToneTime = 0L
            lastVibrateTime = 0L
        }
    }

    private fun vibrateAlarm(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastVibrateTime < VIBRATE_COOLDOWN_MS) return

        lastVibrateTime = now
        vibrate(context)
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                if (!vibrator.hasVibrator()) return

                vibrator.vibrate(createVibrationEffect())
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (!vibrator.hasVibrator()) return

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(createVibrationEffect())
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(VIBRATE_PATTERN_MS, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createVibrationEffect(): VibrationEffect {
        return VibrationEffect.createWaveform(VIBRATE_PATTERN_MS, -1)
    }
}

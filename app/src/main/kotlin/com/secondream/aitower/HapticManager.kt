package com.secondream.aitower

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

class HapticManager(private val context: Context) {
    var enabled: Boolean = true
    private val vibrator: Vibrator? = context.getSystemService()

    fun light() {
        if (!enabled) return
        vibrate(18)
    }

    fun perfect() {
        if (!enabled) return
        vibrate(30)
    }

    fun gameOver() {
        if (!enabled) return
        try {
            val timings = longArrayOf(0, 90, 60, 120)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

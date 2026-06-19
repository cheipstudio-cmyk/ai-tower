package com.secondream.aitower

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class AudioManager(private val context: Context) {
    var enabled: Boolean = true
    private var placeSound: Int = 0
    private var perfectSound: Int = 0
    private var overSound: Int = 0

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    init {
        try {
            placeSound = soundPool.load(context, R.raw.tap, 1)
            perfectSound = soundPool.load(context, R.raw.upgrade, 1)
            overSound = soundPool.load(context, R.raw.breakthrough, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPlace() {
        if (!enabled) return
        try { soundPool.play(placeSound, 0.7f, 0.7f, 1, 0, 1f) } catch (e: Exception) { e.printStackTrace() }
    }

    fun playPerfect(rate: Float = 1f) {
        if (!enabled) return
        try { soundPool.play(perfectSound, 0.9f, 0.9f, 1, 0, rate) } catch (e: Exception) { e.printStackTrace() }
    }

    fun playGameOver() {
        if (!enabled) return
        try { soundPool.play(overSound, 1f, 1f, 2, 0, 1f) } catch (e: Exception) { e.printStackTrace() }
    }

    fun release() {
        soundPool.release()
    }
}

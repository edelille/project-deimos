package com.example.floatingd20

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool
    private var rollSoundId: Int = 0
    private var volume: Float = 1.0f
    private var isEnabled: Boolean = true

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build()
        } else {
            SoundPool(1, android.media.AudioManager.STREAM_MUSIC, 0)
        }

        // Load the roll sound
        rollSoundId = soundPool.load(context, R.raw.dice_roll, 1)
    }

    fun playRollSound() {
        if (isEnabled) {
            soundPool.play(rollSoundId, volume, volume, 1, 0, 1.0f)
        }
    }

    fun setVolume(volume: Float) {
        this.volume = volume
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
    }

    fun release() {
        soundPool.release()
    }
} 
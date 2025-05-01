package com.example.floatingd20

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class OptionsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var colorPreview: View
    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar
    private lateinit var soundSwitch: Switch
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var adView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("D20Settings", MODE_PRIVATE)

        // Initialize UI elements
        colorPreview = findViewById(R.id.colorPreview)
        redSeekBar = findViewById(R.id.redSeekBar)
        greenSeekBar = findViewById(R.id.greenSeekBar)
        blueSeekBar = findViewById(R.id.blueSeekBar)
        soundSwitch = findViewById(R.id.soundSwitch)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        adView = findViewById(R.id.adView)

        // Load saved settings
        loadSettings()

        // Set up listeners
        setupColorSeekBars()
        setupSoundControls()

        // Initialize ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun loadSettings() {
        // Load dice color
        val red = sharedPreferences.getInt("dice_red", 255)
        val green = sharedPreferences.getInt("dice_green", 255)
        val blue = sharedPreferences.getInt("dice_blue", 255)
        redSeekBar.progress = red
        greenSeekBar.progress = green
        blueSeekBar.progress = blue
        updateColorPreview()

        // Load sound settings
        soundSwitch.isChecked = sharedPreferences.getBoolean("sound_enabled", true)
        volumeSeekBar.progress = sharedPreferences.getInt("sound_volume", 100)
    }

    private fun setupColorSeekBars() {
        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateColorPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                saveColorSettings()
            }
        }

        redSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        greenSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        blueSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
    }

    private fun setupSoundControls() {
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                sharedPreferences.edit().putInt("sound_volume", seekBar.progress).apply()
            }
        })
    }

    private fun updateColorPreview() {
        val color = android.graphics.Color.rgb(
            redSeekBar.progress,
            greenSeekBar.progress,
            blueSeekBar.progress
        )
        colorPreview.setBackgroundColor(color)
    }

    private fun saveColorSettings() {
        sharedPreferences.edit()
            .putInt("dice_red", redSeekBar.progress)
            .putInt("dice_green", greenSeekBar.progress)
            .putInt("dice_blue", blueSeekBar.progress)
            .apply()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
    }
} 
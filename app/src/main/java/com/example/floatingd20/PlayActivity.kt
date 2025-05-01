package com.example.floatingd20

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class PlayActivity : AppCompatActivity() {
    private lateinit var d20SurfaceView: D20SurfaceView
    private lateinit var resultText: TextView
    private lateinit var adView: AdView
    private lateinit var soundManager: SoundManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rollButton: Button

    // Variables for touch rotation
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("D20Settings", MODE_PRIVATE)

        // Initialize SoundManager
        soundManager = SoundManager(this)
        soundManager.setEnabled(sharedPreferences.getBoolean("sound_enabled", true))
        soundManager.setVolume(sharedPreferences.getInt("sound_volume", 100) / 100f)

        // Initialize D20SurfaceView
        d20SurfaceView = findViewById(R.id.surfaceView)

        // Initialize UI elements
        resultText = findViewById(R.id.resultText)
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        
        // Set up Roll button
        rollButton = findViewById(R.id.rollButton)
        rollButton.setOnClickListener {
            // Start a 5-second roll animation
            d20SurfaceView.rollWithDuration(5.0f)
            
            // Play a sound effect
            soundManager.playRollSound()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
                // Signal the start of a touch sequence
                d20SurfaceView.rotateByFinger(0f, 0f, isNewTouch = true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                val dy = event.y - previousY

                // Pass touch delta to SurfaceView for rotation
                d20SurfaceView.rotateByFinger(dx, dy)

                previousX = event.x
                previousY = event.y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Signal the end of touch to apply momentum
                d20SurfaceView.rotateByFinger(0f, 0f, isEndTouch = true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        d20SurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        d20SurfaceView.onResume()
    }
} 
package com.example.floatingd20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class D20Activity : AppCompatActivity() {
    private lateinit var d20SurfaceView: D20SurfaceView
    private lateinit var rollButton: Button
    private lateinit var debugAngleTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateDebugDisplay()
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_d20)

        d20SurfaceView = findViewById(R.id.d20SurfaceView)
        rollButton = findViewById(R.id.rollButton)
        debugAngleTextView = findViewById(R.id.debugAngleTextView)

        rollButton.setOnClickListener {
            d20SurfaceView.startSequentialRoll()
        }
    }

    private fun updateDebugDisplay() {
        val angles = d20SurfaceView.getCurrentEulerAngles()
        val topFace = d20SurfaceView.getCurrentTopFace()
        debugAngleTextView.text = String.format(Locale.US, 
            "Top:%d Angles: P:%.1f Y:%.1f R:%.1f", 
            topFace, angles[0], angles[1], angles[2])
    }

    override fun onResume() {
        super.onResume()
        d20SurfaceView.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        d20SurfaceView.onPause()
        handler.removeCallbacks(updateRunnable)
    }
} 
package com.example.floatingd20

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {
    private lateinit var adView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AdMob
        MobileAds.initialize(this)

        // Initialize banner ad
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Set up button click listeners
        findViewById<Button>(R.id.playButton).setOnClickListener {
            startActivity(Intent(this, PlayActivity::class.java))
        }

        findViewById<Button>(R.id.optionsButton).setOnClickListener {
            startActivity(Intent(this, OptionsActivity::class.java))
        }

        findViewById<Button>(R.id.creditsButton).setOnClickListener {
            startActivity(Intent(this, CreditsActivity::class.java))
        }

        findViewById<Button>(R.id.exitButton).setOnClickListener {
            finish()
        }
    }

    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }
} 
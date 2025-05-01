package com.example.floatingd20

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.time.LocalDate
import java.time.Period

class CreditsActivity : AppCompatActivity() {
    private lateinit var adView: AdView
    private lateinit var timeSinceInceptionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)

        // Initialize UI elements
        timeSinceInceptionText = findViewById(R.id.timeSinceInceptionText)
        adView = findViewById(R.id.adView)

        // Calculate and display time since inception
        val inceptionDate = LocalDate.of(2025, 4, 25)
        val currentDate = LocalDate.now()
        val period = Period.between(inceptionDate, currentDate)
        
        val timeSinceInception = String.format(
            "%d years, %d months, %d days",
            period.years,
            period.months,
            period.days
        )
        
        timeSinceInceptionText.text = getString(R.string.time_since_inception, timeSinceInception)

        // Initialize ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
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
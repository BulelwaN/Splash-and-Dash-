package com.example.splashanddash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout

class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------------------------------------------------------
        // SPLASH SCREEN
        // ---------------------------------------------------------

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            setBackgroundColor(
                getColor(R.color.navy)
            )

            setPadding(
                dp(30),
                dp(30),
                dp(30),
                dp(30)
            )
        }

        // ---------------------------------------------------------
        // LOGO
        // ---------------------------------------------------------

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.splash_dash_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        layout.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(190),
                dp(190)
            )
        )

        // ---------------------------------------------------------
        // TITLE
        // ---------------------------------------------------------

        val title = text(
            "Splash & Dash",
            32f,
            true
        ).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        layout.addView(title)

        // ---------------------------------------------------------
        // SUBTITLE
        // ---------------------------------------------------------

        val subtitle = text(
            "Professional Vehicle Cleaning Services",
            15f
        ).apply {
            gravity = Gravity.CENTER
            setTextColor(
                Color.rgb(
                    216,
                    237,
                    249
                )
            )
        }

        layout.addView(subtitle)

        setContentView(layout)

        // ---------------------------------------------------------
        // OPEN MAIN SCREEN
        // ---------------------------------------------------------

        Handler(Looper.getMainLooper()).postDelayed({

            startActivity(
                Intent(
                    this@SplashActivity,
                    MainActivity::class.java
                )
            )

            finish()

        }, 1200)
    }
}
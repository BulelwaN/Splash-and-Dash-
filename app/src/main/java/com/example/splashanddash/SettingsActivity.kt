package com.example.splashanddash

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = createScreen()

        setContentView(root)
    }

    override fun createScreen(): LinearLayout {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.background))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        // Header
        val header = TextView(this).apply {
            text = "Settings"
            textSize = 28f
            setTextColor(getColor(R.color.text))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this).apply {
            text = "Manage your SplashDash preferences"
            textSize = 15f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(6), 0, dp(20))
        }

        root.addView(subtitle)

        // Appearance section
        root.addView(
            sectionTitle("Appearance")
        )

        val darkModeSwitch = SwitchMaterial(this).apply {
            text = "Dark Mode"
            textSize = 16f
            setTextColor(getColor(R.color.text))

            isChecked =
                AppCompatDelegate.getDefaultNightMode() ==
                        AppCompatDelegate.MODE_NIGHT_YES

            setOnCheckedChangeListener { _, enabled ->

                val mode =
                    if (enabled) {
                        AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }

                getSharedPreferences(
                    "SplashDashSettings",
                    MODE_PRIVATE
                )
                    .edit()
                    .putInt("theme", mode)
                    .apply()

                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        root.addView(
            darkModeSwitch,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(20))
            }
        )

        // Notifications section
        root.addView(
            sectionTitle("Notifications")
        )

        val notificationSwitch = SwitchMaterial(this).apply {
            text = "Enable Notifications"
            textSize = 16f
            setTextColor(getColor(R.color.text))

            isChecked = getSharedPreferences(
                "SplashDashSettings",
                MODE_PRIVATE
            ).getBoolean(
                "notifications",
                true
            )

            setOnCheckedChangeListener { _, enabled ->

                getSharedPreferences(
                    "SplashDashSettings",
                    MODE_PRIVATE
                )
                    .edit()
                    .putBoolean("notifications", enabled)
                    .apply()
            }
        }

        root.addView(
            notificationSwitch,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(20))
            }
        )

        // Information section
        root.addView(
            sectionTitle("About")
        )

        val aboutCard = cardView(
            title = "SplashDash Car Wash",
            description = "Car wash management system\nVersion 1.0"
        )

        root.addView(
            aboutCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(20))
            }
        )

        // Reset button
        val resetButton = primaryButton("Reset Preferences")

        resetButton.setOnClickListener {

            getSharedPreferences(
                "SplashDashSettings",
                MODE_PRIVATE
            )
                .edit()
                .clear()
                .apply()

            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        root.addView(resetButton)

        return root
    }

    override fun sectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(getColor(R.color.bright_blue))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(10))
        }
    }

    private fun cardView(
        title: String,
        description: String
    ): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(16)
            )
            setBackgroundResource(R.drawable.rounded_card)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(getColor(R.color.text))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val descriptionView = TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(5), 0, 0)
        }

        card.addView(titleView)
        card.addView(descriptionView)

        return card
    }
}
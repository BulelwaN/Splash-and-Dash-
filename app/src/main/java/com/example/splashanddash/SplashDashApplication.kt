package com.example.splashanddash

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SplashDashApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val preferences = getSharedPreferences(
            "SplashDashSettings",
            MODE_PRIVATE
        )

        val savedTheme = preferences.getInt(
            "theme",
            AppCompatDelegate.MODE_NIGHT_NO
        )

        AppCompatDelegate.setDefaultNightMode(
            savedTheme
        )
    }
}
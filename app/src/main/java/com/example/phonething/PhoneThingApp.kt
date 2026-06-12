package com.example.phonething

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PhoneThingApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("phone_thing_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", true)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}

package com.example.stars1

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("StarfieldSettings", Context.MODE_PRIVATE)

    fun getFlightTime(): Float {
        return prefs.getFloat("flight_time", 5f)
    }

    fun setFlightTime(value: Float) {
        prefs.edit().putFloat("flight_time", value).apply()
    }

    fun getCreationInterval(): Float {
        return prefs.getFloat("creation_interval", 1f)
    }

    fun setCreationInterval(value: Float) {
        prefs.edit().putFloat("creation_interval", value).apply()
    }
}

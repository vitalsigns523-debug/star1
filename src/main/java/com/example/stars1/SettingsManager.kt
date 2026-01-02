package com.example.stars1

import android.content.Context
import android.content.SharedPreferences

enum class RollMode { IGNORE, INVARIANT, AILERON }
enum class PitchMode { IGNORE, TILT_VIEW, ELEVATOR }
enum class YawMode { IGNORE, PAN_VIEW, RUDDER }

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("StarfieldSettings", Context.MODE_PRIVATE)

    fun getFlightTime(): Float = prefs.getFloat("flight_time", 5f)
    fun setFlightTime(value: Float) = prefs.edit().putFloat("flight_time", value).apply()

    fun getCreationInterval(): Float = prefs.getFloat("creation_interval", 1f)
    fun setCreationInterval(value: Float) = prefs.edit().putFloat("creation_interval", value).apply()

    fun getRollMode(): RollMode = RollMode.values()[prefs.getInt("roll_mode", RollMode.INVARIANT.ordinal)]
    fun setRollMode(value: RollMode) = prefs.edit().putInt("roll_mode", value.ordinal).apply()

    fun getPitchMode(): PitchMode = PitchMode.values()[prefs.getInt("pitch_mode", PitchMode.TILT_VIEW.ordinal)]
    fun setPitchMode(value: PitchMode) = prefs.edit().putInt("pitch_mode", value.ordinal).apply()

    fun getYawMode(): YawMode = YawMode.values()[prefs.getInt("yaw_mode", YawMode.PAN_VIEW.ordinal)]
    fun setYawMode(value: YawMode) = prefs.edit().putInt("yaw_mode", value.ordinal).apply()
}

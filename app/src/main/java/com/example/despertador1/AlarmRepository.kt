package com.example.despertador1

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("AlarmAppPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmListKey = "alarmList"

    fun loadAlarms(): MutableList<AlarmItem> {
        val json = sharedPreferences.getString(alarmListKey, null)
        if (json.isNullOrEmpty()) {
            return mutableListOf()
        }
        // TypeToken le dice a Gson cómo convertir el texto JSON de nuevo a una lista de AlarmItem
        // No hay que hacer conversión manual de Uri aquí porque ya se guarda como String.
        val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAlarms(alarms: List<AlarmItem>) {
        // No hay que hacer conversión manual de Uri aquí porque ya la guardamos como String.
        val json = gson.toJson(alarms)
        sharedPreferences.edit().putString(alarmListKey, json).apply()
    }
}

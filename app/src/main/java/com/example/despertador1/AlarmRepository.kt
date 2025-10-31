package com.example.despertador1

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmRepository(context: Context) {

    // Se crea o accede a un archivo de preferencias llamado "AlarmAppPrefs"
    private val sharedPreferences = context.getSharedPreferences("AlarmAppPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmListKey = "alarmList"

    /**
     * Carga la lista de alarmas guardada en formato JSON desde SharedPreferences.
     * @return Una lista mutable de AlarmItem. Si no hay nada guardado, devuelve una lista vacía.
     */
    fun loadAlarms(): MutableList<AlarmItem> {
        val json = sharedPreferences.getString(alarmListKey, null)
        if (json.isNullOrEmpty()) {
            return mutableListOf()
        }
        // TypeToken le dice a Gson cómo convertir el texto JSON de nuevo a una lista de objetos complejos.
        val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Guarda la lista completa de alarmas en SharedPreferences, convirtiéndola a formato JSON.
     * @param alarms La lista de alarmas que se va a guardar.
     */
    fun saveAlarms(alarms: List<AlarmItem>) {
        val json = gson.toJson(alarms) // Convierte la lista de objetos a un único string de texto en formato JSON.
        sharedPreferences.edit().putString(alarmListKey, json).apply() // Guarda el string en el archivo de preferencias.
    }
}

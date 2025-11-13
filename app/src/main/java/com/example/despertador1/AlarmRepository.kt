package com.example.despertador1

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmRepository(context: Context) {

    private val prefsName = "AlarmAppPreferences"
    private val alarmsKey = "saved_alarms"
    // --- NOVEDAD: Clave para guardar la última carpeta ---
    private val lastFolderUriKey = "last_folder_uri"

    private val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAlarms(alarms: List<AlarmItem>) {
        val jsonString = gson.toJson(alarms)
        sharedPreferences.edit().putString(alarmsKey, jsonString).apply()
    }

    fun loadAlarms(): List<AlarmItem> {
        val jsonString = sharedPreferences.getString(alarmsKey, null)
        if (jsonString != null) {
            val type = object : TypeToken<List<AlarmItem>>() {}.type
            return gson.fromJson(jsonString, type)
        }
        return emptyList()
    }

    // --- NOVEDAD: Funciones para guardar y cargar la carpeta ---
    /**
     * Guarda la URI de la última carpeta seleccionada.
     */
    fun saveLastFolderUri(uri: Uri?) {
        sharedPreferences.edit().putString(lastFolderUriKey, uri?.toString()).apply()
    }

    /**
     * Carga la URI de la última carpeta guardada.
     */
    fun loadLastFolderUri(): Uri? {
        val uriString = sharedPreferences.getString(lastFolderUriKey, null)
        return if (uriString != null) Uri.parse(uriString) else null
    }
}

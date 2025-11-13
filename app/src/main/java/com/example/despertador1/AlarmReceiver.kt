package com.example.despertador1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmApp", "¡Alarma recibida por AlarmReceiver!")

        val alarmId = intent.getIntExtra("alarm_id", -1)
        val hour = intent.getIntExtra("alarm_hour", -1)
        val minute = intent.getIntExtra("alarm_minute", -1)
        val folderUri = intent.getStringExtra("alarm_folder_uri")
        val daysArray = intent.getIntArrayExtra("alarm_days")
        val days = daysArray?.toSet()
        val isActive = intent.getBooleanExtra("alarm_is_active", true)
        // --- NOVEDAD: Extraemos el volumen ---
        val volume = intent.getFloatExtra("alarm_volume", 1.0f)

        if (alarmId == -1 || folderUri == null || days == null) {
            Log.e("AlarmApp", "Datos de alarma incompletos en el receiver. No se puede continuar.")
            return
        }

        // 1. Iniciar el servicio de música, pasando el volumen.
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("folder_uri", folderUri)
            putExtra("alarm_volume", volume) // Añadimos el volumen al servicio
        }
        context.startForegroundService(serviceIntent)

        // 2. Reprogramar la alarma si es repetitiva.
        if (days.isNotEmpty()) {
            val alarmToReschedule = AlarmItem(alarmId, hour, minute, folderUri, days, isActive, volume)
            setAlarm(context, alarmToReschedule)
            Log.d("AlarmApp", "Alarma (ID: $alarmId) reprogramada para la siguiente ocurrencia.")
        } else {
            // Si la alarma era de "una sola vez", la desactivamos en los datos guardados.
            val alarmRepository = AlarmRepository(context)
            val alarms = alarmRepository.loadAlarms().toMutableList()
            val alarmIndex = alarms.indexOfFirst { it.id == alarmId }
            if (alarmIndex != -1) {
                alarms[alarmIndex] = alarms[alarmIndex].copy(isActive = false)
                alarmRepository.saveAlarms(alarms)
                Log.d("AlarmApp", "Alarma de una sola vez (ID: $alarmId) desactivada.")
            }
        }
    }
}

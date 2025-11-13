package com.example.despertador1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d("AlarmApp", "¡Teléfono reiniciado! Reprogramando alarmas...")

            val alarmRepository = AlarmRepository(context)
            val savedAlarms = alarmRepository.loadAlarms()

            if (savedAlarms.isEmpty()) {
                Log.d("AlarmApp", "No hay alarmas guardadas para reprogramar.")
                return
            }

            for (alarm in savedAlarms) {
                setAlarm(context, alarm)
                Log.d(
                    "AlarmApp",
                    "Alarma reprogramada para las ${
                        String.format(
                            "%02d:%02d",
                            alarm.hour,
                            alarm.minute
                        )
                    }"
                )
            }

            Log.d("AlarmApp", "Se han reprogramado ${savedAlarms.size} alarmas.")
        }
    }
}
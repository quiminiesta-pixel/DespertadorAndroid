package com.example.despertador1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")
        val folderUri = intent.getStringExtra("folder_uri")
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("folder_uri", folderUri)
        }
        context.startService(serviceIntent)
    }
}

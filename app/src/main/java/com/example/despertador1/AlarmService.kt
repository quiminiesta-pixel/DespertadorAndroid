package com.example.despertador1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile


class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var musicFiles: List<DocumentFile>? = null

    companion object {
        const val ACTION_STOP = "com.example.despertador1.STOP_ALARM"
        const val NOTIFICATION_CHANNEL_ID = "ALARM_SERVICE_CHANNEL"
        const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.d("AlarmService", "Stopping alarm.")
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("AlarmService", "Alarm service started")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val folderUriString = intent?.getStringExtra("folder_uri")
        if (folderUriString != null) {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(this, folderUri)
            if (folder != null && folder.isDirectory) {
                musicFiles = folder.listFiles().filter { it.name?.endsWith(".mp3") == true }
                playRandomSong()
            } else {
                Log.d("AlarmService", "Music folder not found or is not a directory")
                stopSelf()
            }
        } else {
            Log.d("AlarmService", "No folder URI provided")
            stopSelf()
        }

        return START_STICKY
    }

    private fun playRandomSong() {
        if (musicFiles.isNullOrEmpty()) {
            Log.d("AlarmService", "No mp3 files found.")
            stopSelf()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                val randomFile = musicFiles!!.random()
                setDataSource(applicationContext, randomFile.uri)
                prepare()
                start()
                setOnCompletionListener { playRandomSong() }
            } catch (e: Exception) {
                Log.e("AlarmService", "Error playing music", e)
                stopSelf()
            }
        }
    }

    private fun createNotification(): android.app.Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alarm")
            .setContentText("Alarm is ringing")
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your own icon
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d("AlarmService", "Alarm service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

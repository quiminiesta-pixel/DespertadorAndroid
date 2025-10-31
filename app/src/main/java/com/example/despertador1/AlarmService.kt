package com.example.despertador1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
            stopSelf() // Llama a onDestroy
            return START_NOT_STICKY
        }

        val folderUriString = intent?.getStringExtra("folder_uri")
        if (folderUriString != null) {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(this, folderUri)

            if (folder != null && folder.isDirectory) {
                musicFiles = folder.listFiles().filter { it.isFile && it.name?.endsWith(".mp3", true) == true }
                if (!musicFiles.isNullOrEmpty()) {
                    playRandomSong()
                } else {
                    Log.d("AlarmService", "No se encontraron archivos .mp3 en la carpeta.")
                    stopSelf()
                }
            } else {
                Log.d("AlarmService", "La carpeta seleccionada no es válida o no existe.")
                stopSelf()
            }
        } else {
            Log.d("AlarmService", "No se proporcionó URI de carpeta.")
            stopSelf()
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    // --- LÓGICA DE REPRODUCCIÓN MEJORADA ---
    private fun playRandomSong() {
        if (musicFiles.isNullOrEmpty()) {
            Log.d("AlarmService", "No hay canciones para reproducir.")
            stopSelf()
            return
        }

        try {
            // Liberamos el reproductor anterior si existe
            mediaPlayer?.release()

            val randomFile = musicFiles!!.random()
            Log.d("AlarmService", "Reproduciendo: ${randomFile.name}")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, randomFile.uri)
                prepare() // Preparamos la canción
                start() // La reproducimos

                // --- ¡ESTA ES LA CLAVE! ---
                // Cuando una canción termina (OnCompletion), llamamos a playRandomSong() otra vez.
                setOnCompletionListener {
                    Log.d("AlarmService", "Canción terminada. Reproduciendo la siguiente.")
                    playRandomSong()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error al reproducir música", e)
            stopSelf() // Si hay un error, paramos el servicio.
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d("AlarmService", "Servicio de alarma destruido.")
    }

    private fun createNotification(): android.app.Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alarma sonando")
            .setContentText("Toca para detener")
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(R.drawable.ic_launcher_foreground, "DETENER", stopPendingIntent)
            .build()
    }

    // El resto del servicio no necesita cambios
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.despertador1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var musicFiles = mutableListOf<Uri>()
    private var currentTrackIndex = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "ALARM_SERVICE_CHANNEL"
        const val ACTION_STOP = "com.example.despertador1.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        if (!requestAudioFocus()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmApp:AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)

        createNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(this, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val stopIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val stopAction = NotificationCompat.Action.Builder(R.drawable.ic_launcher_foreground, "Detener", stopPendingIntent).build()

        // --- LÓGICA DE NOTIFICACIÓN ÚNICA Y SIMPLIFICADA (SIN PANTALLA COMPLETA) ---
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alarma Activa")
            .setContentText("Tu alarma está sonando")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(null)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            // --- ¡¡¡LÍNEA ELIMINADA!!! Ya no se abre la app a pantalla completa ---
            // .setFullScreenIntent(openAppPendingIntent, true)
            .addAction(stopAction)
            .build()
        // ----------------------------------------------------------------------

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val volume = intent?.getFloatExtra("alarm_volume", 1.0f) ?: 1.0f
        val folderUriString = intent?.getStringExtra("folder_uri")
        if (folderUriString != null) {
            val folderUri = Uri.parse(folderUriString)
            prepareAndPlayMusic(folderUri, volume)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    // --- (El resto de funciones no cambia) ---

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private fun requestAudioFocus(): Boolean {
        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            result = audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            result = audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Alarmas Activas", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones que se muestran cuando una alarma está sonando."
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    private fun prepareAndPlayMusic(folderUri: Uri, volume: Float) {
        val documentFolder = DocumentFile.fromTreeUri(this, folderUri)
        if (documentFolder == null || !documentFolder.isDirectory) {
            stopAlarm()
            return
        }
        musicFiles.clear()
        findMusicFilesRecursively(documentFolder, musicFiles)
        if (musicFiles.isEmpty()) {
            stopAlarm()
            return
        }
        musicFiles.shuffle()
        playNextTrack(volume)
    }
    private fun findMusicFilesRecursively(folder: DocumentFile, fileList: MutableList<Uri>) {
        for (file in folder.listFiles()) {
            if (file.isDirectory) findMusicFilesRecursively(file, fileList)
            else if (file.isFile && (file.type?.startsWith("audio/") == true || file.name?.endsWith(".mp3") == true)) fileList.add(file.uri)
        }
    }
    private fun playNextTrack(volume: Float) {
        if (currentTrackIndex >= musicFiles.size) {
            stopAlarm()
            return
        }
        mediaPlayer?.release()
        mediaPlayer = null
        val trackUri = musicFiles[currentTrackIndex]
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val newVolume = (volume * maxVolume).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, trackUri)
                setOnCompletionListener {
                    currentTrackIndex++
                    playNextTrack(volume)
                }
                setOnErrorListener { _, _, _ ->
                    currentTrackIndex++
                    playNextTrack(volume)
                    true
                }
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
        } catch (e: Exception) {
            currentTrackIndex++
            playNextTrack(volume)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

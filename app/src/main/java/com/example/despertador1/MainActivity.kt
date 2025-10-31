package com.example.despertador1

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.despertador1.ui.theme.Despertador1Theme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Despertador1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreen()
                }
            }
        }
    }
}

@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    var folderUri by remember { mutableStateOf<Uri?>(null) }
    var folderName by remember { mutableStateOf("Ninguna carpeta seleccionada") }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            folderUri = uri
            // Persistir el permiso para acceder al contenido del URI en reinicios del dispositivo
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            folderName = uri.path ?: "Carpeta seleccionada"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Despertador con Música")

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = folderName)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            openDocumentTreeLauncher.launch(null)
        }) {
            Text("Seleccionar Carpeta de Música")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                folderUri?.let { uri ->
                    setAlarm(context, uri)
                }
            },
            enabled = folderUri != null // El botón solo se activa si se ha seleccionado una carpeta
        ) {
            Text("Programar Alarma (8:00 AM)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            cancelAlarm(context)
        }) {
            Text("Cancelar Alarma")
        }
    }
}

fun setAlarm(context: Context, folderUri: Uri) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val alarmIntent = Intent(context, AlarmService::class.java).apply {
        putExtra("folder_uri", folderUri.toString())
    }

    val pendingIntent = PendingIntent.getService(
        context,
        0,
        alarmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Configurar la alarma para las 8:00 AM
    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    // Si la hora ya pasó hoy, programarla para mañana
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Usar setExactAndAllowWhileIdle para mayor precisión en versiones modernas de Android
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
    )
}

fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmService::class.java)
    val pendingIntent = PendingIntent.getService(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)

    // También detener el servicio si está en ejecución
    val stopServiceIntent = Intent(context, AlarmService::class.java).apply {
        action = AlarmService.ACTION_STOP
    }
    context.startService(stopServiceIntent)
}

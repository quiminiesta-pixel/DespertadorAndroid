package com.example.despertador1

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log // <-- IMPORTACIÓN AÑADIDA
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // <-- IMPORTACIÓN AÑADIDA
import androidx.compose.runtime.setValue // <-- IMPORTACIÓN AÑADIDA
import androidx.compose.runtime.mutableStateListOf // <-- IMPORTACIÓN AÑADIDA
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despertador1.ui.theme.Despertador1Theme
import java.util.*

// --- NUEVA DATA CLASS para representar una alarma ---
data class AlarmItem(
    val id: Int, // Identificador único para cada alarma
    val hour: Int,
    val minute: Int,
    val folderUri: Uri
)

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

    val alarmList = remember { mutableStateListOf<AlarmItem>() }

    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    var folderUri by remember { mutableStateOf<Uri?>(null) }
    var folderName by remember { mutableStateOf("Ninguna carpeta seleccionada") }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
        }, selectedHour, selectedMinute, true
    )

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            folderUri = uri
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                folderName = uri.path?.substringAfterLast(':') ?: "Carpeta seleccionada"
            } catch (e: SecurityException) { // <-- BLOQUE TRY-CATCH MEJORADO
                Log.e("AlarmApp", "Error de seguridad al persistir el permiso del URI", e)
                folderName = "Error de permiso al seleccionar"
                Toast.makeText(context, "No se pudo obtener permiso para la carpeta.", Toast.LENGTH_LONG).show()
            } catch (e: IllegalArgumentException) {
                Log.e("AlarmApp", "Error en el argumento al persistir el permiso del URI", e)
                folderName = "Error al seleccionar la carpeta"
                Toast.makeText(context, "El tipo de carpeta seleccionado no es válido.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mis Alarmas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // --- SECCIÓN PARA AÑADIR UNA NUEVA ALARMA ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { timePickerDialog.show() }) {
                    Text("Cambiar Hora")
                }
                Spacer(Modifier.height(12.dp))
                Text(text = folderName, fontSize = 12.sp)
                Button(onClick = { openDocumentTreeLauncher.launch(null) }) {
                    Text("Seleccionar Carpeta")
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        folderUri?.let { uri ->
                            val newAlarmId = System.currentTimeMillis().toInt()
                            val newAlarm = AlarmItem(newAlarmId, selectedHour, selectedMinute, uri)

                            setAlarm(context, newAlarm)
                            alarmList.add(newAlarm)

                            Toast.makeText(context, "Alarma programada", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(context, "Selecciona una carpeta de música", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = folderUri != null
                ) {
                    Text("AÑADIR ALARMA")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider() // <-- ESTA CLASE NECESITABA IMPORTACIÓN

        // --- LISTA DE ALARMAS PROGRAMADAS (NUEVO) ---
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(alarmList, key = { it.id }) { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    onDelete = {
                        cancelAlarm(context, alarm.id)
                        alarmList.remove(alarm)
                        Toast.makeText(context, "Alarma eliminada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// --- NUEVO COMPOSABLE para mostrar un item en la lista de alarmas ---
@Composable
fun AlarmListItem(alarm: AlarmItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar Alarma")
            }
        }
    }
}


// --- FUNCIÓN SETALARM ACTUALIZADA para aceptar un AlarmItem ---
fun setAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val alarmIntent = Intent(context, AlarmService::class.java).apply {
        putExtra("folder_uri", alarm.folderUri.toString())
    }

    val pendingIntent = PendingIntent.getService(
        context,
        alarm.id,
        alarmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
    }

    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Toast.makeText(context, "Permiso para alarmas exactas no concedido. La alarma podría no ser precisa.", Toast.LENGTH_LONG).show()
        // Opcional: Redirigir al usuario a la configuración para que otorgue el permiso.
        // context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}

// --- FUNCIÓN CANCELALARM ACTUALIZADA para aceptar un ID de alarma ---
fun cancelAlarm(context: Context, alarmId: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmService::class.java)

    val pendingIntent = PendingIntent.getService(
        context,
        alarmId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    alarmManager.cancel(pendingIntent)
}

package com.example.despertador1

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.despertador1.ui.theme.Despertador1Theme
import java.text.SimpleDateFormat
import java.util.*

data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val folderUri: String,
    val days: Set<Int>,
    val isActive: Boolean = true,
    val volume: Float = 0.2f
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
                    PermissionHandler { AlarmScreen() }
                }
            }
        }
    }
}

// --- GESTOR DE PERMISOS ---
@Composable
fun PermissionHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var permissionsGranted by remember {
        mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

    if (permissionsGranted) {
        content()
    } else {
        PermissionScreen(
            permissionsToRequest = permissionsToRequest,
            onPermissionsGranted = { permissionsGranted = true }
        )
    }
}

@Composable
fun PermissionScreen(permissionsToRequest: List<String>, onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var currentPermissionIndex by remember { mutableStateOf(0) }
    var showRationaleDialog by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                currentPermissionIndex++
            } else {
                showRationaleDialog = permissionsToRequest[currentPermissionIndex]
            }
        }
    )

    LaunchedEffect(currentPermissionIndex) {
        if (currentPermissionIndex >= permissionsToRequest.size) {
            onPermissionsGranted()
        } else {
            val permission = permissionsToRequest[currentPermissionIndex]
            launcher.launch(permission)
        }
    }

    if (showRationaleDialog != null) {
        PermissionRationaleDialog(
            permission = showRationaleDialog!!,
            onRetry = {
                val permission = showRationaleDialog
                showRationaleDialog = null
                if (permission != null) launcher.launch(permission)
            },
            onGoToSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Se requieren permisos para continuar...")
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun PermissionRationaleDialog(permission: String, onRetry: () -> Unit, onGoToSettings: () -> Unit) {
    val permissionText = when (permission) {
        Manifest.permission.POST_NOTIFICATIONS -> "notificaciones. Son esenciales para avisarte cuando suena la alarma."
        Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE -> "acceso a los archivos de audio. Es necesario para reproducir la música de la carpeta que selecciones."
        else -> "un permiso requerido."
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Permiso Denegado") },
        text = { Text("La aplicación necesita permiso de $permissionText Para funcionar correctamente, por favor, concede el permiso.", textAlign = TextAlign.Center) },
        confirmButton = { Button(onClick = onGoToSettings) { Text("Ir a Ajustes") } },
        dismissButton = { Button(onClick = onRetry) { Text("Reintentar") } }
    )
}
// --- FIN DEL GESTOR DE PERMISOS ---


@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    val alarmRepository = remember { AlarmRepository(context) }
    val alarmList = remember { mutableStateListOf<AlarmItem>().apply { addAll(alarmRepository.loadAlarms()) } }

    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }

    var folderUri by remember { mutableStateOf(alarmRepository.loadLastFolderUri()) }
    var folderName by remember {
        val loadedUri = alarmRepository.loadLastFolderUri()
        mutableStateOf(getFolderNameFromUri(context, loadedUri) ?: "Ninguna carpeta seleccionada")
    }
    var selectedDays by remember {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        mutableStateOf(setOf(currentDay))
    }

    var selectedVolume by rememberSaveable { mutableStateOf(0.2f) }

    val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
        selectedHour = hourOfDay
        selectedMinute = minute
    }, selectedHour, selectedMinute, true)

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            folderUri = uri
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                alarmRepository.saveLastFolderUri(uri)
                folderName = getFolderNameFromUri(context, uri) ?: "Carpeta seleccionada"
            } catch (e: Exception) {
                Log.e("AlarmApp", "Error al obtener permiso persistente", e)
                Toast.makeText(context, "No se pudo obtener permiso para la carpeta.", Toast.LENGTH_SHORT).show()
                folderName = "Error de permiso"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mis Alarmas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { timePickerDialog.show() }) { Text("Cambiar Hora") }
                Spacer(Modifier.height(12.dp))
                Text(text = folderName, fontSize = 12.sp, textAlign = TextAlign.Center)
                Button(onClick = { openDocumentTreeLauncher.launch(null) }) { Text("Seleccionar Carpeta") }
                Spacer(Modifier.height(16.dp))
                Text("Repetir en:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                DaySelector(selectedDays = selectedDays, onDaySelected = { day, isSelected ->
                    selectedDays = if (isSelected) selectedDays + day else selectedDays - day
                })
                Spacer(Modifier.height(16.dp))
                Text("Volumen:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = selectedVolume,
                    onValueChange = { selectedVolume = it },
                    valueRange = 0f..1f,
                    steps = 9
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (folderUri == null) {
                            Toast.makeText(context, "Primero selecciona una carpeta de música", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newAlarmId = System.currentTimeMillis().toInt()
                        val newAlarm = AlarmItem(
                            id = newAlarmId,
                            hour = selectedHour,
                            minute = selectedMinute,
                            folderUri = folderUri.toString(),
                            days = selectedDays,
                            isActive = true,
                            volume = selectedVolume
                        )
                        setAlarm(context, newAlarm)
                        alarmList.add(newAlarm)
                        alarmRepository.saveAlarms(alarmList)

                        selectedDays = setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                        selectedVolume = 0.2f
                    },
                    enabled = folderUri != null
                ) { Text("AÑADIR ALARMA") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Divider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(alarmList, key = { it.id }) { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    onDelete = {
                        cancelAlarm(context, alarm)
                        alarmList.remove(alarm)
                        alarmRepository.saveAlarms(alarmList)
                        Toast.makeText(context, "Alarma eliminada", Toast.LENGTH_SHORT).show()
                    },
                    onToggle = { isActive ->
                        val index = alarmList.indexOf(alarm)
                        if (index != -1) {
                            val updatedAlarm = alarm.copy(isActive = isActive)
                            alarmList[index] = updatedAlarm
                            alarmRepository.saveAlarms(alarmList)

                            cancelAlarm(context, updatedAlarm)

                            if (isActive) {
                                setAlarm(context, updatedAlarm)
                            } else {
                                Toast.makeText(context, "Alarma desactivada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun getFolderNameFromUri(context: Context, uri: Uri?): String? {
    if (uri == null) return null
    return try {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val cursor = context.contentResolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                it.getString(0)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e("AlarmApp", "Error al obtener el nombre de la carpeta", e)
        uri.path?.substringAfterLast('/')
    }
}


// --- COMPONENTES DE UI Y FUNCIONES DE AYUDA ---
@Composable
fun DaySelector(selectedDays: Set<Int>, onDaySelected: (Int, Boolean) -> Unit) {
    val days = listOf(
        "D" to Calendar.SUNDAY, "L" to Calendar.MONDAY, "M" to Calendar.TUESDAY,
        "X" to Calendar.WEDNESDAY, "J" to Calendar.THURSDAY, "V" to Calendar.FRIDAY, "S" to Calendar.SATURDAY
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { (label, day) ->
            val isSelected = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onDaySelected(day, !isSelected) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AlarmListItem(alarm: AlarmItem, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    val textColor = if (alarm.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = formatSelectedDays(alarm.days),
                    fontSize = 14.sp,
                    color = if (alarm.isActive) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
            }
            Switch(
                checked = alarm.isActive,
                onCheckedChange = onToggle
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar Alarma", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun formatSelectedDays(days: Set<Int>): String {
    if (days.size == 7) return "Todos los días"
    if (days.isEmpty()) return "Solo una vez"
    val dayMapping = mapOf(
        Calendar.SUNDAY to "dom", Calendar.MONDAY to "lun", Calendar.TUESDAY to "mar",
        Calendar.WEDNESDAY to "mié", Calendar.THURSDAY to "jue", Calendar.FRIDAY to "vie", Calendar.SATURDAY to "sáb"
    )
    val sortedDays = days.sortedWith(compareBy { (it + 5) % 7 })
    return sortedDays.joinToString(", ") { dayMapping[it] ?: "" }
}

private fun createAlarmPendingIntent(context: Context, alarm: AlarmItem): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarm_id", alarm.id)
        putExtra("alarm_hour", alarm.hour)
        putExtra("alarm_minute", alarm.minute)
        putExtra("alarm_folder_uri", alarm.folderUri)
        putExtra("alarm_days", alarm.days.toIntArray())
        putExtra("alarm_is_active", alarm.isActive)
        putExtra("alarm_volume", alarm.volume)
    }
    return PendingIntent.getBroadcast(
        context, alarm.id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )
}

fun setAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Log.w("AlarmApp", "La app no tiene permiso para programar alarmas exactas.")
    }

    val pendingIntent = createAlarmPendingIntent(context, alarm)
    val nextAlarmTime = getNextAlarmTime(alarm.hour, alarm.minute, alarm.days)

    if (nextAlarmTime != null) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmTime.timeInMillis, pendingIntent)
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.getDefault())
            Log.d("AlarmApp", "Alarma (ID: ${alarm.id}) programada para: ${sdf.format(nextAlarmTime.time)}")
            Toast.makeText(context, "Alarma activada", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("AlarmApp", "Error de seguridad al programar la alarma.", e)
        }
    }
}

fun cancelAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = createAlarmPendingIntent(context, alarm)
    alarmManager.cancel(pendingIntent)
    Log.d("AlarmApp", "Alarma (ID: ${alarm.id}) cancelada.")
}

// --- VERSIÓN MEJORADA Y MÁS LIMPIA DE LA FUNCIÓN ---
fun getNextAlarmTime(hour: Int, minute: Int, days: Set<Int>): Calendar? {
    val now = Calendar.getInstance()

    // Si no se seleccionan días, es una alarma de "una sola vez".
    if (days.isEmpty()) {
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Si la hora ya pasó hoy, se programa para mañana.
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        return alarmTime
    }

    // Para alarmas repetitivas, buscamos el próximo día válido.
    var alarmTime = Calendar.getInstance()
    for (i in 0..7) { // Buscamos en los próximos 7 días
        alarmTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, i)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si el día calculado es un día seleccionado Y es en el futuro, lo devolvemos.
        if (days.contains(alarmTime.get(Calendar.DAY_OF_WEEK)) && alarmTime.after(now)) {
            return alarmTime
        }
    }

    return null // No se encontró una fecha válida (no debería ocurrir)
}


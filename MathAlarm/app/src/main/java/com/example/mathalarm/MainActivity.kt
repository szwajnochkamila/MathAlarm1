package com.example.mathalarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.mathalarm.data.MathPreferencesManager
import com.example.mathalarm.data.MathSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mathalarm.ui.theme.MathAlarmTheme
import com.example.mathalarm.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
// importy dla listy i bazy danych
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mathalarm.ui.alarms.AlarmsViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import com.example.mathalarm.data.local.entities.AlarmEntity
import dagger.hilt.android.AndroidEntryPoint
import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@AndroidEntryPoint // ADNOTACJA HILT: Rejestruje aktywnosc w grafie zaleznosci, umozliwiajac automatyczne wstrzykiwanie ViewModelu
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()
        setContent {
            MathAlarmTheme {
                MainScreen()
            }
        }
    }

    private fun ensureNotificationPermission() {
        // zgoda na powiadomienia
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
    }
}

@Composable
fun MainScreen(
    viewModel: AlarmsViewModel = hiltViewModel() // wprowadzenie ViewModela z bazą
) {
    var currentTab by remember { mutableStateOf("SetAlarm") }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DustyGrape) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Budziki") },
                    selected = currentTab == "Alarms",
                    onClick = { currentTab = "Alarms" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkAmethyst,
                        unselectedIconColor = White,
                        indicatorColor = Lilac
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Ustaw") },
                    selected = currentTab == "SetAlarm",
                    onClick = { currentTab = "SetAlarm" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkAmethyst,
                        unselectedIconColor = White,
                        indicatorColor = Lilac
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") },
                    selected = currentTab == "Settings",
                    onClick = { currentTab = "Settings" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkAmethyst,
                        unselectedIconColor = White,
                        indicatorColor = Lilac
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PinkOrchid),
            contentAlignment = Alignment.Center
        ) {
            when (currentTab) {
                "Alarms" -> AlarmsListScreen(viewModel) // wywolanie nowego ekranu listy
                "SetAlarm" -> AlarmSetupScreen(viewModel) // przekazanie ViewModel do ustawien
                "Settings" -> SettingsScreen()
            }
        }
    }
}

// ekran listy budzikow
@Composable
fun AlarmsListScreen(viewModel: AlarmsViewModel, modifier: Modifier = Modifier) {
    // obserwujemy liste budzikow z bazy danych
    val alarms by viewModel.alarmsState.collectAsState()

    if (alarms.isEmpty()) {
        Text("Brak zapisanych budzików", color = DarkAmethyst)
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onDelete = { viewModel.deleteAlarm(alarm) },
                    onToggle = { isChecked -> viewModel.toggleAlarmActive(alarm, isChecked) }
                )
            }
        }
    }
}

// WIDOK POJEDYNCZEGO BUDZIKA NA LISCIE
@Composable
fun AlarmItem(
    alarm: AlarmEntity,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeText = String.format("%02d:%02d", alarm.hour, alarm.minute)
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineMedium,
                color = if (alarm.isActive) DarkAmethyst else AmethystSmoke
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Lilac,
                        uncheckedThumbColor = DustyGrape,
                        uncheckedTrackColor = AmethystSmoke
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = DustyGrape)
                }
            }
        }
    }
}

// EKRAN USTAWIENIA BUDZIKA
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSetupScreen(viewModel: AlarmsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = 7,
        initialMinute = 0,
        is24Hour = true
    )
    var statusText by remember { mutableStateOf("Brak zaplanowanych alarmów") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MathAlarm",
            style = MaterialTheme.typography.headlineLarge,
            color = DarkAmethyst
        )
        Text(
            text = "Aby wyłączyć, rozwiąż zadanie",
            style = MaterialTheme.typography.bodyMedium,
            color = DustyGrape
        )
        TimePicker(state = timeState)
        Button(onClick = {
            if (!canScheduleExactAlarms(context)) {
                requestExactAlarmPermission(context)
                Toast.makeText(
                    context,
                    "Zezwól na dokładne alarmy i spróbuj ponownie",
                    Toast.LENGTH_LONG
                ).show()
                return@Button
            }

            viewModel.addAlarm(
                hour = timeState.hour,
                minute = timeState.minute,
                onSuccess = {
                    // budzik zapisany w bazie - nastawiamy fizyczny budzik w systemie
                    val triggerAt = AlarmScheduler.schedule(context, timeState.hour, timeState.minute)
                    val formatted = SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault())
                        .format(Date(triggerAt))
                    statusText = "Budzik ustawiony na: $formatted"
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                },
                onDuplicate = {
                    // budzik juz istnieje (pokazywanie komunikatu)
                    Toast.makeText(context, "Budzik na tę godzinę już istnieje!", Toast.LENGTH_SHORT).show()
                }
            )

        }) {
            Text("Zapisz budzik")
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = DarkAmethyst
        )
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { MathPreferencesManager(context) }

    val settings by preferencesManager.mathSettingsFlow.collectAsState(
        initial = MathSettings(
            isAdditionEnabled = true,
            isSubtractionEnabled = true,
            isMultiplicationEnabled = false,
            isDivisionEnabled = false,
            difficultyLevel = "Łatwy",
            customRingtoneUri = null
        )
    )

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // wyciaganie adresu wybranego dzwieku
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

            // zapisujemy nowy adres w naszej bazie ustawien
            coroutineScope.launch {
                preferencesManager.setCustomRingtoneUri(uri?.toString())
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Opcje matematyczne",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkAmethyst
        )
        Text(
            text = "Wybierz rodzaje zadań do pobudki:",
            style = MaterialTheme.typography.bodyMedium,
            color = DustyGrape,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        SettingToggleRow(
            title = "Dodawanie (+)",
            isChecked = true,
            enabled = false
        ) { }

        SettingToggleRow(
            title = "Odejmowanie (-)",
            isChecked = settings.isSubtractionEnabled
        ) { isChecked ->
            coroutineScope.launch { preferencesManager.setSubtractionEnabled(isChecked) }
        }

        SettingToggleRow(
            title = "Mnożenie (×)",
            isChecked = settings.isMultiplicationEnabled
        ) { isChecked ->
            coroutineScope.launch { preferencesManager.setMultiplicationEnabled(isChecked) }
        }

        SettingToggleRow(
            title = "Dzielenie (÷)",
            isChecked = settings.isDivisionEnabled
        ) { isChecked ->
            coroutineScope.launch { preferencesManager.setDivisionEnabled(isChecked) }
        }

        // WYBOR DZWONKA
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Dźwięk budzika",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkAmethyst
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // tworzymy zapytanie o okienko z alarmami
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)

                    // jesl uzytkownik ma juz dzwonek, zaznaczamy go na liscie
                    settings.customRingtoneUri?.let { uriString ->
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uriString))
                    }
                }
                ringtoneLauncher.launch(intent)
            }
        ) {
            val buttonText = if (settings.customRingtoneUri == null) "Wybierz dzwonek" else "Zmień dzwonek"
            Text(text = buttonText)
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) DarkAmethyst else AmethystSmoke
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = Lilac,
                uncheckedThumbColor = DustyGrape,
                uncheckedTrackColor = AmethystSmoke,
                disabledCheckedThumbColor = White.copy(alpha = 0.7f),
                disabledCheckedTrackColor = Lilac.copy(alpha = 0.5f)
            )
        )
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
}

private fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
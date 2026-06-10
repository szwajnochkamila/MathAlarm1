package com.example.mathalarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mathalarm.ui.theme.MathAlarmTheme
import com.example.mathalarm.data.MathPreferencesManager
import com.example.mathalarm.data.MathSettings
import kotlinx.coroutines.flow.first

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // blokowanie przycisku "Wstecz"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })

        // wymuszenie odgaszenia ekranu i zrzucenie blokady
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MathAlarmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    DismissScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onCorrectPassword = { dismissAlarm() }
                    )
                }
            }
        }
    }

    private fun dismissAlarm() {
        startService(AlarmService.stopIntent(this))
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AlarmService.NOTIFICATION_ID)
        finish()
    }
}

@Composable
private fun DismissScreen(
    modifier: Modifier = Modifier,
    onCorrectPassword: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { MathPreferencesManager(context) }

    var problemData by remember { mutableStateOf<Pair<String, Int>?>(null) }

    LaunchedEffect(Unit) { // Unit zapewnia, ze ten blok kodu wykona sie tylko raz  (gdy pojawia się ekran na wyswietlaczu)
        val settings = preferencesManager.mathSettingsFlow.first()
        problemData = generateMathProblem(settings)
    }

    if (problemData == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val (questionText, correctAnswer) = problemData!!

    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pobudka!",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Rozwiąż zadanie, aby wyłączyć budzik:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Text(
            text = questionText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                error = false
            },
            label = { Text("Wynik") },
            singleLine = true,
            isError = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (error) {
            Text(
                text = "Niepoprawny wynik!",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(
            onClick = {
                if (input.toIntOrNull() == correctAnswer) {
                    onCorrectPassword()
                } else {
                    error = true
                    input = ""
                }
            },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Wyłącz budzik")
        }
    }
}

private fun generateMathProblem(settings: MathSettings): Pair<String, Int> {
    val availableOperations = mutableListOf<String>()
    availableOperations.add("+")
    if (settings.isSubtractionEnabled) availableOperations.add("-")
    if (settings.isMultiplicationEnabled) availableOperations.add("×")
    if (settings.isDivisionEnabled) availableOperations.add("÷")

    val selectedOperation = availableOperations.random()

    return when (selectedOperation) {
        "+" -> {
            val a = (10..50).random()
            val b = (10..50).random()
            Pair("$a + $b = ?", a + b)
        }
        "-" -> {
            val a = (20..99).random()
            val b = (5..a - 1).random()
            Pair("$a - $b = ?", a - b)
        }
        "×" -> {
            val a = (3..12).random()
            val b = (3..12).random()
            Pair("$a × $b = ?", a * b)
        }
        "÷" -> {
            val divisor = (2..10).random()
            val answer = (3..12).random()
            val dividend = divisor * answer
            Pair("$dividend ÷ $divisor = ?", answer)
        }
        else -> Pair("2 + 2 = ?", 4)
    }
}
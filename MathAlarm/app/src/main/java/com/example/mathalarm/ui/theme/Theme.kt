package com.example.mathalarm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// kolory do ciemnego motywu
private val DarkColorScheme = darkColorScheme(
    primary = DustyGrape,
    secondary = Lilac,
    tertiary = AmethystSmoke,
    background = DarkAmethyst,
    surface = DarkAmethyst
)

// kolory do jasnego motywu
private val LightColorScheme = lightColorScheme(
    primary = DustyGrape,
    secondary = Lilac,
    tertiary = AmethystSmoke,
    background = PinkOrchid,
    surface = PinkOrchid
)

@Composable
fun MathAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
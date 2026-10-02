package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PesaGreenAccent,
    onPrimary = Color(0xFF003918),
    primaryContainer = PesaGreenDark,
    onPrimaryContainer = Color(0xFF9FF2B9),
    secondary = PesaGold,
    onSecondary = Color(0xFF422B00),
    secondaryContainer = Color(0xFF5F3F00),
    onSecondaryContainer = PesaGoldLight,
    tertiary = PesaTeal,
    background = ForestDark,
    onBackground = Color(0xFFE2EBE5),
    surface = DeepSurfaceDark,
    onSurface = Color(0xFFE2EBE5),
    surfaceVariant = CardSurfaceDark,
    onSurfaceVariant = Color(0xFFC0CEC5),
    error = PesaRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PesaGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = PesaGreenLight,
    onPrimaryContainer = PesaGreenDark,
    secondary = PesaGold,
    onSecondary = Color.White,
    secondaryContainer = PesaGoldLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = PesaTeal,
    background = Neutral50,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral700,
    error = PesaRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent brand identity
    content: @Composable () -> Unit,
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

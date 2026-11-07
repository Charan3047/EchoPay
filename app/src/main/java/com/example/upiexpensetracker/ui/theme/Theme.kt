package com.example.upiexpensetracker.ui.theme

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
    primary = Color(0xFFA8D9B5),
    onPrimary = Color(0xFF123B2A),
    primaryContainer = Color(0xFF174B3B),
    onPrimaryContainer = Color(0xFFD9F2DF),
    secondary = Color(0xFFB7DCC1),
    onSecondary = Color(0xFF183B2B),
    secondaryContainer = Color(0xFF294737),
    onSecondaryContainer = Color(0xFFD7EEDD),
    tertiary = Color(0xFFE7A18A),
    onTertiary = Color(0xFF48271D),
    background = Color(0xFF101814),
    onBackground = Color(0xFFE4ECE5),
    surface = Color(0xFF18231D),
    onSurface = Color(0xFFE4ECE5),
    surfaceVariant = Color(0xFF26352C),
    onSurfaceVariant = Color(0xFFB5C3B8),
    outline = Color(0xFF718176),
    outlineVariant = Color(0xFF3A4A40)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF174B3B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF174B3B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF418B70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFE6C8),
    onSecondaryContainer = Color(0xFF174B3B),
    tertiary = Color(0xFFCD7259),
    onTertiary = Color.White,
    background = Color(0xFFF5F4EE),
    onBackground = Color(0xFF182A25),
    surface = Color.White,
    onSurface = Color(0xFF182A25),
    surfaceVariant = Color(0xFFEBEFE9),
    onSurfaceVariant = Color(0xFF718078),
    outline = Color(0xFFA0AEA4),
    outlineVariant = Color(0xFFE0E5DF)
)

@Composable
fun UPIExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

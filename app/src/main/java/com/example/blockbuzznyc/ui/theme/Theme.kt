package com.example.blockbuzznyc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define the new color palette
val TextColor = Color(0xFF080505)
val BackgroundColor = Color(0xFFdedada)
val PrimaryColor = Color(0xFFbccea8)
val SecondaryColor = Color(0xFFf28380)
val AccentColor = Color(0xFF84bb85)

// Dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = TextColor,
    secondary = SecondaryColor,
    onSecondary = TextColor,
    tertiary = AccentColor,
    onTertiary = TextColor,
    background = BackgroundColor,
    onBackground = TextColor,
    surface = BackgroundColor,
    onSurface = TextColor,
)

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = TextColor,
    secondary = SecondaryColor,
    onSecondary = TextColor,
    tertiary = AccentColor,
    onTertiary = TextColor,
    background = BackgroundColor,
    onBackground = TextColor,
    surface = BackgroundColor,
    onSurface = TextColor,
)

@Composable
fun BlockBuzzNYCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set this to 'false' to use custom colors always
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}

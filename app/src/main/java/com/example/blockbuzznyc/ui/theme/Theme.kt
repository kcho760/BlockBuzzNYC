package com.example.blockbuzznyc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme.typography

// Define the new color palette
val DarkCharcoal = Color(0xFF292F36)
val LavenderGray = Color(0xFF9D8CA1)
val TeaGreen = Color(0xFFEEFFDB)
val SteelBlue = Color(0xFF348AA7)
val ArtichokeGreen = Color(0xFF56876D)

// Dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = DarkCharcoal,
    secondary = LavenderGray,
    tertiary = SteelBlue,
    background = ArtichokeGreen,
    surface = TeaGreen,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    // Add other overrides for colors as needed
)

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = DarkCharcoal,
    secondary = LavenderGray,
    tertiary = SteelBlue,
    background = ArtichokeGreen,
    surface = TeaGreen,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    // Add other overrides for colors as needed
)

@Composable
fun BlockBuzzNYCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set this to 'false' to use custom colors always
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

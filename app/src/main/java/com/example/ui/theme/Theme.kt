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

private val DarkColorScheme =
  darkColorScheme(
    primary = Amber80,
    onPrimary = DarkRoast,
    primaryContainer = WarmBronze,
    onPrimaryContainer = Color(0xFFFFD59E),
    secondary = AmberGrey80,
    onSecondary = DarkRoast,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = WarmSand80,
    background = DarkRoast,
    onBackground = Color(0xFFEDE0D4),
    surface = DarkSurface,
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD7C2B2),
    outline = Color(0xFF6B5E52),
    outlineVariant = Color(0xFF4A4036)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = WarmBronze,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF4E2600),
    secondary = WarmSlate,
    onSecondary = Color.White,
    secondaryContainer = SoftCream,
    onSecondaryContainer = DarkRoast,
    background = BookParchment,
    onBackground = DarkRoast,
    surface = Color(0xFFFFFFFF),
    onSurface = DarkRoast,
    surfaceVariant = SoftCream,
    onSurfaceVariant = WarmSlate,
    outline = Color(0xFFBCAAA4),
    outlineVariant = Color(0xFFE0D6CC)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to studio dark theme for audio gear vibe
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


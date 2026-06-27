package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = StudioPrimary,
    secondary = StudioAccent,
    tertiary = StudioGold,
    background = StudioBlack,
    surface = StudioCardBg,
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color.White,
    onSecondary = StudioBlack,
    onTertiary = StudioBlack
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StudioPrimary,
    secondary = StudioAccent,
    tertiary = StudioGold,
    background = Color(0xFFF8F9FD),
    surface = Color.White,
    onBackground = StudioBlack,
    onSurface = StudioBlack
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force gorgeous dark studio theme by default for premium visual identity
  dynamicColor: Boolean = false, // Use our handcrafted luxury palette for brand cohesion
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

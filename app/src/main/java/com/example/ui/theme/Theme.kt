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

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentTeal,
    secondary = SunYellow,
    tertiary = EarthBlue,
    background = SpaceBlack,
    surface = SpaceDarkIndigo,
    onPrimary = SpaceBlack,
    onSecondary = SpaceBlack,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = SpaceNavy
  )

private val LightColorScheme =
  darkColorScheme( // We want to keep it cosmic-themed even in light mode for astronomical visualization!
    primary = AccentTeal,
    secondary = SunYellow,
    tertiary = EarthBlue,
    background = SpaceBlack,
    surface = SpaceDarkIndigo,
    onPrimary = SpaceBlack,
    onSecondary = SpaceBlack,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = SpaceNavy
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default for the celestial atmosphere
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored space aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

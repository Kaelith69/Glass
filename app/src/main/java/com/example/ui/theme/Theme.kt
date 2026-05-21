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

private val DarkColorScheme = darkColorScheme(
    primary = AccentVermilion,
    onPrimary = TextPrimaryDark,
    secondary = RatingGold,
    onSecondary = CinematicBlack,
    background = CinematicBlack,
    onBackground = TextPrimaryDark,
    surface = SurfaceCharcoal,
    onSurface = TextPrimaryDark,
    surfaceVariant = SystemGlassOver,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCrimson,
    onPrimary = TextPrimaryLight,
    secondary = RatingGold,
    onSecondary = CinematicPaper,
    background = CinematicPaper,
    onBackground = TextPrimaryLight,
    surface = SurfacePearl,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSystemGlassOver,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // We use bespoke cinematic styles not dynamic material
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

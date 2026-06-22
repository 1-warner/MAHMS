package com.doodoobops.mahms.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Light-only Mpilo colour scheme. Dark mode is intentionally not supported. */
private val MahmsColorScheme = lightColorScheme(
    primary = MpiloBlue500,
    onPrimary = Neutral0,
    primaryContainer = MpiloBlue600,
    onPrimaryContainer = Neutral0,
    secondary = CareTeal400,
    onSecondary = Neutral0,
    secondaryContainer = CareTeal50,
    onSecondaryContainer = CareTeal700,
    tertiary = CareTeal400,
    error = Emergency500,
    onError = Neutral0,
    errorContainer = Emergency50,
    onErrorContainer = Emergency700,
    background = Neutral25,
    onBackground = Neutral900,
    surface = Neutral0,
    onSurface = Neutral900,
    surfaceVariant = Neutral50,
    onSurfaceVariant = Neutral500,
    outline = Neutral200,
    outlineVariant = Neutral100,
)

@Composable
fun MahmsTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MpiloBlue500.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = MahmsColorScheme,
        typography = MahmsTypography,
        shapes = MahmsShapes,
        content = content,
    )
}

package com.uruk.ime.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = ClayBrown,
    onPrimary = ClayLight,
    primaryContainer = ClayLight,
    onPrimaryContainer = WedgeInk,
    secondary = AccentClay,
    onSecondary = ClayLight,
    surface = TabletSurface,
    onSurface = WedgeInk,
    background = ClayLight,
    onBackground = WedgeInk,
)

private val DarkScheme = darkColorScheme(
    primary = ClayLight,
    onPrimary = WedgeInk,
    primaryContainer = ClayBrown,
    onPrimaryContainer = ClayLight,
    secondary = AccentClay,
    onSecondary = WedgeInk,
    surface = WedgeInk,
    onSurface = ClayLight,
    background = Color(0xFF1C1917),
    onBackground = ClayLight,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun UrukImeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}

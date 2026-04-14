package com.overklassniy.q25.dialer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.overklassniy.q25.dialer.data.PreferencesManager

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightOnSurface,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = LightSurfaceVariant,
    onTertiaryContainer = LightOnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

@Composable
fun Q25DialerTheme(
    darkTheme: Boolean = true,
    colorRefreshKey: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Re-read colors when refresh key changes
    val colorScheme = remember(darkTheme, colorRefreshKey) {
        fun c(key: String) = prefs.getCustomColor(key)?.let { Color(it.toULong()) }
        baseScheme.copy(
        primary = c(PreferencesManager.KEY_COLOR_PRIMARY) ?: baseScheme.primary,
        onPrimary = c(PreferencesManager.KEY_COLOR_ON_PRIMARY) ?: baseScheme.onPrimary,
        secondary = c(PreferencesManager.KEY_COLOR_SECONDARY) ?: baseScheme.secondary,
        background = c(PreferencesManager.KEY_COLOR_BACKGROUND) ?: baseScheme.background,
        onBackground = c(PreferencesManager.KEY_COLOR_ON_BACKGROUND) ?: baseScheme.onBackground,
        surface = c(PreferencesManager.KEY_COLOR_SURFACE) ?: baseScheme.surface,
        onSurface = c(PreferencesManager.KEY_COLOR_ON_SURFACE) ?: baseScheme.onSurface,
            surfaceVariant = c(PreferencesManager.KEY_COLOR_SURFACE_VARIANT) ?: baseScheme.surfaceVariant,
            onSurfaceVariant = c(PreferencesManager.KEY_COLOR_ON_SURFACE_VARIANT) ?: baseScheme.onSurfaceVariant,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
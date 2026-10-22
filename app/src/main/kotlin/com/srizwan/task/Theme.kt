package com.srizwan.task

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode { System, Light, Dark }

@Immutable
data class ExtraColors(
    val canvas: Color,
    val muted: Color,
    val line: Color,
    val panel2: Color,
    val primary2: Color,
    val gold: Color,
    val ok: Color,
    val warn: Color
)

private val LightExtra = ExtraColors(
    canvas = Color(0xFFE3F0E9),
    muted = Color(0xFF587068),
    line = Color(0xFFC6DBD0),
    panel2 = Color(0xFFD9E9E0),
    primary2 = Color(0xFF16A583),
    gold = Color(0xFFC9972B),
    ok = Color(0xFF15803D),
    warn = Color(0xFFB45309)
)

private val DarkExtra = ExtraColors(
    canvas = Color(0xFF06110E),
    muted = Color(0xFF8DB0A3),
    line = Color(0xFF1F3B32),
    panel2 = Color(0xFF1A3329),
    primary2 = Color(0xFF27B88C),
    gold = Color(0xFFE8C56F),
    ok = Color(0xFF6FD08C),
    warn = Color(0xFFFBBF24)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0E7A62),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF16A583),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FCFA),
    onBackground = Color(0xFF12261F),
    surface = Color(0xFFEAF4EF),
    onSurface = Color(0xFF12261F),
    surfaceVariant = Color(0xFFD9E9E0),
    onSurfaceVariant = Color(0xFF587068),
    outline = Color(0xFFC6DBD0),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3FD3A6),
    onPrimary = Color(0xFF04130E),
    secondary = Color(0xFF27B88C),
    onSecondary = Color(0xFF04130E),
    background = Color(0xFF0C1B16),
    onBackground = Color(0xFFE4F1EA),
    surface = Color(0xFF12261F),
    onSurface = Color(0xFFE4F1EA),
    surfaceVariant = Color(0xFF1A3329),
    onSurfaceVariant = Color(0xFF8DB0A3),
    outline = Color(0xFF1F3B32),
    error = Color(0xFFF87171),
    onError = Color(0xFF04130E)
)

private val LocalExtraColors = staticCompositionLocalOf { LightExtra }

object AppColors {
    val extra: ExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current
}

@Composable
fun RizwanTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content
        )
    }
}

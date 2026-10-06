package com.kazushiki.pilates.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun rgb(r: Double, g: Double, b: Double) = Color(r.toFloat(), g.toFloat(), b.toFloat())

/** Kazushiki Pilates colors. Every color adapts to light and dark mode (same values as iPhone). */
@Immutable
data class KPColors(
    val background: Color,
    val surface: Color,
    val stage: Color,
    val text: Color,
    val mutedText: Color,
    val divider: Color,
    /** Brand plum. */
    val accent: Color,
    val accentSoft: Color,
    /** Text or icons placed on top of accent. */
    val onAccent: Color,
    val figureFar: Color,
    val mat: Color,
    val floor: Color,
    /** The reformer's frame, footbar and pulley posts. */
    val apparatus: Color,
    /** Equipment drawn with the figure (ball, ring, band, weights, wall); also the gold accent. */
    val prop: Color,
) {
    val figure: Color get() = accent
}

val LightKPColors = KPColors(
    background = rgb(0.965, 0.957, 0.969),
    surface = rgb(1.0, 1.0, 1.0),
    stage = rgb(0.984, 0.980, 0.988),
    text = rgb(0.149, 0.129, 0.169),
    mutedText = rgb(0.424, 0.392, 0.455),
    divider = rgb(0.890, 0.867, 0.910),
    accent = rgb(0.541, 0.310, 0.490),
    accentSoft = rgb(0.945, 0.902, 0.933),
    onAccent = rgb(1.0, 1.0, 1.0),
    figureFar = rgb(0.812, 0.663, 0.773),
    mat = rgb(0.725, 0.827, 0.776),
    floor = rgb(0.851, 0.824, 0.871),
    apparatus = rgb(0.600, 0.557, 0.635),
    prop = rgb(0.831, 0.659, 0.353),
)

val DarkKPColors = KPColors(
    background = rgb(0.094, 0.082, 0.106),
    surface = rgb(0.133, 0.118, 0.149),
    stage = rgb(0.114, 0.102, 0.129),
    text = rgb(0.925, 0.902, 0.937),
    mutedText = rgb(0.651, 0.612, 0.682),
    divider = rgb(0.216, 0.188, 0.239),
    accent = rgb(0.831, 0.576, 0.769),
    accentSoft = rgb(0.227, 0.153, 0.208),
    onAccent = rgb(0.094, 0.082, 0.106),
    figureFar = rgb(0.431, 0.290, 0.400),
    mat = rgb(0.247, 0.353, 0.302),
    floor = rgb(0.227, 0.200, 0.251),
    apparatus = rgb(0.412, 0.376, 0.451),
    prop = rgb(0.878, 0.722, 0.451),
)

val LocalKPColors = staticCompositionLocalOf { LightKPColors }

/** Shorthand: `KP.colors.accent`, `KP.cornerRadius`. */
object KP {
    val colors: KPColors
        @Composable @ReadOnlyComposable get() = LocalKPColors.current
    val cornerRadius: Dp = 18.dp
    val compactCornerRadius: Dp = 12.dp
    /** The larger rounded cards (mode cards, tiles, setting cards). */
    val cardRadius: Dp = 22.dp
}

@Composable
fun KazushikiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkKPColors else LightKPColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft, onPrimaryContainer = colors.text,
            secondary = colors.accent, onSecondary = colors.onAccent,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.background, onSurfaceVariant = colors.mutedText,
            outline = colors.divider, outlineVariant = colors.divider,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
            surfaceContainerLow = colors.surface, surfaceContainerHighest = colors.surface,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft, onPrimaryContainer = colors.text,
            secondary = colors.accent, onSecondary = colors.onAccent,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.background, onSurfaceVariant = colors.mutedText,
            outline = colors.divider, outlineVariant = colors.divider,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
            surfaceContainerLow = colors.surface, surfaceContainerHighest = colors.surface,
        )
    }
    CompositionLocalProvider(LocalKPColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** The standard card: surface fill, 1dp divider border, rounded corners, inner padding. */
@Composable
fun Modifier.kpCard(padding: Dp = 16.dp, radius: Dp = KP.cornerRadius): Modifier {
    val c = KP.colors
    val shape = RoundedCornerShape(radius)
    return this
        .background(c.surface, shape)
        .border(1.dp, c.divider, shape)
        .padding(padding)
}

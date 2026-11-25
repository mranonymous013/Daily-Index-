package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = OnTealContainerLight,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSoftBg,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = SlateCanvasLight,
    onBackground = SlateTextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateTextSecondaryLight,
    outline = SlateOutlineLight,
    outlineVariant = SlateOutlineVariantLight,
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseSoftBg,
    onErrorContainer = Color(0xFF9F1239),
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = OnTealPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberBarLight,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = SlateCanvasDark,
    onBackground = SlateTextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = SlateOutlineDark,
    outlineVariant = Color(0xFF1E293B),
    error = RoseBarLight,
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6),
)

val DailyFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DailyFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DailyFlowTypography,
        shapes = DailyFlowShapes,
        content = content,
    )
}

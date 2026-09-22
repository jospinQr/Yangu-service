package com.megamind.yanguservice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MonochromeColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoLightSurface,
    onPrimaryContainer = MonoBlack,
    inversePrimary = MonoWhite,
    secondary = MonoBlack,
    onSecondary = MonoWhite,
    secondaryContainer = MonoLightSurface,
    onSecondaryContainer = MonoBlack,
    tertiary = MonoBlack,
    onTertiary = MonoWhite,
    tertiaryContainer = MonoLightSurface,
    onTertiaryContainer = MonoBlack,
    background = MonoWhite,
    onBackground = MonoBlack,
    surface = MonoWhite,
    onSurface = MonoBlack,
    surfaceVariant = MonoSurface,
    onSurfaceVariant = MonoCharcoal,
    surfaceTint = MonoBlack,
    inverseSurface = MonoBlack,
    inverseOnSurface = MonoWhite,
    error = MonoBlack,
    onError = MonoWhite,
    errorContainer = MonoLightSurface,
    onErrorContainer = MonoBlack,
    outline = MonoBorder,
    outlineVariant = MonoSoftBorder,
    scrim = MonoBlack,
    surfaceBright = MonoWhite,
    surfaceDim = MonoMediumSurface,
    surfaceContainerLowest = MonoWhite,
    surfaceContainerLow = MonoAlmostWhite,
    surfaceContainer = MonoSurface,
    surfaceContainerHigh = MonoLightSurface,
    surfaceContainerHighest = MonoMediumSurface,
    primaryFixed = MonoLightSurface,
    primaryFixedDim = MonoMediumSurface,
    onPrimaryFixed = MonoBlack,
    onPrimaryFixedVariant = MonoCharcoal,
    secondaryFixed = MonoLightSurface,
    secondaryFixedDim = MonoMediumSurface,
    onSecondaryFixed = MonoBlack,
    onSecondaryFixedVariant = MonoCharcoal,
    tertiaryFixed = MonoLightSurface,
    tertiaryFixedDim = MonoMediumSurface,
    onTertiaryFixed = MonoBlack,
    onTertiaryFixedVariant = MonoCharcoal,
)

@Composable
fun YanguServiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeColorScheme,
        typography = Typography,
        content = content,
    )
}

package com.parv.tasteindia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SpicePrimaryLight,
    onPrimary = SpiceOnPrimaryLight,
    primaryContainer = SpicePrimaryContainerLight,
    onPrimaryContainer = SpiceOnPrimaryContainerLight,
    secondary = SpiceSecondaryLight,
    onSecondary = SpiceOnSecondaryLight,
    secondaryContainer = SpiceSecondaryContainerLight,
    onSecondaryContainer = SpiceOnSecondaryContainerLight,
    tertiary = SpiceTertiaryLight,
    onTertiary = SpiceOnTertiaryLight,
    tertiaryContainer = SpiceTertiaryContainerLight,
    onTertiaryContainer = SpiceOnTertiaryContainerLight,
    background = SpiceBackgroundLight,
    onBackground = SpiceOnBackgroundLight,
    surface = SpiceSurfaceLight,
    onSurface = SpiceOnSurfaceLight,
    surfaceVariant = SpiceSurfaceVariantLight,
    onSurfaceVariant = SpiceOnSurfaceVariantLight,
    outline = SpiceOutlineLight,
    outlineVariant = SpiceOutlineVariantLight,
    error = SpiceErrorLight,
    onError = SpiceOnErrorLight,
    surfaceContainerLowest = SpiceSurfaceContainerLowestLight,
    surfaceContainerLow = SpiceSurfaceContainerLowLight,
    surfaceContainer = SpiceSurfaceContainerLight,
    surfaceContainerHigh = SpiceSurfaceContainerHighLight,
    surfaceContainerHighest = SpiceSurfaceContainerHighestLight,
)

private val DarkColors = darkColorScheme(
    primary = SpicePrimaryDark,
    onPrimary = SpiceOnPrimaryDark,
    primaryContainer = SpicePrimaryContainerDark,
    onPrimaryContainer = SpiceOnPrimaryContainerDark,
    secondary = SpiceSecondaryDark,
    onSecondary = SpiceOnSecondaryDark,
    secondaryContainer = SpiceSecondaryContainerDark,
    onSecondaryContainer = SpiceOnSecondaryContainerDark,
    tertiary = SpiceTertiaryDark,
    onTertiary = SpiceOnTertiaryDark,
    tertiaryContainer = SpiceTertiaryContainerDark,
    onTertiaryContainer = SpiceOnTertiaryContainerDark,
    background = SpiceBackgroundDark,
    onBackground = SpiceOnBackgroundDark,
    surface = SpiceSurfaceDark,
    onSurface = SpiceOnSurfaceDark,
    surfaceVariant = SpiceSurfaceVariantDark,
    onSurfaceVariant = SpiceOnSurfaceVariantDark,
    outline = SpiceOutlineDark,
    outlineVariant = SpiceOutlineVariantDark,
    error = SpiceErrorDark,
    onError = SpiceOnErrorDark,
    surfaceContainerLowest = SpiceSurfaceContainerLowestDark,
    surfaceContainerLow = SpiceSurfaceContainerLowDark,
    surfaceContainer = SpiceSurfaceContainerDark,
    surfaceContainerHigh = SpiceSurfaceContainerHighDark,
    surfaceContainerHighest = SpiceSurfaceContainerHighestDark,
)

/**
 * Dynamic (Material You) colour is intentionally OFF: for a review build the palette should be
 * the same TasteIndia identity on every device rather than the user's wallpaper. It can be
 * re-enabled with a flag if desired.
 */
@Composable
fun TasteIndiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}

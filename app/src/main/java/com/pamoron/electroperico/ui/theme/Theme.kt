package com.pamoron.electroperico.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

/**
 * Colores de las etiquetas de valoración.
 *
 * No forman parte del esquema de Material, así que viajan por su propio
 * `CompositionLocal` para adaptarse a modo claro y oscuro.
 */
data class RatingColors(
    val goodContainer: Color,
    val onGood: Color,
    val neutralContainer: Color,
    val onNeutral: Color,
    val warnContainer: Color,
    val onWarn: Color,
    val badContainer: Color,
    val onBad: Color,
)

private val LightRatingColors = RatingColors(
    goodContainer = RatingGoodContainerLight,
    onGood = RatingGoodContentLight,
    neutralContainer = RatingNeutralContainerLight,
    onNeutral = RatingNeutralContentLight,
    warnContainer = RatingWarnContainerLight,
    onWarn = RatingWarnContentLight,
    badContainer = RatingBadContainerLight,
    onBad = RatingBadContentLight,
)

private val DarkRatingColors = RatingColors(
    goodContainer = RatingGoodContainerDark,
    onGood = RatingGoodContentDark,
    neutralContainer = RatingNeutralContainerDark,
    onNeutral = RatingNeutralContentDark,
    warnContainer = RatingWarnContainerDark,
    onWarn = RatingWarnContentDark,
    badContainer = RatingBadContainerDark,
    onBad = RatingBadContentDark,
)

val LocalRatingColors = staticCompositionLocalOf { LightRatingColors }

/**
 * Tema de la aplicación, con soporte de modo claro y oscuro.
 *
 * En Android 12 o superior se usa el color dinámico del sistema si está
 * disponible; en versiones anteriores, la paleta propia.
 */
@Composable
fun ElectroPericoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    val ratingColors = if (darkTheme) DarkRatingColors else LightRatingColors

    CompositionLocalProvider(LocalRatingColors provides ratingColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ElectroPericoTypography,
            content = content,
        )
    }
}

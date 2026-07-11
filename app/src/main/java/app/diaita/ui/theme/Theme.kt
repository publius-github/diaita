package app.diaita.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = GreenPrimary,
        onPrimary = GreenOnPrimary,
        primaryContainer = GreenPrimaryContainer,
        onPrimaryContainer = GreenOnPrimaryContainer,
        secondary = OrangeSecondary,
        onSecondary = OrangeOnSecondary,
        tertiary = AmberTertiary,
        onTertiary = AmberOnTertiary,
        error = ErrorLight,
        onError = OnErrorLight,
        outlineVariant = OutlineVariantLight,
    )

private val DarkColors =
    darkColorScheme(
        primary = GreenPrimaryDark,
        onPrimary = GreenOnPrimaryDark,
        primaryContainer = GreenPrimaryContainerDark,
        onPrimaryContainer = GreenOnPrimaryContainerDark,
        secondary = OrangeSecondaryDark,
        onSecondary = OrangeOnSecondaryDark,
        tertiary = AmberTertiaryDark,
        onTertiary = AmberOnTertiaryDark,
        error = ErrorDark,
        onError = OnErrorDark,
        outlineVariant = OutlineVariantDark,
    )

@Composable
fun CalorieCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColors
            else -> LightColors
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

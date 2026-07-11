package app.diaita.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    primaryContainer = GreenPrimaryContainer,
    secondary = OrangeSecondary,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryContainer,
    secondary = OrangeSecondary,
    error = ErrorRed,
)

@Composable
fun CalorieCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

package app.diaita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.diaita.data.settings.ThemeMode
import app.diaita.ui.navigation.AppNavHost
import app.diaita.ui.theme.CalorieCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as CalorieCounterApplication).container.settingsRepository
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dynamicColor by settingsRepository.dynamicColor.collectAsStateWithLifecycle(true)
            CalorieCounterTheme(
                darkTheme =
                    when (themeMode) {
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    },
                dynamicColor = dynamicColor,
            ) {
                AppNavHost()
            }
        }
    }
}

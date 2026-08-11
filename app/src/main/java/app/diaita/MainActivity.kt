package app.diaita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.diaita.data.settings.ThemeMode
import app.diaita.ui.navigation.AppNavHost
import app.diaita.ui.onboarding.OnboardingFlow
import app.diaita.ui.theme.CalorieCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash stays up until the onboarding decision is read from DataStore, so
        // neither the diary nor the onboarding flow ever flashes in as the wrong first frame.
        var keepSplash = true
        installSplashScreen().setKeepOnScreenCondition { keepSplash }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as CalorieCounterApplication).container
        val settingsRepository = container.settingsRepository
        setContent {
            // null = still deciding (splash covers the gap). Once onboarding is shown, the
            // done flag flips this to false in place, swapping the main UI in.
            val needsOnboarding by produceState<Boolean?>(initialValue = null) {
                if (!container.onboardingGate.shouldShowOnboarding()) {
                    value = false
                } else {
                    value = true
                    settingsRepository.onboardingDone.collect { done ->
                        if (done) value = false
                    }
                }
            }
            LaunchedEffect(needsOnboarding) {
                if (needsOnboarding != null) keepSplash = false
            }
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
                when (needsOnboarding) {
                    null -> Unit
                    true -> OnboardingFlow()
                    false -> AppNavHost()
                }
            }
        }
    }
}

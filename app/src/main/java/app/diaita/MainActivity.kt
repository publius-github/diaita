package app.diaita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.diaita.ui.navigation.AppNavHost
import app.diaita.ui.theme.CalorieCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            CalorieCounterTheme {
                AppNavHost()
            }
        }
    }
}

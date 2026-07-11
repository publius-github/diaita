package app.diaita.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.diaita.R
import app.diaita.ui.addfood.AddFoodScreen
import app.diaita.ui.daydetail.DayDetailScreen
import app.diaita.ui.diary.DiaryScreen
import app.diaita.ui.goals.GoalsScreen
import app.diaita.ui.history.HistoryScreen
import app.diaita.ui.presets.PresetEditScreen
import app.diaita.ui.presets.PresetListScreen
import app.diaita.ui.settings.BackupScreen
import app.diaita.ui.settings.SettingsScreen

private data class BottomNavItem(val screen: Screen, val labelRes: Int, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Diary, R.string.nav_today, Icons.Filled.Today),
    BottomNavItem(Screen.PresetList, R.string.nav_presets, Icons.Filled.List),
    BottomNavItem(Screen.History, R.string.nav_history, Icons.Filled.History),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (bottomNavItems.any { it.screen.route == currentRoute }) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Diary.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Diary.route) {
                DiaryScreen(onAddFoodClick = { navController.navigate(Screen.AddFood.route) })
            }
            composable(Screen.AddFood.route) {
                AddFoodScreen(onDone = { navController.popBackStack() })
            }
            composable(Screen.PresetList.route) {
                PresetListScreen(
                    onAddPresetClick = { navController.navigate(Screen.PresetEdit.create()) },
                    onEditPresetClick = { presetId ->
                        navController.navigate(Screen.PresetEdit.create(presetId))
                    },
                    onPresetLogged = {
                        navController.navigate(Screen.Diary.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(
                route = Screen.PresetEdit.route,
                arguments = listOf(
                    navArgument("presetId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val presetId = entry.arguments?.getLong("presetId") ?: -1L
                PresetEditScreen(
                    onDone = { navController.popBackStack() },
                    presetId = presetId.takeIf { it > 0 },
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(onViewDay = { date -> navController.navigate(Screen.DayDetail.create(date)) })
            }
            composable(
                route = Screen.DayDetail.route,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) {
                DayDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onGoalsClick = { navController.navigate(Screen.Goals.route) },
                    onBackupClick = { navController.navigate(Screen.Backup.route) },
                )
            }
            composable(Screen.Goals.route) {
                GoalsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Backup.route) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

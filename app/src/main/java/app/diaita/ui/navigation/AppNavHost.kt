package app.diaita.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.diaita.R
import app.diaita.ui.about.AboutScreen
import app.diaita.ui.addfood.AddFoodScreen
import app.diaita.ui.daydetail.DayDetailScreen
import app.diaita.ui.diary.DiaryScreen
import app.diaita.ui.goals.GoalWizardScreen
import app.diaita.ui.goals.GoalsScreen
import app.diaita.ui.history.HistoryScreen
import app.diaita.ui.presets.PresetEditScreen
import app.diaita.ui.presets.PresetListScreen
import app.diaita.ui.settings.AppearanceSettingsScreen
import app.diaita.ui.settings.BackupScreen
import app.diaita.ui.settings.DataSettingsScreen
import app.diaita.ui.settings.NotificationsSettingsScreen
import app.diaita.ui.settings.SettingsScreen
import app.diaita.ui.settings.TrackingSettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomNavItems =
    listOf(
        BottomNavItem(Screen.Diary, R.string.nav_home, Icons.Filled.Home),
        BottomNavItem(Screen.PresetList, R.string.nav_presets, Icons.Filled.List),
        BottomNavItem(Screen.History, R.string.nav_history, Icons.Filled.History),
        BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings),
    )

/** SavedStateHandle flag the Home tab raises so the Diary snaps back to today. */
private const val DIARY_JUMP_TO_TODAY_KEY = "diaryJumpToToday"

/** Screen-switch cross-fade duration; NavHost's 700ms default reads as lag. */
private const val NAV_FADE_MILLIS = 200

/** Compact bottom-bar height (excl. system inset) replacing M3's default 80dp. */
private val COMPACT_NAV_BAR_HEIGHT = 64.dp

/**
 * Item content (icon + indicator pill + label) measures 52dp, so a 64dp bar centers it with
 * 6dp of slack above and below. Reserving 12dp at the top doubles the visible gap above the
 * selection pill and drops the content those 6dp lower, flush with the bar's bottom edge.
 */
private val NAV_BAR_CONTENT_TOP_GAP = 12.dp

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (bottomNavItems.any { it.screen.route == currentRoute }) {
                // M3's default 80dp bar reads as dead space above the icons; 64dp still fits
                // icon + label comfortably. The system nav inset is added back on top so the
                // items themselves sit lower, right above the gesture/button area.
                val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                NavigationBar(
                    modifier = Modifier.height(COMPACT_NAV_BAR_HEIGHT + bottomInset),
                    windowInsets =
                        NavigationBarDefaults.windowInsets.add(WindowInsets(top = NAV_BAR_CONTENT_TOP_GAP)),
                ) {
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
                                // Home doubles as "back to today": every tap on it, whether a
                                // tab switch or a reselect, resets the diary's shown date.
                                if (item.screen == Screen.Diary) {
                                    navController
                                        .getBackStackEntry(Screen.Diary.route)
                                        .savedStateHandle[DIARY_JUMP_TO_TODAY_KEY] = true
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
            // NavHost's default screen transition is a 700ms cross-fade, which reads as lag
            // on every tab switch; a 200ms fade keeps the continuity cue but feels instant.
            enterTransition = { fadeIn(animationSpec = tween(NAV_FADE_MILLIS)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_FADE_MILLIS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(NAV_FADE_MILLIS)) },
            popExitTransition = { fadeOut(animationSpec = tween(NAV_FADE_MILLIS)) },
            // consumeWindowInsets stops nested Scaffolds (Diary, Goals, ...) from applying
            // the status-bar inset a second time on top of this Scaffold's padding.
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        ) {
            composable(Screen.Diary.route) { entry ->
                val jumpToToday by entry.savedStateHandle
                    .getStateFlow(DIARY_JUMP_TO_TODAY_KEY, false)
                    .collectAsStateWithLifecycle()
                DiaryScreen(
                    onAddFoodClick = { date -> navController.navigate(Screen.AddFood.create(date)) },
                    onOpenGoals = { navController.navigate(Screen.Goals.route) },
                    onOpenWizard = { navController.navigate(Screen.GoalWizard.route) },
                    jumpToToday = jumpToToday,
                    onJumpToTodayHandled = { entry.savedStateHandle[DIARY_JUMP_TO_TODAY_KEY] = false },
                )
            }
            composable(
                route = Screen.AddFood.route,
                arguments =
                    listOf(
                        navArgument("date") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
            ) {
                AddFoodScreen(onDone = { navController.popBackStack() })
            }
            composable(Screen.PresetList.route) {
                PresetListScreen(
                    onAddPresetClick = { isDish ->
                        navController.navigate(Screen.PresetEdit.create(isDish = isDish))
                    },
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
                arguments =
                    listOf(
                        navArgument("presetId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument("dish") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
            ) { entry ->
                val presetId = entry.arguments?.getLong("presetId") ?: -1L
                PresetEditScreen(
                    onDone = { navController.popBackStack() },
                    presetId = presetId.takeIf { it > 0 },
                    isDish = entry.arguments?.getBoolean("dish") == true,
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
                    onWizardClick = { navController.navigate(Screen.GoalWizard.route) },
                    onAppearanceClick = { navController.navigate(Screen.SettingsAppearance.route) },
                    onTrackingClick = { navController.navigate(Screen.SettingsTracking.route) },
                    onNotificationsClick = { navController.navigate(Screen.SettingsNotifications.route) },
                    onDataClick = { navController.navigate(Screen.SettingsData.route) },
                    onAboutClick = { navController.navigate(Screen.About.route) },
                )
            }
            composable(Screen.SettingsAppearance.route) {
                AppearanceSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsTracking.route) {
                TrackingSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsNotifications.route) {
                NotificationsSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsData.route) {
                DataSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onBackupClick = { navController.navigate(Screen.Backup.route) },
                )
            }
            composable(Screen.Goals.route) {
                GoalsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWizard = { navController.navigate(Screen.GoalWizard.route) },
                )
            }
            composable(Screen.GoalWizard.route) {
                GoalWizardScreen(onDone = { navController.popBackStack() })
            }
            composable(Screen.Backup.route) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

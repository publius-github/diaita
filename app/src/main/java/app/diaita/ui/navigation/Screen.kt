package app.diaita.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Diary : Screen("diary")

    data object AddFood : Screen("add_food?date={date}") {
        fun create(date: String): String = "add_food?date=$date"
    }

    data object PresetList : Screen("presets")

    data object PresetEdit : Screen("presets/edit?presetId={presetId}") {
        fun create(presetId: Long? = null): String = "presets/edit?presetId=${presetId ?: -1L}"
    }

    data object History : Screen("history")

    data object Settings : Screen("settings")

    data object About : Screen("about")

    data object Goals : Screen("settings/goals")

    data object GoalWizard : Screen("settings/goal-wizard")

    data object Backup : Screen("settings/backup")

    data object SettingsAppearance : Screen("settings/appearance")

    data object SettingsTracking : Screen("settings/tracking")

    data object SettingsNotifications : Screen("settings/notifications")

    data object SettingsData : Screen("settings/data")

    data object DayDetail : Screen("day/{date}") {
        fun create(date: String): String = "day/$date"
    }
}

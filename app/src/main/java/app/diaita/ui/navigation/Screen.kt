package app.diaita.ui.navigation

sealed class Screen(val route: String) {
    data object Diary : Screen("diary")
    data object AddFood : Screen("add_food")
    data object PresetList : Screen("presets")
    data object PresetEdit : Screen("presets/edit?presetId={presetId}") {
        fun create(presetId: Long? = null): String = "presets/edit?presetId=${presetId ?: -1L}"
    }
    data object History : Screen("history")
    data object Goals : Screen("goals")
    data object DayDetail : Screen("day/{date}") {
        fun create(date: String): String = "day/$date"
    }
}

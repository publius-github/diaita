package app.diaita.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.diaita.AppContainer
import app.diaita.CalorieCounterApplication
import app.diaita.ui.addfood.AddFoodViewModel
import app.diaita.ui.daydetail.DayDetailViewModel
import app.diaita.ui.diary.DiaryViewModel
import app.diaita.ui.goals.GoalsViewModel
import app.diaita.ui.history.HistoryViewModel
import app.diaita.ui.presets.PresetViewModel
import app.diaita.ui.settings.SettingsViewModel

private fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalorieCounterApplication).container

/** Manual DI wiring for ViewModels — no Hilt/Koin, matches [AppContainer]'s scale. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DiaryViewModel(
                appContainer().diaryRepository,
                appContainer().goalRepository,
                appContainer().weightRepository,
            )
        }
        initializer {
            AddFoodViewModel(
                appContainer().foodRepository,
                appContainer().diaryRepository,
                appContainer().settingsRepository,
            )
        }
        initializer { PresetViewModel(appContainer().presetRepository, appContainer().foodRepository) }
        initializer { GoalsViewModel(appContainer().goalRepository) }
        initializer {
            SettingsViewModel(appContainer().settingsRepository, appContainer().exportRepository)
        }
        initializer {
            HistoryViewModel(
                appContainer().diaryRepository,
                appContainer().goalRepository,
                appContainer().weightRepository,
                appContainer().database.monthlyAggregateDao().getAll(),
            )
        }
        initializer { DayDetailViewModel(appContainer().diaryRepository, createSavedStateHandle()) }
    }
}

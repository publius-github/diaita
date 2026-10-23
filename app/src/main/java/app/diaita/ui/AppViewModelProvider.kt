package app.diaita.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.diaita.AppContainer
import app.diaita.DiaitaApplication
import app.diaita.ui.addfood.AddFoodViewModel
import app.diaita.ui.daydetail.DayDetailViewModel
import app.diaita.ui.diary.DiaryViewModel
import app.diaita.ui.goals.GoalWizardViewModel
import app.diaita.ui.goals.GoalsViewModel
import app.diaita.ui.history.HistoryViewModel
import app.diaita.ui.onboarding.OnboardingViewModel
import app.diaita.ui.presets.PresetViewModel
import app.diaita.ui.settings.SettingsViewModel

private fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DiaitaApplication).container

/** Manual DI wiring for ViewModels — no Hilt/Koin, matches [AppContainer]'s scale. */
object AppViewModelProvider {
    val Factory =
        viewModelFactory {
            initializer {
                DiaryViewModel(
                    appContainer().diaryRepository,
                    appContainer().goalRepository,
                    appContainer().weightRepository,
                    appContainer().waterRepository,
                    appContainer().settingsRepository,
                )
            }
            initializer {
                AddFoodViewModel(
                    appContainer().foodRepository,
                    appContainer().diaryRepository,
                    appContainer().settingsRepository,
                    createSavedStateHandle(),
                )
            }
            initializer {
                PresetViewModel(
                    appContainer().presetRepository,
                    appContainer().foodRepository,
                    appContainer().diaryRepository,
                    appContainer().settingsRepository,
                )
            }
            initializer {
                GoalsViewModel(
                    appContainer().goalRepository,
                    appContainer().settingsRepository,
                    appContainer().weightRepository,
                )
            }
            initializer {
                GoalWizardViewModel(
                    appContainer().goalRepository,
                    appContainer().settingsRepository,
                    appContainer().weightRepository,
                )
            }
            initializer {
                SettingsViewModel(
                    appContainer().settingsRepository,
                    appContainer().exportRepository,
                    appContainer().backupManager,
                )
            }
            initializer {
                HistoryViewModel(
                    appContainer().diaryRepository,
                    appContainer().goalRepository,
                    appContainer().weightRepository,
                    appContainer().settingsRepository,
                )
            }
            initializer {
                DayDetailViewModel(
                    appContainer().diaryRepository,
                    appContainer().settingsRepository,
                    createSavedStateHandle(),
                )
            }
            initializer {
                OnboardingViewModel(appContainer().settingsRepository)
            }
        }
}

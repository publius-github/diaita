package app.diaita.data.onboarding

import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.WeightRepository
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Decides whether the first-run onboarding should be shown. Only a genuinely fresh
 * install qualifies: any existing goal, weigh-in, or diary entry means the user predates
 * onboarding, so the flag is stamped and the flow never appears — including for users
 * upgrading from a version without onboarding.
 */
class OnboardingGate(
    private val settingsRepository: SettingsRepository,
    private val goalRepository: GoalRepository,
    private val weightRepository: WeightRepository,
    private val diaryRepository: DiaryRepository,
) {
    suspend fun shouldShowOnboarding(): Boolean {
        if (settingsRepository.onboardingDone.first()) return false
        val hasExistingData =
            goalRepository.getGoal().first() != null ||
                weightRepository.latest().first() != null ||
                diaryRepository.hasAnyEntries()
        if (hasExistingData) {
            settingsRepository.setOnboardingDone()
            return false
        }
        return true
    }
}

package app.diaita.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.settings.SettingsRepository
import app.diaita.ui.common.FoodSearchController
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AddFoodTab { FOODS, PRESETS }

data class AddFoodUiState(
    val selectedTab: AddFoodTab = AddFoodTab.FOODS,
    val foodPendingQuantity: FoodItem? = null,
    val justLogged: Boolean = false,
)

class AddFoodViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    /** Shared search/scan/manual logic; a picked food opens the quantity dialog to log it. */
    val foodSearch =
        FoodSearchController(
            scope = viewModelScope,
            foodRepository = foodRepository,
            diaryRepository = diaryRepository,
            settingsRepository = settingsRepository,
            onFoodReady = { food -> _uiState.update { it.copy(foodPendingQuantity = food) } },
        )

    fun onTabSelected(tab: AddFoodTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun dismissQuantityDialog() = _uiState.update { it.copy(foodPendingQuantity = null) }

    fun confirmQuantity(
        foodItemId: Long,
        quantityGrams: Double,
        mealSlot: MealSlot,
    ) {
        viewModelScope.launch {
            diaryRepository.logFood(foodItemId, quantityGrams, DateUtils.today(), mealSlot)
            _uiState.update { it.copy(foodPendingQuantity = null, justLogged = true) }
        }
    }

    fun consumeJustLogged() = _uiState.update { it.copy(justLogged = false) }
}

package app.diaita.ui.addfood

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.settings.DayPartBoundaries
import app.diaita.data.settings.SettingsRepository
import app.diaita.ui.common.FoodSearchController
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AddFoodTab { FOODS, PRESETS }

data class AddFoodUiState(
    val selectedTab: AddFoodTab = AddFoodTab.FOODS,
    val foodPendingQuantity: FoodItem? = null,
    /** Portion memory: the grams this food was last logged with, to prefill the quantity sheet. */
    val pendingQuantitySuggestion: Double? = null,
    val showQuickAdd: Boolean = false,
    val justLogged: Boolean = false,
)

class AddFoodViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** The diary date this screen logs to — set by the Diary's date navigation, today by default. */
    val logDate: String = savedStateHandle.get<String>("date").orEmpty().ifBlank { DateUtils.today() }

    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    /** User-configured day-part cut-offs, used to suggest the meal slot for a new log. */
    val dayPartBoundaries: StateFlow<DayPartBoundaries> =
        settingsRepository.dayPartBoundaries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayPartBoundaries())

    /** Shared search/scan/manual logic; a picked food opens the quantity dialog to log it. */
    val foodSearch =
        FoodSearchController(
            scope = viewModelScope,
            foodRepository = foodRepository,
            diaryRepository = diaryRepository,
            settingsRepository = settingsRepository,
            onFoodReady = ::openQuantityDialog,
        )

    /** Looks up the food's last logged portion first, so the sheet opens prefilled with it. */
    private fun openQuantityDialog(food: FoodItem) {
        viewModelScope.launch {
            val lastQuantity = diaryRepository.lastLoggedQuantity(food.id)
            _uiState.update {
                it.copy(foodPendingQuantity = food, pendingQuantitySuggestion = lastQuantity)
            }
        }
    }

    fun onTabSelected(tab: AddFoodTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun dismissQuantityDialog() =
        _uiState.update { it.copy(foodPendingQuantity = null, pendingQuantitySuggestion = null) }

    fun confirmQuantity(
        foodItemId: Long,
        quantityGrams: Double,
        mealSlot: MealSlot,
    ) {
        viewModelScope.launch {
            diaryRepository.logFood(foodItemId, quantityGrams, logDate, mealSlot)
            _uiState.update {
                it.copy(foodPendingQuantity = null, pendingQuantitySuggestion = null, justLogged = true)
            }
        }
    }

    fun showQuickAdd() = _uiState.update { it.copy(showQuickAdd = true) }

    fun dismissQuickAdd() = _uiState.update { it.copy(showQuickAdd = false) }

    /** Calorie-only log: a hidden QUICK food at [kcal]/100g logged as 100 g lands exactly [kcal]. */
    fun confirmQuickAdd(
        name: String,
        kcal: Double,
        mealSlot: MealSlot,
    ) {
        viewModelScope.launch {
            val foodId = foodRepository.addQuickFood(name, kcal)
            diaryRepository.logFood(foodId, 100.0, logDate, mealSlot)
            _uiState.update { it.copy(showQuickAdd = false, justLogged = true) }
        }
    }

    fun consumeJustLogged() = _uiState.update { it.copy(justLogged = false) }
}

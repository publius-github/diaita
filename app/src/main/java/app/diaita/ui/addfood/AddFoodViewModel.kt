package app.diaita.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.OffSearchOutcome
import app.diaita.data.settings.SettingsRepository
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AddFoodTab { FOODS, PRESETS }

data class AddFoodUiState(
    val selectedTab: AddFoodTab = AddFoodTab.FOODS,
    val showManualEntry: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FoodItem> = emptyList(),
    val recentFoods: List<FoodItem> = emptyList(),
    val isOnline: Boolean = true,
    val openFoodFactsEnabled: Boolean = true,
    val searchHadNetworkError: Boolean = false,
    val foodPendingQuantity: FoodItem? = null,
    val manualName: String = "",
    val manualCalories: String = "",
    val manualProtein: String = "",
    val manualFat: String = "",
    val manualCarbs: String = "",
    val manualHasError: Boolean = false,
    val justLogged: Boolean = false,
)

class AddFoodViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Local Room search — reactive, and Room re-emits automatically once OFF results get cached below.
        viewModelScope.launch {
            searchQueryFlow
                .flatMapLatest { query -> foodRepository.search(query) }
                .collect { results -> _uiState.update { it.copy(searchResults = results) } }
        }
        // Recently logged foods for the quick-access chip row.
        viewModelScope.launch {
            diaryRepository.recentFoods()
                .collect { foods -> _uiState.update { it.copy(recentFoods = foods) } }
        }
        // The persisted OpenFoodFacts toggle is the single source of truth (DataStore).
        viewModelScope.launch {
            settingsRepository.openFoodFactsEnabled
                .collect { enabled -> _uiState.update { it.copy(openFoodFactsEnabled = enabled) } }
        }
        // Debounced OpenFoodFacts lookup: skips the network call entirely while offline or disabled.
        viewModelScope.launch {
            searchQueryFlow
                .debounce(400)
                .collect { query ->
                    val online = foodRepository.isOnline()
                    _uiState.update { it.copy(isOnline = online) }
                    if (online && _uiState.value.openFoodFactsEnabled && query.isNotBlank()) {
                        val outcome = foodRepository.refreshFromOpenFoodFacts(query)
                        _uiState.update {
                            it.copy(searchHadNetworkError = outcome is OffSearchOutcome.NetworkError)
                        }
                    } else {
                        _uiState.update { it.copy(searchHadNetworkError = false) }
                    }
                }
        }
    }

    fun onTabSelected(tab: AddFoodTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun onShowManualEntryChanged(show: Boolean) = _uiState.update { it.copy(showManualEntry = show) }

    fun onOpenFoodFactsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOpenFoodFactsEnabled(enabled) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun onFoodSelected(food: FoodItem) = _uiState.update { it.copy(foodPendingQuantity = food) }

    fun dismissQuantityDialog() = _uiState.update { it.copy(foodPendingQuantity = null) }

    fun confirmQuantity(foodItemId: Long, quantityGrams: Double, mealSlot: MealSlot) {
        viewModelScope.launch {
            diaryRepository.logFood(foodItemId, quantityGrams, DateUtils.today(), mealSlot)
            _uiState.update { it.copy(foodPendingQuantity = null, justLogged = true) }
        }
    }

    fun onManualNameChanged(value: String) = _uiState.update { it.copy(manualName = value, manualHasError = false) }
    fun onManualCaloriesChanged(value: String) = _uiState.update { it.copy(manualCalories = value, manualHasError = false) }
    fun onManualProteinChanged(value: String) = _uiState.update { it.copy(manualProtein = value, manualHasError = false) }
    fun onManualFatChanged(value: String) = _uiState.update { it.copy(manualFat = value, manualHasError = false) }
    fun onManualCarbsChanged(value: String) = _uiState.update { it.copy(manualCarbs = value, manualHasError = false) }

    fun saveManualFood() {
        val state = _uiState.value
        val calories = state.manualCalories.toDoubleOrNull()
        val protein = state.manualProtein.toDoubleOrNull()
        val fat = state.manualFat.toDoubleOrNull()
        val carbs = state.manualCarbs.toDoubleOrNull()
        if (state.manualName.isBlank() || calories == null || protein == null || fat == null || carbs == null) {
            _uiState.update { it.copy(manualHasError = true) }
            return
        }
        viewModelScope.launch {
            val name = state.manualName.trim()
            val id = foodRepository.addManualFood(name, calories, protein, fat, carbs)
            _uiState.update {
                it.copy(
                    manualName = "",
                    manualCalories = "",
                    manualProtein = "",
                    manualFat = "",
                    manualCarbs = "",
                    showManualEntry = false,
                    // reuse the same quantity-dialog flow used by search results
                    foodPendingQuantity = FoodItem(
                        id = id,
                        name = name,
                        caloriesPer100g = calories,
                        proteinPer100g = protein,
                        fatPer100g = fat,
                        carbsPer100g = carbs,
                        source = FoodSource.MANUAL,
                    ),
                )
            }
        }
    }

    fun consumeJustLogged() = _uiState.update { it.copy(justLogged = false) }
}

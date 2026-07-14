package app.diaita.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.BarcodeLookupOutcome
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

enum class ScanError { NOT_FOUND, NETWORK }

data class AddFoodUiState(
    val selectedTab: AddFoodTab = AddFoodTab.FOODS,
    val showManualEntry: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FoodItem> = emptyList(),
    val recentFoods: List<FoodItem> = emptyList(),
    val isOnline: Boolean = true,
    val openFoodFactsEnabled: Boolean = true,
    val searchHadNetworkError: Boolean = false,
    val offSearching: Boolean = false,
    val scanError: ScanError? = null,
    val foodPendingQuantity: FoodItem? = null,
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
            diaryRepository
                .recentFoods()
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
                        _uiState.update { it.copy(offSearching = true) }
                        val outcome = foodRepository.refreshFromOpenFoodFacts(query)
                        _uiState.update {
                            it.copy(
                                offSearching = false,
                                searchHadNetworkError = outcome is OffSearchOutcome.NetworkError,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(searchHadNetworkError = false, offSearching = false) }
                    }
                }
        }
    }

    fun onTabSelected(tab: AddFoodTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun onShowManualEntryChanged(show: Boolean) = _uiState.update { it.copy(showManualEntry = show) }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, scanError = null) }
        searchQueryFlow.value = query
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanError = null, offSearching = true) }
            val outcome = foodRepository.lookupByBarcode(barcode)
            _uiState.update { state ->
                when (outcome) {
                    is BarcodeLookupOutcome.Found ->
                        state.copy(offSearching = false, foodPendingQuantity = outcome.food)
                    BarcodeLookupOutcome.NotFound ->
                        state.copy(offSearching = false, scanError = ScanError.NOT_FOUND)
                    BarcodeLookupOutcome.NetworkError ->
                        state.copy(offSearching = false, scanError = ScanError.NETWORK)
                }
            }
        }
    }

    fun onFoodSelected(food: FoodItem) = _uiState.update { it.copy(foodPendingQuantity = food) }

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

    /** Field state and validation live in [app.diaita.ui.common.ManualFoodForm]. */
    fun saveManualFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbsPer100g: Double,
    ) {
        viewModelScope.launch {
            val id = foodRepository.addManualFood(name, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g)
            _uiState.update {
                it.copy(
                    showManualEntry = false,
                    // reuse the same quantity-dialog flow used by search results
                    foodPendingQuantity =
                        FoodItem(
                            id = id,
                            name = name,
                            caloriesPer100g = caloriesPer100g,
                            proteinPer100g = proteinPer100g,
                            fatPer100g = fatPer100g,
                            carbsPer100g = carbsPer100g,
                            source = FoodSource.MANUAL,
                        ),
                )
            }
        }
    }

    fun consumeJustLogged() = _uiState.update { it.copy(justLogged = false) }
}

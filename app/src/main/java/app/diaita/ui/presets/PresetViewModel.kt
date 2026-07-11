package app.diaita.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.dao.PresetWithItems
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.PresetRepository
import app.diaita.data.repository.PresetSummary
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DraftPresetItem(
    val food: FoodItem,
    val quantityGrams: Double,
)

data class PresetEditUiState(
    /** Non-null when editing an existing preset rather than creating a new one. */
    val editingPresetId: Long? = null,
    val name: String = "",
    val searchQuery: String = "",
    val searchResults: List<FoodItem> = emptyList(),
    val foodPendingQuantity: FoodItem? = null,
    val draftItems: List<DraftPresetItem> = emptyList(),
    val hasError: Boolean = false,
    val saved: Boolean = false,
)

class PresetViewModel(
    private val presetRepository: PresetRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    val presets: StateFlow<List<PresetSummary>> =
        presetRepository
            .getAllSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _justLogged = MutableStateFlow(false)
    val justLogged: StateFlow<Boolean> = _justLogged.asStateFlow()

    private val _editState = MutableStateFlow(PresetEditUiState())
    val editState: StateFlow<PresetEditUiState> = _editState.asStateFlow()

    private val _undoEvent = MutableSharedFlow<PresetWithItems>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<PresetWithItems> = _undoEvent.asSharedFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQueryFlow
                .flatMapLatest { query -> foodRepository.search(query) }
                .collect { results -> _editState.update { it.copy(searchResults = results) } }
        }
    }

    fun logPreset(presetId: Long) {
        viewModelScope.launch {
            presetRepository.logPreset(presetId, DateUtils.today())
            _justLogged.value = true
        }
    }

    fun consumeJustLogged() {
        _justLogged.value = false
    }

    /** Deletes immediately; the emitted event lets the snackbar offer Undo. */
    fun deletePreset(preset: MealPreset) {
        viewModelScope.launch {
            val presetWithItems = presetRepository.getWithItems(preset.id)
            presetRepository.deletePreset(preset)
            if (presetWithItems != null) _undoEvent.emit(presetWithItems)
        }
    }

    fun undoDeletePreset(presetWithItems: PresetWithItems) {
        viewModelScope.launch { presetRepository.restorePreset(presetWithItems) }
    }

    // --- Create/edit draft ---

    /** Populates the draft from an existing preset. Safe to call repeatedly — only loads once per id. */
    fun loadForEdit(presetId: Long) {
        if (_editState.value.editingPresetId == presetId) return
        viewModelScope.launch {
            val presetWithItems = presetRepository.getWithItems(presetId) ?: return@launch
            val draftItems =
                presetWithItems.items.mapNotNull { item ->
                    foodRepository.getById(item.foodItemId)?.let { food ->
                        DraftPresetItem(food, item.quantityGrams)
                    }
                }
            _editState.update {
                it.copy(
                    editingPresetId = presetId,
                    name = presetWithItems.preset.name,
                    draftItems = draftItems,
                )
            }
        }
    }

    fun onNameChanged(name: String) = _editState.update { it.copy(name = name, hasError = false) }

    fun onSearchQueryChanged(query: String) {
        _editState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun onFoodSelected(food: FoodItem) = _editState.update { it.copy(foodPendingQuantity = food) }

    fun dismissQuantityDialog() = _editState.update { it.copy(foodPendingQuantity = null) }

    fun addDraftItem(
        food: FoodItem,
        quantityGrams: Double,
    ) {
        _editState.update {
            it.copy(
                draftItems = it.draftItems + DraftPresetItem(food, quantityGrams),
                foodPendingQuantity = null,
            )
        }
    }

    fun removeDraftItem(item: DraftPresetItem) {
        _editState.update { it.copy(draftItems = it.draftItems - item) }
    }

    fun savePreset() {
        val state = _editState.value
        if (state.name.isBlank() || state.draftItems.isEmpty()) {
            _editState.update { it.copy(hasError = true) }
            return
        }
        viewModelScope.launch {
            val items = state.draftItems.map { it.food.id to it.quantityGrams }
            if (state.editingPresetId != null) {
                presetRepository.updatePreset(state.editingPresetId, state.name.trim(), items)
            } else {
                presetRepository.createPreset(state.name.trim(), items)
            }
            _editState.update { PresetEditUiState(saved = true) }
        }
    }

    fun consumeSaved() = _editState.update { it.copy(saved = false) }
}

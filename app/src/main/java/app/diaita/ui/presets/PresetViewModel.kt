package app.diaita.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.diaita.data.db.dao.PresetWithItems
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.PresetRepository
import app.diaita.data.repository.PresetSummary
import app.diaita.data.settings.SettingsRepository
import app.diaita.ui.common.FoodSearchController
import app.diaita.util.DateUtils
import app.diaita.util.formatGramsForInput
import app.diaita.util.normalizedDecimalOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val mealSlot: MealSlot = MealSlot.BREAKFAST,
    /** Raw text of the cooked-weight field; blank = plain preset, a value = dish. */
    val cookedWeightText: String = "",
    /**
     * A new draft started from the Dishes tab. The cooked weight is then part of what a dish is,
     * so the field is shown and required; a draft started from the Custom tab never shows it and
     * always saves as a plain preset. Stays false while editing — an existing preset keeps the
     * optional field, which is also how a preset is promoted to a dish or demoted back.
     */
    val isDish: Boolean = false,
    val foodPendingQuantity: FoodItem? = null,
    val showQuickAdd: Boolean = false,
    val draftItems: List<DraftPresetItem> = emptyList(),
    /** Index into [draftItems] of the row being edited via the quantity sheet, if any. */
    val editingDraftIndex: Int? = null,
    val hasError: Boolean = false,
    val hasCookedWeightError: Boolean = false,
    val saved: Boolean = false,
)

class PresetViewModel(
    private val presetRepository: PresetRepository,
    private val foodRepository: FoodRepository,
    diaryRepository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val presets: StateFlow<List<PresetSummary>> =
        presetRepository
            .getAllSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _justLogged = MutableStateFlow(false)
    val justLogged: StateFlow<Boolean> = _justLogged.asStateFlow()

    private val _editState = MutableStateFlow(PresetEditUiState(mealSlot = DateUtils.defaultMealSlotNow()))
    val editState: StateFlow<PresetEditUiState> = _editState.asStateFlow()

    private val _undoEvent = MutableSharedFlow<PresetWithItems>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<PresetWithItems> = _undoEvent.asSharedFlow()

    private val _dishPendingPortion = MutableStateFlow<PresetSummary?>(null)
    val dishPendingPortion: StateFlow<PresetSummary?> = _dishPendingPortion.asStateFlow()

    init {
        // Align a fresh draft's default meal slot with the user's day-part settings. Guarded on
        // editingPresetId so a concurrently loaded existing preset never gets its slot clobbered.
        viewModelScope.launch {
            val boundaries = settingsRepository.dayPartBoundaries.first()
            _editState.update {
                if (it.editingPresetId == null) {
                    it.copy(mealSlot = DateUtils.defaultMealSlotNow(boundaries))
                } else {
                    it
                }
            }
        }
    }

    /** Same search/scan/manual pane as the Add-food screen; a picked food opens the quantity dialog. */
    val foodSearch =
        FoodSearchController(
            scope = viewModelScope,
            foodRepository = foodRepository,
            diaryRepository = diaryRepository,
            settingsRepository = settingsRepository,
            onFoodReady = { food -> _editState.update { it.copy(foodPendingQuantity = food) } },
        )

    fun logPreset(
        presetId: Long,
        date: String = DateUtils.today(),
    ) {
        viewModelScope.launch {
            val boundaries = settingsRepository.dayPartBoundaries.first()
            presetRepository.logPreset(
                presetId,
                date,
                fallbackSlot = DateUtils.defaultMealSlotNow(boundaries),
            )
            _justLogged.value = true
        }
    }

    /**
     * Dish rows can't log one-tap — the eaten portion is unknown — so the tap parks the dish
     * here and the screen shows the portion sheet; confirm inserts the scaled diary entries.
     */
    fun requestDishPortion(summary: PresetSummary) {
        _dishPendingPortion.value = summary
    }

    fun dismissDishPortion() {
        _dishPendingPortion.value = null
    }

    fun confirmDishPortion(
        eatenGrams: Double,
        date: String = DateUtils.today(),
    ) {
        val summary = _dishPendingPortion.value ?: return
        _dishPendingPortion.value = null
        viewModelScope.launch {
            val boundaries = settingsRepository.dayPartBoundaries.first()
            presetRepository.logDishPortion(
                presetId = summary.preset.id,
                eatenGrams = eatenGrams,
                logDate = date,
                fallbackSlot = DateUtils.defaultMealSlotNow(boundaries),
            )
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

    /** Marks a fresh draft as a dish (Add tapped on the Dishes tab). Never touches typed data. */
    fun startNewDraft(isDish: Boolean) = _editState.update { it.copy(isDish = isDish) }

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
                    mealSlot = presetWithItems.preset.mealSlot,
                    cookedWeightText =
                        presetWithItems.preset.cookedWeightGrams
                            ?.let(::formatGramsForInput)
                            .orEmpty(),
                    draftItems = draftItems,
                )
            }
        }
    }

    // A generous sanity cap only — list rows ellipsize long names, and the edit form shows
    // them in full, so the limit just keeps pathological input out of the database.
    fun onNameChanged(name: String) =
        _editState.update { it.copy(name = name.take(MAX_PRESET_NAME_LENGTH), hasError = false) }

    fun onMealSlotChanged(mealSlot: MealSlot) = _editState.update { it.copy(mealSlot = mealSlot) }

    fun onCookedWeightChanged(text: String) =
        _editState.update { it.copy(cookedWeightText = text, hasCookedWeightError = false) }

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
        foodSearch.clearSearch()
    }

    fun showQuickAdd() = _editState.update { it.copy(showQuickAdd = true) }

    fun dismissQuickAdd() = _editState.update { it.copy(showQuickAdd = false) }

    /**
     * Calorie-only draft item, mirroring the Add-food screen's quick add: a hidden QUICK food at
     * [kcal]/100g taken as 100 g lands exactly [kcal] in the preset.
     */
    fun confirmQuickAdd(
        name: String,
        kcal: Double,
    ) {
        viewModelScope.launch {
            val foodId = foodRepository.addQuickFood(name, kcal)
            val food = foodRepository.getById(foodId) ?: return@launch
            _editState.update {
                it.copy(
                    draftItems = it.draftItems + DraftPresetItem(food, 100.0),
                    showQuickAdd = false,
                )
            }
            foodSearch.clearSearch()
        }
    }

    /**
     * Stars/unstars a component right in the draft list, so e.g. a product that entered the
     * catalog via a barcode scan can be pinned for reuse while reviewing the preset. Persists
     * through the catalog row and mirrors the new state into the draft snapshot.
     */
    fun setDraftItemFavorite(
        index: Int,
        favorite: Boolean,
    ) {
        val item = _editState.value.draftItems.getOrNull(index) ?: return
        viewModelScope.launch {
            foodRepository.setFavorite(item.food.id, favorite)
            _editState.update { state ->
                val items = state.draftItems.toMutableList()
                items.getOrNull(index)?.let { items[index] = it.copy(food = it.food.copy(isFavorite = favorite)) }
                state.copy(draftItems = items)
            }
        }
    }

    fun removeDraftItem(index: Int) {
        _editState.update { it.copy(draftItems = it.draftItems.filterIndexed { i, _ -> i != index }) }
    }

    // Edits go by index, not by value: two identical lines of the same food would otherwise
    // be indistinguishable and always hit the first one.

    fun requestEditDraftItem(index: Int) = _editState.update { it.copy(editingDraftIndex = index) }

    fun cancelDraftItemEdit() = _editState.update { it.copy(editingDraftIndex = null) }

    fun confirmDraftItemEdit(quantityGrams: Double) {
        _editState.update { state ->
            val index = state.editingDraftIndex ?: return@update state
            val items = state.draftItems.toMutableList()
            items.getOrNull(index)?.let { items[index] = it.copy(quantityGrams = quantityGrams) }
            state.copy(draftItems = items, editingDraftIndex = null)
        }
    }

    fun savePreset() {
        val state = _editState.value
        if (state.name.isBlank() || state.draftItems.isEmpty()) {
            _editState.update { it.copy(hasError = true) }
            return
        }
        // Blank = plain preset; anything typed must parse to a positive number to save. A dish
        // draft has no blank option — without a cooked weight it would land in the Custom tab.
        val cookedWeightGrams = state.cookedWeightText.normalizedDecimalOrNull()?.takeIf { it > 0 }
        if (cookedWeightGrams == null && (state.isDish || state.cookedWeightText.isNotBlank())) {
            _editState.update { it.copy(hasCookedWeightError = true) }
            return
        }
        viewModelScope.launch {
            val items = state.draftItems.map { it.food.id to it.quantityGrams }
            if (state.editingPresetId != null) {
                presetRepository.updatePreset(
                    state.editingPresetId,
                    state.name.trim(),
                    state.mealSlot,
                    items,
                    cookedWeightGrams,
                )
            } else {
                presetRepository.createPreset(state.name.trim(), state.mealSlot, items, cookedWeightGrams)
            }
            val boundaries = settingsRepository.dayPartBoundaries.first()
            _editState.update {
                PresetEditUiState(
                    saved = true,
                    mealSlot = DateUtils.defaultMealSlotNow(boundaries),
                    isDish = state.isDish,
                )
            }
        }
    }

    fun consumeSaved() = _editState.update { it.copy(saved = false) }

    companion object {
        const val MAX_PRESET_NAME_LENGTH = 80
    }
}

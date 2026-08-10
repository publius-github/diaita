package app.diaita.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.repository.BarcodeLookupOutcome
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.OffSearchOutcome
import app.diaita.data.settings.SettingsRepository
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanError { NOT_FOUND, NETWORK }

/** Client-side source filter for search results. */
enum class SourceFilter {
    ALL,
    CATALOG,
    MINE,
    OPENFOODFACTS,
    ;

    fun matches(food: FoodItem): Boolean =
        when (this) {
            ALL -> true
            CATALOG -> food.source == FoodSource.SEED
            MINE -> food.source == FoodSource.MANUAL
            OPENFOODFACTS -> food.source == FoodSource.OPENFOODFACTS
        }
}

data class FoodSearchUiState(
    val searchQuery: String = "",
    val searchResults: List<FoodItem> = emptyList(),
    val recentFoods: List<FoodItem> = emptyList(),
    val favoriteFoods: List<FoodItem> = emptyList(),
    val sourceFilter: SourceFilter = SourceFilter.ALL,
    val isOnline: Boolean = true,
    val openFoodFactsEnabled: Boolean = true,
    val searchHadNetworkError: Boolean = false,
    val searchRateLimited: Boolean = false,
    val offSearching: Boolean = false,
    /** True while OFF reports more result pages for the current query. */
    val offHasMore: Boolean = false,
    val scanError: ScanError? = null,
    /** Search result being corrected in the edit form; null means no edit in progress. */
    val foodBeingEdited: FoodItem? = null,
    /** True when deleting [foodBeingEdited] was refused because history references it. */
    val deleteBlocked: Boolean = false,
) {
    val filteredResults: List<FoodItem> get() = searchResults.filter(sourceFilter::matches)
}

/**
 * Shared food discovery — local search, OpenFoodFacts lookup, barcode scan and manual entry —
 * used by both the Add-food screen and the preset editor's food picker so the two behave
 * identically. Owns its own search state; the host ViewModel decides what a picked food means
 * (log it, or add it to a preset draft) by handling [onFoodReady].
 */
class FoodSearchController(
    private val scope: CoroutineScope,
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
    private val onFoodReady: (FoodItem) -> Unit,
) {
    private val _state = MutableStateFlow(FoodSearchUiState())
    val state: StateFlow<FoodSearchUiState> = _state.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    /** The last OFF page fetched for the current query; load-more continues from here. */
    private var offPage = 1

    init {
        // Local Room search — reactive, and Room re-emits automatically once OFF results get cached below.
        scope.launch {
            searchQueryFlow
                .flatMapLatest { query -> foodRepository.search(query) }
                .collect { results -> _state.update { it.copy(searchResults = results) } }
        }
        // Recently logged foods for the quick-access chip row; the count is user-configurable.
        scope.launch {
            settingsRepository.recentFoodsCount
                .flatMapLatest { count -> diaryRepository.recentFoods(count) }
                .collect { foods -> _state.update { it.copy(recentFoods = foods) } }
        }
        // Pinned favorites get their own chip row above the recents.
        scope.launch {
            foodRepository.favorites().collect { foods ->
                _state.update { it.copy(favoriteFoods = foods) }
            }
        }
        // The persisted OpenFoodFacts toggle is the single source of truth (DataStore).
        scope.launch {
            settingsRepository.openFoodFactsEnabled
                .collect { enabled -> _state.update { it.copy(openFoodFactsEnabled = enabled) } }
        }
        // Debounced OpenFoodFacts lookup: skips the network call entirely while offline or disabled.
        scope.launch {
            searchQueryFlow
                .debounce(400)
                .collect { query ->
                    offPage = 1
                    val online = foodRepository.isOnline()
                    _state.update { it.copy(isOnline = online) }
                    if (online && _state.value.openFoodFactsEnabled && query.isNotBlank()) {
                        refreshFromOff(query, page = 1)
                    } else {
                        _state.update {
                            it.copy(
                                searchHadNetworkError = false,
                                searchRateLimited = false,
                                offSearching = false,
                                offHasMore = false,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun refreshFromOff(
        query: String,
        page: Int,
    ) {
        _state.update { it.copy(offSearching = true) }
        val outcome = foodRepository.refreshFromOpenFoodFacts(query, page)
        _state.update {
            it.copy(
                offSearching = false,
                searchHadNetworkError = outcome is OffSearchOutcome.NetworkError,
                searchRateLimited = outcome is OffSearchOutcome.RateLimited,
                offHasMore = (outcome as? OffSearchOutcome.Success)?.hasMore ?: false,
            )
        }
        if (outcome is OffSearchOutcome.Success) offPage = outcome.page
    }

    /** Fetches the next OpenFoodFacts result page for the current query (result 21+). */
    fun loadMoreFromOff() {
        val query = _state.value.searchQuery
        if (query.isBlank() || _state.value.offSearching) return
        scope.launch { refreshFromOff(query, page = offPage + 1) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query, scanError = null) }
        searchQueryFlow.value = query
    }

    fun onSourceFilterChanged(filter: SourceFilter) = _state.update { it.copy(sourceFilter = filter) }

    fun onFoodSelected(food: FoodItem) = onFoodReady(food)

    fun onToggleFavorite(food: FoodItem) {
        scope.launch { foodRepository.setFavorite(food.id, !food.isFavorite) }
    }

    fun onBarcodeScanned(barcode: String) {
        scope.launch {
            _state.update { it.copy(scanError = null, offSearching = true) }
            when (val outcome = foodRepository.lookupByBarcode(barcode)) {
                is BarcodeLookupOutcome.Found -> {
                    _state.update { it.copy(offSearching = false) }
                    onFoodReady(outcome.food)
                }
                BarcodeLookupOutcome.NotFound ->
                    _state.update { it.copy(offSearching = false, scanError = ScanError.NOT_FOUND) }
                BarcodeLookupOutcome.NetworkError ->
                    _state.update { it.copy(offSearching = false, scanError = ScanError.NETWORK) }
            }
        }
    }

    /**
     * Persists a manually entered food and hands it straight to [onFoodReady], so it flows into
     * the same quantity dialog as a search pick. Field state/validation live in [ManualFoodForm].
     */
    fun saveManualFood(draft: FoodDraft) {
        scope.launch {
            val food = draft.toFoodItem()
            val id = foodRepository.addFood(food)
            onFoodReady(food.copy(id = id))
        }
    }

    fun onEditFood(food: FoodItem) = _state.update { it.copy(foodBeingEdited = food, deleteBlocked = false) }

    fun dismissEdit() = _state.update { it.copy(foodBeingEdited = null, deleteBlocked = false) }

    /**
     * Applies a correction to the food currently in the edit form. Only the user-visible
     * nutrition fields change — id, source, seedKey, barcode etc. are preserved, so diary
     * entries and future seed-catalog syncs keep working against the same row.
     */
    fun saveEditedFood(draft: FoodDraft) {
        val food = _state.value.foodBeingEdited ?: return
        scope.launch {
            foodRepository.updateFood(
                food.copy(
                    name = draft.name,
                    caloriesPer100g = draft.caloriesPer100g,
                    proteinPer100g = draft.proteinPer100g,
                    fatPer100g = draft.fatPer100g,
                    carbsPer100g = draft.carbsPer100g,
                    fiberPer100g = draft.fiberPer100g,
                    sugarPer100g = draft.sugarPer100g,
                    saltPer100g = draft.saltPer100g,
                    defaultServingGrams = draft.defaultServingGrams,
                    gramsPerMilliliter = draft.gramsPerMilliliter,
                    gramsPerPiece = draft.gramsPerPiece,
                ),
            )
            _state.update { it.copy(foodBeingEdited = null, deleteBlocked = false) }
        }
    }

    /** Deletes the food open in the edit form; refused (flagged) when history references it. */
    fun deleteEditedFood() {
        val food = _state.value.foodBeingEdited ?: return
        scope.launch {
            if (foodRepository.deleteFood(food)) {
                _state.update { it.copy(foodBeingEdited = null, deleteBlocked = false) }
            } else {
                _state.update { it.copy(deleteBlocked = true) }
            }
        }
    }

    fun clearSearch() = onSearchQueryChanged("")
}

private fun FoodDraft.toFoodItem(): FoodItem =
    FoodItem(
        name = name,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        fatPer100g = fatPer100g,
        carbsPer100g = carbsPer100g,
        fiberPer100g = fiberPer100g,
        sugarPer100g = sugarPer100g,
        saltPer100g = saltPer100g,
        defaultServingGrams = defaultServingGrams,
        gramsPerMilliliter = gramsPerMilliliter,
        gramsPerPiece = gramsPerPiece,
        source = FoodSource.MANUAL,
    )

/**
 * Wires up the ZXing barcode scanner once and returns a lambda that launches it. Shared so the
 * Add-food and preset pickers scan with identical options (product codes, no beep).
 */
@Composable
fun rememberFoodBarcodeScanner(onScanned: (String) -> Unit): () -> Unit {
    val scanPrompt = stringResource(R.string.scan_prompt)
    val launcher =
        rememberLauncherForActivityResult(ScanContract()) { result ->
            result.contents?.let(onScanned)
        }
    return {
        launcher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
                .setPrompt(scanPrompt)
                .setBeepEnabled(false),
        )
    }
}

/**
 * The full food-discovery pane: search field, favorite/recent chips, source filter,
 * offline/error banners, results list with favorite toggles, an OFF load-more action and a
 * manual-entry fallback. Rendered inside whatever Scaffold the host screen provides, so both
 * the Add-food tab and the preset picker present the exact same body.
 */
@Composable
fun FoodSearchContent(
    state: FoodSearchUiState,
    manualSaveLabel: String,
    onSearchQueryChanged: (String) -> Unit,
    onFoodSelected: (FoodItem) -> Unit,
    onManualSave: (FoodDraft) -> Unit,
    onEditFood: (FoodItem) -> Unit,
    onEditSave: (FoodDraft) -> Unit,
    onEditDismiss: () -> Unit,
    onToggleFavorite: (FoodItem) -> Unit,
    onSourceFilterChanged: (SourceFilter) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteFood: () -> Unit,
    modifier: Modifier = Modifier,
    /** Calorie-only quick logging; only the Add-food screen offers it (null hides the button). */
    onQuickAdd: (() -> Unit)? = null,
) {
    var showManualEntry by rememberSaveable { mutableStateOf(false) }

    // Editing takes over the whole pane, same as manual entry — it's the same form, prefilled.
    state.foodBeingEdited?.let { food ->
        ManualFoodForm(
            saveLabel = stringResource(R.string.save_changes),
            onBack = onEditDismiss,
            onSave = onEditSave,
            modifier = modifier,
            initial = food,
            onDelete = onDeleteFood,
            deleteBlocked = state.deleteBlocked,
        )
        return
    }

    if (showManualEntry) {
        ManualFoodForm(
            saveLabel = manualSaveLabel,
            onBack = { showManualEntry = false },
            onSave = { draft ->
                onManualSave(draft)
                showManualEntry = false
            },
            modifier = modifier,
        )
        return
    }

    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text(stringResource(R.string.search_foods)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.offSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            // No autofocus on purpose: most logs come from the recent-food chips below, so the
            // keyboard must not pop up until the user actually taps the search field.
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.searchQuery.isBlank() && state.favoriteFoods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.favorite_foods), style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.favoriteFoods, key = { it.id }) { food ->
                    AssistChip(
                        onClick = { onFoodSelected(food) },
                        label = { Text(food.name) },
                        leadingIcon = {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
        }
        if (state.searchQuery.isBlank() && state.recentFoods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.recent_foods), style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.recentFoods, key = { it.id }) { food ->
                    AssistChip(
                        onClick = { onFoodSelected(food) },
                        label = { Text(food.name) },
                    )
                }
            }
        }
        if (state.searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SourceFilter.entries) { filter ->
                    FilterChip(
                        selected = state.sourceFilter == filter,
                        onClick = { onSourceFilterChanged(filter) },
                        label = {
                            Text(
                                stringResource(
                                    when (filter) {
                                        SourceFilter.ALL -> R.string.filter_all
                                        SourceFilter.CATALOG -> R.string.filter_catalog
                                        SourceFilter.MINE -> R.string.filter_mine
                                        SourceFilter.OPENFOODFACTS -> R.string.filter_off
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (state.openFoodFactsEnabled && !state.isOnline) {
            Text(
                stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.searchHadNetworkError) {
            Text(
                stringResource(R.string.search_error_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.searchRateLimited) {
            Text(
                stringResource(R.string.search_rate_limited),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.scanError?.let { error ->
            Text(
                stringResource(
                    when (error) {
                        ScanError.NOT_FOUND -> R.string.scan_not_found
                        ScanError.NETWORK -> R.string.search_error_off
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!state.searchHadNetworkError &&
            !state.offSearching &&
            state.searchQuery.isNotBlank() &&
            state.filteredResults.isEmpty()
        ) {
            Text(
                stringResource(R.string.no_matches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.filteredResults, key = { it.id }) { food ->
                CompactRowCard(
                    title = listOfNotNull(food.name, food.brand).joinToString(" — "),
                    subtitle = stringResource(R.string.kcal_per_100g, food.caloriesPer100g.toInt()),
                    onClick = { onFoodSelected(food) },
                ) {
                    IconButton(onClick = { onToggleFavorite(food) }) {
                        Icon(
                            if (food.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription =
                                stringResource(
                                    if (food.isFavorite) R.string.unfavorite_food else R.string.favorite_food,
                                ),
                        )
                    }
                    // Catalog corrections (wrong seeded/OFF numbers) happen right where the
                    // mistake is spotted — in the search results.
                    IconButton(onClick = { onEditFood(food) }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_food),
                        )
                    }
                }
            }
            if (state.offHasMore && state.searchQuery.isNotBlank() && state.isOnline) {
                item(key = "load_more") {
                    TextButton(
                        onClick = onLoadMore,
                        enabled = !state.offSearching,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.load_more_results))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showManualEntry = true },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.add_manually))
            }
            onQuickAdd?.let { quickAdd ->
                OutlinedButton(
                    onClick = quickAdd,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.quick_add))
                }
            }
        }
    }
}

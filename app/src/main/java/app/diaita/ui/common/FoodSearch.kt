package app.diaita.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import app.diaita.R
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.repository.BarcodeLookupOutcome
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.OffSearchOutcome
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanError { NOT_FOUND, NETWORK }

data class FoodSearchUiState(
    val searchQuery: String = "",
    val searchResults: List<FoodItem> = emptyList(),
    val recentFoods: List<FoodItem> = emptyList(),
    val isOnline: Boolean = true,
    val openFoodFactsEnabled: Boolean = true,
    val searchHadNetworkError: Boolean = false,
    val offSearching: Boolean = false,
    val scanError: ScanError? = null,
    /** Search result being corrected in the edit form; null means no edit in progress. */
    val foodBeingEdited: FoodItem? = null,
)

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
                    val online = foodRepository.isOnline()
                    _state.update { it.copy(isOnline = online) }
                    if (online && _state.value.openFoodFactsEnabled && query.isNotBlank()) {
                        _state.update { it.copy(offSearching = true) }
                        val outcome = foodRepository.refreshFromOpenFoodFacts(query)
                        _state.update {
                            it.copy(
                                offSearching = false,
                                searchHadNetworkError = outcome is OffSearchOutcome.NetworkError,
                            )
                        }
                    } else {
                        _state.update { it.copy(searchHadNetworkError = false, offSearching = false) }
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query, scanError = null) }
        searchQueryFlow.value = query
    }

    fun onFoodSelected(food: FoodItem) = onFoodReady(food)

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
    fun saveManualFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbsPer100g: Double,
    ) {
        scope.launch {
            val id = foodRepository.addManualFood(name, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g)
            onFoodReady(
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

    fun onEditFood(food: FoodItem) = _state.update { it.copy(foodBeingEdited = food) }

    fun dismissEdit() = _state.update { it.copy(foodBeingEdited = null) }

    /**
     * Applies a correction to the food currently in the edit form. Only the user-visible
     * nutrition fields change — id, source, seedKey, barcode etc. are preserved, so diary
     * entries and future seed-catalog syncs keep working against the same row.
     */
    fun saveEditedFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbsPer100g: Double,
    ) {
        val food = _state.value.foodBeingEdited ?: return
        scope.launch {
            foodRepository.updateFood(
                food.copy(
                    name = name,
                    caloriesPer100g = caloriesPer100g,
                    proteinPer100g = proteinPer100g,
                    fatPer100g = fatPer100g,
                    carbsPer100g = carbsPer100g,
                    editedByUser = true,
                ),
            )
            _state.update { it.copy(foodBeingEdited = null) }
        }
    }

    fun clearSearch() = onSearchQueryChanged("")
}

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
 * The full food-discovery pane: search field, recent-food chips, offline/error banners, results
 * list and a manual-entry fallback. Rendered inside whatever Scaffold the host screen provides,
 * so both the Add-food tab and the preset picker present the exact same body.
 */
@Composable
fun FoodSearchContent(
    state: FoodSearchUiState,
    manualSaveLabel: String,
    onSearchQueryChanged: (String) -> Unit,
    onFoodSelected: (FoodItem) -> Unit,
    onManualSave: (String, Double, Double, Double, Double) -> Unit,
    onEditFood: (FoodItem) -> Unit,
    onEditSave: (String, Double, Double, Double, Double) -> Unit,
    onEditDismiss: () -> Unit,
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
        )
        return
    }

    if (showManualEntry) {
        ManualFoodForm(
            saveLabel = manualSaveLabel,
            onBack = { showManualEntry = false },
            onSave = { name, calories, protein, fat, carbs ->
                onManualSave(name, calories, protein, fat, carbs)
                showManualEntry = false
            },
            modifier = modifier,
        )
        return
    }

    val focusRequester = remember { FocusRequester() }
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
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
            state.searchResults.isEmpty()
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.searchResults, key = { it.id }) { food ->
                ListItem(
                    headlineContent = { Text(food.name) },
                    supportingContent = {
                        Text(stringResource(R.string.kcal_per_100g, food.caloriesPer100g.toInt()))
                    },
                    trailingContent = {
                        // Catalog corrections (wrong seeded/OFF numbers) happen right where the
                        // mistake is spotted — in the search results.
                        IconButton(onClick = { onEditFood(food) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_food),
                            )
                        }
                    },
                    modifier = Modifier.clickable { onFoodSelected(food) },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { showManualEntry = true },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.add_manually))
            }
            onQuickAdd?.let { quickAdd ->
                TextButton(
                    onClick = quickAdd,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.quick_add))
                }
            }
        }
    }
}

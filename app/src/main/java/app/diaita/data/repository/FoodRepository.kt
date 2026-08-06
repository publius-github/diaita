package app.diaita.data.repository

import app.diaita.data.connectivity.ConnectivityObserver
import app.diaita.data.db.dao.FoodItemDao
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.remote.OpenFoodFactsApi
import app.diaita.data.remote.dto.OffProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.net.HttpURLConnection

/** Outcome of an [FoodRepository.refreshFromOpenFoodFacts] call, so the UI can tell "no matches" apart from a failed request. */
sealed class OffSearchOutcome {
    data object Success : OffSearchOutcome()

    data object NetworkError : OffSearchOutcome()
}

/** Outcome of a barcode scan lookup, so the UI can offer the right follow-up for each case. */
sealed class BarcodeLookupOutcome {
    data class Found(
        val food: FoodItem,
    ) : BarcodeLookupOutcome()

    data object NotFound : BarcodeLookupOutcome()

    data object NetworkError : BarcodeLookupOutcome()
}

class FoodRepository(
    private val foodItemDao: FoodItemDao,
    private val openFoodFactsApi: OpenFoodFactsApi,
    private val connectivityObserver: ConnectivityObserver,
) {
    /** Local Room search — reactive, and automatically re-emits once [refreshFromOpenFoodFacts] caches new rows. */
    fun search(query: String): Flow<List<FoodItem>> =
        foodItemDao.search(query, since = System.currentTimeMillis() - FRECENCY_WINDOW_MILLIS)

    suspend fun getById(id: Long): FoodItem? = foodItemDao.getById(id)

    /**
     * Persists a user correction to a catalog entry (any source). Diary entries reference the row
     * by id and store only grams, so past logs recalculate against the fixed values — which is the
     * point: the old numbers were wrong.
     */
    suspend fun updateFood(food: FoodItem) = foodItemDao.update(food)

    suspend fun isOnline(): Boolean = connectivityObserver.isOnline.first()

    /**
     * Queries OpenFoodFacts and write-through caches any new results into Room, keyed by [OffProduct.code].
     * Callers don't need the cached rows directly — [search]'s Flow re-emits automatically once new rows land,
     * since Room invalidates the query on table writes. The return value only distinguishes success from a
     * failed request, so the UI can show "couldn't reach OpenFoodFacts" instead of silently looking like
     * "no matches found".
     */
    suspend fun refreshFromOpenFoodFacts(query: String): OffSearchOutcome {
        if (query.isBlank()) return OffSearchOutcome.Success
        return runCatching { openFoodFactsApi.searchByName(query = query) }
            .onSuccess { response -> response.products.forEach { product -> cacheProductIfNew(product) } }
            .fold(
                onSuccess = { OffSearchOutcome.Success },
                onFailure = { OffSearchOutcome.NetworkError },
            )
    }

    private suspend fun cacheProductIfNew(product: OffProduct) {
        val offId = product.code ?: return
        val name = product.productName?.takeIf { it.isNotBlank() } ?: return
        val calories = product.nutriments?.energyKcal100g ?: return
        if (foodItemDao.getByOffId(offId) != null) return

        foodItemDao.insert(
            FoodItem(
                name = name,
                brand = firstBrand(product.brands),
                barcode = offId,
                caloriesPer100g = calories,
                proteinPer100g = product.nutriments.proteins100g ?: 0.0,
                fatPer100g = product.nutriments.fat100g ?: 0.0,
                carbsPer100g = product.nutriments.carbohydrates100g ?: 0.0,
                source = FoodSource.OPENFOODFACTS,
                offId = offId,
            ),
        )
    }

    private fun firstBrand(brands: String?): String? =
        brands
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    /**
     * Resolves a scanned barcode to a food: local cache first (any source), then the
     * OpenFoodFacts product API with write-through caching. Unknown barcodes surface as
     * [BarcodeLookupOutcome.NotFound] whether OFF reports them via `status = 0` or HTTP 404.
     */
    suspend fun lookupByBarcode(barcode: String): BarcodeLookupOutcome {
        foodItemDao.getByBarcode(barcode)?.let { return BarcodeLookupOutcome.Found(it) }
        return runCatching { openFoodFactsApi.productByBarcode(barcode) }
            .fold(
                onSuccess = { response ->
                    val product = response.product
                    val name = product?.productName?.takeIf { it.isNotBlank() }
                    val calories = product?.nutriments?.energyKcal100g
                    if (response.status != 1 || product == null || name == null || calories == null) {
                        BarcodeLookupOutcome.NotFound
                    } else {
                        val id =
                            foodItemDao.insert(
                                FoodItem(
                                    name = name,
                                    brand = firstBrand(product.brands),
                                    barcode = barcode,
                                    caloriesPer100g = calories,
                                    proteinPer100g = product.nutriments.proteins100g ?: 0.0,
                                    fatPer100g = product.nutriments.fat100g ?: 0.0,
                                    carbsPer100g = product.nutriments.carbohydrates100g ?: 0.0,
                                    source = FoodSource.OPENFOODFACTS,
                                    offId = product.code ?: barcode,
                                ),
                            )
                        BarcodeLookupOutcome.Found(checkNotNull(foodItemDao.getById(id)))
                    }
                },
                onFailure = { error ->
                    if (error is HttpException && error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                        BarcodeLookupOutcome.NotFound
                    } else {
                        BarcodeLookupOutcome.NetworkError
                    }
                },
            )
    }

    suspend fun addManualFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbsPer100g: Double,
    ): Long =
        foodItemDao.insert(
            FoodItem(
                name = name,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                fatPer100g = fatPer100g,
                carbsPer100g = carbsPer100g,
                source = FoodSource.MANUAL,
            ),
        )

    /**
     * Backing row for a calorie-only quick log. Stored at [kcal] per 100 g so a 100 g diary
     * entry yields exactly [kcal]; the QUICK source keeps it out of search and recents.
     */
    suspend fun addQuickFood(
        name: String,
        kcal: Double,
    ): Long =
        foodItemDao.insert(
            FoodItem(
                name = name,
                caloriesPer100g = kcal,
                proteinPer100g = 0.0,
                fatPer100g = 0.0,
                carbsPer100g = 0.0,
                source = FoodSource.QUICK,
            ),
        )

    companion object {
        /** Logging within the last 90 days counts toward the search ranking. */
        private const val FRECENCY_WINDOW_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}

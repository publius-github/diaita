package app.diaita.data.repository

import app.diaita.data.connectivity.ConnectivityObserver
import app.diaita.data.db.dao.FoodItemDao
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.remote.OpenFoodFactsApi
import app.diaita.data.remote.dto.OffSearchHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Outcome of an [FoodRepository.refreshFromOpenFoodFacts] call, so the UI can tell "no matches" apart from a failed request. */
sealed class OffSearchOutcome {
    data object Success : OffSearchOutcome()
    data object NetworkError : OffSearchOutcome()
}

class FoodRepository(
    private val foodItemDao: FoodItemDao,
    private val openFoodFactsApi: OpenFoodFactsApi,
    private val connectivityObserver: ConnectivityObserver,
) {
    /** Local Room search — reactive, and automatically re-emits once [refreshFromOpenFoodFacts] caches new rows. */
    fun search(query: String): Flow<List<FoodItem>> = foodItemDao.search(query)

    suspend fun getById(id: Long): FoodItem? = foodItemDao.getById(id)

    suspend fun isOnline(): Boolean = connectivityObserver.isOnline.first()

    /**
     * Queries OpenFoodFacts and write-through caches any new results into Room, keyed by [OffSearchHit.code].
     * Callers don't need the cached rows directly — [search]'s Flow re-emits automatically once new rows land,
     * since Room invalidates the query on table writes. The return value only distinguishes success from a
     * failed request, so the UI can show "couldn't reach OpenFoodFacts" instead of silently looking like
     * "no matches found".
     */
    suspend fun refreshFromOpenFoodFacts(query: String): OffSearchOutcome {
        if (query.isBlank()) return OffSearchOutcome.Success
        return runCatching { openFoodFactsApi.searchByName(query = query) }
            .onSuccess { response -> response.hits.forEach { hit -> cacheHitIfNew(hit) } }
            .fold(
                onSuccess = { OffSearchOutcome.Success },
                onFailure = { OffSearchOutcome.NetworkError },
            )
    }

    private suspend fun cacheHitIfNew(hit: OffSearchHit) {
        val offId = hit.code ?: return
        val name = hit.productName?.takeIf { it.isNotBlank() } ?: return
        val calories = hit.nutriments?.energyKcal100g ?: return
        if (foodItemDao.getByOffId(offId) != null) return

        foodItemDao.insert(
            FoodItem(
                name = name,
                brand = hit.brands?.firstOrNull(),
                barcode = offId,
                caloriesPer100g = calories,
                proteinPer100g = hit.nutriments.proteins100g ?: 0.0,
                fatPer100g = hit.nutriments.fat100g ?: 0.0,
                carbsPer100g = hit.nutriments.carbohydrates100g ?: 0.0,
                source = FoodSource.OPENFOODFACTS,
                offId = offId,
            ),
        )
    }

    suspend fun addManualFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbsPer100g: Double,
    ): Long = foodItemDao.insert(
        FoodItem(
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            fatPer100g = fatPer100g,
            carbsPer100g = carbsPer100g,
            source = FoodSource.MANUAL,
        ),
    )
}

package app.diaita.data.repository

import app.diaita.data.connectivity.ConnectivityObserver
import app.diaita.data.db.dao.FoodItemDao
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.remote.OpenFoodFactsApi
import app.diaita.data.remote.dto.OffProduct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.util.Locale

/** Outcome of an [FoodRepository.refreshFromOpenFoodFacts] call, so the UI can tell "no matches" apart from a failed request. */
sealed class OffSearchOutcome {
    /** The request went through; [hasMore] says whether another page is available upstream. */
    data class Success(
        val totalCount: Int,
        val page: Int,
        val pageSize: Int,
    ) : OffSearchOutcome() {
        val hasMore: Boolean get() = pageSize > 0 && page * pageSize < totalCount
    }

    data object NetworkError : OffSearchOutcome()

    /** OFF answered 429 — asked to slow down; retrying immediately would make it worse. */
    data object RateLimited : OffSearchOutcome()
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

    fun favorites(): Flow<List<FoodItem>> = foodItemDao.favorites()

    suspend fun setFavorite(
        id: Long,
        favorite: Boolean,
    ) = foodItemDao.setFavorite(id, favorite)

    suspend fun getById(id: Long): FoodItem? = foodItemDao.getById(id)

    /**
     * Persists a user correction to a catalog entry (any source). Diary entries reference the row
     * by id and store only grams, so past logs recalculate against the fixed values — which is the
     * point: the old numbers were wrong. The edited flag is forced here (not at call sites) so the
     * seed-sync-never-clobbers-corrections invariant cannot be bypassed by a forgetful caller.
     */
    suspend fun updateFood(food: FoodItem) = foodItemDao.update(food.copy(editedByUser = true))

    /**
     * Deletes a catalog food if nothing references it. Returns false when diary entries or
     * preset items still point at the row (the FK is RESTRICT — history must stay intact).
     */
    suspend fun deleteFood(food: FoodItem): Boolean {
        if (foodItemDao.referenceCount(food.id) > 0) return false
        foodItemDao.delete(food)
        return true
    }

    suspend fun isOnline(): Boolean = connectivityObserver.isOnline.first()

    /**
     * Queries OpenFoodFacts and write-through caches any new results into Room, keyed by [OffProduct.code].
     * Callers don't need the cached rows directly — [search]'s Flow re-emits automatically once new rows land,
     * since Room invalidates the query on table writes. Already-cached products get refreshed from the fresh
     * payload once they are older than [CACHE_REFRESH_MILLIS] (unless the user edited them), so upstream OFF
     * corrections eventually arrive.
     */
    suspend fun refreshFromOpenFoodFacts(
        query: String,
        page: Int = 1,
    ): OffSearchOutcome {
        if (query.isBlank()) return OffSearchOutcome.Success(0, 1, 0)
        return try {
            val response =
                openFoodFactsApi.searchByName(
                    query = query,
                    page = page,
                    languageCode = Locale.getDefault().language.takeIf { it.isNotBlank() },
                )
            response.products.forEach { product -> cacheProduct(product) }
            OffSearchOutcome.Success(
                totalCount = response.count,
                page = response.page,
                pageSize = response.pageSize.takeIf { it > 0 } ?: response.products.size,
            )
        } catch (e: CancellationException) {
            // A cancelled search (the user typed another character) is not a network error.
            throw e
        } catch (e: HttpException) {
            if (e.code() == HTTP_TOO_MANY_REQUESTS) OffSearchOutcome.RateLimited else OffSearchOutcome.NetworkError
        } catch (e: Exception) {
            OffSearchOutcome.NetworkError
        }
    }

    private suspend fun cacheProduct(product: OffProduct) {
        val offId = product.code ?: return
        val name = product.productName?.takeIf { it.isNotBlank() } ?: return
        val calories = product.energyKcal ?: return

        val existing = foodItemDao.getByOffId(offId)
        if (existing == null) {
            // The unique offId index makes this a silent no-op if a concurrent search won the race.
            foodItemDao.insertOrIgnore(mapProduct(product, name, calories))
        } else if (!existing.editedByUser && existing.isStale()) {
            foodItemDao.update(
                mapProduct(product, name, calories).copy(
                    id = existing.id,
                    barcode = existing.barcode,
                    isFavorite = existing.isFavorite,
                    createdAt = existing.createdAt,
                ),
            )
        }
    }

    private fun FoodItem.isStale(): Boolean =
        (lastRefreshedAt ?: createdAt) < System.currentTimeMillis() - CACHE_REFRESH_MILLIS

    private fun mapProduct(
        product: OffProduct,
        name: String,
        calories: Double,
    ): FoodItem =
        FoodItem(
            name = name,
            brand = firstBrand(product.brands),
            barcode = product.code,
            caloriesPer100g = calories,
            proteinPer100g = product.nutriments?.proteins100g ?: 0.0,
            fatPer100g = product.nutriments?.fat100g ?: 0.0,
            carbsPer100g = product.nutriments?.carbohydrates100g ?: 0.0,
            fiberPer100g = product.nutriments?.fiber100g,
            sugarPer100g = product.nutriments?.sugars100g,
            saltPer100g = product.nutriments?.salt100g,
            defaultServingGrams = product.servingQuantity?.takeIf { it > 0 },
            source = FoodSource.OPENFOODFACTS,
            offId = product.code,
            lastRefreshedAt = System.currentTimeMillis(),
        )

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
        return try {
            val response = openFoodFactsApi.productByBarcode(barcode)
            val product = response.product
            val name = product?.productName?.takeIf { it.isNotBlank() }
            val calories = product?.energyKcal
            if (response.status != 1 || product == null || name == null || calories == null) {
                BarcodeLookupOutcome.NotFound
            } else {
                foodItemDao.insertOrIgnore(
                    mapProduct(product, name, calories).copy(barcode = barcode, offId = product.code ?: barcode),
                )
                val cached =
                    foodItemDao.getByBarcode(barcode)
                        ?: foodItemDao.getByOffId(product.code ?: barcode)
                cached?.let { BarcodeLookupOutcome.Found(it) } ?: BarcodeLookupOutcome.NotFound
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                BarcodeLookupOutcome.NotFound
            } else {
                BarcodeLookupOutcome.NetworkError
            }
        } catch (e: Exception) {
            BarcodeLookupOutcome.NetworkError
        }
    }

    /** Persists a user-authored catalog food (manual entry form). */
    suspend fun addFood(food: FoodItem): Long = foodItemDao.insert(food)

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

        /** Cached OFF products older than this get refreshed from the next search payload. */
        private const val CACHE_REFRESH_MILLIS = 30L * 24 * 60 * 60 * 1000

        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

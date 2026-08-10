package app.diaita.data.repository

import androidx.room.withTransaction
import app.diaita.data.db.AppDatabase
import app.diaita.data.export.AppCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** What a CSV import actually changed — shown to the user instead of a silent "done". */
data class ImportSummary(
    val foodsCreated: Int,
    val diaryRowsInserted: Int,
    val diaryRowsSkipped: Int,
    val weightsUpserted: Int,
    val presetsCreated: Int,
)

sealed class ImportOutcome {
    data class Success(
        val summary: ImportSummary,
    ) : ImportOutcome()

    data object NotADiaitaExport : ImportOutcome()

    data class Malformed(
        val line: Int,
    ) : ImportOutcome()
}

class ExportRepository(
    private val database: AppDatabase,
) {
    /**
     * Full-fidelity CSV of everything user-entered: catalog foods, diary, weights, presets.
     * Assembled off the main thread — the diary alone can be thousands of rows.
     */
    suspend fun exportAllCsv(): String =
        withContext(Dispatchers.Default) {
            val foods = database.foodItemDao().getAll()
            val entries = database.diaryEntryDao().getAllEntries()
            val weights = database.weightEntryDao().getAll()
            val presets =
                database
                    .mealPresetDao()
                    .getAllWithItems()
                    .first()
                    .map { it.preset to it.items }
            AppCsv.export(foods, entries, weights, presets)
        }

    /**
     * Imports a file produced by [exportAllCsv]. Foods are matched to existing rows by
     * seedKey/offId/(name+brand) and created when missing; diary rows identical to an
     * existing entry are skipped, so re-importing the same file twice changes nothing.
     */
    suspend fun importCsv(content: String): ImportOutcome =
        withContext(Dispatchers.Default) {
            val parsed =
                when (val result = AppCsv.parse(content)) {
                    is AppCsv.ParseResult.NotADiaitaExport -> return@withContext ImportOutcome.NotADiaitaExport
                    is AppCsv.ParseResult.Malformed -> return@withContext ImportOutcome.Malformed(result.line)
                    is AppCsv.ParseResult.Success -> result.parsed
                }

            database.withTransaction {
                val foodDao = database.foodItemDao()
                val diaryDao = database.diaryEntryDao()
                val weightDao = database.weightEntryDao()
                val presetDao = database.mealPresetDao()

                var foodsCreated = 0
                val existingFoods = foodDao.getAll()
                val refToId = mutableMapOf<Long, Long>()
                parsed.foods.forEach { (ref, imported) ->
                    val match =
                        existingFoods.firstOrNull { existing ->
                            (imported.seedKey != null && existing.seedKey == imported.seedKey) ||
                                (imported.offId != null && existing.offId == imported.offId) ||
                                (
                                    existing.name.equals(imported.name, ignoreCase = true) &&
                                        existing.brand.orEmpty().equals(imported.brand.orEmpty(), ignoreCase = true)
                                )
                        }
                    refToId[ref] = match?.id
                        ?: foodDao.insert(imported).also { foodsCreated++ }
                }

                var diaryInserted = 0
                var diarySkipped = 0
                parsed.diary.groupBy { it.date }.forEach { (date, rows) ->
                    val existing = diaryDao.entriesForDateOnce(date)
                    rows.forEach { row ->
                        val foodId = refToId[row.foodRef]
                        if (foodId == null) {
                            diarySkipped++
                            return@forEach
                        }
                        val duplicate =
                            existing.any {
                                it.foodItemId == foodId &&
                                    it.quantityGrams == row.quantityGrams &&
                                    it.loggedAt == row.loggedAt
                            }
                        if (duplicate) {
                            diarySkipped++
                        } else {
                            diaryDao.insert(
                                app.diaita.data.db.entities.DiaryEntry(
                                    foodItemId = foodId,
                                    quantityGrams = row.quantityGrams,
                                    logDate = row.date,
                                    loggedAt = row.loggedAt,
                                    mealSlot = row.mealSlot,
                                    sourcePresetId = row.presetId,
                                    sourcePresetName = row.presetName,
                                ),
                            )
                            diaryInserted++
                        }
                    }
                }

                parsed.weights.forEach { weightDao.upsert(it) }

                var presetsCreated = 0
                val existingPresetNames =
                    presetDao
                        .getAllWithItems()
                        .first()
                        .map { it.preset.name.lowercase() }
                        .toSet()
                parsed.presets.forEach { preset ->
                    if (preset.name.lowercase() in existingPresetNames) return@forEach
                    val presetId =
                        presetDao.insertPreset(
                            app.diaita.data.db.entities
                                .MealPreset(name = preset.name, mealSlot = preset.mealSlot),
                        )
                    preset.items.forEach { (foodRef, quantity) ->
                        refToId[foodRef]?.let { foodId ->
                            presetDao.insertItem(
                                app.diaita.data.db.entities.MealPresetItem(
                                    presetId = presetId,
                                    foodItemId = foodId,
                                    quantityGrams = quantity,
                                ),
                            )
                        }
                    }
                    presetsCreated++
                }

                ImportOutcome.Success(
                    ImportSummary(
                        foodsCreated = foodsCreated,
                        diaryRowsInserted = diaryInserted,
                        diaryRowsSkipped = diarySkipped,
                        weightsUpserted = parsed.weights.size,
                        presetsCreated = presetsCreated,
                    ),
                )
            }
        }
}

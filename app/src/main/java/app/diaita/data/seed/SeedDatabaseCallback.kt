package app.diaita.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.diaita.data.db.AppDatabase
import app.diaita.data.db.entities.FoodSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps the seed food catalog in sync on every database open (which also covers first
 * creation): any [SeedFoods] entry whose name is not yet present among SEED rows gets
 * inserted. This lets catalog additions in app updates reach existing installs without
 * a schema migration, and is idempotent because names are compared first.
 *
 * [databaseProvider] is invoked lazily (Room only opens the underlying SQLite file, and
 * fires callbacks, on first real query) so it's safe for it to reference a database
 * instance that is still being constructed by the caller at registration time.
 */
class SeedDatabaseCallback(
    private val appScope: CoroutineScope,
    private val databaseProvider: () -> AppDatabase,
) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        appScope.launch {
            val foodItemDao = databaseProvider().foodItemDao()
            val existingNames = foodItemDao.getNamesBySource(FoodSource.SEED).toHashSet()
            val missing = SeedFoods.all.filter { it.name !in existingNames }
            if (missing.isNotEmpty()) foodItemDao.insertAll(missing)
        }
    }
}

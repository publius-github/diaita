package app.diaita.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.diaita.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Populates the seed food list on first database creation. [databaseProvider] is invoked
 * lazily (Room only opens the underlying SQLite file, and fires onCreate, on first real
 * query) so it's safe for it to reference a database instance that is still being
 * constructed by the caller at the point this callback is registered.
 */
class SeedDatabaseCallback(
    private val appScope: CoroutineScope,
    private val databaseProvider: () -> AppDatabase,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        appScope.launch {
            databaseProvider().foodItemDao().insertAll(SeedFoods.all)
        }
    }
}

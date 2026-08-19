package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One tap of the water button — kept as individual rows so a tap can be undone exactly. */
@Entity(tableName = "water_entries", indices = [Index("date")])
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amountMl: Int,
    val loggedAt: Long = System.currentTimeMillis(),
)

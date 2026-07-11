package app.diaita.util

import app.diaita.data.db.entities.MealSlot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun today(): String = LocalDate.now().format(isoFormatter)

    fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)

    /**
     * The meal slot a log made right now most likely belongs to. Thresholds match the
     * day-part mapping on the Diary screen (before 12:00 = morning, before 17:00 = day,
     * later = evening) so auto-slotted entries land in the matching bar segment.
     */
    fun defaultMealSlotNow(): MealSlot {
        val hour = LocalTime.now().hour
        return when {
            hour < 12 -> MealSlot.BREAKFAST
            hour < 17 -> MealSlot.LUNCH
            else -> MealSlot.DINNER
        }
    }
}

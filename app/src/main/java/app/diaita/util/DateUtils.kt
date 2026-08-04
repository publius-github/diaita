package app.diaita.util

import app.diaita.data.db.entities.MealSlot
import app.diaita.data.settings.DayPartBoundaries
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun today(): String = LocalDate.now().format(isoFormatter)

    /**
     * The wall-clock instant a log made right now should carry for [date] (ISO yyyy-MM-dd).
     * Today that is simply "now"; when backfilling another day it is that day at the current
     * time-of-day, so entry ordering and the snack time-of-day day-part fallback keep working.
     */
    fun loggedAtFor(date: String): Long {
        if (date == today()) return System.currentTimeMillis()
        return LocalDate
            .parse(date, isoFormatter)
            .atTime(LocalTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun formatTime(epochMillis: Long): String =
        Instant
            .ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)

    /**
     * The meal slot a log made right now most likely belongs to. Thresholds are the same
     * user-configurable [DayPartBoundaries] as the day-part mapping on the Diary screen,
     * so auto-slotted entries land in the matching bar segment.
     */
    fun defaultMealSlotNow(boundaries: DayPartBoundaries = DayPartBoundaries()): MealSlot {
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        return when {
            minutes < boundaries.morningEndMinutes -> MealSlot.BREAKFAST
            minutes < boundaries.dayEndMinutes -> MealSlot.LUNCH
            else -> MealSlot.DINNER
        }
    }
}

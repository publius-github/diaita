package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class DateUtilsTest {
    @Test
    fun `loggedAtFor today is now`() {
        val millis = DateUtils.loggedAtFor(DateUtils.today())
        assertTrue(abs(System.currentTimeMillis() - millis) < 5_000)
    }

    @Test
    fun `loggedAtFor a past date lands on that date at the current time of day`() {
        val date = "2026-07-10"
        val millis = DateUtils.loggedAtFor(date)
        val stamped = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        assertEquals(LocalDate.parse(date), stamped.toLocalDate())
        // Time-of-day carried over from "now" (minute precision is enough for the assertion).
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val stampedMinutes = stamped.toLocalTime().let { it.hour * 60 + it.minute }
        assertTrue(abs(nowMinutes - stampedMinutes) <= 1)
    }
}

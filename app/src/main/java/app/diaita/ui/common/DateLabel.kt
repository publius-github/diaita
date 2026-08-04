package app.diaita.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.diaita.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Human label for an ISO diary date: "Today", "Yesterday", or a short weekday-and-day form
 * ("Tue, 15 Jul"). Shared by the Diary date navigation and the Add-food screen so the two
 * always name the same day the same way.
 */
@Composable
fun friendlyDateLabel(date: String): String {
    val parsed = LocalDate.parse(date)
    val today = LocalDate.now()
    return when (parsed) {
        today -> stringResource(R.string.today_title)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> parsed.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    }
}

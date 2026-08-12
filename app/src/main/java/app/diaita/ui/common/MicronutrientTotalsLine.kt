package app.diaita.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.diaita.R
import app.diaita.util.Micronutrient
import app.diaita.util.micronutrientTotals

/**
 * Fiber/sugar/salt totals under the day's calorie/macro figures, shared by the Diary card and
 * the History day detail.
 *
 * Not every food carries these values — quick-add entries never do, and so do foods cached from
 * OpenFoodFacts before the columns existed. A day made only of those has nothing to sum, and
 * silently rendering nothing then reads as a broken feature, so [hasEntries] days say "no data"
 * out loud instead. An empty day still renders nothing: there is no absence to explain.
 */
@Composable
fun MicronutrientTotalsLine(
    fiberGrams: Double?,
    sugarGrams: Double?,
    saltGrams: Double?,
    hasEntries: Boolean,
    modifier: Modifier = Modifier,
) {
    val totals = micronutrientTotals(fiberGrams, sugarGrams, saltGrams)
    if (totals.isEmpty()) {
        if (hasEntries) {
            Text(
                stringResource(R.string.micro_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier,
            )
        }
        return
    }
    val parts =
        totals.map { total ->
            stringResource(
                when (total.nutrient) {
                    Micronutrient.FIBER -> R.string.micro_fiber_value
                    Micronutrient.SUGAR -> R.string.micro_sugar_value
                    Micronutrient.SALT -> R.string.micro_salt_value
                },
                total.displayGrams,
            )
        }
    Text(
        parts.joinToString("   "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

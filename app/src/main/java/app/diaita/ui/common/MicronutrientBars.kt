package app.diaita.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.util.Micronutrient
import app.diaita.util.MicronutrientProgress
import app.diaita.util.micronutrientProgress
import app.diaita.util.micronutrientTotals

/**
 * Fiber/sugar/salt as progress bars under the macro bars on the Diary card, against the
 * reference intakes in `Micronutrients.kt`. Unlike the macro bars these need no user goal,
 * so they appear as soon as the day's foods carry the values.
 *
 * Renders something in every state, because a block that vanishes on its own reads as a broken
 * feature: bars when there is data, a plain zero line when nothing is logged yet, and an
 * explicit "no data" when the day's entries carry none of the three (quick-add entries never
 * do, nor do foods cached from OpenFoodFacts before the columns existed).
 */
@Composable
fun MicronutrientBars(
    fiberGrams: Double?,
    sugarGrams: Double?,
    saltGrams: Double?,
    hasEntries: Boolean,
    modifier: Modifier = Modifier,
) {
    val totals = micronutrientTotals(fiberGrams, sugarGrams, saltGrams)
    if (totals.isEmpty()) {
        MicronutrientTotalsLine(
            fiberGrams = fiberGrams,
            sugarGrams = sugarGrams,
            saltGrams = saltGrams,
            hasEntries = hasEntries,
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        micronutrientProgress(totals).forEachIndexed { index, progress ->
            if (index > 0) Spacer(modifier = Modifier.height(6.dp))
            MicronutrientBar(progress)
        }
    }
}

@Composable
private fun MicronutrientBar(progress: MicronutrientProgress) {
    val name =
        stringResource(
            when (progress.total.nutrient) {
                Micronutrient.FIBER -> R.string.micro_fiber_label
                Micronutrient.SUGAR -> R.string.micro_sugar_label
                Micronutrient.SALT -> R.string.micro_salt_label
            },
        )
    val line =
        stringResource(
            R.string.micro_bar_line,
            name,
            progress.total.displayGrams,
            progress.referenceDisplayGrams,
        )
    // The bar is a limit for sugar/salt, so passing it is a warning rather than an achievement;
    // TalkBack gets that as words, since colour alone doesn't carry it.
    val spoken =
        when {
            progress.exceeded -> stringResource(R.string.micro_limit_exceeded, line)
            progress.reached -> stringResource(R.string.micro_target_reached, line)
            else -> line
        }

    Column(modifier = Modifier.semantics { contentDescription = spoken }) {
        Text(
            line,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (progress.exceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            color =
                if (progress.exceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            trackColor = progressTrackColor(),
        )
    }
}

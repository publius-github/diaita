package app.diaita.ui.presets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.PresetSummary
import app.diaita.ui.common.label

/** A meal group in the preset list, mirroring the Diary screen's section layout. */
data class PresetSection(
    val slot: MealSlot,
    val presets: List<PresetSummary>,
)

// Same slot order as the Diary sections, so both screens read top-to-bottom the same way.
private val PRESET_SECTION_ORDER =
    listOf(
        MealSlot.BREAKFAST,
        MealSlot.LUNCH,
        MealSlot.DINNER,
        MealSlot.SNACK,
        MealSlot.UNSPECIFIED,
    )

/** Groups presets by meal slot and orders each group alphabetically by name (case-insensitive). */
fun groupPresetsBySlot(presets: List<PresetSummary>): List<PresetSection> =
    PRESET_SECTION_ORDER.mapNotNull { slot ->
        val group =
            presets
                .filter { it.preset.mealSlot == slot }
                .sortedBy { it.preset.name.lowercase() }
        if (group.isEmpty()) null else PresetSection(slot, group)
    }

/** Header row for a preset section: meal name on the left, group calorie total on the right. */
@Composable
fun PresetSectionHeader(section: PresetSection) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(section.slot.label(), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(
                R.string.kcal_amount,
                section.presets.sumOf { it.totalCalories }.toInt(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Tap-to-log preset list used by the Add-food screen's Presets tab: grouped by meal slot with
 * the same section headers as the management screen, each preset a tappable [ListItem].
 */
fun LazyListScope.PresetLogList(
    presets: List<PresetSummary>,
    onLogPreset: (Long) -> Unit,
) {
    groupPresetsBySlot(presets).forEach { section ->
        item(key = "header_${section.slot}") { PresetSectionHeader(section) }
        items(section.presets, key = { it.preset.id }) { presetWithItems ->
            ListItem(
                headlineContent = { Text(presetWithItems.preset.name) },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.preset_items_kcal_tap_to_log,
                            presetWithItems.items.size,
                            presetWithItems.totalCalories.toInt(),
                        ),
                    )
                },
                modifier = Modifier.clickable { onLogPreset(presetWithItems.preset.id) },
            )
        }
    }
}

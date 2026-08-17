package app.diaita.ui.presets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.repository.PresetSummary
import app.diaita.ui.common.CompactRowCard
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

/** Header row for a preset section: just the meal name — presets are alternatives, so a group calorie total would be meaningless. */
@Composable
fun PresetSectionHeader(section: PresetSection) {
    Text(
        section.slot.label(),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Tap-to-log preset list used by the Add-food screen's Presets tab: grouped by meal slot with
 * the same section headers as the management screen, each preset a tappable [CompactRowCard].
 */
fun LazyListScope.presetLogList(
    presets: List<PresetSummary>,
    onLogPreset: (Long) -> Unit,
) {
    groupPresetsBySlot(presets).forEach { section ->
        item(key = "header_${section.slot}") { PresetSectionHeader(section) }
        items(section.presets, key = { it.preset.id }) { presetWithItems ->
            CompactRowCard(
                title = presetWithItems.preset.name,
                subtitle =
                    stringResource(
                        R.string.preset_items_kcal_tap_to_log,
                        presetWithItems.items.size,
                        presetWithItems.totalCalories.toInt(),
                    ),
                onClick = { onLogPreset(presetWithItems.preset.id) },
            )
        }
    }
}

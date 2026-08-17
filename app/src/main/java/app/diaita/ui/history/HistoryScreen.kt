package app.diaita.ui.history

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.db.entities.WeightEntry
import app.diaita.data.settings.WeightUnit
import app.diaita.ui.AppViewModelProvider
import app.diaita.ui.common.LogWeightDialog
import app.diaita.ui.common.label
import app.diaita.util.BmiBand
import app.diaita.util.bmi
import app.diaita.util.bmiBand
import app.diaita.util.formatWeightForDisplay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun HistoryScreen(
    onViewDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWeightDialog by remember { mutableStateOf(false) }

    // Static screen: no outer scrolling. The two charts are weighted so the content always
    // fills the viewport exactly; the growing monthly list scrolls inside its own slot.
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.history_title_days, uiState.rangeDays),
            style = MaterialTheme.typography.titleMedium,
        )
        uiState.goalCalories?.let { goal ->
            Text(
                stringResource(R.string.goal_line_label, goal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.maintenanceCalories?.takeIf { it != uiState.goalCalories }?.let { maintenance ->
            Text(
                stringResource(R.string.maintenance_line_label, maintenance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryViewModel.RANGE_OPTIONS.forEach { rangeDays ->
                // Five chips leave no room for "%d days" labels (they wrapped to two
                // lines); every chip uses the uniform "7d…365d" form, TalkBack keeps the words.
                val fullLabel = stringResource(R.string.range_days_chip, rangeDays)
                FilterChip(
                    selected = uiState.rangeDays == rangeDays,
                    onClick = { viewModel.setRangeDays(rangeDays) },
                    label = {
                        Text(
                            stringResource(R.string.range_days_chip_short, rangeDays),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = fullLabel },
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.calories_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (!uiState.hasAnyCalories) {
            Text(stringResource(R.string.history_empty))
        } else {
            CalorieBarChart(
                days = uiState.days,
                bucketDays = uiState.bucketDays,
                goalCalories = uiState.goalCalories,
                maintenanceCalories = uiState.maintenanceCalories,
                selectedIndex = uiState.selectedIndex,
                onBarTap = viewModel::onBarSelected,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1.5f),
            )
            // The row is always composed at a fixed minimum height so selecting/deselecting a day
            // doesn't shift the weight chart below; the text takes weight(1f) so a long line
            // (with the logged weight appended) wraps instead of pushing the button away.
            // 48dp matches the TextButton's enforced minimum touch target — anything smaller
            // makes the row grow when the button appears, nudging the weighted charts.
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                uiState.selectedDay?.let { day ->
                    val selectedWeightKg = uiState.selectedDayWeightKg
                    val baseInfo =
                        when {
                            uiState.bucketDays > 1 ->
                                stringResource(
                                    R.string.selected_week_info,
                                    formatDate(day.date),
                                    day.calories.toInt(),
                                )
                            selectedWeightKg != null ->
                                stringResource(
                                    R.string.selected_day_info_with_weight,
                                    formatDate(day.date),
                                    day.calories.toInt(),
                                    formatWeightForDisplay(uiState.weightUnit.fromKg(selectedWeightKg)),
                                    uiState.weightUnit.label(),
                                )
                            else ->
                                stringResource(
                                    R.string.selected_day_info,
                                    formatDate(day.date),
                                    day.calories.toInt(),
                                )
                        }
                    // On screen the zone is carried by color only (the bar above already shows
                    // it); the spelled-out zone stays in the semantics for TalkBack users, for
                    // whom color is not perceivable at all.
                    val zoneWord =
                        when (day.zone) {
                            CalorieZone.OFF_TRACK -> stringResource(R.string.zone_off_track)
                            CalorieZone.BETWEEN -> stringResource(R.string.zone_between)
                            CalorieZone.ON_TRACK -> null
                        }
                    Text(
                        baseInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            when (day.zone) {
                                CalorieZone.OFF_TRACK -> MaterialTheme.colorScheme.error
                                CalorieZone.BETWEEN -> MaterialTheme.colorScheme.tertiary
                                CalorieZone.ON_TRACK -> MaterialTheme.colorScheme.primary
                            },
                        modifier =
                            Modifier.weight(1f).semantics {
                                zoneWord?.let { contentDescription = "$baseInfo · $it" }
                            },
                    )
                    // A weekly bucket has no single day to open; drill-down stays daily-only.
                    if (uiState.bucketDays == 1) {
                        TextButton(onClick = { onViewDay(day.date) }) {
                            Text(stringResource(R.string.view_day))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.weight_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.weights.isEmpty()) {
            Text(stringResource(R.string.weight_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            val windowStart =
                uiState.days
                    .firstOrNull()
                    ?.date
                    ?.let { LocalDate.parse(it) }
                    ?: LocalDate.now().minusDays(uiState.rangeDays - 1L)
            WeightLineChart(
                weights = uiState.weights,
                targetKg = uiState.weightTargetKg,
                weightUnit = uiState.weightUnit,
                windowStart = windowStart,
                totalDays = uiState.rangeDays,
                // In bucket space one calorie-bar step is bucketDays real days, so this
                // lands the weight labels on exactly the dates the bars above label.
                labelStepDays = xLabelStep(uiState.days.size) * uiState.bucketDays,
                // The weekly view's bar indices live in bucket space, so the day-slot
                // highlight can't line up — taps still select the containing week.
                selectedIndex = uiState.selectedIndex.takeIf { uiState.bucketDays == 1 },
                onDayTap = { dayIndex -> viewModel.onBarSelected(dayIndex / uiState.bucketDays) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
            // 16dp mirrors the gap above the selection line (a bodySmall line centered in the
            // 48dp row), so both summary blocks sit at the same offset from their charts.
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            R.string.latest_weight,
                            formatWeightForDisplay(uiState.weightUnit.fromKg(uiState.weights.last().weightKg)),
                            uiState.weightUnit.label(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    uiState.weightRatePerWeek?.let { rate ->
                        Text(
                            stringResource(
                                if (rate <= 0) R.string.weight_rate_loss else R.string.weight_rate_gain,
                                formatWeightForDisplay(abs(uiState.weightUnit.fromKg(rate))),
                                uiState.weightUnit.label(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    uiState.weightTargetKg?.let { target ->
                        Text(
                            listOfNotNull(
                                stringResource(
                                    R.string.weight_target_line,
                                    formatWeightForDisplay(uiState.weightUnit.fromKg(target)),
                                    uiState.weightUnit.label(),
                                ),
                                uiState.projectedTargetDate?.let { date ->
                                    stringResource(R.string.weight_projection, formatDate(date))
                                },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Needs a height, which only the goal wizard collects — no wizard, no tile.
                bmi(uiState.weights.last().weightKg, uiState.heightCm)?.let { value ->
                    BmiTile(
                        value,
                        athleticBuild = uiState.athleticBmiBands,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showWeightDialog = true }) {
            Text(stringResource(R.string.log_weight))
        }

        if (uiState.monthlyAverages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.monthly_averages), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // Scrolls inside its own capped slot: one row lands per month, and the charts above
            // must not shrink as the list grows over the years.
            LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                items(uiState.monthlyAverages, key = { it.yearMonth }) { aggregate ->
                    Text(
                        stringResource(
                            R.string.monthly_avg_row,
                            formatYearMonth(aggregate.yearMonth),
                            aggregate.avgCalories.toInt(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }

    if (showWeightDialog) {
        LogWeightDialog(
            weightUnit = uiState.weightUnit,
            onDismiss = { showWeightDialog = false },
            onConfirm = { weightKg ->
                viewModel.logWeight(weightKg)
                showWeightDialog = false
            },
        )
    }
}

/** Left gutter for y-axis labels and bottom strip for date labels, shared by both charts. */
private val AXIS_LEFT_PAD = 42.dp
private val AXIS_BOTTOM_PAD = 18.dp

/**
 * At most this many date labels on the x-axis — one rule for every range and both charts,
 * so the cadence stops feeling arbitrary. 7 labels of "28.12" at 10sp still have clear
 * air between them at the narrowest chart width.
 */
private const val MAX_X_LABELS = 7

/** Items (days or weekly buckets) per date label so the strip never exceeds [MAX_X_LABELS]. */
private fun xLabelStep(itemCount: Int): Int = ceil(itemCount / MAX_X_LABELS.toFloat()).toInt().coerceAtLeast(1)

/** Weight point value labels overlap beyond ~2 weeks, so longer views keep only the first and last. */
private fun showAllPointLabels(totalDays: Int): Boolean = totalDays <= 14

/** TalkBack can't tap a canvas bar, so day selection is exposed as custom actions (short ranges). */
private const val MAX_A11Y_DAY_ACTIONS = 31

@Composable
private fun CalorieBarChart(
    days: List<HistoryDayUi>,
    bucketDays: Int,
    goalCalories: Int?,
    maintenanceCalories: Int?,
    selectedIndex: Int?,
    onBarTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val betweenColor = MaterialTheme.colorScheme.tertiary
    val offTrackColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionColor = MaterialTheme.colorScheme.tertiary
    val labelTextSizePx = with(LocalDensity.current) { 10.sp.toPx() }

    val maxCalories = days.maxOf { it.calories }
    val chartMax =
        (
            maxOf(
                maxCalories,
                goalCalories?.toDouble() ?: 0.0,
                maintenanceCalories?.toDouble() ?: 0.0,
            ) * 1.15
        ).coerceAtLeast(100.0)
    val gridStep = calorieGridStep(chartMax)
    val labelStep = xLabelStep(days.size)
    val chartDescription =
        stringResource(
            if (bucketDays > 1) R.string.chart_calories_weeks_a11y else R.string.chart_calories_a11y,
            days.size,
            days.map { it.calories }.average().toInt(),
        )
    val selectDayLabel = stringResource(R.string.a11y_select_day)
    val dayActions =
        if (days.size <= MAX_A11Y_DAY_ACTIONS) {
            days.mapIndexed { index, day ->
                CustomAccessibilityAction("$selectDayLabel ${day.label}") {
                    onBarTap(index)
                    true
                }
            }
        } else {
            emptyList()
        }

    // Height comes from the caller (a weighted slot in the History column), not a fixed value.
    Canvas(
        modifier =
            modifier
                .semantics {
                    contentDescription = chartDescription
                    if (dayActions.isNotEmpty()) customActions = dayActions
                }.pointerInput(days) {
                    detectTapGestures { offset ->
                        val leftPad = AXIS_LEFT_PAD.toPx()
                        if (offset.x < leftPad) return@detectTapGestures
                        val slot = (size.width - leftPad) / days.size
                        val index = ((offset.x - leftPad) / slot).toInt().coerceIn(0, days.size - 1)
                        onBarTap(index)
                    }
                },
    ) {
        val leftPad = AXIS_LEFT_PAD.toPx()
        val bottomPad = AXIS_BOTTOM_PAD.toPx()
        val chartWidth = size.width - leftPad
        val chartHeight = size.height - bottomPad
        val slot = chartWidth / days.size

        val axisPaint =
            Paint().apply {
                color = labelColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
        val centerPaint = Paint(axisPaint).apply { textAlign = Paint.Align.CENTER }

        fun yFor(value: Double): Float = (chartHeight - value / chartMax * chartHeight).toFloat()

        // Horizontal gridlines with y-axis value labels.
        var gridValue = gridStep
        while (gridValue <= chartMax) {
            val y = yFor(gridValue)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                gridValue.toInt().toString(),
                leftPad - 4.dp.toPx(),
                y + labelTextSizePx / 3,
                axisPaint,
            )
            gridValue += gridStep
        }

        // Selection highlight behind the bar.
        selectedIndex?.let { index ->
            drawRoundRect(
                color = selectionColor.copy(alpha = 0.25f),
                topLeft = Offset(leftPad + index * slot, 0f),
                size = Size(slot, chartHeight),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }

        // Bars.
        val barWidth = slot * 0.65f
        days.forEachIndexed { index, day ->
            if (day.calories <= 0) return@forEachIndexed
            val barHeight = (day.calories / chartMax * chartHeight).toFloat()
            val left = leftPad + index * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color =
                    when (day.zone) {
                        CalorieZone.OFF_TRACK -> offTrackColor
                        CalorieZone.BETWEEN -> betweenColor
                        CalorieZone.ON_TRACK -> barColor
                    },
                topLeft = Offset(left, chartHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }

        // Dashed goal line.
        if (goalCalories != null && goalCalories > 0) {
            val goalY = yFor(goalCalories.toDouble())
            drawLine(
                color = labelColor,
                start = Offset(leftPad, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
            )
        }

        // Dashed maintenance (TDEE) line — tertiary color and a shorter dash so it reads
        // differently from the goal line. Skipped when it coincides with the goal (MAINTAIN plan).
        if (maintenanceCalories != null && maintenanceCalories > 0 && maintenanceCalories != goalCalories) {
            val maintenanceY = yFor(maintenanceCalories.toDouble())
            drawLine(
                color = betweenColor,
                start = Offset(leftPad, maintenanceY),
                end = Offset(size.width, maintenanceY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
        }

        // Static baseline (0 kcal axis) with its label.
        drawLine(
            color = labelColor,
            start = Offset(leftPad, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.5f,
        )
        drawContext.canvas.nativeCanvas.drawText(
            "0",
            leftPad - 4.dp.toPx(),
            chartHeight + labelTextSizePx / 3,
            axisPaint,
        )

        // Date labels ("d.MM"), drawn in the canvas so they never wrap.
        days.forEachIndexed { index, day ->
            if (index % labelStep == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    day.label,
                    leftPad + index * slot + slot / 2f,
                    size.height - 4.dp.toPx(),
                    centerPaint,
                )
            }
        }
    }
}

/** Picks a y-axis grid step so the calorie chart has at most ~5 gridlines. */
private fun calorieGridStep(chartMax: Double): Double =
    listOf(100.0, 250.0, 500.0, 1000.0, 2000.0, 5000.0).firstOrNull { chartMax / it <= 5 } ?: 5000.0

/** Picks a whole-kg grid step so the weight chart has at most ~4 gridlines. */
private fun weightGridStep(span: Double): Double =
    listOf(1.0, 2.0, 5.0, 10.0, 20.0).firstOrNull { span / it <= 4 } ?: 20.0

@Composable
private fun WeightLineChart(
    weights: List<WeightEntry>,
    targetKg: Double?,
    weightUnit: WeightUnit,
    windowStart: LocalDate,
    totalDays: Int,
    /** Days between date labels — supplied by the caller so both charts label the same dates. */
    labelStepDays: Int,
    selectedIndex: Int?,
    onDayTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionColor = MaterialTheme.colorScheme.tertiary
    val labelTextSizePx = with(LocalDensity.current) { 10.sp.toPx() }
    val chartDescription =
        stringResource(
            R.string.chart_weight_a11y,
            formatWeightForDisplay(weightUnit.fromKg(weights.last().weightKg)),
            weightUnit.label(),
        )

    // Height comes from the caller (a weighted slot in the History column), not a fixed value.
    Canvas(
        modifier =
            modifier
                .semantics { contentDescription = chartDescription }
                .pointerInput(totalDays) {
                    detectTapGestures { offset ->
                        val leftPad = AXIS_LEFT_PAD.toPx()
                        if (offset.x < leftPad) return@detectTapGestures
                        val slot = (size.width - leftPad) / totalDays
                        val index = ((offset.x - leftPad) / slot).toInt().coerceIn(0, totalDays - 1)
                        onDayTap(index)
                    }
                },
    ) {
        val leftPad = AXIS_LEFT_PAD.toPx()
        val bottomPad = AXIS_BOTTOM_PAD.toPx()
        val chartWidth = size.width - leftPad
        val chartHeight = size.height - bottomPad
        val slot = chartWidth / totalDays

        // All chart math runs in the display unit so axis labels stay round numbers.
        val displayValues = weights.map { weightUnit.fromKg(it.weightKg) }
        val displayTarget = targetKg?.let { weightUnit.fromKg(it) }

        // Padded whole-unit scale: the bottom of the chart is a static axis, never a data value,
        // so points and the target line always sit clearly above it (and below the top).
        val rawMin = minOf(displayValues.min(), displayTarget ?: Double.MAX_VALUE)
        val rawMax = maxOf(displayValues.max(), displayTarget ?: Double.MIN_VALUE)
        val scaleLow = floor(rawMin - 1.0)
        val scaleHigh = ceil(rawMax + 1.0)
        val span = (scaleHigh - scaleLow).coerceAtLeast(2.0)
        val gridStep = weightGridStep(span)

        val axisPaint =
            Paint().apply {
                color = labelColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
        val centerPaint = Paint(axisPaint).apply { textAlign = Paint.Align.CENTER }

        fun yFor(value: Double): Float = (chartHeight - (value - scaleLow) / span * chartHeight).toFloat()

        fun xFor(entry: WeightEntry): Float {
            val dayIndex =
                java.time.temporal.ChronoUnit.DAYS
                    .between(windowStart, LocalDate.parse(entry.date))
                    .toInt()
                    .coerceIn(0, totalDays - 1)
            return leftPad + dayIndex * slot + slot / 2f
        }

        // Whole-kg gridlines with labels (skipping the baseline row).
        var gridValue = ceil(scaleLow / gridStep) * gridStep
        while (gridValue <= scaleHigh) {
            val y = yFor(gridValue)
            if (y < chartHeight - 1f) {
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.0f", gridValue),
                    leftPad - 4.dp.toPx(),
                    y + labelTextSizePx / 3,
                    axisPaint,
                )
            }
            gridValue += gridStep
        }

        // Selection highlight — same day slot as the calorie chart above, so the two stay in sync.
        selectedIndex?.let { index ->
            drawRoundRect(
                color = selectionColor.copy(alpha = 0.25f),
                topLeft = Offset(leftPad + index * slot, 0f),
                size = Size(slot, chartHeight),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }

        // Dashed target line, labeled at the right end just above the line.
        if (displayTarget != null) {
            val y = yFor(displayTarget)
            drawLine(
                color = labelColor,
                start = Offset(leftPad, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatWeightForDisplay(displayTarget),
                size.width - 2.dp.toPx(),
                y - 4.dp.toPx(),
                axisPaint,
            )
        }

        // Points, connecting lines, and value labels. In longer views intermediate labels
        // overlap into an unreadable strip, so only the first and last points keep theirs.
        val allPointLabels = showAllPointLabels(totalDays)
        weights.forEachIndexed { index, entry ->
            val point = Offset(xFor(entry), yFor(displayValues[index]))
            drawCircle(color = lineColor, radius = 2.dp.toPx(), center = point)
            if (index > 0) {
                drawLine(
                    color = lineColor,
                    start = Offset(xFor(weights[index - 1]), yFor(displayValues[index - 1])),
                    end = point,
                    strokeWidth = 2.dp.toPx(),
                )
            }
            if (allPointLabels || index == 0 || index == weights.lastIndex) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatWeightForDisplay(displayValues[index]),
                    point.x,
                    point.y - 6.dp.toPx(),
                    centerPaint,
                )
            }
        }

        // Static baseline (X axis).
        drawLine(
            color = labelColor,
            start = Offset(leftPad, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.5f,
        )

        // Date labels — same timeline and cadence as the calorie chart above.
        for (index in 0 until totalDays step labelStepDays) {
            drawContext.canvas.nativeCanvas.drawText(
                windowStart.plusDays(index.toLong()).format(DATE_LABEL_FORMATTER),
                leftPad + index * slot + slot / 2f,
                size.height - 4.dp.toPx(),
                centerPaint,
            )
        }
    }
}

private val DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("d.MM")

/**
 * BMI badge next to the weight summary. The value colors by WHO band: green inside the
 * normal 18.5-25 range, amber for under/overweight, red for obese — the same
 * green/amber/red language the rest of the app uses for on-track/warning/over.
 */
@Composable
private fun BmiTile(
    value: Double,
    athleticBuild: Boolean,
    modifier: Modifier = Modifier,
) {
    val color =
        when (bmiBand(value, athleticBuild)) {
            BmiBand.NORMAL -> MaterialTheme.colorScheme.primary
            BmiBand.UNDERWEIGHT, BmiBand.OVERWEIGHT -> MaterialTheme.colorScheme.tertiary
            BmiBand.OBESE -> MaterialTheme.colorScheme.error
        }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.bmi_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.bmi_value, value),
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

private fun formatDate(isoDate: String): String =
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

private fun formatYearMonth(yearMonth: String): String =
    YearMonth.parse(yearMonth).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))

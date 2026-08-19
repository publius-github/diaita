package app.diaita.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.diaita.R

@Composable
fun MacroProgressRing(
    consumedCalories: Double,
    goalCalories: Int?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
        if (goalCalories != null && goalCalories > 0) {
            val isOverGoal = consumedCalories > goalCalories
            val progress = (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f)
            val ringColor = if (isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                color = ringColor,
                trackColor = progressTrackColor(),
            )
            Text(
                stringResource(R.string.ring_progress, consumedCalories.toInt(), goalCalories),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (isOverGoal) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        } else {
            Text(
                stringResource(R.string.ring_no_goal, consumedCalories.toInt()),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * The space-saving sibling of [MacroProgressRing]: the consumed/goal figures live in the
 * card header instead of the ring's center, so only the percentage has to fit inside and
 * the ring shrinks to roughly the height of the header text block. Callers must pass a
 * positive [goalCalories] — without a goal there is no percentage to show.
 */
@Composable
fun CompactProgressRing(
    consumedCalories: Double,
    goalCalories: Int,
    modifier: Modifier = Modifier,
) {
    val isOverGoal = consumedCalories > goalCalories
    val progress = (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f)
    val ringColor = if (isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(76.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            color = ringColor,
            trackColor = progressTrackColor(),
        )
        Text(
            stringResource(R.string.ring_percent, percentOf(consumedCalories, goalCalories)),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = if (isOverGoal) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
    }
}

/**
 * Half-circle gauge variant: same visual language as the ring at roughly half the height.
 * The percentage sits in the notch under the arc. Callers must pass a positive [goalCalories].
 */
@Composable
fun ProgressGauge(
    consumedCalories: Double,
    goalCalories: Int,
    modifier: Modifier = Modifier,
) {
    val isOverGoal = consumedCalories > goalCalories
    val progress = (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f)
    val arcColor = if (isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val trackColor = progressTrackColor()
    Box(
        modifier = modifier.width(GAUGE_WIDTH).height(GAUGE_HEIGHT),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = GAUGE_STROKE.toPx(), cap = StrokeCap.Round)
            val diameter = size.width - stroke.width
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(stroke.width / 2, stroke.width / 2)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            if (progress > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Text(
            stringResource(R.string.ring_percent, percentOf(consumedCalories, goalCalories)),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = if (isOverGoal) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
    }
}

/**
 * Full-width linear variant: thicker than the 6dp macro/day-part bars below it on the card
 * so the day total keeps reading as the headline indicator. Callers must pass a positive
 * [goalCalories].
 */
@Composable
fun CalorieProgressBar(
    consumedCalories: Double,
    goalCalories: Int,
    modifier: Modifier = Modifier,
) {
    val isOverGoal = consumedCalories > goalCalories
    val progress = (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth().height(10.dp),
        color = if (isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        trackColor = progressTrackColor(),
    )
}

private fun percentOf(
    consumedCalories: Double,
    goalCalories: Int,
): Int = (consumedCalories / goalCalories * 100).toInt()

private val GAUGE_WIDTH = 112.dp
private val GAUGE_HEIGHT = 62.dp
private val GAUGE_STROKE = 10.dp

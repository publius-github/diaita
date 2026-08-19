package app.diaita.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.diaita.R

/**
 * The row height every list row lands on. Rows with trailing icon buttons get it for free from
 * the 48dp touch target; rows without any trailing action would otherwise sit visibly shorter
 * than their neighbours, so [CompactRowCard] enforces it for all of them.
 */
private val RowContentMinHeight = 48.dp

/**
 * The single compact list-row card used by every content list in the app — diary rows, the
 * preset editor, the preset lists and the food search results — so they all stay visually
 * identical and any styling change lands everywhere at once: title over subtitle, optional
 * trailing actions on the right, the whole card tappable when [onClick] is provided.
 */
@Composable
fun CompactRowCard(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.Unspecified,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val cardModifier =
        if (onClick != null) {
            modifier.fillMaxWidth().clickable(onClick = onClick)
        } else {
            modifier.fillMaxWidth()
        }

    // Compact row: trimmed vertical padding fits noticeably more rows per screen. The title
    // never wraps — long names ellipsize so row heights and paddings stay uniform. Trailing
    // icon buttons carry ~12dp of built-in whitespace around the glyph, so the end padding
    // shrinks to 4dp when they're present, giving the text those 8dp back.
    Card(modifier = cardModifier) {
        Row(
            modifier =
                Modifier
                    .padding(
                        start = 12.dp,
                        end = if (trailing != null) 4.dp else 12.dp,
                        top = 5.dp,
                        bottom = 5.dp,
                    ).fillMaxWidth()
                    .heightIn(min = RowContentMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    // 20sp line height (vs bodyLarge's 24sp) keeps the row-height trim from
                    // the density pass even for the single line.
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            trailing?.invoke(this)
        }
    }
}

/** [CompactRowCard] preconfigured with the delete action used by the diary and preset editor. */
@Composable
fun FoodRowCard(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompactRowCard(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
    ) {
        // Default IconButton size keeps the Material-minimum 48dp touch target (a11y).
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

package app.diaita.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.diaita.R

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
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val cardModifier =
        if (onClick != null) {
            modifier.fillMaxWidth().clickable(onClick = onClick)
        } else {
            modifier.fillMaxWidth()
        }

    // Compact row: the default 48dp IconButton touch target plus uniform 12dp padding used
    // to inflate every entry to ~72dp; trimming both fits noticeably more rows per screen.
    Card(modifier = cardModifier) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
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

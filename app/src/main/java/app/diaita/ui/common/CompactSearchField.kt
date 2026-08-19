package app.diaita.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.diaita.R

/** Height of the field: the standard Material button height, so the two line up exactly. */
private val FieldHeight = 40.dp

/** The trailing button is sized down to fit inside [FieldHeight]; the field itself stays tappable. */
private val TrailingButtonSize = 32.dp
private val IconSize = 18.dp

/**
 * The one search/filter field in the app — the presets list, the food search, anywhere else a
 * query narrows a list — so they all look and measure the same. A plain `OutlinedTextField`
 * cannot: its 56dp minimum plus label-sized padding towers over a 40dp button beside it and the
 * two read as unrelated controls, so this wraps [BasicTextField] in the same outlined decoration
 * with the vertical padding taken out. A non-empty query always offers the clear (x); [trailing]
 * (a progress spinner, say) takes the slot over while it is showing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(FieldHeight),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = {
                Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                )
            },
            trailingIcon =
                when {
                    trailing != null -> trailing
                    value.isNotEmpty() -> {
                        {
                            IconButton(
                                onClick = { onValueChange("") },
                                modifier = Modifier.size(TrailingButtonSize),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear_search),
                                    modifier = Modifier.size(IconSize),
                                )
                            }
                        }
                    }
                    else -> null
                },
            colors = colors,
            // Zero vertical padding: the fixed height above is what centers the text now.
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = MaterialTheme.shapes.small,
                )
            },
        )
    }
}

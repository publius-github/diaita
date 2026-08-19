package app.diaita.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

/** The clear (x) button is sized down to fit inside [FieldHeight]; the field itself stays tappable. */
private val ClearButtonSize = 32.dp
private val ClearIconSize = 18.dp

/**
 * A single-line filter field the height of the Material button beside it. A plain
 * `OutlinedTextField` cannot do this — its 56dp minimum plus label-sized padding towers over a
 * 40dp button and makes the two read as unrelated controls — so this wraps [BasicTextField] in
 * the same outlined decoration with the vertical padding taken out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
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
            trailingIcon =
                if (value.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(ClearButtonSize),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                modifier = Modifier.size(ClearIconSize),
                            )
                        }
                    }
                } else {
                    null
                },
            colors = colors,
            // Zero vertical padding: the fixed height above is what centers the text now.
            contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
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

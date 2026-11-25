package com.mad.maduikit

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Controlled single-line [TextFieldValue] with passive, clipped artwork. The visual
 * [placeholder] is an example only. Below 144 dp no icon is shown; below 200 dp
 * at most one is shown, preferring logical [leading]. Icons have no names/actions.
 */
@Composable
public fun MadDecoratedTextField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    leading: Painter? = null,
    trailing: Painter? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    require(label.isNotBlank()) { "MadDecoratedTextField requires a nonblank label" }
    require(placeholder == null || placeholder.isNotBlank()) {
        "MadDecoratedTextField placeholder must be nonblank or null"
    }
    require(supportingText == null || supportingText.isNotBlank()) {
        "MadDecoratedTextField supportingText must be nonblank or null"
    }
    require(errorMessage == null || errorMessage.isNotBlank()) {
        "MadDecoratedTextField errorMessage must be nonblank or null"
    }
    val message = errorMessage ?: supportingText
    BoxWithConstraints(modifier) {
        val start = leading?.takeIf { maxWidth >= 144.dp }
        val end = trailing?.takeIf { maxWidth >= 144.dp && (start == null || maxWidth >= 200.dp) }
        val field = Modifier.fillMaxWidth().let {
            if (errorMessage == null) it else it.semantics { error(errorMessage) }
        }
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = field,
            enabled = enabled, readOnly = readOnly, label = { Text(label) },
            leadingIcon = start?.let { { PassiveFieldIcon(it) } },
            trailingIcon = end?.let { { PassiveFieldIcon(it) } },
            placeholder = placeholder?.let { { Text(it, Modifier.clearAndSetSemantics {}) } },
            supportingText = message?.let { { Text(it) } }, isError = errorMessage != null,
            shape = RoundedCornerShape(MadGeometryDefaults.CornerRadii[MadCornerRole.Medium].dp),
            singleLine = true, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        )
    }
}

@Composable
private fun PassiveFieldIcon(painter: Painter) {
    MadIcon(painter, contentDescription = null, modifier = Modifier.size(24.dp).clipToBounds(), tint = Color.Unspecified)
}

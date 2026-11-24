package com.mad.maduikit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * A controlled single-line text field. Store the complete new [TextFieldValue]
 * promptly, including selection and composition; validate separately without
 * reconstructing the edit state from its string.
 *
 * Supply localized [label], [supportingText] and [errorMessage]; an error
 * replaces help visually and is provided as the specific error in semantics.
 * A disabled field is not editable or focusable. A read-only enabled field can
 * be focused and selected but is not editable. Keyboard actions and focus order
 * remain host decisions; the platform supplies the actual input method.
 */
@Composable
public fun MadTextField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: String? = null,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    require(label.isNotBlank()) { "MadTextField requires a nonblank label" }
    require(supportingText == null || supportingText.isNotBlank()) {
        "MadTextField supportingText must be nonblank or null"
    }
    require(errorMessage == null || errorMessage.isNotBlank()) {
        "MadTextField errorMessage must be nonblank or null"
    }
    val message = errorMessage ?: supportingText
    val decorated = if (errorMessage == null) modifier else modifier.semantics { error(errorMessage) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = decorated,
        enabled = enabled, readOnly = readOnly, label = { Text(label) },
        supportingText = message?.let { { Text(it) } }, isError = errorMessage != null,
        shape = RoundedCornerShape(MadGeometryDefaults.CornerRadii[MadCornerRole.Medium].dp),
        singleLine = true, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
    )
}

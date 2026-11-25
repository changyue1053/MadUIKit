package com.mad.maduikit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Host-controlled multiline editing. Retain the complete value as it is
 * reported, including selection and composing range. A visual example
 * [placeholder] never replaces the accessible label or the supporting text.
 * The visible line range bounds height, not characters or line count.
 * Validation, keyboard actions and submission remain host-owned.
 */
@Composable
public fun MadTextArea(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    minVisibleLines: Int = 2,
    maxVisibleLines: Int = 4,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    require(label.isNotBlank()) { "MadTextArea requires a nonblank label" }
    require(placeholder == null || placeholder.isNotBlank()) {
        "MadTextArea placeholder must be nonblank or null"
    }
    require(minVisibleLines in 1..maxVisibleLines) {
        "MadTextArea visible lines must satisfy 1 <= min <= max"
    }
    require(supportingText == null || supportingText.isNotBlank()) {
        "MadTextArea supportingText must be nonblank or null"
    }
    require(errorMessage == null || errorMessage.isNotBlank()) {
        "MadTextArea errorMessage must be nonblank or null"
    }
    val message = errorMessage ?: supportingText
    val decorated = if (errorMessage == null) modifier else modifier.semantics { error(errorMessage) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = decorated,
        enabled = enabled, readOnly = readOnly, label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, Modifier.clearAndSetSemantics {}) } },
        supportingText = message?.let { { Text(it) } }, isError = errorMessage != null,
        shape = RoundedCornerShape(MadGeometryDefaults.CornerRadii[MadCornerRole.Medium].dp),
        singleLine = false, minLines = minVisibleLines, maxLines = maxVisibleLines, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
    )
}

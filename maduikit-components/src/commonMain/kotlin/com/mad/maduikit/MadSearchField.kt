package com.mad.maduikit

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A single-line [MadTextField] configured for the Search keyboard action.
 * Retain complete values from [onValueChange], including selection and composition.
 * [onSearch] runs once per Search action while editable and reads host-owned query
 * state; editing alone never searches. The host owns validation, search and results.
 * [supportingText] is visible help (not an inline placeholder); [errorMessage] replaces
 * it. Supply localized strings. There is no implicit clear or focus change.
 */
@Composable
public fun MadSearchField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: String? = null,
    errorMessage: String? = null,
) {
    val latestSearch = rememberUpdatedState(if (enabled && !readOnly) onSearch else null)
    MadTextField(
        label = label, value = value, onValueChange = onValueChange,
        modifier = modifier, enabled = enabled, readOnly = readOnly,
        supportingText = supportingText, errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { latestSearch.value?.invoke() }),
    )
}

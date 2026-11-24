package com.mad.maduikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A labeled, controlled radio choice. The entire row requests a new value
 * through [onSelect] if enabled and unselected; selected rows do not request again.
 * The host supplies [selected] and controls exclusivity (e.g. a selectableGroup).
 * Supply a nonblank localized [label]. No callback is requested while disabled.
 * Use [modifier] for layout, without adding a second accessible name or action.
 * The default touch target is at least 48 dp in both axes if constraints allow.
 */
@Composable
public fun MadRadio(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    require(label.isNotBlank()) { "MadRadio requires a nonblank label" }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { if (enabled && !selected) onSelect() },
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, modifier = Modifier.weight(1f, fill = false))
    }
}

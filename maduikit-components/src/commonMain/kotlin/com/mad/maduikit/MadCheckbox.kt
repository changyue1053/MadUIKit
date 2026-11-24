package com.mad.maduikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A labeled, controlled binary selection. The entire row requests a new value
 * through [onCheckedChange]; only the host's [checked] parameter changes selection.
 * Supply a nonblank localized [label]. No callback is requested while disabled.
 * Use [modifier] for layout, without adding a second accessible name or action.
 * The default touch target is at least 48 dp in both axes if constraints allow.
 */
@Composable
public fun MadCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    require(label.isNotBlank()) { "MadCheckbox requires a nonblank label" }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { requested -> if (enabled) onCheckedChange(requested) },
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, modifier = Modifier.weight(1f, fill = false))
    }
}

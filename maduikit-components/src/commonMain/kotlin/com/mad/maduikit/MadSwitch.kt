package com.mad.maduikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * One labeled, controlled switch. The whole row is actionable, and [checked] is
 * always supplied by the caller. [onCheckedChange] requests, but does not store,
 * a new value. Supply a nonblank, localized [label].
 * Long labels may wrap; the switch graphic keeps its space when constraints permit.
 * Existing themes provide colors and typography; the host remains the source of truth.
 */
@Composable
public fun MadSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    require(label.isNotBlank()) { "MadSwitch requires a nonblank label" }
    Row(
        modifier = modifier.heightIn(min = 48.dp).toggleable(
            value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f, fill = false))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

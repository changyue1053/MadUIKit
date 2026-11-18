package com.mad.maduikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** Groups controlled filter tabs for accessibility without prescribing size, spacing or appearance. */
@Composable
fun MadFilterTabs(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/**
 * A single controlled filter tab. [selected] is supplied by the caller; [onSelect] does not mutate
 * it internally. The slot owns all visuals and measured geometry, including its owner-provided
 * accessible text and target sizing. Use one selected tab per [MadFilterTabs] group.
 */
@Composable
fun MadFilterTab(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onSelect),
        content = content,
    )
}

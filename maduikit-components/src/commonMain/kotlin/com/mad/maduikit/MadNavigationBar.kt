package com.mad.maduikit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Stable host destination identity, localized visible label and caller-supplied decorative artwork. */
public data class MadNavigationItem(
    public val id: String,
    public val label: String,
    public val painter: Painter,
    public val enabled: Boolean = true,
) {
    init {
        require(id.isNotBlank()) { "MadNavigationItem requires a nonblank id" }
        require(label.isNotBlank()) { "MadNavigationItem requires a nonblank label" }
    }
}

/**
 * Controlled bottom destination bar for two to five destinations in host order.
 * [selectedId] must identify an item and remains host-owned, including if the
 * current item is disabled. A selected or disabled item never requests selection.
 * Every activation of another enabled item requests its stable ID; the host
 * decides whether to accept it and can suppress pending duplicates.
 * Provide bounded width of at least 48 dp per destination for touch targets.
 * A label wraps to two lines and visually ellipsizes. Semantics keep it complete.
 * The host supplies inset padding, destination content and routing, if any.
 */
@Composable
public fun MadNavigationBar(
    items: List<MadNavigationItem>,
    selectedId: String,
    onDestinationRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(items.size in 2..5) { "MadNavigationBar requires two to five items" }
    require(items.map { it.id }.toSet().size == items.size) { "MadNavigationBar item ids must be unique" }
    require(items.any { it.id == selectedId }) { "MadNavigationBar selectedId must identify an item" }

    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).background(colors.surfaceContainer).selectableGroup()) {
        items.forEach { item ->
            key(item.id) {
                val selected = item.id == selectedId
                Column(
                    Modifier.weight(1f).fillMaxHeight().defaultMinSize(minHeight = 64.dp)
                        .selectable(selected, enabled = item.enabled, role = Role.Tab) {
                            if (item.enabled && !selected) onDestinationRequest(item.id)
                        }.padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    val iconSize = Modifier.size(width = 42.dp, height = 28.dp)
                    Box(if (selected) iconSize.background(colors.secondaryContainer, CircleShape) else iconSize,
                        contentAlignment = Alignment.Center) {
                        Image(item.painter, contentDescription = null,
                            modifier = Modifier.size(20.dp).alpha(if (item.enabled) 1f else .38f))
                    }
                    val baseColor = if (selected) colors.onSurface else colors.onSurfaceVariant
                    Text(item.label, color = if (item.enabled) baseColor else baseColor.copy(alpha = baseColor.alpha*.38f),
                        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

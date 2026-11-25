package com.mad.maduikit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A static or actionable text row for host-owned lists. Nonblank [label] and
 * optional [supportingText] are localized by the host. The label wraps to two
 * lines and support to three; longer content ellipsizes visually but keeps its
 * full text in semantics.
 *
 * Without [onClick], the row is static and [enabled] does not apply. Otherwise
 * the entire row is one action, at least 48 dp tall when constraints allow.
 * Put other interactive elements beside, not inside, the row; the host owns
 * keys, list state, navigation and actions.
 */
@Composable
public fun MadListItem(
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    require(label.isNotBlank()) { "MadListItem requires a nonblank label" }
    require(supportingText == null || supportingText.isNotBlank()) {
        "MadListItem supportingText must be nonblank or null"
    }
    val interaction = if (onClick == null) Modifier else {
        Modifier.clickable(enabled = enabled, role = Role.Button) { if (enabled) onClick() }
    }
    val disabled = onClick != null && !enabled
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(interaction)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = if (disabled) colors.onSurface.copy(alpha = colors.onSurface.alpha * .38f) else colors.onSurface,
            style = typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (supportingText != null) {
            Text(supportingText, color = if (disabled) colors.onSurfaceVariant.copy(alpha = colors.onSurfaceVariant.alpha * .38f) else colors.onSurfaceVariant,
                style = typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

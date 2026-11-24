package com.mad.maduikit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Displays a caller-supplied icon. Pass null for decoration, or a nonblank,
 * localized [contentDescription] when the icon itself conveys meaning.
 * Pass [Color.Unspecified] as [tint] to retain the painter's original colors.
 */
@Composable
public fun MadIcon(
    painter: Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    require(contentDescription == null || contentDescription.isNotBlank()) {
        "MadIcon requires a nonblank description or null"
    }
    Icon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

/**
 * A single icon action with a required nonblank, localized accessibility [label].
 * The icon is decorative within the action. The host owns all effects of [onClick].
 * The default minimum target is 48dp when supported by the parent's constraints.
 */
@Composable
public fun MadIconButton(
    painter: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    require(label.isNotBlank()) { "MadIconButton requires a nonblank label" }
    val color = LocalContentColor.current
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        MadIcon(painter = painter, tint = if (enabled) color else color.copy(alpha = color.alpha * 0.38f))
    }
}

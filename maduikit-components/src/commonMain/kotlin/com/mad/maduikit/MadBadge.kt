package com.mad.maduikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Read-only rounded badge for a short status or formatted count. Both strings are
 * localized by the caller. [spokenLabel] optionally replaces the visible [text]
 * in semantics (for example, "12 unread messages" for "12").
 *
 * The badge wraps when space is limited and grows with font scale when allowed.
 * The caller formats/caps counts and owns the announcement policy; the badge
 * has no click, dismiss, live-region, or automatically announced update.
 */
@Composable
public fun MadBadge(text: String, modifier: Modifier = Modifier, spokenLabel: String? = null) {
    require(text.isNotBlank()) { "MadBadge requires nonblank text" }
    require(spokenLabel == null || spokenLabel.isNotBlank()) {
        "MadBadge spokenLabel must be nonblank or null"
    }
    val colors = MaterialTheme.colorScheme
    val semantics = if (spokenLabel == null) Modifier else Modifier.clearAndSetSemantics {
        contentDescription = spokenLabel
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(colors.secondaryContainer, RoundedCornerShape(8.dp))
            .then(semantics)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colors.onSecondaryContainer,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

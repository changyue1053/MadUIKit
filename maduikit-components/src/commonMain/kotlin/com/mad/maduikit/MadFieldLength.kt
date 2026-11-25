package com.mad.maduikit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

/**
 * Read-only, localized counter or limit hint. [current] and [limit] are nonnegative,
 * in the same unit chosen by the host. Use [message] for visible status and name the
 * over-limit condition in words when [current] exceeds [limit]. [spokenMessage] can
 * replace that text in accessibility; it must carry the same meaning.
 *
 * This does not receive, count, or modify the field's text or composition.
 * It adds no live region and does not submit or validate on the host's behalf.
 */
@Composable
public fun MadFieldLength(
    current: Int,
    limit: Int,
    message: String,
    modifier: Modifier = Modifier,
    spokenMessage: String? = null,
) {
    require(current >= 0 && limit >= 0) { "MadFieldLength counts must be nonnegative" }
    require(message.isNotBlank()) { "MadFieldLength requires a nonblank message" }
    require(spokenMessage == null || spokenMessage.isNotBlank()) {
        "MadFieldLength spokenMessage must be nonblank or null"
    }
    val colors = MaterialTheme.colorScheme
    val label = if (spokenMessage == null) Modifier else Modifier.clearAndSetSemantics {
        contentDescription = spokenMessage
    }
    Text(
        message, modifier = modifier.then(label),
        color = if (current > limit) colors.error else colors.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

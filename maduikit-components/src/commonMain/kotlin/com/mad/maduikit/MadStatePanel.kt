package com.mad.maduikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

public enum class MadPanelState { Loading, Empty, Error }

/** Optional host-owned action for empty or error content. */
public class MadStatePanelAction(public val label: String, public val onClick: () -> Unit) {
    init {
        require(label.isNotBlank()) { "MadStatePanelAction requires a nonblank label" }
    }
}

/**
 * Host-controlled loading, empty, or error content. Supply a localized nonblank
 * [title] and optionally [message]. Loading displays quiet, stationary unknown
 * progress and hides [action]; no state announces itself automatically.
 * An empty or error action emits intent only. Jobs, retries and state stay with the host.
 */
@Composable
public fun MadStatePanel(
    state: MadPanelState,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: MadStatePanelAction? = null,
) {
    require(title.isNotBlank()) { "MadStatePanel requires a nonblank title" }
    require(message == null || message.isNotBlank()) { "MadStatePanel message must be nonblank or null" }

    val currentState = rememberUpdatedState(state)
    val currentAction = rememberUpdatedState(action)
    val colors = MaterialTheme.colorScheme
    val spacing = MadSpacingDefaults.Standard
    MadCard(modifier = modifier.fillMaxWidth(), borderRole = MadBorderRole.Subtle) {
        Column(
            Modifier.fillMaxWidth().padding(spacing.mediumDp.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.mediumDp.dp),
        ) {
            Text(
                title, modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = if (state == MadPanelState.Error) colors.error else colors.onSurface,
            )
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (state == MadPanelState.Loading) {
                MadLinearProgress(fraction = null)
            } else if (action != null) {
                key(state, action.label) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = spacing.mediumDp.dp, vertical = spacing.smallDp.dp),
                        onClick = {
                            val now = currentAction.value
                            if (currentState.value == state && now?.label == action.label) now.onClick()
                        },
                    ) { Text(action.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

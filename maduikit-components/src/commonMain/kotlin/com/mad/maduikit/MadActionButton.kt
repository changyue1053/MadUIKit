package com.mad.maduikit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

public enum class MadButtonVariant { Primary, Secondary, Text, Danger }

/**
 * Variant action with a nonblank localized [label]. A non-null, nonblank localized
 * [loadingStatus] indicates loading: the action is disabled and the status is
 * exposed as a state description, not another name. Loading wins over [enabled].
 * The caller owns loading and may suppress a new activation by updating this input.
 */
@Composable
public fun MadActionButton(
    label: String,
    variant: MadButtonVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loadingStatus: String? = null,
) {
    require(label.isNotBlank()) { "MadActionButton requires a nonblank label" }
    require(loadingStatus == null || loadingStatus.isNotBlank()) {
        "MadActionButton loadingStatus must be nonblank or null"
    }

    val actionable = enabled && loadingStatus == null
    val activate = { if (actionable) onClick() }
    val hostModifier = if (loadingStatus == null) modifier else modifier.semantics {
        stateDescription = loadingStatus
    }
    val content: @Composable RowScope.() -> Unit = {
        if (loadingStatus != null) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp).clearAndSetSemantics {},
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(label)
    }
    when (variant) {
        MadButtonVariant.Primary -> Button(activate, hostModifier, actionable, content = content)
        MadButtonVariant.Secondary -> OutlinedButton(activate, hostModifier, actionable, content = content)
        MadButtonVariant.Text -> TextButton(activate, hostModifier, actionable, content = content)
        MadButtonVariant.Danger -> Button(
            onClick = activate,
            modifier = hostModifier,
            enabled = actionable,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            content = content,
        )
    }
}

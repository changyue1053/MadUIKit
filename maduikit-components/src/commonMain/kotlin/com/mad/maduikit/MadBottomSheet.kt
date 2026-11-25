package com.mad.maduikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private class SheetCloseLifetime(var active: Boolean = false)

/**
 * A controlled modal bottom sheet with text and a single explicit close action.
 * The host owns [visible]; an action only requests closure. Back, outside taps
 * and drag dismissal are disabled. The sheet opens expanded up to available
 * space (at most 480 dp) and scrolls its message separately.
 * Supply localized, nonblank [title], [message] and [closeLabel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MadBottomSheet(
    visible: Boolean,
    title: String,
    message: String,
    closeLabel: String,
    onCloseRequest: () -> Unit,
) {
    if (!visible) return

    require(title.isNotBlank()) { "MadBottomSheet requires a nonblank title" }
    require(message.isNotBlank()) { "MadBottomSheet requires a nonblank message" }
    require(closeLabel.isNotBlank()) { "MadBottomSheet requires a nonblank close label" }

    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden })
    val latestClose = rememberUpdatedState(onCloseRequest)
    val lifetime = remember { SheetCloseLifetime() }
    DisposableEffect(lifetime) {
        lifetime.active = true
        onDispose { lifetime.active = false }
    }
    val spacing = MadSpacingDefaults.Standard
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = state,
        sheetGesturesEnabled = false,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(spacing.largeDp.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.mediumDp.dp),
        ) {
            Text(
                title, modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                message, modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = { if (lifetime.active) latestClose.value() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(closeLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

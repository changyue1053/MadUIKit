package com.mad.maduikit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * A controlled confirmation modal. Confirm, Cancel and ambient dismissal
 * request separate host actions; none mutates [visible]. Back/Escape requests
 * dismissal by default, outside taps do not. The platform handles modal focus.
 * Supply nonblank, localized title, message and distinguishable button labels.
 */
@Composable
public fun MadConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false,
) {
    if (!visible) return
    require(title.isNotBlank()) { "MadConfirmDialog requires a nonblank title" }
    require(message.isNotBlank()) { "MadConfirmDialog requires a nonblank message" }
    require(confirmLabel.isNotBlank() && cancelLabel.isNotBlank()) {
        "MadConfirmDialog requires nonblank action labels"
    }
    require(confirmLabel.trim() != cancelLabel.trim()) {
        "MadConfirmDialog action labels must be distinct"
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(cancelLabel) } },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    )
}

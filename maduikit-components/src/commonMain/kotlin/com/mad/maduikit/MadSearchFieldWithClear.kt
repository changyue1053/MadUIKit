package com.mad.maduikit

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private class ClearActionLifetime(var active: Boolean = false)

/**
 * [MadSearchField] with a separately named, host-owned clear request.
 * The button exists only for editable, nonempty input. The host may decline
 * without changing the value, or accept by supplying a new empty [TextFieldValue].
 * Clear does not search, set the value, or explicitly change focus.
 */
@Composable
public fun MadSearchFieldWithClear(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    clearLabel: String,
    clearIcon: Painter,
    onClearRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: String? = null,
    errorMessage: String? = null,
) {
    require(clearLabel.isNotBlank()) { "MadSearchFieldWithClear requires a nonblank clearLabel" }
    require(label.trim() != clearLabel.trim()) { "MadSearchFieldWithClear label and clearLabel must be distinct" }
    val available = enabled && !readOnly && value.text.isNotEmpty()
    val latest = rememberUpdatedState(if (available) onClearRequest else null)
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            MadSearchField(label, value, onValueChange, onSearch, Modifier.fillMaxWidth(),
                enabled, readOnly, supportingText, errorMessage)
            if (available) {
                key(label, clearLabel) {
                    val lifetime = remember { ClearActionLifetime() }
                    DisposableEffect(lifetime) {
                        lifetime.active = true
                        onDispose { lifetime.active = false }
                    }
                    var focus by remember { mutableStateOf(false) }
                    var pressedKey by remember { mutableStateOf<Key?>(null) }
                    val outline = MaterialTheme.colorScheme.primary
                    val ring = if (focus) Modifier.border(2.dp, outline, CircleShape) else Modifier
                    val request = { if (lifetime.active) latest.value?.invoke(); Unit }
                    // Passive pointer activation must not take hardware focus from the editor.
                    // Keyboard users can independently focus and activate this sibling.
                    Box(
                        Modifier.size(48.dp).then(ring)
                            .onFocusChanged {
                                focus = it.isFocused
                                if (!focus) pressedKey = null
                            }
                            .pointerInput(lifetime) { detectTapGestures { request() } }
                            .onKeyEvent {
                                when (it.key) {
                                    Key.Enter, Key.Spacebar, Key.NumPadEnter -> {
                                        if (it.type == KeyEventType.KeyDown) pressedKey = it.key
                                        if (it.type == KeyEventType.KeyUp && pressedKey == it.key) {
                                            pressedKey = null
                                            request()
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }.focusable().semantics {
                                contentDescription = clearLabel
                                role = Role.Button
                                onClick { request(); true }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MadIcon(clearIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
    ) { children, c ->
        val clearWidth = 48.dp.roundToPx()
        val gap = 4.dp.roundToPx()
        val hasClear = children.size == 2
        val inline = hasClear && c.maxWidth >= 152.dp.roundToPx()
        val height = if (hasClear && !inline && c.hasBoundedHeight)
            (c.maxHeight - clearWidth - gap).coerceAtLeast(0) else c.maxHeight
        val editorWidth = if (inline && c.hasBoundedWidth) c.maxWidth - clearWidth - gap else c.maxWidth
        val editor = children[0].measure(Constraints(maxWidth = editorWidth, maxHeight = height))
        val clear = children.getOrNull(1)?.measure(Constraints(
            maxWidth = minOf(clearWidth, c.maxWidth), maxHeight = minOf(clearWidth, c.maxHeight)))
        val contentWidth = if (inline) editor.width + gap + (clear?.width ?: 0)
            else maxOf(editor.width, clear?.width ?: 0)
        val contentHeight = if (inline || clear == null) maxOf(editor.height, clear?.height ?: 0)
            else editor.height + gap + clear.height
        val width = contentWidth.coerceIn(c.minWidth, c.maxWidth)
        val totalHeight = contentHeight.coerceIn(c.minHeight, c.maxHeight)
        layout(width, totalHeight) {
            editor.placeRelative(0, 0)
            if (clear != null) {
                clear.placeRelative(width - clear.width, if (inline) 0 else (editor.height + gap).coerceAtMost((totalHeight - clear.height).coerceAtLeast(0)))
            }
        }
    }
}

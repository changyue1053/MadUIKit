package com.mad.maduikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/**
 * A bounded host surface with optional top and bottom slots and an exact
 * remaining content viewport. The top slot measures first; the bottom slot
 * gets what remains, then content. At low height content may have zero space.
 * Each slot is clipped to its own measured bounds and retains its composition
 * across size changes. Use a bounded parent, with scrolling inside a slot.
 *
 * No route, window-inset, IME, focus, or scroll policy is installed. The host
 * applies any desired system padding once, e.g. on [modifier].
 */
@Composable
public fun MadScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        content = {
            Box(Modifier.clipToBounds()) { topBar?.invoke() }
            Box(Modifier.clipToBounds()) { bottomBar?.invoke() }
            Box(Modifier.clipToBounds()) { content() }
        },
    ) { slots, viewport ->
        require(viewport.hasBoundedWidth && viewport.hasBoundedHeight) {
            "MadScaffold requires a bounded width and height"
        }
        val width = viewport.maxWidth
        val height = viewport.maxHeight
        fun available(height: Int) = Constraints(minWidth = width, maxWidth = width, maxHeight = height)
        val top = slots[0].measure(available(height))
        val bottom = slots[1].measure(available(height - top.height))
        val body = slots[2].measure(Constraints.fixed(width, height - top.height - bottom.height))
        layout(width, height) {
            top.placeRelative(0, 0)
            body.placeRelative(0, top.height)
            bottom.placeRelative(0, height - bottom.height)
        }
    }
}

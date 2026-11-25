package com.mad.maduikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One caller-owned action; the painter is decorative and [label] names the action. */
public class MadTopAppBarAction(
    public val painter: Painter,
    public val label: String,
    public val onClick: () -> Unit,
) {
    init {
        require(label.isNotBlank()) { "MadTopAppBarAction requires a nonblank label" }
    }
}

/**
 * Non-collapsing, unscrolled top bar with a localized [title] and at most one
 * logical-start navigation and one logical-end action. The host owns routes,
 * effects and system insets. It also owns the artwork direction (e.g. mirroring arrows).
 * Labels must be distinct; the title keeps full semantics even when ellipsized.
 * Small available widths place actions below the title and eventually on separate
 * lines; supply at least 48 dp available width for a 48 dp action target.
 */
@Composable
public fun MadTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: MadTopAppBarAction? = null,
    action: MadTopAppBarAction? = null,
) {
    require(title.isNotBlank()) { "MadTopAppBar requires a nonblank title" }
    require(navigation == null || action == null || navigation.label.trim() != action.label.trim()) {
        "MadTopAppBar action labels must be distinct"
    }

    val colors = MaterialTheme.colorScheme
    val actionCount = listOfNotNull(navigation, action).size
    val icon: @Composable (MadTopAppBarAction) -> Unit = { control ->
        MadIconButton(control.painter, control.label, control.onClick, Modifier.size(48.dp))
    }
    val text: @Composable (Modifier) -> Unit = { position ->
        Text(
            title, modifier = position.padding(horizontal = 8.dp, vertical = 4.dp).semantics { heading() },
            color = colors.onSurface, style = MaterialTheme.typography.titleLarge,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        BoxWithConstraints(modifier.fillMaxWidth().background(colors.surfaceContainer)) {
            val availableWidth = maxWidth
            if (actionCount == 0 || availableWidth >= (64 + 48 * actionCount).dp) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (navigation != null) icon(navigation)
                    text(Modifier.weight(1f))
                    if (action != null) icon(action)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    text(Modifier.fillMaxWidth())
                    if (actionCount == 2 && availableWidth < 96.dp) {
                        if (navigation != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { icon(navigation) }
                        }
                        if (action != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { icon(action) }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = when {
                            navigation == null -> Arrangement.End
                            action == null -> Arrangement.Start
                            else -> Arrangement.SpaceBetween
                        }) {
                            if (navigation != null) icon(navigation)
                            if (action != null) icon(action)
                        }
                    }
                }
            }
        }
    }
}

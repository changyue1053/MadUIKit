package com.mad.maduikit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A static, clipped content surface. The host owns size, padding inside the
 * slot, content semantics and child actions. The card itself is not an action.
 * Geometry uses the explicit role values; null [borderRole] draws no border.
 * The nearest theme supplies surface, border and scoped content colors.
 */
@Composable
public fun MadCard(
    modifier: Modifier = Modifier,
    cornerRole: MadCornerRole = MadCornerRole.Medium,
    cornerRadii: MadCornerRadii = MadGeometryDefaults.CornerRadii,
    borderRole: MadBorderRole? = null,
    borderWidths: MadBorderWidths = MadGeometryDefaults.BorderWidths,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadii[cornerRole].dp)
    val outline = if (borderRole == null) Modifier else {
        Modifier.border(borderWidths[borderRole].dp, colors.outlineVariant, shape)
    }
    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        Column(
            modifier = modifier.clip(shape).background(colors.surfaceContainer).then(outline),
            content = content,
        )
    }
}

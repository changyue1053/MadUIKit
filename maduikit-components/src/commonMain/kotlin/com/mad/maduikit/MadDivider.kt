package com.mad.maduikit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Decorative horizontal divider. Adopts a border-width role and takes the
 * bounded available width; give it an explicit width in an unbounded parent.
 * If the host constrains its height it never paints outside the measured bounds.
 */
@Composable
public fun MadDivider(
    modifier: Modifier = Modifier,
    borderWidths: MadBorderWidths = MadGeometryDefaults.BorderWidths,
    role: MadBorderRole = MadBorderRole.Subtle,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val thickness = borderWidths[role].dp
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .drawBehind {
                drawRect(color, size = Size(size.width, minOf(size.height, thickness.toPx())))
            },
    )
}

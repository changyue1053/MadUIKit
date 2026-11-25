package com.mad.maduikit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Noninteractive avatar. Pass null [contentDescription] deliberately for decorative use;
 * otherwise give one localized, nonblank name (not a repetition of adjacent text).
 * The host owns [painter] loading and [initials] formatting. A painter takes priority.
 * Without one, initials appear only if a single line fits at the current text scale;
 * otherwise a decorative dot is drawn. No text is shortened or parsed by the SDK.
 * The default allocation is 40 dp; a smaller rectangular allocation still contains a
 * centered circle, whose diameter is bounded by the shorter side.
 */
@Composable
public fun MadAvatar(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    initials: String? = null,
) {
    require(contentDescription == null || contentDescription.isNotBlank()) {
        "MadAvatar contentDescription must be nonblank or null"
    }
    require(initials == null || (initials.isNotBlank() && initials.none { it in "\r\n\u0085\u2028\u2029" })) {
        "MadAvatar initials must be a nonblank single line or null"
    }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography.labelLarge
    val description = if (contentDescription == null) Modifier else Modifier.semantics {
        this.contentDescription = contentDescription
        role = Role.Image
    }
    BoxWithConstraints(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight)
        val circle = Modifier.size(diameter).clip(CircleShape).background(colors.primaryContainer).then(description)
        if (painter != null) {
            Image(painter, contentDescription = null, modifier = circle, contentScale = ContentScale.Crop)
        } else {
            val measurer = rememberTextMeasurer()
            val text = initials?.let { measurer.measure(it, style = type, maxLines = 1, softWrap = false) }
            Canvas(circle) {
                val limit = (size.minDimension - 4.dp.toPx()).coerceAtLeast(0f)
                if (text != null && !text.hasVisualOverflow &&
                    sqrt(text.size.width.toFloat() * text.size.width + text.size.height.toFloat() * text.size.height) <= limit
                ) {
                    drawText(text, color = colors.onPrimaryContainer,
                        topLeft = Offset((size.width - text.size.width) / 2f, (size.height - text.size.height) / 2f))
                } else if (size.minDimension > 0f) {
                    drawCircle(colors.onPrimaryContainer, radius = size.minDimension / 8f)
                }
            }
        }
    }
}

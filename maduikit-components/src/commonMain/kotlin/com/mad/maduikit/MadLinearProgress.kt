package com.mad.maduikit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Read-only horizontal progress. [fraction] is finite in 0..1, or null when the
 * amount is unknown. Null paints a stationary two-segment motif, not a percentage;
 * neither mode animates or needs a platform motion setting. Progress follows the
 * layout direction. There is no automatic announcement policy or work scheduling.
 *
 * Supply a localized nonblank [label] only when neighboring content does not name
 * the operation. Supply a bounded width if the parent does not constrain it.
 */
@Composable
public fun MadLinearProgress(
    fraction: Float?,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    require(fraction == null || fraction.isFinite() && fraction in 0f..1f) {
        "MadLinearProgress fraction must be finite and within 0..1, or null"
    }
    require(label == null || label.isNotBlank()) { "MadLinearProgress label must be nonblank or null" }
    val colors = MaterialTheme.colorScheme
    val track = colors.secondaryContainer
    val fill = colors.primary
    Canvas(modifier.fillMaxWidth().height(MadSpacingDefaults.Standard.smallDp.dp)
        .clip(CircleShape).semantics {
            progressBarRangeInfo = if (fraction == null) ProgressBarRangeInfo.Indeterminate
                else ProgressBarRangeInfo(fraction, 0f..1f)
            if (label != null) contentDescription = label
        }) {
        drawRect(track)
        fun segment(start: Float, end: Float) {
            val left = if (layoutDirection == LayoutDirection.Rtl) 1f-end else start
            drawRect(fill, topLeft = Offset(left*size.width, 0f), size = Size((end-start)*size.width, size.height))
        }
        if (fraction == null) {
            segment(.12f, .34f)
            segment(.55f, .77f)
        } else if (fraction > 0f) {
            segment(0f, fraction)
        }
    }
}

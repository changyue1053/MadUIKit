package com.mad.maduikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

public enum class MadBannerTone { Neutral, Warning, Error }

/** One host-owned follow-up or dismiss intent. */
public class MadBannerAction(public val label: String, public val onClick: () -> Unit) {
    init {
        require(label.isNotBlank()) { "MadBannerAction requires a nonblank label" }
    }
}

private class BannerActionLifetime(var active: Boolean = false)

/**
 * Durable in-flow notice; unlike a state panel, it sits beside existing content.
 * [statusLabel] identifies the tone in localized visible text, and [message]
 * explains what happened. [tone] changes the accent, not the message or semantics.
 * An optional [action] requests one host operation; the host decides whether to
 * remove the banner. No timeout, live region or automatic announcement is added.
 */
@Composable
public fun MadBanner(
    tone: MadBannerTone,
    statusLabel: String,
    message: String,
    modifier: Modifier = Modifier,
    action: MadBannerAction? = null,
) {
    require(statusLabel.isNotBlank()) { "MadBanner requires a nonblank statusLabel" }
    require(message.isNotBlank()) { "MadBanner requires a nonblank message" }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val spacing = MadSpacingDefaults.Standard
    val shape = RoundedCornerShape(MadGeometryDefaults.CornerRadii[MadCornerRole.Small].dp)
    val lineColor = when (tone) {
        MadBannerTone.Neutral -> colors.primary
        MadBannerTone.Warning -> colors.tertiary
        MadBannerTone.Error -> colors.error
    }
    val thickness = MadGeometryDefaults.BorderWidths[MadBorderRole.Strong].dp
    Column(
        modifier = modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainer)
            .drawBehind {
                val stripe = minOf(size.width, thickness.toPx())
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - stripe else 0f
                drawRect(lineColor, Offset(x, 0f), Size(stripe, size.height))
            }.padding(horizontal = spacing.mediumDp.dp, vertical = spacing.smallDp.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.smallDp.dp),
    ) {
        Text(statusLabel, style = type.labelLarge, color = colors.onSurface)
        Text(message, style = type.bodyMedium, color = colors.onSurfaceVariant)
        if (action != null) {
            key(tone, statusLabel, message, action.label) {
                val callback = rememberUpdatedState(action.onClick)
                val lifetime = remember { BannerActionLifetime() }
                DisposableEffect(lifetime) {
                    lifetime.active = true
                    onDispose { lifetime.active = false }
                }
                TextButton(
                    onClick = { if (lifetime.active) callback.value() },
                    modifier = Modifier.fillMaxWidth().sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    contentPadding = PaddingValues(8.dp, 4.dp),
                ) {
                    Text(action.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

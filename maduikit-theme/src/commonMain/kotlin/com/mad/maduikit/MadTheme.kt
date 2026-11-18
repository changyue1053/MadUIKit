package com.mad.maduikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/** Semantic accent pair. The caller is responsible for custom color contrast. */
@Immutable
public data class MadColors(public val primary: Color, public val onPrimary: Color)

/** Default semantic color pairs for the P0 runtime. */
public object MadColorDefaults {
    public val Light: MadColors = MadColors(Color(0xFF3153A6), Color.White)
    public val Dark: MadColors = MadColors(Color(0xFFBAC8FF), Color(0xFF09265F))
}

/** Applies the SDK theme to a Compose subtree; follows system dark mode by default. */
@Composable
public fun MadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: MadColors = if (darkTheme) MadColorDefaults.Dark else MadColorDefaults.Light,
    content: @Composable () -> Unit,
) {
    val scheme = remember(darkTheme, colors) {
        if (darkTheme) darkColorScheme(primary = colors.primary, onPrimary = colors.onPrimary)
        else lightColorScheme(primary = colors.primary, onPrimary = colors.onPrimary)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

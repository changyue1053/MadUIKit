package com.mad.maduikit

import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class MadThemeTest {
    @Test
    fun defaultAccentPairsMeetNormalTextContrastThreshold() {
        for (colors in listOf(MadColorDefaults.Light, MadColorDefaults.Dark)) {
            val background = colors.primary.luminance()
            val foreground = colors.onPrimary.luminance()
            val ratio = (max(background, foreground) + 0.05f) /
                (min(background, foreground) + 0.05f)
            assertTrue(ratio >= 4.5f, "$colors has contrast $ratio:1")
        }
    }
}

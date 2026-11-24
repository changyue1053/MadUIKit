package com.mad.maduikit

public enum class MadCornerRole { Small, Medium, Large }
public enum class MadBorderRole { Subtle, Strong }

/** Opt-in corner radii in dp. Zero is a square corner; roles must be ordered. */
public data class MadCornerRadii(
    public val smallDp: Int,
    public val mediumDp: Int,
    public val largeDp: Int,
) {
    init {
        require(0 <= smallDp && smallDp <= mediumDp && mediumDp <= largeDp) {
            "Corner radii must be nonnegative and ordered: small <= medium <= large"
        }
    }

    public operator fun get(role: MadCornerRole): Int = when (role) {
        MadCornerRole.Small -> smallDp
        MadCornerRole.Medium -> mediumDp
        MadCornerRole.Large -> largeDp
    }
}

/** Opt-in positive widths in dp; equal widths are allowed. Zero is not hairline. */
public data class MadBorderWidths(public val subtleDp: Int, public val strongDp: Int) {
    init {
        require(0 < subtleDp && subtleDp <= strongDp) {
            "Border widths must be positive and ordered: subtle <= strong"
        }
    }

    public operator fun get(role: MadBorderRole): Int = when (role) {
        MadBorderRole.Subtle -> subtleDp
        MadBorderRole.Strong -> strongDp
    }
}

/** Starting points for opt-in geometry; does not alter MadTheme or existing widgets. */
public object MadGeometryDefaults {
    public val CornerRadii: MadCornerRadii = MadCornerRadii(4, 8, 16)
    public val BorderWidths: MadBorderWidths = MadBorderWidths(1, 2)
}

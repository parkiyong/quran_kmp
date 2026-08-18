package io.github.parkiyong.quran.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    companion object {
        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < 600.dp -> COMPACT
            width < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

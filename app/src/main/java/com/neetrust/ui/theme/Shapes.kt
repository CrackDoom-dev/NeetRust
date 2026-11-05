package com.neetrust.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import com.neetrust.ui.config.CornerPreset

fun neetRustShapes(corner: CornerPreset): Shapes = Shapes(
    extraSmall = RoundedCornerShape(corner.radiusL / 2),
    small = RoundedCornerShape(corner.radiusL),
    medium = RoundedCornerShape(corner.radiusL),
    large = RoundedCornerShape(corner.radiusXL),
    extraLarge = RoundedCornerShape(corner.radiusXL)
)

val NeetRustShapes: Shapes = neetRustShapes(CornerPreset.MODERN)

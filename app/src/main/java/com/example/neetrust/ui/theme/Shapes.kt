// [KATALOG] ui.theme.Shapes
package com.example.neetrust.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import com.example.neetrust.ui.config.CornerPreset

/** Semua bentuk Material mengikuti preset corner (SHARP, MODERN, ROUND). */
fun neetRustShapes(corner: CornerPreset): Shapes = Shapes(
    extraSmall = RoundedCornerShape(corner.radiusL / 2),
    small = RoundedCornerShape(corner.radiusL),
    medium = RoundedCornerShape(corner.radiusL),
    large = RoundedCornerShape(corner.radiusXL),
    extraLarge = RoundedCornerShape(corner.radiusXL)
)

/** Alias lama, dipertahankan agar kode lama tetap terkompilasi. */
val NeetRustShapes: Shapes = neetRustShapes(CornerPreset.MODERN)

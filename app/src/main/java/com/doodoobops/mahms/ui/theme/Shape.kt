package com.doodoobops.mahms.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii: XS 4 / SM 8 / MD 12 (default) / LG 16 / XL 24 / Pill 999. */
object Radii {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(999.dp)
}

val MahmsShapes = Shapes(
    extraSmall = Radii.xs,
    small = Radii.sm,
    medium = Radii.md,
    large = Radii.lg,
    extraLarge = Radii.xl,
)

/** Spacing scale on a 4dp base. */
object Spacing {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x10 = 40.dp
    val x12 = 48.dp
    val x16 = 64.dp
}

package com.sachin.compassnav.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Colors defined by user request
val DeepBlack = Color(0xFF0A0A0A)
val DarkPurpleTint = Color(0xFF0D0014)
val ElectricPurple = Color(0xFFA855F7)
val HotPink = Color(0xFFEC4899)
val NeonCyan = Color(0xFF06B6D4)
val BrightOrangeRed = Color(0xFFFF4500)
val PureWhite = Color(0xFFFFFFFF)
val SoftLavender = Color(0xFFC4B5FD)
val GlassDark = Color(0x331A1A2E) // ~20% opacity
val CardBorder = Color(0x44A855F7) // Glowing border using Electric Purple

// Reusable gradients
val PurplePinkGradient = Brush.horizontalGradient(listOf(ElectricPurple, HotPink))
val PurpleCyanGradient = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan))
val RadialSpaceGradient = Brush.radialGradient(
    colors = listOf(Color(0xFF1A0033), DeepBlack)
)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    secondary = HotPink,
    tertiary = NeonCyan,
    background = DeepBlack,
    surface = GlassDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = PureWhite,
    onSurface = PureWhite
)

@Composable
fun CompassNavTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

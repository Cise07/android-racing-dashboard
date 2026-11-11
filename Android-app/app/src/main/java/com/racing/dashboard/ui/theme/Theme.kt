package com.racing.dashboard.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ascher Hardware Bezel Colors
val BezelDark = Color(0xFF101214)
val BezelSurface = Color(0xFF181B1E)
val BezelBorder = Color(0xFF2B2E33)

// Screen Cockpit Colors
val ScreenBackground = Color(0xFF060709)
val CompartmentBorder = Color(0xFF22252A)
val CompartmentSurface = Color(0xFF0C0E12)

// Neon & Display Colors matching Ascher display
val AscherLimeYellow = Color(0xFFD6E823) // Center Speed, Gear, RPM
val AscherCyan = Color(0xFF45D0DF)       // Brake Bias, Fuel labels
val AscherGreen = Color(0xFF2EEB5E)      // Best Lap, Green Shift LEDs
val AscherRed = Color(0xFFFF4955)        // Red Shift LEDs, Warnings
val AscherSalmon = Color(0xFFFA6B75)     // Delta label
val AscherBlue = Color(0xFF5CE1E6)       // Blue Shift LEDs
val AscherAmber = Color(0xFFE5A836)      // Asphalt Temp label
val AscherYellowLed = Color(0xFFFFD700)  // Side 6 yellow LEDs

// Aliases for compatibility
val NeonGreen = AscherGreen
val NeonYellow = AscherLimeYellow
val GearShiftRed = AscherRed
val NeonBlue = AscherBlue

// Card tints
val DeltaCardBackground = Color(0xFF330D15)
val DeltaCardBorder = Color(0xFF5A1624)
val AsphaltCardBackground = Color(0xFF33230C)
val AsphaltCardBorder = Color(0xFF593C14)
val LeftEncoderBackground = Color(0xFF581419)
val RightEncoderBackground = Color(0xFF112A4F)

val RacingDarkColors = darkColorScheme(
    primary = AscherLimeYellow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A2E05),
    secondary = AscherCyan,
    onSecondary = Color.Black,
    background = ScreenBackground,
    onBackground = Color(0xFFE6E8EC),
    surface = CompartmentSurface,
    onSurface = Color(0xFFE6E8EC),
    surfaceVariant = CompartmentSurface,
    outline = CompartmentBorder,
    outlineVariant = CompartmentBorder,
    error = AscherRed,
    onError = Color.White
)

@Composable
fun RacingDashboardTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingDarkColors,
        typography = Typography,
        content = content
    )
}

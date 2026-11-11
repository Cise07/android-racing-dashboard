package com.racing.dashboard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.racing.dashboard.ui.theme.*

@Composable
fun ShiftLightsBar(
    rpm: Int,
    maxRpm: Int,
    isShiftLightActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Dynamic calibrated RPM ratio for EVERY vehicle in both games
    val effectiveMaxRpm = if (maxRpm > 1000) maxRpm.toFloat() else 7500f
    val ratio = (rpm.toFloat() / effectiveMaxRpm).coerceIn(0f, 1.15f)

    // Shift point reached at 97% of max RPM or when vehicle telemetry triggers shift flag
    val isShiftPoint = ratio >= 0.97f || isShiftLightActive

    // Shift blink animation: high-frequency racing strobe (65ms cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "shift_flash")
    val flashState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 65, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )
    val flashOn = flashState > 0.5f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val ledCount = 15
        val availableWidth = size.width * 0.72f // center 72% for the arched bar
        val startX = (size.width - availableWidth) / 2f
        val spacing = availableWidth / (ledCount - 1)
        val radius = 9.dp.toPx()

        for (i in 0 until ledCount) {
            val cx = startX + i * spacing
            // Gentle parabolic arch: highest in the middle
            val normX = (i - 7) / 7f
            val cy = size.height * 0.72f - (1f - (normX * normX)) * 6.dp.toPx()

            // Pin-point stepped LED activation thresholds:
            // 0..4 (5 Green): 70%, 72%, 74%, 76%, 78% (rev range entry)
            // 5..9 (5 Red): 80%, 82%, 84%, 86%, 88% (power peak)
            // 10..14 (5 Blue): 90%, 91.5%, 93%, 94.5%, 96% (shift approach)
            val threshold = when {
                i < 5 -> 0.70f + i * 0.02f
                i < 10 -> 0.80f + (i - 5) * 0.02f
                else -> 0.90f + (i - 10) * 0.015f
            }
            val isLitNormally = ratio >= threshold

            val isLit = if (isShiftPoint) {
                flashOn // Strobe flash at redline shift point
            } else {
                isLitNormally
            }

            // Colors: authentic GT3 cockpit style
            // During shift flash: all LEDs strobe in electric hyper-cyan
            val (litColor, unlitColor, glowColor) = if (isShiftPoint) {
                Triple(
                    Color(0xFFE8FFFF),
                    Color(0xFF0F2B36),
                    Color(0xFF00E5FF).copy(alpha = 0.75f)
                )
            } else {
                when {
                    i < 5 -> Triple(
                        AscherGreen,
                        Color(0xFF0F2615),
                        AscherGreen.copy(alpha = 0.5f)
                    )
                    i < 10 -> Triple(
                        AscherRed,
                        Color(0xFF301014),
                        AscherRed.copy(alpha = 0.5f)
                    )
                    else -> Triple(
                        AscherBlue,
                        Color(0xFF0F2B36),
                        AscherBlue.copy(alpha = 0.5f)
                    )
                }
            }

            // Draw outer metallic housing bezel
            drawCircle(
                color = Color(0xFF2B2E33),
                radius = radius + 2.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF0C0E10),
                radius = radius + 1.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Draw LED circle
            if (isLit) {
                // High-intensity halo glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius * 2.3f
                    ),
                    radius = radius * 2.3f,
                    center = Offset(cx, cy)
                )
                // Lit LED core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, litColor),
                        center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            } else {
                // Unlit LED
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(unlitColor, Color(0xFF07090B)),
                        center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun SideLeds(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = 3
        val spacing = size.height / (count + 1)
        val radius = 7.dp.toPx()
        val cx = size.width / 2f

        for (i in 1..count) {
            val cy = i * spacing
            val isUpperWarning = i == 1

            // Outer bezel
            drawCircle(
                color = Color(0xFF2A2D33),
                radius = radius + 2.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF0E1012),
                radius = radius + 1.dp.toPx(),
                center = Offset(cx, cy)
            )

            val unlitYellow = Color(0xFF2E270A)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(unlitYellow, Color(0xFF0D0C03)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

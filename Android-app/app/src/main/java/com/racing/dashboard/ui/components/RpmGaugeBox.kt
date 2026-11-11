package com.racing.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.racing.dashboard.data.TelemetryState
import com.racing.dashboard.ui.theme.GearShiftRed
import com.racing.dashboard.ui.theme.NeonGreen
import com.racing.dashboard.ui.theme.NeonYellow

@Composable
fun RpmGaugeBox() {
    val telemetryState = TelemetryState.getInstance()
    val rpmData = telemetryState.telemetryData.collectAsState()

    val rpm = rpmData.value.rpm
    val maxRpm = rpmData.value.maxRpm
    val rpmRatio = if (maxRpm > 0) rpm.toFloat() / maxRpm.toFloat() else 0f

    val animatedRatio by animateFloatAsState(
        targetValue = rpmRatio,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 50f)
    )

    val isFlashing = rpmRatio >= 0.9f && (rpm % 60 < 30)

    val barColor = when {
        rpmRatio < 0.7f -> NeonGreen
        rpmRatio < 0.9f -> NeonYellow
        else -> if (isFlashing) GearShiftRed else Color.White
    }

    val backgroundColor = if (isFlashing) GearShiftRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RPM",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(backgroundColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            color = barColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        )
                )

                val fillWidth = animatedRatio * 100f
                val emptyWidth = (1f - fillWidth / 100f) * 100

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            color = barColor.copy(alpha = 0.8f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        )
                        .padding(start = emptyWidth.dp)
                ) {}

                Text(
                    text = "$rpm",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Companion.Center)
                )
            }
        }
    }
}
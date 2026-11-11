package com.racing.dashboard.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.racing.dashboard.data.TelemetryState

@Composable
fun GearIndicatorBox() {
    val telemetryState = TelemetryState.getInstance()
    val gear = telemetryState.telemetryData.collectAsState()

    val gearText = when (gear.value.gear) {
        0 -> "-"
        -1 -> "R"
        else -> gear.value.gear.toString()
    }

    val displayColor = when (gear.value.gear) {
        -1 -> Color(0xFFFF5555)
        0 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GEAR",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            )

            Text(
                text = gearText,
                color = displayColor,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = displayColor
                )
            )
        }
    }
}
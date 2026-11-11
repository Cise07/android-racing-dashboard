package com.racing.dashboard.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.racing.dashboard.data.TelemetryState
import com.racing.dashboard.ui.components.*
import com.racing.dashboard.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen() {
    val telemetryState = TelemetryState.getInstance()
    val telemetryData by telemetryState.telemetryData.collectAsState()
    val isConnected by telemetryState.isConnected.collectAsState()
    val isDemoMode by telemetryState.isDemoMode.collectAsState()
    val showBezel by telemetryState.showBezel.collectAsState()
    val showSettings by telemetryState.showSettingsDialog.collectAsState()

    // Real-time clock update (HH:mm:ss)
    var currentTime by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (showBezel) {
            // Steering Wheel Hardware Shell
            DashboardHardwareShell(
                rpm = telemetryData.rpm,
                maxRpm = telemetryData.maxRpm,
                isShiftLightActive = telemetryData.pitLimiterOn
            ) {
                DashboardDisplayContent(
                    data = telemetryData,
                    currentTime = currentTime,
                    onOpenSettings = { telemetryState.setShowSettingsDialog(true) }
                )
            }
        } else {
            // Screen-Only Mode (Fills 100% of mobile display)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBackground)
                    .padding(4.dp)
            ) {
                // Top Shift Lights
                ShiftLightsBar(
                    rpm = telemetryData.rpm,
                    maxRpm = telemetryData.maxRpm,
                    isShiftLightActive = telemetryData.pitLimiterOn,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Screen Content
                DashboardDisplayContent(
                    data = telemetryData,
                    currentTime = currentTime,
                    onOpenSettings = { telemetryState.setShowSettingsDialog(true) }
                )
            }
        }

        // Discrete subtle status indicator / quick settings button in top corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = { telemetryState.setShowSettingsDialog(true) },
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0x33000000), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isConnected) AscherGreen.copy(alpha = 0.7f)
                    else if (isDemoMode) AscherYellowLed.copy(alpha = 0.8f)
                    else Color(0x66FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(onDismiss = { telemetryState.setShowSettingsDialog(false) })
        }
    }
}

@Composable
private fun DashboardHardwareShell(
    rpm: Int,
    maxRpm: Int,
    isShiftLightActive: Boolean = false,
    content: @Composable () -> Unit
) {
    // Outer hardware enclosure matching steering wheel unit
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF22252A),
                        Color(0xFF141618),
                        Color(0xFF0F1012)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP BEZEL: Arched Shift Lights
            ShiftLightsBar(
                rpm = rpm,
                maxRpm = maxRpm,
                isShiftLightActive = isShiftLightActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(top = 2.dp)
            )

            // 2. MIDDLE ROW: Left 3 Yellow LEDs + Center Screen Display + Right 3 Yellow LEDs
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 3 vertical yellow LEDs
                SideLeds(
                    isLeft = true,
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight(0.7f)
                )

                // Center Display Unit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScreenBackground)
                        .border(1.5.dp, Color(0xFF26282E), RoundedCornerShape(8.dp))
                ) {
                    content()
                }

                // Right 3 vertical yellow LEDs
                SideLeds(
                    isLeft = false,
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight(0.7f)
                )
            }

            // 3. BOTTOM BEZEL: Clean Brandplate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                DashboardBrandplate()
            }
        }
    }
}

@Composable
private fun DashboardDisplayContent(
    data: com.racing.dashboard.data.model.TelemetryData,
    currentTime: String,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        // Status Bar Header: 00:00.000 | L 0 | P 0 | 15:13:53
        DashboardHeader(
            data = data,
            currentTime = currentTime,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Main Compartmentalized Telemetry Grid
        DashboardGrid(
            data = data,
            modifier = Modifier.weight(1f)
        )
    }
}

package com.racing.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racing.dashboard.data.model.TelemetryData
import com.racing.dashboard.ui.theme.*
import java.util.Locale

@Composable
fun DashboardHeader(
    data: TelemetryData,
    currentTime: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Header: Vehicle / Game Name (aligned to Left Column)
        Box(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val headerVehicle = if (data.vehicleName.isNotEmpty()) data.vehicleName else "RACING DASHBOARD"
            Text(
                text = headerVehicle.uppercase(Locale.getDefault()),
                color = Color(0xFFD4D8E0),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }

        // Center Header: Symmetrical LAP & POSITION indicators (aligned above Center Column)
        Box(
            modifier = Modifier.weight(1.55f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Symmetrical Lap Indicator Badge
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(22.dp)
                        .background(Color(0xFF13161C), RoundedCornerShape(5.dp))
                        .border(1.dp, Color(0xFF2B2F38), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val lapText = if (data.currentLap > 0) "L ${data.currentLap}" else "L 0"
                    Text(
                        text = lapText,
                        color = Color(0xFFE2E6EE),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Symmetrical Position Indicator Badge
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(22.dp)
                        .background(Color(0xFF13161C), RoundedCornerShape(5.dp))
                        .border(1.dp, Color(0xFF2B2F38), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val posText = if (data.position > 0) "P ${data.position}" else "P 0"
                    Text(
                        text = posText,
                        color = Color(0xFFE2E6EE),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Right Header: Real-time Clock (aligned to Right Column)
        Box(
            modifier = Modifier
                .weight(1.0f)
                .padding(end = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = currentTime,
                color = Color(0xFFD4D8E0),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun DashboardGrid(
    data: TelemetryData,
    modifier: Modifier = Modifier
) {
    val isForza = data.gameSource.contains("Forza", ignoreCase = true) ||
                  data.vehicleName.contains("Forza", ignoreCase = true)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 1. LEFT COLUMN (4 Boxes: BOOST, FUEL IN LITERS, THROTTLE/BRAKE, CLUTCH)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Box 1: BOOST (bar)
            val boostVal = if (data.boostBar > 0f) String.format(Locale.US, "%.2f", data.boostBar) else "0.00"
            TelemetryBox(
                label = "BOOST (bar)",
                labelColor = AscherCyan,
                value = boostVal,
                valueColor = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Box 2: FUEL (L)
            val fuelStr = if (isForza || data.fuelLiters == null) {
                "N/D"
            } else {
                String.format(Locale.US, "%.1f", data.fuelLiters)
            }
            TelemetryBox(
                label = "FUEL (L)",
                labelColor = AscherCyan,
                value = fuelStr,
                valueColor = if (fuelStr == "N/D") Color(0xFF8C92A0) else Color.White,
                modifier = Modifier.weight(1f)
            )

            // Box 3: Split Row THROTTLE (%) & BRAKE (%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TelemetryBox(
                    label = "THROTTLE (%)",
                    labelColor = Color(0xFF2DA8B5),
                    value = "${data.throttlePct}%",
                    valueColor = Color.White,
                    labelSize = 7.5.sp,
                    valueSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                TelemetryBox(
                    label = "BRAKE (%)",
                    labelColor = Color(0xFFE0525C),
                    value = "${data.brakePct}%",
                    valueColor = Color.White,
                    labelSize = 7.5.sp,
                    valueSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Box 4: CLUTCH (%)
            TelemetryBox(
                label = "CLUTCH (%)",
                labelColor = Color(0xFF558FC4),
                value = "${data.clutchPct}%",
                valueColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        // 2. CENTER COLUMN (~24% width: Speed, Giant Gear, RPM)
        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight()
                .background(
                    color = CompartmentSurface,
                    shape = RoundedCornerShape(7.dp)
                )
                .border(
                    width = 1.dp,
                    color = CompartmentBorder,
                    shape = RoundedCornerShape(7.dp)
                )
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed (Top)
            val speedDisplay = data.speedKmh.toInt().coerceAtLeast(0)
            Text(
                text = "$speedDisplay",
                color = AscherLimeYellow,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            // Giant Gear (Middle)
            val gearText = when (data.gear) {
                -1 -> "R"
                0, 11 -> "N"
                in 1..10 -> "${data.gear}"
                else -> "N"
            }
            Text(
                text = gearText,
                color = AscherLimeYellow,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 76.sp,
                lineHeight = 76.sp,
                textAlign = TextAlign.Center
            )

            // RPM (Bottom)
            val rpmDisplay = data.rpm.coerceAtLeast(0)
            Text(
                text = "$rpmDisplay",
                color = AscherLimeYellow,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
        }

        // 3. RIGHT COLUMN (4 Boxes: LAP TIME, BEST LAP, DELTA, WATER TEMP)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Box 1: LAP TIME (above BEST as requested!)
            val lapTimeStr = if (data.lapTimeSeconds > 0f) formatLapTime(data.lapTimeSeconds) else "00:00.000"
            TelemetryBox(
                label = "LAP TIME",
                labelColor = AscherCyan,
                value = lapTimeStr,
                valueColor = Color.White,
                valueSize = 22.sp,
                modifier = Modifier.weight(1f)
            )

            // Box 2: BEST LAP
            val bestLapStr = if (data.bestLapTimeSeconds > 0f) formatBestLap(data.bestLapTimeSeconds) else "00:00.000"
            TelemetryBox(
                label = "BEST",
                labelColor = AscherGreen,
                value = bestLapStr,
                valueColor = AscherGreen,
                valueSize = 22.sp,
                modifier = Modifier.weight(1f)
            )

            // Box 3: DELTA (Secs)
            val deltaStr = if (data.bestLapTimeSeconds > 0f && data.lapTimeSeconds > 0f) {
                if (data.deltaSeconds >= 0) {
                    String.format(Locale.US, "+%.2f", data.deltaSeconds)
                } else {
                    String.format(Locale.US, "%.2f", data.deltaSeconds)
                }
            } else {
                "+0.00"
            }
            TelemetryBox(
                label = "DELTA (Secs)",
                labelColor = AscherSalmon,
                value = deltaStr,
                valueColor = Color.White,
                backgroundColor = DeltaCardBackground,
                borderColor = DeltaCardBorder,
                modifier = Modifier.weight(1f)
            )

            // Box 4: WATER TEMP (°C) (Oil temp removed as requested!)
            val waterTempStr = if (isForza || data.waterTempC == null) {
                "N/D"
            } else {
                String.format(Locale.US, "%.1f", data.waterTempC)
            }
            TelemetryBox(
                label = "WATER TEMP (°C)",
                labelColor = AscherAmber,
                value = waterTempStr,
                valueColor = if (waterTempStr == "N/D") Color(0xFF8C92A0) else Color.White,
                backgroundColor = AsphaltCardBackground,
                borderColor = AsphaltCardBorder,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TelemetryBox(
    label: String,
    labelColor: Color,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    labelSize: androidx.compose.ui.unit.TextUnit = 8.5.sp,
    valueSize: androidx.compose.ui.unit.TextUnit = 26.sp,
    backgroundColor: Color = CompartmentSurface,
    borderColor: Color = CompartmentBorder
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(7.dp))
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = labelColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = labelSize,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = valueSize,
                textAlign = TextAlign.Start,
                maxLines = 1
            )
        }
    }
}

private fun formatLapTime(seconds: Float): String {
    if (seconds <= 0f) return "00:00.000"
    val mins = (seconds / 60).toInt()
    val remSecs = seconds - (mins * 60)
    val secs = remSecs.toInt()
    val millis = ((remSecs - secs) * 1000).toInt()
    return String.format(Locale.US, "%02d:%02d.%03d", mins, secs, millis)
}

private fun formatBestLap(seconds: Float): String {
    if (seconds <= 0f) return "00:00.000"
    val mins = (seconds / 60).toInt()
    val remSecs = seconds - (mins * 60)
    val secs = remSecs.toInt()
    val millis = ((remSecs - secs) * 1000).toInt()
    return String.format(Locale.US, "%02d:%02d.%03d", mins, secs, millis)
}

package com.racing.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.racing.dashboard.data.TelemetryState
import com.racing.dashboard.data.network.DEFAULT_PORT
import com.racing.dashboard.data.network.WebSocketClient
import com.racing.dashboard.ui.theme.*

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val telemetryState = TelemetryState.getInstance()
    val wsClient = WebSocketClient.getInstance()

    val isConnected by telemetryState.isConnected.collectAsState()
    val isDemoMode by telemetryState.isDemoMode.collectAsState()
    val statusText by telemetryState.connectionStatus.collectAsState()
    val showBezel by telemetryState.showBezel.collectAsState()

    var hostInput by remember { mutableStateOf(wsClient.serverHost) }
    var portInput by remember { mutableStateOf(wsClient.serverPort.toString()) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            val port = portInput.toIntOrNull() ?: DEFAULT_PORT
            wsClient.saveConfig(hostInput, port)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF14171D),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF383E4C)),
            modifier = Modifier
                .width(460.dp)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title and connection badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMPOSTAZIONI DASHBOARD",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )

                    // Status pill
                    val statusBg = when {
                        isDemoMode -> Color(0xFF332800)
                        isConnected -> Color(0xFF0F3819)
                        else -> Color(0xFF381014)
                    }
                    val statusColor = when {
                        isDemoMode -> AscherYellowLed
                        isConnected -> AscherGreen
                        else -> AscherRed
                    }
                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(6.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isDemoMode) "DEMO" else if (isConnected) "ONLINE" else "OFFLINE",
                            color = statusColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = statusText,
                    color = Color(0xFFA0A6B2),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                HorizontalDivider(color = Color(0xFF2A2E38), thickness = 1.dp)

                // Host & Port inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = {
                            hostInput = it
                            val port = portInput.toIntOrNull() ?: DEFAULT_PORT
                            wsClient.saveConfig(it, port)
                        },
                        label = { Text("IP Locale PC", color = Color(0xFF9EACB8), fontSize = 11.sp) },
                        placeholder = { Text("es. 192.168.1.21", color = Color(0xFF555D6B)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AscherCyan,
                            unfocusedBorderColor = Color(0xFF454B59)
                        ),
                        modifier = Modifier.weight(2.2f)
                    )

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = {
                            portInput = it
                            val port = it.toIntOrNull() ?: DEFAULT_PORT
                            wsClient.saveConfig(hostInput, port)
                        },
                        label = { Text("Porta WS", color = Color(0xFF9EACB8), fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AscherCyan,
                            unfocusedBorderColor = Color(0xFF454B59)
                        ),
                        modifier = Modifier.weight(1.1f)
                    )
                }

                // Toggles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1E26), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Modalità Simulazione (Demo)",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Testa contagiri e marce senza giochi",
                            color = Color(0xFF8B929E),
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = { enable ->
                            if (enable) {
                                wsClient.startDemoSimulation()
                            } else {
                                wsClient.stopDemoSimulation()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AscherLimeYellow,
                            checkedTrackColor = Color(0xFF4A4E0A)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1E26), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cornice Volante & LED",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Mostra la scocca con LED e bezel",
                            color = Color(0xFF8B929E),
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = showBezel,
                        onCheckedChange = { wsClient.saveBezel(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AscherCyan,
                            checkedTrackColor = Color(0xFF0F3B40)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // High-visibility action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CONNETTI E SALVA (Bright Green Accent)
                    Button(
                        onClick = {
                            val port = portInput.toIntOrNull() ?: DEFAULT_PORT
                            wsClient.saveConfig(hostInput, port)
                            if (isDemoMode) {
                                wsClient.stopDemoSimulation()
                            }
                            wsClient.connect(hostInput, port)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2EEB5E),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "CONNETTI E SALVA",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // CHIUDI (High-Contrast Solid Slate)
                    Button(
                        onClick = {
                            val port = portInput.toIntOrNull() ?: DEFAULT_PORT
                            wsClient.saveConfig(hostInput, port)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6B7280)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B303C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "CHIUDI",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

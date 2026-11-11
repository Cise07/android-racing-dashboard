package com.racing.dashboard.data.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.racing.dashboard.data.TelemetryState
import com.racing.dashboard.data.model.TelemetryData
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "WebSocketClient"
private const val PREFS_NAME = "racing_dashboard_prefs"
private const val KEY_HOST = "server_host"
private const val KEY_PORT = "server_port"
private const val KEY_SHOW_BEZEL = "show_bezel"
const val DEFAULT_PORT = 9000
const val DEFAULT_HOST = "10.0.2.2" // Works in Android emulator, can be changed to PC LAN IP

class WebSocketClient private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: WebSocketClient? = null

        fun getInstance(): WebSocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketClient().also { INSTANCE = it }
            }
        }
    }

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var prefs: SharedPreferences? = null
    var serverHost: String = DEFAULT_HOST
        private set
    var serverPort: Int = DEFAULT_PORT
        private set

    private val telemetryState = TelemetryState.getInstance()
    private val isConnecting = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private var demoJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        serverHost = prefs?.getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
        serverPort = prefs?.getInt(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT

        val savedBezel = prefs?.getBoolean(KEY_SHOW_BEZEL, true) ?: true
        telemetryState.setShowBezel(savedBezel)

        // Start auto-connect
        connect(serverHost, serverPort)
    }

    fun saveConfig(host: String, port: Int) {
        serverHost = host.trim()
        serverPort = port
        prefs?.edit()?.apply {
            putString(KEY_HOST, serverHost)
            putInt(KEY_PORT, serverPort)
            apply()
        }
    }

    fun saveBezel(show: Boolean) {
        telemetryState.setShowBezel(show)
        prefs?.edit()?.putBoolean(KEY_SHOW_BEZEL, show)?.apply()
    }

    fun connect(host: String = serverHost, port: Int = serverPort) {
        saveConfig(host, port)

        if (isConnecting.getAndSet(true)) {
            disconnectInternal(false)
        }

        telemetryState.setConnectionStatus("Connecting to ws://$serverHost:$serverPort...")

        val request = Request.Builder()
            .url("ws://$serverHost:$serverPort")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to $serverHost:$serverPort")
                isConnecting.set(false)
                telemetryState.setConnected(true, "Connected to $serverHost:$serverPort")
                cancelReconnect()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                parseMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket error: ${t.message}")
                isConnecting.set(false)
                telemetryState.setConnected(false, "Connection error: ${t.localizedMessage ?: "Failed"}")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                isConnecting.set(false)
                telemetryState.setConnected(false, "Disconnected")
            }
        })
    }

    fun disconnect() {
        cancelReconnect()
        disconnectInternal(true)
    }

    private fun disconnectInternal(notifyState: Boolean) {
        try {
            webSocket?.close(1000, "User disconnect")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing websocket", e)
        }
        webSocket = null
        if (notifyState) {
            telemetryState.setConnected(false, "Disconnected")
        }
    }

    private fun scheduleReconnect() {
        if (telemetryState.isDemoMode.value) return
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            delay(3000)
            if (!telemetryState.isConnected.value && !telemetryState.isDemoMode.value) {
                Log.d(TAG, "Attempting auto-reconnect...")
                mainHandler.post {
                    connect(serverHost, serverPort)
                }
            }
        }
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun parseMessage(message: String) {
        if (telemetryState.isDemoMode.value) return
        try {
            val data = gson.fromJson(message, TelemetryData::class.java)
            if (data != null) {
                telemetryState.updateTelemetry(data)
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse JSON telemetry: $message", e)
        }
    }

    fun startDemoSimulation() {
        demoJob?.cancel()
        telemetryState.setDemoMode(true)
        telemetryState.setConnectionStatus("Demo Mode (Simulating)")
        cancelReconnect()

        demoJob = coroutineScope.launch {
            var simTime = 0.0
            val dt = 0.033 // ~30 fps
            while (isActive && telemetryState.isDemoMode.value) {
                simTime += dt
                val cycle = simTime % 24.0

                val gear: Int
                val speed: Float
                val rpm: Int

                when {
                    cycle < 3.0 -> {
                        gear = 2
                        val p = (cycle / 3.0).toFloat()
                        speed = 70f + p * 40f
                        rpm = (4500 + p * 3300).toInt()
                    }
                    cycle < 7.0 -> {
                        gear = 3
                        val p = ((cycle - 3.0) / 4.0).toFloat()
                        speed = 110f + p * 45f
                        rpm = (5000 + p * 2800).toInt()
                    }
                    cycle < 12.0 -> {
                        gear = 4
                        val p = ((cycle - 7.0) / 5.0).toFloat()
                        speed = 155f + p * 45f
                        rpm = (5200 + p * 2600).toInt()
                    }
                    cycle < 18.0 -> {
                        gear = 5
                        val p = ((cycle - 12.0) / 6.0).toFloat()
                        speed = 200f + p * 35f
                        rpm = (5800 + p * 2000).toInt()
                    }
                    cycle < 21.0 -> {
                        gear = 6
                        val p = ((cycle - 18.0) / 3.0).toFloat()
                        speed = 235f + p * 15f
                        rpm = (6800 + p * 1000).toInt()
                    }
                    else -> {
                        val p = ((cycle - 21.0) / 3.0).toFloat()
                        gear = maxOf(2, (6 - p * 4).toInt())
                        speed = 250f - p * 180f
                        rpm = (7200 - p * 2800).toInt()
                    }
                }

                val lapTime = (simTime % 75.0).toFloat()
                val delta = if (gear == 5) 0.21f else -0.14f

                val simulatedData = TelemetryData(
                    speedKmh = speed,
                    rpm = rpm.coerceIn(1000, 8000),
                    maxRpm = 8000,
                    gear = gear,
                    boostBar = if (gear >= 2) 1.25f else 0.2f,
                    fuelLiters = (48.5f - (simTime * 0.02f).toFloat()).coerceAtLeast(5f),
                    fuelKg = (48.5f - (simTime * 0.02f).toFloat()).coerceAtLeast(5f),
                    waterTempC = 88.5f,
                    oilTempC = 96.2f,
                    throttlePct = if (cycle >= 21.0) 0 else 100,
                    brakePct = if (cycle >= 21.0) 75 else 0,
                    clutchPct = if (cycle >= 21.0) 40 else 0,
                    lapTimeSeconds = lapTime,
                    bestLapTimeSeconds = 62.663f,
                    deltaSeconds = delta,
                    fuelPerLap = 0.97f,
                    stintLaps = 33.61f,
                    brakeBias = 53.40f,
                    asphaltTempC = 42.8f,
                    ambientTempC = 25.6f,
                    currentLap = 3 + (simTime / 75.0).toInt(),
                    position = 1,
                    vehicleName = "Demo Simulation",
                    gameSource = "DEMO",
                    isEngineOn = true
                )

                telemetryState.updateTelemetry(simulatedData)
                delay(33)
            }
        }
    }

    fun stopDemoSimulation() {
        demoJob?.cancel()
        demoJob = null
        telemetryState.setDemoMode(false)
        connect(serverHost, serverPort)
    }
}

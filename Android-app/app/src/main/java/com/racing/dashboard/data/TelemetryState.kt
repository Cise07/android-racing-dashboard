package com.racing.dashboard.data

import com.racing.dashboard.data.model.TelemetryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TelemetryState private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: TelemetryState? = null

        fun getInstance(): TelemetryState {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelemetryState().also { INSTANCE = it }
            }
        }
    }

    private val _telemetryData = MutableStateFlow(TelemetryData())
    val telemetryData: StateFlow<TelemetryData> = _telemetryData.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showBezel = MutableStateFlow(true)
    val showBezel: StateFlow<Boolean> = _showBezel.asStateFlow()

    fun updateTelemetry(data: TelemetryData) {
        _telemetryData.value = data
    }

    fun setConnected(connected: Boolean, status: String = if (connected) "Connected" else "Disconnected") {
        _isConnected.value = connected
        _connectionStatus.value = status
    }

    fun setConnectionStatus(status: String) {
        _connectionStatus.value = status
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun toggleDemoMode() {
        _isDemoMode.value = !_isDemoMode.value
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowBezel(show: Boolean) {
        _showBezel.value = show
    }

    fun toggleBezel() {
        _showBezel.value = !_showBezel.value
    }
}

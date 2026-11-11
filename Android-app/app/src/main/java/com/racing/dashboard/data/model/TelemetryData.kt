package com.racing.dashboard.data.model

data class TelemetryData(
    val speedKmh: Float = 0f,
    val rpm: Int = 0,
    val maxRpm: Int = 7500,
    val gear: Int = 0,
    val boostBar: Float = 0f,           // Turbo / Boost pressure in bar
    val fuelLiters: Float? = null,      // Remaining Fuel in Liters (null if N/D)
    val throttlePct: Int = 0,           // Throttle % (0 to 100)
    val brakePct: Int = 0,              // Brake % (0 to 100)
    val clutchPct: Int = 0,             // Clutch % (0 to 100)
    val waterTempC: Float? = null,      // Engine Coolant / Water temp in °C (null if N/D)
    val oilTempC: Float? = null,        // Engine Oil temp in °C (null if N/D)
    val lapTimeSeconds: Float = 0f,     // Lap time (0 if outside race)
    val bestLapTimeSeconds: Float = 0f, // Best lap time (0 if outside race)
    val deltaSeconds: Float = 0f,       // Delta time (0 if outside race)
    val currentLap: Int = 0,            // Current lap (0 if outside race)
    val lapsTotal: Int = 0,
    val position: Int = 0,              // Position (0 if outside race)
    val vehicleName: String = "",
    val gameSource: String = "",        // "Forza Horizon", "BeamNG.drive", "ETS2", "DEMO"
    val isEngineOn: Boolean = false,
    val isInPit: Boolean = false,
    val pitLimiterOn: Boolean = false,
    val absActive: Boolean = false,
    val tcActive: Boolean = false,

    // Legacy fields for backward compatibility
    val fuelKg: Float? = null,
    val fuelPct: Float? = null,
    val brakeBias: Float = 0f,
    val fuelPerLap: Float = 0f,
    val stintLaps: Float = 0f,
    val asphaltTempC: Float = 0f,
    val ambientTempC: Float = 0f,
    val leftEncoder: String = "",
    val rightEncoder: String = ""
)

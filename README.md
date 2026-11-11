# Sim Racing Dashboard - Android Telemetry App

A high-fidelity native Android telemetry dashboard connected in real-time to Windows PC sim racing games (Forza Horizon 4/5, BeamNG.drive, ETS2).

## Visual Design & Layout

- **Curved Top Shift Lights**: 15 circular LEDs that illuminate progressively with engine RPM:
  - 5 Green LEDs (low-to-mid range, 70%–78%)
  - 5 Red LEDs (mid-to-high range, 80%–88%)
  - 5 Sky Blue LEDs (high range, 90%–96%)
  - **Shift Alert**: Synchronized high-frequency strobe of all 15 LEDs in electric cyan at optimal shift point (>= 97% max RPM)
- **Top Status Bar**: Live Session Time (`00:00:00`), Current Lap (`L 3`), Position (`P 1`), and real-time clock (`15:13:53`)
- **Left Column**:
  - `BOOST (bar)`: Turbo/Supercharger boost pressure
  - `FUEL (L)`: Remaining fuel volume in Liters
  - `THROTTLE / BRAKE (%)`: Real-time pedal input bars and percentages
  - `CLUTCH (%)`: Clutch pedal input percentage
- **Center Focal Column**:
  - `SPEED`: High-visibility digital readout in km/h
  - `GEAR`: Giant central gear numeral (`N`, `R`, `1`, `2`...) in neon lime/yellow
  - `RPM`: Engine revolutions per minute readout
- **Right Column**:
  - `LAP TIME`: Current active lap time (`00:00.000`)
  - `BEST`: Best lap time in vibrant neon green (`00:00.000`)
  - `DELTA (Secs)`: Live delta comparison in seconds
  - `WATER TEMP (°C)`: Engine coolant temperature
- **Bottom Bezel**: Clean carbon racing brandplate (`RACING DASHBOARD`)

## Project Structure

```
racing-dashboard/
├── Android-app/                       # Native Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/racing/dashboard/
│   │   │   ├── MainActivity.kt        # Landscape, immersive fullscreen, keep screen awake
│   │   │   ├── data/
│   │   │   │   ├── TelemetryState.kt  # StateFlow reactive state holder
│   │   │   │   ├── model/
│   │   │   │   │   └── TelemetryData.kt # Unified telemetry schema
│   │   │   │   └── network/
│   │   │   │       └── WebSocketClient.kt # Reconnecting WebSocket client + offline demo
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   ├── ShiftLightsBar.kt      # Arched 15-LED shift bar & side LEDs
│   │   │       │   ├── DashboardGrid.kt       # 3-column symmetrical 4x4 layout
│   │   │       │   ├── DashboardBrandplate.kt # Racing dashboard brandplate
│   │   │       │   └── SettingsDialog.kt      # Host/port settings, persistent preferences
│   │   │       ├── dashboard/
│   │   │       │   └── DashboardScreen.kt     # Root screen with bezel/fullscreen modes
│   │   │       └── theme/                     # Racing color palette and typography
│   │   └── build.gradle.kts
│   ├── gradlew.bat
│   └── build.gradle.kts
│
├── pc-scripts/
│   ├── telemetry_bridge.py            # Async WebSocket bridge (Forza UDP + BeamNG OutGauge + ETS2)
│   └── requirements.txt
├── start_bridge.bat                   # Quick launcher on PC
└── README.md
```

## Setup & Execution

### 1. PC Telemetry Bridge
Install dependencies:
```bash
pip install -r pc-scripts/requirements.txt
```
Launch the bridge:
```bash
start_bridge.bat
```

### 2. Android Dashboard
1. Install the APK on your Android device (`Android-app/app/build/outputs/apk/debug/app-debug.apk`).
2. Open the app, tap the top-right Settings icon, and enter your PC's local IP address.
3. Tap **Connetti**.

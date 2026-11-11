# 🏎️ Sim Racing Dashboard — Android Telemetry App

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows-2EEB5E?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square)
![Python](https://img.shields.io/badge/Python-3.8%2B-3776AB?style=flat-square)
![Protocol](https://img.shields.io/badge/Telemetry-UDP%20%E2%86%92%20WebSocket%2030Hz-00E5FF?style=flat-square)

A high-fidelity, real-time **GT3-style Sim Racing Telemetry Dashboard** for Android devices, powered by a lightweight Python WebSocket bridge for Windows PC racing simulators (**Forza Horizon 4/5**, **Forza Motorsport**, **BeamNG.drive**, and **Euro Truck Simulator 2**).

---

## ✨ Key Features & Visual Layout

- 🚨 **15-LED Arched Shift Lights Bar**:
  - **5 Green LEDs** (`70%–78%` max RPM)
  - **5 Red LEDs** (`80%–88%` max RPM)
  - **5 Sky Blue LEDs** (`90%–96%` max RPM)
  - ⚡ **Shift Alert Strobe**: Synchronized high-frequency electric cyan strobe across all 15 LEDs at optimal shift point (`>= 97%` max RPM)
- ⏱️ **Top Status Bar**: Live Session Time (`00:00:00`), Current Lap (`L 3`), Race Position (`P 1`), and real-time clock.
- 📊 **Left Telemetry Column**:
  - `BOOST (bar)` — Turbo / Supercharger pressure
  - `FUEL (L)` — Remaining fuel in liters (`N/D` when not exposed by the sim)
  - `THROTTLE / BRAKE (%)` — Real-time progressive pedal input bars and percentages
  - `CLUTCH (%)` — Clutch pedal input percentage
- 🎯 **Center Focal Display**:
  - `SPEED` — High-visibility digital readout in `km/h`
  - `GEAR` — Giant central gear indicator (`R`, `N`, `1`–`10`) in neon racing yellow
  - `RPM` — Live engine revolutions per minute
- 🏁 **Right Telemetry Column**:
  - `LAP TIME` — Current active lap time (`00:00.000`)
  - `BEST` — Best session lap time in neon green (`00:00.000`)
  - `DELTA (Secs)` — Live delta comparison against best lap
  - `WATER TEMP (°C)` — Engine coolant temperature
- 🖥️ **Dual Display Modes & Demo Simulation**:
  - Switch between **Steering Wheel Bezel Mode** (with side warning LEDs and carbon brandplate) and **Edge-to-Edge Fullscreen Mode**.
  - Built-in **Demo Mode** to preview and test the dashboard without a PC connection.

---

## ⚙️ How It Works

```
[ PC Racing Game ]  ──(UDP / TCP Telemetry)──►  [ Python Bridge (PC) ]  ──(WebSocket 30Hz Wi-Fi)──►  [ Android Dashboard ]
```

1. **Game Telemetry Output**: The racing simulator sends raw binary UDP/TCP telemetry packets locally on the PC (`127.0.0.1`).
2. **PC Telemetry Bridge (`telemetry_bridge.py`)**: Captures and parses game-specific binary packets (Forza Sled/Dash structs, BeamNG OutGauge 96-byte structs, or ETS2 telemetry), normalizes them into a unified JSON schema, and broadcasts them at **30 Hz** over a WebSocket server (default port `9000`).
3. **Android App**: Connects to the PC's local Wi-Fi IP address via WebSocket, keeps the screen awake in immersive landscape mode, and renders telemetry at 60 FPS using Jetpack Compose.

---

## 🚀 Setup & Usage Guide

### 1️⃣ PC Setup (Telemetry Bridge)

1. Make sure **Python 3.8+** is installed on your Windows PC.
2. Install the required dependency:
   ```bash
   pip install -r pc-scripts/requirements.txt
   ```
3. Start the telemetry bridge by double-clicking **`start_bridge.bat`** (or running `python pc-scripts/telemetry_bridge.py`).
4. Note your PC's **Local IPv4 Address** displayed in the console window (e.g. `192.168.1.50`).

> 💡 **Tip**: Run `python pc-scripts/telemetry_bridge.py --demo` on your PC to stream simulated GT3 telemetry over Wi-Fi without launching a game.

---

### 2️⃣ Android App Setup

1. **Install the App**:
   - **Option A (Pre-built APK)**: Download the latest `.apk` from the [**GitHub Releases**](../../releases) section and install it on your Android phone/tablet.
   - **Option B (Build from Source)**: Open the `Android-app/` folder in **Android Studio** and run the `app` configuration on your device, or build via CLI:
     ```bash
     cd Android-app
     ./gradlew assembleDebug
     ```
2. **Connect to PC**:
   - Ensure your Android device and PC are connected to the **same local network (Wi-Fi / LAN)**.
   - Open the app and tap the **⚙️ Settings icon** in the top-right corner.
   - Enter your PC's **IPv4 address** and port **`9000`**.
   - Tap **CONNETTI E SALVA**.

---

### 3️⃣ In-Game Telemetry Configuration

#### 🎮 Forza Horizon 4 / 5 & Forza Motorsport
1. Open the game and go to **Settings** → **HUD and Gameplay**.
2. Scroll down to **Data Out** and set it to **ON**.
3. Set **Data Out IP Address** to `127.0.0.1`.
4. Set **Data Out IP Port** to `5300`.
5. Set **Data Out Packet Format** to **Car Dash** (324-byte packet).

#### 🚗 BeamNG.drive
1. Open BeamNG.drive and go to **Options** → **Other**.
2. Scroll to **OutGauge Support** and check **Enable**.
3. Set **IP** to `127.0.0.1`.
4. Set **Port** to `4444` (the bridge automatically listens on `5100`, `4444`, and `4567`).

#### 🚛 Euro Truck Simulator 2
- The bridge automatically polls local ETS2 telemetry on `127.0.0.1:27000` when a compatible telemetry plugin/server is active.

---

## 📁 Project Structure

```text
android-racing-dashboard/
├── Android-app/                           # Native Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/racing/dashboard/
│   │   │   ├── MainActivity.kt            # Immersive landscape fullscreen & wake-lock
│   │   │   ├── data/
│   │   │   │   ├── TelemetryState.kt      # Reactive StateFlow telemetry holder
│   │   │   │   ├── model/
│   │   │   │   │   └── TelemetryData.kt   # Unified telemetry data model
│   │   │   │   └── network/
│   │   │   │       └── WebSocketClient.kt # Auto-reconnecting WebSocket client + offline demo
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   ├── ShiftLightsBar.kt      # 15-LED arched RPM bar & side alert LEDs
│   │   │       │   ├── DashboardGrid.kt       # 3-column GT3 telemetry grid
│   │   │       │   ├── DashboardBrandplate.kt # Carbon racing brandplate
│   │   │       │   └── SettingsDialog.kt      # IP/Port config, Demo & Bezel toggles
│   │   │       ├── dashboard/
│   │   │       │   └── DashboardScreen.kt     # Root screen container
│   │   │       └── theme/                     # Racing color palette & typography
│   │   └── build.gradle.kts
│   ├── gradlew / gradlew.bat
│   └── build.gradle.kts
│
├── pc-scripts/
│   ├── telemetry_bridge.py                # Async 30Hz WebSocket bridge (Forza / BeamNG / ETS2)
│   └── requirements.txt                   # Python dependencies (websockets)
├── start_bridge.bat                       # One-click Windows launcher with IP detection
└── README.md
```

---

## 🛠️ Troubleshooting

- **Android app stays on "Disconnesso" / "Connessione..."**:
  - Verify that both PC and smartphone are on the same Wi-Fi network (avoid Guest Wi-Fi with client isolation).
  - Allow **Python** (or TCP port `9000`) through **Windows Defender Firewall** on Private Networks.
- **Connected to Bridge, but values stay at `0`**:
  - Verify that **Data Out** (Forza) or **OutGauge** (BeamNG) is enabled in-game with IP `127.0.0.1` and the matching UDP port (`5300` for Forza, `4444`/`5100` for BeamNG).
  - For Forza on the same PC, if UWP loopback restrictions block `127.0.0.1`, enter your PC's LAN IPv4 address (e.g. `192.168.1.x`) directly in Forza's **Data Out IP Address** setting.

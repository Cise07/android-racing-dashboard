#!/usr/bin/env python3
"""
PC Telemetry Bridge for Racing Games
Supports: Forza Horizon 4/5/6 & Motorsport, BeamNG.drive (OutGauge), Euro Truck Simulator 2
Broadcasts unified JSON telemetry via WebSocket to Android app
"""

import asyncio
import json
import struct
import socket
import threading
import logging
import time
import sys
import math
from dataclasses import dataclass, asdict
from typing import Optional, List, Tuple
import websockets
from websockets.asyncio.server import serve

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("TelemetryBridge")

DEFAULT_WS_PORT = 9000
FORZA_PORT = 5300
BEAMNG_PORTS = [5100, 4444, 4567]


@dataclass
class TelemetryData:
    speedKmh: float = 0.0
    rpm: int = 0
    maxRpm: int = 7500
    gear: int = 0
    boostBar: float = 0.0
    fuelLiters: Optional[float] = None
    throttlePct: int = 0
    brakePct: int = 0
    clutchPct: int = 0
    waterTempC: Optional[float] = None
    oilTempC: Optional[float] = None
    lapTimeSeconds: float = 0.0
    bestLapTimeSeconds: float = 0.0
    deltaSeconds: float = 0.0
    currentLap: int = 0
    position: int = 0
    vehicleName: str = ""
    gameSource: str = ""
    isEngineOn: bool = False
    isInPit: bool = False
    pitLimiterOn: bool = False
    absActive: bool = False
    tcActive: bool = False

    # Legacy fields kept for backward compatibility
    fuelKg: Optional[float] = None
    fuelPct: Optional[float] = None
    brakeBias: float = 0.0
    fuelPerLap: float = 0.0
    stintLaps: float = 0.0
    asphaltTempC: float = 0.0
    ambientTempC: float = 0.0
    lapsTotal: int = 0
    leftEncoder: str = ""
    rightEncoder: str = ""
    tireTemps: dict = None

    def __post_init__(self):
        if self.tireTemps is None:
            self.tireTemps = {
                "frontLeft": 0.0,
                "frontRight": 0.0,
                "rearLeft": 0.0,
                "rearRight": 0.0
            }

    def to_json(self) -> str:
        return json.dumps(asdict(self), separators=(',', ':'))


class ForzaTelemetryParser:
    """Parse Forza Horizon 4/5/6 & Motorsport telemetry packets with exact byte offsets"""

    def parse(self, data: bytes) -> Optional[TelemetryData]:
        try:
            length = len(data)
            if length < 232:
                return None

            is_race_on = struct.unpack_from('<i', data, 0)[0]
            engine_max_rpm = struct.unpack_from('<f', data, 8)[0]
            engine_idle_rpm = struct.unpack_from('<f', data, 12)[0]
            current_engine_rpm = struct.unpack_from('<f', data, 16)[0]

            # Velocity vector (m/s) -> calculate km/h
            vx, vy, vz = struct.unpack_from('<fff', data, 32)
            speed_ms = (vx**2 + vy**2 + vz**2) ** 0.5
            speed_kmh = round(speed_ms * 3.6, 1)

            # Tire temps (FL, FR, RL, RR) in Fahrenheit -> convert to Celsius (pure tire telemetry only)
            t_fl, t_fr, t_rl, t_rr = struct.unpack_from('<ffff', data, 164)
            tire_temps = {
                "frontLeft": round((t_fl - 32.0) * 5.0 / 9.0, 1),
                "frontRight": round((t_fr - 32.0) * 5.0 / 9.0, 1),
                "rearLeft": round((t_rl - 32.0) * 5.0 / 9.0, 1),
                "rearRight": round((t_rr - 32.0) * 5.0 / 9.0, 1)
            }

            # Water / Oil temps & Fuel: NOT reliably available or relevant in Forza -> strictly None (shown as N/D)
            water_temp = None
            oil_temp = None
            fuel_liters = None

            boost_bar = 0.0
            best_lap = 0.0
            curr_lap_time = 0.0
            lap_num = 0
            race_pos = 0
            accel_pct = 0
            brake_pct = 0
            clutch_pct = 0
            gear = 0

            if length >= 323:
                # 324-byte Horizon packet
                raw_boost = struct.unpack_from('<f', data, 284)[0]
                boost_bar = max(0.0, round(raw_boost * 0.0689476, 2)) # PSI to Bar

                raw_best = struct.unpack_from('<f', data, 296)[0]
                best_lap = round(raw_best, 3) if raw_best > 0.1 else 0.0

                raw_curr = struct.unpack_from('<f', data, 304)[0]
                curr_lap_time = round(raw_curr, 3) if raw_curr > 0.1 else 0.0

                raw_lap = struct.unpack_from('<H', data, 312)[0]
                lap_num = int(raw_lap) + 1 if is_race_on != 0 and raw_lap < 200 else 0

                raw_pos = struct.unpack_from('<B', data, 314)[0]
                race_pos = int(raw_pos) if is_race_on != 0 and raw_pos <= 24 else 0

                # Check Horizon offset 315..317 and Motorsport offset 303..305
                a_315 = data[315] if length > 315 else 0
                b_316 = data[316] if length > 316 else 0
                c_317 = data[317] if length > 317 else 0

                a_303 = data[303] if length > 303 else 0
                b_304 = data[304] if length > 304 else 0
                c_305 = data[305] if length > 305 else 0

                if a_315 > 0 or b_316 > 0 or c_317 > 0:
                    accel_val, brake_val, clutch_val = a_315, b_316, c_317
                elif a_303 > 0 or b_304 > 0 or c_305 > 0:
                    accel_val, brake_val, clutch_val = a_303, b_304, c_305
                else:
                    accel_val, brake_val, clutch_val = a_315, b_316, c_317

                accel_pct = max(0, min(100, int(round(accel_val / 2.55))))
                brake_pct = max(0, min(100, int(round(brake_val / 2.55))))
                clutch_pct = max(0, min(100, int(round(clutch_val / 2.55))))

                raw_gear = struct.unpack_from('<B', data, 319)[0]
                if raw_gear == 0:
                    gear = -1  # Reverse
                elif raw_gear == 11:
                    gear = 0   # Neutral
                else:
                    gear = raw_gear

            elif length >= 311:
                # FM7 packet
                raw_best = struct.unpack_from('<f', data, 284)[0]
                best_lap = round(raw_best, 3) if raw_best > 0.1 else 0.0
                raw_curr = struct.unpack_from('<f', data, 292)[0]
                curr_lap_time = round(raw_curr, 3) if raw_curr > 0.1 else 0.0
                lap_num = int(struct.unpack_from('<H', data, 300)[0]) + 1
                race_pos = int(struct.unpack_from('<B', data, 302)[0])
                accel_pct = max(0, min(100, int(round(data[303] / 2.55))))
                brake_pct = max(0, min(100, int(round(data[304] / 2.55))))
                clutch_pct = max(0, min(100, int(round(data[305] / 2.55))))
                raw_gear = struct.unpack_from('<B', data, 307)[0]
                gear = -1 if raw_gear == 0 else (0 if raw_gear == 11 else raw_gear)

            delta = round(curr_lap_time - best_lap, 2) if (curr_lap_time > 0 and best_lap > 0) else 0.0
            is_shift_point = bool(current_engine_rpm >= (engine_max_rpm * 0.97)) if engine_max_rpm > 1000 else False

            return TelemetryData(
                speedKmh=speed_kmh,
                rpm=max(0, int(current_engine_rpm)),
                maxRpm=int(engine_max_rpm) if engine_max_rpm > 1000 else 8000,
                gear=gear,
                boostBar=boost_bar,
                fuelLiters=None,
                fuelKg=None,
                fuelPct=None,
                throttlePct=accel_pct,
                brakePct=brake_pct,
                clutchPct=clutch_pct,
                waterTempC=None,
                oilTempC=None,
                lapTimeSeconds=curr_lap_time,
                bestLapTimeSeconds=best_lap,
                deltaSeconds=delta,
                currentLap=lap_num,
                position=race_pos,
                vehicleName="Forza Horizon",
                gameSource="Forza Horizon",
                isEngineOn=current_engine_rpm > 100,
                isInPit=False,
                pitLimiterOn=is_shift_point,
                tireTemps=tire_temps
            )
        except Exception as e:
            logger.debug(f"Forza parse error: {e}")
            return None


class BeamNGTelemetryParser:
    """Parse BeamNG.drive OutGauge telemetry (LFS standard 96-byte struct)"""

    def __init__(self):
        self.max_observed_rpm = 7000

    def parse(self, data: bytes) -> Optional[TelemetryData]:
        try:
            if len(data) < 40:
                return None

            # OutGauge layout:
            # 0:4   time (u32)
            # 4:8   car (char[4])
            # 8:10  flags (u16)
            # 10    gear (B) 0=Reverse, 1=Neutral, 2=1st, 3=2nd...
            # 11    plid (B)
            # 12:16 speed (f32, m/s)
            # 16:20 rpm (f32)
            # 20:24 turbo (f32, bar)
            # 24:28 engTemp (f32, °C)
            # 28:32 fuel (f32, 0.0-1.0 or liters)
            # 32:36 oilPressure (f32, bar)
            # 36:40 oilTemp (f32, °C)
            # 40:44 dashLights (u32)
            # 44:48 showLights (u32)
            # 48:52 throttle (f32, 0.0 to 1.0)
            # 52:56 brake (f32, 0.0 to 1.0)
            # 56:60 clutch (f32, 0.0 to 1.0)
            raw_gear = struct.unpack_from('<B', data, 10)[0]
            if raw_gear == 0:
                gear = -1  # Reverse
            elif raw_gear == 1:
                gear = 0   # Neutral
            else:
                gear = raw_gear - 1

            speed_ms = struct.unpack_from('<f', data, 12)[0]
            rpm = int(struct.unpack_from('<f', data, 16)[0])
            turbo_bar = struct.unpack_from('<f', data, 20)[0]
            water_temp = struct.unpack_from('<f', data, 24)[0]
            fuel_raw = struct.unpack_from('<f', data, 28)[0]
            oil_temp = struct.unpack_from('<f', data, 36)[0] if len(data) >= 40 else 0.0

            # Convert fuel to Liters (BeamNG sends 0.0 - 1.0 ratio; assume ~55L tank)
            if fuel_raw > 1.0:
                fuel_liters = round(fuel_raw, 1)
            elif fuel_raw > 0.0:
                fuel_liters = round(fuel_raw * 55.0, 1)
            else:
                fuel_liters = 0.0

            # Decode dashLights / showLights if available
            show_lights = struct.unpack_from('<I', data, 44)[0] if len(data) >= 48 else 0
            abs_active = bool(show_lights & (1 << 10))
            tc_active = bool(show_lights & (1 << 4))
            pit_limiter = bool(show_lights & (1 << 0))

            # Pedal inputs (throttle, brake, clutch)
            throttle_raw = struct.unpack_from('<f', data, 48)[0] if len(data) >= 52 else 0.0
            brake_raw = struct.unpack_from('<f', data, 52)[0] if len(data) >= 56 else 0.0
            clutch_raw = struct.unpack_from('<f', data, 56)[0] if len(data) >= 60 else 0.0

            if math.isnan(throttle_raw) or math.isinf(throttle_raw):
                throttle_raw = 0.0
            if math.isnan(brake_raw) or math.isinf(brake_raw):
                brake_raw = 0.0
            if math.isnan(clutch_raw) or math.isinf(clutch_raw):
                clutch_raw = 0.0

            thr_calc = throttle_raw if throttle_raw > 1.0 else throttle_raw * 100.0
            brk_calc = brake_raw if brake_raw > 1.0 else brake_raw * 100.0
            clt_calc = clutch_raw if clutch_raw > 1.0 else clutch_raw * 100.0

            throttle_pct = max(0, min(100, int(round(thr_calc))))
            brake_pct = max(0, min(100, int(round(brk_calc))))
            clutch_pct = max(0, min(100, int(round(clt_calc))))

            # Exact vehicle redline RPM from BeamNG outgauge.lua o.id
            raw_id = struct.unpack_from('<i', data, 92)[0] if len(data) >= 96 else 0
            if raw_id > 1500:
                max_rpm = raw_id
            else:
                if (show_lights & 1) and rpm > 1500:
                    self.max_observed_rpm = max(self.max_observed_rpm, int(rpm / 0.95))
                elif rpm > self.max_observed_rpm:
                    self.max_observed_rpm = max(rpm, 6500)
                max_rpm = max(6500, self.max_observed_rpm)

            return TelemetryData(
                speedKmh=round(max(0.0, speed_ms * 3.6), 1),
                rpm=max(0, rpm),
                maxRpm=max_rpm,
                gear=gear,
                boostBar=round(max(0.0, turbo_bar), 2),
                fuelLiters=fuel_liters,
                fuelKg=fuel_liters,
                fuelPct=round(min(100.0, fuel_raw * 100.0), 1) if fuel_raw <= 1.0 else 100.0,
                throttlePct=throttle_pct,
                brakePct=brake_pct,
                clutchPct=clutch_pct,
                waterTempC=round(max(0.0, water_temp), 1),
                oilTempC=round(max(0.0, oil_temp), 1),
                lapTimeSeconds=0.0,
                bestLapTimeSeconds=0.0,
                deltaSeconds=0.0,
                currentLap=0,
                position=0,
                vehicleName="BeamNG.drive",
                gameSource="BeamNG.drive",
                isEngineOn=rpm > 150,
                isInPit=False,
                pitLimiterOn=pit_limiter,
                absActive=abs_active,
                tcActive=tc_active
            )
        except Exception as e:
            logger.error(f"BeamNG parse error: {e}")
            return None


class ETS2TelemetryClient:
    """Handle ETS2 local telemetry via localhost TCP connection"""

    def __init__(self, host: str = "127.0.0.1", port: int = 27000):
        self.host = host
        self.port = port
        self.socket: Optional[socket.socket] = None
        self.lock = threading.Lock()

    def connect(self) -> bool:
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(0.5)
            sock.connect((self.host, self.port))
            sock.setblocking(False)
            with self.lock:
                self.socket = sock
            logger.info(f"ETS2 connected on {self.host}:{self.port}")
            return True
        except Exception as e:
            logger.debug(f"ETS2 connection not available: {e}")
            return False

    def poll(self) -> Optional[TelemetryData]:
        with self.lock:
            if not self.socket:
                return None
            try:
                data = self.socket.recv(2048)
                if data and len(data) >= 32:
                    speed_mph = struct.unpack('<f', data[0:4])[0]
                    rpm = struct.unpack('<I', data[12:16])[0]
                    gear = int(struct.unpack('<i', data[16:20])[0])
                    fuel = struct.unpack('<f', data[24:28])[0]
                    return TelemetryData(
                        speedKmh=round(speed_mph * 1.60934, 1),
                        rpm=rpm,
                        maxRpm=2500,
                        gear=gear,
                        fuelLiters=round(fuel, 1),
                        fuelKg=round(fuel, 1),
                        vehicleName="Euro Truck Simulator 2",
                        gameSource="ETS2",
                        isEngineOn=rpm > 400
                    )
            except (BlockingIOError, socket.error):
                pass
            except Exception as e:
                logger.debug(f"ETS2 poll error: {e}")
        return None

    def close(self):
        with self.lock:
            if self.socket:
                try:
                    self.socket.close()
                except Exception:
                    pass
                self.socket = None


class TelemetryBridge:
    """Main bridge class coordinating all telemetry sources and WebSocket clients"""

    def __init__(self, ws_port: int = DEFAULT_WS_PORT, force_demo: bool = False):
        self.ws_port = ws_port
        self.force_demo = force_demo
        self.clients: set = set()
        self.current_data = TelemetryData()
        self.forza_parser = ForzaTelemetryParser()
        self.beamng_parser = BeamNGTelemetryParser()
        self.ets2_client = ETS2TelemetryClient()
        self.forza_socket: Optional[socket.socket] = None
        self.beamng_sockets: List[Tuple[int, socket.socket]] = []
        self.running = False
        self.last_real_packet_time = 0.0
        self._sim_time = 0.0
        self._last_logged_gear = None
        self._game_connected_logged = False
        self._setup_sockets()

    def _setup_sockets(self):
        # Forza UDP socket (port 5300)
        try:
            self.forza_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.forza_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.forza_socket.bind(('', FORZA_PORT))
            self.forza_socket.setblocking(False)
            logger.info(f"Forza UDP socket bound to port {FORZA_PORT}")
        except Exception as e:
            logger.warning(f"Could not bind Forza socket on port {FORZA_PORT}: {e}")

        # BeamNG UDP sockets on standard ports (5100, 4444, 4567)
        for port in BEAMNG_PORTS:
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                sock.bind(('', port))
                sock.setblocking(False)
                self.beamng_sockets.append((port, sock))
                logger.info(f"BeamNG OutGauge UDP socket bound to port {port}")
            except Exception as e:
                logger.warning(f"Could not bind BeamNG socket on port {port}: {e}")

    async def register_client(self, websocket):
        self.clients.add(websocket)
        logger.info(f"Client connected: {websocket.remote_address}")
        try:
            await websocket.send(self.current_data.to_json())
            await websocket.wait_closed()
        except Exception as e:
            logger.debug(f"Client connection closed: {e}")
        finally:
            self.clients.discard(websocket)
            logger.info(f"Client disconnected: {websocket.remote_address}")

    async def broadcast_telemetry(self):
        """Broadcast telemetry data to all connected WebSocket clients"""
        if not self.clients:
            return

        message = self.current_data.to_json()
        dead_clients = set()

        for client in list(self.clients):
            try:
                await client.send(message)
            except Exception as e:
                logger.debug(f"Failed to send to client: {e}")
                dead_clients.add(client)

        for client in dead_clients:
            self.clients.discard(client)

    def receive_forza_packet(self):
        """Non-blocking receive and parse Forza telemetry"""
        if not self.forza_socket:
            return
        try:
            while True:
                data, _ = self.forza_socket.recvfrom(4096)
                parsed = self.forza_parser.parse(data)
                if parsed:
                    self.current_data = parsed
                    self.last_real_packet_time = time.time()
                    if not self._game_connected_logged:
                        self._game_connected_logged = True
                        logger.info(">>> Telemetria Forza Horizon RICEVUTA CON SUCCESSO! <<<")
                    if parsed.gear != self._last_logged_gear:
                        self._last_logged_gear = parsed.gear
                        g_str = "R" if parsed.gear == -1 else ("N" if parsed.gear == 0 else str(parsed.gear))
                        fuel_disp = f"{parsed.fuelLiters:.1f}L" if parsed.fuelLiters is not None else "N/D"
                        logger.info(f"[FH Live] Marcia: {g_str} | Vel: {parsed.speedKmh:.0f} km/h | Giri: {parsed.rpm} | Thr: {parsed.throttlePct}% | Brk: {parsed.brakePct}% | Fuel: {fuel_disp} | H2O: N/D")
        except (BlockingIOError, socket.error):
            pass
        except Exception as e:
            logger.debug(f"Forza receive error: {e}")

    def receive_beamng_packet(self):
        """Non-blocking receive and parse BeamNG OutGauge telemetry across all bound ports"""
        for port, sock in self.beamng_sockets:
            try:
                while True:
                    data, _ = sock.recvfrom(4096)
                    parsed = self.beamng_parser.parse(data)
                    if parsed:
                        self.current_data = parsed
                        self.last_real_packet_time = time.time()
                        if not self._game_connected_logged:
                            self._game_connected_logged = True
                            logger.info(f">>> Telemetria BeamNG.drive RICEVUTA CON SUCCESSO sulla porta {port}! <<<")
                        if parsed.gear != self._last_logged_gear:
                            self._last_logged_gear = parsed.gear
                            g_str = "R" if parsed.gear == -1 else ("N" if parsed.gear == 0 else str(parsed.gear))
                            fuel_disp = f"{parsed.fuelLiters:.1f}L" if parsed.fuelLiters is not None else "N/D"
                        water_disp = f"{parsed.waterTempC:.1f}°C" if parsed.waterTempC is not None else "N/D"
                        logger.info(f"[BeamNG Live :{port}] Marcia: {g_str} | Vel: {parsed.speedKmh:.0f} km/h | Giri: {parsed.rpm} | Thr: {parsed.throttlePct}% | Brk: {parsed.brakePct}% | Clt: {parsed.clutchPct}% | Fuel: {fuel_disp} | H2O: {water_disp}")
            except (BlockingIOError, socket.error):
                pass
            except Exception as e:
                logger.debug(f"BeamNG receive error on port {port}: {e}")

    def update_simulation_tick(self, dt: float):
        """
        Generate realistic GT3 telemetry matching the dashboard.
        Used ONLY when --demo flag is active.
        """
        self._sim_time += dt
        t = self._sim_time

        cycle = t % 24.0

        if cycle < 3.0:
            gear = 2
            p = cycle / 3.0
            speed = 70.0 + p * 40.0
            rpm = int(4500 + p * 3300)
            boost = 0.95
            throttle = 100
            brake = 0
            clutch = 0
        elif cycle < 7.0:
            gear = 3
            p = (cycle - 3.0) / 4.0
            speed = 110.0 + p * 45.0
            rpm = int(5000 + p * 2800)
            boost = 1.15
            throttle = 100
            brake = 0
            clutch = 0
        elif cycle < 12.0:
            gear = 4
            p = (cycle - 7.0) / 5.0
            speed = 155.0 + p * 45.0
            rpm = int(5200 + p * 2600)
            boost = 1.25
            throttle = 100
            brake = 0
            clutch = 0
        elif cycle < 18.0:
            gear = 5
            p = (cycle - 12.0) / 6.0
            speed = 200.0 + p * 35.0
            rpm = int(5800 + p * 2000)
            boost = 1.28
            throttle = 100
            brake = 0
            clutch = 0
        elif cycle < 21.0:
            gear = 6
            p = (cycle - 18.0) / 3.0
            speed = 235.0 + p * 15.0
            rpm = int(6800 + p * 1000)
            boost = 1.30
            throttle = 100
            brake = 0
            clutch = 0
        else:
            # Braking zone
            p = (cycle - 21.0) / 3.0
            gear = max(2, int(6 - p * 4))
            speed = 250.0 - p * 180.0
            rpm = int(7200 - p * 2800)
            boost = 0.10
            throttle = 0
            brake = int(85 * (1.0 - p * 0.5))
            clutch = int(60 * (1.0 - p))

        lap_seconds = (t % 75.0)
        best_lap = 62.663
        delta = round((lap_seconds - 62.45), 2)
        if delta > 3.0:
            delta = 0.21
        elif delta < -3.0:
            delta = -0.15

        fuel_l = round(max(5.0, 48.5 - (t * 0.02)), 1)

        self.current_data = TelemetryData(
            speedKmh=round(speed, 0),
            rpm=max(1000, min(8000, rpm)),
            maxRpm=8000,
            gear=gear,
            boostBar=round(boost, 2),
            fuelLiters=fuel_l,
            fuelKg=fuel_l,
            fuelPct=round(fuel_l / 0.55, 1),
            throttlePct=throttle,
            brakePct=brake,
            clutchPct=clutch,
            waterTempC=88.5,
            oilTempC=96.2,
            lapTimeSeconds=round(lap_seconds, 3),
            bestLapTimeSeconds=best_lap,
            deltaSeconds=delta,
            vehicleName="Demo Simulation",
            gameSource="DEMO",
            currentLap=3 + int(t // 75.0),
            position=1,
            isEngineOn=True
        )

    async def telemetry_loop(self):
        """Main loop: receives telemetry and broadcasts at 30Hz"""
        last_broadcast = 0.0
        broadcast_interval = 1.0 / 30.0  # 30 Hz
        last_tick = time.time()

        while self.running:
            now = time.time()
            dt = now - last_tick
            last_tick = now

            if self.force_demo:
                self.update_simulation_tick(dt)
            else:
                # Poll real telemetry sockets
                self.receive_forza_packet()
                self.receive_beamng_packet()

                # Poll ETS2 if connected
                ets2_data = self.ets2_client.poll()
                if ets2_data:
                    self.current_data = ets2_data
                    self.last_real_packet_time = now

            # Broadcast to WebSocket clients at 30 Hz
            if now - last_broadcast >= broadcast_interval:
                await self.broadcast_telemetry()
                last_broadcast = now

            # Sleep tiny amount to yield cleanly
            await asyncio.sleep(0.015)

    async def run(self, host: str = "0.0.0.0"):
        """Start the telemetry bridge server"""
        self.running = True
        logger.info("==================================================")
        logger.info(f"Telemetria Bridge Server avviato su {host}:{self.ws_port}")
        logger.info(f"In ascolto UDP Forza Horizon: porta {FORZA_PORT}")
        logger.info(f"In ascolto UDP BeamNG OutGauge: porte {BEAMNG_PORTS}")
        if self.force_demo:
            logger.info("Modalità DEMO abilitata: simulazione automatica attiva")
        else:
            logger.info("Modalità GIOCHI REALI attiva: in attesa dei dati da BeamNG o FH5")
        logger.info("==================================================")

        # Try ETS2 connection in background
        threading.Thread(target=self.ets2_client.connect, daemon=True).start()

        async with serve(self.register_client, host, self.ws_port):
            await self.telemetry_loop()

    def stop(self):
        self.running = False
        if self.forza_socket:
            try:
                self.forza_socket.close()
            except Exception:
                pass
        for _, sock in self.beamng_sockets:
            try:
                sock.close()
            except Exception:
                pass
        self.ets2_client.close()


async def main():
    force_demo = "--demo" in sys.argv
    bridge = TelemetryBridge(ws_port=DEFAULT_WS_PORT, force_demo=force_demo)
    try:
        await bridge.run()
    except KeyboardInterrupt:
        logger.info("Chiusura bridge...")
        bridge.stop()


if __name__ == "__main__":
    asyncio.run(main())

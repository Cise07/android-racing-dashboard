@echo off
title Telemetry Bridge - Racing Dashboard
cd /d "%~dp0"
echo ===================================================
echo     STARTING TELEMETRY BRIDGE FOR RACING DASHBOARD
echo ===================================================
echo.
echo Supportati: Forza Horizon 4/5, BeamNG.drive (OutGauge), ETS2
echo IP Locale PC: 
ipconfig | findstr /i "IPv4"
echo.
python pc-scripts\telemetry_bridge.py
pause

@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
set CODE=%errorlevel%
if not "%CODE%"=="0" echo Server stopped with exit code %CODE%. See logs\latest.log.
pause
exit /b %CODE%

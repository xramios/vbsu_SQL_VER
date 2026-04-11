@echo off
chcp 65001 >nul
REM Run seeder script - Double-click this file to run the seeder

set "SCRIPT_DIR=%~dp0"
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run_seeder.ps1"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Seeder failed with error code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Done!
pause

@echo off
title Super Mario X
echo ========================================
echo   Launching Super Mario X (with Sound)
echo ========================================
cd /d "%~dp0"
java -cp "lib/*;out/production/Mario-X" com.supermariox.Main
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Game closed or encountered an error.
    pause
)

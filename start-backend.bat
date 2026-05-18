@echo off
setlocal
cd /d "%~dp0"

echo [1/3] Checking Java...
where java >nul 2>nul
if errorlevel 1 (
    echo [Error] Java not found. Please install JDK 21 and configure PATH.
    pause
    exit /b 1
)

echo [2/3] Checking Maven...
where mvn >nul 2>nul
if errorlevel 1 (
    echo [Error] Maven not found. Please install Maven and configure PATH.
    pause
    exit /b 1
)

echo [3/3] Starting backend...
cd /d "%~dp0backend"
mvn spring-boot:run

endlocal

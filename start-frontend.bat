@echo off
setlocal
cd /d "%~dp0"

echo [1/3] Checking Node.js...
where node >nul 2>nul
if errorlevel 1 (
    echo [Error] Node.js not found. Please install Node.js and configure PATH.
    pause
    exit /b 1
)

echo [2/3] Checking npm...
where npm >nul 2>nul
if errorlevel 1 (
    echo [Error] npm not found. Please install Node.js correctly.
    pause
    exit /b 1
)

cd /d "%~dp0frontend"

if not exist "node_modules" (
    echo [3/3] node_modules not found. Installing dependencies...
    npm install
    if errorlevel 1 (
        echo [Error] npm install failed.
        pause
        exit /b 1
    )
)

echo [3/3] Starting frontend...
npm run dev

endlocal

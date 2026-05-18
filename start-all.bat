@echo off
setlocal
cd /d "%~dp0"

echo Starting backend in a new window...
start "xkxt-backend" cmd /k call "%~dp0start-backend.bat"

timeout /t 3 /nobreak >nul

echo Starting frontend in a new window...
start "xkxt-frontend" cmd /k call "%~dp0start-frontend.bat"

echo.
echo Backend and frontend start commands have been launched.
echo Frontend default URL: http://localhost:5173
echo Backend default URL:  http://localhost:8080
echo.
pause

endlocal

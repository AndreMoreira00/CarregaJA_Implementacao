@echo off
rem Desliga a carregaja-api e fecha a janela dela.
call "%~dp0_porta.bat" 8081
if errorlevel 1 (
  echo [API] ja estava parada.
) else (
  for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":8081 .*LISTENING"') do taskkill /PID %%p /T /F >nul 2>&1
  echo [API] parada.
)
taskkill /FI "WINDOWTITLE eq carregaja-api 8081*" /T /F >nul 2>&1
exit /b 0

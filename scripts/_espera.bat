@echo off
rem Uso: call _espera.bat URL NOME SEGUNDOS. Espera a URL responder, com qualquer status.
set /a "_limite=%3 / 2"
set /a "_tentativa=0"
:repete
curl -s -m 3 -o nul "%~1"
if not errorlevel 1 exit /b 0
set /a "_tentativa+=1"
if %_tentativa% geq %_limite% (
  echo [%2] NAO respondeu em %3 s.
  exit /b 1
)
ping -n 3 127.0.0.1 >nul
goto repete

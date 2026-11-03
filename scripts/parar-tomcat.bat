@echo off
rem Desliga o Tomcat. Se ele nao obedecer em 10 s, encerra o processo.
call "%~dp0_comum.bat" || exit /b 1
call "%~dp0_porta.bat" 8080
if errorlevel 1 (
  echo [Tomcat] ja estava parado.
  exit /b 0
)
call "%TOMCAT_DIR%\bin\shutdown.bat" >nul 2>&1
set /a "_tentativa=0"
:espera
ping -n 3 127.0.0.1 >nul
call "%~dp0_porta.bat" 8080
if errorlevel 1 (
  echo [Tomcat] parado.
  exit /b 0
)
set /a "_tentativa+=1"
if %_tentativa% lss 5 goto espera
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do taskkill /PID %%p /T /F >nul 2>&1
echo [Tomcat] parado a forca.
exit /b 0

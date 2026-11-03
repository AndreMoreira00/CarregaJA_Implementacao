@echo off
rem Use depois de mudar algo na API.
call "%~dp0parar-api.bat"
ping -n 3 127.0.0.1 >nul
call "%~dp0iniciar-api.bat"
exit /b %errorlevel%

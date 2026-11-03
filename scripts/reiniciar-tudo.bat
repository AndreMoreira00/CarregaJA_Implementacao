@echo off
call "%~dp0parar-tudo.bat"
ping -n 4 127.0.0.1 >nul
call "%~dp0iniciar-tudo.bat"
exit /b %errorlevel%

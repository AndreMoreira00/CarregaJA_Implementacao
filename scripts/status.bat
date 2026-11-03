@echo off
rem Mostra o que esta no ar.
call "%~dp0_comum.bat" || exit /b 1
call :peca 3306 "MySQL "
call :peca 8081 "API   "
call :peca 8080 "Tomcat"
if not defined CARREGAJA_SEM_PAUSA pause
exit /b 0

:peca
call "%~dp0_porta.bat" %1
if errorlevel 1 (echo %~2  porta %1  parado) else (echo %~2  porta %1  no ar)
exit /b 0

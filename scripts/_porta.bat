@echo off
rem Uso: call _porta.bat 8081. Devolve 0 se ha algo escutando na porta.
netstat -ano | findstr /r /c:":%1 .*LISTENING" >nul
exit /b %errorlevel%

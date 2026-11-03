@echo off
rem Desliga na ordem inversa: telas, API, banco.
call "%~dp0parar-tomcat.bat"
call "%~dp0parar-api.bat"
call "%~dp0parar-mysql.bat"
echo Tudo parado.
exit /b 0

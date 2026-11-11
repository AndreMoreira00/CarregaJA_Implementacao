@echo off
rem Desliga as telas, depois a API e por ultimo o banco.
call "%~dp0parar-tomcat.bat"
call "%~dp0parar-api.bat"
call "%~dp0parar-mysql.bat"
echo Tudo parado.
exit /b 0

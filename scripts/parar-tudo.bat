@echo off
rem Desliga a API, que serve as telas, e por ultimo o banco.
call "%~dp0parar-api.bat"
call "%~dp0parar-mysql.bat"
echo Tudo parado.
exit /b 0

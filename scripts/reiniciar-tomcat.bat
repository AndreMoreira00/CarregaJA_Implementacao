@echo off
rem Recompila as telas e reinicia o Tomcat.
call "%~dp0parar-tomcat.bat" || exit /b 1
call "%~dp0publicar-jsf.bat" || exit /b 1
call "%~dp0iniciar-tomcat.bat"
exit /b %errorlevel%

@echo off
rem Carrega a configuracao. Os outros scripts chamam este primeiro.
set "RAIZ=%~dp0.."
if not exist "%~dp0config.bat" (
  copy /y "%~dp0config.exemplo.bat" "%~dp0config.bat" >nul
  echo Criei scripts\config.bat. Abra, informe MYSQL_SENHA e os caminhos, e rode de novo.
  exit /b 1
)
call "%~dp0config.bat"
if "%MYSQL_SENHA%"=="" (
  echo Informe MYSQL_SENHA em scripts\config.bat.
  exit /b 1
)
rem O cliente do MySQL le a senha daqui, sem ela aparecer na linha de comando
set "MYSQL_PWD=%MYSQL_SENHA%"
set "CATALINA_HOME=%TOMCAT_DIR%"
exit /b 0

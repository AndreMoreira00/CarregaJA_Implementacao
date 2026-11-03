@echo off
rem Liga o Tomcat 9 em http://localhost:8080, na janela propria dele.
call "%~dp0_comum.bat" || exit /b 1
call "%~dp0_porta.bat" 8080
if not errorlevel 1 (
  echo [Tomcat] ja esta no ar.
  exit /b 0
)
call "%TOMCAT_DIR%\bin\startup.bat" >nul
echo [Tomcat] subindo...
call "%~dp0_espera.bat" http://localhost:8080/carregaja/autenticarusuario/AutenticarUsuarioView.xhtml Tomcat 120
if errorlevel 1 (
  echo [Tomcat] Veja o erro na janela do Tomcat, ou em %TOMCAT_DIR%\logs.
  exit /b 1
)
echo [Tomcat] no ar: http://localhost:8080/carregaja
exit /b 0

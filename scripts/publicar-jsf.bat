@echo off
rem Compila o carregaja-jsf e poe o .war no Tomcat. Com o Tomcat ligado, ele recarrega sozinho em cerca de 30 s.
call "%~dp0_comum.bat" || exit /b 1
pushd "%RAIZ%\carregaja-jsf"
echo [JSF] compilando...
call mvn -q clean package
if errorlevel 1 (
  popd
  echo [JSF] ERRO na compilacao: veja as mensagens acima.
  exit /b 1
)
call "%~dp0_porta.bat" 8080
if errorlevel 1 if exist "%TOMCAT_DIR%\webapps\carregaja" rmdir /s /q "%TOMCAT_DIR%\webapps\carregaja"
copy /y target\carregaja.war "%TOMCAT_DIR%\webapps\" >nul
popd
echo [JSF] publicado em %TOMCAT_DIR%\webapps
exit /b 0

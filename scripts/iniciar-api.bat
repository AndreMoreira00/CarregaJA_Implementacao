@echo off
rem Sobe a carregaja-api numa janela propria: http://localhost:8081. Precisa do MySQL ligado.
call "%~dp0_comum.bat" || exit /b 1
call "%~dp0_porta.bat" 8081
if not errorlevel 1 (
  echo [API] ja esta no ar.
  exit /b 0
)
set "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/%CARREGAJA_BANCO%?createDatabaseIfNotExist=true&useTimezone=true&serverTimezone=UTC"
pushd "%RAIZ%\carregaja-api"
start "carregaja-api 8081" cmd /k "chcp 65001 >nul & mvn spring-boot:run"
popd
echo [API] subindo na janela "carregaja-api 8081"...
call "%~dp0_espera.bat" http://localhost:8081/veiculos/meu API 120
if errorlevel 1 (
  echo [API] Veja o erro na janela "carregaja-api 8081".
  exit /b 1
)
echo [API] no ar: http://localhost:8081
exit /b 0

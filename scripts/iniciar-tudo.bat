@echo off
rem Sobe o MySQL, depois a API, que depende do banco, e por ultimo as telas no Tomcat, que dependem da API.
call "%~dp0_comum.bat" || goto falhou
call "%~dp0iniciar-mysql.bat" || goto falhou
call "%~dp0iniciar-api.bat" || goto falhou
call "%~dp0_porta.bat" 8080
if errorlevel 1 (
  call "%~dp0publicar-jsf.bat" || goto falhou
  call "%~dp0iniciar-tomcat.bat" || goto falhou
) else (
  echo [Tomcat] ja esta no ar. Para publicar as telas de novo: reiniciar-tomcat.bat
)
echo.
echo Tudo no ar:
echo   Tela    http://localhost:8080/carregaja
echo   API     http://localhost:8081
echo   Banco   localhost:3306, schema %CARREGAJA_BANCO%
echo   Login   %CARREGAJA_SINDICO_EMAIL% / %CARREGAJA_SINDICO_SENHA%  (senha provisoria, enquanto nao for trocada)
if not defined CARREGAJA_SEM_NAVEGADOR start "" http://localhost:8080/carregaja
exit /b 0

:falhou
echo.
echo Algo falhou: leia as mensagens acima.
if not defined CARREGAJA_SEM_PAUSA pause
exit /b 1

@echo off
rem Sobe o MySQL e depois a API, que depende do banco e serve as telas.
call "%~dp0_comum.bat" || goto falhou
call "%~dp0iniciar-mysql.bat" || goto falhou
call "%~dp0iniciar-api.bat" || goto falhou
echo.
echo Tudo no ar:
echo   Telas e API   http://localhost:8081
echo   Banco         localhost:3306, schema %CARREGAJA_BANCO%
echo   Login         %CARREGAJA_SINDICO_EMAIL% / %CARREGAJA_SINDICO_SENHA%  (senha provisoria, enquanto nao for trocada)
if not defined CARREGAJA_SEM_NAVEGADOR start "" http://localhost:8081
exit /b 0

:falhou
echo.
echo Algo falhou: leia as mensagens acima.
if not defined CARREGAJA_SEM_PAUSA pause
exit /b 1

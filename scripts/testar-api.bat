@echo off
rem Roda os testes de unidade da API. Nao precisa de banco.
pushd "%~dp0..\carregaja-api"
call mvn test
set "_resultado=%errorlevel%"
popd
if not defined CARREGAJA_SEM_PAUSA pause
exit /b %_resultado%

@echo off
rem APAGA o banco de desenvolvimento e sobe a API, que recria as tabelas e o primeiro sindico.
rem Use antes de rodar a colecao do Postman do comeco. So toca no banco CARREGAJA_BANCO.
call "%~dp0_comum.bat" || exit /b 1
echo Isto APAGA todos os dados do banco "%CARREGAJA_BANCO%".
set "_confirma="
set /p "_confirma=Digite ZERAR para confirmar: "
if not "%_confirma%"=="ZERAR" (
  echo Cancelado: nada foi apagado.
  exit /b 1
)
if "%MYSQL_DIR%"=="" (
  echo Sem MYSQL_DIR nao tenho o cliente do MySQL. Apague o banco "%CARREGAJA_BANCO%" a mao e rode iniciar-api.bat.
  exit /b 1
)
call "%~dp0iniciar-mysql.bat" || exit /b 1
call "%~dp0parar-api.bat"
ping -n 3 127.0.0.1 >nul
"%MYSQL_DIR%\bin\mysql.exe" -uroot -e "DROP DATABASE IF EXISTS `%CARREGAJA_BANCO%`"
if errorlevel 1 (
  echo [Banco] NAO consegui apagar: confira MYSQL_SENHA.
  exit /b 1
)
echo [Banco] "%CARREGAJA_BANCO%" apagado.
call "%~dp0iniciar-api.bat"
exit /b %errorlevel%

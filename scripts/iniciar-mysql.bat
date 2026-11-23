@echo off
rem Liga o MySQL em localhost:3306, numa janela minimizada.
call "%~dp0_comum.bat" || exit /b 1
call "%~dp0_porta.bat" 3306
if not errorlevel 1 (
  echo [MySQL] ja esta no ar.
  exit /b 0
)
if "%MYSQL_DIR%"=="" (
  echo [MySQL] a porta 3306 nao responde. Ligue o seu MySQL ou informe MYSQL_DIR em scripts\config.bat.
  exit /b 1
)
start "MySQL 3306" /min "%MYSQL_DIR%\bin\mysqld.exe" --defaults-file="%MYSQL_DIR%\my.ini" --console
echo [MySQL] subindo...
set /a "_tentativa=0"
:espera
ping -n 2 127.0.0.1 >nul
"%MYSQL_DIR%\bin\mysqladmin.exe" -uroot ping >nul 2>&1
if not errorlevel 1 (
  echo [MySQL] no ar: localhost:3306
  exit /b 0
)
set /a "_tentativa+=1"
if %_tentativa% lss 30 goto espera
echo [MySQL] NAO subiu em 30 s. Veja a janela "MySQL 3306".
exit /b 1

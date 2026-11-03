@echo off
rem Desliga o MySQL, se foi este projeto que o subiu pela pasta MYSQL_DIR.
call "%~dp0_comum.bat" || exit /b 1
call "%~dp0_porta.bat" 3306
if errorlevel 1 (
  echo [MySQL] ja estava parado.
  exit /b 0
)
if "%MYSQL_DIR%"=="" (
  echo [MySQL] MYSQL_DIR vazio: deixei o seu MySQL ligado.
  exit /b 0
)
"%MYSQL_DIR%\bin\mysqladmin.exe" -uroot shutdown >nul 2>&1
echo [MySQL] parado.
exit /b 0

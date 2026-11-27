@echo off
chcp 65001 >nul
title Reiniciar base de datos - Cafeteria universitaria
cd /d "%~dp0"
echo ==========================================================
echo   ATENCION: se borrara la base CafeteriaDB y se creara de nuevo
echo   con los datos de prueba. Los pedidos registrados se pierden.
echo ==========================================================
choice /c SN /m "Desea continuar"
if errorlevel 2 exit /b 0

for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
  if "%%a"=="MSSQL_SA_PASSWORD" set "SA_PWD=%%b"
)

docker exec cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%SA_PWD%" -C -I -b -Q "IF DB_ID('CafeteriaDB') IS NOT NULL BEGIN ALTER DATABASE CafeteriaDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE CafeteriaDB; END"
if errorlevel 1 (
  echo [ERROR] No se pudo borrar la base. Verifica que Docker y el contenedor esten corriendo.
  pause
  exit /b 1
)
echo Base anterior eliminada.
call "%~dp0levantar_bd.bat"

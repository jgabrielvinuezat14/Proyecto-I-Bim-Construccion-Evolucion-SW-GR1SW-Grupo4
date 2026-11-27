@echo off
chcp 65001 >nul
title Base de datos - Cafeteria universitaria
cd /d "%~dp0"

echo ==========================================================
echo   Levantar SQL Server y crear la base CafeteriaDB
echo ==========================================================
echo.

docker info >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker no esta corriendo.
  echo Abre Docker Desktop, espera a que diga "Engine running" y vuelve a ejecutar este archivo.
  pause
  exit /b 1
)

if not exist .env (
  copy .env.example .env >nul
  echo Se creo el archivo .env con la clave de ejemplo.
)

for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
  if "%%a"=="MSSQL_SA_PASSWORD" set "SA_PWD=%%b"
)

echo [1/4] Iniciando SQL Server en Docker (la primera vez tarda unos minutos)...
docker compose up -d
if errorlevel 1 (
  echo [ERROR] No se pudo iniciar el contenedor.
  pause
  exit /b 1
)

echo.
echo [2/4] Esperando a que SQL Server este listo...
set /a intentos=0
:esperar
docker exec cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%SA_PWD%" -C -Q "SELECT 1" >nul 2>&1
if not errorlevel 1 goto listo
set /a intentos+=1
if %intentos% geq 36 (
  echo [ERROR] SQL Server no respondio despues de 3 minutos.
  echo Revisa en Docker Desktop el contenedor cafeteria-sqlserver.
  pause
  exit /b 1
)
timeout /t 5 /nobreak >nul
goto esperar

:listo
echo SQL Server listo.
echo.
echo [3/4] Creando tablas y cargando datos de prueba...
docker exec cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%SA_PWD%" -C -I -b -i /scripts/schema.sql
if errorlevel 1 goto fallo
docker exec cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%SA_PWD%" -C -I -b -i /scripts/datos_iniciales.sql
if errorlevel 1 goto fallo

echo.
echo [4/4] Productos guardados en la base:
docker exec cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%SA_PWD%" -C -W -Q "SET NOCOUNT ON; SELECT codigo, nombre, precio, cantidad_disponible, disponible FROM CafeteriaDB.dbo.Producto ORDER BY codigo"
echo.
echo Listo. La base CafeteriaDB esta creada y con datos de prueba.
pause
exit /b 0

:fallo
echo.
echo [ERROR] Fallo al ejecutar un script SQL. Toma una captura de esta ventana.
pause
exit /b 1

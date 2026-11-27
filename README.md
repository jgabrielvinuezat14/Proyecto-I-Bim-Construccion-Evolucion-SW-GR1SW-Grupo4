# Gestión de pedidos de una cafetería universitaria

**Escuela Politécnica Nacional · Facultad de Ingeniería de Sistemas**  
**Asignatura:** (RRA20) Construcción y Evolución de Software  
**Grupo:** 4 — GR1SW | **Release:** 1.0 | **Entrega:** 27 de noviembre de 2026

## 1. Problema y objetivo

La cafetería recibe pedidos presenciales durante los recesos, lo que genera filas y dificulta el seguimiento de la preparación. La aplicación permite realizar pedidos anticipados, consultar su estado y gestionar la entrega mediante un código único de retiro.

## 2. Alcance

| HU | Funcionalidad |
|---|---|
| HU-01 | Consultar menú y precios vigentes |
| HU-02 | Agregar, modificar y quitar productos del pedido |
| HU-03 | Revisar subtotales y total |
| HU-04 | Confirmar pedido, descontar unidades y generar código de retiro |
| HU-05 | Consultar pedidos propios y su estado |
| HU-06 | Administrar productos, precios, cantidades y disponibilidad |
| HU-07 | Consultar y filtrar pedidos recibidos |
| HU-08 | Registrar avances y entrega |
| HU-09 | Registrar clientes e iniciar y cerrar sesión |
| HU-10 | Controlar el acceso del personal |

Estados del pedido: **Pendiente → En preparación → Listo para retirar → Entregado**.

El Release 1.0 no incluye pagos electrónicos, promociones, reembolsos, pedidos programados ni inventario de ingredientes.

## 3. Tecnologías y requisitos previos

- Java JDK 21 y Apache Maven 3.9.x.
- Apache Tomcat 9 (Servlet/JSP con `javax.servlet`).
- Microsoft SQL Server 2022 Developer, JDBC y Docker Compose.
- JUnit 5, Python 3 (biblioteca estándar para E2E) y Git.
- Docker Desktop iniciado; puertos 1433 y 8080 disponibles.

## 4. Instalación y ejecución (Windows PowerShell)

```powershell
git clone https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4.git
cd Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Configurar `MSSQL_SA_PASSWORD`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `STAFF_ACCESS_CODE` y `STAFF_PASSWORD`. No publicar `.env` ni credenciales reales. Para el entorno local con `sa`, usar la misma contraseña de SQL Server en `DB_PASSWORD`.

```powershell
.\levantar_bd.bat
mvn -B clean package
```

El script existente inicializa `CafeteriaDB` y sus datos de ejemplo. El WAR queda en `target/cafeteria.war`. Para Tomcat instalado localmente, configurar las variables de entorno de la aplicación en el proceso de Tomcat, copiar el WAR a `webapps` e iniciar el servidor. Abrir `http://localhost:8080/cafeteria/menu`. Si Tomcat se ejecuta en Docker, usar `host.docker.internal` en la URL JDBC en lugar de `localhost`.

Consultar el [manual de instalación](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/blob/documentacion/docs/instalacion/Manual_Instalacion_Cafeteria_Release_1.0.pdf) para más detalles.

## 5. Pruebas unitarias

```powershell
mvn -B clean test
```

Los resultados JUnit se generan en `target/surefire-reports/`. El flujo de integración continua en `.github/workflows/ci.yml` automatiza compilación, pruebas y empaquetado.

## 6. Pruebas de extremo a extremo (E2E)

El script `pruebas-e2e/e2e.py` utiliza HTTP, cookies de sesión y consultas SQL mediante `docker exec`. Verifica los flujos de cliente y personal, validaciones, importes, persistencia de datos durante la ejecución, permisos y concurrencia. La persistencia tras reinicio se verifica por separado.

**Advertencia:** ejecutar únicamente sobre `CafeteriaDB_Prueba`, con datos iniciales limpios; el script registra clientes y pedidos y modifica productos. No utilizar la base principal.

Con SQL Server activo, crear la base aislada y cargar datos (desde la raíz):

```powershell
docker exec cafeteria-sqlserver sh -c 'sed s/CafeteriaDB/CafeteriaDB_Prueba/g /scripts/schema.sql | /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $MSSQL_SA_PASSWORD -C -b -I'
docker exec cafeteria-sqlserver sh -c 'sed s/CafeteriaDB/CafeteriaDB_Prueba/g /scripts/datos_iniciales.sql | /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $MSSQL_SA_PASSWORD -C -b -I'
```

Estos comandos no limpian una base ya utilizada. Antes de repetir la suite, conservar evidencias y restaurar exclusivamente la base de pruebas. Nunca usar `docker compose down -v` para esta tarea.

Desplegar el WAR de pruebas en Tomcat 9 (nombre y puerto deben estar libres):

```powershell
$clave = docker exec cafeteria-sqlserver printenv MSSQL_SA_PASSWORD
$war = (Resolve-Path .\target\cafeteria.war).Path
docker run -d --name cafeteria-tomcat-pruebas `
  -p 127.0.0.1:8080:8080 `
  -e "DB_URL=jdbc:sqlserver://host.docker.internal:1433;databaseName=CafeteriaDB_Prueba;encrypt=true;trustServerCertificate=true" `
  -e "DB_USER=sa" `
  -e "DB_PASSWORD=$clave" `
  -e "STAFF_ACCESS_CODE=CAFE-2026" `
  -e "STAFF_PASSWORD=Personal#2026" `
  --mount "type=bind,source=$war,target=/usr/local/tomcat/webapps/cafeteria.war,readonly" `
  tomcat:9.0-jdk21-temurin
Remove-Variable clave
```

Las credenciales del personal de este ejemplo son exclusivamente fixtures locales del script, no credenciales de producción. Comprobar que el menú responde antes de ejecutar:

```powershell
$ev = "$HOME\Documents\Evidencias-Cafeteria\Release-final"
New-Item -ItemType Directory -Force $ev | Out-Null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
python -u .\pruebas-e2e\e2e.py "$ev\codigo-retiro.txt" 2>&1 | Tee-Object "$ev\resultados-e2e.txt"
$LASTEXITCODE
```

Un código de salida 0 indica que todas las comprobaciones de la suite pasaron; un valor distinto de 0 requiere revisión. No repetir la suite sobre datos ya modificados.

### Resultados registrados al 27/11/2026

Código evaluado: base `6a00859` (PR #34, SonarQube) más la corrección HU-09 y el script E2E, registrados después en los commits `786a1bd` y `1d5c840`. El PR #35 (`9a1b7b2`) incorporó únicamente documentación. Las primeras dos ejecuciones se realizaron sobre `5f980b6`.

| Verificación | Resultado observado |
|---|---|
| Unitarias | 45/45, sin fallos ni errores |
| E2E inicial | 51/57 |
| E2E tras corregir cinco comprobaciones | 56/57 (98,25 %) |
| E2E tras integrar SonarQube y corregir HU-09 | 57/57 (100 %), salida 0 |
| Persistencia de pedidos tras reinicio | 3/3 registros conservados |

CA-09.1 E1 corregido: el registro genera el código, muestra la confirmación y redirige a iniciar sesión sin autenticar automáticamente. La tercera ejecución fue satisfactoria. Maven: BUILD SUCCESS (5,721 s), 45 pruebas sin fallos, errores ni omisiones.

SonarQube: 48 hallazgos iniciales, 16 corregidos y 32 pendientes, según el reporte comunicado por el equipo (PR #34). No se realizó un nuevo análisis estático independiente para este informe.

Los resultados no certifican todos los escenarios del backlog. Revisar también destinos inmediatos de cierre de sesión (HU-09 E7 y HU-10 E5), no verificados directamente por la suite actual.

## 7. Estructura del repositorio

```text
.github/workflows/       Integración continua
database/                Esquema y datos iniciales
src/main/java/           Modelo, DAO, servicios, controladores y configuración
src/main/webapp/         JSP, CSS y configuración web
src/test/java/           Pruebas unitarias y soportes
pruebas-e2e/e2e.py        Pruebas HTTP y SQL
pom.xml                  Construcción Maven
docker-compose.yml       SQL Server
levantar_bd.bat          Inicialización de la base principal
reiniciar_bd.bat         Restauración de la base principal (destructiva)
.env.example             Plantilla de configuración
```

## 8. Documentación y colaboración

La documentación académica está en la carpeta `docs/` de la rama [`documentacion`](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/tree/documentacion/docs). La rama `main` contiene solo el código.

- [Backlog del Release 1.0](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/blob/documentacion/docs/requisitos/Backlog_Cafeteria_Release_1.0.pdf)
- [Diseño](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/tree/documentacion/docs/diseno)
- [Casos de prueba y resultados](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/tree/documentacion/docs/pruebas)
- [Informe de resultados de pruebas](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/blob/documentacion/docs/pruebas/Informe_Resultados_Pruebas_Release_1.0.pdf)
- [Reporte de análisis estático](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/blob/documentacion/docs/analisis-estatico/Reporte_Analisis_Estatico_Cafeteria_Release_1.0.pdf)
- [Manual de instalación](https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4/tree/documentacion/docs/instalacion)

Flujo de trabajo: issue por tarea, rama por cambio, Conventional Commits, Pull Request y revisión de otro integrante antes de integrar. No subir credenciales, `target/`, archivos temporales de Python ni evidencias con información sensible.

## 9. Integrantes

David Jumbo · Gabriel Vinueza · Mateo Yaguari · Analy Yepez.

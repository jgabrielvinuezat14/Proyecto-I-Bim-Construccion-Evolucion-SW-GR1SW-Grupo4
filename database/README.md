# Base de datos — CafeteriaDB

Motor: **Microsoft SQL Server 2022** (edición Developer), ejecutado en Docker con un volumen persistente.

## Archivos

| Archivo | Contenido |
| --- | --- |
| `schema.sql` | Crea la base `CafeteriaDB` y las tablas `Cliente`, `Producto`, `Pedido` y `DetallePedido`. |
| `datos_iniciales.sql` | Carga productos y clientes de prueba que coinciden con los escenarios del backlog. |

Ambos scripts se pueden ejecutar varias veces sin duplicar datos.

## Forma rápida (Windows)

Con Docker Desktop abierto, dar doble clic en `levantar_bd.bat` (raíz del repositorio). Levanta SQL Server, crea las tablas, carga los datos de prueba y muestra los productos.

## Levantar SQL Server

Desde la raíz del repositorio:

```bash
cp .env.example .env        # en Windows: copy .env.example .env
# editar .env y poner una contraseña propia
docker compose up -d
```

## Crear las tablas y cargar los datos

```bash
docker exec -it cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "TU_CLAVE" -C -I -i /scripts/schema.sql
docker exec -it cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "TU_CLAVE" -C -I -i /scripts/datos_iniciales.sql
```

También se pueden abrir y ejecutar desde SQL Server Management Studio (SSMS) conectándose a `localhost,1433` con el usuario `sa`.

## Comprobar que los datos persisten

```bash
docker compose down        # detiene y elimina el contenedor, NO el volumen
docker compose up -d
docker exec -it cafeteria-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "TU_CLAVE" -C -Q "SELECT codigo, nombre FROM CafeteriaDB.dbo.Producto"
```

Los productos deben seguir apareciendo. **No usar** `docker compose down -v`, porque ese comando borra el volumen con los datos.

## Notas

- Las contraseñas van solo en `.env`, que no se sube al repositorio.
- El código de acceso del personal no se guarda en la base (propuesta pendiente de confirmar con el equipo).

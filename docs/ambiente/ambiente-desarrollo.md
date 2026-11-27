# Documentación del Ambiente de Desarrollo — Release 1.0

- **Proyecto:** Gestión de pedidos de una cafetería universitaria
- **Asignatura:** Construcción y Evolución de Software (GR1SW — Grupo 4)
- **Fecha comprometida del Release 1.0:** 27 de noviembre de 2026
- **Versión del documento:** 1.0
- **Responsable de integración:** David Jumbo

---

## 1. Propósito

Este documento describe las herramientas seleccionadas, la configuración del entorno de ejecución con Microsoft SQL Server en Docker, las variables de configuración y el flujo de trabajo en GitHub para el desarrollo del Release 1.0.

**Repositorio:** https://github.com/jgabrielvinuezat14/Proyecto-I-Bim-Construccion-Evolucion-SW-GR1SW-Grupo4

---

## 2. Herramientas y tecnologías seleccionadas

La base tecnológica acordada por el equipo corresponde a una aplicación web monolítica por capas (presentación, controladores, lógica de negocio y acceso a datos).

| Categoría | Herramienta / Tecnología | Versión verificada | Uso en el proyecto |
| --- | --- | --- | --- |
| Lenguaje de programación | Java (JDK) | OpenJDK 21.0.10 LTS (`maven.compiler.release = 21`) | Implementación de controladores, servicios y acceso a datos. |
| Entorno de desarrollo (IDE) | IntelliJ IDEA | 2024.x Community/Ultimate | Desarrollo, depuración y ejecución de pruebas locales. |
| Tecnología web | Servlets, JSP, HTML y CSS | Servlet API 4.0.1, JSP API 2.3.3 y JSTL 1.2 (`javax.servlet`, HTML5 y CSS3) | Capa de presentación y manejo de solicitudes HTTP. |
| Servidor web | Apache Tomcat | 9.0.x (9.0.124 / 9.0.97) | Despliegue de la aplicación empaquetada en formato WAR. |
| Gestión de dependencias y build | Apache Maven | Apache Maven 3.9.10 | Compilación, gestión de dependencias y empaquetado WAR (`cafeteria.war`). |
| Motor de base de datos | Microsoft SQL Server | SQL Server 2022 (16.0.4325), en Docker | Persistencia de clientes, productos, pedidos y detalles. |
| Controlador de base de datos | Microsoft JDBC Driver for SQL Server | `mssql-jdbc` 12.8.1.jre11 (`com.microsoft.sqlserver`) | Conexión JDBC desde la capa de acceso a datos hacia SQL Server. |
| Seguridad de contraseñas | Spring Security Crypto | `spring-security-crypto` 7.1.2 (`org.springframework.security`) | Generación y verificación de hashes BCrypt para contraseñas de clientes (HU-09, RNF-04). |
| Contenedores | Docker / Docker Compose | Docker Engine 29.8.2 y Docker Compose v5.6.0 | Ejecución de Microsoft SQL Server con persistencia por volumen (`docker-compose.yml`). |
| Pruebas automatizadas | JUnit | JUnit Jupiter (JUnit 5) 5.10.2 (`org.junit.jupiter:junit-jupiter`) | Pruebas unitarias de reglas de negocio y servicios. |
| Análisis estático | SonarQube | SonarQube Community Build v26.6.0.123539 (en Docker) | Generación del reporte de análisis estático del código (Entregable 5). |
| Control de versiones | Git / GitHub | Git 2.54.0.windows.1 / GitHub | Alojamiento del repositorio, gestión de issues, ramas y revisión de código. |

### Notas de compatibilidad técnica

- **Apache Tomcat 9 vs. Tomcat 10+:** Apache Tomcat 9 utiliza el espacio de nombres `javax.servlet`, mientras que Tomcat 10 y posteriores utilizan `jakarta.servlet`. En caso de cambiar la versión de Tomcat, el equipo deberá ajustar las dependencias en `pom.xml` y las importaciones en el código fuente de manera coherente.
- **Controlador JDBC y nivel de compatibilidad:** La imagen utilizada es `mcr.microsoft.com/mssql/server:2022-latest` (SQL Server 2022, versión 16.0.4325, nivel de compatibilidad 160). Se utiliza `mssql-jdbc` 12.8.1.jre11 configurado en `pom.xml` y verificado con JDK 21.

---

## 3. Configuración de base de datos con Docker y persistencia

Microsoft SQL Server se ejecuta dentro de un contenedor Docker para estandarizar el motor de base de datos entre todos los integrantes y facilitar la reproducibilidad del entorno.

### 3.1 Registro de imágenes y volúmenes Docker

| Componente | Imagen / Recurso | Etiqueta / Versión verificada | Propósito |
| --- | --- | --- | --- |
| Base de datos | Imagen oficial de Microsoft SQL Server (`mcr.microsoft.com/mssql/server`) | `2022-latest` (SQL Server 2022, versión 16.0.4325) | Instancia de SQL Server para desarrollo y pruebas, definida en `docker-compose.yml` (contenedor `cafeteria-sqlserver`, puerto `1433`). |
| Persistencia de datos | Volumen nominado de Docker `cafeteria_sqlserver_data` | N/A | Conservar los archivos de base de datos (`/var/opt/mssql`) fuera del ciclo de vida del contenedor. |
| Scripts de inicialización | Montaje de solo lectura `./database:/scripts:ro` | `schema.sql` y `datos_iniciales.sql` | Creación repetible del esquema `CafeteriaDB` y carga de datos de prueba mediante `levantar_bd.bat` / `reiniciar_bd.bat`. |

### 3.2 Consideraciones de persistencia y red

1. **Persistencia ante reinicios y recreación (RNF-01):** Los datos de clientes, productos, pedidos y detalles se almacenan en el volumen persistente `cafeteria_sqlserver_data` montado sobre `/var/opt/mssql`, conservando los registros tanto ante reinicios de la aplicación como ante la recreación del contenedor de SQL Server.
2. **Conectividad según modo de ejecución de la aplicación:**
   - **Tomcat local + SQL Server en Docker:** La aplicación se conecta mediante JDBC al puerto mapeado del contenedor en el equipo anfitrión (por defecto, `1433`).
   - **Tomcat en Docker + SQL Server en Docker:** Ambos contenedores comparten una red de Docker y la cadena de conexión utiliza el nombre del servicio del contenedor de base de datos (no `localhost`, dado que dentro del contenedor web `localhost` apunta al propio contenedor).

### 3.3 Concurrencia

Las operaciones de confirmación de pedidos y descuento de unidades disponibles se ejecutan dentro de una misma transacción JDBC en SQL Server (`commit` / `rollback`), garantizando consistencia ante solicitudes concurrentes (RNF-03).

---

## 4. Variables de configuración

Por seguridad (RNF-04), ninguna contraseña, cadena de conexión con credenciales ni clave de acceso del personal se incluye en el código fuente ni en el repositorio de GitHub. Los archivos locales de configuración (como `.env`, creado a partir de la plantilla `.env.example`) se excluyen mediante `.gitignore`.

| Variable | Ámbito | Propósito |
| --- | --- | --- |
| `DB_URL` | Aplicación web | Cadena de conexión JDBC hacia la instancia de Microsoft SQL Server (host o servicio Docker, puerto y base `CafeteriaDB`). |
| `DB_USER` | Aplicación web | Usuario autorizado para la conexión JDBC con la base de datos. |
| `DB_PASSWORD` | Aplicación web | Contraseña del usuario de base de datos. |
| `ACCEPT_EULA` | Contenedor SQL Server | Aceptación de los términos de licencia requerida por la imagen oficial de Microsoft SQL Server. |
| `MSSQL_PID` | Contenedor SQL Server | Edición configurada del motor (`Developer`). |
| `MSSQL_SA_PASSWORD` | Contenedor SQL Server | Contraseña de administrador definida localmente al inicializar el contenedor de SQL Server. |
| `STAFF_ACCESS_CODE` | Aplicación web (HU-10) | Código de acceso utilizado para autenticar al personal de cafetería en el módulo administrativo. |
| `STAFF_PASSWORD` | Aplicación web (HU-10) | Contraseña configurada en entorno utilizada junto con el código de acceso para autenticar al personal de cafetería. |

---

## 5. Estrategia de ramificación y flujo de trabajo en GitHub

### 5.1 Estructura de ramas

| Rama | Propósito | Regla de integración |
| --- | --- | --- |
| `main` | Contiene únicamente el código fuente estable, compilable y probado de la aplicación. | Solo recibe cambios mediante Pull Request aprobado tras revisión y verificación con `mvn clean package`. |
| `documentacion` | Reúne los ocho entregables documentales exigidos para el Release 1.0 dentro de `docs/`. | Recibe actualizaciones por Pull Request desde ramas `docs/*` para mantener trazabilidad y evitar sobrescrituras. |
| `feature/<descripcion>` | Desarrollo de historias de usuario o componentes funcionales nuevos. | Se crea desde `main` y se integra de vuelta a `main` mediante Pull Request. |
| `fix/<descripcion>` | Corrección de defectos detectados en pruebas o hallazgos de análisis estático. | Se crea desde `main` y se integra mediante Pull Request documentando la causa y verificación. |
| `chore/<descripcion>` | Mantenimiento técnico o completitud de JavaDoc sin alterar comportamiento funcional. | Se crea desde `main` y se integra a `main` mediante Pull Request. |
| `test/<descripcion>` | Incorporación de suites de pruebas automatizadas de extremo a extremo. | Se crea desde `main` y se integra a `main` mediante Pull Request. |
| `docs/<descripcion>` | Elaboración o actualización de entregables documentales. | Se crea desde `documentacion` y se integra a `documentacion` mediante Pull Request. |

### 5.2 Flujo de trabajo del equipo

1. **Registro en Issues:** Cada historia de usuario, tarea técnica, corrección o entregable documental se gestiona mediante un Issue en GitHub con responsable asignado.
2. **Creación de rama:** El responsable actualiza la rama base correspondiente y crea su rama de trabajo desde `main` (código) o desde `documentacion` (documentos) siguiendo la convención definida.
3. **Commits atómicos y claros:** Cada commit refleja un cambio coherente con un mensaje descriptivo tipo Conventional Commits (por ejemplo, `feat(hu09): ... (#14)` o `docs(ambiente): ...`), sin commits vacíos ni credenciales.
4. **Verificación previa:** Para ramas de código hacia `main`, es obligatorio comprobar localmente que `mvn clean package` compila y aprueba el 100% de las pruebas unitarias acumuladas antes de abrir el PR.
5. **Pull Request (PR) y revisión:** Se abre un PR hacia la rama destino (`Closes #N`) y al menos otro integrante del equipo lo revisa y aprueba antes de hacer *Merge*.

---

## 6. Control de versión documental

| Entregable | Archivos | Rama | Versión | Estado |
| --- | --- | --- | --- | --- |
| Entregable 3 — Ambiente de desarrollo | `docs/ambiente/ambiente-desarrollo.md`, `docs/ambiente/Ambiente_desarrollo.pdf` y `CONTRIBUTING.md` | `documentacion` | 1.0 | Finalizado y verificado para el Release 1.0 |

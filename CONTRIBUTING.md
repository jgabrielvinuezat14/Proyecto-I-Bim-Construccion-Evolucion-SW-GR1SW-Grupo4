# Guía de Contribución — Cafetería Universitaria (Release 1.0)

Reglas de entorno y flujo de colaboración (**GitHub Flow**) acordadas por el **Grupo 4 (GR1SW)** para la asignatura *Construcción y Evolución de Software* (EPN).

---

## 1. Requisitos del entorno local

- **JDK:** OpenJDK 21 (`21.0.10 LTS`)
- **Build:** Apache Maven `3.9.10`
- **Contenedores:** Docker Desktop con Docker Compose (`cafeteria-sqlserver` sobre puerto `1433`)
- **Servidor web:** Apache Tomcat `9.0.x` (`javax.servlet` 4.0)

Antes de ejecutar la aplicación localmente:
1. Copiar `.env.example` como `.env` y definir contraseñas propias (**nunca** subir `.env` al repositorio).
2. Iniciar la base de datos y datos de prueba ejecutando `levantar_bd.bat` (o los scripts `database/schema.sql` y `database/datos_iniciales.sql`).

---

## 2. Separación de ramas principales

- **`main`:** contiene **únicamente el código fuente, scripts SQL y pruebas automatizadas** en estado compilable y verificado. No subir entregables de `docs/` a `main`.
- **`documentacion`:** contiene los **8 entregables documentales** del Release 1.0 organizados bajo `docs/` (acompañados de su versión en Markdown/fuente y PDF).

---

## 3. Flujo paso a paso para cambios de código (`main`)

1. Asegurarse de tener un **Issue** asignado en GitHub.
2. Actualizar `main` y crear una rama aislada (`feature/...`, `fix/...`, `chore/...` o `test/...`):
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/nombre-corto
   ```
3. Implementar únicamente los archivos correspondientes a la tarea, sin incluir configuraciones locales del IDE (`.idea/`, `.vscode/`) ni credenciales quemadas en código (**RNF-04**).
4. Ejecutar obligatoriamente la compilación y todas las pruebas unitarias acumuladas:
   ```bash
   mvn clean package
   ```
5. Realizar commits atómicos referenciando el Issue y subir la rama:
   ```bash
   git commit -m "feat(alcance): descripcion breve (#NumeroIssue)"
   git push -u origin feature/nombre-corto
   ```
6. Abrir un **Pull Request** hacia `main` indicando `Closes #NumeroIssue` y asignar al revisor del equipo. Ningún PR se une sin aprobación previa de otro integrante.

---

## 4. Flujo paso a paso para documentación (`documentacion`)

1. Partir siempre desde `documentacion` actualizada:
   ```bash
   git checkout documentacion
   git pull origin documentacion
   git checkout -b docs/nombre-entregable
   ```
2. Colocar los archivos dentro de su subcarpeta correspondiente en `docs/` (`requisitos/`, `diseno/`, `ambiente/`, `javadoc/`, `analisis-estatico/`, `pruebas/`, `instalacion/`, `usuario/`), incluyendo tanto el documento editable/Markdown como el PDF exportado.
3. Abrir un **Pull Request** apuntando a **`documentacion`** (nunca a `main`) para revisión e integración.

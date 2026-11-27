/* =====================================================================
   Proyecto: Gestión de pedidos de una cafetería universitaria
   Release 1.0 — Construcción y Evolución de Software, GR1SW, Grupo 4
   Script de creación de la base de datos (Microsoft SQL Server 2022)

   Ejecutar con un usuario con permisos para crear bases de datos.
   El script es repetible: si la base ya existe, solo crea lo que falta.
   ===================================================================== */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF DB_ID(N'CafeteriaDB') IS NULL
    CREATE DATABASE CafeteriaDB;
GO

USE CafeteriaDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ---------------------------------------------------------------------
   Cliente
   Cliente universitario (estudiante o docente). Según la especificación
   del cliente (sección 7), se identifica mediante un código y sus datos:
   nombre, apellido, cédula, celular y correo institucional. El equipo
   acordó que el cliente ingresa con su correo institucional.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Cliente', N'U') IS NULL
CREATE TABLE dbo.Cliente (
    id_cliente      INT IDENTITY(1,1)  NOT NULL,
    codigo_cliente  VARCHAR(10)        NOT NULL,
    nombre          NVARCHAR(60)       NOT NULL,
    apellido        NVARCHAR(60)       NOT NULL,
    cedula          CHAR(10)           NOT NULL,
    celular         VARCHAR(10)        NOT NULL,
    correo          NVARCHAR(120)      NOT NULL,
    fecha_registro  DATETIME2(0)       NOT NULL CONSTRAINT DF_Cliente_fecha DEFAULT SYSDATETIME(),
    CONSTRAINT PK_Cliente PRIMARY KEY (id_cliente),
    CONSTRAINT UQ_Cliente_codigo UNIQUE (codigo_cliente),
    CONSTRAINT UQ_Cliente_cedula UNIQUE (cedula),
    CONSTRAINT UQ_Cliente_correo UNIQUE (correo),
    CONSTRAINT CK_Cliente_cedula CHECK (cedula NOT LIKE '%[^0-9]%' AND LEN(cedula) = 10),
    CONSTRAINT CK_Cliente_celular CHECK (celular NOT LIKE '%[^0-9]%' AND LEN(celular) = 10),
    CONSTRAINT CK_Cliente_correo CHECK (correo LIKE N'%_@_%._%')
);
GO

/* ---------------------------------------------------------------------
   Producto
   Artículo del menú (alimento o bebida): código, nombre, precio y
   disponibilidad, según la especificación del cliente (sección 7).
   unidades_disponibles es una AMPLIACIÓN DEL EQUIPO definida en el
   backlog (HU-01, HU-02, HU-04 y HU-06); no la pide el cliente y no
   equivale a inventario de ingredientes. Un producto se muestra al
   cliente solo si disponible = 1 y unidades_disponibles > 0.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Producto', N'U') IS NULL
CREATE TABLE dbo.Producto (
    id_producto           INT IDENTITY(1,1)  NOT NULL,
    codigo                VARCHAR(10)        NOT NULL,
    nombre                NVARCHAR(100)      NOT NULL,
    precio                DECIMAL(10,2)      NOT NULL,
    unidades_disponibles  INT                NOT NULL CONSTRAINT DF_Producto_unidades DEFAULT 0,
    disponible            BIT                NOT NULL CONSTRAINT DF_Producto_disponible DEFAULT 1,
    CONSTRAINT PK_Producto PRIMARY KEY (id_producto),
    CONSTRAINT UQ_Producto_codigo UNIQUE (codigo),
    CONSTRAINT CK_Producto_precio CHECK (precio > 0),
    CONSTRAINT CK_Producto_unidades CHECK (unidades_disponibles >= 0)
);
GO

/* ---------------------------------------------------------------------
   Pedido
   Pedido confirmado. Inicia en PENDIENTE (RN-NEG-06) y recibe un código
   de retiro único de 6 caracteres alfanuméricos en mayúsculas (RN-NEG-05).
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Pedido', N'U') IS NULL
CREATE TABLE dbo.Pedido (
    id_pedido       INT IDENTITY(1,1)  NOT NULL,
    codigo_retiro   CHAR(6)            NOT NULL,
    id_cliente      INT                NOT NULL,
    fecha_registro  DATETIME2(0)       NOT NULL CONSTRAINT DF_Pedido_fecha DEFAULT SYSDATETIME(),
    estado          VARCHAR(20)        NOT NULL CONSTRAINT DF_Pedido_estado DEFAULT 'PENDIENTE',
    total           DECIMAL(10,2)      NOT NULL,
    CONSTRAINT PK_Pedido PRIMARY KEY (id_pedido),
    CONSTRAINT UQ_Pedido_codigo_retiro UNIQUE (codigo_retiro),
    CONSTRAINT FK_Pedido_Cliente FOREIGN KEY (id_cliente) REFERENCES dbo.Cliente (id_cliente),
    CONSTRAINT CK_Pedido_codigo_retiro CHECK (
        codigo_retiro COLLATE Latin1_General_BIN LIKE '[A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9]'
    ),
    CONSTRAINT CK_Pedido_estado CHECK (
        estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO_PARA_RETIRAR', 'ENTREGADO')
    ),
    CONSTRAINT CK_Pedido_total CHECK (total > 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Pedido_estado')
    CREATE INDEX IX_Pedido_estado ON dbo.Pedido (estado, fecha_registro);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Pedido_cliente')
    CREATE INDEX IX_Pedido_cliente ON dbo.Pedido (id_cliente);
GO

/* ---------------------------------------------------------------------
   DetallePedido
   Producto incluido en un pedido. precio_unitario guarda el precio
   vigente al confirmar y no cambia si luego cambia el menú (RN-NEG-04).
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.DetallePedido', N'U') IS NULL
CREATE TABLE dbo.DetallePedido (
    id_detalle       INT IDENTITY(1,1)  NOT NULL,
    id_pedido        INT                NOT NULL,
    id_producto      INT                NOT NULL,
    cantidad         INT                NOT NULL,
    precio_unitario  DECIMAL(10,2)      NOT NULL,
    subtotal         AS (CAST(cantidad * precio_unitario AS DECIMAL(12,2))) PERSISTED,
    CONSTRAINT PK_DetallePedido PRIMARY KEY (id_detalle),
    CONSTRAINT FK_Detalle_Pedido FOREIGN KEY (id_pedido) REFERENCES dbo.Pedido (id_pedido),
    CONSTRAINT FK_Detalle_Producto FOREIGN KEY (id_producto) REFERENCES dbo.Producto (id_producto),
    CONSTRAINT UQ_Detalle_pedido_producto UNIQUE (id_pedido, id_producto),
    CONSTRAINT CK_Detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT CK_Detalle_precio CHECK (precio_unitario > 0)
);
GO

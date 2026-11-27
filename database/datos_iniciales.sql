/* =====================================================================
   Datos de prueba — Cafetería universitaria, Release 1.0
   Coinciden con los ejemplos de los escenarios del backlog
   (Sándwich $3.00, Jugo $2.00, Ensalada, etc.).
   Ejecutar después de schema.sql. Es repetible: no duplica registros.
   ===================================================================== */

USE CafeteriaDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ---------- Productos del menú ---------- */
MERGE dbo.Producto AS destino
USING (VALUES
    ('P001', N'Sándwich',         3.00, 20, 1),
    ('P002', N'Jugo',             2.00, 15, 1),
    ('P003', N'Ensalada',         3.50, 10, 1),
    ('P004', N'Café',             1.25, 30, 1),
    ('P005', N'Empanada',         1.00, 25, 1),
    ('P006', N'Agua',             0.75, 40, 1),
    ('P007', N'Almuerzo del día', 4.50,  0, 1),  -- sin unidades: no debe verse en el menú
    ('P008', N'Brownie',          1.50, 12, 0)   -- no disponible: no debe verse en el menú
) AS origen (codigo, nombre, precio, unidades_disponibles, disponible)
ON destino.codigo = origen.codigo
WHEN NOT MATCHED THEN
    INSERT (codigo, nombre, precio, unidades_disponibles, disponible)
    VALUES (origen.codigo, origen.nombre, origen.precio, origen.unidades_disponibles, origen.disponible);
GO

/* ---------- Clientes de prueba ----------
   Contraseña de ambos clientes: Prueba#2026
   (se guarda solo su hash BCrypt, como exige HU-09). */
MERGE dbo.Cliente AS destino
USING (VALUES
    ('C001', N'Cliente', N'Prueba', '1700000001', '0990000001', N'cliente.prueba@epn.edu.ec', '$2a$10$aj8rWCC3gNXBtRqihgzBguVMP41J3HNlPLiR57mfEWvmr8kOyVMv6'),
    ('C002', N'Docente', N'Prueba', '1700000002', '0990000002', N'docente.prueba@epn.edu.ec', '$2a$10$aj8rWCC3gNXBtRqihgzBguVMP41J3HNlPLiR57mfEWvmr8kOyVMv6')
) AS origen (codigo_cliente, nombre, apellido, cedula, celular, correo, contrasena_hash)
ON destino.codigo_cliente = origen.codigo_cliente
WHEN NOT MATCHED THEN
    INSERT (codigo_cliente, nombre, apellido, cedula, celular, correo, contrasena_hash)
    VALUES (origen.codigo_cliente, origen.nombre, origen.apellido, origen.cedula, origen.celular, origen.correo, origen.contrasena_hash);
GO

-- =====================================================================
-- CARMIR · PROPUESTA de migración para el Módulo 5 (Inventario y Recetas)
-- Este archivo NO se ejecuta solo. Ejecútenlo después de CARMIR_BaseDeDatos.sql
-- (y de V2 si la aplicaron). No borra datos.
--
-- Huecos que se encontraron al construir el inventario:
--  1. La Administradora NO está en la tabla Usuario (decisión del Módulo 7),
--     pero MovimientoInventario.IdUsuario es NOT NULL. Sin este cambio la
--     Administradora no puede registrar compras ni mermas (el Personal sí).
--     Con el cambio, IdUsuario vacío = Administradora o el sistema.
--  2. La vista vw_CostoReceta suma también las recetas INACTIVAS, así que el
--     precio de un postre seguía contando una receta dada de baja. Se vuelve a
--     crear para que solo cuente las recetas activas (RF-44: una receta dada
--     de baja ya no se usa).
--
-- Huecos que el panel resolvió sin cambiar la base de datos:
--  - "Requiere revisión" (RF-36 / RF-41): se calcula al leer la receta
--    (tiene algún insumo dado de baja); no hace falta columna.
--  - "Agotado" (RF-39): alerta de existencia baja con existencia en 0.
--  - Días de aviso de caducidad (RF-40): config/app.properties
--    (inventario.diasAvisoCaducidad).
--  - Rendimiento de la receta (RF-41/42): no hay columna; si lo quieren,
--    agreguen Receta.Rendimiento VARCHAR(50) NULL y se muestra después.
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- 1. Movimientos de la Administradora
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE MovimientoInventario
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que lo registró (relación Captura); vacío = Administradora o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Costo de receta solo con recetas activas
CREATE OR REPLACE VIEW vw_CostoReceta AS
SELECT r.IdReceta, r.IdOpcion, r.NombreReceta,
       COALESCE(SUM(ri.CantidadInsumo * i.CostoUnitario), 0) AS CostoReceta
FROM Receta r
LEFT JOIN RecetaInsumo ri ON ri.IdReceta = r.IdReceta
LEFT JOIN Insumo i ON i.IdInsumo = ri.IdInsumo
WHERE r.EstadoReceta = 'Activa'
GROUP BY r.IdReceta, r.IdOpcion, r.NombreReceta;

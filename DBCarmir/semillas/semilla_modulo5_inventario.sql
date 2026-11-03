-- =====================================================================
-- CARMIR · Datos semilla del Módulo 5 (Inventario y Recetas)
-- OPCIONAL: sirve para probar "Inventario de Insumos" y "Recetas" del panel.
-- Ejecutar DESPUÉS de:
--   1. CARMIR_BaseDeDatos.sql
--   2. semillas/semilla_modulo3_catalogo.sql   (las recetas son de esas opciones)
--   3. migraciones/PROPUESTA_V3__inventario.sql (las compras de la Administradora
--      se guardan con IdUsuario NULL)
-- No cambia ninguna tabla, solo agrega datos. Se puede ejecutar varias veces
-- sin duplicar nada (un insumo solo recibe su compra inicial si no tiene lotes).
--
-- Crea:
--   1. 22 insumos.
--   2. Una compra inicial (lote + movimiento "Compra") por insumo.
--      La fresa caduca en 2 días y la frambuesa queda bajo su stock mínimo,
--      para ver las alertas al abrir el inventario.
--   3. Una receta por cada opción del catálogo semilla, con sus ingredientes.
--      Con esto el catálogo ya calcula el precio de las opciones y productos.
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- 1. Insumos -------------------------------------------------------------
-- (nombre, categoría, unidad, costo por unidad, stock mínimo, perecedero)
INSERT IGNORE INTO Insumo (NombreInsumo, CategoriaInsumo, UnidadMedida, CostoUnitario, StockMinimo, Perecedero) VALUES
  ('Harina de trigo', 'Harinas', 'kg', 22, 5, 0),
  ('Huevo', 'Huevo', 'pieza', 3.2, 30, 1),
  ('Azúcar', 'Endulzantes', 'kg', 26, 3, 0),
  ('Mantequilla', 'Lácteos', 'kg', 160, 1, 1),
  ('Leche entera', 'Lácteos', 'L', 27, 4, 1),
  ('Extracto de vainilla', 'Saborizantes', 'L', 380, 0.1, 0),
  ('Chocolate amargo', 'Chocolates', 'kg', 210, 1, 0),
  ('Leche condensada', 'Lácteos', 'L', 70, 1, 0),
  ('Leche evaporada', 'Lácteos', 'L', 45, 1, 0),
  ('Crema para batir', 'Lácteos', 'L', 95, 1, 1),
  ('Fresa', 'Frutas', 'kg', 70, 1, 1),
  ('Frambuesa', 'Frutas', 'kg', 220, 0.5, 1),
  ('Cajeta', 'Dulces', 'kg', 120, 1, 0),
  ('Moño de listón', 'Decoración', 'pieza', 12, 5, 0),
  ('Listón decorativo', 'Decoración', 'pieza', 8, 5, 0),
  ('Fondant', 'Decoración', 'kg', 180, 0.5, 0),
  ('Colorante vegetal', 'Decoración', 'ml', 0.8, 20, 0),
  ('Vela de cumpleaños', 'Decoración', 'pieza', 2.5, 20, 0),
  ('Grenetina', 'Gelatinas', 'kg', 420, 0.2, 0),
  ('Limón', 'Frutas', 'kg', 35, 1, 1),
  ('Gelatina de sabores', 'Gelatinas', 'kg', 90, 0.5, 0),
  ('Durazno en almíbar', 'Frutas', 'kg', 85, 0.5, 0);

-- 2. Compra inicial de cada insumo ----------------------------------------
-- Primero el movimiento y después el lote; los dos solo si el insumo aún no tiene lotes.
-- Costo de la compra = cantidad × costo por unidad. Caducidad = hoy + días (NULL = no caduca).
INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 12, 'Compra inicial (semilla)', 264 FROM Insumo i
WHERE i.NombreInsumo = 'Harina de trigo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 12, 12, 264, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Harina de trigo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 90, 'Compra inicial (semilla)', 288.0 FROM Insumo i
WHERE i.NombreInsumo = 'Huevo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 90, 90, 288.0, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 14 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Huevo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 10, 'Compra inicial (semilla)', 260 FROM Insumo i
WHERE i.NombreInsumo = 'Azúcar' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 10, 10, 260, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Azúcar' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 3, 'Compra inicial (semilla)', 480 FROM Insumo i
WHERE i.NombreInsumo = 'Mantequilla' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 3, 3, 480, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 20 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Mantequilla' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 12, 'Compra inicial (semilla)', 324 FROM Insumo i
WHERE i.NombreInsumo = 'Leche entera' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 12, 12, 324, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 8 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Leche entera' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 0.5, 'Compra inicial (semilla)', 190.0 FROM Insumo i
WHERE i.NombreInsumo = 'Extracto de vainilla' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 0.5, 0.5, 190.0, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Extracto de vainilla' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 3, 'Compra inicial (semilla)', 630 FROM Insumo i
WHERE i.NombreInsumo = 'Chocolate amargo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 3, 3, 630, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Chocolate amargo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 4, 'Compra inicial (semilla)', 280 FROM Insumo i
WHERE i.NombreInsumo = 'Leche condensada' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 4, 4, 280, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Leche condensada' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 4, 'Compra inicial (semilla)', 180 FROM Insumo i
WHERE i.NombreInsumo = 'Leche evaporada' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 4, 4, 180, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Leche evaporada' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 3, 'Compra inicial (semilla)', 285 FROM Insumo i
WHERE i.NombreInsumo = 'Crema para batir' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 3, 3, 285, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 6 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Crema para batir' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 2, 'Compra inicial (semilla)', 140 FROM Insumo i
WHERE i.NombreInsumo = 'Fresa' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 2, 2, 140, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 2 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Fresa' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 0.3, 'Compra inicial (semilla)', 66.0 FROM Insumo i
WHERE i.NombreInsumo = 'Frambuesa' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 0.3, 0.3, 66.0, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 4 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Frambuesa' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 2, 'Compra inicial (semilla)', 240 FROM Insumo i
WHERE i.NombreInsumo = 'Cajeta' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 2, 2, 240, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Cajeta' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 20, 'Compra inicial (semilla)', 240 FROM Insumo i
WHERE i.NombreInsumo = 'Moño de listón' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 20, 20, 240, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Moño de listón' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 20, 'Compra inicial (semilla)', 160 FROM Insumo i
WHERE i.NombreInsumo = 'Listón decorativo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 20, 20, 160, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Listón decorativo' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 1.5, 'Compra inicial (semilla)', 270.0 FROM Insumo i
WHERE i.NombreInsumo = 'Fondant' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 1.5, 1.5, 270.0, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Fondant' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 120, 'Compra inicial (semilla)', 96.0 FROM Insumo i
WHERE i.NombreInsumo = 'Colorante vegetal' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 120, 120, 96.0, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Colorante vegetal' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 100, 'Compra inicial (semilla)', 250.0 FROM Insumo i
WHERE i.NombreInsumo = 'Vela de cumpleaños' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 100, 100, 250.0, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Vela de cumpleaños' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 1, 'Compra inicial (semilla)', 420 FROM Insumo i
WHERE i.NombreInsumo = 'Grenetina' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 1, 1, 420, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Grenetina' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 3, 'Compra inicial (semilla)', 105 FROM Insumo i
WHERE i.NombreInsumo = 'Limón' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 3, 3, 105, CURDATE(), 'Proveedor de ejemplo', CURDATE() + INTERVAL 10 DAY FROM Insumo i
WHERE i.NombreInsumo = 'Limón' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 2, 'Compra inicial (semilla)', 180 FROM Insumo i
WHERE i.NombreInsumo = 'Gelatina de sabores' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 2, 2, 180, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Gelatina de sabores' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
SELECT i.IdInsumo, NULL, 'Compra', 2, 'Compra inicial (semilla)', 170 FROM Insumo i
WHERE i.NombreInsumo = 'Durazno en almíbar' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);
INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
SELECT i.IdInsumo, 2, 2, 170, CURDATE(), 'Proveedor de ejemplo', NULL FROM Insumo i
WHERE i.NombreInsumo = 'Durazno en almíbar' AND NOT EXISTS (SELECT 1 FROM Lote l WHERE l.IdInsumo = i.IdInsumo);

-- 3. Recetas (una por opción) y sus ingredientes ---------------------------
-- Las cantidades son por una unidad de la opción. Las opciones que se escalan
-- (sabor, relleno, flores) se multiplican por el tamaño al descontar (RF-45).
INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Bizcocho base chico', '1. Precalentar el horno a 180 °C.
2. Batir la mantequilla con el azúcar hasta acremar.
3. Agregar los huevos uno a uno.
4. Incorporar la harina alternando con la leche.
5. Hornear 35 minutos en molde de 18 cm.', 50
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Chico';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Harina de trigo' AS Nombre, 0.3 AS Cantidad UNION ALL SELECT 'Huevo' AS Nombre, 4 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.2 AS Cantidad UNION ALL SELECT 'Mantequilla' AS Nombre, 0.1 AS Cantidad UNION ALL SELECT 'Leche entera' AS Nombre, 0.2 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Chico';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Bizcocho base mediano', '1. Precalentar el horno a 180 °C.
2. Acremar mantequilla y azúcar.
3. Agregar los huevos uno a uno.
4. Incorporar harina y leche.
5. Hornear 45 minutos en molde de 24 cm.', 60
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Harina de trigo' AS Nombre, 0.45 AS Cantidad UNION ALL SELECT 'Huevo' AS Nombre, 6 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.3 AS Cantidad UNION ALL SELECT 'Mantequilla' AS Nombre, 0.15 AS Cantidad UNION ALL SELECT 'Leche entera' AS Nombre, 0.3 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Bizcocho base grande', '1. Precalentar el horno a 180 °C.
2. Acremar mantequilla y azúcar.
3. Agregar los huevos uno a uno.
4. Incorporar harina y leche.
5. Hornear 55 minutos en molde de 30 cm.', 75
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Harina de trigo' AS Nombre, 0.6 AS Cantidad UNION ALL SELECT 'Huevo' AS Nombre, 8 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.4 AS Cantidad UNION ALL SELECT 'Mantequilla' AS Nombre, 0.2 AS Cantidad UNION ALL SELECT 'Leche entera' AS Nombre, 0.4 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Sabor vainilla', '1. Agregar la vainilla a la mezcla del bizcocho antes de hornear.', 5
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Vainilla';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Extracto de vainilla' AS Nombre, 0.01 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.05 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Vainilla';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Sabor chocolate', '1. Derretir el chocolate a baño maría.
2. Incorporarlo a la mezcla del bizcocho antes de hornear.', 15
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Chocolate';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Chocolate amargo' AS Nombre, 0.15 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Chocolate';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Baño tres leches', '1. Mezclar las tres leches.
2. Picar el bizcocho frío y bañarlo poco a poco.
3. Refrigerar 2 horas.', 15
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Tres leches';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Leche condensada' AS Nombre, 0.35 AS Cantidad UNION ALL SELECT 'Leche evaporada' AS Nombre, 0.35 AS Cantidad UNION ALL SELECT 'Crema para batir' AS Nombre, 0.2 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Tres leches';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Relleno de fresa', '1. Lavar y picar la fresa.
2. Cocer con el azúcar 10 minutos.
3. Enfriar antes de rellenar.', 20
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Fresa';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Fresa' AS Nombre, 0.25 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.05 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Fresa';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Relleno de frambuesa', '1. Cocer la frambuesa con el azúcar 10 minutos.
2. Colar si se quiere sin semillas.
3. Enfriar antes de rellenar.', 20
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Frambuesa';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Frambuesa' AS Nombre, 0.2 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.05 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Frambuesa';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Relleno de cajeta', '1. Entibiar la cajeta y untar entre capas.', 5
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Cajeta';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Cajeta' AS Nombre, 0.25 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Cajeta';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Moño de listón', '1. Colocar el moño al frente del pastel.', 5
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Moño';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Moño de listón' AS Nombre, 1 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Moño';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Listón alrededor', '1. Rodear la base del pastel con el listón.', 5
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Listón';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Listón decorativo' AS Nombre, 1 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Listón';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Flores de azúcar', '1. Pintar el fondant con colorante.
2. Formar las flores y dejar secar 30 minutos.
3. Colocarlas sobre el pastel.', 40
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Flores de azúcar';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Fondant' AS Nombre, 0.05 AS Cantidad UNION ALL SELECT 'Colorante vegetal' AS Nombre, 5 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Flores de azúcar';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Velas', '1. Colocar las velas al entregar.', 2
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Velas';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Vela de cumpleaños' AS Nombre, 1 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Pastel' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Velas';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Base de gelatina individual', '1. Hidratar la grenetina.
2. Calentar la leche con el azúcar y disolver la grenetina.
3. Vaciar al molde individual y refrigerar 3 horas.', 20
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Individual';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Grenetina' AS Nombre, 0.01 AS Cantidad UNION ALL SELECT 'Leche entera' AS Nombre, 0.15 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.03 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Individual';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Base de gelatina mediana', '1. Hidratar la grenetina.
2. Calentar la leche con el azúcar y disolver la grenetina.
3. Vaciar al molde mediano y refrigerar 4 horas.', 25
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediana';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Grenetina' AS Nombre, 0.02 AS Cantidad UNION ALL SELECT 'Leche entera' AS Nombre, 0.5 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.1 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediana';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Base de gelatina familiar', '1. Hidratar la grenetina.
2. Calentar la leche con el azúcar y disolver la grenetina.
3. Vaciar al molde familiar y refrigerar 5 horas.', 30
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Familiar';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Grenetina' AS Nombre, 0.04 AS Cantidad UNION ALL SELECT 'Leche entera' AS Nombre, 1 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.2 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Familiar';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Sabor fresa', '1. Licuar la fresa y agregarla a la base tibia.', 10
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Fresa';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Fresa' AS Nombre, 0.1 AS Cantidad UNION ALL SELECT 'Colorante vegetal' AS Nombre, 2 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Fresa';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Sabor limón', '1. Exprimir los limones y agregar el jugo a la base tibia.', 10
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Limón';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Limón' AS Nombre, 0.1 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Limón';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Mosaico de colores', '1. Preparar las gelatinas de colores y cortarlas en cubos.
2. Acomodarlos en el molde antes de vaciar la base.', 40
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Mosaico';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Gelatina de sabores' AS Nombre, 0.12 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Mosaico';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Fruta picada', '1. Picar la fruta y acomodarla encima al desmoldar.', 10
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Fruta picada';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Fresa' AS Nombre, 0.05 AS Cantidad UNION ALL SELECT 'Durazno en almíbar' AS Nombre, 0.1 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Fruta picada';

INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
SELECT o.IdOpcion, 'Crema chantilly', '1. Batir la crema fría con el azúcar.
2. Decorar con duya al desmoldar.', 10
FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Crema chantilly';
INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
SELECT r.IdReceta, i.IdInsumo, x.Cantidad
FROM (SELECT 'Crema para batir' AS Nombre, 0.1 AS Cantidad UNION ALL SELECT 'Azúcar' AS Nombre, 0.02 AS Cantidad) x JOIN Insumo i ON i.NombreInsumo = x.Nombre
JOIN Receta r JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion
JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria WHERE cp.NombreCategoria = 'Gelatina' AND ca.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Crema chantilly';

-- Revisión rápida: costo de cada receta y precio de los productos
SELECT r.NombreReceta, cr.CostoReceta FROM Receta r JOIN vw_CostoReceta cr ON cr.IdReceta = r.IdReceta ORDER BY r.IdOpcion;
SELECT p.NombrePostre, v.CostoIngredientes, v.PrecioVenta FROM Postre p JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre;

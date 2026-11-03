-- =====================================================================
-- CARMIR · Datos semilla del Módulo 1 (Catálogo Público de la página web)
-- OPCIONAL: sirve para probar la página web con productos visibles.
--
-- Ejecutar DESPUÉS de (en este orden):
--   CARMIR_BaseDeDatos.sql → MIGRACION_COMPLETA.sql → semilla_modulo3_catalogo.sql
--   → semilla_modulo5_inventario.sql (sin recetas los precios salen en 0 y el catálogo los oculta)
--
-- Qué hace (no cambia tablas, solo datos; se puede ejecutar varias veces):
--   1. Muestra en la tienda los 3 productos de la semilla del Módulo 3.
--   2. Agrega 4 productos base (plantillas) más, ya visibles en la tienda.
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

SET @pastel = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Pastel');
SET @gelatina = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Gelatina');

-- 1. Los productos de la semilla 3 se hacen visibles -----------------------
UPDATE Postre SET MostrarEnTienda = 1
WHERE NombrePostre IN ('Pastel clásico de vainilla', 'Pastel tres leches de fiesta', 'Gelatina mosaico familiar');

-- 2. Productos base nuevos ------------------------------------------------
-- Cada uno se arma igual que en la semilla 3: primero se juntan sus opciones en
-- tmp_opciones, luego se guarda el Postre con su ClaveConfiguracion y sus PostreOpcion.
DROP TEMPORARY TABLE IF EXISTS tmp_opciones;
CREATE TEMPORARY TABLE tmp_opciones (IdOpcion INT NOT NULL, Cantidad TINYINT NOT NULL);

-- 2.1 Pastel de chocolate con fresa: Mediano + Chocolate + Fresa + Velas
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pastel AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Chocolate')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Fresa')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Velas'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pastel, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pastel de chocolate con fresa', 'Pan de chocolate con mermelada de fresa para 20 personas, listo para festejar con velas', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pastel de chocolate con fresa';
DELETE FROM tmp_opciones;

-- 2.2 Pastel tres leches con cajeta: Chico + Tres leches + Cajeta + Moño
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pastel AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Chico')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Tres leches')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Cajeta')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Moño'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pastel, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pastel tres leches con cajeta', 'Clásico bizcocho bañado en tres leches con cajeta de Celaya, para 10 personas', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pastel tres leches con cajeta';
DELETE FROM tmp_opciones;

-- 2.3 Gelatina de fresa con chantilly: Mediana + Fresa + Crema chantilly
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @gelatina AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediana')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Fresa')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Crema chantilly'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @gelatina, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Gelatina de fresa con chantilly', 'Gelatina de leche sabor fresa con copos de crema batida, 8 porciones', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Gelatina de fresa con chantilly';
DELETE FROM tmp_opciones;

-- 2.4 Gelatina de limón individual: Individual + Limón
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @gelatina AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Individual')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Limón'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @gelatina, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Gelatina de limón individual', 'Gelatina de agua sabor limón, una porción refrescante', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Gelatina de limón individual';

DROP TEMPORARY TABLE tmp_opciones;

-- Resumen: lo que verá el cliente en la página web
SELECT p.IdPostre, p.NombrePostre, c.NombreCategoria, v.PrecioVenta
FROM Postre p
JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria
LEFT JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre
WHERE p.MostrarEnTienda = 1 AND p.NombrePostre IS NOT NULL
ORDER BY c.NombreCategoria, p.NombrePostre;

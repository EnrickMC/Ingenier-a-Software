-- =====================================================================
-- CARMIR · Datos semilla del Módulo 3 (Catálogo de Personalización)
-- OPCIONAL: sirve para probar el apartado "Catálogo de Postres" del panel.
-- Ejecutar DESPUÉS de CARMIR_BaseDeDatos.sql. No cambia ninguna tabla,
-- solo agrega datos. Se puede ejecutar varias veces sin duplicar nada.
--
-- Crea:
--   1. Categorías Pastel y Gelatina.
--   2. Sus características (Tamaño, Sabor, Relleno, Decoración) con reglas.
--   3. Opciones con multiplicadores y puntos de selección.
--   4. Dos exclusiones de ejemplo.
--   5. Tres productos del catálogo.
-- Los precios aparecen cuando las opciones tengan receta (Módulo 5);
-- mientras tanto el panel muestra "Sin receta".
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- 1. Categorías ----------------------------------------------------------
INSERT IGNORE INTO CategoriaPostre (NombreCategoria, DescripcionCategoria) VALUES
  ('Pastel', 'Pasteles personalizados para toda ocasión'),
  ('Gelatina', 'Gelatinas de leche y de agua');

SET @pastel = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Pastel');
SET @gelatina = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Gelatina');

-- 2. Características -----------------------------------------------------
-- (categoría, nombre, descripción, obligatoria, selección múltiple, presupuesto, orden)
INSERT IGNORE INTO Caracteristica
  (IdCategoria, NombreCaracteristica, DescripcionCaracteristica, Obligatoria, SeleccionMultiple, PresupuestoSeleccion, OrdenMostrar)
VALUES
  (@pastel, 'Tamaño', 'Número de personas', 1, 0, 1, 1),
  (@pastel, 'Sabor', 'Sabor del pan', 1, 0, 1, 2),
  (@pastel, 'Relleno', 'Relleno entre capas', 0, 0, 1, 3),
  (@pastel, 'Decoración', 'Adornos; cada uno gasta puntos del presupuesto', 0, 1, 3, 4),
  (@gelatina, 'Tamaño', 'Número de porciones', 1, 0, 1, 1),
  (@gelatina, 'Sabor', 'Sabor de la gelatina', 1, 0, 1, 2),
  (@gelatina, 'Decoración', 'Toppings', 0, 1, 2, 3);

-- 3. Opciones ------------------------------------------------------------
-- Cada bloque agrega las opciones de una característica si todavía no existen.
-- Columnas: nombre, descripción, mult. ingredientes, mult. costo, se escala, puntos, mult. presupuesto
INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Chico' AS n, '10 personas' AS d, 1.00 AS mi, 3.00 AS mc, 0 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Mediano', '20 personas', 1.50, 3.00, 0, 1.00, 1.50
      UNION ALL SELECT 'Grande', '40 personas', 2.00, 3.00, 0, 1.00, 2.00) v
WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Tamaño'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Vainilla' AS n, 'Pan de vainilla' AS d, 1.00 AS mi, 3.00 AS mc, 1 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Chocolate', 'Pan de chocolate', 1.00, 3.00, 1, 1.00, 1.00
      UNION ALL SELECT 'Tres leches', 'Pan bañado en tres leches', 1.00, 3.50, 1, 1.00, 1.00) v
WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Sabor'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Fresa' AS n, 'Mermelada de fresa' AS d, 1.00 AS mi, 3.00 AS mc, 1 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Frambuesa', 'Mermelada de frambuesa', 1.00, 3.00, 1, 1.00, 1.00
      UNION ALL SELECT 'Cajeta', 'Cajeta de Celaya', 1.00, 3.00, 1, 1.00, 1.00) v
WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Relleno'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Moño' AS n, 'Moño de listón' AS d, 1.00 AS mi, 3.00 AS mc, 0 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Listón', 'Listón alrededor del pastel', 1.00, 3.00, 0, 2.00, 1.00
      UNION ALL SELECT 'Flores de azúcar', 'Flores hechas a mano', 1.00, 4.00, 1, 1.00, 1.00
      UNION ALL SELECT 'Velas', 'Juego de velas', 1.00, 2.00, 0, 1.00, 1.00) v
WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Decoración'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Individual' AS n, '1 porción' AS d, 1.00 AS mi, 3.00 AS mc, 0 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Mediana', '8 porciones', 1.50, 3.00, 0, 1.00, 1.00
      UNION ALL SELECT 'Familiar', '16 porciones', 2.00, 3.00, 0, 1.00, 2.00) v
WHERE c.IdCategoria = @gelatina AND c.NombreCaracteristica = 'Tamaño'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Fresa' AS n, 'Gelatina de leche sabor fresa' AS d, 1.00 AS mi, 3.00 AS mc, 1 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Limón', 'Gelatina de agua sabor limón', 1.00, 3.00, 1, 1.00, 1.00
      UNION ALL SELECT 'Mosaico', 'Cubos de colores en gelatina de leche', 1.00, 3.50, 1, 1.00, 1.00) v
WHERE c.IdCategoria = @gelatina AND c.NombreCaracteristica = 'Sabor'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

INSERT INTO OpcionCaracteristica
  (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
SELECT c.IdCaracteristica, v.n, v.d, v.mi, v.mc, v.se, v.p, v.mp
FROM Caracteristica c
JOIN (SELECT 'Fruta picada' AS n, 'Fruta de temporada' AS d, 1.00 AS mi, 3.00 AS mc, 1 AS se, 1.00 AS p, 1.00 AS mp
      UNION ALL SELECT 'Crema chantilly', 'Copos de crema batida', 1.00, 3.00, 1, 1.00, 1.00) v
WHERE c.IdCategoria = @gelatina AND c.NombreCaracteristica = 'Decoración'
  AND NOT EXISTS (SELECT 1 FROM OpcionCaracteristica o WHERE o.IdCaracteristica = c.IdCaracteristica AND o.NombreOpcion = v.n);

-- Variables con los id de las opciones que se usan abajo
SET @tresLeches = (SELECT o.IdOpcion FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
                   WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Tres leches');
SET @frambuesa = (SELECT o.IdOpcion FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
                  WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Frambuesa');
SET @chocolate = (SELECT o.IdOpcion FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
                  WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Chocolate');
SET @cajeta = (SELECT o.IdOpcion FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
               WHERE c.IdCategoria = @pastel AND c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Cajeta');

-- 4. Exclusiones ---------------------------------------------------------
INSERT IGNORE INTO ExclusionOpciones (IdOpcion, IdOpcionExcluida, MotivoExclusion) VALUES
  (@tresLeches, @frambuesa, 'El ácido de la frambuesa corta la leche del pan'),
  (@chocolate, @cajeta, 'La combinación resulta demasiado dulce');

-- 5. Productos del catálogo ---------------------------------------------
-- La ClaveConfiguracion es "IdOpcion:Cantidad" separados por "|", ordenados por IdOpcion
-- (la misma regla que ConfiguracionPostre.claveConfiguracion() en el panel).
DROP TEMPORARY TABLE IF EXISTS tmp_opciones;
CREATE TEMPORARY TABLE tmp_opciones (IdOpcion INT, Cantidad INT);

-- 5.1 Pastel clásico de vainilla: Chico + Vainilla + Fresa + 1 Moño
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pastel AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Chico')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Vainilla')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Fresa')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Moño'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pastel, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pastel clásico de vainilla', 'Pan de vainilla con relleno de fresa y un moño', NULL, 0
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pastel clásico de vainilla';
DELETE FROM tmp_opciones;

-- 5.2 Pastel tres leches de fiesta: Grande + Tres leches + Cajeta + 1 Listón + 2 Flores
INSERT INTO tmp_opciones
SELECT o.IdOpcion, IF(o.NombreOpcion = 'Flores de azúcar', 2, 1)
FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pastel AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Tres leches')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Cajeta')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Listón', 'Flores de azúcar')));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pastel, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pastel tres leches de fiesta', 'Para 40 personas, con cajeta, listón y flores de azúcar', NULL, 0
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pastel tres leches de fiesta';
DELETE FROM tmp_opciones;

-- 5.3 Gelatina mosaico familiar: Familiar + Mosaico + Fruta picada
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @gelatina AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Familiar')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Mosaico')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Fruta picada'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @gelatina, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Gelatina mosaico familiar', '16 porciones con fruta de temporada', NULL, 0
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Gelatina mosaico familiar';

DROP TEMPORARY TABLE tmp_opciones;

-- Resumen
SELECT c.NombreCategoria, COUNT(DISTINCT ca.IdCaracteristica) AS Caracteristicas, COUNT(o.IdOpcion) AS Opciones
FROM CategoriaPostre c
LEFT JOIN Caracteristica ca ON ca.IdCategoria = c.IdCategoria
LEFT JOIN OpcionCaracteristica o ON o.IdCaracteristica = ca.IdCaracteristica
GROUP BY c.NombreCategoria;
SELECT IdPostre, NombrePostre, ClaveConfiguracion FROM Postre WHERE NombrePostre IS NOT NULL;

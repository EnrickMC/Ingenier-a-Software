-- =====================================================================
-- CARMIR · POBLAR LA BASE DE DATOS (datos de prueba para el Panel y la página web)
--
-- Cuándo ejecutarlo: DESPUÉS de crear la estructura, es decir, después de
--   1. CARMIR_BaseDeDatos.sql
--   2. migraciones/MIGRACION_COMPLETA.sql   (con sus Partes 1 a 7)
-- (INSTALACION_COMPLETA.sql ya hace los pasos 1 y 2 y además ejecuta este archivo).
--
-- No borra ni cambia tablas, solo agrega datos. Se puede ejecutar varias veces:
-- lo que ya existe no se duplica. No crea usuarios de MySQL.
--
-- Cómo usarlo en MySQL Workbench (conectado como root):
--   File > Open SQL Script > este archivo, y luego el rayo (Execute All).
--
-- Qué agrega:
--   1. Permisos y 4 usuarios del Personal (la Administradora sigue siendo admin / admin, fuera de la BD):
--        maria / Maria2026   pedidos, agenda, insumos y recetas
--        jose  / Jose2026    agenda, insumos y recetas
--        lucia / Lucia2026   pedidos, validar pagos y datos financieros
--        pedro / Pedro2026   BLOQUEADO (para probar el desbloqueo)
--   2. Catálogo amplio: 7 categorías (Pastel, Gelatina, Cupcakes, Cheesecakes, Pays, Brownies y Donas),
--      43 características (con y sin receta), más de 175 opciones compatibles con el visor 3D,
--      47 reglas de exclusión para evitar combinaciones raras o incompatibles y 3 productos base.
--   3. Inventario: 41 insumos con su compra inicial y una receta activa por cada opción con receta.
--   4. Catálogo de la web: 20 productos visibles en la tienda (plantillas listas para personalizar).
--   5. Negocio: códigos postales de León con envío (37238, 37000, 37160, 37360, 37500)
--      y datos bancarios de prueba (solo si estaban vacíos).
--   6. Pedidos: 11 pedidos en todos los estados con fechas a partir de HOY, pagos en
--      Efectivo y Transferencia (validados, pendientes y rechazados), un comprobante
--      subido como archivo (ComprobanteArchivo), cancelación, baja y notificaciones.
--   7. Reportes: escenario de AGOSTO 2026 con resultados conocidos (ingresos $1,300,
--      gastos $850, utilidad $450). El detalle está en la sección 7.
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;


-- =====================================================================
-- 1. USUARIOS DEL PANEL (Módulo 7)
-- =====================================================================
-- 1. Permisos -----------------------------------------------------------
INSERT IGNORE INTO Permiso (NombrePermiso) VALUES
  ('Gestionar usuarios'),
  ('Gestionar pedidos'),
  ('Ver agenda de producción'),
  ('Validar pagos'),
  ('Gestionar catálogo'),
  ('Gestionar productos'),
  ('Gestionar insumos'),
  ('Gestionar recetas'),
  ('Ver datos financieros'),
  ('Configurar sistema');

-- 2. Usuario de prueba del Personal --------------------------------------
INSERT IGNORE INTO Usuario (NombreCompleto, NombreUsuario, `Contraseña`, EstadoUsuario, IntentosFallidos)
VALUES ('María López (prueba)', 'maria', 'pbkdf2$120000$w4y+r20Zk7UKzJAUPsyCWQ==$R4IrJnYJ9RRMdbRyrxQ2KwWMBuNcnDbq1UxIiIiTDGw=', 'Activo', 0);

-- Permisos de ejemplo: pedidos, agenda, insumos y recetas
INSERT IGNORE INTO UsuarioPermiso (IdUsuario, IdPermiso)
SELECT u.IdUsuario, p.IdPermiso
FROM Usuario u JOIN Permiso p
  ON p.NombrePermiso IN ('Gestionar pedidos', 'Ver agenda de producción', 'Gestionar insumos', 'Gestionar recetas')
WHERE u.NombreUsuario = 'maria';

-- 1.3 Más usuarios del Personal para probar permisos y bloqueos
--     jose  / Jose2026   pastelero: agenda, insumos y recetas
--     lucia / Lucia2026  caja: pedidos, validar pagos y datos financieros
--     pedro / Pedro2026  cuenta BLOQUEADA (3 intentos fallidos) para probar el desbloqueo
INSERT IGNORE INTO Usuario (NombreCompleto, NombreUsuario, `Contraseña`, EstadoUsuario, IntentosFallidos) VALUES
  ('José Hernández (prueba)', 'jose',  'pbkdf2$120000$aLv5e/ELhmYKxK1EDQD6Vw==$NFmhowDycXsBKWv498G8RF80YZsq1d8c+/8ajcj8Bpc=', 'Activo', 0),
  ('Lucía Martínez (prueba)', 'lucia', 'pbkdf2$120000$ofN94TVdEa64r1OFCvbJng==$+eqqBTbIj4aEupMpBMdM3vEzBDddoJ0FRDm7GMFDyRY=', 'Activo', 0),
  ('Pedro Ruiz (prueba)',     'pedro', 'pbkdf2$120000$rs9y9FGiuq8jk4zQA8+VWQ==$Z6aozdAmR0r8yTpV8Mkp+gep5s+hanWKObOyw7wjzQc=', 'Bloqueado', 3);

INSERT IGNORE INTO UsuarioPermiso (IdUsuario, IdPermiso)
SELECT u.IdUsuario, p.IdPermiso
FROM Usuario u JOIN Permiso p
  ON p.NombrePermiso IN ('Ver agenda de producción', 'Gestionar insumos', 'Gestionar recetas')
WHERE u.NombreUsuario = 'jose';

INSERT IGNORE INTO UsuarioPermiso (IdUsuario, IdPermiso)
SELECT u.IdUsuario, p.IdPermiso
FROM Usuario u JOIN Permiso p
  ON p.NombrePermiso IN ('Gestionar pedidos', 'Validar pagos', 'Ver datos financieros')
WHERE u.NombreUsuario = 'lucia';

INSERT IGNORE INTO UsuarioPermiso (IdUsuario, IdPermiso)
SELECT u.IdUsuario, p.IdPermiso
FROM Usuario u JOIN Permiso p
  ON p.NombrePermiso IN ('Gestionar pedidos')
WHERE u.NombreUsuario = 'pedro';


-- =====================================================================
-- 2. CATÁLOGO DE PERSONALIZACIÓN (Módulo 3)
-- =====================================================================
-- 1. Categorías ----------------------------------------------------------
INSERT IGNORE INTO CategoriaPostre (NombreCategoria, DescripcionCategoria) VALUES
  ('Pastel', 'Pasteles personalizados para toda ocasión'),
  ('Gelatina', 'Gelatinas artesanales de leche, de agua y mosaico'),
  ('Cupcakes', 'Cupcakes gourmet decorados en caja para regalar o compartir'),
  ('Cheesecakes', 'Tartas de queso horneadas y frías sobre base crujiente de galleta'),
  ('Pays', 'Pays caseros con masa dorada de mantequilla y rellenos naturales'),
  ('Brownies', 'Brownies húmedos de cacao intenso horneados al día'),
  ('Donas', 'Donas esponjosas glaseadas y decoradas artesanalmente');

SET @pastel      = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Pastel');
SET @gelatina    = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Gelatina');
SET @cupcakes    = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Cupcakes');
SET @cheesecakes = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Cheesecakes');
SET @pays        = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Pays');
SET @brownies    = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Brownies');
SET @donas       = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Donas');

-- 2. Características -----------------------------------------------------
-- (categoría, nombre, descripción, obligatoria, selección múltiple, presupuesto, orden, permite receta)
INSERT IGNORE INTO Caracteristica
  (IdCategoria, NombreCaracteristica, DescripcionCaracteristica, Obligatoria, SeleccionMultiple, PresupuestoSeleccion, OrdenMostrar, PermiteReceta)
VALUES
  -- Pastel (10 características; Tamaño es la #1 y Tarjeta de regalo la #10)
  (@pastel, 'Tamaño', 'Número aproximado de personas', 1, 0, 1, 1, 1),
  (@pastel, 'Pisos', 'Niveles del pastel; con 2 o más permite personalizar por capa', 0, 0, 1, 2, 1),
  (@pastel, 'Forma', 'Figura geométrica del molde', 0, 0, 1, 3, 0),
  (@pastel, 'Sabor', 'Sabor del bizcocho', 1, 0, 1, 4, 1),
  (@pastel, 'Relleno', 'Relleno entre capas de pan', 0, 0, 1, 5, 1),
  (@pastel, 'Cobertura', 'Acabado exterior del pastel', 0, 0, 1, 6, 1),
  (@pastel, 'Color de cobertura', 'Tono principal del acabado exterior', 0, 0, 1, 7, 0),
  (@pastel, 'Decoración', 'Adornos; cada uno consume puntos del presupuesto disponible', 0, 1, 3, 8, 1),
  (@pastel, 'Base', 'Plato, disco o pedestal donde se entrega el pastel', 0, 0, 1, 9, 1),
  (@pastel, 'Tarjeta de regalo', 'Tarjeta impresa que acompaña al postre (de cortesía)', 0, 0, 1, 10, 0),

  -- Gelatina (6 características)
  (@gelatina, 'Tamaño', 'Número de porciones', 1, 0, 1, 1, 1),
  (@gelatina, 'Forma', 'Figura del molde de gelatina', 0, 0, 1, 2, 0),
  (@gelatina, 'Sabor', 'Sabor principal de la gelatina', 1, 0, 1, 3, 1),
  (@gelatina, 'Relleno', 'Fruta o centro cremoso encapsulado', 0, 0, 1, 4, 1),
  (@gelatina, 'Decoración', 'Toppings frescos; limitados por presupuesto', 0, 1, 2, 5, 1),
  (@gelatina, 'Base', 'Charola o base resistente a humedad', 0, 0, 1, 6, 1),

  -- Cupcakes (6 características)
  (@cupcakes, 'Tamaño', 'Cantidad y formato de la caja de cupcakes', 1, 0, 1, 1, 1),
  (@cupcakes, 'Sabor', 'Sabor del panqué individual', 1, 0, 1, 2, 1),
  (@cupcakes, 'Relleno', 'Centro dulce inyectado en el cupcake', 0, 0, 1, 3, 1),
  (@cupcakes, 'Frosting', 'Copete decorativo superior', 1, 0, 1, 4, 1),
  (@cupcakes, 'Color de cobertura', 'Tono del copete de frosting', 0, 0, 1, 5, 0),
  (@cupcakes, 'Decoración', 'Detalles finales sobre cada cupcake', 0, 1, 2, 6, 1),

  -- Cheesecakes (6 características)
  (@cheesecakes, 'Tamaño', 'Rendimiento en rebanadas', 1, 0, 1, 1, 1),
  (@cheesecakes, 'Forma', 'Molde desmontable de presentación', 0, 0, 1, 2, 0),
  (@cheesecakes, 'Sabor', 'Variedad de la mezcla de queso', 1, 0, 1, 3, 1),
  (@cheesecakes, 'Cobertura', 'Capa brillante o cremosa en la superficie', 0, 0, 1, 4, 1),
  (@cheesecakes, 'Decoración', 'Frutas y detalles superiores', 0, 1, 2, 5, 1),
  (@cheesecakes, 'Base', 'Base de presentación', 0, 0, 1, 6, 1),

  -- Pays (5 características)
  (@pays, 'Tamaño', 'Rendimiento aproximado del pay', 1, 0, 1, 1, 1),
  (@pays, 'Forma', 'Figura del molde para hornear', 0, 0, 1, 2, 0),
  (@pays, 'Sabor', 'Relleno horneado o frío del pay', 1, 0, 1, 3, 1),
  (@pays, 'Cobertura', 'Acabado superior sobre el relleno', 0, 0, 1, 4, 1),
  (@pays, 'Decoración', 'Toques decorativos finales', 0, 1, 2, 5, 1),

  -- Brownies (5 características)
  (@brownies, 'Tamaño', 'Presentación de la caja o charola', 1, 0, 1, 1, 1),
  (@brownies, 'Sabor', 'Variedad del brownie de cacao', 1, 0, 1, 2, 1),
  (@brownies, 'Cobertura', 'Cubierta sobre los cuadros de brownie', 0, 0, 1, 3, 1),
  (@brownies, 'Decoración', 'Complementos superiores', 0, 1, 2, 4, 1),
  (@brownies, 'Base', 'Charola o tabla de entrega', 0, 0, 1, 5, 1),

  -- Donas (5 características)
  (@donas, 'Tamaño', 'Cantidad de piezas por caja', 1, 0, 1, 1, 1),
  (@donas, 'Sabor', 'Sabor de la masa esponjosa', 1, 0, 1, 2, 1),
  (@donas, 'Cobertura', 'Glaseado o cubierta superior', 1, 0, 1, 3, 1),
  (@donas, 'Color de cobertura', 'Tono del glaseado', 0, 0, 1, 4, 0),
  (@donas, 'Decoración', 'Toppings sobre el glaseado fresco', 0, 1, 2, 5, 1);

-- Procedimiento auxiliar para insertar opciones sin duplicarlas
DROP PROCEDURE IF EXISTS carmir_poblar_opcion;
DELIMITER $$
CREATE PROCEDURE carmir_poblar_opcion(
  IN p_cat INT,
  IN p_car VARCHAR(50),
  IN p_nom VARCHAR(60),
  IN p_desc VARCHAR(255),
  IN p_mi DECIMAL(4,2),
  IN p_mc DECIMAL(4,2),
  IN p_se TINYINT,
  IN p_pts DECIMAL(6,2),
  IN p_mp DECIMAL(4,2)
)
BEGIN
  DECLARE v_car INT DEFAULT NULL;
  SELECT IdCaracteristica INTO v_car
  FROM Caracteristica
  WHERE IdCategoria = p_cat AND NombreCaracteristica = p_car
  LIMIT 1;

  IF v_car IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM OpcionCaracteristica WHERE IdCaracteristica = v_car AND NombreOpcion = p_nom
  ) THEN
    INSERT INTO OpcionCaracteristica
      (IdCaracteristica, NombreOpcion, DescripcionOpcion, MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, PuntosSeleccion, MultiplicadorPresupuesto)
    VALUES
      (v_car, p_nom, p_desc, p_mi, p_mc, p_se, p_pts, p_mp);
  END IF;
END$$
DELIMITER ;

-- 3. Opciones ------------------------------------------------------------
-- 3.1 Categoría Pastel (se insertan primero las opciones clásicas para conservar orden e IDs)
CALL carmir_poblar_opcion(@pastel, 'Tamaño', 'Chico', '10 personas', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Tamaño', 'Mediano', '20 personas', 1.50, 3.00, 0, 1.00, 1.50);
CALL carmir_poblar_opcion(@pastel, 'Tamaño', 'Grande', '40 personas', 2.00, 3.00, 0, 1.00, 2.00);
CALL carmir_poblar_opcion(@pastel, 'Tamaño', 'Extra grande', '60 personas para eventos especiales', 2.80, 3.00, 0, 1.00, 2.50);

CALL carmir_poblar_opcion(@pastel, 'Pisos', '1 piso', 'Un nivel tradicional', 1.00, 2.50, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Pisos', '2 pisos', 'Dos niveles con soporte interno; personalizable por capa', 1.80, 3.00, 0, 1.00, 1.50);
CALL carmir_poblar_opcion(@pastel, 'Pisos', '3 pisos', 'Tres niveles monumentales; personalizable por capa', 2.60, 3.20, 0, 1.00, 2.00);

CALL carmir_poblar_opcion(@pastel, 'Forma', 'Redondo', 'Molde circular clásico', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Forma', 'Cuadrado', 'Molde cuadrado de líneas rectas', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Forma', 'Corazón', 'Molde romántico en figura de corazón', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Forma', 'Hexágono', 'Molde geométrico de seis lados', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Forma', 'Flor', 'Molde contorneado de pétalos', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Vainilla', 'Pan de vainilla', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Chocolate', 'Pan de chocolate', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Tres leches', 'Pan bañado en tres leches', 1.00, 3.50, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Red velvet', 'Bizcocho terciopelo rojo con ligero toque de cacao', 1.00, 3.40, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Zanahoria', 'Pan especiado con zanahoria fresca, canela y nuez', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Limón', 'Bizcocho esponjoso con ralladura cítrica natural', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Café', 'Bizcocho aromático infusionado con espresso', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Sabor', 'Oreo', 'Bizcocho vainilla con trozos de galleta oscura', 1.00, 3.30, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Fresa', 'Mermelada de fresa', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Frambuesa', 'Mermelada de frambuesa', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Cajeta', 'Cajeta de Celaya', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Nutella', 'Crema untable de avellana tostada y cacao', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Queso crema', 'Relleno suave de queso crema batido', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Crema pastelera', 'Crema pastelera artesanal a la vainilla', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Relleno', 'Durazno', 'Cubos de durazno en almíbar con crema batida', 1.00, 3.00, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Betún de la casa', 'Betún cremoso tradicional', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Buttercream', 'Crema de mantequilla peinada en franjas suaves', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Chantilly', 'Crema batida ligera y fresca', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Fondant liso', 'Cubierta de pasta de azúcar de acabado impecable', 1.00, 3.60, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Ganache de chocolate', 'Cubierta brillante de chocolate semiamargo', 1.00, 3.40, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Naked cake', 'Acabado semidesnudo que deja lucir las capas de pan', 1.00, 2.80, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Mármol', 'Betún blanco con vetas marmoleadas', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Cobertura', 'Degradado', 'Betún difuminado de color intenso abajo a blanco arriba', 1.00, 3.30, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Blanco', 'Tono blanco marfil clásico', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Rosa pastel', 'Tono rosa suave', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Azul cielo', 'Tono celeste pastel', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Verde menta', 'Tono menta fresco', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Lila', 'Tono lavanda suave', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Amarillo', 'Tono amarillo luminoso', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Dorado', 'Acabado festivo tono oro', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Rojo', 'Tono rojo intenso', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Color de cobertura', 'Chocolate', 'Tono cacao profundo', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Moño', 'Moño de listón', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Listón', 'Listón alrededor del pastel', 1.00, 3.00, 0, 2.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Flores de azúcar', 'Flores hechas a mano', 1.00, 4.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Velas', 'Juego de velas', 1.00, 2.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Fresas', 'Fresas frescas seleccionadas', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Frutos rojos', 'Zarzamoras y frambuesas frescas', 1.00, 3.20, 1, 1.50, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Perlas', 'Perlas comestibles nacaradas', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Chispas', 'Chispas confitadas de colores', 1.00, 2.80, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Goteo de chocolate', 'Drip de ganache por el contorno superior', 1.00, 3.20, 1, 1.50, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Rosetas de crema', 'Corona de rosetas hechas con duya', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Decoración', 'Hoja de oro', 'Laminillas de oro comestible', 1.00, 3.50, 0, 2.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Base', 'Plato blanco', 'Base rígida blanca clásica', 1.00, 2.50, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Base', 'Base dorada', 'Disco rígido acabado dorado', 1.00, 2.80, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Base', 'Base plateada', 'Disco rígido acabado plateado', 1.00, 2.80, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Base', 'Tabla de madera', 'Base rústica de madera para presentación', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Base', 'Pedestal de cristal', 'Base elevada tipo pedestal de exhibición', 1.00, 3.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@pastel, 'Tarjeta de regalo', 'Cumpleaños', 'Tarjeta con globos y «Feliz cumpleaños»', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Tarjeta de regalo', 'Aniversario', 'Tarjeta con corazones', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Tarjeta de regalo', 'Graduación', 'Tarjeta conmemorativa con birrete', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pastel, 'Tarjeta de regalo', 'Felicidades', 'Tarjeta sencilla para cualquier ocasión', 1.00, 1.00, 0, 1.00, 1.00);

-- 3.2 Categoría Gelatina
CALL carmir_poblar_opcion(@gelatina, 'Tamaño', 'Individual', '1 porción', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Tamaño', 'Mediana', '8 porciones', 1.50, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Tamaño', 'Familiar', '16 porciones', 2.00, 3.00, 0, 1.00, 2.00);
CALL carmir_poblar_opcion(@gelatina, 'Tamaño', 'Extra grande', '26 porciones para reunión familiar', 2.80, 3.00, 0, 1.00, 2.50);

CALL carmir_poblar_opcion(@gelatina, 'Forma', 'Molde de gajos', 'Molde estriado tradicional', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Forma', 'Rosca', 'Molde de corona con centro abierto', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Forma', 'Domo', 'Molde semiesférico liso', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Forma', 'Corazón', 'Molde romántico de corazón', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Forma', 'Flor', 'Molde floral contorneado', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Fresa', 'Gelatina de leche sabor fresa', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Limón', 'Gelatina de agua sabor limón', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Mosaico', 'Cubos de colores en gelatina de leche', 1.00, 3.50, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Mango', 'Gelatina elaborada con pulpa fresca de mango', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Rompope', 'Gelatina cremosa de leche al rompope', 1.00, 3.40, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Coco', 'Gelatina cremosa de coco natural', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Sabor', 'Uva', 'Gelatina cristalina de agua sabor uva', 1.00, 3.00, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@gelatina, 'Relleno', 'Durazno', 'Trozos de durazno encapsulados', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Relleno', 'Fresas', 'Fresas frescas encapsuladas', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Relleno', 'Queso crema', 'Centro suave de queso crema', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Relleno', 'Nuez', 'Trozos selectos de nuez pecana', 1.00, 3.30, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@gelatina, 'Decoración', 'Fruta picada', 'Fruta de temporada', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Decoración', 'Crema chantilly', 'Copos de crema batida', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Decoración', 'Frutos rojos', 'Frambuesas y zarzamoras naturales', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Decoración', 'Perlas', 'Perlas dulces comestibles', 1.00, 3.00, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@gelatina, 'Base', 'Plato blanco', 'Charola blanca rígida', 1.00, 2.50, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Base', 'Base dorada', 'Base dorada impermeable', 1.00, 2.80, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@gelatina, 'Base', 'Base plateada', 'Base plateada impermeable', 1.00, 2.80, 0, 1.00, 1.00);

-- 3.3 Categoría Cupcakes
CALL carmir_poblar_opcion(@cupcakes, 'Tamaño', 'Mini', 'Caja de 12 mini cupcakes de bocado', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Tamaño', 'Mediano', 'Caja de 6 cupcakes tamaño clásico', 1.20, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Tamaño', 'Grande', 'Docena de 12 cupcakes tamaño clásico', 2.00, 3.00, 0, 1.00, 1.50);
CALL carmir_poblar_opcion(@cupcakes, 'Tamaño', 'Jumbo', 'Caja de 6 cupcakes jumbo especiales', 1.80, 3.10, 0, 1.00, 1.50);

CALL carmir_poblar_opcion(@cupcakes, 'Sabor', 'Vainilla', 'Panqué esponjoso de vainilla', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Sabor', 'Chocolate', 'Panqué húmedo de cacao intenso', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Sabor', 'Red velvet', 'Panqué terciopelo rojo artesanal', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Sabor', 'Limón', 'Panqué cítrico con ralladura de limón', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Sabor', 'Zanahoria', 'Panqué especiado con zanahoria y canela', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Sabor', 'Oreo', 'Panqué con trozos de galleta oscura', 1.00, 3.20, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@cupcakes, 'Relleno', 'Mermelada de fresa', 'Corazón frutal de fresa cocida en casa', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Relleno', 'Nutella', 'Centro cremoso de avellana y cacao', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Relleno', 'Cajeta', 'Centro de cajeta tradicional', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Relleno', 'Queso crema', 'Centro suave de queso batido', 1.00, 3.10, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@cupcakes, 'Frosting', 'Remolino de betún', 'Espiral alto de betún de mantequilla', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Frosting', 'Remolino de chantilly', 'Copete aireado de crema batida', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Frosting', 'Remolino de queso crema', 'Espiral cremoso de queso suave', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Frosting', 'Ganache de chocolate', 'Copete brillante de ganache semiamargo', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Frosting', 'Domo de fondant', 'Cubierta lisa semiesférica de fondant', 1.00, 3.40, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@cupcakes, 'Color de cobertura', 'Blanco', 'Blanco natural', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Color de cobertura', 'Rosa pastel', 'Rosa delicado', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Color de cobertura', 'Azul cielo', 'Celeste suave', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Color de cobertura', 'Lila', 'Lavanda pastel', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Color de cobertura', 'Verde menta', 'Verde menta suave', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Color de cobertura', 'Amarillo', 'Amarillo vainilla', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@cupcakes, 'Decoración', 'Chispas', 'Chispas de colores sobre el copete', 1.00, 2.80, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Decoración', 'Perlas', 'Perlas de azúcar nacaradas', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Decoración', 'Fresas', 'Media fresa fresca coronando el cupcake', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Decoración', 'Flores de azúcar', 'Pequeñas flores modeladas a mano', 1.00, 3.50, 1, 1.50, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Decoración', 'Hoja de oro', 'Destellos de oro comestible', 1.00, 3.50, 0, 2.00, 1.00);
CALL carmir_poblar_opcion(@cupcakes, 'Decoración', 'Velas', 'Vela individual para festejo', 1.00, 2.00, 0, 1.00, 1.00);

-- 3.4 Categoría Cheesecakes
CALL carmir_poblar_opcion(@cheesecakes, 'Tamaño', 'Chico', '8 rebanadas', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Tamaño', 'Mediano', '14 rebanadas', 1.50, 3.00, 0, 1.00, 1.50);
CALL carmir_poblar_opcion(@cheesecakes, 'Tamaño', 'Grande', '22 rebanadas', 2.00, 3.00, 0, 1.00, 2.00);

CALL carmir_poblar_opcion(@cheesecakes, 'Forma', 'Redondo', 'Molde circular clásico', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Forma', 'Cuadrado', 'Corte cuadrado contemporáneo', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Forma', 'Corazón', 'Molde especial de corazón', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@cheesecakes, 'Sabor', 'Queso', 'Estilo Nueva York horneado lentamente', 1.00, 3.10, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Sabor', 'Frutos rojos', 'Queso cremoso con compota de frutos del bosque', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Sabor', 'Oreo', 'Mezcla de queso con trozos de galleta oscura', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Sabor', 'Cajeta', 'Cheesecake horneado al dulce de leche', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Sabor', 'Mango', 'Cheesecake frío con pulpa de mango natural', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Sabor', 'Limón', 'Cheesecake refrescante al limón real', 1.00, 3.00, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@cheesecakes, 'Cobertura', 'Glaseado', 'Espejo frutal brillante sobre el queso', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Cobertura', 'Ganache de chocolate', 'Capa intensa de ganache semiamargo', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Cobertura', 'Chantilly', 'Acabado suave de crema batida', 1.00, 3.00, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@cheesecakes, 'Decoración', 'Fresas', 'Fresas frescas fileteadas', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Decoración', 'Frutos rojos', 'Zarzamoras y frambuesas enteras', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Decoración', 'Rosetas de crema', 'Borde decorado con rosetas de duya', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Decoración', 'Hoja de oro', 'Toque de lámina dorada comestible', 1.00, 3.50, 0, 2.00, 1.00);

CALL carmir_poblar_opcion(@cheesecakes, 'Base', 'Plato blanco', 'Plato cerámico o rígido blanco', 1.00, 2.50, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Base', 'Base dorada', 'Disco dorado de presentación', 1.00, 2.80, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@cheesecakes, 'Base', 'Tabla de madera', 'Base rústica de madera', 1.00, 3.00, 0, 1.00, 1.00);

-- 3.5 Categoría Pays
CALL carmir_poblar_opcion(@pays, 'Tamaño', 'Chico', '6 rebanadas', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Tamaño', 'Mediano', '10 rebanadas', 1.40, 3.00, 0, 1.00, 1.50);
CALL carmir_poblar_opcion(@pays, 'Tamaño', 'Grande', '16 rebanadas', 1.90, 3.00, 0, 1.00, 2.00);

CALL carmir_poblar_opcion(@pays, 'Forma', 'Redondo', 'Molde circular ondulado tradicional', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Forma', 'Cuadrado', 'Molde cuadrado rústico', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@pays, 'Sabor', 'Nuez', 'Pay tradicional de nuez pecana caramelizada', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Sabor', 'Manzana con canela', 'Compota horneada de manzana fresca y canela', 1.00, 3.10, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Sabor', 'Limón', 'Pay frío y cremoso de limón verde', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Sabor', 'Piña', 'Relleno dulce de piña natural cocida', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Sabor', 'Queso', 'Pay casero horneado de queso suave', 1.00, 3.10, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@pays, 'Cobertura', 'Chantilly', 'Capa suave de crema batida', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Cobertura', 'Betún de la casa', 'Merengue dorado ligeramente flameado', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Cobertura', 'Glaseado', 'Brillo ligero artesanal sobre el relleno', 1.00, 2.80, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@pays, 'Decoración', 'Fruta picada', 'Fruta fresca seleccionada', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Decoración', 'Fresas', 'Fresas enteras brillantes', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Decoración', 'Rosetas de crema', 'Corona perimetral de rosetas', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@pays, 'Decoración', 'Perlas', 'Perlas dulces decorativas', 1.00, 3.00, 1, 1.00, 1.00);

-- 3.6 Categoría Brownies
CALL carmir_poblar_opcion(@brownies, 'Tamaño', 'Mini', 'Caja de 9 cuadritos de bocado', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Tamaño', 'Mediano', 'Caja de 6 brownies clásicos', 1.40, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Tamaño', 'Grande', 'Caja de 12 brownies clásicos', 2.30, 3.00, 0, 1.00, 1.50);
CALL carmir_poblar_opcion(@brownies, 'Tamaño', 'Jumbo', 'Charola familiar de fiesta', 2.80, 3.00, 0, 1.00, 2.00);

CALL carmir_poblar_opcion(@brownies, 'Sabor', 'Chocolate', 'Cacao amargo estilo fudge húmedo', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Sabor', 'Nutella', 'Brownie marmoleado con crema de avellana', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Sabor', 'Nuez', 'Brownie horneado con trozos de nuez pecana', 1.00, 3.30, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Sabor', 'Oreo', 'Brownie con trozos crujientes de galleta', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Sabor', 'Cajeta', 'Brownie con remolino de cajeta de Celaya', 1.00, 3.20, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@brownies, 'Cobertura', 'Ganache de chocolate', 'Cubierta espesa de ganache semiamargo', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Cobertura', 'Glaseado', 'Hilos finos de glaseado dulce', 1.00, 2.80, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Cobertura', 'Mármol', 'Cubierta veteada bicolor', 1.00, 3.20, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@brownies, 'Decoración', 'Chispas', 'Chispas de colores confitadas', 1.00, 2.80, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Decoración', 'Fresas', 'Fresas frescas sobre la charola', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Decoración', 'Frutos rojos', 'Frambuesas y zarzamoras frescas', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Decoración', 'Hoja de oro', 'Detalles en lámina de oro comestible', 1.00, 3.50, 0, 2.00, 1.00);

CALL carmir_poblar_opcion(@brownies, 'Base', 'Plato blanco', 'Caja o base blanca de presentación', 1.00, 2.50, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Base', 'Tabla de madera', 'Tabla rústica de madera', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@brownies, 'Base', 'Pizarra', 'Charola negra tipo piedra pizarra', 1.00, 3.00, 0, 1.00, 1.00);

-- 3.7 Categoría Donas
CALL carmir_poblar_opcion(@donas, 'Tamaño', 'Mini', 'Caja de 12 mini donas', 1.00, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Tamaño', 'Mediano', 'Media docena (6 donas clásicas)', 1.30, 3.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Tamaño', 'Grande', 'Docena completa (12 donas clásicas)', 2.20, 3.00, 0, 1.00, 1.50);

CALL carmir_poblar_opcion(@donas, 'Sabor', 'Vainilla', 'Masa brioche clásica a la vainilla', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Sabor', 'Chocolate', 'Masa esponjosa de cacao', 1.00, 3.10, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Sabor', 'Canela', 'Masa aromática con canela recién molida', 1.00, 3.00, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@donas, 'Cobertura', 'Glaseado', 'Glaseado brillante clásico', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Cobertura', 'Ganache de chocolate', 'Cobertura intensa de chocolate semiamargo', 1.00, 3.20, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Cobertura', 'Mármol', 'Glaseado artístico marmoleado', 1.00, 3.20, 1, 1.00, 1.00);

CALL carmir_poblar_opcion(@donas, 'Color de cobertura', 'Blanco', 'Glaseado blanco perlado', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Color de cobertura', 'Rosa', 'Glaseado rosa clásico', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Color de cobertura', 'Azul cielo', 'Glaseado celeste suave', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Color de cobertura', 'Lila', 'Glaseado lavanda', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Color de cobertura', 'Amarillo', 'Glaseado amarillo alegre', 1.00, 1.00, 0, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Color de cobertura', 'Chocolate', 'Tono cacao intenso', 1.00, 1.00, 0, 1.00, 1.00);

CALL carmir_poblar_opcion(@donas, 'Decoración', 'Chispas', 'Lluvia de chispas de colores', 1.00, 2.80, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Decoración', 'Perlas', 'Perlas crujientes de azúcar', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Decoración', 'Fresas', 'Trozos frescos de fresa natural', 1.00, 3.00, 1, 1.00, 1.00);
CALL carmir_poblar_opcion(@donas, 'Decoración', 'Hoja de oro', 'Toque gourmet de oro comestible', 1.00, 3.50, 0, 2.00, 1.00);

DROP PROCEDURE IF EXISTS carmir_poblar_opcion;

-- 4. Exclusiones (47 reglas para evitar combinaciones raras o inviables) ---
DROP PROCEDURE IF EXISTS carmir_excluir;
DELIMITER $$
CREATE PROCEDURE carmir_excluir(
  IN p_cat INT,
  IN p_car1 VARCHAR(50),
  IN p_opc1 VARCHAR(60),
  IN p_car2 VARCHAR(50),
  IN p_opc2 VARCHAR(60),
  IN p_motivo VARCHAR(255)
)
BEGIN
  DECLARE v_a INT DEFAULT NULL;
  DECLARE v_b INT DEFAULT NULL;

  SELECT o.IdOpcion INTO v_a
  FROM OpcionCaracteristica o
  JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
  WHERE c.IdCategoria = p_cat AND c.NombreCaracteristica = p_car1 AND o.NombreOpcion = p_opc1
  LIMIT 1;

  SELECT o.IdOpcion INTO v_b
  FROM OpcionCaracteristica o
  JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
  WHERE c.IdCategoria = p_cat AND c.NombreCaracteristica = p_car2 AND o.NombreOpcion = p_opc2
  LIMIT 1;

  IF v_a IS NOT NULL AND v_b IS NOT NULL AND v_a <> v_b AND NOT EXISTS (
    SELECT 1 FROM ExclusionOpciones e
    WHERE (e.IdOpcion = v_a AND e.IdOpcionExcluida = v_b)
       OR (e.IdOpcion = v_b AND e.IdOpcionExcluida = v_a)
  ) THEN
    INSERT INTO ExclusionOpciones (IdOpcion, IdOpcionExcluida, MotivoExclusion)
    VALUES (v_a, v_b, p_motivo);
  END IF;
END$$
DELIMITER ;

-- 4.1 Exclusiones en Pastel (humedad, peso estructural, choque de sabores y color sobre ganache)
CALL carmir_excluir(@pastel, 'Sabor', 'Tres leches', 'Relleno', 'Frambuesa', 'El ácido de la frambuesa corta la leche del pan');
CALL carmir_excluir(@pastel, 'Sabor', 'Chocolate', 'Relleno', 'Cajeta', 'La combinación resulta demasiado dulce');
CALL carmir_excluir(@pastel, 'Sabor', 'Tres leches', 'Cobertura', 'Fondant liso', 'La humedad de las tres leches ablanda y agrieta el fondant');
CALL carmir_excluir(@pastel, 'Sabor', 'Tres leches', 'Cobertura', 'Naked cake', 'El pan bañado en tres leches requiere cobertura completa para retener el jarabe');
CALL carmir_excluir(@pastel, 'Sabor', 'Tres leches', 'Pisos', '3 pisos', 'El peso de tres niveles comprime el bizcocho húmedo de tres leches');
CALL carmir_excluir(@pastel, 'Tamaño', 'Chico', 'Pisos', '2 pisos', 'El diámetro chico no tiene superficie suficiente para sostener dos niveles');
CALL carmir_excluir(@pastel, 'Tamaño', 'Chico', 'Pisos', '3 pisos', 'El tamaño chico no soporta una estructura de tres niveles');
CALL carmir_excluir(@pastel, 'Tamaño', 'Mediano', 'Pisos', '3 pisos', 'Tres niveles requieren al menos tamaño grande para mantener estabilidad');
CALL carmir_excluir(@pastel, 'Forma', 'Flor', 'Pisos', '3 pisos', 'El molde de pétalos pierde definición estructural en tres niveles');
CALL carmir_excluir(@pastel, 'Sabor', 'Limón', 'Relleno', 'Cajeta', 'El perfil cítrico del limón choca con el dulzor intenso de la cajeta');
CALL carmir_excluir(@pastel, 'Sabor', 'Limón', 'Relleno', 'Nutella', 'La acidez del limón opaca el sabor tostado de la avellana');
CALL carmir_excluir(@pastel, 'Sabor', 'Café', 'Relleno', 'Fresa', 'La acidez frutal de la fresa desentona con el tueste amargo del café');
CALL carmir_excluir(@pastel, 'Sabor', 'Café', 'Relleno', 'Frambuesa', 'La frambuesa ácida no armoniza con el sabor espresso del bizcocho');
CALL carmir_excluir(@pastel, 'Sabor', 'Zanahoria', 'Cobertura', 'Ganache de chocolate', 'El cacao amargo cubre por completo las especias suaves del pan de zanahoria');
CALL carmir_excluir(@pastel, 'Cobertura', 'Ganache de chocolate', 'Color de cobertura', 'Blanco', 'El ganache oscuro natural no puede teñirse en tono blanco');
CALL carmir_excluir(@pastel, 'Cobertura', 'Ganache de chocolate', 'Color de cobertura', 'Rosa pastel', 'La base oscura de cacao impide obtener colores pastel claros');
CALL carmir_excluir(@pastel, 'Cobertura', 'Ganache de chocolate', 'Color de cobertura', 'Azul cielo', 'El cacao oscuro del ganache no permite teñir en azul claro');
CALL carmir_excluir(@pastel, 'Cobertura', 'Ganache de chocolate', 'Color de cobertura', 'Amarillo', 'El color oscuro del ganache impide lograr un tono amarillo limpio');
CALL carmir_excluir(@pastel, 'Cobertura', 'Naked cake', 'Decoración', 'Listón', 'El listón decorativo se mancha al tocar las paredes sin cobertura completa');
CALL carmir_excluir(@pastel, 'Cobertura', 'Chantilly', 'Decoración', 'Flores de azúcar', 'La humedad de la crema chantilly ablanda los pétalos de azúcar');
CALL carmir_excluir(@pastel, 'Tamaño', 'Extra grande', 'Base', 'Pedestal de cristal', 'El peso de 60 porciones supera la resistencia segura del pedestal de cristal');

-- 4.2 Exclusiones en Gelatina (química agua/lácteos y desmolde)
CALL carmir_excluir(@gelatina, 'Sabor', 'Limón', 'Decoración', 'Crema chantilly', 'El cítrico de la gelatina de agua corta los copos de crema batida');
CALL carmir_excluir(@gelatina, 'Sabor', 'Limón', 'Relleno', 'Queso crema', 'La gelatina transparente de limón pierde firmeza con centro lácteo de queso');
CALL carmir_excluir(@gelatina, 'Sabor', 'Uva', 'Relleno', 'Nuez', 'La nuez suelta taninos y enturbia la gelatina cristalina de uva');
CALL carmir_excluir(@gelatina, 'Sabor', 'Uva', 'Relleno', 'Queso crema', 'El centro cremoso de queso se separa de la base acuosa de uva');
CALL carmir_excluir(@gelatina, 'Tamaño', 'Individual', 'Forma', 'Rosca', 'El molde de rosca solo está disponible para porciones medianas o mayores');
CALL carmir_excluir(@gelatina, 'Tamaño', 'Individual', 'Sabor', 'Mosaico', 'Los cubos de mosaico requieren molde mediano o familiar para lucir bien');
CALL carmir_excluir(@gelatina, 'Sabor', 'Rompope', 'Relleno', 'Fresas', 'El jugo ácido de la fresa natural altera el sabor delicado del rompope');
CALL carmir_excluir(@gelatina, 'Forma', 'Domo', 'Tamaño', 'Extra grande', 'El molde semiesférico no soporta el peso de 26 porciones al desmoldar');

-- 4.3 Exclusiones en Cupcakes
CALL carmir_excluir(@cupcakes, 'Tamaño', 'Mini', 'Relleno', 'Nutella', 'La cavidad del mini cupcake es muy pequeña para inyectar relleno espeso');
CALL carmir_excluir(@cupcakes, 'Tamaño', 'Mini', 'Relleno', 'Cajeta', 'Los mini cupcakes de bocado pierden firmeza al rellenarse con cajeta');
CALL carmir_excluir(@cupcakes, 'Frosting', 'Domo de fondant', 'Decoración', 'Velas', 'La vela perfora y agrieta el domo liso de fondant del cupcake');
CALL carmir_excluir(@cupcakes, 'Frosting', 'Ganache de chocolate', 'Color de cobertura', 'Rosa pastel', 'El ganache oscuro de cacao no admite tonos pastel claros');
CALL carmir_excluir(@cupcakes, 'Frosting', 'Ganache de chocolate', 'Color de cobertura', 'Blanco', 'El ganache de chocolate amargo conserva su tono cacao natural');
CALL carmir_excluir(@cupcakes, 'Sabor', 'Limón', 'Relleno', 'Cajeta', 'El panqué cítrico de limón no armoniza con el relleno dulce de cajeta');

-- 4.4 Exclusiones en Cheesecakes
CALL carmir_excluir(@cheesecakes, 'Sabor', 'Limón', 'Cobertura', 'Ganache de chocolate', 'El chocolate amargo opaca la frescura cítrica del cheesecake de limón');
CALL carmir_excluir(@cheesecakes, 'Sabor', 'Mango', 'Cobertura', 'Ganache de chocolate', 'El mango fresco armoniza mejor con glaseado frutal o chantilly');
CALL carmir_excluir(@cheesecakes, 'Sabor', 'Cajeta', 'Decoración', 'Frutos rojos', 'La acidez intensa de los frutos rojos choca con el dulce de leche');
CALL carmir_excluir(@cheesecakes, 'Forma', 'Corazón', 'Tamaño', 'Grande', 'El molde desmontable de corazón solo se hornea en tamaño chico o mediano');

-- 4.5 Exclusiones en Pays
CALL carmir_excluir(@pays, 'Sabor', 'Nuez', 'Decoración', 'Fruta picada', 'La humedad de la fruta fresca ablanda la cubierta caramelizada de nuez');
CALL carmir_excluir(@pays, 'Sabor', 'Limón', 'Forma', 'Cuadrado', 'El pay frío de limón se elabora únicamente en molde circular tradicional');
CALL carmir_excluir(@pays, 'Sabor', 'Manzana con canela', 'Decoración', 'Fresas', 'Las especias cálidas de manzana y canela no combinan con fresa fresca');

-- 4.6 Exclusiones en Brownies
CALL carmir_excluir(@brownies, 'Tamaño', 'Mini', 'Decoración', 'Fresas', 'La fresa entera resulta demasiado pesada para un cuadrito mini de brownie');
CALL carmir_excluir(@brownies, 'Sabor', 'Cajeta', 'Cobertura', 'Ganache de chocolate', 'El brownie con remolino de cajeta resulta empalagoso bajo ganache espeso');

-- 4.7 Exclusiones en Donas
CALL carmir_excluir(@donas, 'Cobertura', 'Ganache de chocolate', 'Color de cobertura', 'Rosa', 'El ganache oscuro de cacao no puede teñirse en color rosa');
CALL carmir_excluir(@donas, 'Cobertura', 'Ganache de chocolate', 'Color de cobertura', 'Blanco', 'El ganache de cacao semiamargo mantiene su tono oscuro natural');
CALL carmir_excluir(@donas, 'Tamaño', 'Mini', 'Decoración', 'Fresas', 'Las mini donas no tienen superficie suficiente para sostener fruta fresca');

DROP PROCEDURE IF EXISTS carmir_excluir;

-- 5. Productos iniciales del catálogo (conservan IdPostre 1, 2 y 3) --------
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


-- =====================================================================
-- 3. INVENTARIO Y RECETAS (Módulo 5)
-- =====================================================================
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

-- 2.2 Insumos adicionales para el catálogo ampliado y sus lotes iniciales --
INSERT IGNORE INTO Insumo (NombreInsumo, CategoriaInsumo, UnidadMedida, CostoUnitario, StockMinimo, Perecedero) VALUES
  ('Queso crema', 'Lácteos', 'kg', 145, 1, 1),
  ('Crema de avellana', 'Dulces', 'kg', 175, 0.5, 0),
  ('Galleta de chocolate', 'Galletas', 'kg', 95, 1, 0),
  ('Galleta de vainilla', 'Galletas', 'kg', 75, 1, 0),
  ('Nuez pecana', 'Frutos secos', 'kg', 320, 0.5, 0),
  ('Zanahoria', 'Frutas', 'kg', 24, 1, 1),
  ('Canela molida', 'Especias', 'kg', 260, 0.1, 0),
  ('Café soluble gourmet', 'Saborizantes', 'kg', 340, 0.2, 0),
  ('Mango', 'Frutas', 'kg', 48, 1, 1),
  ('Manzana', 'Frutas', 'kg', 52, 1, 1),
  ('Piña en almíbar', 'Frutas', 'kg', 68, 0.5, 0),
  ('Coco rallado', 'Frutos secos', 'kg', 110, 0.5, 0),
  ('Rompope artesanal', 'Saborizantes', 'L', 165, 0.5, 0),
  ('Zarzamora', 'Frutas', 'kg', 190, 0.5, 1),
  ('Azúcar glass', 'Endulzantes', 'kg', 38, 2, 0),
  ('Chispas de colores', 'Decoración', 'kg', 115, 0.5, 0),
  ('Perlas comestibles', 'Decoración', 'kg', 240, 0.2, 0),
  ('Lámina de oro comestible', 'Decoración', 'pieza', 28, 5, 0),
  ('Base para repostería', 'Empaque', 'pieza', 18, 10, 0);

DROP PROCEDURE IF EXISTS carmir_compra_inicial;
DELIMITER $$
CREATE PROCEDURE carmir_compra_inicial(
  IN p_insumo VARCHAR(80),
  IN p_cantidad DECIMAL(10,3),
  IN p_importe DECIMAL(10,2),
  IN p_dias_cad INT
)
BEGIN
  DECLARE v_id INT DEFAULT NULL;
  SELECT IdInsumo INTO v_id FROM Insumo WHERE NombreInsumo = p_insumo LIMIT 1;

  IF v_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Lote WHERE IdInsumo = v_id) THEN
    INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, TipoMovimiento, CantidadMovimiento, Motivo, CostoMovimiento)
    VALUES (v_id, NULL, 'Compra', p_cantidad, 'Compra inicial (semilla)', p_importe);

    INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, Proveedor, FechaCaducidad)
    VALUES (v_id, p_cantidad, p_cantidad, p_importe, CURDATE(), 'Proveedor de ejemplo',
            IF(p_dias_cad IS NULL, NULL, CURDATE() + INTERVAL p_dias_cad DAY));
  END IF;
END$$
DELIMITER ;

CALL carmir_compra_inicial('Queso crema', 4.000, 580.00, 15);
CALL carmir_compra_inicial('Crema de avellana', 2.000, 350.00, NULL);
CALL carmir_compra_inicial('Galleta de chocolate', 3.000, 285.00, NULL);
CALL carmir_compra_inicial('Galleta de vainilla', 3.000, 225.00, NULL);
CALL carmir_compra_inicial('Nuez pecana', 2.000, 640.00, NULL);
CALL carmir_compra_inicial('Zanahoria', 4.000, 96.00, 12);
CALL carmir_compra_inicial('Canela molida', 0.500, 130.00, NULL);
CALL carmir_compra_inicial('Café soluble gourmet', 1.000, 340.00, NULL);
CALL carmir_compra_inicial('Mango', 3.000, 144.00, 7);
CALL carmir_compra_inicial('Manzana', 3.000, 156.00, 10);
CALL carmir_compra_inicial('Piña en almíbar', 3.000, 204.00, NULL);
CALL carmir_compra_inicial('Coco rallado', 2.000, 220.00, NULL);
CALL carmir_compra_inicial('Rompope artesanal', 2.000, 330.00, NULL);
CALL carmir_compra_inicial('Zarzamora', 1.500, 285.00, 5);
CALL carmir_compra_inicial('Azúcar glass', 6.000, 228.00, NULL);
CALL carmir_compra_inicial('Chispas de colores', 2.000, 230.00, NULL);
CALL carmir_compra_inicial('Perlas comestibles', 1.000, 240.00, NULL);
CALL carmir_compra_inicial('Lámina de oro comestible', 25.000, 700.00, NULL);
CALL carmir_compra_inicial('Base para repostería', 40.000, 720.00, NULL);

DROP PROCEDURE IF EXISTS carmir_compra_inicial;

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

-- 3.4 Recetas de las nuevas opciones (todas las características con PermiteReceta = 1)
DROP PROCEDURE IF EXISTS carmir_poblar_receta;
DELIMITER $$
CREATE PROCEDURE carmir_poblar_receta(
  IN p_cat VARCHAR(50),
  IN p_car VARCHAR(50),
  IN p_opc VARCHAR(60),
  IN p_nom VARCHAR(100),
  IN p_pasos TEXT,
  IN p_tiempo INT,
  IN p_ins1 VARCHAR(80),
  IN p_cant1 DECIMAL(10,3),
  IN p_ins2 VARCHAR(80),
  IN p_cant2 DECIMAL(10,3)
)
BEGIN
  DECLARE v_opc INT DEFAULT NULL;
  DECLARE v_rec INT DEFAULT NULL;
  DECLARE v_i1 INT DEFAULT NULL;
  DECLARE v_i2 INT DEFAULT NULL;

  SELECT o.IdOpcion INTO v_opc
  FROM OpcionCaracteristica o
  JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica
  JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria
  WHERE cp.NombreCategoria = p_cat AND ca.NombreCaracteristica = p_car AND o.NombreOpcion = p_opc
  LIMIT 1;

  IF v_opc IS NOT NULL THEN
    INSERT IGNORE INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion)
    VALUES (v_opc, p_nom, p_pasos, p_tiempo);

    SELECT IdReceta INTO v_rec FROM Receta WHERE IdOpcion = v_opc LIMIT 1;
    SELECT IdInsumo INTO v_i1 FROM Insumo WHERE NombreInsumo = p_ins1 LIMIT 1;

    IF v_rec IS NOT NULL AND v_i1 IS NOT NULL AND p_cant1 > 0 THEN
      INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
      VALUES (v_rec, v_i1, p_cant1);
    END IF;

    IF p_ins2 IS NOT NULL AND p_cant2 IS NOT NULL AND p_cant2 > 0 THEN
      SELECT IdInsumo INTO v_i2 FROM Insumo WHERE NombreInsumo = p_ins2 LIMIT 1;
      IF v_rec IS NOT NULL AND v_i2 IS NOT NULL THEN
        INSERT IGNORE INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo)
        VALUES (v_rec, v_i2, p_cant2);
      END IF;
    END IF;
  END IF;
END$$
DELIMITER ;

-- Nuevas opciones de Pastel
CALL carmir_poblar_receta('Pastel', 'Tamaño', 'Extra grande', 'Bizcocho base extra grande', '1. Batir mantequilla, azúcar y huevo.\n2. Incorporar harina y leche.\n3. Hornear 65 minutos en molde de 36 cm.', 85, 'Harina de trigo', 0.900, 'Huevo', 12.000);
CALL carmir_poblar_receta('Pastel', 'Pisos', '1 piso', 'Nivelado sencillo de un piso', '1. Nivelar el bizcocho frío y asentar sobre disco.', 10, 'Leche entera', 0.050, 'Azúcar', 0.020);
CALL carmir_poblar_receta('Pastel', 'Pisos', '2 pisos', 'Montaje estructural de dos pisos', '1. Colocar soportes internos en la base.\n2. Asentar y centrar el segundo nivel.', 25, 'Base para repostería', 1.000, 'Azúcar glass', 0.080);
CALL carmir_poblar_receta('Pastel', 'Pisos', '3 pisos', 'Montaje estructural de tres pisos', '1. Reforzar niveles con soportes internos.\n2. Montar y nivelar las tres capas.', 40, 'Base para repostería', 2.000, 'Azúcar glass', 0.120);
CALL carmir_poblar_receta('Pastel', 'Sabor', 'Red velvet', 'Mezcla red velvet de cacao', '1. Integrar cacao y tono rojo intenso al batido antes de hornear.', 18, 'Chocolate amargo', 0.080, 'Colorante vegetal', 8.000);
CALL carmir_poblar_receta('Pastel', 'Sabor', 'Zanahoria', 'Mezcla de zanahoria especiada', '1. Rallar zanahoria fresca.\n2. Integrar con canela y nuez picada al batido.', 22, 'Zanahoria', 0.350, 'Nuez pecana', 0.060);
CALL carmir_poblar_receta('Pastel', 'Sabor', 'Limón', 'Aromatizado cítrico de limón', '1. Rallar cáscara y exprimir jugo fresco.\n2. Integrar a la masa.', 12, 'Limón', 0.250, 'Azúcar', 0.050);
CALL carmir_poblar_receta('Pastel', 'Sabor', 'Café', 'Infusión intensa de café gourmet', '1. Disolver café en leche tibia e incorporar al batido.', 12, 'Café soluble gourmet', 0.045, 'Leche entera', 0.100);
CALL carmir_poblar_receta('Pastel', 'Sabor', 'Oreo', 'Mezcla con trozos de galleta oscura', '1. Trocear galletas e incorporar de forma envolvente.', 10, 'Galleta de chocolate', 0.220, 'Crema para batir', 0.060);
CALL carmir_poblar_receta('Pastel', 'Relleno', 'Nutella', 'Relleno cremoso de avellana', '1. Suavizar crema de avellana con crema batida y untar.', 10, 'Crema de avellana', 0.200, 'Crema para batir', 0.050);
CALL carmir_poblar_receta('Pastel', 'Relleno', 'Queso crema', 'Relleno batido de queso crema', '1. Acremar queso frío con azúcar glass y vainilla.', 15, 'Queso crema', 0.200, 'Azúcar glass', 0.060);
CALL carmir_poblar_receta('Pastel', 'Relleno', 'Crema pastelera', 'Crema pastelera a la vainilla', '1. Cocer leche, huevo, azúcar y vainilla hasta espesar.\n2. Enfriar.', 25, 'Leche entera', 0.350, 'Huevo', 3.000);
CALL carmir_poblar_receta('Pastel', 'Relleno', 'Durazno', 'Relleno de durazno con crema', '1. Picar durazno en cubos y mezclar con crema batida.', 15, 'Durazno en almíbar', 0.220, 'Crema para batir', 0.080);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Betún de la casa', 'Betún cremoso de la casa', '1. Batir mantequilla y azúcar glass hasta lograr picos suaves.\n2. Cubrir el pastel.', 18, 'Mantequilla', 0.120, 'Azúcar glass', 0.180);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Buttercream', 'Buttercream peinado en franjas', '1. Batir mantequilla con azúcar glass.\n2. Alisar con peine repostero.', 25, 'Mantequilla', 0.160, 'Azúcar glass', 0.200);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Chantilly', 'Cubierta fresca de crema chantilly', '1. Montar crema fría con azúcar glass y cubrir el pastel.', 15, 'Crema para batir', 0.220, 'Azúcar glass', 0.050);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Fondant liso', 'Forrado artesanal en fondant', '1. Extender pasta fondant con rodillo.\n2. Forrar y alisar bordes.', 40, 'Fondant', 0.250, 'Azúcar glass', 0.060);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Ganache de chocolate', 'Cubierta brillante de ganache', '1. Fundir chocolate amargo en crema caliente.\n2. Bañar y alisar.', 20, 'Chocolate amargo', 0.150, 'Crema para batir', 0.120);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Naked cake', 'Acabado ligero seminaked', '1. Aplicar película fina de betún dejando visibles las capas.', 15, 'Mantequilla', 0.080, 'Azúcar glass', 0.100);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Mármol', 'Cobertura veteada marmoleada', '1. Alternar betún blanco con vetas finas de cacao y alisar.', 25, 'Mantequilla', 0.140, 'Chocolate amargo', 0.040);
CALL carmir_poblar_receta('Pastel', 'Cobertura', 'Degradado', 'Betún difuminado degradado', '1. Aplicar franjas de intensidad gradual y fundir con espátula.', 25, 'Mantequilla', 0.140, 'Colorante vegetal', 6.000);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Fresas', 'Corona de fresas frescas', '1. Desinfectar, secar y acomodar fresas en la parte superior.', 10, 'Fresa', 0.150, 'Azúcar', 0.020);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Frutos rojos', 'Corona de frutos del bosque', '1. Seleccionar zarzamoras y frambuesas frescas y colocar arriba.', 10, 'Zarzamora', 0.070, 'Frambuesa', 0.040);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Perlas', 'Detalle de perlas nacaradas', '1. Colocar perlas de azúcar con pinza de precisión.', 8, 'Perlas comestibles', 0.035, 'Azúcar glass', 0.010);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Chispas', 'Lluvia festiva de chispas', '1. Espolvorear chispas sobre la cubierta fresca.', 5, 'Chispas de colores', 0.070, 'Azúcar glass', 0.010);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Goteo de chocolate', 'Drip de ganache en bordes', '1. Dosificar ganache tibio por la orilla superior para formar gotas.', 12, 'Chocolate amargo', 0.070, 'Crema para batir', 0.040);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Rosetas de crema', 'Corona de rosetas con duya', '1. Formar rosetas simétricas con duya estrella.', 12, 'Crema para batir', 0.100, 'Azúcar glass', 0.030);
CALL carmir_poblar_receta('Pastel', 'Decoración', 'Hoja de oro', 'Aplicación de hoja de oro', '1. Adherir lámina de oro comestible con pincel seco.', 10, 'Lámina de oro comestible', 1.000, 'Perlas comestibles', 0.010);
CALL carmir_poblar_receta('Pastel', 'Base', 'Plato blanco', 'Montaje en plato rígido blanco', '1. Fijar el pastel centrado sobre base blanca limpia.', 3, 'Base para repostería', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Pastel', 'Base', 'Base dorada', 'Montaje en disco dorado', '1. Presentar sobre disco laminado dorado con cinta lateral.', 4, 'Base para repostería', 1.000, 'Listón decorativo', 0.500);
CALL carmir_poblar_receta('Pastel', 'Base', 'Base plateada', 'Montaje en disco plateado', '1. Presentar sobre disco laminado plateado con cinta lateral.', 4, 'Base para repostería', 1.000, 'Listón decorativo', 0.500);
CALL carmir_poblar_receta('Pastel', 'Base', 'Tabla de madera', 'Montaje sobre base rústica', '1. Colocar blonda protectora y centrar en tabla tratada.', 5, 'Base para repostería', 1.500, NULL, NULL);
CALL carmir_poblar_receta('Pastel', 'Base', 'Pedestal de cristal', 'Montaje en base tipo pedestal', '1. Asegurar sobre soporte elevado para mesa principal.', 5, 'Base para repostería', 2.000, 'Listón decorativo', 0.500);

-- Nuevas opciones de Gelatina
CALL carmir_poblar_receta('Gelatina', 'Tamaño', 'Extra grande', 'Base de gelatina extra grande', '1. Hidratar grenetina, disolver en leche tibia endulzada y refrigerar 6 horas.', 40, 'Grenetina', 0.060, 'Leche entera', 1.400);
CALL carmir_poblar_receta('Gelatina', 'Sabor', 'Mango', 'Pulpa fresca de mango para gelatina', '1. Licuar mango maduro con azúcar e integrar a la base tibia.', 15, 'Mango', 0.300, 'Azúcar', 0.040);
CALL carmir_poblar_receta('Gelatina', 'Sabor', 'Rompope', 'Infusión cremosa al rompope', '1. Incorporar rompope artesanal y leche condensada sin hervir.', 12, 'Rompope artesanal', 0.140, 'Leche condensada', 0.080);
CALL carmir_poblar_receta('Gelatina', 'Sabor', 'Coco', 'Mezcla cremosa de coco', '1. Infundir coco rallado con leche evaporada e integrar.', 15, 'Coco rallado', 0.140, 'Leche evaporada', 0.180);
CALL carmir_poblar_receta('Gelatina', 'Sabor', 'Uva', 'Base cristalina sabor uva', '1. Disolver preparado frutal de uva en agua caliente y enfriar.', 10, 'Gelatina de sabores', 0.120, 'Colorante vegetal', 3.000);
CALL carmir_poblar_receta('Gelatina', 'Relleno', 'Durazno', 'Encapsulado de durazno', '1. Acomodar cubos de durazno en capa intermedia semicuajada.', 15, 'Durazno en almíbar', 0.180, 'Grenetina', 0.008);
CALL carmir_poblar_receta('Gelatina', 'Relleno', 'Fresas', 'Encapsulado de fresas naturales', '1. Filetear fresas frescas y sellar en gelatina tibia.', 15, 'Fresa', 0.180, 'Grenetina', 0.008);
CALL carmir_poblar_receta('Gelatina', 'Relleno', 'Queso crema', 'Centro cremoso de queso suave', '1. Licuar queso crema con leche condensada y verter al centro.', 20, 'Queso crema', 0.150, 'Leche condensada', 0.070);
CALL carmir_poblar_receta('Gelatina', 'Relleno', 'Nuez', 'Centro selecto de nuez pecana', '1. Trocear nuez pecana e integrar en capa intermedia.', 10, 'Nuez pecana', 0.065, 'Leche evaporada', 0.050);
CALL carmir_poblar_receta('Gelatina', 'Decoración', 'Frutos rojos', 'Decorado con frutos rojos', '1. Coronar con zarzamoras y frambuesas frescas al desmoldar.', 10, 'Zarzamora', 0.060, 'Frambuesa', 0.035);
CALL carmir_poblar_receta('Gelatina', 'Decoración', 'Perlas', 'Perlas dulces sobre gelatina', '1. Colocar perlas justo antes de entregar.', 5, 'Perlas comestibles', 0.030, NULL, NULL);
CALL carmir_poblar_receta('Gelatina', 'Base', 'Plato blanco', 'Charola blanca impermeable', '1. Desmoldar cuidadosamente sobre charola blanca húmeda.', 3, 'Base para repostería', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Gelatina', 'Base', 'Base dorada', 'Base dorada impermeable', '1. Desmoldar sobre disco dorado plastificado.', 3, 'Base para repostería', 1.000, 'Listón decorativo', 0.400);
CALL carmir_poblar_receta('Gelatina', 'Base', 'Base plateada', 'Base plateada impermeable', '1. Desmoldar sobre disco plateado plastificado.', 3, 'Base para repostería', 1.000, 'Listón decorativo', 0.400);

-- Opciones de Cupcakes
CALL carmir_poblar_receta('Cupcakes', 'Tamaño', 'Mini', 'Horneado de 12 mini cupcakes', '1. Batir masa base y dosificar en capacillos mini.\n2. Hornear 14 minutos a 175 °C.', 30, 'Harina de trigo', 0.220, 'Huevo', 3.000);
CALL carmir_poblar_receta('Cupcakes', 'Tamaño', 'Mediano', 'Horneado de 6 cupcakes clásicos', '1. Batir masa con mantequilla y llenar capacillos a 2/3.\n2. Hornear 20 minutos.', 35, 'Harina de trigo', 0.280, 'Mantequilla', 0.110);
CALL carmir_poblar_receta('Cupcakes', 'Tamaño', 'Grande', 'Horneado de 12 cupcakes clásicos', '1. Preparar batido doble y hornear 22 minutos en charola de 12 cavidades.', 45, 'Harina de trigo', 0.520, 'Mantequilla', 0.200);
CALL carmir_poblar_receta('Cupcakes', 'Tamaño', 'Jumbo', 'Horneado de 6 cupcakes jumbo', '1. Dosificar en capacillos altos tipo tulipán y hornear 26 minutos.', 45, 'Harina de trigo', 0.450, 'Mantequilla', 0.180);
CALL carmir_poblar_receta('Cupcakes', 'Sabor', 'Vainilla', 'Panqué de vainilla natural', '1. Perfumar la masa con extracto puro de vainilla y leche.', 8, 'Extracto de vainilla', 0.012, 'Azúcar', 0.080);
CALL carmir_poblar_receta('Cupcakes', 'Sabor', 'Chocolate', 'Panqué húmedo de chocolate', '1. Incorporar chocolate fundido al batido de cupcakes.', 12, 'Chocolate amargo', 0.110, 'Leche entera', 0.100);
CALL carmir_poblar_receta('Cupcakes', 'Sabor', 'Red velvet', 'Panqué red velvet', '1. Añadir cacao suave y color rojo característico a la masa.', 12, 'Chocolate amargo', 0.050, 'Colorante vegetal', 6.000);
CALL carmir_poblar_receta('Cupcakes', 'Sabor', 'Limón', 'Panqué cítrico de limón', '1. Integrar ralladura fina y jugo de limón fresco.', 10, 'Limón', 0.200, 'Azúcar', 0.060);
CALL carmir_poblar_receta('Cupcakes', 'Sabor', 'Zanahoria', 'Panqué especiado de zanahoria', '1. Mezclar zanahoria rallada fina con toque de canela.', 15, 'Zanahoria', 0.250, 'Canela molida', 0.012);
CALL carmir_poblar_receta('Cupcakes', 'Sabor', 'Oreo', 'Panqué con trozos de Oreo', '1. Incorporar trozos de galleta oscura en cada capacillo.', 10, 'Galleta de chocolate', 0.160, 'Leche entera', 0.080);
CALL carmir_poblar_receta('Cupcakes', 'Relleno', 'Mermelada de fresa', 'Centro frutal de fresa', '1. Descorazonar cupcakes fríos y rellenar con compota de fresa.', 12, 'Fresa', 0.140, 'Azúcar', 0.040);
CALL carmir_poblar_receta('Cupcakes', 'Relleno', 'Nutella', 'Centro cremoso de Nutella', '1. Inyectar crema de avellana con manga pastelera.', 10, 'Crema de avellana', 0.120, NULL, NULL);
CALL carmir_poblar_receta('Cupcakes', 'Relleno', 'Cajeta', 'Centro tradicional de cajeta', '1. Rellenar el centro del panqué con cajeta suave.', 10, 'Cajeta', 0.140, NULL, NULL);
CALL carmir_poblar_receta('Cupcakes', 'Relleno', 'Queso crema', 'Centro batido de queso', '1. Batir queso crema dulce e inyectar al centro.', 12, 'Queso crema', 0.120, 'Azúcar glass', 0.040);
CALL carmir_poblar_receta('Cupcakes', 'Frosting', 'Remolino de betún', 'Espiral clásico de betún', '1. Batir mantequilla y azúcar glass y formar espiral alto con duya 1M.', 15, 'Mantequilla', 0.100, 'Azúcar glass', 0.150);
CALL carmir_poblar_receta('Cupcakes', 'Frosting', 'Remolino de chantilly', 'Copete ligero de chantilly', '1. Montar crema fría y decorar en espiral.', 12, 'Crema para batir', 0.180, 'Azúcar glass', 0.040);
CALL carmir_poblar_receta('Cupcakes', 'Frosting', 'Remolino de queso crema', 'Frosting de queso crema', '1. Acremar queso crema con mantequilla y formar remolino.', 15, 'Queso crema', 0.140, 'Mantequilla', 0.050);
CALL carmir_poblar_receta('Cupcakes', 'Frosting', 'Ganache de chocolate', 'Copete de ganache montado', '1. Emulsionar chocolate con crema, enfriar y batir para duyar.', 20, 'Chocolate amargo', 0.120, 'Crema para batir', 0.080);
CALL carmir_poblar_receta('Cupcakes', 'Frosting', 'Domo de fondant', 'Cubierta domo de fondant', '1. Cortar círculos de fondant y moldear en cúpula suave.', 25, 'Fondant', 0.150, 'Azúcar glass', 0.040);
CALL carmir_poblar_receta('Cupcakes', 'Decoración', 'Chispas', 'Toque de chispas en cupcakes', '1. Espolvorear chispas sobre cada remolino.', 5, 'Chispas de colores', 0.050, NULL, NULL);
CALL carmir_poblar_receta('Cupcakes', 'Decoración', 'Perlas', 'Perlas nacaradas sobre frosting', '1. Distribuir perlas comestibles sobre el copete.', 6, 'Perlas comestibles', 0.025, NULL, NULL);
CALL carmir_poblar_receta('Cupcakes', 'Decoración', 'Fresas', 'Coronado con fresa natural', '1. Colocar media fresa fresca en cada pieza.', 8, 'Fresa', 0.120, NULL, NULL);
CALL carmir_poblar_receta('Cupcakes', 'Decoración', 'Flores de azúcar', 'Mini flores de fondant', '1. Modelar pequeñas flores y colocarlas sobre el frosting.', 25, 'Fondant', 0.045, 'Colorante vegetal', 3.000);
CALL carmir_poblar_receta('Cupcakes', 'Decoración', 'Hoja de oro', 'Destellos dorados en cupcakes', '1. Colocar hojuelas de oro comestible con pinza.', 8, 'Lámina de oro comestible', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Cupcakes', 'Decoración', 'Velas', 'Vela festiva para caja', '1. Incluir vela decorativa lista para encender.', 2, 'Vela de cumpleaños', 2.000, NULL, NULL);

-- Opciones de Cheesecakes
CALL carmir_poblar_receta('Cheesecakes', 'Tamaño', 'Chico', 'Base de galleta y horneado chico', '1. Prensar galleta molida con mantequilla.\n2. Hornear a baño maría 45 minutos.', 60, 'Galleta de vainilla', 0.200, 'Mantequilla', 0.090);
CALL carmir_poblar_receta('Cheesecakes', 'Tamaño', 'Mediano', 'Base de galleta y horneado mediano', '1. Formar costra firme de galleta y mantequilla.\n2. Hornear 55 minutos.', 70, 'Galleta de vainilla', 0.300, 'Mantequilla', 0.130);
CALL carmir_poblar_receta('Cheesecakes', 'Tamaño', 'Grande', 'Base de galleta y horneado grande', '1. Prensar base familiar y hornear lentamente 65 minutos.', 80, 'Galleta de vainilla', 0.420, 'Mantequilla', 0.180);
CALL carmir_poblar_receta('Cheesecakes', 'Sabor', 'Queso', 'Relleno clásico estilo Nueva York', '1. Batir queso crema a temperatura ambiente con huevo, azúcar y vainilla.', 25, 'Queso crema', 0.400, 'Huevo', 3.000);
CALL carmir_poblar_receta('Cheesecakes', 'Sabor', 'Frutos rojos', 'Mezcla de queso con frutos rojos', '1. Incorporar reducción artesanal de zarzamora y frambuesa al queso.', 30, 'Queso crema', 0.360, 'Zarzamora', 0.120);
CALL carmir_poblar_receta('Cheesecakes', 'Sabor', 'Oreo', 'Mezcla de queso con Oreo', '1. Integrar galleta de chocolate troceada en la crema de queso.', 25, 'Queso crema', 0.360, 'Galleta de chocolate', 0.160);
CALL carmir_poblar_receta('Cheesecakes', 'Sabor', 'Cajeta', 'Cheesecake marmoleado de cajeta', '1. Batir queso suave con cajeta tradicional de Celaya.', 25, 'Queso crema', 0.350, 'Cajeta', 0.160);
CALL carmir_poblar_receta('Cheesecakes', 'Sabor', 'Mango', 'Cheesecake frío de mango', '1. Integrar queso crema, pulpa de mango natural y toque de grenetina.', 30, 'Queso crema', 0.340, 'Mango', 0.250);
CALL carmir_poblar_receta('Cheesecakes', 'Sabor', 'Limón', 'Cheesecake cítrico de limón', '1. Batir queso crema con leche condensada y jugo fresco de limón.', 20, 'Queso crema', 0.340, 'Limón', 0.220);
CALL carmir_poblar_receta('Cheesecakes', 'Cobertura', 'Glaseado', 'Espejo brillante para cheesecake', '1. Preparar reducción brillante con fruta y cubrir la superficie fría.', 15, 'Fresa', 0.120, 'Azúcar', 0.060);
CALL carmir_poblar_receta('Cheesecakes', 'Cobertura', 'Ganache de chocolate', 'Capa superior de ganache', '1. Verter ganache tibio sobre el cheesecake frío y refrigerar.', 15, 'Chocolate amargo', 0.120, 'Crema para batir', 0.080);
CALL carmir_poblar_receta('Cheesecakes', 'Cobertura', 'Chantilly', 'Capa cremosa de chantilly', '1. Cubrir suavemente con crema batida fresca.', 12, 'Crema para batir', 0.160, 'Azúcar glass', 0.040);
CALL carmir_poblar_receta('Cheesecakes', 'Decoración', 'Fresas', 'Abanico de fresas frescas', '1. Acomodar fresas laminadas en espiral.', 10, 'Fresa', 0.150, NULL, NULL);
CALL carmir_poblar_receta('Cheesecakes', 'Decoración', 'Frutos rojos', 'Corona de frutos del bosque', '1. Decorar con zarzamoras y frambuesas seleccionadas.', 10, 'Zarzamora', 0.065, 'Frambuesa', 0.035);
CALL carmir_poblar_receta('Cheesecakes', 'Decoración', 'Rosetas de crema', 'Borde de rosetas de crema', '1. Duyar rosetas alrededor del borde del cheesecake.', 10, 'Crema para batir', 0.100, 'Azúcar glass', 0.025);
CALL carmir_poblar_receta('Cheesecakes', 'Decoración', 'Hoja de oro', 'Detalle de oro comestible', '1. Colocar lámina dorada al centro del postre.', 8, 'Lámina de oro comestible', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Cheesecakes', 'Base', 'Plato blanco', 'Presentación en plato blanco', '1. Desmoldar sobre base rígida blanca.', 3, 'Base para repostería', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Cheesecakes', 'Base', 'Base dorada', 'Presentación en disco dorado', '1. Desmoldar sobre disco dorado de gala.', 3, 'Base para repostería', 1.000, 'Listón decorativo', 0.400);
CALL carmir_poblar_receta('Cheesecakes', 'Base', 'Tabla de madera', 'Presentación en tabla rústica', '1. Colocar sobre tabla de madera tratada.', 4, 'Base para repostería', 1.500, NULL, NULL);

-- Opciones de Pays
CALL carmir_poblar_receta('Pays', 'Tamaño', 'Chico', 'Masa quebrada para pay chico', '1. Sablear harina fría con mantequilla.\n2. Forrar molde chico y prehornear.', 40, 'Harina de trigo', 0.220, 'Mantequilla', 0.110);
CALL carmir_poblar_receta('Pays', 'Tamaño', 'Mediano', 'Masa quebrada para pay mediano', '1. Extender masa de mantequilla y forrar molde mediano.\n2. Hornear en blanco.', 45, 'Harina de trigo', 0.320, 'Mantequilla', 0.150);
CALL carmir_poblar_receta('Pays', 'Tamaño', 'Grande', 'Masa quebrada para pay grande', '1. Preparar masa familiar y hornear hasta dorar bordes.', 55, 'Harina de trigo', 0.440, 'Mantequilla', 0.210);
CALL carmir_poblar_receta('Pays', 'Sabor', 'Nuez', 'Relleno caramelizado de nuez pecana', '1. Mezclar nuez pecana tostada con huevo, azúcar y mantequilla.\n2. Hornear 40 min.', 45, 'Nuez pecana', 0.180, 'Azúcar', 0.150);
CALL carmir_poblar_receta('Pays', 'Sabor', 'Manzana con canela', 'Compota horneada de manzana y canela', '1. Laminar manzana, saltear con mantequilla, azúcar y canela y hornear.', 40, 'Manzana', 0.450, 'Canela molida', 0.015);
CALL carmir_poblar_receta('Pays', 'Sabor', 'Limón', 'Crema helada de limón para pay', '1. Licuar leche condensada, evaporada y jugo de limón.\n2. Refrigerar 3 horas.', 25, 'Leche condensada', 0.300, 'Limón', 0.250);
CALL carmir_poblar_receta('Pays', 'Sabor', 'Piña', 'Relleno artesanal de piña', '1. Reducir piña picada con azúcar hasta espesar y rellenar.', 35, 'Piña en almíbar', 0.400, 'Azúcar', 0.080);
CALL carmir_poblar_receta('Pays', 'Sabor', 'Queso', 'Relleno tradicional de queso horneado', '1. Licuar queso crema, leche condensada y huevo.\n2. Hornear 45 minutos.', 50, 'Queso crema', 0.260, 'Leche condensada', 0.180);
CALL carmir_poblar_receta('Pays', 'Cobertura', 'Chantilly', 'Cubierta de chantilly para pay', '1. Montar crema fría y extender en ondas suaves.', 12, 'Crema para batir', 0.150, 'Azúcar glass', 0.030);
CALL carmir_poblar_receta('Pays', 'Cobertura', 'Betún de la casa', 'Merengue flameado artesanal', '1. Montar claras con azúcar y dorar ligeramente los picos.', 18, 'Huevo', 3.000, 'Azúcar', 0.120);
CALL carmir_poblar_receta('Pays', 'Cobertura', 'Glaseado', 'Barniz brillante para pay', '1. Aplicar brillo tibio con brocha repostera.', 8, 'Azúcar', 0.080, 'Durazno en almíbar', 0.060);
CALL carmir_poblar_receta('Pays', 'Decoración', 'Fruta picada', 'Fruta fresca sobre pay', '1. Acomodar cubos frescos de fruta de estación.', 10, 'Fresa', 0.080, 'Durazno en almíbar', 0.100);
CALL carmir_poblar_receta('Pays', 'Decoración', 'Fresas', 'Decorado de fresas enteras', '1. Colocar fresas brillantes en la orilla.', 8, 'Fresa', 0.140, NULL, NULL);
CALL carmir_poblar_receta('Pays', 'Decoración', 'Rosetas de crema', 'Corona de rosetas en pay', '1. Formar borde de rosetas con duya.', 10, 'Crema para batir', 0.100, 'Azúcar glass', 0.020);
CALL carmir_poblar_receta('Pays', 'Decoración', 'Perlas', 'Perlas crujientes sobre pay', '1. Esparcir perlas de azúcar.', 5, 'Perlas comestibles', 0.025, NULL, NULL);

-- Opciones de Brownies
CALL carmir_poblar_receta('Brownies', 'Tamaño', 'Mini', 'Charola de 9 mini brownies', '1. Fundir mantequilla y hornear en molde cuadrado.\n2. Cortar en 9 bocados.', 35, 'Mantequilla', 0.110, 'Huevo', 2.000);
CALL carmir_poblar_receta('Brownies', 'Tamaño', 'Mediano', 'Caja de 6 brownies clásicos', '1. Preparar base húmeda de mantequilla, huevo y harina.\n2. Hornear 28 minutos.', 40, 'Mantequilla', 0.150, 'Harina de trigo', 0.180);
CALL carmir_poblar_receta('Brownies', 'Tamaño', 'Grande', 'Caja de 12 brownies clásicos', '1. Hornear charola grande y porcionar en 12 cuadros exactos.', 50, 'Mantequilla', 0.250, 'Harina de trigo', 0.300);
CALL carmir_poblar_receta('Brownies', 'Tamaño', 'Jumbo', 'Charola familiar de brownies', '1. Hornear charola completa para evento y presentar cortada.', 55, 'Mantequilla', 0.320, 'Harina de trigo', 0.380);
CALL carmir_poblar_receta('Brownies', 'Sabor', 'Chocolate', 'Cacao intenso tipo fudge', '1. Fundir chocolate amargo con azúcar para lograr corteza craquelada.', 15, 'Chocolate amargo', 0.160, 'Azúcar', 0.150);
CALL carmir_poblar_receta('Brownies', 'Sabor', 'Nutella', 'Marmoleado de Nutella', '1. Incorporar cucharadas de crema de avellana y marmolear con palillo.', 15, 'Chocolate amargo', 0.120, 'Crema de avellana', 0.120);
CALL carmir_poblar_receta('Brownies', 'Sabor', 'Nuez', 'Brownie con nuez pecana', '1. Tostar nuez pecana e integrar en la mezcla de chocolate.', 18, 'Chocolate amargo', 0.120, 'Nuez pecana', 0.075);
CALL carmir_poblar_receta('Brownies', 'Sabor', 'Oreo', 'Brownie con galleta Oreo', '1. Cubrir e intercalar trozos grandes de galleta oscura antes de hornear.', 15, 'Chocolate amargo', 0.120, 'Galleta de chocolate', 0.160);
CALL carmir_poblar_receta('Brownies', 'Sabor', 'Cajeta', 'Remolino de cajeta artesanal', '1. Trazar espirales de cajeta de Celaya sobre la mezcla antes del horno.', 15, 'Chocolate amargo', 0.120, 'Cajeta', 0.140);
CALL carmir_poblar_receta('Brownies', 'Cobertura', 'Ganache de chocolate', 'Baño espeso de ganache', '1. Cubrir la charola fría con ganache brillante.', 15, 'Chocolate amargo', 0.100, 'Crema para batir', 0.070);
CALL carmir_poblar_receta('Brownies', 'Cobertura', 'Glaseado', 'Líneas de glaseado dulce', '1. Trazar hilos diagonales de azúcar glass batida.', 10, 'Azúcar glass', 0.120, 'Leche entera', 0.030);
CALL carmir_poblar_receta('Brownies', 'Cobertura', 'Mármol', 'Cubierta marmoleada en brownies', '1. Combinar ganache oscuro con hilos blancos y marmolear.', 18, 'Chocolate amargo', 0.080, 'Azúcar glass', 0.080);
CALL carmir_poblar_receta('Brownies', 'Decoración', 'Chispas', 'Chispas confitadas sobre brownie', '1. Espolvorear chispas al cubrir.', 5, 'Chispas de colores', 0.050, NULL, NULL);
CALL carmir_poblar_receta('Brownies', 'Decoración', 'Fresas', 'Fresas frescas sobre brownies', '1. Colocar fresas seleccionadas al momento de empacar.', 8, 'Fresa', 0.130, NULL, NULL);
CALL carmir_poblar_receta('Brownies', 'Decoración', 'Frutos rojos', 'Frutos rojos sobre brownies', '1. Distribuir zarzamoras y frambuesas frescas.', 8, 'Zarzamora', 0.060, 'Frambuesa', 0.030);
CALL carmir_poblar_receta('Brownies', 'Decoración', 'Hoja de oro', 'Toque de oro en brownies', '1. Aplicar destellos de hoja de oro comestible.', 8, 'Lámina de oro comestible', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Brownies', 'Base', 'Plato blanco', 'Empaque en charola blanca', '1. Acomodar piezas con papel encerado en base blanca.', 3, 'Base para repostería', 1.000, NULL, NULL);
CALL carmir_poblar_receta('Brownies', 'Base', 'Tabla de madera', 'Presentación en tabla de madera', '1. Presentar cuadros sobre tabla rústica.', 4, 'Base para repostería', 1.500, NULL, NULL);
CALL carmir_poblar_receta('Brownies', 'Base', 'Pizarra', 'Presentación en base pizarra', '1. Acomodar brownies sobre pizarra negra de degustación.', 4, 'Base para repostería', 1.500, NULL, NULL);

-- Opciones de Donas
CALL carmir_poblar_receta('Donas', 'Tamaño', 'Mini', 'Amasado y cocción de 12 mini donas', '1. Amasar, fermentar 45 minutos, cortar aros mini y dorar.', 65, 'Harina de trigo', 0.250, 'Mantequilla', 0.060);
CALL carmir_poblar_receta('Donas', 'Tamaño', 'Mediano', 'Amasado y cocción de 6 donas clásicas', '1. Fermentar masa brioche suave, cortar 6 piezas y cocer.', 70, 'Harina de trigo', 0.340, 'Mantequilla', 0.080);
CALL carmir_poblar_receta('Donas', 'Tamaño', 'Grande', 'Amasado y cocción de 12 donas clásicas', '1. Preparar docena completa fermentada al día.', 85, 'Harina de trigo', 0.620, 'Mantequilla', 0.140);
CALL carmir_poblar_receta('Donas', 'Sabor', 'Vainilla', 'Masa perfumada a la vainilla', '1. Aromatizar el amasado con extracto natural de vainilla y huevo.', 10, 'Extracto de vainilla', 0.012, 'Huevo', 2.000);
CALL carmir_poblar_receta('Donas', 'Sabor', 'Chocolate', 'Masa suave de cacao', '1. Integrar chocolate fundido a la masa brioche.', 12, 'Chocolate amargo', 0.090, 'Leche entera', 0.120);
CALL carmir_poblar_receta('Donas', 'Sabor', 'Canela', 'Masa aromática de canela', '1. Incorporar canela recién molida y azúcar a la masa.', 10, 'Canela molida', 0.018, 'Azúcar', 0.080);
CALL carmir_poblar_receta('Donas', 'Cobertura', 'Glaseado', 'Glaseado real brillante', '1. Batir azúcar glass con leche tibia y sumergir cada dona.', 12, 'Azúcar glass', 0.220, 'Leche entera', 0.060);
CALL carmir_poblar_receta('Donas', 'Cobertura', 'Ganache de chocolate', 'Cobertura de chocolate para donas', '1. Sumergir donas tibias en ganache semiamargo brillante.', 15, 'Chocolate amargo', 0.110, 'Crema para batir', 0.070);
CALL carmir_poblar_receta('Donas', 'Cobertura', 'Mármol', 'Glaseado marmoleado bicolor', '1. Trazar vetas de cacao sobre glaseado blanco y sumergir las piezas.', 15, 'Azúcar glass', 0.180, 'Chocolate amargo', 0.040);
CALL carmir_poblar_receta('Donas', 'Decoración', 'Chispas', 'Chispas clásicas sobre donas', '1. Espolvorear chispas inmediatamente después de glasear.', 5, 'Chispas de colores', 0.060, NULL, NULL);
CALL carmir_poblar_receta('Donas', 'Decoración', 'Perlas', 'Perlas crujientes sobre donas', '1. Decorar el aro glaseado con perlas dulces.', 6, 'Perlas comestibles', 0.025, NULL, NULL);
CALL carmir_poblar_receta('Donas', 'Decoración', 'Fresas', 'Fresas frescas picadas en donas', '1. Coronar el glaseado con cubitos frescos de fresa.', 8, 'Fresa', 0.120, NULL, NULL);
CALL carmir_poblar_receta('Donas', 'Decoración', 'Hoja de oro', 'Lámina de oro gourmet en donas', '1. Colocar detalle de oro comestible en cada dona.', 8, 'Lámina de oro comestible', 1.000, NULL, NULL);

DROP PROCEDURE IF EXISTS carmir_poblar_receta;


-- =====================================================================
-- 4. CATÁLOGO DE LA PÁGINA WEB (Módulo 1)
-- =====================================================================
SET @pastel      = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Pastel');
SET @gelatina    = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Gelatina');
SET @cupcakes    = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Cupcakes');
SET @cheesecakes = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Cheesecakes');
SET @pays        = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Pays');
SET @brownies    = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Brownies');
SET @donas       = (SELECT IdCategoria FROM CategoriaPostre WHERE NombreCategoria = 'Donas');

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
DELETE FROM tmp_opciones;

-- 2.5 Pastel red velvet imperial: Mediano + 2 pisos + Redondo + Red velvet + Queso crema + Buttercream + Rosa pastel + Frutos rojos + Perlas + Base dorada
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pastel AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Pisos' AND o.NombreOpcion = '2 pisos')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Redondo')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Red velvet')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Queso crema')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Buttercream')
   OR (c.NombreCaracteristica = 'Color de cobertura' AND o.NombreOpcion = 'Rosa pastel')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Frutos rojos', 'Perlas'))
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Base dorada'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pastel, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pastel red velvet imperial', 'Pastel de dos niveles en terciopelo rojo con queso crema, buttercream rosa pastel, frutos rojos y perlas', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pastel red velvet imperial';
DELETE FROM tmp_opciones;

-- 2.6 Pastel zanahoria rústico: Mediano + 1 piso + Redondo + Zanahoria + Queso crema + Naked cake + Rosetas de crema + Tabla de madera
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pastel AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Pisos' AND o.NombreOpcion = '1 piso')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Redondo')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Zanahoria')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Queso crema')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Naked cake')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Rosetas de crema')
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Tabla de madera'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pastel, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pastel zanahoria rústico', 'Bizcocho especiado de zanahoria y nuez con acabado naked cake sobre tabla de madera', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pastel zanahoria rústico';
DELETE FROM tmp_opciones;

-- 2.7 Gelatina rosca de rompope y nuez: Familiar + Rosca + Rompope + Nuez + Crema chantilly + Base dorada
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @gelatina AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Familiar')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Rosca')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Rompope')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Nuez')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Crema chantilly')
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Base dorada'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @gelatina, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Gelatina rosca de rompope y nuez', 'Rosca cremosa de rompope artesanal rellena de nuez pecana sobre base dorada', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Gelatina rosca de rompope y nuez';
DELETE FROM tmp_opciones;

-- 2.8 Caja de cupcakes red velvet: Mediano + Red velvet + Queso crema + Remolino de queso crema + Blanco + Fresas + Perlas
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @cupcakes AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Red velvet')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Queso crema')
   OR (c.NombreCaracteristica = 'Frosting' AND o.NombreOpcion = 'Remolino de queso crema')
   OR (c.NombreCaracteristica = 'Color de cobertura' AND o.NombreOpcion = 'Blanco')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Fresas', 'Perlas')));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @cupcakes, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Caja de cupcakes red velvet', 'Seis cupcakes de terciopelo rojo rellenos de queso crema con fresa fresca y perlas', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Caja de cupcakes red velvet';
DELETE FROM tmp_opciones;

-- 2.9 Docena de cupcakes doble chocolate: Grande + Chocolate + Nutella + Ganache de chocolate + Chispas + Hoja de oro
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @cupcakes AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Chocolate')
   OR (c.NombreCaracteristica = 'Relleno' AND o.NombreOpcion = 'Nutella')
   OR (c.NombreCaracteristica = 'Frosting' AND o.NombreOpcion = 'Ganache de chocolate')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Chispas', 'Hoja de oro')));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @cupcakes, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Docena de cupcakes doble chocolate', 'Doce cupcakes húmedos de cacao con corazón de avellana, ganache y destellos de oro', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Docena de cupcakes doble chocolate';
DELETE FROM tmp_opciones;

-- 2.10 Cheesecake estilo Nueva York con frutos rojos: Mediano + Redondo + Frutos rojos + Glaseado + Fresas + Frutos rojos + Base dorada
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @cheesecakes AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Redondo')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Frutos rojos')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Glaseado')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Fresas', 'Frutos rojos'))
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Base dorada'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @cheesecakes, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Cheesecake estilo Nueva York con frutos rojos', 'Tarta horneada de queso cremoso con espejo brillante, fresas y frutos del bosque', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Cheesecake estilo Nueva York con frutos rojos';
DELETE FROM tmp_opciones;

-- 2.11 Cheesecake supremo de Oreo: Chico + Redondo + Oreo + Ganache de chocolate + Rosetas de crema + Plato blanco
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @cheesecakes AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Chico')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Redondo')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Oreo')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Ganache de chocolate')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Rosetas de crema')
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Plato blanco'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @cheesecakes, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Cheesecake supremo de Oreo', 'Cheesecake con galleta de chocolate, cubierta de ganache semiamargo y rosetas de crema', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Cheesecake supremo de Oreo';
DELETE FROM tmp_opciones;

-- 2.12 Pay artesanal de nuez pecana: Mediano + Redondo + Nuez + Glaseado + Rosetas de crema
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pays AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Redondo')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Nuez')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Glaseado')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Rosetas de crema'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pays, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pay artesanal de nuez pecana', 'Masa dorada de mantequilla rellena de nuez pecana tostada y caramelizada', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pay artesanal de nuez pecana';
DELETE FROM tmp_opciones;

-- 2.13 Pay helado de limón con merengue: Grande + Redondo + Limón + Betún de la casa + Perlas
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @pays AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande')
   OR (c.NombreCaracteristica = 'Forma' AND o.NombreOpcion = 'Redondo')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Limón')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Betún de la casa')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Perlas'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @pays, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Pay helado de limón con merengue', 'Pay frío y cremoso de limón verde coronado con merengue dorado', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Pay helado de limón con merengue';
DELETE FROM tmp_opciones;

-- 2.14 Caja de brownies fudge con avellana: Mediano + Nutella + Ganache de chocolate + Fresas + Tabla de madera
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @brownies AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Nutella')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Ganache de chocolate')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion = 'Fresas')
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Tabla de madera'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @brownies, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Caja de brownies fudge con avellana', 'Seis brownies húmedos marmoleados con crema de avellana, ganache y fresas frescas', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Caja de brownies fudge con avellana';
DELETE FROM tmp_opciones;

-- 2.15 Charola de brownies de chocolate y nuez: Grande + Nuez + Mármol + Chispas + Hoja de oro + Pizarra
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @brownies AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Nuez')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Mármol')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Chispas', 'Hoja de oro'))
   OR (c.NombreCaracteristica = 'Base' AND o.NombreOpcion = 'Pizarra'));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @brownies, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Charola de brownies de chocolate y nuez', 'Doce brownies de cacao y nuez pecana con cubierta marmoleada sobre pizarra', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Charola de brownies de chocolate y nuez';
DELETE FROM tmp_opciones;

-- 2.16 Media docena de donas glaseadas pastel: Mediano + Vainilla + Glaseado + Rosa + Chispas + Perlas
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @donas AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Mediano')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Vainilla')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Glaseado')
   OR (c.NombreCaracteristica = 'Color de cobertura' AND o.NombreOpcion = 'Rosa')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Chispas', 'Perlas')));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @donas, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Media docena de donas glaseadas pastel', 'Seis donas esponjosas de vainilla con glaseado rosa brillante, chispas y perlas', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Media docena de donas glaseadas pastel';
DELETE FROM tmp_opciones;

-- 2.17 Docena de donas de cacao y oro: Grande + Chocolate + Ganache de chocolate + Chocolate + Hoja de oro + Fresas
INSERT INTO tmp_opciones
SELECT o.IdOpcion, 1 FROM OpcionCaracteristica o JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
WHERE c.IdCategoria = @donas AND (
      (c.NombreCaracteristica = 'Tamaño' AND o.NombreOpcion = 'Grande')
   OR (c.NombreCaracteristica = 'Sabor' AND o.NombreOpcion = 'Chocolate')
   OR (c.NombreCaracteristica = 'Cobertura' AND o.NombreOpcion = 'Ganache de chocolate')
   OR (c.NombreCaracteristica = 'Color de cobertura' AND o.NombreOpcion = 'Chocolate')
   OR (c.NombreCaracteristica = 'Decoración' AND o.NombreOpcion IN ('Hoja de oro', 'Fresas')));
INSERT IGNORE INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda)
SELECT @donas, GROUP_CONCAT(CONCAT(IdOpcion, ':', Cantidad) ORDER BY IdOpcion SEPARATOR '|'),
       'Docena de donas de cacao y oro', 'Doce donas de cacao cubiertas de ganache semiamargo con fresa fresca y hoja de oro', NULL, 1
FROM tmp_opciones;
INSERT IGNORE INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida)
SELECT p.IdPostre, t.IdOpcion, t.Cantidad FROM tmp_opciones t JOIN Postre p ON p.NombrePostre = 'Docena de donas de cacao y oro';

DROP TEMPORARY TABLE tmp_opciones;


-- =====================================================================
-- 5. DATOS DEL NEGOCIO: COBERTURA DE ENVÍO EN LEÓN
-- =====================================================================

-- 5.1 Códigos postales de León con envío a domicilio (RF-9) y su costo.
--     Cualquier otro código postal sale «sin cobertura» en la página web (por ejemplo 37680).
INSERT IGNORE INTO CodigoPostalCobertura (CodigoPostal, CostoEnvio) VALUES
  ('37238', 30.00),   -- Prado Hermoso (donde está la tienda)
  ('37000', 50.00),   -- Centro
  ('37160', 50.00),   -- Jardines del Moral
  ('37360', 60.00),   -- Arbide
  ('37500', 70.00);   -- Los Gavilanes

-- 5.2 Datos bancarios de prueba para que la web ofrezca «Transferencia».
--     Solo se escriben si están vacíos (no borra lo que haya guardado la administradora).
UPDATE Configuracion
SET Valor = 'Banco de prueba · Cuenta 0123456789 · CLABE 012345678901234567 · A nombre de Delicias de Postres'
WHERE Clave = 'DatosBancarios' AND Valor = '';


-- =====================================================================
-- 6. PEDIDOS, PAGOS Y NOTIFICACIONES (Módulos 2 y 4)
-- =====================================================================

-- Imagen PNG del comprobante de prueba (la ve el Panel en «Validación de pagos»)
SET @poblar_png = UNHEX('89504E470D0A1A0A0000000D4948445200000168000000C8040300000037A066BC00000030504C5445FFFFFFFBFBFBF0F1F3E5E6E8DBDCDDC8CBD2A6AEBC94969B7B818A5A6271414F661E468C3434341E1E1E17171714141457E48E0500000FEC4944415478DAED9D7B7814559AC67FD5DDD5DD0938F6AE28B2E3A5C380E82EAEAD78210FBA74986D2546B489839AE83E340CA883F818586F08990E379919111A119D512271D72841905608E0304A671113C206DBEB720BE97577071961A795845CAABB6BFF38557D0938F4834944B7DE3FAACEF5ABB74E9D73EAD4DB5F554B5BF9FEC18441DA206D9036481BA40DD2066983F4994A5AF1549DF6918E17656575C7B8539BB2E881C6B59FFFE394D3E092B8E9C7E92752B3EEAFAF9E121F8B34E45E577C2C50F4E0D87E418055E766656F84927D4B77CDFEE068CDB7BF709D2BFFDCBC0E40DD3FA75B56D09D95054B3C94754B3F659AE56EFCF6A4FF6079FEC27540BFE08A37817E41886B7D005776A41D6177962DAD84AC6EAE859A3B6683E2A97A6E0ECF3F80E2A97A7E1A54DFDCF50B9427EF0A023C3F0D76786E7C0AAA6FEEBAAF8A45FC8F2742CDBDB30188989DE63B0070ABE18C03C524975643F1543D1FA82EA2C3134659D4DD2AAE50B6DD23CEEDDAD56DFC27A07DFDCE2D6FECAB828E37F657000F44F8CDB6A3CF859309EAEF03C0EC83357AAF6869AC0070C474AA4EA2997DDF0C30FBE06AA0FD0D3DF537EF1EFD2D99569DB1EC493B00569A5FF4FD09D8B88967B610840D6FBB7681326A7357287F935AA5258CDAFAB6FB0FA08C9A1B0BCFE2C75B9D6BE5ADBE460047FCD1A704ED080E68F378F4760B4B80326A793C081BB696E917F8EE4DF16E561D6AF6539E0320243B8B1321B8C684D9EC8AC33526770C4C3E6214CBCE037A0226A70A26EF087D905919AF0078507FFF04002129B30F4725C0EC1D42082E494E76F8644730D36A16A493535E144844FB61260A4E09330EC08923016668C185EB8F5A82B2F85D643023893E9088E281B00BAC8FBC1855C297D3E6C1963910B5269288901A922D78C09E6935FB9B8BB95B0FD44FE3E478E1DD6FCCBA71F524228034744166C6890DA89EDC6A54CAB6A5CDACF381C9D1A1F76E0D11A2E2B4F208BBC2929610B46FA8AE4E3F738712D4C3E69FADD29A38132E1548A0E24C0E4DC8632BA06458CD82B4D6D2B2BB2B4423EE186F98D2A7C95D8990453BB9901219924C48DD014CFC2FB83B23F135006B76D39471D6A903C58178F000C2BC2316A9020B4BD85595693562C97A203E9258E899CDCF639EAAF3D2B3C7DD14BE0600ABABF666C9A725B83B0A53A3CCD57E13536253C7BE04A03CE6F9A52575D66D1E8F4FBFA46A18E41D0F9ABD008C494C3583D5BDD9F34424D3EAA9EF2D49D2D685579E7327F6297F75EDFCF46C7BF1D00A117AB2E09CFB5D5AC2A37937252F32531D607B38EF9AFB01AEBD3D6FC87D273D904C045838F82E11B5155FE2031E1B73CE1D15195663D153DF39A5BF248B2937DFEDEBB1F5E48A5D55D5D59B4E59ACC3BBE50C5A4F4FFA32AB624D326710E9DCDAAC8A8DDAF0037D72910CA9D7206D9036481BA40DD2066903060C183060C0800103060CFCBF854D4D34384470FAD16F28539E1E99D65CEFB4A9F17A875F55E3F8DD408EAAAAA11C5555439436BF8FD89C5B790C463635387BE371ABCDB2B90C00F3EC9F6451CDBA6442B58F36794B19059259A4B54B922FD42E49BE90BC74C2DB3E79E984B77D7876010F2F7CF65F7AA3A55BC90D0320B76553BE7F087399AD95DCB0DF0DA2A581556E6095DBFE39FDABEC9FD3BF0A6CC76026F2F1DE79B04D0CA6B0B99A17723B286CAEC6BEBDB2A2B0B91AFBBF55D643617335E5F8133B5D5AE930F100A00E4E37268D0A691BED07BDA8485F4242EA1DD2A6164B60EA61EFBDC7ED96C0D4C35ECEDBB82030F5B09781B55BBC7260EA4660AE79EE32465700718F46F360BA31F902B1891DBA6A462876E8AA19213DC7ACF64AF730F92B72EBC80DC86DE4D6911BB01F4FEEFB0772EBB481686913A4CDEB774E1695FCAADAA9778F9C8FB4CD3DEA67DA06DB3180E175BDD1D2FDE2B30203B691700362AF68FB2F893B066C0360609322515701C4EF08CEF6D14F191BA040B2E9C6AE79576CE4392356FBE4392356FBF47E336F69AFB4B4798977B4AAAA51B98DD1AAAA46EDE1E43EA76A741950CED393CDA90135FC235BAB3E06FDEE896A14FC5EC0EF25B78E9C706E1D3961ADA5737A741CA6FA74FC97335AE64A920310FB68720F2D620E2F7A294D90DA9F3135BE2C39605C101817C4F401EA60D307A961FAE413BD439ACE6B8F148850F77D32A4A24DC9D81D48278C2DCB706D93B812A9257125528B761DEF0FF428E9D40FD14A6DEEF905D6FC85D0757E8135FF57A93D28E78F19F81AEC281E06A30B2AC0B2E99EA2B527D88A699BD875578D5B1FBBEEAA71EB45C6FDBFEE9DDB782B4C2C2B6C6E70C96D50D8DCE0B28792FB9C2A6D9EBEAC79FE71317B5816373738459F5655D525668FFE55FA665A73BD436C2E56D54E3E50D50E63A964C0800103060C183060E0072C8B255C5855EF37E6DF0730EDC03CA170AD52D598AE83C9AA1A4FB350E9E2823A5BDCC9E8B2DE63AB3F6EA96ECC91B4747BF8C4139B7E63AB50B82649D698AE83D1A6C962C242958FC94B3179FBA4A53B435CBECAFB4DA4A71D7C156EF09154B8725A751D4C6ECDB0603D6EF9025BD7477DD2D2F1AB7187A1B4F95921842DBA224669F3B322D3FAF4EF36B8F106530AD780B5291D2CC342D7A6917B4139AF579B3849FA138683BCE4CE412E06D66EF1CEFAD0222FB97390CBDC019877465F0B71E1F42365BAC2551C4AEA60394796A75960D9334B2151EDEC0BD26C348D0379CFBF5778B9F08DA7DDE81100F51C802185A3160197EC02FE5E90361F8045F9F9CE94051AFF2E08047D7D42FAE3095FC28010CD4208438BC4ED80D235793C484FED7DD521142EF32D115D0753E6EC9FEF4B59E05CB313D8757B9F90DE3A623DE44551E1CF22454400E2851FBFE82612E67317F6B141B0F41719052180779C290BCC9CE903BAE43E211D9F10861607525255161100FEB4CBEBD5929F7C02907789F471C13453F10961904B7F3715486C72F60569E5AC201C71F313D15BD5F408D0E422ECE2A2B050B87E2AD22DC301F869246561E0DEAE430E6095B72F48270600CAA5575788C653873A944BAFAE089A3B00FBC38E29415E7C64586954285CEE103CE4C312839C9F0F2D0FA62CCC5CAA3E5306ECEF13D2A2B166D61C12771565F591B4C8F98F9606E8A8DFF10B7C7EB503B4E9D9B21694FCFAFAD48D482E0DB2660AD015F9EED726F79D512B25C303D2800103060C183060E0070C9B9AD8E9EE9E68DD1D6E10B9A90573BACB985F3DF05C32592B53DEDD484DEA10F17A47CF2E4DDBCCF3E775CF1BF9AFAEBF5CB960E4A197B33F549BBC25A53A3DDD132DDD8AF50437B189A77864F2BB91BFE0E40D7C92964E3AA47D6BD2690F011296CA7D5E165F76A00C4BE53E6F4ED57AB5034BE53EAF2D6C538F2CC7BEBDB2A21C79DDDE5447526A9D1436578B2E245C156CEA91E5E437BD0F606DAA4658150F9E83E5757BDDE437BD6F9FA98616E7EFA1B0B91AF3CA03BED3242DDDB88BC2F65B57204D2F5A4861FBAD2BDA7DE3A53025ED456EE8942E1DE4E0BC8D1550F2D98CE5C27F02E07397F02D033497B24EE9D2418E190F2F770125B5AF388555718C83259FCD58CE8C87975FBA44724BF78CB404A61EF6DAECF9234E7320AA31376B1DD4B0D4C74E119AE8A581B50E6D20DEE6B51F8772D662DA2348FBDDF0902FB78EDC4039B670D2A58CDBC66B3E4B6BB0740A5B9A43DA5A4C7BA4E3A27B2CF5097FB48B7DA73D10A51B9E95AE8FF2DFA8413E142100E9FA288034A725E84001A4EB497C225CC604741F33349732694E4BF0ECCF26037003B170D2563F65ECB2EB497CA26A990445DDA3F73A4FBB4F37C8A681AA3A13350A2204603A5B5C89D20237B46809D1547D4734CFAFB63B35192D59B47CF1AB80C90191A4AD3673FED7674354CB241125CFAFB63B3BF6349E6E9F463D96382C49122A20420089AF849EB126E2D43E11F315E91FF0B828ACFB98E92E65F29A8893CDD775FA20110567CA965E5964A26A7E69B1290F3D77BAA4A51FA9EFE96C4E0C45F50FCDA8EF611A9E12678A22499F323D10C505FBFD4E603B1667CA56B2F27EBF339A5625B1FAEBD3253DF2025E5E30E071114985DE5C303400B109C3F443BF357FACA2CF1E031E7F9BAEF30B6EAA103AD498124451F38AC193C240ED3C4F24CD96A86C5E317852F8EBF180A89B5F7CC959A747BA9FBAAC94CD390DFD4434157A2DA736049D7BDEACD24ABEF6B74B1FD482DB1A064D2456B6726E1040297BF11644D1F8A75B870581D76E991349B3252AC73FDD3A2CB866490844DDA6BB373C602CA40C183060C0800103060C7CAF21AF3CF26A76A25586BC55239ED47303C9EC92DD8D93605505F8D523737527325BAB88F6AC2C36EB8B61FFF18D8252A6175686BC75C2292DFDE7071F41FA87F140C1A8C7D2DDD00A463DD6B3A4E569738EFE2A9A659DC45369A7D799A9F7DDF9DB6D3BFDC87F3314606F2DC4BC0C0989BCBDB53D4BDA721894A0BC6EAF3B258B69EF52AEEBE63A06A8E27D4B6B5335D8C24CDFE745CF1F1D80D7B1347E2CF427A7EE86A6477B94F436D0142F697AD1424ADA8BDCDABB94D59BCB325CC700E9A02530F5B0B7A4F61527202FBE798CE6670642DBB97ADB0E27C04551DD890C2DDA93B8A202108A972E8B25DFA1CCA9B387C90D717985B983F4F72DD760E9C416CEADE2B6406E88CB2BC0244ECBEF9E5886DF3DAC03DBB139A63F3EE4B5B58A68CF0E44928A972E8B25DFA58C8F20DD758C7ECA58F1BEE50DC4C28025CC3B9A9F998E71A1752ED8B6E7D7E84E64E8D11E247DD09154BCD4A8268609BDAB055522DD758C36737E34CFAFB60F764004C883849E9FF84F007938B19F4141FE14742732F4680F928E1524452B5517C384DE95275E3C4C731D43CBFB51149C400B9892F9EF3900B34D6DB7000D871C9A1399500B0F397A96F440906F4B2A5EEA7B0E5DB392313775771DD3F2B663710231176392F9756530E1DC6592B41160B94F73221358EEEB51D2CA730BCE79DCF1D6FCB14A4A0C137AD77F153FB23EC3758CA49E553BCF130194BB86DC9AF433ABB9BFE0BAB9C561D8EE055E1FAF399109BC3EBE176EE3629E8617F479BAC165DFBEAE1EA952CCD3DAEC01E2FD4AEBEE1D35D8C24C3F521A48CEE325BB1B27553AE0F280DF8DDC6E3B0668B307727BDFAC49ECA13370A1647CE4CF800103060C183060E0070C9BAA76FB2324D2DC8F100E29B6F412E599256AFA926DCA4945FC3DCF495E9EFC7E2F4D3314B0F9670869F3CA03BE4557C4C427C4AC4DD5647A860905AC34C04AA8A15C9448954DFBF0585F92B6D9F347CCFAD0223E215652FB8AB39B6798A680E928A97DC5995676AE79EEB2BE1F88818B7D7A9FB6B4698E5E199E619A02A6B7F41A2C9D6965C1D2D6F703B14C77DB1AD8A4489AA35737CF304B987792911B8885536545A8EFBB87EEB6F5E80A8BE6E87576B73FB2CA8344B2A60322A9B23CBAC2F25D0C44DD6DABE82593E6E895E919A6296044C1A9B982A5CA667E78AC0FA7BCC4EAAF55B44F880947AF74CF3034058CAF9C16279A2A962C9BF6E1B15E47FA25CD1FF4E959EA50477447F130A89D571FE1ADF9F58AF898988072D7825B5BA99B5C1F412B912ABBA378D877701B3F665DB7CF2755C6C427C4ACBB77D488795ACC1EAA1AD31530EBEE576A28172552652F6B9E7F9C331469BFBE7D2F6EE3DFCBB5870103060C183060C0800103060C18F861E3FF009944E0E7A120772F0000000049454E44AE426082');

-- Los precios y costos se toman de vw_CostoPrecioPostre (los mismos que calcula el Panel).
-- El anticipo es el 50 % (PorcentajeAnticipo) y el costo de envío va DENTRO de MontoTotal.
-- Las fechas se calculan a partir de HOY, así la agenda siempre tiene pedidos:
--   #1  En proceso de aceptación (recoger, entrega en 4 días)
--   #2  Aprobado a domicilio: pago en efectivo validado + transferencia PENDIENTE con archivo de comprobante
--   #3  Pagado (anticipo por transferencia validado, entrega en 2 días)
--   #4  En preparación (entrega hoy)
--   #5  Pedido terminado y liquidado (entrega ayer: aparece «Atrasado»)
--   #6  Solicitud con datos incompletos y fecha vencida (a domicilio sin dirección)
--   #7  Aprobado y cancelado, con reembolso pendiente
--   #8  Entregado a domicilio la semana pasada
--   #9  Solicitud dada de baja (duplicada de la #1)
--   #10 Aprobado con una transferencia RECHAZADA (comprobante ilegible)
--   #11 Entregado hace tres semanas (cliente frecuente: también tiene el #3)
-- Para buscar en «Mis pedidos» de la web usa el folio y el teléfono del cliente (477...).

DROP PROCEDURE IF EXISTS poblar_pedidos;

DELIMITER $$
CREATE PROCEDURE poblar_pedidos()
BEGIN
  DECLARE vCliente INT;
  DECLARE vDireccion INT;
  DECLARE vFolio INT;
  DECLARE vPago INT;
  DECLARE vMaria INT;
  DECLARE vLucia INT;
  DECLARE vTotal DECIMAL(10,2);
  -- Postres del catálogo y su precio y costo
  DECLARE vVainilla INT;
  DECLARE vTresLeches INT;
  DECLARE vMosaico INT;
  DECLARE vChocoFresa INT;
  DECLARE vGelFresa INT;
  DECLARE pVainilla DECIMAL(10,2);
  DECLARE cVainilla DECIMAL(10,2);
  DECLARE pTresLeches DECIMAL(10,2);
  DECLARE cTresLeches DECIMAL(10,2);
  DECLARE pMosaico DECIMAL(10,2);
  DECLARE cMosaico DECIMAL(10,2);
  DECLARE pChocoFresa DECIMAL(10,2);
  DECLARE cChocoFresa DECIMAL(10,2);
  DECLARE pGelFresa DECIMAL(10,2);
  DECLARE cGelFresa DECIMAL(10,2);

  SELECT IdUsuario INTO vMaria FROM Usuario WHERE NombreUsuario = 'maria';
  SELECT IdUsuario INTO vLucia FROM Usuario WHERE NombreUsuario = 'lucia';

  SELECT p.IdPostre, v.PrecioVenta, v.CostoIngredientes INTO vVainilla, pVainilla, cVainilla
  FROM Postre p JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre WHERE p.NombrePostre = 'Pastel clásico de vainilla' LIMIT 1;
  SELECT p.IdPostre, v.PrecioVenta, v.CostoIngredientes INTO vTresLeches, pTresLeches, cTresLeches
  FROM Postre p JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre WHERE p.NombrePostre = 'Pastel tres leches de fiesta' LIMIT 1;
  SELECT p.IdPostre, v.PrecioVenta, v.CostoIngredientes INTO vMosaico, pMosaico, cMosaico
  FROM Postre p JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre WHERE p.NombrePostre = 'Gelatina mosaico familiar' LIMIT 1;
  SELECT p.IdPostre, v.PrecioVenta, v.CostoIngredientes INTO vChocoFresa, pChocoFresa, cChocoFresa
  FROM Postre p JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre WHERE p.NombrePostre = 'Pastel de chocolate con fresa' LIMIT 1;
  SELECT p.IdPostre, v.PrecioVenta, v.CostoIngredientes INTO vGelFresa, pGelFresa, cGelFresa
  FROM Postre p JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre WHERE p.NombrePostre = 'Gelatina de fresa con chantilly' LIMIT 1;

  IF vMaria IS NULL OR vLucia IS NULL OR vVainilla IS NULL OR vTresLeches IS NULL OR vMosaico IS NULL
     OR vChocoFresa IS NULL OR vGelFresa IS NULL THEN
    SELECT 'Faltan usuarios o postres: ejecuta este archivo completo, desde el principio' AS Aviso;
  ELSEIF EXISTS (SELECT 1 FROM Cliente WHERE TelefonoWhatsApp = '4771234501') THEN
    SELECT 'Los pedidos de prueba ya existen: no se agregó nada' AS Aviso;
  ELSE

    -- #1 En proceso de aceptación ------------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Ana María López', '4771234501', 'ana.lopez@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    SET vTotal = pVainilla + pMosaico;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 15 MINUTE, CURDATE() + INTERVAL 4 DAY, '14:00', 'Recoger en tienda', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'En proceso de aceptacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vVainilla, 1, '¡Feliz cumpleaños, mamá!', 'Letras en color rosa', pVainilla, cVainilla),
      (vFolio, vMosaico, 1, NULL, NULL, pMosaico, cMosaico);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 15 MINUTE);
    INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, FechaHoraEnvio, ResultadoEnvio, MotivoFallo) VALUES
      (vFolio, 'Pedido registrado', 'WhatsApp', '4771234501', CONCAT('Recibimos tu pedido #', vFolio, '. Te avisaremos cuando sea aprobado.'), NOW() - INTERVAL 15 MINUTE, 'Enviado', NULL),
      (vFolio, 'Pedido registrado', 'Correo', 'ana.lopez@ejemplo.com', CONCAT('Recibimos tu pedido #', vFolio, '. Te avisaremos cuando sea aprobado.'), NOW() - INTERVAL 15 MINUTE, 'Pendiente', NULL);

    -- #2 Aprobado a domicilio, con transferencia pendiente y archivo de comprobante --
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Carlos Ramírez', '4771234502', 'carlos.ramirez@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Direccion (IdCliente, Calle, NumeroExterior, NumeroInterior, Colonia, CodigoPostal, Referencias)
      VALUES (vCliente, 'Blvd. Campestre', '1520', '3B', 'Jardines del Moral', '37160', 'Edificio azul, tocar el timbre 3');
    SET vDireccion = LAST_INSERT_ID();
    SET vTotal = pTresLeches * 2 + 50.00;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 2 DAY, CURDATE() + INTERVAL 1 DAY, '11:00', 'Envio a domicilio', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'Aprobado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO EntregaDomicilio (Folio, IdDireccion) VALUES (vFolio, vDireccion);
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vTresLeches, 2, 'Felicidades Lupita', NULL, pTresLeches, cTresLeches);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 2 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 1 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, vLucia, 500.00, NOW() - INTERVAL 20 HOUR, 'Efectivo', NULL, NULL, 'Validado');
    -- El segundo pago lo subió el cliente desde la web: sin enlace, con el archivo en ComprobanteArchivo (Parte 7)
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, NULL, 627.75, NOW() - INTERVAL 1 HOUR, 'Transferencia', 'SPEI-778812', NULL, 'Pendiente');
    SET vPago = LAST_INSERT_ID();
    INSERT INTO ComprobanteArchivo (IdPago, NombreArchivo, TipoContenido, TamanoBytes, Contenido, FechaSubida)
      VALUES (vPago, 'comprobante-spei-778812.png', 'image/png', LENGTH(@poblar_png), @poblar_png, NOW() - INTERVAL 1 HOUR);
    INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, FechaHoraEnvio, ResultadoEnvio, MotivoFallo) VALUES
      (vFolio, 'Pedido registrado', 'WhatsApp', '4771234502', CONCAT('Recibimos tu pedido #', vFolio, '.'), NOW() - INTERVAL 2 DAY, 'Enviado', NULL),
      (vFolio, 'Pedido aprobado', 'WhatsApp', '4771234502', CONCAT('Tu pedido #', vFolio, ' fue aprobado. Anticipo: $', ROUND(vTotal * 0.5, 2)), NOW() - INTERVAL 1 DAY, 'Enviado', NULL),
      (vFolio, 'Pago validado', 'WhatsApp', '4771234502', CONCAT('Validamos tu pago de $500.00 del pedido #', vFolio, '.'), NOW() - INTERVAL 20 HOUR, 'Fallido', 'El número no tiene WhatsApp (prueba)');

    -- #3 Pagado --------------------------------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Laura Gómez', '4771234503', 'laura.gomez@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    SET vTotal = pMosaico * 2;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 3 DAY, CURDATE() + INTERVAL 2 DAY, '16:30', 'Recoger en tienda', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'Pagado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vMosaico, 2, NULL, 'Una de las dos sin nuez', pMosaico, cMosaico);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 3 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
      (vFolio, vLucia, 'Aprobado', 'Pagado', NOW() - INTERVAL 2 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, vLucia, ROUND(vTotal * 0.5, 2), NOW() - INTERVAL 2 DAY, 'Transferencia', 'SPEI-551200',
              'https://ejemplo.com/comprobantes/551200.jpg', 'Validado');

    -- #4 En preparación (se entrega hoy) ---------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Pedro Sánchez', '4771234504', NULL);
    SET vCliente = LAST_INSERT_ID();
    SET vTotal = pChocoFresa;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 4 DAY, CURDATE(), '18:00', 'Recoger en tienda', 'Pago total',
              vTotal, vTotal, 'En preparacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vChocoFresa, 1, 'Feliz día, papá', NULL, pChocoFresa, cChocoFresa);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 4 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 4 DAY + INTERVAL 1 HOUR),
      (vFolio, vLucia, 'Aprobado', 'Pagado', NOW() - INTERVAL 3 DAY),
      (vFolio, vMaria, 'Pagado', 'En preparacion', NOW() - INTERVAL 2 HOUR);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, vLucia, vTotal, NOW() - INTERVAL 3 DAY, 'Efectivo', NULL, NULL, 'Validado');

    -- #5 Pedido terminado (atrasado: la entrega era ayer) ------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Sofía Hernández', '4771234505', 'sofia.h@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    SET vTotal = pTresLeches;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 6 DAY, CURDATE() - INTERVAL 1 DAY, '12:00', 'Recoger en tienda', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'Pedido terminado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vTresLeches, 1, 'Feliz aniversario', NULL, pTresLeches, cTresLeches);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 6 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 6 DAY + INTERVAL 1 HOUR),
      (vFolio, vLucia, 'Aprobado', 'Pagado', NOW() - INTERVAL 5 DAY),
      (vFolio, vMaria, 'Pagado', 'En preparacion', NOW() - INTERVAL 2 DAY),
      (vFolio, vMaria, 'En preparacion', 'Pedido terminado', NOW() - INTERVAL 1 DAY);
    -- Anticipo + liquidación = total exacto
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion) VALUES
      (vFolio, vLucia, ROUND(vTotal * 0.5, 2), NOW() - INTERVAL 5 DAY, 'Transferencia', 'SPEI-440021', NULL, 'Validado'),
      (vFolio, vLucia, vTotal - ROUND(vTotal * 0.5, 2), NOW() - INTERVAL 1 DAY, 'Efectivo', NULL, NULL, 'Validado');

    -- #6 Datos incompletos y fecha vencida (a domicilio sin dirección) -----------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Rosa Delgado', '4771234506', NULL);
    SET vCliente = LAST_INSERT_ID();
    SET vTotal = pMosaico;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 5 DAY, CURDATE() - INTERVAL 1 DAY, '10:00', 'Envio a domicilio', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'En proceso de aceptacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vMosaico, 1, NULL, NULL, pMosaico, cMosaico);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 5 DAY);

    -- #7 Aprobado y cancelado con reembolso pendiente ---------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Jorge Méndez', '4771234507', NULL);
    SET vCliente = LAST_INSERT_ID();
    SET vTotal = pVainilla * 2;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 4 DAY, CURDATE() + INTERVAL 5 DAY, '13:00', 'Recoger en tienda', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'Aprobado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vVainilla, 2, NULL, NULL, pVainilla, cVainilla);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 4 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 3 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, vLucia, 100.00, NOW() - INTERVAL 3 DAY, 'Efectivo', NULL, NULL, 'Validado');
    INSERT INTO Cancelacion (Folio, IdUsuario, MotivoCancelacion, FechaHoraCancelacion, MontoReembolso, EstadoReembolso)
      VALUES (vFolio, vMaria, 'Solicitud del cliente: cambió la fecha de su evento', NOW() - INTERVAL 1 DAY, 100.00, 'Pendiente');
    INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, FechaHoraEnvio, ResultadoEnvio, MotivoFallo) VALUES
      (vFolio, 'Pedido cancelado', 'WhatsApp', '4771234507', CONCAT('Tu pedido #', vFolio, ' fue cancelado. Te reembolsaremos $100.00.'), NOW() - INTERVAL 1 DAY, 'Enviado', NULL);

    -- #8 Entregado a domicilio la semana pasada ----------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Mariana Torres', '4771234508', 'mariana.t@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Direccion (IdCliente, Calle, NumeroExterior, NumeroInterior, Colonia, CodigoPostal, Referencias)
      VALUES (vCliente, 'Calle Hacienda de Arbide', '45', NULL, 'Arbide', '37360', 'Portón negro, frente al parque');
    SET vDireccion = LAST_INSERT_ID();
    SET vTotal = pGelFresa * 2 + 60.00;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 12 DAY, CURDATE() - INTERVAL 7 DAY, '17:00', 'Envio a domicilio', 'Pago total',
              vTotal, vTotal, 'Entregado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO EntregaDomicilio (Folio, IdDireccion) VALUES (vFolio, vDireccion);
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vGelFresa, 2, NULL, NULL, pGelFresa, cGelFresa);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 12 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 12 DAY + INTERVAL 1 HOUR),
      (vFolio, vLucia, 'Aprobado', 'Pagado', NOW() - INTERVAL 11 DAY),
      (vFolio, vMaria, 'Pagado', 'En preparacion', NOW() - INTERVAL 8 DAY),
      (vFolio, vMaria, 'En preparacion', 'Pedido terminado', NOW() - INTERVAL 7 DAY - INTERVAL 3 HOUR),
      (vFolio, vMaria, 'Pedido terminado', 'Entregado', NOW() - INTERVAL 7 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, vLucia, vTotal, NOW() - INTERVAL 11 DAY, 'Transferencia', 'SPEI-300118', NULL, 'Validado');
    INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, FechaHoraEnvio, ResultadoEnvio, MotivoFallo) VALUES
      (vFolio, 'Cambio de estado', 'Correo', 'mariana.t@ejemplo.com', CONCAT('Tu pedido #', vFolio, ' fue entregado. ¡Gracias por tu compra!'), NOW() - INTERVAL 7 DAY, 'Enviado', NULL);

    -- #9 Solicitud dada de baja (duplicada de la #1) ------------------------------
    SELECT IdCliente INTO vCliente FROM Cliente WHERE TelefonoWhatsApp = '4771234501';
    SET vTotal = pVainilla;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 14 MINUTE, CURDATE() + INTERVAL 4 DAY, '14:00', 'Recoger en tienda', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'En proceso de aceptacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vVainilla, 1, NULL, NULL, pVainilla, cVainilla);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 14 MINUTE);
    INSERT INTO BajaPedido (Folio, MotivoBaja, FechaHoraBaja, IdUsuario)
      VALUES (vFolio, 'Pedido duplicado: el cliente envió la solicitud dos veces', NOW() - INTERVAL 5 MINUTE, vMaria);

    -- #10 Aprobado con una transferencia rechazada --------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Fernanda Ortiz', '4771234509', 'fer.ortiz@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Direccion (IdCliente, Calle, NumeroExterior, NumeroInterior, Colonia, CodigoPostal, Referencias)
      VALUES (vCliente, 'Calle Prado Verde', '118', NULL, 'Prado Hermoso', '37238', 'Casa blanca de dos pisos');
    SET vDireccion = LAST_INSERT_ID();
    SET vTotal = pChocoFresa + 30.00;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 1 DAY, CURDATE() + INTERVAL 6 DAY, '15:00', 'Envio a domicilio', 'Anticipo',
              vTotal, ROUND(vTotal * 0.5, 2), 'Aprobado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO EntregaDomicilio (Folio, IdDireccion) VALUES (vFolio, vDireccion);
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vChocoFresa, 1, 'Feliz graduación, Diego', 'Sin velas', pChocoFresa, cChocoFresa);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 1 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 20 HOUR);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion, MotivoRechazo)
      VALUES (vFolio, vLucia, ROUND(vTotal * 0.5, 2), NOW() - INTERVAL 10 HOUR, 'Transferencia', 'SPEI-990045',
              'https://ejemplo.com/comprobantes/990045.jpg', 'Rechazado', 'Comprobante ilegible');
    INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, FechaHoraEnvio, ResultadoEnvio, MotivoFallo) VALUES
      (vFolio, 'Pedido aprobado', 'WhatsApp', '4771234509', CONCAT('Tu pedido #', vFolio, ' fue aprobado.'), NOW() - INTERVAL 20 HOUR, 'Enviado', NULL),
      (vFolio, 'Pago rechazado', 'WhatsApp', '4771234509', CONCAT('No pudimos validar tu pago del pedido #', vFolio, ': comprobante ilegible.'), NOW() - INTERVAL 9 HOUR, 'Enviado', NULL);

    -- #11 Entregado hace tres semanas (Laura Gómez, cliente frecuente) -------------
    SELECT IdCliente INTO vCliente FROM Cliente WHERE TelefonoWhatsApp = '4771234503';
    SET vTotal = pVainilla + pGelFresa;
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 25 DAY, CURDATE() - INTERVAL 21 DAY, '12:30', 'Recoger en tienda', 'Pago total',
              vTotal, vTotal, 'Entregado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, vVainilla, 1, 'Bienvenido, Mateo', NULL, pVainilla, cVainilla),
      (vFolio, vGelFresa, 1, NULL, NULL, pGelFresa, cGelFresa);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 25 DAY),
      (vFolio, vMaria, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 25 DAY + INTERVAL 2 HOUR),
      (vFolio, vLucia, 'Aprobado', 'Pagado', NOW() - INTERVAL 24 DAY),
      (vFolio, vMaria, 'Pagado', 'En preparacion', NOW() - INTERVAL 22 DAY),
      (vFolio, vMaria, 'En preparacion', 'Pedido terminado', NOW() - INTERVAL 21 DAY - INTERVAL 2 HOUR),
      (vFolio, vMaria, 'Pedido terminado', 'Entregado', NOW() - INTERVAL 21 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, vLucia, vTotal, NOW() - INTERVAL 24 DAY, 'Efectivo', NULL, NULL, 'Validado');

    SELECT 'Pedidos de prueba agregados' AS Resultado;
  END IF;
END$$
DELIMITER ;

CALL poblar_pedidos();
DROP PROCEDURE IF EXISTS poblar_pedidos;
SET @poblar_png = NULL;


-- =====================================================================
-- 7. ESCENARIO PARA REPORTES FINANCIEROS (Módulo 6, agosto 2026)
-- =====================================================================
-- RESULTADO ESPERADO en "Reportes Financieros" con el periodo 01/08/2026 – 31/08/2026:
--   Ingresos y gastos
--     Anticipos         $750.00   (pedido A $500 + pedido C $250)
--     Liquidaciones     $500.00   (pedido A)
--     Pagos totales     $300.00   (pedido B)
--     Reembolsos       -$250.00   (pedido C, cancelado con reembolso realizado)
--     INGRESOS        $1,300.00
--     Compras           $800.00
--     Mermas             $50.00
--     GASTOS            $850.00
--     UTILIDAD BRUTA    $450.00
--     Costo de producción (informativo) $120.00
--     Pagos por reembolsar (aparte)     $200.00   (pedido D, cancelado con reembolso pendiente)
--     Movimientos por revisar: 3 (pago del pedido E dado de baja, compra con costo $0, merma con cantidad positiva)
--   Más vendidos
--     1. Pastel clásico de vainilla     2 unidades   $1,000.00   costo $400.00   utilidad $600.00
--     2. Pastel tres leches de fiesta   1 unidad       $300.00   costo $120.00   utilidad $180.00
--     Total: 2 pedidos, 3 unidades, $1,300.00, utilidad $780.00, margen 60 %
--     1 venta no incluida por datos incompletos (pedido G, sin precio)
DROP PROCEDURE IF EXISTS poblar_reportes;

DELIMITER $$
CREATE PROCEDURE poblar_reportes()
BEGIN
  DECLARE vCliente INT;
  DECLARE vFolio INT;
  DECLARE vPastel1 INT;
  DECLARE vPastel2 INT;
  DECLARE vGelatina INT;
  DECLARE vHarina INT;

  SELECT IdPostre INTO vPastel1 FROM Postre WHERE NombrePostre = 'Pastel clásico de vainilla' LIMIT 1;
  SELECT IdPostre INTO vPastel2 FROM Postre WHERE NombrePostre = 'Pastel tres leches de fiesta' LIMIT 1;
  SELECT IdPostre INTO vGelatina FROM Postre WHERE NombrePostre = 'Gelatina mosaico familiar' LIMIT 1;
  SELECT IdInsumo INTO vHarina FROM Insumo WHERE NombreInsumo = 'Harina de trigo' LIMIT 1;

  IF vPastel1 IS NULL OR vPastel2 IS NULL OR vGelatina IS NULL OR vHarina IS NULL THEN
    SELECT 'Faltan postres o insumos: ejecuta este archivo completo, desde el principio' AS Aviso;
  ELSEIF NOT EXISTS (SELECT 1 FROM Cliente WHERE TelefonoWhatsApp = '4779990001') THEN

    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Cliente de reportes', '4779990001', 'reportes@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();

    -- A: entregado. Anticipo $500 el 02/08 y liquidación $500 el 10/08 ----------------
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-02 10:00:00', '2026-08-10', '12:00', 'Recoger en tienda', 'Anticipo', 1000.00, 500.00, 'Entregado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vPastel1, 2, 500.00, 200.00);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', '2026-08-02 10:00:00'),
             (vFolio, NULL, 'Pedido terminado', 'Entregado', '2026-08-10 12:00:00');
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, EstadoValidacion) VALUES
      (vFolio, NULL, 500.00, '2026-08-02 11:00:00', 'Efectivo', NULL, 'Validado'),
      (vFolio, NULL, 500.00, '2026-08-10 12:00:00', 'Efectivo', NULL, 'Validado');

    -- B: pagado con pago total $300 el 04/08 (más un pago rechazado y uno pendiente que NO cuentan)
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-04 09:00:00', '2026-08-20', '15:00', 'Recoger en tienda', 'Pago total', 300.00, 300.00, 'Pagado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vPastel2, 1, 300.00, 120.00);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', '2026-08-04 09:00:00');
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, EstadoValidacion, MotivoRechazo) VALUES
      (vFolio, NULL, 300.00, '2026-08-04 09:30:00', 'Transferencia', 'REP-0001', 'Validado', NULL),
      (vFolio, NULL, 300.00, '2026-08-04 09:10:00', 'Transferencia', 'REP-0000', 'Rechazado', 'Comprobante ilegible'),
      (vFolio, NULL, 50.00,  '2026-08-05 09:10:00', 'Transferencia', 'REP-0002', 'Pendiente', NULL);

    -- C: cancelado con reembolso REALIZADO. Anticipo $250 el 05/08, reembolso $250 el 07/08 ---------
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-05 10:00:00', '2026-08-25', '12:00', 'Recoger en tienda', 'Anticipo', 500.00, 250.00, 'Pagado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vPastel1, 1, 500.00, 200.00);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, EstadoValidacion)
      VALUES (vFolio, NULL, 250.00, '2026-08-05 10:30:00', 'Efectivo', NULL, 'Validado');
    INSERT INTO Cancelacion (Folio, IdUsuario, MotivoCancelacion, FechaHoraCancelacion, MontoReembolso, EstadoReembolso)
      VALUES (vFolio, NULL, 'El cliente cambió de fecha (semilla de reportes)', '2026-08-07 10:00:00', 250.00, 'Realizado');

    -- D: cancelado con reembolso PENDIENTE. Pago $200 el 06/08 (se muestra aparte) ---------------
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-06 10:00:00', '2026-08-26', '12:00', 'Recoger en tienda', 'Anticipo', 400.00, 200.00, 'Pagado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vGelatina, 1, 400.00, 150.00);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, EstadoValidacion)
      VALUES (vFolio, NULL, 200.00, '2026-08-06 10:30:00', 'Efectivo', NULL, 'Validado');
    INSERT INTO Cancelacion (Folio, IdUsuario, MotivoCancelacion, FechaHoraCancelacion, MontoReembolso, EstadoReembolso)
      VALUES (vFolio, NULL, 'Pedido duplicado por el cliente (semilla de reportes)', '2026-08-08 10:00:00', 200.00, 'Pendiente');

    -- E: dado de baja, con un pago validado de $100 el 08/08 (va a «Movimientos por revisar») ----
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-08 10:00:00', '2026-08-28', '12:00', 'Recoger en tienda', 'Anticipo', 300.00, 100.00, 'Pagado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vPastel2, 1, 300.00, 120.00);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, EstadoValidacion)
      VALUES (vFolio, NULL, 100.00, '2026-08-08 10:30:00', 'Efectivo', NULL, 'Validado');
    INSERT INTO BajaPedido (Folio, MotivoBaja, FechaHoraBaja, IdUsuario)
      VALUES (vFolio, 'Pedido de prueba (semilla de reportes)', '2026-08-09 10:00:00', NULL);

    -- F: aprobado sin pago: NO entra a ninguno de los dos reportes --------------------------
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-12 10:00:00', '2026-08-30', '12:00', 'Recoger en tienda', 'Anticipo', 500.00, 250.00, 'Aprobado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vPastel1, 1, 500.00, 200.00);

    -- G: entregado pero con la venta incompleta (sin precio): se excluye y se avisa ----------
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, '2026-08-15 10:00:00', '2026-08-16', '12:00', 'Recoger en tienda', 'Pago total', 0.00, 0.00, 'Entregado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, PrecioUnitario, CostoUnitario) VALUES (vFolio, vGelatina, 1, 0.00, 0.00);

    -- Movimientos de inventario de agosto (solo para el reporte; no cambian los lotes) ----------
    INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, Folio, TipoMovimiento, CantidadMovimiento, Motivo, FechaMovimiento, CostoMovimiento) VALUES
      (vHarina, NULL, NULL, 'Compra',      10.000, 'Compra de agosto (semilla de reportes)',            '2026-08-03 09:00:00', 800.00),
      (vHarina, NULL, NULL, 'Compra',       5.000, 'Compra sin costo capturado (semilla de reportes)',  '2026-08-03 09:30:00',   0.00),
      (vHarina, NULL, NULL, 'Merma',       -1.000, 'Bulto húmedo (semilla de reportes)',                '2026-08-09 09:00:00',  50.00),
      (vHarina, NULL, NULL, 'Merma',        1.000, 'Merma capturada con signo equivocado (semilla)',    '2026-08-09 09:30:00',  20.00),
      (vHarina, NULL, NULL, 'Produccion',  -2.000, 'Producción de agosto (semilla de reportes)',        '2026-08-10 08:00:00', 120.00);
  END IF;
END$$
DELIMITER ;

CALL poblar_reportes();
DROP PROCEDURE IF EXISTS poblar_reportes;


-- =====================================================================
-- Resumen: cuántos datos quedaron
-- =====================================================================
SELECT 'Base poblada' AS Resultado,
       (SELECT COUNT(*) FROM Usuario) AS Usuarios,
       (SELECT COUNT(*) FROM CategoriaPostre) AS Categorias,
       (SELECT COUNT(*) FROM Caracteristica) AS Caracteristicas,
       (SELECT COUNT(*) FROM OpcionCaracteristica) AS Opciones,
       (SELECT COUNT(*) FROM ExclusionOpciones) AS Exclusiones,
       (SELECT COUNT(*) FROM Postre WHERE MostrarEnTienda = 1) AS ProductosEnTienda,
       (SELECT COUNT(*) FROM Insumo) AS Insumos,
       (SELECT COUNT(*) FROM Receta) AS Recetas,
       (SELECT COUNT(*) FROM Cliente) AS Clientes,
       (SELECT COUNT(*) FROM Pedido) AS Pedidos,
       (SELECT COUNT(*) FROM Pago) AS Pagos,
       (SELECT COUNT(*) FROM ComprobanteArchivo) AS ComprobantesArchivo,
       (SELECT COUNT(*) FROM Notificacion) AS Notificaciones,
       (SELECT COUNT(*) FROM CodigoPostalCobertura) AS CodigosPostales;

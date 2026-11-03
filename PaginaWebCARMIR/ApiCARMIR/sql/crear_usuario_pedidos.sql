-- =====================================================================
-- CARMIR · Módulo 2 (Portal de Pedidos) · Usuario de MySQL para GUARDAR pedidos
--
-- NO CAMBIA NINGUNA TABLA. Solo crea un usuario nuevo con permisos mínimos.
-- Requisito: la base creada con DBCarmir/CARMIR_BaseDeDatos.sql + DBCarmir/migraciones/MIGRACION_COMPLETA.sql
-- (la migración ya trae lo que el portal necesita: HistorialEstado.IdUsuario vacío, Notificacion "Pendiente",
--  y las tablas Configuracion, CodigoPostalCobertura, BajaPedido y ComprobanteArchivo).
--
-- Por qué un usuario aparte:
--   La API ya usa 'carmir_web' (solo lectura) para el catálogo. Para registrar pedidos necesita escribir,
--   pero solo en las tablas del pedido. Si alguien encontrara una falla en la página, con este usuario
--   NO podría borrar ni cambiar nada: solo puede AGREGAR registros nuevos en estas tablas
--   (y completar el correo de un cliente que no tenía). No puede leer Usuario ni las contraseñas.
--
-- Cómo usarlo (una sola vez, como root):
--   1. Cambia 'CambiaEstaContrasena2026' por una contraseña tuya (las dos veces).
--   2. Ejecuta el archivo en MySQL Workbench (abrir y el rayo) o:  mysql -u root -p < crear_usuario_pedidos.sql
--   3. Pon el mismo usuario y contraseña en ApiCARMIR/.env:
--        DB_ESCRITURA_USUARIO=carmir_pedidos
--        DB_ESCRITURA_CONTRASENA=la_contraseña_que_pusiste
-- Se puede ejecutar más de una vez sin problema.
-- =====================================================================
USE CARMIR_DB;

CREATE USER IF NOT EXISTS 'carmir_pedidos'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026';
CREATE USER IF NOT EXISTS 'carmir_pedidos'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026';

-- ---------------------------------------------------------------------
-- 1. LEER (dentro de la transacción del pedido)
--    precios vigentes, capacidad del día, cliente existente, cobertura y saldo para los comprobantes
-- ---------------------------------------------------------------------
GRANT SELECT ON CARMIR_DB.Configuracion          TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.CodigoPostalCobertura  TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.Cliente                TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.Direccion              TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.Pedido                 TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.EntregaDomicilio       TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.DetallePedido          TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.Postre                 TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.Pago                   TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.Cancelacion            TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.BajaPedido             TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.vw_CostoPrecioPostre   TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT SELECT ON CARMIR_DB.vw_SaldoPedido         TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';

-- ---------------------------------------------------------------------
-- 2. AGREGAR (solo INSERT: nunca puede borrar ni modificar lo que ya existe)
-- ---------------------------------------------------------------------
GRANT INSERT ON CARMIR_DB.Cliente                TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.Direccion              TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.Pedido                 TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.EntregaDomicilio       TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.DetallePedido          TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.Postre                 TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.PostreOpcion           TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.HistorialEstado        TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.Notificacion           TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
GRANT INSERT ON CARMIR_DB.Pago                   TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';
-- Foto o PDF del comprobante subido por el cliente (tabla de la PARTE 7 de MIGRACION_COMPLETA.sql).
-- Solo puede agregarlos: no puede leerlos ni borrarlos.
GRANT INSERT ON CARMIR_DB.ComprobanteArchivo     TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';

-- ---------------------------------------------------------------------
-- 3. MODIFICAR: una sola columna. Si un cliente que ya existía no tenía correo, se completa.
--    (El nombre y el teléfono de un cliente existente nunca se cambian desde la página.)
-- ---------------------------------------------------------------------
GRANT UPDATE (CorreoCliente) ON CARMIR_DB.Cliente TO 'carmir_pedidos'@'localhost', 'carmir_pedidos'@'127.0.0.1';

-- ---------------------------------------------------------------------
-- 4. (OPCIONAL) Datos para probar el envío a domicilio y la transferencia
--    Lo normal es capturarlos en el Panel > Configuración (cobertura y parámetros).
--    Sin códigos postales la página solo ofrece «Recoger en tienda» con éxito;
--    sin DatosBancarios solo ofrece pago en efectivo en la repostería.
--    Quita los dos guiones (--) de las líneas que quieras usar.
-- ---------------------------------------------------------------------
-- INSERT IGNORE INTO CodigoPostalCobertura (CodigoPostal, CostoEnvio) VALUES ('76000', 50.00), ('76010', 80.00);
-- UPDATE Configuracion SET Valor = 'BBVA · CLABE 012345678901234567 · A nombre de Delicias de Postres'
--  WHERE Clave = 'DatosBancarios';

-- Revisión: debe mostrar solo SELECT, INSERT y UPDATE (CorreoCliente) en las tablas de arriba
SHOW GRANTS FOR 'carmir_pedidos'@'localhost';

-- =====================================================================
-- CARMIR · Permisos en Amazon RDS (migración a AWS)
--
-- Se ejecuta UNA vez en RDS, DESPUÉS de INSTALACION_COMPLETA.sql, conectado como admin:
--   mysql -h basecarmir.clsq6iea8ubs.us-east-2.rds.amazonaws.com -u admin -p --ssl-mode=REQUIRED < PERMISOS_RDS.sql
--
-- Por qué hace falta:
--   Los 4 usuarios (carmir_web, carmir_pedidos, carmir_respaldo, carmir_panel) ya se crearon en el
--   paso 7 de la configuración de AWS, con host '172.31.%' (la red privada de la VPC) y REQUIRE SSL.
--   carmir_web, carmir_respaldo y carmir_panel tienen permiso sobre toda la base, así que no necesitan nada más.
--   carmir_pedidos solo puede tocar algunas tablas, y MySQL solo deja dar permisos por tabla cuando
--   las tablas ya existen: por eso se dan aquí, después de crear la base.
--   La sección 7.2 de INSTALACION_COMPLETA.sql busca carmir_pedidos en localhost y 127.0.0.1; en RDS
--   no existen y solo muestra un aviso: es normal.
--
-- Son los mismos permisos de ApiCARMIR/sql/crear_usuario_pedidos.sql, con el host de AWS.
-- Se puede ejecutar más de una vez sin problema.
-- =====================================================================
USE CARMIR_DB;

-- ---------------------------------------------------------------------
-- 1. LEER (dentro de la transacción del pedido)
-- ---------------------------------------------------------------------
GRANT SELECT ON CARMIR_DB.Configuracion            TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.CodigoPostalCobertura    TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.Cliente                  TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.Direccion                TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.Pedido                   TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.EntregaDomicilio         TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.DetallePedido            TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.Postre                   TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.Pago                     TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.Cancelacion              TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.BajaPedido               TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.vw_CostoPrecioPostre     TO 'carmir_pedidos'@'172.31.%';
GRANT SELECT ON CARMIR_DB.vw_SaldoPedido           TO 'carmir_pedidos'@'172.31.%';

-- ---------------------------------------------------------------------
-- 2. AGREGAR (solo INSERT: nunca puede borrar ni modificar lo que ya existe)
-- ---------------------------------------------------------------------
GRANT INSERT ON CARMIR_DB.Cliente                  TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.Direccion                TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.Pedido                   TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.EntregaDomicilio         TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.DetallePedido            TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.Postre                   TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.PostreOpcion             TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.HistorialEstado          TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.Notificacion             TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.Pago                     TO 'carmir_pedidos'@'172.31.%';
GRANT INSERT ON CARMIR_DB.ComprobanteArchivo       TO 'carmir_pedidos'@'172.31.%';

-- ---------------------------------------------------------------------
-- 3. MODIFICAR: solo el correo de un cliente que no lo tenía
-- ---------------------------------------------------------------------
GRANT UPDATE (CorreoCliente) ON CARMIR_DB.Cliente TO 'carmir_pedidos'@'172.31.%';

-- ---------------------------------------------------------------------
-- 4. Revisión
-- ---------------------------------------------------------------------
-- carmir_pedidos: solo SELECT, INSERT y UPDATE (CorreoCliente) en las tablas de arriba
SHOW GRANTS FOR 'carmir_pedidos'@'172.31.%';
-- Los demás usuarios deben seguir con sus permisos del paso 7
SHOW GRANTS FOR 'carmir_web'@'172.31.%';
SHOW GRANTS FOR 'carmir_respaldo'@'172.31.%';
SHOW GRANTS FOR 'carmir_panel'@'172.31.%';
-- Debe dar 25 tablas o más y las 5 vistas
SELECT COUNT(*) AS Tablas FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'CARMIR_DB' AND TABLE_TYPE = 'BASE TABLE';
SELECT TABLE_NAME AS Vistas FROM information_schema.VIEWS WHERE TABLE_SCHEMA = 'CARMIR_DB';
-- Debe dar la hora de León (zona America/Monterrey del grupo de parámetros)
SELECT NOW() AS HoraDelServidor;

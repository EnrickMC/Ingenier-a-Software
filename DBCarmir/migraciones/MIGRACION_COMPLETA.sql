-- =====================================================================
-- CARMIR · MIGRACIÓN COMPLETA (un solo script)
--
-- Reúne, en orden, TODOS los cambios que el Panel Administrativo necesita:
--   Parte 1 = PROPUESTA_V2__catalogo.sql     (Módulo 3 · Catálogo)
--   Parte 2 = PROPUESTA_V3__inventario.sql   (Módulo 5 · Inventario y Recetas)
--   Parte 3 = PROPUESTA_V4__pedidos.sql      (Módulo 4 · Pedidos y Producción)
--   Parte 4 = NUEVO: Reportes Financieros y Configuración (Módulo 6 y apartado Configuración)
--   Parte 5 = NUEVO: cuentas de WhatsApp y correo de las notificaciones (tabla ConfiguracionNotificacion)
--   Parte 6 = NUEVO: casilla «Sus opciones pueden tener receta» en las características (Caracteristica.PermiteReceta)
--   Parte 7 = NUEVO: foto o PDF del comprobante que sube el cliente desde la página web (tabla ComprobanteArchivo)
--   Parte 8 = NUEVO: se quita el método de pago «Depósito» (los pagos viejos pasan a «Transferencia»)
--
-- CÓMO USARLO
--   1. Ten tu base creada con DBCarmir/CARMIR_BaseDeDatos.sql (con o sin datos).
--   2. Ejecuta ESTE archivo completo (MySQL Workbench: abrir el archivo y el rayo ⚡,
--      o en consola:  mysql -u root -p < MIGRACION_COMPLETA.sql).
--   3. Listo. NO ejecutes además las PROPUESTA_V2, V3 ni V4: ya están aquí dentro.
--
-- Es seguro ejecutarlo más de una vez: cada cambio revisa primero si ya existe
-- (columnas, llaves foráneas, índices, tablas y valores). No borra datos ni tablas.
-- Funciona en MySQL 8.0 o superior y en MariaDB 10.4 o superior.
--
-- Semillas (datos de prueba, TODAS OPCIONALES; ejecútalas después de este script y en este orden):
--   semillas/semilla_modulo7_usuarios.sql    permisos + usuario de prueba maria / Maria2026
--   semillas/semilla_modulo3_catalogo.sql    categorías, opciones y 3 productos
--   semillas/semilla_modulo5_inventario.sql  22 insumos con compra inicial y recetas
--   semillas/semilla_modulo4_pedidos.sql     9 pedidos de prueba (necesita las semillas 3 y 5)
--   semillas/semilla_modulo6_reportes.sql    pagos, compras y mermas de meses pasados para los reportes
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. Procedimientos de apoyo (se borran al final)
--    MySQL no tiene "ADD COLUMN IF NOT EXISTS", así que estos procedimientos
--    revisan information_schema antes de hacer cada cambio.
--    Se compara con LOWER() porque en Windows MySQL guarda los nombres en minúsculas.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS carmir_agregar_columna;
DROP PROCEDURE IF EXISTS carmir_agregar_llave_foranea;
DROP PROCEDURE IF EXISTS carmir_agregar_indice;

DELIMITER $$

-- Agrega una columna solo si la tabla todavía no la tiene
CREATE PROCEDURE carmir_agregar_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND LOWER(TABLE_NAME) = LOWER(tabla)
                   AND LOWER(COLUMN_NAME) = LOWER(columna)) THEN
    SET @carmir_sql = CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN ', columna, ' ', definicion);
    PREPARE sentencia FROM @carmir_sql;
    EXECUTE sentencia;
    DEALLOCATE PREPARE sentencia;
  END IF;
END$$

-- Agrega una llave foránea solo si no existe una restricción con ese nombre
CREATE PROCEDURE carmir_agregar_llave_foranea(IN tabla VARCHAR(64), IN nombre VARCHAR(64), IN definicion TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE()
                   AND LOWER(TABLE_NAME) = LOWER(tabla)
                   AND LOWER(CONSTRAINT_NAME) = LOWER(nombre)) THEN
    SET @carmir_sql = CONCAT('ALTER TABLE ', tabla, ' ADD CONSTRAINT ', nombre, ' ', definicion);
    PREPARE sentencia FROM @carmir_sql;
    EXECUTE sentencia;
    DEALLOCATE PREPARE sentencia;
  END IF;
END$$

-- Crea un índice solo si la tabla todavía no tiene uno con ese nombre
CREATE PROCEDURE carmir_agregar_indice(IN tabla VARCHAR(64), IN nombre VARCHAR(64), IN columnas TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND LOWER(TABLE_NAME) = LOWER(tabla)
                   AND LOWER(INDEX_NAME) = LOWER(nombre)) THEN
    SET @carmir_sql = CONCAT('CREATE INDEX ', nombre, ' ON ', tabla, ' (', columnas, ')');
    PREPARE sentencia FROM @carmir_sql;
    EXECUTE sentencia;
    DEALLOCATE PREPARE sentencia;
  END IF;
END$$

DELIMITER ;


-- =====================================================================
-- PARTE 1 · Módulo 3 · Catálogo (antes PROPUESTA_V2__catalogo.sql)
-- =====================================================================

-- 1.1 RF-4: distinguir las plantillas de postre de los productos normales
CALL carmir_agregar_columna('Postre', 'EsPlantilla',
  'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Si el postre es una plantilla base que el cliente puede personalizar (RF-4)'' AFTER MostrarEnTienda');


-- =====================================================================
-- PARTE 2 · Módulo 5 · Inventario y Recetas (antes PROPUESTA_V3__inventario.sql)
-- =====================================================================

-- 2.1 La Administradora no está en la tabla Usuario: sus compras y mermas se guardan con IdUsuario vacío
--     (volver a ejecutarlo no cambia nada)
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE MovimientoInventario
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que lo registró (relación Captura); vacío = Administradora o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

-- 2.2 El costo de una receta solo cuenta las recetas activas (RF-58)
CREATE OR REPLACE VIEW vw_CostoReceta AS
SELECT r.IdReceta, r.IdOpcion, r.NombreReceta,
       COALESCE(SUM(ri.CantidadInsumo * i.CostoUnitario), 0) AS CostoReceta
FROM Receta r
LEFT JOIN RecetaInsumo ri ON ri.IdReceta = r.IdReceta
LEFT JOIN Insumo i ON i.IdInsumo = ri.IdInsumo
WHERE r.EstadoReceta = 'Activa'
GROUP BY r.IdReceta, r.IdOpcion, r.NombreReceta;


-- =====================================================================
-- PARTE 3 · Módulo 4 · Pedidos y Producción (antes PROPUESTA_V4__pedidos.sql)
-- =====================================================================

-- 3.1 Historial de estados sin usuario (Administradora, cliente desde la web o el sistema)
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE HistorialEstado
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que hizo el cambio (relación Realiza); vacío = Administradora, cliente (web) o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

-- 3.2 Quién canceló el pedido (RF-39)
CALL carmir_agregar_columna('Cancelacion', 'IdUsuario',
  'INT NULL COMMENT ''Usuario que canceló; vacío = Administradora o el cliente'' AFTER Folio');
CALL carmir_agregar_llave_foranea('Cancelacion', 'fk_cancelacion_idusuario',
  'FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE');

-- 3.3 Baja de pedidos registrados por error (no es un estado ni una cancelación)
CREATE TABLE IF NOT EXISTS BajaPedido (
  Folio INT NOT NULL COMMENT 'Pedido dado de baja',
  MotivoBaja VARCHAR(255) NOT NULL COMMENT 'Por qué se dio de baja (duplicado, de prueba, datos falsos...)',
  FechaHoraBaja DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la baja',
  IdUsuario INT NULL COMMENT 'Usuario que dio la baja; vacío = Administradora',
  CONSTRAINT pk_bajapedido PRIMARY KEY (Folio),
  CONSTRAINT fk_bajapedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_bajapedido_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Pedidos dados de baja: se conservan en el historial pero salen de las listas de trabajo.';

-- 3.4 Bitácora de modificaciones del pedido
CREATE TABLE IF NOT EXISTS ModificacionPedido (
  IdModificacion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la modificación',
  Folio INT NOT NULL COMMENT 'Pedido modificado',
  IdUsuario INT NULL COMMENT 'Usuario que modificó; vacío = Administradora',
  DatosAnteriores TEXT NOT NULL COMMENT 'Datos que cambiaron, como estaban antes',
  DatosNuevos TEXT NOT NULL COMMENT 'Datos que cambiaron, como quedaron',
  FechaHoraModificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la modificación',
  CONSTRAINT pk_modificacionpedido PRIMARY KEY (IdModificacion),
  CONSTRAINT fk_modificacionpedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_modificacionpedido_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Qué cambió en cada modificación de un pedido, cuándo y quién lo hizo.';

-- 3.5 Notificaciones pendientes de reenvío (RF-47) y motivo del fallo
ALTER TABLE Notificacion
  MODIFY ResultadoEnvio ENUM('Enviado','Fallido','Pendiente') NOT NULL COMMENT 'Si el mensaje se entregó; Pendiente = se reintentará';
CALL carmir_agregar_columna('Notificacion', 'MotivoFallo',
  'VARCHAR(255) NULL COMMENT ''Por qué falló el envío'' AFTER ResultadoEnvio');


-- =====================================================================
-- PARTE 4 · NUEVO · Reportes Financieros (Módulo 6) y apartado Configuración
-- =====================================================================

-- 4.1 Parámetros del negocio que antes estaban fijos en config/app.properties.
--     Los edita el apartado "Configuración" del panel y también los leerá la tienda en línea.
CREATE TABLE IF NOT EXISTS Configuracion (
  Clave VARCHAR(50) NOT NULL COMMENT 'Nombre del parámetro, por ejemplo CapacidadDiaria',
  Valor VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Valor guardado como texto',
  Descripcion VARCHAR(255) NULL COMMENT 'Para qué sirve el parámetro',
  CONSTRAINT pk_configuracion PRIMARY KEY (Clave)
) ENGINE=InnoDB COMMENT='Parámetros del negocio (horario, anticipo, capacidad, avisos...).';

-- 4.2 Bitácora de cambios de la configuración: quién cambió qué y cuándo
CREATE TABLE IF NOT EXISTS CambioConfiguracion (
  IdCambio INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del cambio',
  Clave VARCHAR(50) NOT NULL COMMENT 'Parámetro o código postal que cambió',
  ValorAnterior VARCHAR(255) NULL COMMENT 'Valor antes del cambio; vacío si es nuevo',
  ValorNuevo VARCHAR(255) NULL COMMENT 'Valor después del cambio; vacío si se eliminó',
  IdUsuario INT NULL COMMENT 'Usuario que hizo el cambio; vacío = Administradora',
  FechaHoraCambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento del cambio',
  CONSTRAINT pk_cambioconfiguracion PRIMARY KEY (IdCambio),
  CONSTRAINT fk_cambioconfiguracion_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Historial de cambios de la configuración y de la cobertura de envío.';

-- 4.3 Códigos postales a los que se hace envío a domicilio (RF-10) y su costo
CREATE TABLE IF NOT EXISTS CodigoPostalCobertura (
  CodigoPostal CHAR(5) NOT NULL COMMENT 'Código postal con servicio a domicilio',
  CostoEnvio DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Lo que se cobra por enviar a ese código postal',
  CONSTRAINT pk_codigopostalcobertura PRIMARY KEY (CodigoPostal),
  CONSTRAINT chk_cobertura_cp CHECK (CodigoPostal REGEXP '^[0-9]{5}$'),
  CONSTRAINT chk_cobertura_costo CHECK (CostoEnvio >= 0)
) ENGINE=InnoDB COMMENT='Zona de cobertura de envío a domicilio.';

-- 4.4 Valores iniciales (INSERT IGNORE: si ya existen, se respetan los que haya cambiado la administradora)
INSERT IGNORE INTO Configuracion (Clave, Valor, Descripcion) VALUES
  ('NombreNegocio',            'Delicias de Postres', 'Nombre del negocio que aparece en pantallas, tickets y mensajes'),
  ('CapacidadDiaria',          '6',     'Pedidos que se pueden entregar en un mismo día antes de avisar "Agenda saturada"'),
  ('DiasParaPreparar',         '2',     'Días antes de la entrega en que un pedido debe pasar a "En preparación"'),
  ('DiasMaximosAgenda',        '62',    'Días máximos de un rango personalizado en la agenda de producción'),
  ('DiasAnticipacionMinima',   '3',     'Días mínimos entre el pedido y la entrega (tienda en línea)'),
  ('HoraApertura',             '09:00', 'Primera hora de entrega (formato HH:mm)'),
  ('HoraCierre',               '19:00', 'Última hora de entrega (formato HH:mm)'),
  ('DiasLaborables',           'L,M,X,J,V,S', 'Días con entregas: L M X J V S D separados por coma'),
  ('PorcentajeAnticipo',       '50',    'Porcentaje del total que se pide como anticipo'),
  ('DatosBancarios',           '',      'Datos para transferencia; vacío = solo pago en sucursal'),
  ('DiasAvisoCaducidad',       '3',     'Días antes de la caducidad de un lote en que se avisa "Próxima caducidad"'),
  ('IntentosMaximosLogin',     '3',     'Intentos fallidos seguidos antes de bloquear una cuenta del personal'),
  ('MinutosInactividadSesion', '30',    'Minutos sin usar el panel antes de cerrar la sesión (0 = nunca)');

-- 4.5 Índices para que los reportes por periodo sean rápidos
CALL carmir_agregar_indice('Pago', 'idx_pago_validacion_fecha', 'EstadoValidacion, FechaPago');
CALL carmir_agregar_indice('Pedido', 'idx_pedido_solicitud', 'FechaSolicitud');
CALL carmir_agregar_indice('MovimientoInventario', 'idx_movimiento_tipo_fecha', 'TipoMovimiento, FechaMovimiento');


-- =====================================================================
-- PARTE 5 · NUEVO · Cuentas de WhatsApp y correo de las notificaciones (RF-47)
-- =====================================================================
-- 5.1 Datos de WhatsApp Business Cloud API y del correo SMTP que se escriben en el Panel
--     (Configuración → Respaldos y sistema → Notificaciones al cliente).
--     El token y la contraseña se guardan CIFRADOS por el Panel (EsSecreto = 1).
--     Va en una tabla aparte de Configuracion para que el usuario de la página web
--     (carmir_pedidos) no pueda leerla: no tiene permiso sobre esta tabla.
--     No lleva valores iniciales: mientras esté vacía el Panel usa notificaciones.properties (modo simulado).
CREATE TABLE IF NOT EXISTS ConfiguracionNotificacion (
  Clave VARCHAR(50) NOT NULL COMMENT 'Nombre del dato, por ejemplo SmtpServidor o WhatsAppToken',
  Valor VARCHAR(2000) NOT NULL DEFAULT '' COMMENT 'Valor como texto; si EsSecreto = 1 va cifrado',
  EsSecreto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = token o contraseña (cifrado, nunca se muestra)',
  FechaModificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Último cambio',
  CONSTRAINT pk_configuracionnotificacion PRIMARY KEY (Clave)
) ENGINE=InnoDB COMMENT='Cuentas de WhatsApp y correo para avisar a los clientes (RF-47).';


-- =====================================================================
-- PARTE 6 · NUEVO · Características que pueden tener receta
-- =====================================================================
-- 6.1 Casilla del Catálogo (Característica → «Sus opciones pueden tener receta»).
--     1 = las opciones de esa característica pueden tener receta (valor de todas las existentes).
--     0 = no admite receta (por ejemplo, una nota o un mensaje escrito); el Panel ya no
--         las ofrece al registrar recetas.
CALL carmir_agregar_columna('Caracteristica', 'PermiteReceta',
  'TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''Si las opciones de la característica pueden tener receta'' AFTER Disponible');


-- =====================================================================
-- PARTE 7 · NUEVO · Comprobantes de pago subidos desde la página web (RF-13 y RF-42)
-- =====================================================================
-- 7.1 El cliente ya puede subir la foto (JPG, PNG o WEBP) o el PDF de su comprobante desde sus archivos,
--     además de pegar un enlace (Pago.LinkComprobante). El archivo se guarda aquí, uno por pago,
--     con el mismo IdPago. La API revisa el tipo real del archivo y que no pase de 5 MB.
--     Se guarda en la base de datos (no en una carpeta pública) para que nadie lo abra desde internet;
--     el Panel lo muestra en «Validación de pagos». Si se borra el pago, se borra su archivo.
--     MEDIUMBLOB admite hasta 16 MB.
CREATE TABLE IF NOT EXISTS ComprobanteArchivo (
  IdPago INT NOT NULL COMMENT 'Pago al que pertenece el comprobante (mismo IdPago)',
  NombreArchivo VARCHAR(120) NOT NULL COMMENT 'Nombre del archivo que subió el cliente (limpio, sin rutas)',
  TipoContenido VARCHAR(40) NOT NULL COMMENT 'image/jpeg, image/png, image/webp o application/pdf',
  TamanoBytes INT NOT NULL COMMENT 'Tamaño del archivo en bytes (máximo 5 MB)',
  Contenido MEDIUMBLOB NOT NULL COMMENT 'El archivo tal cual',
  FechaSubida DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo lo subió el cliente',
  CONSTRAINT pk_comprobantearchivo PRIMARY KEY (IdPago),
  CONSTRAINT fk_comprobantearchivo_idpago FOREIGN KEY (IdPago) REFERENCES Pago (IdPago) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_comprobantearchivo_tamano CHECK (TamanoBytes > 0 AND TamanoBytes <= 5242880),
  CONSTRAINT chk_comprobantearchivo_tipo CHECK (TipoContenido IN ('image/jpeg', 'image/png', 'image/webp', 'application/pdf'))
) ENGINE=InnoDB COMMENT='Foto o PDF del comprobante que el cliente subió desde la página web.';

-- 7.2 Permiso para el usuario de la página web (solo si ya existe; si no, lo da crear_usuario_pedidos.sql).
--     Solo puede AGREGAR comprobantes; no puede leerlos, cambiarlos ni borrarlos.
SET @carmir_hay_usuario_web = (SELECT COUNT(*) FROM mysql.user WHERE User = 'carmir_pedidos' AND Host = 'localhost');
SET @carmir_sql = IF(@carmir_hay_usuario_web > 0,
  'GRANT INSERT ON ComprobanteArchivo TO ''carmir_pedidos''@''localhost''',
  'SELECT ''Sin usuario carmir_pedidos: se omite el permiso'' AS Aviso');
PREPARE carmir_sentencia FROM @carmir_sql;
EXECUTE carmir_sentencia;
DEALLOCATE PREPARE carmir_sentencia;
SET @carmir_hay_usuario_web = (SELECT COUNT(*) FROM mysql.user WHERE User = 'carmir_pedidos' AND Host = '127.0.0.1');
SET @carmir_sql = IF(@carmir_hay_usuario_web > 0,
  'GRANT INSERT ON ComprobanteArchivo TO ''carmir_pedidos''@''127.0.0.1''',
  'SELECT ''Sin usuario carmir_pedidos@127.0.0.1: se omite el permiso'' AS Aviso');
PREPARE carmir_sentencia FROM @carmir_sql;
EXECUTE carmir_sentencia;
DEALLOCATE PREPARE carmir_sentencia;


-- =====================================================================
-- PARTE 8 · NUEVO · Se quita el método de pago «Depósito» (RF-11 y RF-42)
-- =====================================================================
-- 8.1 La página web y el Panel solo ofrecen «Transferencia» y «Efectivo».
--     Los pagos que se hayan guardado como «Deposito» pasan a «Transferencia»
--     (los dos son pagos al banco con número de referencia) y después se quita
--     «Deposito» de la lista de valores. Si ya se aplicó, no cambia nada.
--     (IdPago > 0 es para que MySQL Workbench lo acepte con el modo de actualización segura)
UPDATE Pago SET MetodoPago = 'Transferencia' WHERE IdPago > 0 AND MetodoPago = 'Deposito';
ALTER TABLE Pago
  MODIFY MetodoPago ENUM('Transferencia','Efectivo') NOT NULL COMMENT 'Forma de pago';


-- ---------------------------------------------------------------------
-- 5. Limpieza: se borran los procedimientos de apoyo
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS carmir_agregar_columna;
DROP PROCEDURE IF EXISTS carmir_agregar_llave_foranea;
DROP PROCEDURE IF EXISTS carmir_agregar_indice;

-- Resumen: debe mostrar las tablas nuevas y el número de parámetros
SELECT 'Migración completa aplicada' AS Resultado,
       (SELECT COUNT(*) FROM Configuracion) AS Parametros,
       (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND LOWER(TABLE_NAME) IN ('bajapedido', 'modificacionpedido', 'configuracion',
                                     'cambioconfiguracion', 'codigopostalcobertura',
                                     'configuracionnotificacion', 'comprobantearchivo')) AS TablasNuevas;

-- =====================================================================
-- CARMIR · ACTUALIZACIÓN SIN BORRAR DATOS (MIGRACIÓN COMPLETA)
--
-- Aplica TODOS los cambios recientes de estructura (incluyendo Partes 1 a 9,
-- PostreOpcion.NumeroCapa y vw_CostoPrecioPostre) SOBRE una base CARMIR_DB
-- que ya existe, SIN ejecutar DROP DATABASE ni borrar tus productos o pedidos.
--
-- Es 100% seguro ejecutarlo varias veces en MySQL 8.0+ y MariaDB 10.4+.
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. Procedimientos de apoyo (se borran al final)
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

-- Agrega una restricción o llave foránea solo si no existe una con ese nombre
CREATE PROCEDURE carmir_agregar_llave_foranea(IN tabla VARCHAR(64), IN nombre VARCHAR(64), IN definicion TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE()
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
-- PARTE 1 · Módulo 3 · Catálogo
-- =====================================================================
CALL carmir_agregar_columna('Postre', 'EsPlantilla',
  'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Si el postre es una plantilla base que el cliente puede personalizar (RF-4)'' AFTER MostrarEnTienda');

-- =====================================================================
-- PARTE 2 · Módulo 5 · Inventario y Recetas
-- =====================================================================
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE MovimientoInventario
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que lo registró (relación Captura); vacío = Administradora o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

CREATE OR REPLACE VIEW vw_CostoReceta AS
SELECT r.IdReceta, r.IdOpcion, r.NombreReceta,
       COALESCE(SUM(ri.CantidadInsumo * i.CostoUnitario), 0) AS CostoReceta
FROM Receta r
LEFT JOIN RecetaInsumo ri ON ri.IdReceta = r.IdReceta
LEFT JOIN Insumo i ON i.IdInsumo = ri.IdInsumo
WHERE r.EstadoReceta = 'Activa'
GROUP BY r.IdReceta, r.IdOpcion, r.NombreReceta;

-- =====================================================================
-- PARTE 3 · Módulo 4 · Pedidos y Producción
-- =====================================================================
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE HistorialEstado
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que hizo el cambio (relación Realiza); vacío = Administradora, cliente (web) o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

CALL carmir_agregar_columna('Cancelacion', 'IdUsuario',
  'INT NULL COMMENT ''Usuario que canceló; vacío = Administradora o el cliente'' AFTER Folio');
CALL carmir_agregar_llave_foranea('Cancelacion', 'fk_cancelacion_idusuario',
  'FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE');

CREATE TABLE IF NOT EXISTS BajaPedido (
  Folio INT NOT NULL COMMENT 'Pedido dado de baja',
  MotivoBaja VARCHAR(255) NOT NULL COMMENT 'Por qué se dio de baja (duplicado, de prueba, datos falsos...)',
  FechaHoraBaja DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la baja',
  IdUsuario INT NULL COMMENT 'Usuario que dio la baja; vacío = Administradora',
  CONSTRAINT pk_bajapedido PRIMARY KEY (Folio),
  CONSTRAINT fk_bajapedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_bajapedido_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Pedidos dados de baja: se conservan en el historial pero salen de las listas de trabajo.';

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

ALTER TABLE Notificacion
  MODIFY ResultadoEnvio ENUM('Enviado','Fallido','Pendiente') NOT NULL COMMENT 'Si el mensaje se entregó; Pendiente = se reintentará';
CALL carmir_agregar_columna('Notificacion', 'MotivoFallo',
  'VARCHAR(255) NULL COMMENT ''Por qué falló el envío'' AFTER ResultadoEnvio');

-- =====================================================================
-- PARTE 4 · Reportes Financieros (Módulo 6) y apartado Configuración
-- =====================================================================
CREATE TABLE IF NOT EXISTS Configuracion (
  Clave VARCHAR(50) NOT NULL COMMENT 'Nombre del parámetro, por ejemplo CapacidadDiaria',
  Valor VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Valor guardado como texto',
  Descripcion VARCHAR(255) NULL COMMENT 'Para qué sirve el parámetro',
  CONSTRAINT pk_configuracion PRIMARY KEY (Clave)
) ENGINE=InnoDB COMMENT='Parámetros del negocio (horario, anticipo, capacidad, avisos...).';

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

CREATE TABLE IF NOT EXISTS CodigoPostalCobertura (
  CodigoPostal CHAR(5) NOT NULL COMMENT 'Código postal con servicio a domicilio',
  CostoEnvio DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Lo que se cobra por enviar a ese código postal',
  CONSTRAINT pk_codigopostalcobertura PRIMARY KEY (CodigoPostal),
  CONSTRAINT chk_cobertura_cp CHECK (CodigoPostal REGEXP '^[0-9]{5}$'),
  CONSTRAINT chk_cobertura_costo CHECK (CostoEnvio >= 0)
) ENGINE=InnoDB COMMENT='Zona de cobertura de envío a domicilio.';

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

CALL carmir_agregar_indice('Pago', 'idx_pago_validacion_fecha', 'EstadoValidacion, FechaPago');
CALL carmir_agregar_indice('Pedido', 'idx_pedido_solicitud', 'FechaSolicitud');
CALL carmir_agregar_indice('MovimientoInventario', 'idx_movimiento_tipo_fecha', 'TipoMovimiento, FechaMovimiento');

-- =====================================================================
-- PARTE 5 · Cuentas de WhatsApp y correo de las notificaciones (RF-47)
-- =====================================================================
CREATE TABLE IF NOT EXISTS ConfiguracionNotificacion (
  Clave VARCHAR(50) NOT NULL COMMENT 'Nombre del dato, por ejemplo SmtpServidor o WhatsAppToken',
  Valor VARCHAR(2000) NOT NULL DEFAULT '' COMMENT 'Valor como texto; si EsSecreto = 1 va cifrado',
  EsSecreto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = token o contraseña (cifrado, nunca se muestra)',
  FechaModificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Último cambio',
  CONSTRAINT pk_configuracionnotificacion PRIMARY KEY (Clave)
) ENGINE=InnoDB COMMENT='Cuentas de WhatsApp y correo para avisar a los clientes (RF-47).';

-- =====================================================================
-- PARTE 6 · Características que pueden tener receta
-- =====================================================================
CALL carmir_agregar_columna('Caracteristica', 'PermiteReceta',
  'TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''Si las opciones de la característica pueden tener receta'' AFTER Disponible');

-- =====================================================================
-- PARTE 7 · Comprobantes de pago subidos desde la página web (RF-13 y RF-42)
-- =====================================================================
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

-- =====================================================================
-- PARTE 8 · Se quita el método de pago «Depósito» (RF-11 y RF-42)
-- =====================================================================
UPDATE Pago SET MetodoPago = 'Transferencia' WHERE IdPago > 0 AND MetodoPago = 'Deposito';
ALTER TABLE Pago
  MODIFY MetodoPago ENUM('Transferencia','Efectivo') NOT NULL COMMENT 'Forma de pago';

-- =====================================================================
-- PARTE 9 · Capas del pastel (RF-3)
-- =====================================================================
CALL carmir_agregar_columna('PostreOpcion', 'NumeroCapa',
  'TINYINT NOT NULL DEFAULT 1 COMMENT ''Capa del pastel donde va la opción: 1 = Base (abajo), 2 = la siguiente. Es 1 si la opción es para todo el postre'' AFTER IdOpcion');

SET @carmir_capa_en_llave = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
                             WHERE TABLE_SCHEMA = DATABASE()
                               AND LOWER(TABLE_NAME) = 'postreopcion'
                               AND CONSTRAINT_NAME = 'PRIMARY'
                               AND LOWER(COLUMN_NAME) = 'numerocapa');
SET @carmir_sql = IF(@carmir_capa_en_llave = 0,
  'ALTER TABLE PostreOpcion DROP PRIMARY KEY, ADD PRIMARY KEY (IdPostre, IdOpcion, NumeroCapa)',
  'SELECT ''La capa ya está en la llave de PostreOpcion'' AS Aviso');
PREPARE carmir_sentencia FROM @carmir_sql;
EXECUTE carmir_sentencia;
DEALLOCATE PREPARE carmir_sentencia;

CALL carmir_agregar_llave_foranea('PostreOpcion', 'chk_postreopcion_capa', 'CHECK (NumeroCapa > 0)');

CREATE OR REPLACE VIEW vw_CostoPrecioPostre AS
SELECT po.IdPostre,
       ROUND(SUM(COALESCE(cr.CostoReceta, 0) * po.CantidadElegida
                 * IF(o.SeEscala = 1, COALESCE(f.Factor, 1), 1)
                 * IF(c.SeleccionMultiple = 1, 1, 1 / fc.Filas)), 2) AS CostoIngredientes,
       ROUND(SUM(COALESCE(cr.CostoReceta, 0) * po.CantidadElegida
                 * IF(o.SeEscala = 1, COALESCE(f.Factor, 1), 1) * o.MultiplicadorCosto
                 * IF(c.SeleccionMultiple = 1, 1, 1 / fc.Filas)), 2) AS PrecioVenta
FROM PostreOpcion po
JOIN OpcionCaracteristica o ON o.IdOpcion = po.IdOpcion
JOIN Caracteristica c ON c.IdCaracteristica = o.IdCaracteristica
JOIN (SELECT po3.IdPostre, o3.IdCaracteristica, COUNT(*) AS Filas
      FROM PostreOpcion po3
      JOIN OpcionCaracteristica o3 ON o3.IdOpcion = po3.IdOpcion
      GROUP BY po3.IdPostre, o3.IdCaracteristica) fc
  ON fc.IdPostre = po.IdPostre AND fc.IdCaracteristica = o.IdCaracteristica
LEFT JOIN vw_CostoReceta cr ON cr.IdOpcion = po.IdOpcion
LEFT JOIN (SELECT po2.IdPostre,
                  EXP(SUM(LN(o2.MultiplicadorIngredientes) * IF(c2.SeleccionMultiple = 1, 1, 1 / fc2.Filas))) AS Factor
      FROM PostreOpcion po2
      JOIN OpcionCaracteristica o2 ON o2.IdOpcion = po2.IdOpcion
      JOIN Caracteristica c2 ON c2.IdCaracteristica = o2.IdCaracteristica
      JOIN (SELECT po4.IdPostre, o4.IdCaracteristica, COUNT(*) AS Filas
            FROM PostreOpcion po4
            JOIN OpcionCaracteristica o4 ON o4.IdOpcion = po4.IdOpcion
            GROUP BY po4.IdPostre, o4.IdCaracteristica) fc2
        ON fc2.IdPostre = po2.IdPostre AND fc2.IdCaracteristica = o2.IdCaracteristica
      WHERE o2.SeEscala = 0
      GROUP BY po2.IdPostre) f ON f.IdPostre = po.IdPostre
GROUP BY po.IdPostre;

-- ---------------------------------------------------------------------
-- Limpieza: se borran los procedimientos de apoyo
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS carmir_agregar_columna;
DROP PROCEDURE IF EXISTS carmir_agregar_llave_foranea;
DROP PROCEDURE IF EXISTS carmir_agregar_indice;

DELIMITER ;

SELECT 'Actualización sin borrar datos aplicada correctamente' AS Resultado;

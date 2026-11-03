-- =====================================================================
-- CARMIR · PROPUESTA de migración para el Módulo 4 (Gestor de Pedidos y Producción)
-- Este archivo NO se ejecuta solo. Ejecútenlo después de CARMIR_BaseDeDatos.sql
-- (y de V2 / V3 si las aplicaron). No borra datos y se puede leer de arriba abajo.
--
-- Huecos que se encontraron al construir el gestor de pedidos:
--  1. La Administradora NO está en la tabla Usuario (decisión del Módulo 7) y el
--     primer estado del pedido lo crea el cliente desde la web, pero
--     HistorialEstado.IdUsuario es NOT NULL. Con el cambio, IdUsuario vacío =
--     Administradora, cliente (web) o el sistema.
--  2. La cancelación (RF-25) debe guardar QUIÉN canceló: columna nueva
--     Cancelacion.IdUsuario (vacía = Administradora).
--  3. Baja de pedidos (CRUD "Eliminación de pedidos"): no había dónde guardar el
--     motivo, la fecha y el usuario. Tabla nueva BajaPedido.
--  4. Modificación de pedidos (CRUD "Modificación de pedidos"): no había bitácora
--     de qué cambió. Tabla nueva ModificacionPedido (datos antes y después).
--  5. Notificaciones (RF-32): se pide marcar «Pendiente de reenvío» y guardar el
--     motivo del fallo. Se agrega 'Pendiente' a ResultadoEnvio y la columna
--     MotivoFallo.
--
-- Lo que el panel resolvió sin cambiar la base de datos:
--  - Capacidad diaria de la agenda (RF-23 «Agenda saturada»): config/app.properties
--    (pedidos.capacidadDiaria).
--  - Reembolso por una modificación que baja el total: el saldo queda negativo en
--    vw_SaldoPedido (saldo a favor del cliente) y se anota en ModificacionPedido.
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- 1. Historial de estados sin usuario (Administradora, cliente o sistema)
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE HistorialEstado
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que hizo el cambio (relación Realiza); vacío = Administradora, cliente (web) o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Quién canceló el pedido
ALTER TABLE Cancelacion
  ADD COLUMN IdUsuario INT NULL COMMENT 'Usuario que canceló; vacío = Administradora o el cliente' AFTER Folio,
  ADD CONSTRAINT fk_cancelacion_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 3. Baja de pedidos registrados por error (no es un estado ni una cancelación)
CREATE TABLE IF NOT EXISTS BajaPedido (
  Folio INT NOT NULL COMMENT 'Pedido dado de baja',
  MotivoBaja VARCHAR(255) NOT NULL COMMENT 'Por qué se dio de baja (duplicado, de prueba, datos falsos...)',
  FechaHoraBaja DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la baja',
  IdUsuario INT NULL COMMENT 'Usuario que dio la baja; vacío = Administradora',
  CONSTRAINT pk_bajapedido PRIMARY KEY (Folio),
  CONSTRAINT fk_bajapedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_bajapedido_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Pedidos dados de baja: se conservan en el historial pero salen de las listas de trabajo.';

-- 4. Bitácora de modificaciones del pedido
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

-- 5. Notificaciones pendientes de reenvío y motivo del fallo
ALTER TABLE Notificacion
  MODIFY ResultadoEnvio ENUM('Enviado','Fallido','Pendiente') NOT NULL COMMENT 'Si el mensaje se entregó; Pendiente = se reintentará',
  ADD COLUMN MotivoFallo VARCHAR(255) NULL COMMENT 'Por qué falló el envío' AFTER ResultadoEnvio;

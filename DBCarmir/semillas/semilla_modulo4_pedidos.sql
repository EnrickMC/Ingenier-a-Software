-- =====================================================================
-- CARMIR · Datos semilla del Módulo 4 (Gestor de Pedidos y Producción)
-- OPCIONAL: sirve para probar "Gestión de Pedidos", "Agenda de Producción"
-- y "Validación de Pagos" del panel sin tener todavía la tienda en línea.
-- Ejecutar DESPUÉS de:
--   1. CARMIR_BaseDeDatos.sql
--   2. semillas/semilla_modulo7_usuarios.sql   (usuario maria)
--   3. semillas/semilla_modulo3_catalogo.sql   (postres 1, 2 y 3)
--   4. migraciones/PROPUESTA_V3__inventario.sql y semillas/semilla_modulo5_inventario.sql
--   5. migraciones/PROPUESTA_V4__pedidos.sql   (OBLIGATORIA: historial sin usuario y bajas)
-- No cambia ninguna tabla, solo agrega datos. Si ya existe el cliente de prueba
-- "Ana María López" (5512345601) no se vuelve a insertar nada.
--
-- Las fechas se calculan a partir de HOY, así la agenda siempre tiene pedidos:
--   #1 En proceso de aceptación (recoger, entrega en 3 días)
--   #2 Aprobado con pago parcial validado y un pago pendiente por validar (domicilio, mañana)
--   #3 Pagado (anticipo cubierto, entrega en 2 días)
--   #4 En preparación (entrega hoy)
--   #5 Pedido terminado, liquidado (entrega ayer: aparece "Atrasado")
--   #6 Solicitud con datos incompletos y fecha vencida (a domicilio sin dirección)
--   #7 Aprobado y cancelado, con reembolso pendiente
--   #8 Entregado la semana pasada
--   #9 Solicitud dada de baja (duplicada)
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS semilla_modulo4;

DELIMITER $$
CREATE PROCEDURE semilla_modulo4()
BEGIN
  DECLARE vCliente INT;
  DECLARE vDireccion INT;
  DECLARE vFolio INT;

  IF NOT EXISTS (SELECT 1 FROM Cliente WHERE TelefonoWhatsApp = '5512345601') THEN

    -- #1 En proceso de aceptación --------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Ana María López', '5512345601', 'ana.lopez@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 15 MINUTE, CURDATE() + INTERVAL 3 DAY, '14:00', 'Recoger en tienda', 'Anticipo',
              540.30, 270.15, 'En proceso de aceptacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 1, 1, '¡Feliz cumpleaños, mamá!', 'Letras en color rosa', 245.70, 81.90),
      (vFolio, 3, 1, NULL, NULL, 294.60, 94.60);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 15 MINUTE);

    -- #2 Aprobado, pago parcial y pago pendiente ------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Carlos Ramírez', '5512345602', NULL);
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Direccion (IdCliente, Calle, NumeroExterior, NumeroInterior, Colonia, CodigoPostal, Referencias)
      VALUES (vCliente, 'Av. Reforma', '120', '3B', 'Centro', '06000', 'Edificio azul, tocar el timbre 3');
    SET vDireccion = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 2 DAY, CURDATE() + INTERVAL 1 DAY, '11:00', 'Envio a domicilio', 'Anticipo',
              2205.50, 1102.75, 'Aprobado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO EntregaDomicilio (Folio, IdDireccion) VALUES (vFolio, vDireccion);
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 2, 2, 'Felicidades Lupita', NULL, 1102.75, 330.50);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 2 DAY),
      (vFolio, 1, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 1 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion) VALUES
      (vFolio, 1, 500.00, NOW() - INTERVAL 20 HOUR, 'Efectivo', NULL, NULL, 'Validado'),
      (vFolio, NULL, 602.75, NOW() - INTERVAL 1 HOUR, 'Transferencia', 'SPEI-778812', 'https://ejemplo.com/comprobantes/778812.jpg', 'Pendiente');

    -- #3 Pagado ---------------------------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Laura Gómez', '5512345603', 'laura.gomez@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 3 DAY, CURDATE() + INTERVAL 2 DAY, '16:30', 'Recoger en tienda', 'Anticipo',
              589.20, 294.60, 'Pagado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 3, 2, NULL, 'Una de las dos sin nuez', 294.60, 94.60);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 3 DAY),
      (vFolio, NULL, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
      (vFolio, 1, 'Aprobado', 'Pagado', NOW() - INTERVAL 2 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, 1, 300.00, NOW() - INTERVAL 2 DAY, 'Transferencia', 'TRF-55120', NULL, 'Validado');

    -- #4 En preparación -------------------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Pedro Sánchez', '5512345604', NULL);
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 4 DAY, CURDATE(), '23:00', 'Recoger en tienda', 'Pago total',
              245.70, 245.70, 'En preparacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 1, 1, NULL, NULL, 245.70, 81.90);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 4 DAY),
      (vFolio, NULL, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 4 DAY + INTERVAL 1 HOUR),
      (vFolio, NULL, 'Aprobado', 'Pagado', NOW() - INTERVAL 3 DAY),
      (vFolio, 1, 'Pagado', 'En preparacion', NOW() - INTERVAL 2 HOUR);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, NULL, 245.70, NOW() - INTERVAL 3 DAY, 'Efectivo', NULL, NULL, 'Validado');

    -- #5 Pedido terminado (atrasado: la entrega era ayer) ----------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Sofía Hernández', '5512345605', 'sofia.h@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 6 DAY, CURDATE() - INTERVAL 1 DAY, '12:00', 'Recoger en tienda', 'Anticipo',
              1102.75, 551.38, 'Pedido terminado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 2, 1, 'Feliz aniversario', NULL, 1102.75, 330.50);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 6 DAY),
      (vFolio, NULL, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 6 DAY + INTERVAL 1 HOUR),
      (vFolio, 1, 'Aprobado', 'Pagado', NOW() - INTERVAL 5 DAY),
      (vFolio, 1, 'Pagado', 'En preparacion', NOW() - INTERVAL 2 DAY),
      (vFolio, 1, 'En preparacion', 'Pedido terminado', NOW() - INTERVAL 1 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion) VALUES
      (vFolio, 1, 551.38, NOW() - INTERVAL 5 DAY, 'Transferencia', 'SPEI-440021', NULL, 'Validado'),
      (vFolio, 1, 551.37, NOW() - INTERVAL 1 DAY, 'Efectivo', NULL, NULL, 'Validado');

    -- #6 Datos incompletos y fecha vencida -------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Rosa Delgado', '5512345606', NULL);
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 5 DAY, CURDATE() - INTERVAL 1 DAY, '10:00', 'Envio a domicilio', 'Anticipo',
              294.60, 147.30, 'En proceso de aceptacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 3, 1, NULL, NULL, 294.60, 94.60);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 5 DAY);

    -- #7 Aprobado y cancelado con reembolso pendiente --------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Jorge Méndez', '5512345607', NULL);
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 4 DAY, CURDATE() + INTERVAL 4 DAY, '13:00', 'Recoger en tienda', 'Anticipo',
              491.40, 245.70, 'Aprobado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 1, 2, NULL, NULL, 245.70, 81.90);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 4 DAY),
      (vFolio, 1, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 3 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, 1, 100.00, NOW() - INTERVAL 3 DAY, 'Efectivo', NULL, NULL, 'Validado');
    INSERT INTO Cancelacion (Folio, IdUsuario, MotivoCancelacion, FechaHoraCancelacion, MontoReembolso, EstadoReembolso)
      VALUES (vFolio, NULL, 'Solicitud del cliente: cambió la fecha de su evento', NOW() - INTERVAL 1 DAY, 100.00, 'Pendiente');

    -- #8 Entregado la semana pasada --------------------------------------
    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Mariana Torres', '5512345608', 'mariana.t@ejemplo.com');
    SET vCliente = LAST_INSERT_ID();
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 12 DAY, CURDATE() - INTERVAL 7 DAY, '17:00', 'Recoger en tienda', 'Pago total',
              294.60, 294.60, 'Entregado');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 3, 1, NULL, NULL, 294.60, 94.60);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio) VALUES
      (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 12 DAY),
      (vFolio, NULL, 'En proceso de aceptacion', 'Aprobado', NOW() - INTERVAL 12 DAY + INTERVAL 1 HOUR),
      (vFolio, NULL, 'Aprobado', 'Pagado', NOW() - INTERVAL 11 DAY),
      (vFolio, 1, 'Pagado', 'En preparacion', NOW() - INTERVAL 8 DAY),
      (vFolio, 1, 'En preparacion', 'Pedido terminado', NOW() - INTERVAL 7 DAY - INTERVAL 3 HOUR),
      (vFolio, 1, 'Pedido terminado', 'Entregado', NOW() - INTERVAL 7 DAY);
    INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion)
      VALUES (vFolio, NULL, 294.60, NOW() - INTERVAL 11 DAY, 'Efectivo', NULL, NULL, 'Validado');

    -- #9 Solicitud dada de baja (duplicada de la #1) ----------------------
    SELECT IdCliente INTO vCliente FROM Cliente WHERE TelefonoWhatsApp = '5512345601';
    INSERT INTO Pedido (IdCliente, FechaSolicitud, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago,
                        MontoTotal, AnticipoRequerido, EstadoPedido)
      VALUES (vCliente, NOW() - INTERVAL 14 MINUTE, CURDATE() + INTERVAL 3 DAY, '14:00', 'Recoger en tienda', 'Anticipo',
              245.70, 122.85, 'En proceso de aceptacion');
    SET vFolio = LAST_INSERT_ID();
    INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) VALUES
      (vFolio, 1, 1, NULL, NULL, 245.70, 81.90);
    INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo, FechaHoraCambio)
      VALUES (vFolio, NULL, NULL, 'En proceso de aceptacion', NOW() - INTERVAL 14 MINUTE);
    INSERT INTO BajaPedido (Folio, MotivoBaja, FechaHoraBaja, IdUsuario)
      VALUES (vFolio, 'Pedido duplicado: el cliente envió la solicitud dos veces', NOW() - INTERVAL 5 MINUTE, NULL);
  END IF;
END$$
DELIMITER ;

CALL semilla_modulo4();
DROP PROCEDURE semilla_modulo4;

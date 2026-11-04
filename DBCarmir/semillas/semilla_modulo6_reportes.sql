-- =====================================================================
-- CARMIR · Datos semilla del Módulo 6 (Reportes Financieros)
-- OPCIONAL: escenario con resultados conocidos para revisar los reportes a mano.
-- Todo ocurre en AGOSTO DE 2026 (del 01/08/2026 al 31/08/2026), así no se mezcla
-- con los pedidos de la semilla 4, que usan fechas cercanas a hoy.
--
-- Ejecutar DESPUÉS de:
--   1. CARMIR_BaseDeDatos.sql
--   2. migraciones/MIGRACION_COMPLETA.sql            (OBLIGATORIA: BajaPedido y Cancelacion.IdUsuario)
--   3. semillas/semilla_modulo3_catalogo.sql         (postres "Pastel clásico de vainilla",
--                                                     "Pastel tres leches de fiesta" y "Gelatina mosaico familiar")
--   4. semillas/semilla_modulo5_inventario.sql       (insumo "Harina de trigo")
-- Si ya existe el cliente de prueba 5599990001 no se vuelve a insertar nada.
--
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
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS semilla_modulo6;

DELIMITER $$
CREATE PROCEDURE semilla_modulo6()
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
    SELECT 'Primero ejecuta las semillas 3 (catálogo) y 5 (inventario)' AS Aviso;
  ELSEIF NOT EXISTS (SELECT 1 FROM Cliente WHERE TelefonoWhatsApp = '5599990001') THEN

    INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente)
      VALUES ('Cliente de reportes', '5599990001', 'reportes@ejemplo.com');
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

CALL semilla_modulo6();
DROP PROCEDURE IF EXISTS semilla_modulo6;

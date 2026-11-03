package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.pedidos.dao.PagoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.Cancelacion;
import com.pac.modulos.pedidos.model.EstadoPago;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.MetodoPago;
import com.pac.modulos.pedidos.model.Pago;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.notificacion.NotificacionService;
import com.pac.modulos.pedidos.notificacion.PlantillasMensaje;
import com.pac.util.Formatos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pagos y saldos (RF-28, RF-29):
 * - Registrar un pago en sucursal, validar o rechazar los que subió el cliente.
 * - Cuando lo pagado cubre el anticipo y el pedido está «Aprobado», pasa solo a «Pagado» (RF-27).
 * - Situación del pedido para el desglose (anticipo pendiente, liquidado, reembolso pendiente...).
 */
public class PagoService {

    public static final String MSG_CANCELADO = "No se pueden registrar pagos en un pedido cancelado";
    public static final String MSG_SIN_APROBAR = "El pedido debe aprobarse antes de registrar pagos";
    public static final String MSG_LIQUIDADO = "El pedido ya está liquidado";
    public static final String MSG_NO_REGISTRADO = "No fue posible registrar el pago";
    public static final String MSG_NO_RECHAZADO = "No fue posible rechazar el pago";
    public static final String MSG_MONTO_INVALIDO = "El monto debe ser mayor a $0.00";
    public static final String MSG_ELIGE_METODO = "Selecciona un método de pago";
    public static final String MSG_REFERENCIA_OBLIGATORIA = "Indica la referencia o folio bancario";
    public static final String MSG_REFERENCIA_LARGA = "La referencia admite máximo 50 caracteres";
    public static final String MSG_REFERENCIA_REPETIDA = "Esa referencia ya está registrada en otro pago";
    public static final String MSG_COMPROBANTE_INVALIDO = "El enlace del comprobante debe empezar con http:// o https://";
    public static final String MSG_YA_REVISADO = "Este pago ya fue revisado por otra persona";
    public static final String MSG_PAGO_NO_ENCONTRADO = "Pago no encontrado";
    public static final String MSG_INDICA_MOTIVO_RECHAZO = "Indica el motivo del rechazo";

    // Situaciones del desglose (RF-29)
    public static final String SITUACION_SIN_APROBAR = "El pedido aún no ha sido aprobado";
    public static final String SITUACION_ANTICIPO_PENDIENTE = "Anticipo pendiente";
    public static final String SITUACION_REEMBOLSO_PENDIENTE = "Pedido cancelado – reembolso pendiente";
    public static final String SITUACION_LIQUIDADO = "Liquidado";

    private PagoDAO pagoDAO;
    private PedidoDAO pedidoDAO;
    private EstadoPedidoService estadoService;

    public PagoService() {
        pagoDAO = DAOFactory.crearPagoDAO();
        pedidoDAO = DAOFactory.crearPedidoDAO();
        estadoService = new EstadoPedidoService();
    }

    // ===================== Consultas =====================

    // Lista de la sección "Validación de pagos" (estado null = todos)
    public List<Pago> listar(EstadoPago estado, String texto) throws DAOException {
        return pagoDAO.listar(estado, texto);
    }

    public List<Pago> listarPorFolio(int folio) throws DAOException {
        return pagoDAO.listarPorFolio(folio);
    }

    /**
     * Situación del pedido para el desglose de pagos (RF-29).
     */
    // RF-29 (Seguimiento de saldos y pagos de pedidos) · Pasos 4 y 5: situación del pedido según lo pagado y el saldo.
    public static String situacion(Pedido pedido) {
        if (pedido.isDadoDeBaja()) {
            return "Solicitud dada de baja";
        }
        if (pedido.isCancelado()) {
            // RF-29 · Excepción paso 5: «Pedido cancelado – reembolso pendiente».
            if (Cancelacion.REEMBOLSO_PENDIENTE.equals(pedido.getCancelacion().getEstadoReembolso())) {
                return SITUACION_REEMBOLSO_PENDIENTE + " (" + Formatos.moneda(pedido.getCancelacion().getMontoReembolso()) + ")";
            }
            return "Pedido cancelado";
        }
        if (pedido.getEstado() == EstadoPedido.EN_ACEPTACION) {
            // RF-29 · Excepción paso 2: «El pedido aún no ha sido aprobado».
            return SITUACION_SIN_APROBAR;
        }
        if (pedido.getSaldo() < -0.004) {
            // RF-29 · Excepción paso 3: «Inconsistencia en los pagos registrados».
            return EstadoPedidoService.MSG_INCONSISTENCIA;
        }
        if (pedido.getSaldo() <= 0.004) {
            return SITUACION_LIQUIDADO;
        }
        if (!pedido.anticipoCubierto()) {
            // RF-29 · Excepción paso 3: sin pagos suficientes, «Anticipo pendiente».
            return SITUACION_ANTICIPO_PENDIENTE + ": faltan " + Formatos.moneda(pedido.getFaltaParaIniciar());
        }
        return "Anticipo cubierto · saldo pendiente " + Formatos.moneda(pedido.getSaldo());
    }

    /**
     * Revisa si al pedido se le pueden registrar pagos. Devuelve el resultado de error o null si sí.
     */
    // RF-28 (Validación de pagos) · Paso 2: el pedido permite pagos y tiene saldo pendiente.
    public ResultadoPedido revisarRegistro(Pedido pedido) {
        if (pedido.isDadoDeBaja()) {
            return ResultadoPedido.error(PedidoService.motivoSinCambios(pedido), null);
        }
        if (pedido.isCancelado()) {
            // RF-28 · Excepción paso 2: «No se pueden registrar pagos en un pedido cancelado».
            return ResultadoPedido.error(MSG_CANCELADO, null);
        }
        if (pedido.getEstado() == EstadoPedido.EN_ACEPTACION) {
            // RF-28 · Excepción paso 2: «El pedido debe aprobarse antes de registrar pagos».
            ResultadoPedido resultado = ResultadoPedido.error(MSG_SIN_APROBAR, null);
            resultado.setOfrecerRevision(true);
            return resultado;
        }
        if (pedido.getSaldo() <= 0.004) {
            // RF-28 · Excepción paso 2: «El pedido ya está liquidado».
            return ResultadoPedido.error(MSG_LIQUIDADO, null);
        }
        return null;
    }

    // ===================== Registrar (pago en sucursal o recibido por otro medio) =====================

    /**
     * Registra un pago ya revisado (queda «Validado» con el usuario de la sesión).
     */
    // RF-28 (Validación de pagos): inicia aquí el registro de un pago recibido. Pasos 5 a 9.
    public ResultadoPedido registrar(int folio, double monto, MetodoPago metodo, String referencia,
                                     String linkComprobante) {
        if (!PermisosPedidos.puedeValidarPagos()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SIN_PERMISO, null);
        }
        ResultadoPedido datos = validarDatos(monto, metodo, referencia, linkComprobante, 0);
        if (datos != null) {
            return datos;
        }

        Pago pago = new Pago();
        pago.setFolio(folio);
        pago.setIdUsuario(PermisosPedidos.idUsuarioSesion());
        pago.setMonto(PedidoService.redondear(monto));
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodo(metodo);
        pago.setReferencia(limpiar(referencia));
        pago.setLinkComprobante(limpiar(linkComprobante));
        pago.setEstado(EstadoPago.VALIDADO);
        return guardarValidado(pago, true);
    }

    // ===================== Validar o rechazar un pago pendiente =====================

    /**
     * Valida un pago «Pendiente» que subió el cliente. La Administradora puede corregir
     * el monto, el método y la referencia antes de validarlo.
     */
    // RF-28: inicia aquí la validación de un pago pendiente (comprobante de la web). Pasos 5 a 9.
    public ResultadoPedido validar(Pago pendiente, double monto, MetodoPago metodo, String referencia) {
        if (!PermisosPedidos.puedeValidarPagos()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SIN_PERMISO, null);
        }
        ResultadoPedido datos = validarDatos(monto, metodo, referencia, pendiente.getLinkComprobante(),
                pendiente.getIdPago());
        if (datos != null) {
            return datos;
        }
        pendiente.setIdUsuario(PermisosPedidos.idUsuarioSesion());
        pendiente.setMonto(PedidoService.redondear(monto));
        pendiente.setMetodo(metodo);
        pendiente.setReferencia(limpiar(referencia));
        pendiente.setEstado(EstadoPago.VALIDADO);
        return guardarValidado(pendiente, false);
    }

    /**
     * Rechaza un pago «Pendiente» con su motivo; no cambia el saldo ni el estado del pedido.
     */
    // RF-28 · Excepción paso 4: el comprobante no es válido; el pago se registra «Rechazado» con su motivo.
    public ResultadoPedido rechazar(Pago pendiente, String motivo) {
        if (!PermisosPedidos.puedeValidarPagos()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SIN_PERMISO, null);
        }
        if (motivo == null || motivo.isBlank()) {
            // RF-28 · Excepción paso 4: falta el motivo del rechazo.
            return ResultadoPedido.error(MSG_INDICA_MOTIVO_RECHAZO, ResultadoPedido.CAMPO_MOTIVO);
        }
        String texto = motivo.trim();
        if (texto.length() > 255) {
            texto = texto.substring(0, 255);
        }

        Connection conexion = null;
        try {
            conexion = ApoyoTransaccion.abrir();
            EstadoPago estado = pagoDAO.bloquear(conexion, pendiente.getIdPago());
            if (estado == null) {
                ApoyoTransaccion.deshacer(conexion);
                return ResultadoPedido.recargar(MSG_PAGO_NO_ENCONTRADO);
            }
            if (estado != EstadoPago.PENDIENTE) {
                ApoyoTransaccion.deshacer(conexion);
                return ResultadoPedido.recargar(MSG_YA_REVISADO + " (" + estado + ")");
            }
            pagoDAO.rechazar(conexion, pendiente.getIdPago(), PermisosPedidos.idUsuarioSesion(), texto);
            conexion.commit();
        } catch (DAOException e) {
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_RECHAZADO, e), null);
        } catch (SQLException e) {
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        ResultadoPedido resultado = ResultadoPedido.correcto("Pago rechazado. El saldo y el estado del pedido no cambian");
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(pendiente.getFolio(),
                PlantillasMensaje.PAGO_RECHAZADO, texto));
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    // ===================== Apoyo =====================

    /**
     * Guarda el pago como «Validado» (nuevo o pendiente), recalcula lo pagado y,
     * si cubre el anticipo de un pedido «Aprobado», lo pasa a «Pagado».
     */
    // RF-28 · Paso 6: registra el pago como «Validado» y recalcula lo pagado y el saldo.
    private ResultadoPedido guardarValidado(Pago pago, boolean esNuevo) {
        Connection conexion = null;
        Pedido pedido;
        double pagadoNuevo;
        boolean pasoAPagado;
        try {
            conexion = ApoyoTransaccion.abrir();
            // Se bloquea el pedido para que dos pagos al mismo tiempo no pasen del saldo
            if (pedidoDAO.bloquear(conexion, pago.getFolio()) == null) {
                ApoyoTransaccion.deshacer(conexion);
                // RF-28 · Excepción paso 2: «Pedido no encontrado».
                return ResultadoPedido.recargar(ApoyoTransaccion.MSG_NO_ENCONTRADO);
            }
            if (!esNuevo) {
                EstadoPago estado = pagoDAO.bloquear(conexion, pago.getIdPago());
                if (estado == null) {
                    ApoyoTransaccion.deshacer(conexion);
                    return ResultadoPedido.recargar(MSG_PAGO_NO_ENCONTRADO);
                }
                if (estado != EstadoPago.PENDIENTE) {
                    ApoyoTransaccion.deshacer(conexion);
                    return ResultadoPedido.recargar(MSG_YA_REVISADO + " (" + estado + ")");
                }
            }
            pedido = pedidoDAO.buscarPorFolio(pago.getFolio());
            ResultadoPedido impedimento = revisarRegistro(pedido);
            if (impedimento != null) {
                ApoyoTransaccion.deshacer(conexion);
                return impedimento;
            }
            // El monto no puede pasar del saldo pendiente
            if (pago.getMonto() > pedido.getSaldo() + 0.004) {
                // RF-28 · Excepción paso 5: el monto no puede pasar del saldo pendiente.
                ApoyoTransaccion.deshacer(conexion);
                return ResultadoPedido.error("El monto no puede ser mayor al saldo pendiente ("
                        + Formatos.moneda(pedido.getSaldo()) + ")", ResultadoPedido.CAMPO_MONTO);
            }

            if (esNuevo) {
                pago.setIdPago(pagoDAO.insertar(conexion, pago));
            } else {
                pagoDAO.validar(conexion, pago);
            }
            pagadoNuevo = pedidoDAO.sumarPagado(conexion, pago.getFolio());
            pasoAPagado = estadoService.marcarPagadoSiCubre(conexion, pedido, pagadoNuevo);
            conexion.commit();
        } catch (DAOException e) {
            // RF-28 · Excepción paso 6: se deshace la operación, «No fue posible registrar el pago».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_REGISTRADO, e), null);
        } catch (SQLException e) {
            // RF-28 · Excepción paso 2: sin conexión, «Error de conexión».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        // RF-28 · Paso 9: informa el pago validado y el saldo actualizado.
        // Mensaje con lo pagado y el saldo actualizado
        pedido.setTotalPagado(pagadoNuevo);
        ResultadoPedido resultado = ResultadoPedido.correcto("Pago validado por " + Formatos.moneda(pago.getMonto())
                + ".\nPagado: " + Formatos.moneda(pagadoNuevo) + " · Saldo pendiente: " + Formatos.moneda(pedido.getSaldo()));
        // RF-28 · Paso 7 y paso 8: el pedido pasó a «Pagado»; se notifica al cliente.
        if (pasoAPagado) {
            resultado.agregarAviso("El anticipo quedó cubierto: el pedido pasó a «Pagado».");
        } else if (pedido.getEstado() == EstadoPedido.APROBADO) {
            resultado.agregarAviso("El pedido sigue «Aprobado»: faltan " + Formatos.moneda(pedido.getFaltaParaIniciar())
                    + " para completar el anticipo.");
        }
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(pago.getFolio(),
                PlantillasMensaje.PAGO_VALIDADO, Formatos.moneda(pago.getMonto())));
        if (pasoAPagado) {
            NotificacionService.getInstancia().notificar(pago.getFolio(), PlantillasMensaje.CAMBIO_ESTADO,
                    "Recibimos tu anticipo; comenzaremos a prepararlo para la fecha acordada.");
        }
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    // Revisa los datos del formulario; devuelve el error o null si están bien
    // RF-28 · Paso 5: valida monto, método y referencia.
    private ResultadoPedido validarDatos(double monto, MetodoPago metodo, String referencia, String linkComprobante,
                                         int idPagoExcluido) {
        if (monto <= 0) {
            // RF-28 · Excepción paso 5: el monto debe ser mayor a $0.00.
            return ResultadoPedido.error(MSG_MONTO_INVALIDO, ResultadoPedido.CAMPO_MONTO);
        }
        if (metodo == null) {
            return ResultadoPedido.error(MSG_ELIGE_METODO, ResultadoPedido.CAMPO_METODO);
        }
        String limpia = limpiar(referencia);
        // Transferencia y depósito siempre traen referencia; en efectivo es opcional
        if (limpia == null && metodo != MetodoPago.EFECTIVO) {
            return ResultadoPedido.error(MSG_REFERENCIA_OBLIGATORIA, ResultadoPedido.CAMPO_REFERENCIA);
        }
        if (limpia != null && limpia.length() > 50) {
            return ResultadoPedido.error(MSG_REFERENCIA_LARGA, ResultadoPedido.CAMPO_REFERENCIA);
        }
        String enlace = limpiar(linkComprobante);
        if (enlace != null && !enlace.matches("(?i)https?://\\S+")) {
            return ResultadoPedido.error(MSG_COMPROBANTE_INVALIDO, ResultadoPedido.CAMPO_COMPROBANTE);
        }
        if (enlace != null && enlace.length() > 255) {
            return ResultadoPedido.error("El enlace del comprobante admite máximo 255 caracteres",
                    ResultadoPedido.CAMPO_COMPROBANTE);
        }
        if (limpia != null) {
            try {
                if (pagoDAO.referenciaEnUso(limpia, idPagoExcluido)) {
                    // RF-28 · Excepción paso 5: la referencia ya está registrada en otro pago.
                    return ResultadoPedido.error(MSG_REFERENCIA_REPETIDA, ResultadoPedido.CAMPO_REFERENCIA);
                }
            } catch (DAOException e) {
                return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
            }
        }
        return null;
    }

    // Texto sin espacios de más; null si queda vacío
    private String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}

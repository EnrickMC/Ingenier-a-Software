package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.inventario.service.DescuentoInsumosService;
import com.pac.modulos.inventario.service.MonitorAlertas;
import com.pac.modulos.inventario.service.ResultadoDescuento;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.notificacion.NotificacionService;
import com.pac.modulos.pedidos.notificacion.PlantillasMensaje;
import com.pac.util.Formatos;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cambios de estado del pedido (RF-40, RF-41) con el bloqueo sin pago (RF-44).
 *
 * Secuencia oficial (solo hacia adelante):
 *   En proceso de aceptación → Aprobado → Pagado → En preparación → Pedido terminado → Entregado
 * - «Aprobado» se alcanza con la aprobación (PedidoService.aprobar).
 * - «Pagado» se alcanza solo al validar el anticipo (PagoService).
 * - «En preparación» exige el anticipo validado y descuenta los insumos (RF-63) en la misma transacción.
 * - «Entregado» exige saldo pendiente de $0.00 y es el estado final.
 */
public class EstadoPedidoService {

    public static final String MSG_DEBE_APROBARSE = "El pedido debe aprobarse primero";
    public static final String MSG_YA_ENTREGADO = "El pedido ya fue entregado; es el estado final";
    public static final String MSG_CANCELADO = "El pedido está cancelado y no admite cambios de estado";
    public static final String MSG_SIN_PAGO = "Acción denegada: el pedido no cuenta con pago confirmado";
    public static final String MSG_PAGADO_AL_VALIDAR =
            "El pedido pasa a «Pagado» solo al validar el anticipo. Registra o valida el pago";
    public static final String MSG_INCONSISTENCIA = "Inconsistencia en los pagos registrados";
    public static final String MSG_NO_CAMBIADO = "No fue posible cambiar el estado";

    private PedidoDAO pedidoDAO;
    private BitacoraPedidoDAO bitacoraDAO;
    private DescuentoInsumosService descuentoService;

    public EstadoPedidoService() {
        pedidoDAO = DAOFactory.crearPedidoDAO();
        bitacoraDAO = DAOFactory.crearBitacoraPedidoDAO();
        descuentoService = new DescuentoInsumosService();
    }

    // RF-40: solo se permite pasar al estado siguiente
    // RF-40 (Estados de pedidos): inicia aquí. Pasos 3 y 4: solo se permite el siguiente estado de la secuencia.
    public static boolean transicionPermitida(EstadoPedido actual, EstadoPedido nuevo) {
        if (actual == null || nuevo == null) {
            return false;
        }
        return actual.siguiente() == nuevo;
    }

    /**
     * Revisa si el pedido puede avanzar al siguiente estado con el botón «Avanzar estado».
     * Devuelve un resultado de error con el motivo (y si se ofrece registrar el pago o abrir la revisión),
     * o null si se puede avanzar.
     */
    // RF-41 (Modificación de estado del pedido) · Paso 3: determina si el estado actual se puede avanzar.
    public ResultadoPedido revisarAvance(Pedido pedido) {
        String sinCambios = PedidoService.motivoSinCambios(pedido);
        if (pedido.isCancelado() && !pedido.isDadoDeBaja()) {
            // RF-41 · Excepción paso 2: «El pedido está cancelado y no admite cambios de estado».
            return ResultadoPedido.error(MSG_CANCELADO, null);
        }
        if (sinCambios != null) {
            return ResultadoPedido.error(sinCambios, null);
        }
        EstadoPedido actual = pedido.getEstado();
        if (actual == EstadoPedido.ENTREGADO) {
            // RF-41 · Excepción paso 3 / RF-40 · Excepción paso 4: «El pedido ya fue entregado; es el estado final».
            return ResultadoPedido.error(MSG_YA_ENTREGADO, null);
        }
        if (actual == EstadoPedido.EN_ACEPTACION) {
            // RF-41 · Excepción paso 3: «El pedido debe aprobarse primero».
            ResultadoPedido resultado = ResultadoPedido.error(MSG_DEBE_APROBARSE, null);
            resultado.setOfrecerRevision(true);
            return resultado;
        }
        // RF-41 · Excepción paso 3: «Aprobado» pasa a «Pagado» solo al validar el anticipo.
        if (actual == EstadoPedido.APROBADO) {
            ResultadoPedido resultado = ResultadoPedido.error(textoFaltaAnticipo(pedido), null);
            resultado.setOfrecerPago(PermisosPedidos.puedeValidarPagos());
            return resultado;
        }
        // RF-44 (Bloqueo de preparación sin pago confirmado): inicia aquí. Pasos 2 y 3.
        if (actual == EstadoPedido.PAGADO) {
            // RF-44: el anticipo (o el total) debe estar validado
            if (!pedido.anticipoCubierto()) {
                // RF-44 · Excepción paso 3: «Falta confirmar $[monto] del anticipo».
                ResultadoPedido resultado = ResultadoPedido.error(MSG_SIN_PAGO + ".\n"
                        + "Falta confirmar " + Formatos.moneda(pedido.getFaltaParaIniciar()) + " del anticipo", null);
                resultado.setOfrecerPago(PermisosPedidos.puedeValidarPagos());
                return resultado;
            }
        }
        // RF-41 · Paso 7: para «Entregado» el saldo pendiente debe ser $0.00.
        if (actual == EstadoPedido.TERMINADO) {
            if (pedido.getSaldo() < -0.004) {
                // RF-43 · Excepción paso 3: «Inconsistencia en los pagos registrados».
                return ResultadoPedido.error(MSG_INCONSISTENCIA + ": lo pagado supera el total por "
                        + Formatos.moneda(-pedido.getSaldo()), null);
            }
            if (pedido.getSaldo() > 0.004) {
                // RF-41 · Excepción paso 7: queda saldo pendiente; se ofrece registrar el pago.
                ResultadoPedido resultado = ResultadoPedido.error("No se puede entregar: hay un saldo pendiente de "
                        + Formatos.moneda(pedido.getSaldo()), ResultadoPedido.CAMPO_MONTO);
                resultado.setOfrecerPago(PermisosPedidos.puedeValidarPagos());
                return resultado;
            }
        }
        return null;
    }

    // Texto para un pedido «Aprobado» que todavía no cubre el anticipo
    private String textoFaltaAnticipo(Pedido pedido) {
        if (pedido.getFaltaParaIniciar() > 0) {
            return MSG_PAGADO_AL_VALIDAR + ".\nFalta confirmar " + Formatos.moneda(pedido.getFaltaParaIniciar())
                    + " del anticipo";
        }
        return MSG_PAGADO_AL_VALIDAR;
    }

    /**
     * Mueve el pedido al estado siguiente (botón «Avanzar estado»).
     *
     * @param continuarSinReceta true si la Administradora ya aceptó descontar aunque haya opciones sin receta
     */
    // RF-41 (Modificación de estado del pedido): inicia aquí la regla de negocio. Pasos 6 a 10.
    public ResultadoPedido avanzar(Pedido abierto, EstadoPedido solicitado, boolean continuarSinReceta) {
        if (!PermisosPedidos.puedeGestionar()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SIN_PERMISO, null);
        }

        Connection conexion = null;
        Pedido actual;
        ResultadoDescuento descuento = null;
        try {
            conexion = ApoyoTransaccion.abrir();
            ResultadoPedido revision = ApoyoTransaccion.bloquearYRevisar(pedidoDAO, conexion, abierto);
            // RF-41 · Excepción paso 8: el pedido fue actualizado por otro usuario; se recarga.
            if (revision != null) {
                ApoyoTransaccion.deshacer(conexion);
                return revision;
            }
            actual = pedidoDAO.buscarPorFolio(abierto.getFolio());

            // RF-40: solo el siguiente estado
            if (!transicionPermitida(actual.getEstado(), solicitado)) {
                ApoyoTransaccion.deshacer(conexion);
                // RF-40 · Excepción paso 4: «Cambio de estado no permitido: de [actual] a [solicitado]».
                return ResultadoPedido.error("Cambio de estado no permitido: de " + actual.getTextoEstado()
                        + " a " + solicitado, null);
            }
            // RF-41 y RF-44: reglas de cada estado (pago, saldo, cancelación)
            ResultadoPedido bloqueo = revisarAvance(actual);
            // RF-44 · Excepción paso 2: «Acción denegada: el pedido no cuenta con pago confirmado».
            if (bloqueo != null) {
                ApoyoTransaccion.deshacer(conexion);
                return bloqueo;
            }

            // RF-63: al pasar a «En preparación» se descuentan los insumos en la misma transacción
            // RF-41 · Paso 6 y RF-63: al pasar a «En preparación» se descuentan los insumos.
            if (solicitado == EstadoPedido.EN_PREPARACION) {
                descuento = descuentoService.descontarPorPedido(actual.getFolio(), conexion, continuarSinReceta);
                // RF-63 · Excepción paso 3: opciones sin receta activa; la administradora decide si continúa.
                if (descuento.getResultado() == ResultadoDescuento.SIN_RECETA) {
                    ApoyoTransaccion.deshacer(conexion);
                    ResultadoPedido pregunta = ResultadoPedido.error(descuento.getMensaje(), null);
                    pregunta.setPreguntarSinReceta(true);
                    return pregunta;
                }
                if (!descuento.permiteContinuar()) {
                    // RF-41 · Excepción paso 6: no hay existencias suficientes; el pedido sigue «Pagado».
                    // Faltan insumos o falló el descuento: se deshace todo y el pedido sigue «Pagado»
                    ApoyoTransaccion.deshacer(conexion);
                    return ResultadoPedido.error(descuento.getMensaje(), null);
                }
            }

            pedidoDAO.cambiarEstado(conexion, actual.getFolio(), solicitado);
            bitacoraDAO.registrarCambio(conexion, actual.getFolio(), PermisosPedidos.idUsuarioSesion(),
                    actual.getEstado().getValorBD(), solicitado.getValorBD());
            conexion.commit();
        } catch (DAOException e) {
            // RF-41 · Excepción paso 8: se deshace todo (incluido el descuento), «No fue posible cambiar el estado».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_CAMBIADO, e), null);
        } catch (SQLException e) {
            // RF-41 · Excepción paso 2: sin conexión, «Error de conexión»; el pedido conserva su estado.
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        ResultadoPedido resultado = ResultadoPedido.correcto("Pedido #" + actual.getFolio() + " ahora está «"
                + solicitado + "»");
        // RF-41 · Paso 9: agenda, notificación al cliente y alertas de inventario.
        if (descuento != null) {
            resultado.agregarAviso(descuento.getMensaje());
            // Con el descuento ya guardado se revisan las alertas de inventario
            MonitorAlertas.getInstancia().revisar();
        }
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(actual.getFolio(),
                PlantillasMensaje.CAMBIO_ESTADO, null));
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    /**
     * Pasa un pedido «Aprobado» a «Pagado» dentro de la transacción de un pago (RF-41).
     * Devuelve true si cambió el estado. No hace commit.
     */
    // RF-42 · Paso 7: si lo pagado cubre el anticipo, el pedido pasa de «Aprobado» a «Pagado».
    public boolean marcarPagadoSiCubre(Connection conexion, Pedido pedido, double pagadoNuevo) throws DAOException {
        if (pedido.getEstado() != EstadoPedido.APROBADO || pedido.isCancelado() || pedido.isDadoDeBaja()) {
            return false;
        }
        if (pagadoNuevo + 0.005 < pedido.getMontoParaIniciar()) {
            return false;
        }
        pedidoDAO.cambiarEstado(conexion, pedido.getFolio(), EstadoPedido.PAGADO);
        bitacoraDAO.registrarCambio(conexion, pedido.getFolio(), PermisosPedidos.idUsuarioSesion(),
                EstadoPedido.APROBADO.getValorBD(), EstadoPedido.PAGADO.getValorBD());
        return true;
    }
}

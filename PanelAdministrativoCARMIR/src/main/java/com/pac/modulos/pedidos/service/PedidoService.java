package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.PagoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.BajaPedido;
import com.pac.modulos.pedidos.model.Cancelacion;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.FiltroPedidos;
import com.pac.modulos.pedidos.model.ModalidadEntrega;
import com.pac.modulos.pedidos.model.ModalidadPago;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.notificacion.NotificacionService;
import com.pac.modulos.pedidos.notificacion.PlantillasMensaje;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de la Gestión de Pedidos:
 * - Lista y filtros (RF-36, RF-37) y revisión de la ficha (RF-33).
 * - Aprobar (RF-38), cancelar (RF-39) y dar de baja (CRUD "Eliminación de pedidos").
 * La modificación está en ModificacionPedidoService, los estados en EstadoPedidoService
 * y los pagos en PagoService.
 */
public class PedidoService {

    // Mensajes de la lista
    public static final String MSG_RANGO_INVALIDO = "Rango de fechas inválido";
    public static final String MSG_SIN_SOLICITUDES = "No hay solicitudes pendientes de revisión";
    public static final String MSG_SIN_RESULTADO_TEXTO = "No se encontraron pedidos con ese criterio";
    public static final String MSG_SIN_RESULTADO_FILTROS = "No se encontraron pedidos con esos criterios";
    public static final String MSG_SIN_ACTIVOS = "No hay pedidos activos";
    public static final String MSG_YA_NO_DISPONIBLE = "El pedido ya no está disponible";

    // Avisos de la ficha (RF-33)
    public static final String MSG_DATOS_INCOMPLETOS = "Datos incompletos";
    public static final String MSG_FECHA_VENCIDA = "La fecha de entrega ya venció";
    public static final String MSG_AGENDA_SATURADA = "Agenda saturada para esa fecha";

    // Aprobación (RF-38)
    public static final String MSG_NO_APROBADO = "No fue posible aprobar el pedido";
    public static final String MSG_ANTICIPO_INVALIDO = "El anticipo debe ser un monto entre $0.00 y el total del pedido";
    public static final String MSG_HASTA_CUBRIR_ANTICIPO =
            "El pedido no podrá pasar a «En preparación» hasta cubrir el anticipo";

    // Cancelación (RF-39)
    public static final String MSG_YA_EN_PRODUCCION_CANCELAR = "El pedido ya entró a producción y no puede cancelarse";
    public static final String MSG_INDICA_MOTIVO_CANCELACION = "Indica el motivo de la cancelación";
    public static final String MSG_NO_CANCELADO = "No fue posible cancelar el pedido";
    public static final String MSG_REEMBOLSO_INVALIDO = "El reembolso debe ser un monto entre $0.00 y lo pagado";

    // Baja
    public static final String MSG_SOLO_NO_APROBADAS = "Solo se pueden dar de baja solicitudes no aprobadas; use la cancelación";
    public static final String MSG_INDICA_MOTIVO_BAJA = "Indica el motivo de la baja";
    public static final String MSG_NO_DADO_DE_BAJA = "No fue posible dar de baja la solicitud";

    // Motivos que se ofrecen en las listas; "Otro" pide escribir el detalle
    public static final String MOTIVO_OTRO = "Otro";
    public static final String[] MOTIVOS_CANCELACION = {
        "Solicitud del cliente", "Falta de pago", "Sin capacidad de producción",
        "Insumos no disponibles", "No se pudo contactar al cliente", MOTIVO_OTRO
    };
    public static final String[] MOTIVOS_BAJA = {
        "Pedido duplicado", "Pedido de prueba", "Datos falsos o incorrectos", "Registrado por error", MOTIVO_OTRO
    };

    private PedidoDAO pedidoDAO;
    private PagoDAO pagoDAO;
    private BitacoraPedidoDAO bitacoraDAO;

    public PedidoService() {
        pedidoDAO = DAOFactory.crearPedidoDAO();
        pagoDAO = DAOFactory.crearPagoDAO();
        bitacoraDAO = DAOFactory.crearBitacoraPedidoDAO();
    }

    // ===================== Lista y filtros (RF-36, RF-37) =====================

    // Revisa el filtro antes de buscar; devuelve el mensaje de error o null si está bien
    // RF-36 (Historial de pedidos): inicia aquí. Paso 2: valida el rango de fechas.
    public String validarFiltro(FiltroPedidos filtro) {
        if (filtro.getDesde() != null && filtro.getHasta() != null && filtro.getDesde().isAfter(filtro.getHasta())) {
            // RF-36 · Excepción paso 2: «Rango de fechas inválido».
            return MSG_RANGO_INVALIDO;
        }
        return null;
    }

    /**
     * Busca los pedidos. El personal solo ve los activos;
     * el historial completo, cancelados y dados de baja son de la Administradora (RF-36).
     */
    // RF-36 / RF-37 · Paso 3: consulta los pedidos que cumplen los criterios (o el estado elegido).
    public List<Pedido> buscar(FiltroPedidos filtro) throws DAOException {
        if (!PermisosPedidos.esAdministradora()) {
            filtro.setGrupo(FiltroPedidos.ACTIVOS);
        }
        return pedidoDAO.buscar(filtro);
    }

    // Mensaje para la tabla vacía según el filtro usado
    // RF-36 · Excepción paso 4 / RF-37 · Excepción paso 2: mensaje de tabla vacía según el filtro.
    public String mensajeSinResultados(FiltroPedidos filtro) {
        boolean conTexto = filtro.getTexto() != null && !filtro.getTexto().isBlank();
        boolean conFechas = filtro.getDesde() != null || filtro.getHasta() != null;
        boolean conEstado = filtro.getEstado() != null;

        if (conEstado && !conTexto && !conFechas) {
            // RF-33 · Excepción paso 1: «No hay solicitudes pendientes de revisión».
            if (filtro.getEstado() == EstadoPedido.EN_ACEPTACION) {
                return MSG_SIN_SOLICITUDES;
            }
            // RF-37 (Filtrar pedidos por estados) · Excepción paso 2: «No hay pedidos en estado [estado]».
            return "No hay pedidos en estado " + filtro.getEstado();
        }
        if (conTexto && !conEstado && !conFechas) {
            return MSG_SIN_RESULTADO_TEXTO;
        }
        if (conTexto || conEstado || conFechas) {
            return MSG_SIN_RESULTADO_FILTROS;
        }
        if (filtro.getGrupo() == FiltroPedidos.CANCELADOS) {
            return "No hay pedidos cancelados";
        }
        if (filtro.getGrupo() == FiltroPedidos.DADOS_DE_BAJA) {
            return "No hay solicitudes dadas de baja";
        }
        if (filtro.getGrupo() == FiltroPedidos.TODOS) {
            return "No hay pedidos registrados";
        }
        return MSG_SIN_ACTIVOS;
    }

    // ===================== Detalle (RF-36, RF-33) =====================

    /**
     * Lee el pedido completo: postres, pagos, historial, modificaciones y notificaciones.
     * Si ya no existe devuelve un resultado para recargar la lista.
     */
    // RF-36 paso 6 y RF-33 (Visualización de pedidos) pasos 2 y 3: valida el pedido y recupera su detalle.
    public ResultadoPedido cargarDetalle(int folio) {
        try {
            Pedido pedido = pedidoDAO.buscarPorFolio(folio);
            if (pedido == null) {
                // RF-36 · Excepción paso 6 / RF-33 · Excepción paso 2: «Pedido no encontrado» y se recarga la lista.
                return ResultadoPedido.recargar(ApoyoTransaccion.MSG_NO_ENCONTRADO);
            }
            // El personal no abre pedidos dados de baja (solo la Administradora ve ese historial)
            if (pedido.isDadoDeBaja() && !PermisosPedidos.esAdministradora()) {
                return ResultadoPedido.recargar(MSG_YA_NO_DISPONIBLE);
            }
            pedido.setLineas(pedidoDAO.listarLineas(folio));
            pedido.setPagos(pagoDAO.listarPorFolio(folio));
            pedido.setHistorial(bitacoraDAO.listarHistorial(folio));
            pedido.setNotificaciones(NotificacionService.getInstancia().listarPorFolio(folio));
            try {
                pedido.setModificaciones(bitacoraDAO.listarModificaciones(folio));
            } catch (DAOException e) {
                // Sin la migración V4 el detalle se muestra igual, sin la bitácora de modificaciones
                System.out.println("Detalle del pedido " + folio + ": " + e.getMessage());
            }
            ResultadoPedido resultado = ResultadoPedido.correcto("");
            resultado.setPedido(pedido);
            return resultado;
        } catch (DAOException e) {
            System.out.println("Error al cargar el pedido " + folio + ": " + e.getMessage());
            // RF-33 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el detalle.
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        }
    }

    // true si el pedido existe y no está cancelado ni dado de baja (para abrirlo desde la agenda)
    public boolean sigueDisponible(int folio) throws DAOException {
        Pedido pedido = pedidoDAO.buscarPorFolio(folio);
        return pedido != null && !pedido.isCancelado() && !pedido.isDadoDeBaja();
    }

    // Lista de los datos que le faltan al pedido (vacía si está completo)
    // RF-33 · Paso 4: valida productos, teléfono de contacto y fecha de entrega.
    public List<String> datosFaltantes(Pedido pedido) {
        List<String> faltan = new ArrayList<String>();
        if (pedido.getNumeroArticulos() <= 0) {
            faltan.add("postres");
        }
        String telefono = pedido.getCliente().getTelefono();
        if (telefono == null || !telefono.matches("[0-9]{10}")) {
            faltan.add("teléfono de contacto");
        }
        if (pedido.getFechaEntrega() == null || pedido.getHorarioEntrega() == null) {
            faltan.add("fecha y hora de entrega");
        }
        if (pedido.getModalidadEntrega() == ModalidadEntrega.DOMICILIO && pedido.getDireccion() == null) {
            faltan.add("dirección de entrega");
        }
        return faltan;
    }

    /**
     * Avisos de la ficha de revisión (RF-33): datos incompletos, fecha vencida,
     * agenda saturada y productos que ya no están disponibles.
     */
    // RF-33 · Pasos 4 y 5: avisos de la ficha.
    public List<String> avisosDeRevision(Pedido pedido) throws DAOException {
        List<String> avisos = new ArrayList<String>();
        List<String> faltan = datosFaltantes(pedido);
        if (!faltan.isEmpty()) {
            // RF-33 · Excepción paso 4: «Datos incompletos».
            avisos.add(MSG_DATOS_INCOMPLETOS + ": falta " + String.join(", ", faltan));
        }
        if (pedido.isActivo() && pedido.getEstado().esAnteriorA(EstadoPedido.EN_PREPARACION) && pedido.isAtrasado()) {
            // RF-33 · Excepción paso 5: «La fecha de entrega ya venció».
            avisos.add(MSG_FECHA_VENCIDA);
        }
        if (pedido.isActivo() && pedido.getFechaEntrega() != null) {
            int carga = cargaDelDia(pedido.getFechaEntrega(), pedido.getFolio());
            int capacidad = ConfiguracionApp.getInstancia().getCapacidadDiaria();
            if (carga >= capacidad) {
                // RF-33 · Excepción paso 5: «Agenda saturada para esa fecha» (no bloquea la aprobación).
                avisos.add(MSG_AGENDA_SATURADA + " (" + carga + " pedido(s) para ese día; capacidad: " + capacidad + ")");
            }
        }
        if (pedido.getEstado() == EstadoPedido.EN_ACEPTACION && pedido.isActivo()) {
            avisos.addAll(pedidoDAO.listarProductosInactivos(pedido.getFolio()));
        }
        return avisos;
    }

    // Otros pedidos activos que se entregan el mismo día
    public int cargaDelDia(LocalDate fecha, int folioExcluido) throws DAOException {
        return pedidoDAO.contarEntregasDelDia(fecha, folioExcluido);
    }

    // ===================== Aprobar (RF-38) =====================

    /**
     * Revisa si el pedido se puede aprobar. Devuelve null si sí, o el motivo por el que no.
     * Lo usa la pantalla para deshabilitar «Aprobar» y explicar por qué.
     */
    // RF-38 (Aprobación de pedidos) · Pasos 2 y 3: revisa si el pedido se puede aprobar.
    public String motivoNoAprobable(Pedido pedido) throws DAOException {
        String motivo = motivoSinCambios(pedido);
        if (motivo != null) {
            return motivo;
        }
        if (pedido.getEstado() != EstadoPedido.EN_ACEPTACION) {
            // RF-38 · Excepción paso 2: «Este pedido ya fue atendido (estado actual)».
            return "Este pedido ya fue atendido (" + pedido.getEstado() + ")";
        }
        if (!datosFaltantes(pedido).isEmpty()) {
            return MSG_DATOS_INCOMPLETOS;
        }
        // RF-38 · Excepción paso 3: la fecha de entrega ya venció.
        if (pedido.isAtrasado()) {
            return MSG_FECHA_VENCIDA;
        }
        List<String> inactivos = pedidoDAO.listarProductosInactivos(pedido.getFolio());
        // RF-38 · Excepción paso 3: hay productos inactivos; se sugiere contactar al cliente o cancelar.
        if (!inactivos.isEmpty()) {
            return inactivos.get(0) + ". Contacta al cliente para modificar el pedido o cancélalo";
        }
        return null;
    }

    /**
     * Aprueba el pedido: ajusta el anticipo (si cambió), lo pasa a «Aprobado»,
     * registra el historial con el usuario y avisa al cliente.
     */
    // RF-38 (Aprobación de pedidos): inicia aquí la regla de negocio. Pasos 6 y 7: cambia a «Aprobado» y lo agrega a la agenda.
    public ResultadoPedido aprobar(Pedido abierto, double anticipo) {
        if (!PermisosPedidos.puedeGestionar()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SIN_PERMISO, null);
        }
        double anticipoFinal = redondear(anticipo);
        if (abierto.getModalidadPago() == ModalidadPago.PAGO_TOTAL) {
            anticipoFinal = abierto.getMontoTotal();
        }
        if (anticipoFinal < 0 || anticipoFinal > abierto.getMontoTotal()) {
            return ResultadoPedido.error(MSG_ANTICIPO_INVALIDO, ResultadoPedido.CAMPO_ANTICIPO);
        }

        Connection conexion = null;
        Pedido actual;
        try {
            conexion = ApoyoTransaccion.abrir();
            ResultadoPedido revision = ApoyoTransaccion.bloquearYRevisar(pedidoDAO, conexion, abierto);
            if (revision != null) {
                ApoyoTransaccion.deshacer(conexion);
                return revision;
            }
            // Se vuelve a leer dentro de la transacción para revisar el estado real
            actual = pedidoDAO.buscarPorFolio(abierto.getFolio());
            String motivo = motivoNoAprobable(actual);
            // RF-38 · Excepción paso 2: el pedido cambió desde otra sesión; no se aprueba y se recarga.
            if (motivo != null) {
                ApoyoTransaccion.deshacer(conexion);
                return ResultadoPedido.recargar(motivo);
            }

            if (Math.abs(anticipoFinal - actual.getAnticipoRequerido()) > 0.004) {
                pedidoDAO.actualizarAnticipo(conexion, actual.getFolio(), anticipoFinal);
            }
            pedidoDAO.cambiarEstado(conexion, actual.getFolio(), EstadoPedido.APROBADO);
            bitacoraDAO.registrarCambio(conexion, actual.getFolio(), PermisosPedidos.idUsuarioSesion(),
                    actual.getEstado().getValorBD(), EstadoPedido.APROBADO.getValorBD());
            conexion.commit();
        } catch (DAOException e) {
            // RF-38 · Excepción paso 6: se deshace la operación, «No fue posible aprobar el pedido».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_APROBADO, e), null);
        } catch (SQLException e) {
            // RF-38 · Excepción paso 2: sin conexión, «Error de conexión».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        // RF-38 · Pasos 8 y 9: notificación al cliente y aviso a la administradora.
        // Después de guardar: avisos, notificación y contadores del menú
        ResultadoPedido resultado = ResultadoPedido.correcto("Pedido #" + actual.getFolio() + " aprobado");
        double montoParaIniciar = anticipoFinal;
        if (actual.getModalidadPago() == ModalidadPago.PAGO_TOTAL) {
            montoParaIniciar = actual.getMontoTotal();
        }
        if (actual.getTotalPagado() + 0.005 < montoParaIniciar) {
            resultado.agregarAviso(MSG_HASTA_CUBRIR_ANTICIPO + " (" + Formatos.moneda(montoParaIniciar) + ").");
            resultado.setOfrecerPago(PermisosPedidos.puedeValidarPagos());
        }
        try {
            int carga = cargaDelDia(actual.getFechaEntrega(), actual.getFolio());
            if (carga >= ConfiguracionApp.getInstancia().getCapacidadDiaria()) {
                resultado.agregarAviso(MSG_AGENDA_SATURADA + ".");
            }
        } catch (DAOException e) {
            System.out.println("No se pudo revisar la carga del día: " + e.getMessage());
        }
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(actual.getFolio(),
                PlantillasMensaje.APROBACION, null));
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    // ===================== Cancelar (RF-39) =====================

    /**
     * Revisa si el pedido se puede cancelar. Devuelve null si sí, o el motivo por el que no.
     */
    // RF-39 (Cancelación de pedidos) · Paso 2: revisa si el pedido se puede cancelar.
    public String motivoNoCancelable(Pedido pedido) {
        String motivo = motivoSinCambios(pedido);
        if (motivo != null) {
            return motivo;
        }
        // RF-39 · Excepción paso 2: «El pedido ya entró a producción y no puede cancelarse».
        if (!pedido.getEstado().esAnteriorA(EstadoPedido.EN_PREPARACION)) {
            return MSG_YA_EN_PRODUCCION_CANCELAR;
        }
        return null;
    }

    /**
     * Cancela el pedido. El estado no cambia: se registra en Cancelacion con el motivo
     * y, si hay pagos validados, el reembolso.
     *
     * @param motivo  motivo elegido en la lista
     * @param detalle descripción (obligatoria si el motivo es "Otro")
     */
    // RF-39 (Cancelación de pedidos): inicia aquí la regla de negocio.
    public ResultadoPedido cancelar(Pedido abierto, String motivo, String detalle, double reembolso,
                                    String estadoReembolso) {
        if (!PermisosPedidos.puedeGestionar()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SIN_PERMISO, null);
        }
        String textoMotivo = armarMotivo(motivo, detalle);
        // RF-39 · Excepción paso 5: «Indica el motivo de la cancelación».
        if (textoMotivo == null) {
            return ResultadoPedido.error(MSG_INDICA_MOTIVO_CANCELACION, campoMotivo(motivo));
        }

        Connection conexion = null;
        Cancelacion cancelacion = new Cancelacion();
        try {
            conexion = ApoyoTransaccion.abrir();
            ResultadoPedido revision = ApoyoTransaccion.bloquearYRevisar(pedidoDAO, conexion, abierto);
            if (revision != null) {
                ApoyoTransaccion.deshacer(conexion);
                return revision;
            }
            Pedido actual = pedidoDAO.buscarPorFolio(abierto.getFolio());
            String impedimento = motivoNoCancelable(actual);
            // RF-39 · Excepción paso 2: pedido no encontrado, ya cancelado o en producción.
            if (impedimento != null) {
                ApoyoTransaccion.deshacer(conexion);
                return ResultadoPedido.recargar(impedimento);
            }

            // RF-39 · Paso 7: registra el reembolso pendiente si tenía pagos validados.
            // Reembolso: solo si hay pagos validados
            double pagado = pedidoDAO.sumarPagado(conexion, actual.getFolio());
            cancelacion.setFolio(actual.getFolio());
            cancelacion.setMotivo(textoMotivo);
            if (pagado > 0) {
                double monto = redondear(reembolso);
                if (monto < 0 || monto > pagado + 0.004) {
                    ApoyoTransaccion.deshacer(conexion);
                    return ResultadoPedido.error(MSG_REEMBOLSO_INVALIDO + " (" + Formatos.moneda(pagado) + ")",
                            ResultadoPedido.CAMPO_REEMBOLSO);
                }
                cancelacion.setMontoReembolso(monto);
                if (monto == 0) {
                    cancelacion.setEstadoReembolso(Cancelacion.REEMBOLSO_NO_APLICA);
                } else if (Cancelacion.REEMBOLSO_REALIZADO.equals(estadoReembolso)) {
                    cancelacion.setEstadoReembolso(Cancelacion.REEMBOLSO_REALIZADO);
                } else {
                    cancelacion.setEstadoReembolso(Cancelacion.REEMBOLSO_PENDIENTE);
                }
            } else {
                cancelacion.setMontoReembolso(0);
                cancelacion.setEstadoReembolso(Cancelacion.REEMBOLSO_NO_APLICA);
            }

            bitacoraDAO.registrarCancelacion(conexion, cancelacion, PermisosPedidos.idUsuarioSesion());
            conexion.commit();
        } catch (DAOException e) {
            // RF-39 · Excepción paso 6: se deshace la operación, «No fue posible cancelar el pedido».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_CANCELADO, e), null);
        } catch (SQLException e) {
            // RF-39 · Excepción paso 2: sin conexión, «Error de conexión».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        ResultadoPedido resultado = ResultadoPedido.correcto("Pedido #" + abierto.getFolio() + " cancelado");
        // RF-39 · Pasos 8 y 9: notificación de la cancelación y aviso del reembolso.
        if (Cancelacion.REEMBOLSO_PENDIENTE.equals(cancelacion.getEstadoReembolso())) {
            resultado.agregarAviso("Reembolso pendiente: " + Formatos.moneda(cancelacion.getMontoReembolso()));
        }
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(abierto.getFolio(),
                PlantillasMensaje.CANCELACION, textoMotivo));
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    // ===================== Dar de baja (CRUD "Eliminación de pedidos") =====================

    /**
     * Revisa si la solicitud se puede dar de baja (solo la Administradora y solo si no se ha aprobado).
     */
    // RF-35 (Eliminación de pedidos): revisa si la solicitud se puede dar de baja (solo la Administradora).
    public String motivoNoBaja(Pedido pedido) {
        if (!PermisosPedidos.esAdministradora()) {
            return PermisosPedidos.MSG_SOLO_ADMINISTRADORA;
        }
        String motivo = motivoSinCambios(pedido);
        if (motivo != null) {
            return motivo;
        }
        if (pedido.getEstado() != EstadoPedido.EN_ACEPTACION) {
            // RF-35 · Excepción: solo se dan de baja solicitudes no aprobadas.
            return MSG_SOLO_NO_APROBADAS;
        }
        return null;
    }

    // Da de baja la solicitud: se conserva en el historial, pero sale de las listas de trabajo
    // RF-35 (Eliminación de pedidos): inicia aquí la regla de negocio.
    public ResultadoPedido darDeBaja(Pedido abierto, String motivo, String detalle) {
        if (!PermisosPedidos.esAdministradora()) {
            return ResultadoPedido.error(PermisosPedidos.MSG_SOLO_ADMINISTRADORA, null);
        }
        String textoMotivo = armarMotivo(motivo, detalle);
        if (textoMotivo == null) {
            // RF-35 · Excepción: falta el motivo de la baja.
            return ResultadoPedido.error(MSG_INDICA_MOTIVO_BAJA, campoMotivo(motivo));
        }

        Connection conexion = null;
        try {
            conexion = ApoyoTransaccion.abrir();
            ResultadoPedido revision = ApoyoTransaccion.bloquearYRevisar(pedidoDAO, conexion, abierto);
            if (revision != null) {
                ApoyoTransaccion.deshacer(conexion);
                return revision;
            }
            Pedido actual = pedidoDAO.buscarPorFolio(abierto.getFolio());
            String impedimento = motivoNoBaja(actual);
            if (impedimento != null) {
                ApoyoTransaccion.deshacer(conexion);
                return ResultadoPedido.recargar(impedimento);
            }
            BajaPedido baja = new BajaPedido();
            baja.setFolio(actual.getFolio());
            baja.setMotivo(textoMotivo);
            bitacoraDAO.registrarBaja(conexion, baja, PermisosPedidos.idUsuarioSesion());
            conexion.commit();
        } catch (DAOException e) {
            // RF-35 · Excepción: se deshace la operación, «No fue posible dar de baja».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_DADO_DE_BAJA, e), null);
        } catch (SQLException e) {
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        ResultadoPedido resultado = ResultadoPedido.correcto("Solicitud #" + abierto.getFolio() + " dada de baja");
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(abierto.getFolio(),
                PlantillasMensaje.BAJA, textoMotivo));
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    // ===================== Apoyo =====================

    /**
     * Motivo por el que un pedido ya no admite ningún cambio (cancelado o dado de baja), o null.
     * Lo usan también la modificación, los pagos y los estados.
     */
    // RF-39 · Excepción paso 2: «Este pedido ya fue cancelado el [fecha]» (o dado de baja).
    public static String motivoSinCambios(Pedido pedido) {
        if (pedido.isDadoDeBaja()) {
            return "Esta solicitud ya fue dada de baja el " + Formatos.fechaHoraCorta(pedido.getBaja().getFecha())
                    + ". Motivo: " + pedido.getBaja().getMotivo();
        }
        if (pedido.isCancelado()) {
            return "Este pedido ya fue cancelado el " + Formatos.fechaHoraCorta(pedido.getCancelacion().getFecha())
                    + ". Motivo: " + pedido.getCancelacion().getMotivo();
        }
        return null;
    }

    // "Motivo" o "Otro: detalle"; null si falta algo
    private String armarMotivo(String motivo, String detalle) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        String limpio = "";
        if (detalle != null) {
            limpio = detalle.trim();
        }
        if (MOTIVO_OTRO.equals(motivo)) {
            if (limpio.isEmpty()) {
                return null;
            }
            return recortar(limpio);
        }
        if (limpio.isEmpty()) {
            return motivo;
        }
        return recortar(motivo + ": " + limpio);
    }

    // Qué campo se marca cuando falta el motivo: la lista o el detalle de "Otro"
    private String campoMotivo(String motivo) {
        if (MOTIVO_OTRO.equals(motivo)) {
            return ResultadoPedido.CAMPO_DETALLE;
        }
        return ResultadoPedido.CAMPO_MOTIVO;
    }

    // Las columnas de motivo admiten 255 caracteres
    private String recortar(String texto) {
        if (texto.length() > 255) {
            return texto.substring(0, 255);
        }
        return texto;
    }

    public static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    // true si la fecha y hora ya pasaron
    public static boolean yaPaso(LocalDateTime momento) {
        return momento != null && momento.isBefore(LocalDateTime.now());
    }
}

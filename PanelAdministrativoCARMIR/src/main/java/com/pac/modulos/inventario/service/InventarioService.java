package com.pac.modulos.inventario.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.dao.InsumoDAO;
import com.pac.modulos.inventario.dao.LoteDAO;
import com.pac.modulos.inventario.dao.MovimientoDAO;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.model.Movimiento;
import com.pac.modulos.inventario.model.TipoMovimiento;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Entradas (RF-59) y salidas (RF-60) de insumos, y el descuento por PEPS
 * que también usa el descuento automático de los pedidos (RF-63).
 *
 * PEPS = primeras entradas, primeras salidas: siempre se descuenta primero
 * del lote que caduca antes, sin dejar ningún lote en negativo.
 */
public class InventarioService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_DISPONIBLE = "Insumo no disponible";
    public static final String MSG_SIN_EXISTENCIAS = "El insumo no tiene existencias para registrar una merma";
    public static final String MSG_CANTIDAD_INVALIDA = "La cantidad debe ser mayor a cero";
    public static final String MSG_COSTO_INVALIDO = "El costo de la compra debe ser mayor a cero";
    public static final String MSG_CADUCIDAD_PASADA = "La fecha de caducidad debe ser posterior a hoy";
    public static final String MSG_CADUCIDAD_OBLIGATORIA = "El insumo es perecedero: indica la fecha de caducidad";
    public static final String MSG_CADUCIDAD_ANTES_COMPRA = "La fecha de caducidad no puede ser anterior a la compra";
    public static final String MSG_COMPRA_FUTURA = "La fecha de compra no puede ser futura";
    public static final String MSG_COMPRA_VACIA = "Indica la fecha de compra";
    public static final String MSG_PROVEEDOR_LARGO = "El proveedor no puede pasar de 100 caracteres";
    public static final String MSG_SUPERA_EXISTENCIA = "La cantidad no puede ser mayor a la existencia disponible";
    public static final String MSG_SIN_MOTIVO = "Indica el motivo de la merma";
    public static final String MSG_LOTE_NO_DISPONIBLE = "El lote elegido ya no tiene existencia";
    public static final String MSG_ENTRADA_FALLO = "No fue posible registrar la entrada";
    public static final String MSG_MERMA_FALLO = "No fue posible registrar la merma";
    public static final String MSG_PROXIMO_CADUCAR = "Este lote está próximo a caducar";

    // Motivos de merma que se ofrecen en el formulario (RF-60)
    public static final String MOTIVO_OTRO = "Otro";
    public static final String[] MOTIVOS_MERMA = {"Desperdicio", "Caducidad", "Daño", MOTIVO_OTRO};

    private static final double MAX_CANTIDAD = 9999999.999;
    private static final double MAX_COSTO = 99999999.99;
    private static final int MAX_PROVEEDOR = 100;
    private static final int MAX_MOTIVO = 255;

    private InsumoDAO insumoDAO;
    private LoteDAO loteDAO;
    private MovimientoDAO movimientoDAO;

    public InventarioService() {
        insumoDAO = DAOFactory.crearInsumoDAO();
        loteDAO = DAOFactory.crearLoteDAO();
        movimientoDAO = DAOFactory.crearMovimientoDAO();
    }

    // ===================== Consultas =====================

    public List<Lote> listarLotes(int idInsumo, boolean soloConExistencia) throws DAOException {
        return loteDAO.listarPorInsumo(idInsumo, soloConExistencia);
    }

    public List<Lote> listarLotesConExistencia() throws DAOException {
        return loteDAO.listarConExistencia();
    }

    public Lote buscarLote(int idLote) throws DAOException {
        return loteDAO.buscarPorId(idLote);
    }

    // Historial filtrado (idInsumo 0, tipo null o fechas null = sin ese filtro)
    public List<Movimiento> listarMovimientos(int idInsumo, TipoMovimiento tipo, LocalDate desde, LocalDate hasta)
            throws DAOException {
        return movimientoDAO.listar(idInsumo, tipo, desde, hasta);
    }

    public List<Movimiento> listarUltimosMovimientos(int idInsumo, int cantidad) throws DAOException {
        return movimientoDAO.listarUltimos(idInsumo, cantidad);
    }

    // Días que le faltan a una fecha de caducidad (negativo = ya pasó)
    public static int diasPara(LocalDate fecha) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), fecha);
    }

    // ===================== Entrada de insumos (RF-59) =====================

    // Paso 2: antes de abrir el formulario, el insumo debe existir y estar activo
    // RF-59 (Registro de entrada de insumos): inicia aquí. Paso 2: el insumo debe existir y estar activo.
    public ResultadoInventario revisarParaEntrada(int idInsumo) {
        if (!PermisosInventario.puedeRegistrarMovimientos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            Insumo insumo = insumoDAO.buscarPorId(idInsumo);
            if (insumo == null || !insumo.isActivo()) {
                // RF-59 · Excepción paso 2: «Insumo no disponible».
                return ResultadoInventario.noEncontrado(MSG_NO_DISPONIBLE);
            }
            return ResultadoInventario.correcto("");
        } catch (DAOException e) {
            // RF-59 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // RF-59 · Pasos 5 a 9: valida y registra la compra como un lote nuevo.
    public ResultadoInventario registrarEntrada(Lote datos) {
        if (!PermisosInventario.puedeRegistrarMovimientos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }

        Insumo insumo;
        try {
            insumo = insumoDAO.buscarPorId(datos.getIdInsumo());
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
        if (insumo == null || !insumo.isActivo()) {
            return ResultadoInventario.noEncontrado(MSG_NO_DISPONIBLE);
        }

        // Paso 5: validaciones
        ResultadoInventario validacion = validarEntrada(datos, insumo);
        // RF-59 · Excepción paso 5: cantidad, costo o fechas inválidas; se resalta el campo.
        if (validacion != null) {
            return validacion;
        }
        datos.setExistencia(datos.getCantidadComprada());

        // Pasos 6 y 7: lote + movimiento tipo Compra en una sola transacción
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            loteDAO.insertar(conexion, datos);

            Movimiento compra = new Movimiento();
            compra.setIdInsumo(datos.getIdInsumo());
            compra.setIdUsuario(PermisosInventario.idUsuarioSesion());
            compra.setTipo(TipoMovimiento.COMPRA);
            compra.setCantidad(datos.getCantidadComprada());
            compra.setCosto(datos.getCostoCompra());
            String motivo = "Compra del lote " + datos.getClave();
            if (!ValidacionCatalogo.estaVacio(datos.getProveedor())) {
                motivo = motivo + " a " + datos.getProveedor().trim();
            }
            compra.setMotivo(motivo);
            movimientoDAO.insertar(conexion, compra);

            conexion.commit();
        } catch (SQLException e) {
            // RF-59 · Excepción paso 6: se deshace la operación, «No fue posible registrar la entrada».
            ApoyoMySQL.deshacer(conexion);
            return ResultadoInventario.error(MSG_ENTRADA_FALLO, ResultadoInventario.CAMPO_CONEXION);
        } catch (DAOException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoInventario.error(mensajeDeFallo(MSG_ENTRADA_FALLO, e), ResultadoInventario.CAMPO_CONEXION);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }

        // Paso 8: se recalculan las alertas (puede retirarse la de stock bajo)
        MonitorAlertas.getInstancia().revisar();

        double nuevaExistencia = insumo.getExistencia() + datos.getCantidadComprada();
        String unidad = insumo.getUnidad().getValorBD();
        ResultadoInventario resultado = ResultadoInventario.correcto("Entrada registrada en el lote " + datos.getClave()
                + ".\nNueva existencia de «" + insumo.getNombre() + "»: " + Formatos.cantidad(nuevaExistencia) + " " + unidad);
        resultado.setValor(nuevaExistencia);

        // Paso 8 (excepción): el lote ya está dentro del periodo de aviso
        if (datos.getFechaCaducidad() != null
                // RF-59 · Excepción paso 8: «Este lote está próximo a caducar».
                && diasPara(datos.getFechaCaducidad()) <= ConfiguracionApp.getInstancia().getDiasAvisoCaducidad()) {
            resultado.setAviso(MSG_PROXIMO_CADUCAR + " (caduca el " + Formatos.fecha(datos.getFechaCaducidad()) + ")");
        }

        // Si el costo de este lote es distinto al costo registrado del insumo, la Administradora puede actualizarlo
        double costoLote = Math.round(datos.getCostoPorUnidad() * 100) / 100.0;
        if (PermisosInventario.puedeAdministrarInsumos() && costoLote > 0
                && Math.abs(costoLote - insumo.getCostoUnitario()) >= 0.01) {
            resultado.setOfrecerActualizarCosto(true);
            resultado.setIdRelacionado(insumo.getIdInsumo());
        }
        return resultado;
    }

    // Cambia el costo por unidad del insumo al de la última compra (solo la Administradora)
    public ResultadoInventario actualizarCostoUnitario(int idInsumo, double costoUnitario) {
        if (!PermisosInventario.puedeAdministrarInsumos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        if (costoUnitario <= 0) {
            return ResultadoInventario.error(InsumoService.MSG_COSTO_INVALIDO, null);
        }
        try {
            insumoDAO.actualizarCosto(idInsumo, Math.round(costoUnitario * 100) / 100.0);
        } catch (DAOException e) {
            return ResultadoInventario.error(InsumoService.MSG_NO_ACTUALIZADO, null);
        }
        return ResultadoInventario.correcto("Se actualizó el costo por unidad. El costo de las recetas y el precio"
                + " de las opciones que lo usan se recalcularon.");
    }

    // ===================== Salida de insumos / merma (RF-60) =====================

    // Paso 2: el insumo existe, está activo y tiene existencia
    // RF-60 (Registro de salida de insumos / merma): inicia aquí. Paso 2: existe, está activo y tiene existencia.
    public ResultadoInventario revisarParaMerma(int idInsumo) {
        if (!PermisosInventario.puedeRegistrarMovimientos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            Insumo insumo = insumoDAO.buscarPorId(idInsumo);
            if (insumo == null || !insumo.isActivo()) {
                // RF-60 · Excepción paso 2: «Insumo no disponible».
                return ResultadoInventario.noEncontrado(MSG_NO_DISPONIBLE);
            }
            if (insumo.getExistencia() <= 0) {
                // RF-60 · Excepción paso 2: «El insumo no tiene existencias para registrar una merma».
                return ResultadoInventario.error(MSG_SIN_EXISTENCIAS, null);
            }
            return ResultadoInventario.correcto("");
        } catch (DAOException e) {
            // RF-60 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    /**
     * Registra una merma.
     * @param motivo  uno de MOTIVOS_MERMA
     * @param detalle texto libre (obligatorio si el motivo es "Otro")
     * @param idLote  lote del que se descuenta; 0 = por PEPS (los que caducan primero)
     */
    // RF-60 · Pasos 5 a 9: valida y registra la merma.
    public ResultadoInventario registrarMerma(int idInsumo, double cantidad, String motivo, String detalle, int idLote) {
        if (!PermisosInventario.puedeRegistrarMovimientos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }

        // Paso 5: validaciones de captura
        if (cantidad <= 0 || cantidad > MAX_CANTIDAD) {
            // RF-60 · Excepción paso 5: la cantidad debe ser mayor a cero.
            return ResultadoInventario.error(MSG_CANTIDAD_INVALIDA, ResultadoInventario.CAMPO_CANTIDAD);
        }
        if (ValidacionCatalogo.estaVacio(motivo)) {
            // RF-60 · Excepción paso 5: «Indica el motivo de la merma».
            return ResultadoInventario.error(MSG_SIN_MOTIVO, ResultadoInventario.CAMPO_MOTIVO);
        }
        if (MOTIVO_OTRO.equals(motivo) && ValidacionCatalogo.estaVacio(detalle)) {
            return ResultadoInventario.error(MSG_SIN_MOTIVO, ResultadoInventario.CAMPO_DETALLE);
        }
        String textoMotivo = motivo;
        if (!ValidacionCatalogo.estaVacio(detalle)) {
            textoMotivo = motivo + ": " + detalle.trim();
        }
        if (textoMotivo.length() > MAX_MOTIVO) {
            return ResultadoInventario.error("El motivo no puede pasar de 255 caracteres", ResultadoInventario.CAMPO_DETALLE);
        }
        cantidad = Math.round(cantidad * 1000) / 1000.0;

        Insumo insumo;
        try {
            insumo = insumoDAO.buscarPorId(idInsumo);
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
        if (insumo == null || !insumo.isActivo()) {
            return ResultadoInventario.noEncontrado(MSG_NO_DISPONIBLE);
        }

        // Pasos 6 y 7: descuento y movimiento en una sola transacción
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // Se revisa la existencia dentro de la transacción (otra persona pudo descontar antes)
            double disponible = loteDAO.sumarExistencia(conexion, idInsumo);
            // RF-60 · Paso 6: descuenta del lote indicado o, si no se indicó, de los que caducan primero (PEPS).
            if (idLote > 0) {
                Lote lote = loteDAO.buscarPorId(idLote);
                if (lote == null || lote.getIdInsumo() != idInsumo || lote.getExistencia() <= 0) {
                    ApoyoMySQL.deshacer(conexion);
                    return ResultadoInventario.error(MSG_LOTE_NO_DISPONIBLE, ResultadoInventario.CAMPO_LOTE);
                }
                disponible = lote.getExistencia();
            }
            if (cantidad > disponible + 0.0005) {
                // RF-60 · Excepción paso 5: «La cantidad no puede ser mayor a la existencia disponible».
                ApoyoMySQL.deshacer(conexion);
                return ResultadoInventario.error(MSG_SUPERA_EXISTENCIA + " (" + Formatos.cantidad(disponible) + " "
                        + insumo.getUnidad().getValorBD() + ")", ResultadoInventario.CAMPO_CANTIDAD);
            }

            descontarPEPS(conexion, idInsumo, cantidad, TipoMovimiento.MERMA, 0, textoMotivo, idLote);
            conexion.commit();
        } catch (SQLException e) {
            // RF-60 · Excepción paso 6: se deshace la operación, «No fue posible registrar la merma».
            ApoyoMySQL.deshacer(conexion);
            return ResultadoInventario.error(MSG_MERMA_FALLO, ResultadoInventario.CAMPO_CONEXION);
        } catch (DAOException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoInventario.error(mensajeDeFallo(MSG_MERMA_FALLO, e), ResultadoInventario.CAMPO_CONEXION);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }

        // Paso 8: se recalculan las alertas (puede activarse la de stock bajo)
        MonitorAlertas.getInstancia().revisar();

        double nuevaExistencia = insumo.getExistencia() - cantidad;
        ResultadoInventario resultado = ResultadoInventario.correcto("Merma registrada.\nNueva existencia de «"
                + insumo.getNombre() + "»: " + Formatos.cantidad(nuevaExistencia) + " " + insumo.getUnidad().getValorBD());
        resultado.setValor(nuevaExistencia);
        return resultado;
    }

    // ===================== Descuento por PEPS (lo usan la merma y RF-63) =====================

    /**
     * Descuenta una cantidad de los lotes del insumo y registra UN movimiento de salida.
     * Debe llamarse dentro de una transacción: no hace commit ni rollback.
     *
     * @param idLote 0 = por PEPS (primero el lote que caduca antes); otro valor = solo de ese lote
     * @return el costo de lo descontado (según el costo de compra de cada lote afectado)
     * @throws DAOException si la existencia no alcanza o falla la base de datos
     */
    // RF-60 paso 6 y RF-63: descuento de existencias por lotes, primero los que caducan antes.
    public double descontarPEPS(Connection conexion, int idInsumo, double cantidad, TipoMovimiento tipo,
                                int folio, String motivo, int idLote) throws DAOException {
        double porDescontar = Math.round(cantidad * 1000) / 1000.0;
        double costo = 0;

        List<Lote> lotes = loteDAO.bloquearParaDescuento(conexion, idInsumo);
        for (Lote lote : lotes) {
            if (porDescontar <= 0.0005) {
                break;
            }
            if (idLote > 0 && lote.getIdLote() != idLote) {
                continue;
            }
            // Se toma lo que alcance de este lote, sin dejarlo en negativo
            double tomado = porDescontar;
            if (tomado > lote.getExistencia()) {
                tomado = lote.getExistencia();
            }
            double queda = Math.round((lote.getExistencia() - tomado) * 1000) / 1000.0;
            loteDAO.actualizarExistencia(conexion, lote.getIdLote(), queda);

            costo = costo + tomado * lote.getCostoPorUnidad();
            porDescontar = Math.round((porDescontar - tomado) * 1000) / 1000.0;
        }

        if (porDescontar > 0.0005) {
            throw new DAOException("La existencia del insumo " + idInsumo + " no alcanza: faltan " + porDescontar);
        }

        Movimiento salida = new Movimiento();
        salida.setIdInsumo(idInsumo);
        salida.setIdUsuario(PermisosInventario.idUsuarioSesion());
        salida.setFolio(folio);
        salida.setTipo(tipo);
        salida.setCantidad(-Math.round(cantidad * 1000) / 1000.0);
        salida.setMotivo(motivo);
        salida.setCosto(Math.round(costo * 100) / 100.0);
        movimientoDAO.insertar(conexion, salida);
        return salida.getCosto();
    }

    // ===================== Apoyo =====================

    // RF-59 paso 5: revisa los datos de la compra; devuelve null si todo está bien
    private ResultadoInventario validarEntrada(Lote datos, Insumo insumo) {
        // RF-59 · Excepción paso 5: cantidad mayor a cero.
        if (datos.getCantidadComprada() <= 0 || datos.getCantidadComprada() > MAX_CANTIDAD) {
            return ResultadoInventario.error(MSG_CANTIDAD_INVALIDA, ResultadoInventario.CAMPO_CANTIDAD);
        }
        // RF-59 · Excepción paso 5: costo mayor a cero.
        if (datos.getCostoCompra() <= 0 || datos.getCostoCompra() > MAX_COSTO) {
            return ResultadoInventario.error(MSG_COSTO_INVALIDO, ResultadoInventario.CAMPO_COSTO);
        }
        if (datos.getFechaCompra() == null) {
            return ResultadoInventario.error(MSG_COMPRA_VACIA, ResultadoInventario.CAMPO_FECHA_COMPRA);
        }
        // RF-59 · Excepción paso 5: «La fecha de compra no puede ser futura».
        if (datos.getFechaCompra().isAfter(LocalDate.now())) {
            return ResultadoInventario.error(MSG_COMPRA_FUTURA, ResultadoInventario.CAMPO_FECHA_COMPRA);
        }
        if (datos.getFechaCaducidad() == null && insumo.isPerecedero()) {
            return ResultadoInventario.error(MSG_CADUCIDAD_OBLIGATORIA, ResultadoInventario.CAMPO_CADUCIDAD);
        }
        if (datos.getFechaCaducidad() != null) {
            // RF-59 · Excepción paso 5: «La fecha de caducidad debe ser posterior a hoy».
            if (!datos.getFechaCaducidad().isAfter(LocalDate.now())) {
                return ResultadoInventario.error(MSG_CADUCIDAD_PASADA, ResultadoInventario.CAMPO_CADUCIDAD);
            }
            if (datos.getFechaCaducidad().isBefore(datos.getFechaCompra())) {
                return ResultadoInventario.error(MSG_CADUCIDAD_ANTES_COMPRA, ResultadoInventario.CAMPO_CADUCIDAD);
            }
        }
        if (!ValidacionCatalogo.largoValido(datos.getProveedor(), MAX_PROVEEDOR)) {
            return ResultadoInventario.error(MSG_PROVEEDOR_LARGO, ResultadoInventario.CAMPO_PROVEEDOR);
        }
        datos.setCantidadComprada(Math.round(datos.getCantidadComprada() * 1000) / 1000.0);
        datos.setCostoCompra(Math.round(datos.getCostoCompra() * 100) / 100.0);
        return null;
    }

    // Si falta la migración V3 se explica el motivo; si no, el mensaje normal
    private String mensajeDeFallo(String mensajeNormal, DAOException e) {
        if (MovimientoDAO.MSG_FALTA_MIGRACION.equals(e.getMessage())) {
            return mensajeNormal + ".\n" + MovimientoDAO.MSG_FALTA_MIGRACION;
        }
        return mensajeNormal;
    }
}

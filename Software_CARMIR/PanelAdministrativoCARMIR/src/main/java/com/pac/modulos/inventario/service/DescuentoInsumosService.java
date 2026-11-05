package com.pac.modulos.inventario.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.ConsumoPedidoDAO;
import com.pac.modulos.inventario.dao.LoteDAO;
import com.pac.modulos.inventario.dao.MovimientoDAO;
import com.pac.modulos.inventario.dao.RecetaDAO;
import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.modulos.inventario.model.NecesidadInsumo;
import com.pac.modulos.inventario.model.OpcionPedido;
import com.pac.modulos.inventario.model.TipoMovimiento;
import com.pac.util.Formatos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Descuento automático de insumos cuando un pedido pasa de "Pagado" a "En preparación" (RF-63).
 *
 * Cómo lo usa el Gestor de Pedidos (Módulo 4):
 *   1. Abre la transacción del cambio de estado.
 *   2. ANTES de cambiar EstadoPedido llama a descontarPorPedido(folio, conexion, continuarSinReceta).
 *   3. Si permiteContinuar() es true, cambia el estado y hace commit; si no, hace rollback.
 *   4. Después del commit llama a MonitorAlertas.getInstancia().revisar().
 * Si prefiere no manejar la transacción, puede usar descontarYConfirmar(folio, continuarSinReceta).
 *
 * Cálculo por cada postre del pedido: para cada opción con receta activa, cada ingrediente se multiplica
 * por la cantidad elegida, por las piezas del pedido y, si la opción se escala, por el factor de escala
 * (producto de MultiplicadorIngredientes de las opciones que NO se escalan), igual que vw_CostoPrecioPostre.
 */
public class DescuentoInsumosService {

    public static final String ESTADO_PAGADO = "Pagado";
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_DISPONIBLE = "El pedido no está disponible (no existe o fue cancelado)";
    public static final String MSG_NO_PAGADO = "Solo se descuentan insumos de pedidos en estado «Pagado»";
    public static final String MSG_YA_DESCONTADO = "Al pedido ya se le descontaron insumos; no se vuelve a descontar";
    public static final String MSG_FALTAN = "No hay existencias suficientes. No se descontó nada";
    public static final String MSG_FALLO = "No fue posible descontar los insumos";

    private ConsumoPedidoDAO consumoDAO;
    private RecetaDAO recetaDAO;
    private LoteDAO loteDAO;
    private MovimientoDAO movimientoDAO;
    private InventarioService inventarioService;

    public DescuentoInsumosService() {
        consumoDAO = DAOFactory.crearConsumoPedidoDAO();
        recetaDAO = DAOFactory.crearRecetaDAO();
        loteDAO = DAOFactory.crearLoteDAO();
        movimientoDAO = DAOFactory.crearMovimientoDAO();
        inventarioService = new InventarioService();
    }

    /**
     * Descuenta los insumos del pedido dentro de la transacción que recibe (no hace commit ni rollback).
     * Si el resultado no es DESCONTADO, no modificó nada en la base de datos.
     *
     * @param continuarSinReceta true = la Administradora ya aceptó continuar aunque haya opciones sin receta activa
     */
    // RF-63 (Descuento automático de insumos en inventario): inicia aquí. Paso 1: recibe el folio del pedido que pasa a «En preparación».
    public ResultadoDescuento descontarPorPedido(int folio, Connection conexion, boolean continuarSinReceta) {
        try {
            // Paso 2: el pedido existe, no está cancelado y está en "Pagado"
            String estado = consumoDAO.buscarEstado(conexion, folio);
            if (estado == null || consumoDAO.estaCancelado(conexion, folio)) {
                // RF-63 · Excepción paso 2: el pedido no existe o está cancelado.
                return new ResultadoDescuento(ResultadoDescuento.PEDIDO_NO_DISPONIBLE, MSG_NO_DISPONIBLE);
            }
            if (!ESTADO_PAGADO.equalsIgnoreCase(estado)) {
                // RF-63 · Excepción paso 2: el estado previo no es «Pagado»; se bloquea el cambio.
                return new ResultadoDescuento(ResultadoDescuento.ESTADO_INVALIDO,
                        MSG_NO_PAGADO + " (estado actual: " + estado + ")");
            }
            // Paso 2: no se descuenta dos veces
            if (movimientoDAO.existeProduccionDeFolio(conexion, folio)) {
                System.out.println("Aviso: el pedido #" + folio + " ya tenía insumos descontados");
                // RF-63 · Excepción paso 2: ya se descontaron insumos; no se repite y se permite continuar.
                return new ResultadoDescuento(ResultadoDescuento.YA_DESCONTADO, MSG_YA_DESCONTADO);
            }

            // Paso 3: opciones del pedido y sus recetas activas
            List<OpcionPedido> opciones = consumoDAO.listarOpciones(conexion, folio);
            List<String> sinReceta = opcionesSinReceta(opciones);
            // RF-63 · Excepción paso 3: «La opción [nombre] no tiene receta activa»; la administradora decide si continúa.
            if (!sinReceta.isEmpty() && !continuarSinReceta) {
                String mensaje = "";
                for (String nombre : sinReceta) {
                    mensaje = mensaje + "La opción " + nombre + " no tiene receta activa\n";
                }
                ResultadoDescuento resultado = new ResultadoDescuento(ResultadoDescuento.SIN_RECETA,
                        mensaje + "¿Deseas continuar sin descontar esas opciones?");
                resultado.setOpcionesSinReceta(sinReceta);
                return resultado;
            }

            // Paso 4: total de cada insumo
            List<NecesidadInsumo> necesidades = calcularNecesidades(conexion, opciones);

            // Paso 5: la existencia debe alcanzar para todos; si no, no se descuenta nada
            List<NecesidadInsumo> faltantes = new ArrayList<NecesidadInsumo>();
            for (NecesidadInsumo necesidad : necesidades) {
                necesidad.setExistencia(loteDAO.sumarExistencia(conexion, necesidad.getIdInsumo()));
                if (necesidad.falta()) {
                    faltantes.add(necesidad);
                }
            }
            // RF-63 · Excepción paso 5: no alcanzan las existencias; no se descuenta nada y el pedido sigue en «Pagado».
            if (!faltantes.isEmpty()) {
                String mensaje = MSG_FALTAN + ". Faltan:";
                for (NecesidadInsumo faltante : faltantes) {
                    String unidad = faltante.getUnidad().getValorBD();
                    mensaje = mensaje + "\n• " + faltante.getNombreInsumo() + ": se necesitan "
                            + Formatos.cantidad(faltante.getCantidadNecesaria()) + " " + unidad + " y hay "
                            + Formatos.cantidad(faltante.getExistencia()) + " " + unidad;
                }
                ResultadoDescuento resultado = new ResultadoDescuento(ResultadoDescuento.FALTAN_INSUMOS, mensaje);
                resultado.setInsumos(faltantes);
                resultado.setOpcionesSinReceta(sinReceta);
                return resultado;
            }

            // Pasos 6 y 7: descuento por PEPS y un movimiento "Produccion" por insumo con el folio
            for (NecesidadInsumo necesidad : necesidades) {
                inventarioService.descontarPEPS(conexion, necesidad.getIdInsumo(), necesidad.getCantidadNecesaria(),
                        TipoMovimiento.PRODUCCION, folio, "Producción del pedido #" + folio, 0);
            }

            ResultadoDescuento resultado = new ResultadoDescuento(ResultadoDescuento.DESCONTADO,
                    "Se descontaron " + necesidades.size() + " insumo(s) del inventario para el pedido #" + folio);
            resultado.setInsumos(necesidades);
            resultado.setOpcionesSinReceta(sinReceta);
            return resultado;
        } catch (DAOException e) {
            // Paso 6 (excepción): quien llamó debe deshacer toda la transacción
            String mensaje = MSG_FALLO;
            if (MovimientoDAO.MSG_FALTA_MIGRACION.equals(e.getMessage())) {
                mensaje = MSG_FALLO + ".\n" + MovimientoDAO.MSG_FALTA_MIGRACION;
            }
            return new ResultadoDescuento(ResultadoDescuento.ERROR, mensaje);
        }
    }

    /**
     * Versión que maneja su propia transacción: descuenta y hace commit, o deshace todo.
     * No cambia el estado del pedido (eso le toca al Gestor de Pedidos).
     */
    public ResultadoDescuento descontarYConfirmar(int folio, boolean continuarSinReceta) {
        Connection conexion = null;
        ResultadoDescuento resultado;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);
            resultado = descontarPorPedido(folio, conexion, continuarSinReceta);
            // RF-63 · Paso 9: confirma el descuento.
            if (resultado.getResultado() == ResultadoDescuento.DESCONTADO) {
                conexion.commit();
            } else {
                conexion.rollback();
            }
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            // RF-63 · Excepción paso 2: sin conexión, «Error de conexión»; el pedido sigue en «Pagado».
            return new ResultadoDescuento(ResultadoDescuento.ERROR, MSG_ERROR_CONEXION);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }

        // RF-63 · Excepción paso 8: si la alerta falla se mantiene el descuento y se genera en la siguiente revisión.
        // Paso 8: con el descuento ya guardado se revisan las alertas (si falla, el descuento se mantiene)
        if (resultado.getResultado() == ResultadoDescuento.DESCONTADO) {
            MonitorAlertas.getInstancia().revisar();
        }
        return resultado;
    }

    /**
     * Estimación de insumos para la Agenda de Producción (RF-46): suma lo que necesitan los pedidos
     * indicados y le pone a cada insumo su existencia actual. No descuenta nada.
     * Las opciones sin receta activa no se cuentan.
     */
    public List<NecesidadInsumo> estimarNecesidades(List<Integer> folios) throws DAOException {
        List<NecesidadInsumo> total = new ArrayList<NecesidadInsumo>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            // Se calcula pedido por pedido (el factor de escala es de cada postre del pedido)
            for (Integer folio : folios) {
                List<OpcionPedido> opciones = consumoDAO.listarOpciones(conexion, folio);
                List<NecesidadInsumo> delPedido = calcularNecesidades(conexion, opciones);
                for (NecesidadInsumo necesidad : delPedido) {
                    NecesidadInsumo acumulada = buscarPorId(total, necesidad);
                    acumulada.sumar(necesidad.getCantidadNecesaria());
                }
            }
            for (NecesidadInsumo necesidad : total) {
                necesidad.redondear();
                necesidad.setExistencia(loteDAO.sumarExistencia(conexion, necesidad.getIdInsumo()));
            }
        } catch (SQLException e) {
            throw new DAOException(MSG_ERROR_CONEXION, e);
        }
        return total;
    }

    // ===================== Apoyo =====================

    // Nombres (sin repetir) de las opciones del pedido que no tienen receta activa
    private List<String> opcionesSinReceta(List<OpcionPedido> opciones) {
        List<String> nombres = new ArrayList<String>();
        for (OpcionPedido opcion : opciones) {
            if (!opcion.tieneRecetaActiva() && !nombres.contains(opcion.getNombreOpcion())) {
                nombres.add(opcion.getNombreOpcion());
            }
        }
        return nombres;
    }

    // Suma lo que necesita cada insumo, postre por postre
    // RF-63 · Paso 4: total de cada insumo según recetas, unidades pedidas y multiplicadores.
    private List<NecesidadInsumo> calcularNecesidades(Connection conexion, List<OpcionPedido> opciones)
            throws DAOException {
        List<NecesidadInsumo> necesidades = new ArrayList<NecesidadInsumo>();

        for (OpcionPedido opcion : opciones) {
            if (!opcion.tieneRecetaActiva()) {
                continue;
            }
            // Por capas: cada capa pone su parte (por ejemplo medio sabor si son 2 capas)
            double multiplicador = opcion.getCantidadElegida() * opcion.getCantidadPostres() * opcion.getParteCapa();
            if (opcion.isSeEscala()) {
                multiplicador = multiplicador * factorEscala(opciones, opcion.getIdPostre());
            }
            List<IngredienteReceta> ingredientes = recetaDAO.listarIngredientes(conexion, opcion.getIdRecetaActiva());
            for (IngredienteReceta ingrediente : ingredientes) {
                NecesidadInsumo necesidad = buscarNecesidad(necesidades, ingrediente);
                necesidad.sumar(ingrediente.getCantidad() * multiplicador);
            }
        }
        for (NecesidadInsumo necesidad : necesidades) {
            necesidad.redondear();
        }
        return necesidades;
    }

    // Factor de escala de un postre = producto de MultiplicadorIngredientes de sus opciones que NO se escalan
    // (por capas, cada fila pone MultiplicadorIngredientes ^ su parte, igual que la vista de precios)
    private double factorEscala(List<OpcionPedido> opciones, int idPostre) {
        double factor = 1;
        for (OpcionPedido opcion : opciones) {
            if (opcion.getIdPostre() == idPostre && !opcion.isSeEscala()) {
                factor = factor * Math.pow(opcion.getMultiplicadorIngredientes(), opcion.getParteCapa());
            }
        }
        return factor;
    }

    // Busca el insumo en la lista; si no está, lo agrega en cero
    private NecesidadInsumo buscarNecesidad(List<NecesidadInsumo> necesidades, IngredienteReceta ingrediente) {
        for (NecesidadInsumo necesidad : necesidades) {
            if (necesidad.getIdInsumo() == ingrediente.getIdInsumo()) {
                return necesidad;
            }
        }
        NecesidadInsumo nueva = new NecesidadInsumo(ingrediente.getIdInsumo(), ingrediente.getNombreInsumo(),
                ingrediente.getUnidad());
        necesidades.add(nueva);
        return nueva;
    }

    // Busca el insumo en la lista total; si no está, lo agrega en cero
    private NecesidadInsumo buscarPorId(List<NecesidadInsumo> total, NecesidadInsumo necesidad) {
        for (NecesidadInsumo acumulada : total) {
            if (acumulada.getIdInsumo() == necesidad.getIdInsumo()) {
                return acumulada;
            }
        }
        NecesidadInsumo nueva = new NecesidadInsumo(necesidad.getIdInsumo(), necesidad.getNombreInsumo(),
                necesidad.getUnidad());
        total.add(nueva);
        return nueva;
    }
}

package com.pac.modulos.inventario.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.inventario.dao.AlertaDAO;
import com.pac.modulos.inventario.dao.InsumoDAO;
import com.pac.modulos.inventario.dao.LoteDAO;
import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.model.TipoAlerta;
import com.pac.util.ConfiguracionApp;

import java.util.List;

/**
 * Alertas de existencia baja (RF-61) y de caducidad próxima (RF-62).
 *
 * revisar() compara todo el inventario y deja las alertas al día:
 * - crea las que faltan, sin duplicar las que ya están pendientes;
 * - actualiza la existencia o los días de las que siguen pendientes;
 * - da por atendidas las que ya no aplican (se compró más, se acabó el lote, etc.).
 * La usa MonitorAlertas al iniciar sesión, cada hora y después de cada movimiento.
 */
public class AlertaService {

    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_REVISO_CADUCIDADES = "No fue posible revisar las caducidades";
    public static final String MSG_ALERTA_NO_ENCONTRADA = "La alerta ya no existe";
    public static final String MSG_YA_ATENDIDA = "La alerta ya estaba atendida";

    private AlertaDAO alertaDAO;
    private InsumoDAO insumoDAO;
    private LoteDAO loteDAO;

    public AlertaService() {
        alertaDAO = DAOFactory.crearAlertaDAO();
        insumoDAO = DAOFactory.crearInsumoDAO();
        loteDAO = DAOFactory.crearLoteDAO();
    }

    // ===================== Consultas =====================

    public List<AlertaInventario> listar(boolean soloPendientes) throws DAOException {
        return alertaDAO.listar(soloPendientes);
    }

    public int contarPendientes() throws DAOException {
        return alertaDAO.contarPendientes();
    }

    public AlertaInventario buscarPorId(int idAlerta) throws DAOException {
        return alertaDAO.buscarPorId(idAlerta);
    }

    // ===================== Revisión automática =====================

    // Revisa existencias y caducidades; devuelve cuántas alertas quedaron pendientes
    // RF-61 y RF-62: inicia aquí la revisión automática de alertas (la llama MonitorAlertas).
    public int revisar() throws DAOException {
        revisarExistencias();
        revisarCaducidades();
        return alertaDAO.contarPendientes();
    }

    // RF-61: existencia igual o menor al stock mínimo
    // RF-61 (Alerta de existencias bajas de insumos): inicia aquí. Pasos 1 y 2: recorre los insumos con su existencia.
    private void revisarExistencias() throws DAOException {
        List<Insumo> insumos = insumoDAO.listar(true);
        for (Insumo insumo : insumos) {
            AlertaInventario pendiente = alertaDAO.buscarPendienteDeExistencia(insumo.getIdInsumo());

            // RF-61 · Excepción paso 2 y paso 3: insumo inactivo o sin stock mínimo, no se genera alerta.
            // Insumo dado de baja o sin stock mínimo: no genera alerta (y se cierra la que tuviera)
            if (!insumo.isActivo() || !insumo.tieneStockMinimo()) {
                if (pendiente != null) {
                    alertaDAO.marcarAtendida(pendiente.getIdAlerta());
                }
                continue;
            }

            // RF-61 · Paso 4: compara la existencia con el stock mínimo.
            if (insumo.estaBajoMinimo()) {
                // RF-61 · Paso 6: registra la alerta de stock bajo.
                if (pendiente == null) {
                    AlertaInventario nueva = new AlertaInventario();
                    nueva.setIdInsumo(insumo.getIdInsumo());
                    nueva.setTipo(TipoAlerta.EXISTENCIA_BAJA);
                    nueva.setExistenciaEnAlerta(insumo.getExistencia());
                    alertaDAO.insertar(nueva);
                } else if (Math.abs(pendiente.getExistenciaEnAlerta() - insumo.getExistencia()) > 0.0005) {
                    // RF-61 · Excepción paso 5: ya había alerta activa, se actualiza sin duplicarla.
                    // Ya tiene alerta: solo se actualiza la existencia que muestra
                    alertaDAO.actualizar(pendiente.getIdAlerta(), TipoAlerta.EXISTENCIA_BAJA, insumo.getExistencia());
                }
            } else if (pendiente != null) {
                // RF-61 · Excepción paso 4: ya no está bajo el mínimo; se desactiva la alerta.
                // La existencia ya se recuperó: la alerta deja de aplicar
                alertaDAO.marcarAtendida(pendiente.getIdAlerta());
            }
        }
    }

    // RF-62: lotes con existencia que ya caducaron o están dentro del periodo de aviso
    // RF-62 (Alerta de caducidad próxima de insumos): inicia aquí. Paso 2: consulta los lotes con existencia.
    private void revisarCaducidades() throws DAOException {
        int diasAviso = ConfiguracionApp.getInstancia().getDiasAvisoCaducidad();
        List<Lote> lotes = loteDAO.listarConExistencia();

        for (Lote lote : lotes) {
            // RF-62 · Excepción paso 3: lote sin caducidad (no perecedero), se omite.
            // Lotes sin caducidad (insumos no perecederos): se omiten
            if (lote.getFechaCaducidad() == null) {
                continue;
            }
            // RF-62 · Paso 4: compara la caducidad con hoy y con el periodo de aviso.
            int dias = InventarioService.diasPara(lote.getFechaCaducidad());
            TipoAlerta tipo = null;
            // RF-62 · Excepción paso 4: el lote ya caducó, se marca «Caducado».
            if (dias < 0) {
                tipo = TipoAlerta.CADUCADO;
            } else if (dias <= diasAviso) {
                tipo = TipoAlerta.PROXIMA_CADUCIDAD;
            }

            AlertaInventario pendiente = alertaDAO.buscarPendienteDeLote(lote.getIdLote());
            if (tipo == null) {
                // RF-62 · Excepción paso 4: fuera del periodo de aviso, no hay alerta.
                // Fuera del periodo de aviso: no hay alerta (se cierra si había una)
                if (pendiente != null) {
                    alertaDAO.marcarAtendida(pendiente.getIdAlerta());
                }
                continue;
            }

            // RF-62 · Paso 5: registra una alerta por cada lote próximo a caducar.
            if (pendiente == null) {
                AlertaInventario nueva = new AlertaInventario();
                nueva.setIdInsumo(lote.getIdInsumo());
                nueva.setTipo(tipo);
                nueva.setExistenciaEnAlerta(lote.getExistencia());
                nueva.setIdLote(lote.getIdLote());
                alertaDAO.insertar(nueva);
            } else if (pendiente.getTipo() != tipo
                    || Math.abs(pendiente.getExistenciaEnAlerta() - lote.getExistencia()) > 0.0005) {
                // RF-62 · Excepción paso 5: el lote ya tenía alerta; se actualiza sin duplicarla.
                // Sin duplicar: la misma alerta pasa de "Próxima caducidad" a "Caducado" o cambia su existencia
                alertaDAO.actualizar(pendiente.getIdAlerta(), tipo, lote.getExistencia());
            }
        }

        // Las alertas de lotes que ya no tienen existencia (se usaron o se registró la merma) se cierran
        for (AlertaInventario alerta : alertaDAO.listarPendientesDeCaducidad()) {
            if (alerta.getExistenciaLote() <= 0.0005) {
                alertaDAO.marcarAtendida(alerta.getIdAlerta());
            }
        }
    }

    // ===================== Atender alertas =====================

    public ResultadoInventario marcarAtendida(int idAlerta) {
        if (!PermisosInventario.puedeRegistrarMovimientos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            AlertaInventario alerta = alertaDAO.buscarPorId(idAlerta);
            if (alerta == null) {
                return ResultadoInventario.noEncontrado(MSG_ALERTA_NO_ENCONTRADA);
            }
            if (!alerta.isPendiente()) {
                return ResultadoInventario.noEncontrado(MSG_YA_ATENDIDA);
            }
            alertaDAO.marcarAtendida(idAlerta);
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
        MonitorAlertas.getInstancia().actualizarConteo();
        return ResultadoInventario.correcto("La alerta se marcó como atendida");
    }

    // Días que le faltan al lote de una alerta de caducidad (como la vista vw_AlertaCaducidad, pero contra hoy)
    public static int diasParaCaducar(AlertaInventario alerta) {
        if (alerta.getFechaCaducidad() == null) {
            return 0;
        }
        return InventarioService.diasPara(alerta.getFechaCaducidad());
    }
}

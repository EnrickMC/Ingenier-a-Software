package com.pac.modulos.catalogo.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.dao.ExclusionDAO;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.dao.CaracteristicaDAO;

import java.util.List;

/**
 * Reglas de las exclusiones entre opciones (Exclusiones entre opciones de características).
 * Una exclusión activa impide que el cliente elija juntas las dos opciones.
 */
public class ExclusionService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_OPCION_NO_ENCONTRADA = "Opción no encontrada";
    public static final String MSG_YA_REGISTRADA = "Esta exclusión ya está registrada";
    public static final String MSG_NO_GUARDADA = "No fue posible guardar la exclusión";
    public static final String MSG_NO_ENCONTRADA = "La exclusión ya no existe";
    public static final String MSG_ELIGE_OPCIONES = "Elige las dos opciones que no se pueden combinar";
    public static final String MSG_MISMA_CARACTERISTICA =
            "La opción excluida debe ser de otra característica de la misma categoría";
    public static final String MSG_MOTIVO = "Escribe el motivo de la exclusión (máximo 255 caracteres)";

    private static final int MAX_MOTIVO = 255;

    private ExclusionDAO exclusionDAO;
    private OpcionDAO opcionDAO;
    private CaracteristicaDAO caracteristicaDAO;

    public ExclusionService() {
        exclusionDAO = DAOFactory.crearExclusionDAO();
        opcionDAO = DAOFactory.crearOpcionDAO();
        caracteristicaDAO = DAOFactory.crearCaracteristicaDAO();
    }

    // ===================== Consultas =====================

    // Exclusiones activas e inactivas de una categoría
    public List<Exclusion> listarPorCategoria(int idCategoria) throws DAOException {
        return exclusionDAO.listarPorCategoria(idCategoria);
    }

    // Exclusiones en las que participa una opción
    public List<Exclusion> listarPorOpcion(int idOpcion) throws DAOException {
        return exclusionDAO.listarPorOpcion(idOpcion);
    }

    // Exclusiones activas entre las opciones de la lista.
    // La usan el formulario de productos y, más adelante, el gestor de pedidos y la API.
    // RF-3 · Paso 7: exclusiones activas entre las opciones elegidas (las usan productos, pedidos y la API).
    public List<Exclusion> opcionesExcluidas(List<Integer> idsOpcion) throws DAOException {
        return exclusionDAO.listarActivasEntre(idsOpcion);
    }

    // ===================== Registro =====================

    // RF «Exclusiones entre opciones de características»: inicia aquí la regla de negocio (pasos 5 a 7).
    public ResultadoCatalogo registrar(Exclusion datos) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        // Paso 5: las dos opciones, distintas, y el motivo
        // RF «Exclusiones entre opciones de características» · Excepción paso 5: falta elegir alguna de las dos opciones.
        if (datos.getIdOpcion() <= 0) {
            return ResultadoCatalogo.error(MSG_ELIGE_OPCIONES, ResultadoCatalogo.CAMPO_OPCION_A);
        }
        if (datos.getIdOpcionExcluida() <= 0) {
            return ResultadoCatalogo.error(MSG_ELIGE_OPCIONES, ResultadoCatalogo.CAMPO_OPCION_B);
        }
        // RF «Exclusiones entre opciones de características» · Excepción paso 5: falta el motivo.
        if (ValidacionCatalogo.estaVacio(datos.getMotivo())
                || !ValidacionCatalogo.largoValido(datos.getMotivo(), MAX_MOTIVO)) {
            return ResultadoCatalogo.error(MSG_MOTIVO, ResultadoCatalogo.CAMPO_MOTIVO);
        }
        if (datos.getEstado() == null) {
            datos.setEstado(EstadoRegistro.ACTIVA);
        }

        try {
            // Paso 2: la opción existe
            Opcion opcionA = opcionDAO.buscarPorId(datos.getIdOpcion());
            // RF «Exclusiones entre opciones de características» · Excepción paso 2: «Opción no encontrada».
            if (opcionA == null) {
                return ResultadoCatalogo.noEncontrado(MSG_OPCION_NO_ENCONTRADA);
            }
            Opcion opcionB = opcionDAO.buscarPorId(datos.getIdOpcionExcluida());
            if (opcionB == null) {
                return ResultadoCatalogo.noEncontrado(MSG_OPCION_NO_ENCONTRADA);
            }

            // Paso 5: de otra característica, pero de la misma categoría
            // RF «Exclusiones entre opciones de características» · Excepción paso 5: la opción excluida es de la misma característica u otra categoría.
            if (opcionA.getIdCaracteristica() == opcionB.getIdCaracteristica()) {
                return ResultadoCatalogo.error(MSG_MISMA_CARACTERISTICA, ResultadoCatalogo.CAMPO_OPCION_B);
            }
            Caracteristica caracteristicaA = caracteristicaDAO.buscarPorId(opcionA.getIdCaracteristica());
            Caracteristica caracteristicaB = caracteristicaDAO.buscarPorId(opcionB.getIdCaracteristica());
            if (caracteristicaA == null || caracteristicaB == null
                    || caracteristicaA.getIdCategoria() != caracteristicaB.getIdCategoria()) {
                return ResultadoCatalogo.error(MSG_MISMA_CARACTERISTICA, ResultadoCatalogo.CAMPO_OPCION_B);
            }

            // Paso 6: ya existe en cualquiera de los dos sentidos
            Exclusion existente = exclusionDAO.buscarPar(datos.getIdOpcion(), datos.getIdOpcionExcluida());
            // RF «Exclusiones entre opciones de características» · Excepción paso 6: «Esta exclusión ya está registrada» con su estado actual.
            if (existente != null) {
                return ResultadoCatalogo.error(MSG_YA_REGISTRADA + " (estado: " + existente.getEstado().getValorBD() + ")",
                        ResultadoCatalogo.CAMPO_OPCION_B);
            }
        } catch (DAOException e) {
            // RF «Exclusiones entre opciones de características» · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 7: se guarda
        try {
            datos.setMotivo(datos.getMotivo().trim());
            exclusionDAO.insertar(datos);
        } catch (DAOException e) {
            // RF «Exclusiones entre opciones de características» · Excepción paso 7: «No fue posible guardar la exclusión».
            return ResultadoCatalogo.error(MSG_NO_GUARDADA, ResultadoCatalogo.CAMPO_CONEXION);
        }
        return ResultadoCatalogo.correcto("La exclusión se registró correctamente");
    }

    // ===================== Cambio de estado y eliminación =====================

    // Activa o inactiva una exclusión
    // RF «Exclusiones entre opciones de características» · Pasos 4 y 7: cambia el estado de una exclusión (la activa se aplica de inmediato, paso 8).
    public ResultadoCatalogo cambiarEstado(int idExclusion, EstadoRegistro estado) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            // RF «Exclusiones entre opciones de características» · Excepción paso 2: la exclusión ya no existe.
            if (exclusionDAO.buscarPorId(idExclusion) == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }
            exclusionDAO.cambiarEstado(idExclusion, estado);
        } catch (DAOException e) {
            return ResultadoCatalogo.error(MSG_NO_GUARDADA, null);
        }
        if (estado == EstadoRegistro.ACTIVA) {
            return ResultadoCatalogo.correcto("La exclusión se activó; se aplica de inmediato en la personalización");
        }
        return ResultadoCatalogo.correcto("La exclusión se inactivó");
    }

    // Borra una exclusión
    public ResultadoCatalogo eliminar(int idExclusion) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            if (exclusionDAO.buscarPorId(idExclusion) == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }
            exclusionDAO.eliminar(idExclusion);
        } catch (DAOException e) {
            return ResultadoCatalogo.error("No fue posible eliminar la exclusión", null);
        }
        return ResultadoCatalogo.correcto("La exclusión se eliminó correctamente");
    }
}

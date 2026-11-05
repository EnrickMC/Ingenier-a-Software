package com.pac.modulos.inventario.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.dao.InsumoDAO;
import com.pac.modulos.inventario.dao.RecetaDAO;
import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.OpcionReceta;
import com.pac.modulos.inventario.model.Receta;
import com.pac.util.Formatos;

import java.util.List;

/**
 * Reglas de las recetas: Visualización (RF-55), Registro (RF-56),
 * Modificación (RF-57) y Eliminación (RF-58).
 *
 * Cada opción del catálogo tiene como máximo una receta. Cuando la receta está activa,
 * el catálogo calcula con ella el costo y el precio de la opción y de los productos.
 */
public class RecetaService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_SIN_RECETAS = "Aún no hay recetas registradas";
    public static final String MSG_SIN_RESULTADOS = "No hay recetas que coincidan con la búsqueda";
    public static final String MSG_NO_DISPONIBLE = "La receta ya no está disponible";
    public static final String MSG_YA_NO_EXISTE = "La receta ya no existe";
    public static final String MSG_SIN_OPCIONES = "No hay opciones pendientes de receta";
    public static final String MSG_OPCION_NO_DISPONIBLE = "La opción ya no está disponible";
    public static final String MSG_OPCION_SIN_RECETA = "Esta característica está marcada como «sin receta» en el catálogo";
    public static final String MSG_OPCION_CON_RECETA = "Esta opción ya tiene una receta registrada";
    public static final String MSG_INSUMO_NO_DISPONIBLE = "El insumo ya no está disponible";
    public static final String MSG_REQUIERE_REVISION = "Esta receta requiere revisión";
    public static final String MSG_INSUMO_DE_BAJA = "Insumo dado de baja";
    public static final String MSG_EN_PEDIDOS = "La receta se necesita para pedidos en curso";
    public static final String MSG_NO_REGISTRADA = "No fue posible registrar la receta";
    public static final String MSG_NO_ACTUALIZADA = "No fue posible actualizar la receta";
    public static final String MSG_NO_DADA_DE_BAJA = "No fue posible dar de baja la receta";
    public static final String MSG_ELIGE_OPCION = "Elige la opción de característica";
    public static final String MSG_NOMBRE_INVALIDO = "Escribe el nombre de la receta (máximo 100 caracteres)";
    public static final String MSG_TIEMPO_INVALIDO = "El tiempo de elaboración debe ser un número entero de minutos mayor a cero";
    public static final String MSG_PASOS_VACIOS = "Escribe los pasos de preparación";
    public static final String MSG_SIN_INGREDIENTES = "Agrega al menos un insumo con cantidad mayor a cero";
    public static final String MSG_INGREDIENTE_REPETIDO = "Un insumo no puede repetirse en la receta";

    private static final int MAX_NOMBRE = 100;
    private static final int MAX_TIEMPO = 100000;
    private static final double MAX_CANTIDAD = 9999999.999;

    private RecetaDAO recetaDAO;
    private InsumoDAO insumoDAO;

    public RecetaService() {
        recetaDAO = DAOFactory.crearRecetaDAO();
        insumoDAO = DAOFactory.crearInsumoDAO();
    }

    // ===================== Consultas (Visualización RF-55) =====================

    // RF-55 (Visualización de recetas): inicia aquí. Paso 2: consulta las opciones que ya tienen receta.
    public List<Receta> listar(boolean incluirInactivas) throws DAOException {
        return recetaDAO.listar(incluirInactivas);
    }

    // Lee una receta con sus ingredientes; null si ya no existe
    public Receta buscarPorId(int idReceta) throws DAOException {
        return recetaDAO.buscarPorId(idReceta);
    }

    // Receta (activa o no) de una opción del catálogo; null si la opción no tiene
    public Receta buscarPorOpcion(int idOpcion) throws DAOException {
        return recetaDAO.buscarPorOpcion(idOpcion);
    }

    // Insumos activos que se pueden agregar a una receta
    public List<Insumo> listarInsumosActivos() throws DAOException {
        return insumoDAO.listar(false);
    }

    // RF-56 · Paso 2: opciones disponibles de características que permiten receta y que aún no tienen receta activa
    public List<OpcionReceta> listarOpcionesSinReceta() throws DAOException {
        return recetaDAO.listarOpcionesSinRecetaActiva();
    }

    // true si la receta coincide con el texto (nombre, opción, característica, categoría o insumo)
    // RF-55 · Paso 4: búsqueda. Excepción: si nada coincide, «No hay recetas que coincidan con la búsqueda».
    public boolean coincide(Receta receta, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String criterio = texto.trim().toLowerCase();
        String todo = receta.getNombre() + " " + receta.getDescripcionOpcion();
        return todo.toLowerCase().contains(criterio);
    }

    // Costo = suma de cantidad × costo unitario de cada insumo (igual que vw_CostoReceta)
    // RF-55 paso 6 y RF-56/RF-57 paso 7: costo estimado de la receta.
    public static double calcularCosto(List<IngredienteReceta> ingredientes) {
        double total = 0;
        for (IngredienteReceta ingrediente : ingredientes) {
            total = total + ingrediente.getCosto();
        }
        return Math.round(total * 100) / 100.0;
    }

    // ===================== Registro (RF-56) =====================

    // Paso 2: antes de abrir el formulario debe haber opciones sin receta
    // RF-56 (Registro de nueva receta): inicia aquí la regla de negocio.
    public ResultadoInventario revisarParaAlta() {
        if (!PermisosInventario.puedeEditarRecetas()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            if (recetaDAO.listarOpcionesSinRecetaActiva().isEmpty()) {
                // RF-56 · Excepción paso 2: «No hay opciones pendientes de receta».
                return ResultadoInventario.error(MSG_SIN_OPCIONES, null);
            }
            return ResultadoInventario.correcto("");
        } catch (DAOException e) {
            // RF-56 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // RF-56 · Pasos 5 a 8: valida y guarda la receta nueva.
    public ResultadoInventario registrar(Receta datos) {
        if (!PermisosInventario.puedeEditarRecetas()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        if (datos.getIdOpcion() <= 0) {
            return ResultadoInventario.error(MSG_ELIGE_OPCION, ResultadoInventario.CAMPO_OPCION);
        }
        ResultadoInventario validacion = validarCampos(datos);
        // RF-56 · Excepción paso 5: datos incompletos o insumos inválidos; se resalta el error.
        if (validacion != null) {
            return validacion;
        }

        Receta existente;
        try {
            // Paso 6: la opción sigue activa y no tiene receta activa
            OpcionReceta opcion = recetaDAO.buscarOpcion(datos.getIdOpcion());
            if (opcion == null || !opcion.isDisponible()) {
                // RF-56 · Excepción paso 6: «La opción ya no está disponible».
                return ResultadoInventario.error(MSG_OPCION_NO_DISPONIBLE, ResultadoInventario.CAMPO_OPCION);
            }
            if (!opcion.isPermiteReceta()) {
                // RF-56 · Excepción paso 6: la característica está marcada como «sin receta» en el catálogo.
                return ResultadoInventario.error(MSG_OPCION_SIN_RECETA, ResultadoInventario.CAMPO_OPCION);
            }
            existente = recetaDAO.buscarPorOpcion(datos.getIdOpcion());
            if (existente != null && existente.isActiva()) {
                // RF-56 · Excepción paso 6: «Esta opción ya tiene una receta registrada»; se ofrece consultarla.
                return ResultadoInventario.existente(MSG_OPCION_CON_RECETA, existente.getIdReceta(),
                        ResultadoInventario.CAMPO_OPCION);
            }
            ResultadoInventario insumos = revisarInsumosActivos(datos);
            // RF-56 · Excepción paso 6: «El insumo ya no está disponible»; se quita de la receta.
            if (insumos != null) {
                return insumos;
            }
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }

        // Paso 7: se guarda receta + ingredientes en una transacción.
        // Si la opción tenía una receta dada de baja, se reutiliza ese registro (solo puede haber una por opción).
        try {
            if (existente != null) {
                datos.setIdReceta(existente.getIdReceta());
                recetaDAO.actualizar(datos);
            } else {
                recetaDAO.insertar(datos);
            }
        } catch (DAOException e) {
            // RF-56 · Excepción paso 7: «No fue posible registrar la receta».
            return ResultadoInventario.error(MSG_NO_REGISTRADA, ResultadoInventario.CAMPO_CONEXION);
        }

        String mensaje = "La receta «" + datos.getNombre() + "» se registró. Costo estimado: "
                + Formatos.moneda(calcularCosto(datos.getIngredientes()))
                + ".\nEl catálogo ya calcula el precio de la opción con esta receta.";
        if (existente != null) {
            mensaje = mensaje + "\n(La opción tenía una receta dada de baja; se reemplazó con estos datos.)";
        }
        ResultadoInventario resultado = ResultadoInventario.correcto(mensaje);
        resultado.setIdRelacionado(datos.getIdReceta());
        return resultado;
    }

    // ===================== Modificación (RF-57) =====================

    // Paso 2: la receta existe y está activa
    // RF-57 (Modificación de receta): inicia aquí. Paso 2: la receta existe y está activa.
    public ResultadoInventario revisarParaModificar(int idReceta) {
        if (!PermisosInventario.puedeEditarRecetas()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            Receta receta = recetaDAO.buscarPorId(idReceta);
            if (receta == null || !receta.isActiva()) {
                // RF-57 · Excepción paso 2: «La receta ya no está disponible».
                return ResultadoInventario.noEncontrado(MSG_NO_DISPONIBLE);
            }
            return ResultadoInventario.correcto("");
        } catch (DAOException e) {
            // RF-57 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // RF-57 · Pasos 5 a 8: valida y actualiza la receta.
    public ResultadoInventario modificar(Receta datos) {
        if (!PermisosInventario.puedeEditarRecetas()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        ResultadoInventario validacion = validarCampos(datos);
        // RF-57 · Excepción paso 5: datos incompletos o insumos inválidos.
        if (validacion != null) {
            return validacion;
        }
        try {
            // Paso 6: la receta sigue existiendo y los insumos siguen activos
            Receta actual = recetaDAO.buscarPorId(datos.getIdReceta());
            if (actual == null || !actual.isActiva()) {
                // RF-57 · Excepción paso 6: «La receta ya no existe».
                return ResultadoInventario.noEncontrado(MSG_YA_NO_EXISTE);
            }
            datos.setIdOpcion(actual.getIdOpcion());
            ResultadoInventario insumos = revisarInsumosActivos(datos);
            // RF-57 · Excepción paso 6: «El insumo ya no está disponible»; se quita de la receta.
            if (insumos != null) {
                return insumos;
            }
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }

        try {
            recetaDAO.actualizar(datos);
        } catch (DAOException e) {
            // RF-57 · Excepción paso 7: «No fue posible actualizar la receta».
            return ResultadoInventario.error(MSG_NO_ACTUALIZADA, ResultadoInventario.CAMPO_CONEXION);
        }
        // RF-57 · Paso 8: informa el éxito y el nuevo costo estimado.
        return ResultadoInventario.correcto("La receta «" + datos.getNombre() + "» se actualizó. Costo estimado: "
                + Formatos.moneda(calcularCosto(datos.getIngredientes()))
                + ".\nSe aplicará a los pedidos que entren a preparación; los que ya están en preparación"
                + " conservan su descuento.");
    }

    // ===================== Baja (RF-58) =====================

    // Pasos 2 a 4: revisa si se puede dar de baja y arma el mensaje de confirmación
    // RF-58 (Eliminación de receta): inicia aquí.
    public ResultadoInventario prepararBaja(int idReceta) {
        if (!PermisosInventario.puedeEditarRecetas()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            Receta receta = recetaDAO.buscarPorId(idReceta);
            if (receta == null || !receta.isActiva()) {
                // RF-58 · Excepción paso 2: «La receta ya no está disponible».
                return ResultadoInventario.noEncontrado(MSG_NO_DISPONIBLE);
            }

            // Paso 3: pedidos pendientes de preparar con esa opción
            List<Integer> folios = recetaDAO.listarFoliosPorPreparar(receta.getIdOpcion());
            if (!folios.isEmpty()) {
                // RF-58 · Excepción paso 3: «La receta se necesita para pedidos en curso» con los folios.
                return ResultadoInventario.error(MSG_EN_PEDIDOS + ": " + ValidacionCatalogo.unirFolios(folios), null);
            }

            String mensaje = "Receta: " + receta.getNombre() + "\nOpción: " + receta.getDescripcionOpcion()
                    + "\n\nUna receta dada de baja ya no descuenta insumos del inventario.";
            // RF-58 · Excepción paso 4: la opción sigue a la venta; se avisa que no descontará insumos.
            if (receta.isOpcionDisponible()) {
                mensaje = mensaje + "\n\nAVISO: la opción sigue disponible para los clientes. Sus próximos pedidos"
                        + " no descontarán insumos y en el catálogo su precio aparecerá como «Sin receta».";
            }

            boolean enPedidos = recetaDAO.opcionEnPedidos(receta.getIdOpcion());
            if (enPedidos) {
                mensaje = mensaje + "\n\nLa opción ya se usó en pedidos, así que la receta se INHABILITARÁ"
                        + " y conservará su historial.";
            } else {
                mensaje = mensaje + "\n\nLa opción no se ha usado en pedidos, así que la receta se ELIMINARÁ."
                        + " Esta acción no se puede deshacer.";
            }
            // RF-58 · Paso 4: mensaje de confirmación.
            return ResultadoInventario.bajaPosible(mensaje, !enPedidos);
        } catch (DAOException e) {
            // RF-58 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // Paso 6: registra la baja
    public ResultadoInventario darDeBaja(int idReceta) {
        ResultadoInventario revision = prepararBaja(idReceta);
        if (!revision.isExitoso()) {
            return revision;
        }
        try {
            // RF-58 · Paso 6: inhabilita la receta (o la elimina si nunca se usó).
            if (revision.isSeEliminara()) {
                recetaDAO.eliminar(idReceta);
                return ResultadoInventario.correcto("La receta se eliminó correctamente");
            }
            recetaDAO.inhabilitar(idReceta);
            return ResultadoInventario.correcto("La receta se dio de baja (inhabilitada) correctamente");
        } catch (DAOException e) {
            // RF-58 · Excepción paso 6: «No fue posible dar de baja la receta».
            return ResultadoInventario.error(MSG_NO_DADA_DE_BAJA, null);
        }
    }

    // ===================== Apoyo =====================

    // Revisa los campos del formulario; devuelve null si todo está bien
    private ResultadoInventario validarCampos(Receta datos) {
        if (ValidacionCatalogo.estaVacio(datos.getNombre()) || datos.getNombre().trim().length() > MAX_NOMBRE) {
            return ResultadoInventario.error(MSG_NOMBRE_INVALIDO, ResultadoInventario.CAMPO_NOMBRE);
        }
        if (datos.getTiempoMinutos() <= 0 || datos.getTiempoMinutos() > MAX_TIEMPO) {
            return ResultadoInventario.error(MSG_TIEMPO_INVALIDO, ResultadoInventario.CAMPO_TIEMPO);
        }
        if (ValidacionCatalogo.estaVacio(datos.getPasos())) {
            return ResultadoInventario.error(MSG_PASOS_VACIOS, ResultadoInventario.CAMPO_PASOS);
        }
        List<IngredienteReceta> ingredientes = datos.getIngredientes();
        // RF-56 / RF-57 · Excepción paso 5: debe haber al menos un insumo con cantidad mayor a cero.
        if (ingredientes.isEmpty()) {
            return ResultadoInventario.error(MSG_SIN_INGREDIENTES, ResultadoInventario.CAMPO_INGREDIENTES);
        }
        for (int i = 0; i < ingredientes.size(); i++) {
            IngredienteReceta ingrediente = ingredientes.get(i);
            if (ingrediente.getCantidad() <= 0 || ingrediente.getCantidad() > MAX_CANTIDAD) {
                return ResultadoInventario.error(MSG_SIN_INGREDIENTES + " (revisa «" + ingrediente.getNombreInsumo() + "»)",
                        ResultadoInventario.CAMPO_INGREDIENTES);
            }
            for (int j = i + 1; j < ingredientes.size(); j++) {
                // RF-56 / RF-57 · Excepción paso 5: insumo repetido.
                if (ingredientes.get(j).getIdInsumo() == ingrediente.getIdInsumo()) {
                    return ResultadoInventario.error(MSG_INGREDIENTE_REPETIDO + ": " + ingrediente.getNombreInsumo(),
                            ResultadoInventario.CAMPO_INGREDIENTES);
                }
            }
            ingrediente.setCantidad(Math.round(ingrediente.getCantidad() * 1000) / 1000.0);
        }
        datos.setNombre(datos.getNombre().trim());
        datos.setPasos(datos.getPasos().trim());
        return null;
    }

    // RF-56 / RF-57 paso 6: cada insumo de la receta debe seguir activo.
    // Si alguno no, se devuelve el error con su id para que el formulario lo quite.
    private ResultadoInventario revisarInsumosActivos(Receta datos) throws DAOException {
        for (IngredienteReceta ingrediente : datos.getIngredientes()) {
            Insumo insumo = insumoDAO.buscarPorId(ingrediente.getIdInsumo());
            if (insumo == null || !insumo.isActivo()) {
                ResultadoInventario error = ResultadoInventario.error(MSG_INSUMO_NO_DISPONIBLE + ": "
                        + ingrediente.getNombreInsumo() + ". Se quitó de la receta.", ResultadoInventario.CAMPO_INGREDIENTES);
                error.setIdRelacionado(ingrediente.getIdInsumo());
                return error;
            }
        }
        return null;
    }
}

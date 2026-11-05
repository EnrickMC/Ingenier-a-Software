package com.pac.modulos.catalogo.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.dao.CategoriaDAO;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;

import java.util.List;

/**
 * Reglas de las categorías (RF-15 a RF-18).
 * Los mensajes son los de los casos de uso del archivo "Requerimientos del sistema CARMIR.json".
 */
public class CategoriaService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_ERROR_CONEXION_LISTA = "Error de conexión, intente de nuevo";
    public static final String MSG_NO_ENCONTRADA = "Categoría no encontrada";
    public static final String MSG_SIN_CATEGORIAS = "No hay categorías registradas";
    public static final String MSG_SIN_RESULTADOS = "Sin resultados para el criterio ingresado";
    public static final String MSG_YA_EXISTE = "La categoría ya existe";
    public static final String MSG_OTRA_CON_NOMBRE = "Ya existe otra categoría con ese nombre";
    public static final String MSG_NO_GUARDADA = "No fue posible guardar la categoría";
    public static final String MSG_NO_ACTUALIZADA = "No fue posible actualizar la categoría";
    public static final String MSG_NO_DADA_DE_BAJA = "No fue posible dar de baja la categoría";
    public static final String MSG_NOMBRE_INVALIDO =
            "Escribe el nombre de la categoría (máximo 50 caracteres, solo letras, números y espacios)";
    public static final String MSG_DESCRIPCION_LARGA = "La descripción no puede pasar de 255 caracteres";

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_DESCRIPCION = 255;

    private CategoriaDAO categoriaDAO;

    public CategoriaService() {
        categoriaDAO = DAOFactory.crearCategoriaDAO();
    }

    // ===================== Consultas (RF-15) =====================

    // Busca categorías por nombre o descripción; estado null = todas
    // RF-15 (Visualización de categorías): inicia aquí la consulta. Pasos 3 y 6: consulta las categorías (con búsqueda y filtro de estado).
    public List<Categoria> buscar(String texto, EstadoRegistro estado) throws DAOException {
        return categoriaDAO.buscar(texto, estado);
    }

    // Solo las categorías activas (para los formularios de productos y características)
    public List<Categoria> listarActivas() throws DAOException {
        return categoriaDAO.buscar("", EstadoRegistro.ACTIVA);
    }

    // Todas las categorías (para los selectores del personal, que también ve las inactivas)
    public List<Categoria> listarTodas() throws DAOException {
        return categoriaDAO.buscar("", null);
    }

    // Lee una categoría; devuelve null si ya no existe
    public Categoria buscarPorId(int idCategoria) throws DAOException {
        return categoriaDAO.buscarPorId(idCategoria);
    }

    // Una categoría tiene datos completos si tiene nombre
    // RF-15 · Paso 4: valida que los datos de la categoría estén completos.
    public boolean datosCompletos(Categoria categoria) {
        return !ValidacionCatalogo.estaVacio(categoria.getNombre());
    }

    // ===================== Registro (RF-16) =====================

    // RF-16 (Registro de categorías): inicia aquí la regla de negocio.
    public ResultadoCatalogo registrar(Categoria datos) {
        // Excepción (RNF-12): solo la Administradora edita el catálogo; el personal solo consulta.
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        // Paso 4: campos obligatorios y formato
        ResultadoCatalogo validacion = validarCampos(datos);
        // RF-16 · Excepción paso 4: campos incompletos o con formato inválido; se resalta el campo.
        if (validacion != null) {
            return validacion;
        }

        try {
            // Paso 5: nombre repetido
            // RF-16 · Excepción paso 5: «La categoría ya existe».
            if (categoriaDAO.buscarPorNombre(datos.getNombre()) != null) {
                return ResultadoCatalogo.error(MSG_YA_EXISTE, ResultadoCatalogo.CAMPO_NOMBRE);
            }
        } catch (DAOException e) {
            // RF-16 · Excepción paso 5: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 6: se guarda
        try {
            categoriaDAO.insertar(datos);
        } catch (DAOException e) {
            // RF-16 · Excepción paso 6: «No fue posible guardar la categoría».
            return ResultadoCatalogo.error(MSG_NO_GUARDADA, ResultadoCatalogo.CAMPO_CONEXION);
        }
        // RF-16 · Paso 6: informa el éxito.
        return ResultadoCatalogo.correcto("La categoría «" + datos.getNombre() + "» se registró correctamente");
    }

    // ===================== Modificación (RF-17) =====================

    // confirmado = la administradora ya aceptó inactivar la categoría
    // RF-17 (Modificación de categorías): inicia aquí la regla de negocio.
    public ResultadoCatalogo modificar(Categoria datos, boolean confirmado) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        ResultadoCatalogo validacion = validarCampos(datos);
        // RF-17 · Excepción paso 5: campos incompletos o con formato inválido.
        if (validacion != null) {
            return validacion;
        }

        Categoria actual;
        List<String> productosActivos;
        try {
            actual = categoriaDAO.buscarPorId(datos.getIdCategoria());
            // RF-17 · Excepción paso 2: «Categoría no encontrada».
            if (actual == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }

            // Paso 6: el nombre no debe ser de otra categoría
            Categoria conMismoNombre = categoriaDAO.buscarPorNombre(datos.getNombre());
            // RF-17 · Excepción paso 6: «Ya existe otra categoría con ese nombre».
            if (conMismoNombre != null && conMismoNombre.getIdCategoria() != datos.getIdCategoria()) {
                return ResultadoCatalogo.error(MSG_OTRA_CON_NOMBRE, ResultadoCatalogo.CAMPO_NOMBRE);
            }

            productosActivos = categoriaDAO.listarProductosActivos(datos.getIdCategoria());
        } catch (DAOException e) {
            // RF-17 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 7: si pasa a Inactiva, se pide confirmar y se dicen los productos afectados
        boolean seInactiva = actual.estaActiva() && !datos.estaActiva();
        // RF-17 · Paso 7: se pide confirmar la inactivación (si no confirma, el formulario vuelve a Activa).
        if (seInactiva && !confirmado) {
            String mensaje = "Al inactivar la categoría, sus productos dejarán de verse en la tienda web.";
            if (productosActivos.isEmpty()) {
                mensaje = mensaje + "\nLa categoría no tiene productos activos.";
            } else {
                mensaje = mensaje + "\nProductos activos: " + ValidacionCatalogo.unir(productosActivos);
            }
            mensaje = mensaje + "\n\n¿Confirmas la inactivación?";
            return ResultadoCatalogo.confirmacion(mensaje, ResultadoCatalogo.CAMPO_ESTADO);
        }

        // Paso 8: se actualiza
        try {
            categoriaDAO.actualizar(datos);
        } catch (DAOException e) {
            // RF-17 · Excepción paso 8: «No fue posible actualizar la categoría».
            return ResultadoCatalogo.error(MSG_NO_ACTUALIZADA, ResultadoCatalogo.CAMPO_CONEXION);
        }
        // RF-17 · Paso 8: informa el éxito.
        return ResultadoCatalogo.correcto("La categoría «" + datos.getNombre() + "» se actualizó correctamente");
    }

    // ===================== Baja (RF-18) =====================

    // Pasos 2 y 3: revisa si se puede dar de baja y arma el mensaje de confirmación
    // RF-18 (Eliminación de categorías): inicia aquí la regla de negocio.
    public ResultadoCatalogo prepararBaja(int idCategoria) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            Categoria categoria = categoriaDAO.buscarPorId(idCategoria);
            // RF-18 · Excepción paso 2: «Categoría no encontrada».
            if (categoria == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }

            // Con productos activos no se permite
            List<String> activos = categoriaDAO.listarProductosActivos(idCategoria);
            // RF-18 · Excepción paso 3: tiene productos activos; se bloquea la baja y se listan.
            if (!activos.isEmpty()) {
                return ResultadoCatalogo.error("No se puede dar de baja la categoría «" + categoria.getNombre()
                        + "» porque tiene productos activos:\n" + ValidacionCatalogo.unir(activos)
                        + "\n\nOculta o da de baja esos productos primero.", null);
            }

            boolean historial = categoriaDAO.tieneHistorial(idCategoria);
            String mensaje = "Categoría: " + categoria.getNombre() + "\n\n";
            if (historial) {
                mensaje = mensaje + "La categoría tiene historial (postres o recetas), así que se INHABILITARÁ:"
                        + " quedará Inactiva y sus características y opciones dejarán de estar disponibles."
                        + " Se retirará del catálogo público.";
            } else {
                mensaje = mensaje + "La categoría no tiene relaciones, así que se ELIMINARÁ junto con sus"
                        + " características, opciones y exclusiones. Esta acción no se puede deshacer.";
            }
            // RF-18 · Paso 4: mensaje de la ventana de confirmación con las consecuencias.
            return ResultadoCatalogo.bajaPosible(mensaje, !historial);
        } catch (DAOException e) {
            // RF-18 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }
    }

    // Paso 6: registra la baja (inhabilita o elimina)
    // RF-18 · Paso 6 (después de que la Administradora confirmó en el paso 5).
    public ResultadoCatalogo darDeBaja(int idCategoria) {
        ResultadoCatalogo revision = prepararBaja(idCategoria);
        if (!revision.isExitoso()) {
            return revision;
        }
        try {
            // RF-18 · Paso 6: sin historial se elimina; con historial se inhabilita (con sus características y opciones).
            if (revision.isSeEliminara()) {
                categoriaDAO.eliminar(idCategoria);
                return ResultadoCatalogo.correcto("La categoría se eliminó correctamente");
            }
            categoriaDAO.inhabilitar(idCategoria);
            return ResultadoCatalogo.correcto("La categoría se inhabilitó correctamente");
        } catch (DAOException e) {
            // RF-18 · Excepción paso 6: «No fue posible dar de baja la categoría».
            return ResultadoCatalogo.error(MSG_NO_DADA_DE_BAJA, null);
        }
    }

    // ===================== Apoyo =====================

    // Revisa los campos del formulario; devuelve null si todo está bien
    // RF-16 · Paso 4 / RF-17 · Paso 5: valida campos obligatorios y formato.
    private ResultadoCatalogo validarCampos(Categoria datos) {
        if (!ValidacionCatalogo.nombreValido(datos.getNombre(), MAX_NOMBRE)) {
            return ResultadoCatalogo.error(MSG_NOMBRE_INVALIDO, ResultadoCatalogo.CAMPO_NOMBRE);
        }
        if (!ValidacionCatalogo.largoValido(datos.getDescripcion(), MAX_DESCRIPCION)) {
            return ResultadoCatalogo.error(MSG_DESCRIPCION_LARGA, ResultadoCatalogo.CAMPO_DESCRIPCION);
        }
        if (datos.getEstado() == null) {
            return ResultadoCatalogo.error("Elige el estado de la categoría", ResultadoCatalogo.CAMPO_ESTADO);
        }
        datos.setNombre(datos.getNombre().trim());
        return null;
    }
}

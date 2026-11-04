package com.pac.modulos.inventario.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.dao.InsumoDAO;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reglas de los insumos: Visualización (RF-51), Registro (RF-52),
 * Modificación (RF-53) y Eliminación (RF-54).
 */
public class InsumoService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_SIN_INSUMOS = "No hay insumos registrados";
    public static final String MSG_SIN_RESULTADOS = "No se encontraron insumos con ese criterio";
    public static final String MSG_NO_ENCONTRADO = "Insumo no encontrado";
    public static final String MSG_YA_REGISTRADO = "El insumo ya está registrado";
    public static final String MSG_OTRO_CON_NOMBRE = "Ya existe otro insumo con ese nombre";
    public static final String MSG_YA_DADO_DE_BAJA = "El insumo ya fue dado de baja";
    public static final String MSG_NO_REGISTRADO = "No fue posible registrar el insumo";
    public static final String MSG_NO_ACTUALIZADO = "No fue posible actualizar el insumo";
    public static final String MSG_NO_DADO_DE_BAJA = "No fue posible dar de baja el insumo";
    public static final String MSG_NOMBRE_INVALIDO =
            "Escribe el nombre del insumo (máximo 80 caracteres, solo letras, números y espacios)";
    public static final String MSG_CATEGORIA_INVALIDA = "Escribe la categoría del insumo (máximo 50 caracteres)";
    public static final String MSG_UNIDAD_INVALIDA = "Elige la unidad de medida";
    public static final String MSG_COSTO_INVALIDO = "El costo por unidad debe ser mayor a cero";
    public static final String MSG_STOCK_INVALIDO = "El stock mínimo no puede ser negativo";
    public static final String MSG_CAMBIO_UNIDAD = "No se puede cambiar la unidad de medida: el insumo ya tiene"
            + " existencias registradas o está en recetas, y sus cantidades quedarían mal. Se restableció la unidad original.";

    // ----- Estados que se muestran en la tabla -----
    public static final String ESTADO_NORMAL = "Normal";
    public static final String ESTADO_STOCK_BAJO = "Stock bajo";
    public static final String ESTADO_AGOTADO = "Agotado";
    public static final String ESTADO_SIN_MINIMO = "Sin stock mínimo";
    public static final String ESTADO_PROXIMO_CADUCAR = "Próximo a caducar";
    public static final String ESTADO_CADUCADO = "Caducado";
    public static final String ESTADO_REVISAR = "Revisar datos";
    public static final String ESTADO_INACTIVO = "Dado de baja";

    private static final int MAX_NOMBRE = 80;
    private static final int MAX_CATEGORIA = 50;
    private static final double MAX_COSTO = 99999999.99;
    private static final double MAX_CANTIDAD = 9999999.999;

    private InsumoDAO insumoDAO;

    public InsumoService() {
        insumoDAO = DAOFactory.crearInsumoDAO();
    }

    // ===================== Consultas (Visualización RF-51) =====================

    // RF-51 (Visualización de datos de insumos): inicia aquí. Paso 3: consulta los insumos activos (o todos).
    public List<Insumo> listar(boolean incluirInactivos) throws DAOException {
        return insumoDAO.listar(incluirInactivos);
    }

    // Lee un insumo; devuelve null si ya no existe
    public Insumo buscarPorId(int idInsumo) throws DAOException {
        return insumoDAO.buscarPorId(idInsumo);
    }

    public List<String> listarCategorias() throws DAOException {
        return insumoDAO.listarCategorias();
    }

    // true si el insumo coincide con el texto (nombre, clave o categoría) y con la categoría elegida
    // RF-51 · Paso 1: búsqueda por nombre, clave o categoría. Excepción paso 4: si nada coincide, «No se encontraron insumos con ese criterio».
    public boolean coincide(Insumo insumo, String texto, String categoria) {
        if (categoria != null && !categoria.isBlank() && !categoria.equalsIgnoreCase(insumo.getCategoria())) {
            return false;
        }
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String criterio = texto.trim().toLowerCase();
        if (insumo.getNombre().toLowerCase().contains(criterio)) {
            return true;
        }
        if (insumo.getClave().toLowerCase().contains(criterio)) {
            return true;
        }
        return insumo.getCategoria().toLowerCase().contains(criterio);
    }

    // RF-51 paso 4: un insumo con costo en cero o sin categoría está incompleto
    // RF-51 · Excepción paso 4: datos incompletos; el insumo se marca «Revisar datos» y se muestran los demás.
    public boolean datosCompletos(Insumo insumo) {
        if (ValidacionCatalogo.estaVacio(insumo.getCategoria())) {
            return false;
        }
        return insumo.getCostoUnitario() > 0;
    }

    // Días que le faltan al lote que caduca antes (negativo = ya caducó). Integer.MAX_VALUE si no caduca
    public int diasParaCaducar(Insumo insumo) {
        LocalDate caducidad = insumo.getCaducidadMasProxima();
        if (caducidad == null) {
            return Integer.MAX_VALUE;
        }
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), caducidad);
    }

    // Estado que se muestra en la tabla (RF-51, RF-61, RF-62), del más grave al menos grave
    // RF-51 · Paso 5: compara existencia con stock mínimo y revisa caducidad de los lotes.
    public String estadoDe(Insumo insumo) {
        if (!insumo.isActivo()) {
            return ESTADO_INACTIVO;
        }
        if (!datosCompletos(insumo)) {
            return ESTADO_REVISAR;
        }
        int dias = diasParaCaducar(insumo);
        if (dias < 0) {
            return ESTADO_CADUCADO;
        }
        if (insumo.tieneStockMinimo() && insumo.getExistencia() <= 0.0005) {
            return ESTADO_AGOTADO;
        }
        if (insumo.estaBajoMinimo()) {
            return ESTADO_STOCK_BAJO;
        }
        if (dias <= ConfiguracionApp.getInstancia().getDiasAvisoCaducidad()) {
            return ESTADO_PROXIMO_CADUCAR;
        }
        // RF-51 · Excepción paso 5: sin stock mínimo configurado, se muestra «Sin stock mínimo» sin generar alerta.
        if (!insumo.tieneStockMinimo()) {
            return ESTADO_SIN_MINIMO;
        }
        return ESTADO_NORMAL;
    }

    // ===================== Registro (RF-52) =====================

    // RF-52 (Registro de datos de insumos): inicia aquí la regla de negocio.
    public ResultadoInventario registrar(Insumo datos) {
        if (!PermisosInventario.puedeAdministrarInsumos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        ResultadoInventario validacion = validarCampos(datos);
        // RF-52 · Paso 4 / Excepción: campos incompletos o inválidos; se resalta el campo y no se guarda.
        if (validacion != null) {
            return validacion;
        }

        // Paso 5: nombre repetido (activo o dado de baja)
        try {
            Insumo existente = insumoDAO.buscarPorNombre(datos.getNombre());
            // RF-52 · Excepción paso 5: «El insumo ya está registrado».
            if (existente != null && existente.isActivo()) {
                return ResultadoInventario.error(MSG_YA_REGISTRADO, ResultadoInventario.CAMPO_NOMBRE);
            }
            // RF-52 · Excepción paso 5: el nombre es de un insumo dado de baja; se ofrece reactivarlo.
            if (existente != null) {
                return ResultadoInventario.reactivable("El insumo «" + existente.getNombre() + "» ya existe, pero"
                        + " fue dado de baja.\n\n¿Deseas reactivarlo en lugar de crear uno nuevo?"
                        + " Conservará sus datos e historial; después podrás modificarlo.", existente.getIdInsumo());
            }
        } catch (DAOException e) {
            // RF-52 · Excepción paso 5: sin conexión, «Error de conexión»; el formulario conserva los datos.
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }

        // Paso 6: se guarda con existencia en cero (no tiene lotes todavía)
        try {
            insumoDAO.insertar(datos);
        } catch (DAOException e) {
            // RF-52 · Excepción paso 6: «No fue posible registrar el insumo».
            return ResultadoInventario.error(MSG_NO_REGISTRADO, ResultadoInventario.CAMPO_CONEXION);
        }
        ResultadoInventario resultado = ResultadoInventario.correcto("El insumo «" + datos.getNombre()
                + "» se registró con la clave " + datos.getClave() + " y existencia en cero."
                + " Registra una compra para agregar existencias.");
        resultado.setIdRelacionado(datos.getIdInsumo());
        return resultado;
    }

    // RF-52 excepción: reactivar el insumo dado de baja en lugar de crear uno nuevo
    public ResultadoInventario reactivar(int idInsumo) {
        if (!PermisosInventario.puedeAdministrarInsumos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            Insumo insumo = insumoDAO.buscarPorId(idInsumo);
            if (insumo == null) {
                return ResultadoInventario.noEncontrado(MSG_NO_ENCONTRADO);
            }
            insumoDAO.reactivar(idInsumo);
            MonitorAlertas.getInstancia().revisar();
            ResultadoInventario resultado = ResultadoInventario.correcto("El insumo «" + insumo.getNombre()
                    + "» se reactivó. Revisa sus datos con «Modificar» si cambiaron.");
            resultado.setIdRelacionado(idInsumo);
            return resultado;
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // ===================== Modificación (RF-53) =====================

    // Paso 2: antes de abrir el formulario, el insumo debe existir y estar activo
    // RF-53 (Modificación de datos de insumos): inicia aquí. Paso 2: el insumo debe existir y estar activo.
    public ResultadoInventario revisarParaModificar(int idInsumo) {
        try {
            Insumo insumo = insumoDAO.buscarPorId(idInsumo);
            if (insumo == null || !insumo.isActivo()) {
                // RF-53 · Excepción paso 2: «Insumo no encontrado».
                return ResultadoInventario.noEncontrado(MSG_NO_ENCONTRADO);
            }
            return ResultadoInventario.correcto("");
        } catch (DAOException e) {
            // RF-53 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // RF-53 · Pasos 5 a 8: valida y guarda los cambios del insumo.
    public ResultadoInventario modificar(Insumo datos) {
        if (!PermisosInventario.puedeAdministrarInsumos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        ResultadoInventario validacion = validarCampos(datos);
        // RF-53 · Excepción paso 5: campos incompletos o inválidos; se resalta el campo.
        if (validacion != null) {
            return validacion;
        }

        Insumo actual;
        try {
            actual = insumoDAO.buscarPorId(datos.getIdInsumo());
            if (actual == null || !actual.isActivo()) {
                return ResultadoInventario.noEncontrado(MSG_NO_ENCONTRADO);
            }
            // Paso 6: nombre de otro insumo
            Insumo conMismoNombre = insumoDAO.buscarPorNombre(datos.getNombre());
            if (conMismoNombre != null && conMismoNombre.getIdInsumo() != datos.getIdInsumo()) {
                // RF-53 · Excepción paso 6: «Ya existe otro insumo con ese nombre».
                return ResultadoInventario.error(MSG_OTRO_CON_NOMBRE, ResultadoInventario.CAMPO_NOMBRE);
            }
            // Paso 6: cambio de unidad con existencias o recetas
            if (actual.getUnidad() != datos.getUnidad() && insumoDAO.tieneLotesORecetas(datos.getIdInsumo())) {
                // RF-53 · Excepción paso 6: no se permite cambiar la unidad si tiene existencias o recetas.
                ResultadoInventario error = ResultadoInventario.error(MSG_CAMBIO_UNIDAD, ResultadoInventario.CAMPO_UNIDAD);
                error.setIdRelacionado(actual.getIdInsumo());
                return error;
            }
        } catch (DAOException e) {
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }

        try {
            insumoDAO.actualizar(datos);
        } catch (DAOException e) {
            // RF-53 · Excepción paso 7: «No fue posible actualizar el insumo».
            return ResultadoInventario.error(MSG_NO_ACTUALIZADO, ResultadoInventario.CAMPO_CONEXION);
        }

        // Paso 7: si cambió el stock mínimo se recalcula su alerta
        if (Math.abs(actual.getStockMinimo() - datos.getStockMinimo()) > 0.0005) {
            MonitorAlertas.getInstancia().revisar();
        }
        String mensaje = "El insumo «" + datos.getNombre() + "» se actualizó correctamente";
        if (Math.abs(actual.getCostoUnitario() - datos.getCostoUnitario()) > 0.0005) {
            mensaje = mensaje + ".\nEl costo de las recetas que lo usan y el precio de sus opciones se recalcularon.";
        }
        // RF-53 · Paso 8: informa el éxito.
        return ResultadoInventario.correcto(mensaje);
    }

    // Unidad original del insumo (para restablecerla en el formulario)
    public Insumo leerOriginal(int idInsumo) throws DAOException {
        return insumoDAO.buscarPorId(idInsumo);
    }

    // ===================== Baja (RF-54) =====================

    // Pasos 2 a 4: revisa si se puede dar de baja y arma el mensaje de confirmación
    // RF-54 (Eliminación de insumo del inventario): inicia aquí.
    public ResultadoInventario prepararBaja(int idInsumo) {
        if (!PermisosInventario.puedeAdministrarInsumos()) {
            return ResultadoInventario.error(PermisosInventario.MSG_SOLO_CONSULTA, null);
        }
        try {
            Insumo insumo = insumoDAO.buscarPorId(idInsumo);
            if (insumo == null) {
                // RF-54 · Excepción paso 2: «Insumo no encontrado».
                return ResultadoInventario.noEncontrado(MSG_NO_ENCONTRADO);
            }
            if (!insumo.isActivo()) {
                // RF-54 · Excepción paso 2: «El insumo ya fue dado de baja».
                return ResultadoInventario.noEncontrado(MSG_YA_DADO_DE_BAJA);
            }

            String mensaje = "Insumo: " + insumo.getClave() + " · " + insumo.getNombre()
                    + "\nExistencia: " + Formatos.cantidad(insumo.getExistencia()) + " " + insumo.getUnidad().getValorBD();

            // Paso 3: recetas activas que lo usan
            List<String> recetas = insumoDAO.listarRecetasActivas(idInsumo);
            // RF-54 · Excepción paso 3: se agregan a la confirmación las recetas que lo usan (quedarán «requiere revisión»).
            if (!recetas.isEmpty()) {
                mensaje = mensaje + "\n\nSe usa en estas recetas activas:\n" + ValidacionCatalogo.unirLineas(recetas)
                        + "\nLas recetas no se modifican, pero quedarán marcadas «Esta receta requiere revisión».";
            }

            boolean historial = insumoDAO.tieneHistorial(idInsumo);
            if (historial) {
                mensaje = mensaje + "\n\nTiene historial (compras, movimientos o recetas), así que se INHABILITARÁ:"
                        + " ya no aparecerá en compras, mermas ni en nuevas recetas, y los reportes anteriores se conservan.";
            } else {
                mensaje = mensaje + "\n\nNo tiene movimientos, así que se ELIMINARÁ. Esta acción no se puede deshacer.";
            }
            // RF-54 · Paso 4: mensaje de confirmación (inhabilitar o eliminar).
            return ResultadoInventario.bajaPosible(mensaje, !historial);
        } catch (DAOException e) {
            // RF-54 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoInventario.error(MSG_ERROR_CONEXION, ResultadoInventario.CAMPO_CONEXION);
        }
    }

    // Paso 6: registra la baja
    public ResultadoInventario darDeBaja(int idInsumo) {
        ResultadoInventario revision = prepararBaja(idInsumo);
        if (!revision.isExitoso()) {
            return revision;
        }
        try {
            // RF-54 · Paso 6: sin movimientos se elimina; con historial se inhabilita.
            if (revision.isSeEliminara()) {
                insumoDAO.eliminar(idInsumo);
                return ResultadoInventario.correcto("El insumo se eliminó correctamente");
            }
            insumoDAO.inhabilitar(idInsumo);
        } catch (DAOException e) {
            // RF-54 · Excepción paso 6: «No fue posible dar de baja el insumo».
            return ResultadoInventario.error(MSG_NO_DADO_DE_BAJA, null);
        }
        MonitorAlertas.getInstancia().revisar();
        // RF-54 · Paso 7: informa el éxito.
        return ResultadoInventario.correcto("El insumo se dio de baja (inhabilitado) correctamente");
    }

    // ===================== Apoyo =====================

    // Revisa los campos del formulario; devuelve null si todo está bien
    private ResultadoInventario validarCampos(Insumo datos) {
        if (!ValidacionCatalogo.nombreValido(datos.getNombre(), MAX_NOMBRE)) {
            return ResultadoInventario.error(MSG_NOMBRE_INVALIDO, ResultadoInventario.CAMPO_NOMBRE);
        }
        if (!ValidacionCatalogo.nombreValido(datos.getCategoria(), MAX_CATEGORIA)) {
            return ResultadoInventario.error(MSG_CATEGORIA_INVALIDA, ResultadoInventario.CAMPO_CATEGORIA);
        }
        if (datos.getUnidad() == null) {
            return ResultadoInventario.error(MSG_UNIDAD_INVALIDA, ResultadoInventario.CAMPO_UNIDAD);
        }
        if (datos.getCostoUnitario() <= 0 || datos.getCostoUnitario() > MAX_COSTO) {
            return ResultadoInventario.error(MSG_COSTO_INVALIDO, ResultadoInventario.CAMPO_COSTO);
        }
        if (datos.getStockMinimo() < 0 || datos.getStockMinimo() > MAX_CANTIDAD) {
            return ResultadoInventario.error(MSG_STOCK_INVALIDO, ResultadoInventario.CAMPO_STOCK);
        }
        datos.setNombre(datos.getNombre().trim());
        datos.setCategoria(datos.getCategoria().trim());
        return null;
    }
}

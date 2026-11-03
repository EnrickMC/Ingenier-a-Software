package com.pac.modulos.catalogo.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.dao.CaracteristicaDAO;
import com.pac.modulos.catalogo.dao.CategoriaDAO;
import com.pac.modulos.catalogo.dao.ExclusionDAO;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.dao.ProductoDAO;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.model.Producto;

import com.pac.util.SelectorImagen;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de los productos del catálogo (RF-17 a RF-20).
 *
 * Un producto es un Postre con nombre comercial: una combinación única de opciones
 * (ClaveConfiguracion). Su precio se calcula con las recetas de esas opciones.
 */
public class ProductoService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_ENCONTRADO = "Producto no encontrado";
    public static final String MSG_YA_NO_EXISTE = "El producto ya no existe";
    public static final String MSG_SIN_PRODUCTOS = "No hay productos registrados con el criterio seleccionado";
    public static final String MSG_BUSQUEDA_VACIA = "No se encontraron productos con ese criterio";
    public static final String MSG_CATEGORIA_NO_ENCONTRADA = "Categoría no encontrada";
    public static final String MSG_SIN_CATEGORIAS = "Debe registrar al menos una categoría activa antes de registrar productos";
    public static final String MSG_CATEGORIA_NO_DISPONIBLE = "La categoría seleccionada ya no está disponible";
    public static final String MSG_YA_REGISTRADO = "El producto ya está registrado";
    public static final String MSG_IMAGEN_NO_PERMITIDA = "Formato o tamaño de imagen no permitido";
    public static final String MSG_NO_REGISTRADO = "No fue posible registrar el producto";
    public static final String MSG_NO_ACTUALIZADO = "No fue posible actualizar el producto";
    public static final String MSG_NO_DADO_DE_BAJA = "No fue posible dar de baja el producto";
    public static final String MSG_EN_PEDIDOS = "El producto tiene pedidos en curso";
    public static final String MSG_NOMBRE_INVALIDO =
            "Escribe el nombre del producto (máximo 100 caracteres, solo letras, números y espacios)";
    public static final String MSG_FOTO_OBLIGATORIA = "La fotografía es obligatoria para mostrar el producto en la tienda";
    public static final String MSG_OPCIONES_CON_PEDIDOS = "Este producto ya aparece en pedidos, así que sus opciones no"
            + " se pueden cambiar (los pedidos conservan lo que pidió el cliente). Regístralo como un producto nuevo.";

    private static final int MAX_NOMBRE = 100;
    private static final int MAX_DESCRIPCION = 2000;
    private static final int MAX_ENLACE = 255;

    private ProductoDAO productoDAO;
    private CategoriaDAO categoriaDAO;
    private CaracteristicaDAO caracteristicaDAO;
    private OpcionDAO opcionDAO;
    private ExclusionDAO exclusionDAO;

    public ProductoService() {
        productoDAO = DAOFactory.crearProductoDAO();
        categoriaDAO = DAOFactory.crearCategoriaDAO();
        caracteristicaDAO = DAOFactory.crearCaracteristicaDAO();
        opcionDAO = DAOFactory.crearOpcionDAO();
        exclusionDAO = DAOFactory.crearExclusionDAO();
    }

    // ===================== Consultas (RF-17) =====================

    // Busca productos por nombre o SKU; idCategoria 0 = todas; visibilidad: constantes de ProductoDAO
    // RF-17 · Paso 3: consulta los productos que cumplen el criterio (nombre o SKU, categoría y estado).
    public List<Producto> buscar(String texto, int idCategoria, int visibilidad) throws DAOException {
        return productoDAO.buscar(texto, idCategoria, visibilidad);
    }

    // Lee un producto con sus opciones; devuelve null si ya no existe
    // RF-17 · Paso 6 / RF-19 · Paso 2: lee el producto con sus opciones (null = ya no existe).
    public Producto buscarPorId(int idPostre) throws DAOException {
        return productoDAO.buscarPorId(idPostre);
    }

    // Lee la categoría elegida en el filtro; devuelve null si ya no existe
    // RF-17 · Paso 2: valida la categoría elegida en el filtro.
    public Categoria buscarCategoria(int idCategoria) throws DAOException {
        return categoriaDAO.buscarPorId(idCategoria);
    }

    // Categorías activas para el formulario de alta
    // RF-18 · Paso 2: categorías activas para el formulario.
    public List<Categoria> listarCategoriasActivas() throws DAOException {
        return categoriaDAO.buscar("", EstadoRegistro.ACTIVA);
    }

    // Arma las reglas de personalización de una categoría: características con opciones y exclusiones activas
    public ConfiguracionPostre cargarConfiguracion(int idCategoria) throws DAOException {
        List<Caracteristica> caracteristicas = caracteristicaDAO.listarPorCategoria(idCategoria);
        for (Caracteristica caracteristica : caracteristicas) {
            caracteristica.setOpciones(opcionDAO.listarPorCaracteristica(caracteristica.getIdCaracteristica()));
        }
        List<Exclusion> activas = new ArrayList<Exclusion>();
        for (Exclusion exclusion : exclusionDAO.listarPorCategoria(idCategoria)) {
            if (exclusion.estaActiva()) {
                activas.add(exclusion);
            }
        }
        return new ConfiguracionPostre(caracteristicas, activas);
    }

    // Lista los datos opcionales que le faltan a un producto (para la ficha de RF-17)
    // RF-17 · Excepción paso 7: datos opcionales que faltan (por ejemplo «Sin imagen»).
    public List<String> camposIncompletos(Producto producto) {
        List<String> faltantes = new ArrayList<String>();
        if (!producto.tieneFotografia()) {
            faltantes.add("fotografía");
        }
        if (ValidacionCatalogo.estaVacio(producto.getDescripcion())) {
            faltantes.add("descripción");
        }
        if (producto.getPrecioVenta() <= 0) {
            faltantes.add("precio (sus opciones aún no tienen receta)");
        }
        return faltantes;
    }

    // ===================== Registro (RF-18) =====================

    // RF-18 (Registro de producto): inicia aquí la regla de negocio (pasos 5 a 7).
    public ResultadoCatalogo registrar(Producto datos) {
        if (!PermisosCatalogo.puedeEditarProductos()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        // Paso 5: campos obligatorios y fotografía
        ResultadoCatalogo validacion = validarCampos(datos);
        // RF-18 · Excepción paso 5: campos incompletos, precio o fotografía no válidos.
        if (validacion != null) {
            return validacion;
        }

        Producto mismaClave;
        try {
            // Paso 6: categoría activa
            ResultadoCatalogo categoria = revisarCategoria(datos.getIdCategoria());
            // RF-18 · Excepción paso 6: «La categoría seleccionada ya no está disponible».
            if (categoria != null) {
                return categoria;
            }

            // Reglas de personalización (obligatorias, presupuesto, exclusiones)
            ResultadoCatalogo configuracion = revisarConfiguracion(datos);
            // RF-18 · Excepción: las opciones elegidas no cumplen las reglas de la categoría (obligatorias, presupuesto, exclusiones).
            if (configuracion != null) {
                return configuracion;
            }

            // Paso 6: nombre repetido
            // RF-18 · Excepción paso 6: «El producto ya está registrado».
            if (productoDAO.buscarPorNombre(datos.getNombre()) != null) {
                return ResultadoCatalogo.error(MSG_YA_REGISTRADO, ResultadoCatalogo.CAMPO_NOMBRE);
            }

            // La misma combinación de opciones ya existe en otro postre
            mismaClave = productoDAO.buscarPorClave(datos.getClaveConfiguracion());
            // RF-18 · Excepción: ya existe un producto con exactamente las mismas opciones.
            if (mismaClave != null && mismaClave.getNombre() != null) {
                return ResultadoCatalogo.existente("Ya existe un producto con exactamente las mismas opciones: «"
                        + mismaClave.getNombre() + "» (" + mismaClave.getSku() + ").",
                        mismaClave.getIdPostre(), ResultadoCatalogo.CAMPO_OPCIONES);
            }
        } catch (DAOException e) {
            // RF-18 · Excepción: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 7: se guarda (Postre + PostreOpcion en una transacción)
        // RF-18 · Paso 7: almacena el producto, genera su SKU y lo asocia a su categoría.
        try {
            if (mismaClave != null) {
                // Un cliente ya había armado este postre desde la web (sin nombre):
                // se reutiliza ese registro y se convierte en producto del catálogo
                datos.setIdPostre(mismaClave.getIdPostre());
                productoDAO.actualizar(datos, false);
            } else {
                productoDAO.insertar(datos);
            }
        } catch (DAOException e) {
            // RF-18 · Excepción paso 7: «No fue posible registrar el producto».
            return ResultadoCatalogo.error(MSG_NO_REGISTRADO, ResultadoCatalogo.CAMPO_CONEXION);
        }
        // RF-18 · Paso 8: informa el éxito.
        return ResultadoCatalogo.correcto("El producto «" + datos.getNombre() + "» se registró con el SKU "
                + datos.getSku());
    }

    // ===================== Modificación (RF-19) =====================

    // confirmado = ya se aceptó el aviso de precio en pedidos vigentes
    // RF-19 (Modificación de producto): inicia aquí la regla de negocio (pasos 5 a 8).
    public ResultadoCatalogo modificar(Producto datos, boolean confirmado) {
        if (!PermisosCatalogo.puedeEditarProductos()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        ResultadoCatalogo validacion = validarCampos(datos);
        // RF-19 · Excepción paso 5: datos inválidos.
        if (validacion != null) {
            return validacion;
        }

        Producto actual;
        boolean cambiaronOpciones;
        List<Integer> foliosVigentes;
        try {
            // Paso 6: el producto sigue existiendo
            actual = productoDAO.buscarPorId(datos.getIdPostre());
            // RF-19 · Excepción paso 6: «El producto ya no existe».
            if (actual == null) {
                return ResultadoCatalogo.noEncontrado(MSG_YA_NO_EXISTE);
            }

            ResultadoCatalogo categoria = revisarCategoria(datos.getIdCategoria());
            // RF-19 · Excepción paso 6: la categoría no está activa.
            if (categoria != null) {
                return categoria;
            }

            ResultadoCatalogo configuracion = revisarConfiguracion(datos);
            if (configuracion != null) {
                return configuracion;
            }

            Producto conMismoNombre = productoDAO.buscarPorNombre(datos.getNombre());
            // RF-19 · Excepción paso 6: el nombre ya pertenece a otro producto.
            if (conMismoNombre != null && conMismoNombre.getIdPostre() != datos.getIdPostre()) {
                return ResultadoCatalogo.error(MSG_YA_REGISTRADO, ResultadoCatalogo.CAMPO_NOMBRE);
            }

            cambiaronOpciones = !datos.getClaveConfiguracion().equals(actual.getClaveConfiguracion())
                    || datos.getIdCategoria() != actual.getIdCategoria();
            if (cambiaronOpciones) {
                // Los pedidos guardan el IdPostre: si ya se vendió, sus opciones no se tocan
                // RF-19 · Excepción: el producto ya se vendió; sus opciones no se pueden cambiar.
                if (productoDAO.tienePedidos(datos.getIdPostre())) {
                    return ResultadoCatalogo.error(MSG_OPCIONES_CON_PEDIDOS, ResultadoCatalogo.CAMPO_OPCIONES);
                }
                Producto mismaClave = productoDAO.buscarPorClave(datos.getClaveConfiguracion());
                if (mismaClave != null && mismaClave.getIdPostre() != datos.getIdPostre()) {
                    String nombre = mismaClave.getNombre();
                    if (nombre == null) {
                        nombre = "un postre armado por un cliente";
                    }
                    return ResultadoCatalogo.existente("Esa combinación de opciones ya existe en «" + nombre + "» ("
                            + mismaClave.getSku() + ").", mismaClave.getIdPostre(), ResultadoCatalogo.CAMPO_OPCIONES);
                }
            }
            foliosVigentes = productoDAO.listarFoliosVigentes(datos.getIdPostre());
        } catch (DAOException e) {
            // RF-19 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 7: si cambia el precio y hay pedidos vigentes, se avisa que conservan su precio
        double precioNuevo = ConfiguracionPostre.calcularPrecio(datos.getOpciones());
        boolean cambioPrecio = Math.abs(precioNuevo - actual.getPrecioVenta()) > 0.004;
        // RF-19 · Paso 7: cambió el precio y hay pedidos vigentes; se pide confirmar (si no, regresa al formulario).
        if (cambioPrecio && !foliosVigentes.isEmpty() && !confirmado) {
            String mensaje = "El precio del producto cambia y está en pedidos vigentes ("
                    + ValidacionCatalogo.unirFolios(foliosVigentes) + "). Esos pedidos conservarán su precio original."
                    + "\n\n¿Deseas continuar?";
            return ResultadoCatalogo.confirmacion(mensaje, null);
        }

        // Paso 8: se actualiza
        try {
            productoDAO.actualizar(datos, cambiaronOpciones);
        } catch (DAOException e) {
            // RF-19 · Excepción paso 8: «No fue posible actualizar el producto».
            return ResultadoCatalogo.error(MSG_NO_ACTUALIZADO, ResultadoCatalogo.CAMPO_CONEXION);
        }
        // RF-19 · Paso 8: informa el éxito.
        return ResultadoCatalogo.correcto("El producto «" + datos.getNombre() + "» se actualizó correctamente");
    }

    // ===================== Baja (RF-20) =====================

    // Pasos 2 y 3: revisa si se puede dar de baja y arma la confirmación doble
    // RF-20 (Eliminación de producto): inicia aquí la regla de negocio (pasos 2 a 4).
    public ResultadoCatalogo prepararBaja(int idPostre) {
        if (!PermisosCatalogo.puedeEditarProductos()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            Producto producto = productoDAO.buscarPorId(idPostre);
            // RF-20 · Excepción paso 2: «Producto no encontrado».
            if (producto == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADO);
            }

            List<Integer> folios = productoDAO.listarFoliosVigentes(idPostre);
            // RF-20 · Excepción paso 3: «El producto tiene pedidos en curso» con sus folios.
            if (!folios.isEmpty()) {
                return ResultadoCatalogo.error(MSG_EN_PEDIDOS + ": " + ValidacionCatalogo.unirFolios(folios), null);
            }

            boolean conPedidos = productoDAO.tienePedidos(idPostre);
            String mensaje = "Producto: " + producto.getNombre() + " (" + producto.getSku() + ")\n\n";
            if (conPedidos) {
                mensaje = mensaje + "El producto tiene pedidos en su historial, así que se INHABILITARÁ:"
                        + " dejará de mostrarse en la tienda web, pero sus pedidos se conservan.";
            } else {
                mensaje = mensaje + "El producto no tiene pedidos, así que se ELIMINARÁ de la base de datos."
                        + " Esta acción no se puede deshacer.";
            }
            // RF-20 · Paso 4: mensaje de la confirmación doble (inhabilitar o eliminar).
            return ResultadoCatalogo.bajaPosible(mensaje, !conPedidos);
        } catch (DAOException e) {
            // RF-20 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }
    }

    // Paso 6: registra la baja
    // RF-20 · Paso 6 (después de que la Administradora confirmó en el paso 5).
    public ResultadoCatalogo darDeBaja(int idPostre) {
        ResultadoCatalogo revision = prepararBaja(idPostre);
        if (!revision.isExitoso()) {
            return revision;
        }
        try {
            // RF-20 · Paso 6: sin pedidos se elimina; con historial se inhabilita.
            if (revision.isSeEliminara()) {
                productoDAO.eliminar(idPostre);
                return ResultadoCatalogo.correcto("El producto se eliminó correctamente");
            }
            productoDAO.ocultar(idPostre);
            return ResultadoCatalogo.correcto("El producto se inhabilitó: ya no se muestra en la tienda web");
        } catch (DAOException e) {
            // RF-20 · Excepción paso 6: «No fue posible dar de baja el producto».
            return ResultadoCatalogo.error(MSG_NO_DADO_DE_BAJA, null);
        }
    }

    // ===================== Apoyo =====================

    // La categoría debe existir y estar activa; devuelve null si está bien
    // RF-18 / RF-19 · Paso 6: la categoría debe existir y estar activa.
    private ResultadoCatalogo revisarCategoria(int idCategoria) throws DAOException {
        Categoria categoria = categoriaDAO.buscarPorId(idCategoria);
        if (categoria == null || !categoria.estaActiva()) {
            return ResultadoCatalogo.error(MSG_CATEGORIA_NO_DISPONIBLE, ResultadoCatalogo.CAMPO_CATEGORIA);
        }
        return null;
    }

    // Vuelve a leer las opciones de la BD, aplica las reglas y calcula la clave; null si está bien
    private ResultadoCatalogo revisarConfiguracion(Producto datos) throws DAOException {
        ConfiguracionPostre configuracion = cargarConfiguracion(datos.getIdCategoria());

        // Se cambian las opciones del formulario por las de la BD (por si alguien las modificó)
        List<OpcionElegida> actualizadas = new ArrayList<OpcionElegida>();
        for (OpcionElegida elegida : datos.getOpciones()) {
            Opcion deLaBD = buscarOpcion(configuracion, elegida.getOpcion().getIdOpcion());
            if (deLaBD == null) {
                deLaBD = elegida.getOpcion();
            }
            actualizadas.add(new OpcionElegida(deLaBD, elegida.getCantidad()));
        }
        datos.setOpciones(actualizadas);

        List<String> errores = configuracion.validar(actualizadas);
        if (!errores.isEmpty()) {
            return ResultadoCatalogo.error(ValidacionCatalogo.unirLineas(errores), ResultadoCatalogo.CAMPO_OPCIONES);
        }
        datos.setClaveConfiguracion(ConfiguracionPostre.claveConfiguracion(actualizadas));
        return null;
    }

    private Opcion buscarOpcion(ConfiguracionPostre configuracion, int idOpcion) {
        for (Caracteristica caracteristica : configuracion.getCaracteristicas()) {
            for (Opcion opcion : caracteristica.getOpciones()) {
                if (opcion.getIdOpcion() == idOpcion) {
                    return opcion;
                }
            }
        }
        return null;
    }

    // Revisa los campos del formulario; devuelve null si todo está bien
    // RF-18 / RF-19 · Paso 5: campos obligatorios, precios y fotografía.
    private ResultadoCatalogo validarCampos(Producto datos) {
        if (datos.getIdCategoria() <= 0) {
            return ResultadoCatalogo.error("Elige la categoría del producto", ResultadoCatalogo.CAMPO_CATEGORIA);
        }
        if (!ValidacionCatalogo.nombreValido(datos.getNombre(), MAX_NOMBRE)) {
            return ResultadoCatalogo.error(MSG_NOMBRE_INVALIDO, ResultadoCatalogo.CAMPO_NOMBRE);
        }
        if (!ValidacionCatalogo.largoValido(datos.getDescripcion(), MAX_DESCRIPCION)) {
            return ResultadoCatalogo.error("La descripción no puede pasar de 2000 caracteres",
                    ResultadoCatalogo.CAMPO_DESCRIPCION);
        }
        // La fotografía es un enlace o un archivo de esta computadora, con formato permitido
        if (datos.tieneFotografia()) {
            // RF-18 · Excepción paso 5: «Formato o tamaño de imagen no permitido».
            if (!ValidacionCatalogo.imagenValida(datos.getLinkFotografia())
                    || !ValidacionCatalogo.largoValido(datos.getLinkFotografia(), MAX_ENLACE)) {
                return ResultadoCatalogo.error(MSG_IMAGEN_NO_PERMITIDA
                        + " (usa un enlace http/https o un archivo existente de tipo " + SelectorImagen.FORMATOS_TEXTO + ")",
                        ResultadoCatalogo.CAMPO_FOTO);
            }
        } else if (datos.isMostrarEnTienda()) {
            return ResultadoCatalogo.error(MSG_FOTO_OBLIGATORIA, ResultadoCatalogo.CAMPO_FOTO);
        }
        datos.setNombre(datos.getNombre().trim());
        return null;
    }
}

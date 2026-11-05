package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.dao.ProductoDAO;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.model.Producto;
import com.pac.modulos.catalogo.service.CategoriaService;
import com.pac.modulos.catalogo.service.ConfiguracionPostre;
import com.pac.modulos.catalogo.service.PermisosCatalogo;
import com.pac.modulos.catalogo.service.ProductoService;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.prefs.Preferences;

/**
 * Pestaña Productos (productos.fxml).
 * - RF-28 Visualización: búsqueda por nombre o SKU, filtros y ficha completa.
 * - RF-29 Registro y RF-30 Modificación: abren formulario-producto.fxml.
 * - RF-31 Baja: revisa pedidos vigentes, confirmación doble, inhabilita u elimina.
 */
public class ProductosController {

    private static final String RUTA_FORMULARIO = "/com/pac/view/catalogo/formulario-producto.fxml";
    private static final String[] VISIBILIDADES = {"Todos", "En tienda", "Ocultos"};

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<String> cmbVisibilidad;
    @FXML private Button btnNuevo;

    @FXML private TableView<FilaProducto> tblProductos;
    @FXML private ScrollPane scrollTiles;
    @FXML private FlowPane flujoTiles;
    @FXML private Button btnVistaTabla;
    @FXML private Button btnVistaCuadricula;
    @FXML private Label lblConteo;
    @FXML private Label lblVacio;

    @FXML private VBox cajaSinSeleccion;
    @FXML private ScrollPane cajaDetalle;
    @FXML private VBox cajaBotones;
    @FXML private ImageView imgProducto;
    @FXML private Label lblSinImagen;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetSku;
    @FXML private Label lblDetCategoria;
    @FXML private Label lblDetPrecio;
    @FXML private Label lblDetCosto;
    @FXML private Label lblDetTienda;
    @FXML private Label lblDetIncompletos;
    @FXML private Label lblDetDescripcion;
    @FXML private VBox listaOpciones;
    @FXML private Button btnEditar;
    @FXML private Button btnDarDeBaja;

    private ProductoService servicio = new ProductoService();
    private CategoriaService categoriaService = new CategoriaService();
    private CatalogoController catalogo;
    private Producto seleccionado;
    private boolean puedeEditar;
    private boolean llenandoSelectores = false;
    // true cuando los productos se ven en cuadrícula (tiles); se recuerda entre sesiones
    private boolean verCuadricula = false;

    // Elemento "Todas las categorías" del filtro (id 0)
    private Categoria todas;

    public void iniciar(CatalogoController catalogo) {
        this.catalogo = catalogo;
        todas = new Categoria();
        todas.setIdCategoria(0);
        todas.setNombre("Todas las categorías");

        for (String visibilidad : VISIBILIDADES) {
            cmbVisibilidad.getItems().add(visibilidad);
        }
        cmbVisibilidad.setValue(VISIBILIDADES[0]);

        verCuadricula = "cuadricula".equals(Preferences.userNodeForPackage(ProductosController.class).get("catalogoVista", "tabla"));
        aplicarVista();

        puedeEditar = PermisosCatalogo.puedeEditarProductos();
        ApoyoVista.mostrarNodo(btnNuevo, puedeEditar);
        ApoyoVista.mostrarNodo(btnEditar, puedeEditar);
        ApoyoVista.mostrarNodo(btnDarDeBaja, puedeEditar);
    }

    // Se llama cada vez que la pestaña queda visible
    // RF-28 (Visualización de productos): inicia aquí. Paso 1: se abre la pestaña Productos.
    public void alMostrar() {
        // RF-28 · Excepción paso 2: sin conexión, «Error de conexión» con «Reintentar».
        if (!llenarCategorias()) {
            mostrarErrorConexion();
            return;
        }
        cargarProductos();
    }

    // Llena el filtro de categorías conservando la elegida; false si no hubo conexión
    private boolean llenarCategorias() {
        Categoria antes = cmbCategoria.getValue();
        List<Categoria> categorias;
        try {
            categorias = categoriaService.listarTodas();
        } catch (DAOException e) {
            return false;
        }
        llenandoSelectores = true;
        cmbCategoria.getItems().clear();
        cmbCategoria.getItems().add(todas);
        cmbCategoria.getItems().addAll(categorias);
        cmbCategoria.setValue(todas);
        if (antes != null) {
            for (Categoria categoria : categorias) {
                if (categoria.getIdCategoria() == antes.getIdCategoria()) {
                    cmbCategoria.setValue(categoria);
                }
            }
        }
        llenandoSelectores = false;
        return true;
    }

    // ===================== Lista (RF-28) =====================

    @FXML
    // RF-28 · Paso 1: el usuario indica el criterio (categoría, nombre o SKU, estado).
    private void onBuscar() {
        if (llenandoSelectores) {
            return;
        }
        cargarProductos();
    }

    @FXML
    private void onLimpiarFiltro() {
        txtBuscar.clear();
        llenandoSelectores = true;
        cmbCategoria.setValue(todas);
        cmbVisibilidad.setValue(VISIBILIDADES[0]);
        llenandoSelectores = false;
        cargarProductos();
    }

    // RF-28 · Pasos 2 a 4: valida la categoría, consulta y muestra el listado.
    private void cargarProductos() {
        int idAntes = 0;
        if (seleccionado != null) {
            idAntes = seleccionado.getIdPostre();
        }
        tblProductos.getItems().clear();
        flujoTiles.getChildren().clear();
        mostrarDetalle(null);
        lblConteo.setText("");

        int idCategoria = 0;
        if (cmbCategoria.getValue() != null) {
            idCategoria = cmbCategoria.getValue().getIdCategoria();
        }

        List<Producto> productos;
        try {
            // Paso 2: la categoría elegida debe existir
            if (idCategoria > 0 && servicio.buscarCategoria(idCategoria) == null) {
                // RF-28 · Excepción paso 2: «Categoría no encontrada»; se limpia el filtro y se recargan las categorías.
                Alertas.aviso(ProductoService.MSG_CATEGORIA_NO_ENCONTRADA);
                llenarCategorias();
                llenandoSelectores = true;
                cmbCategoria.setValue(todas);
                llenandoSelectores = false;
                idCategoria = 0;
            }
            productos = servicio.buscar(txtBuscar.getText(), idCategoria, leerVisibilidad());
        } catch (DAOException e) {
            // RF-28 · Excepción paso 2: sin conexión.
            mostrarErrorConexion();
            return;
        }

        for (Producto producto : productos) {
            FilaProducto nueva = new FilaProducto(producto);
            tblProductos.getItems().add(nueva);
            flujoTiles.getChildren().add(crearTile(nueva));
        }
        aplicarVista();
        lblConteo.setText(productos.size() + " producto(s)");

        // Paso 3: ningún producto cumple el criterio
        if (productos.isEmpty()) {
            if (txtBuscar.getText() != null && !txtBuscar.getText().isBlank()) {
                // RF-28 · Excepción paso 3 / RF-30 y RF-31 · Excepción paso 1: no hay productos con ese criterio.
                lblVacio.setText(ProductoService.MSG_BUSQUEDA_VACIA);
            } else {
                // RF-28 · Excepción paso 3: «No hay productos registrados con el criterio seleccionado» (con «+ Nuevo Postre»).
                lblVacio.setText(ProductoService.MSG_SIN_PRODUCTOS);
            }
        }

        for (FilaProducto fila : tblProductos.getItems()) {
            if (fila.getProducto().getIdPostre() == idAntes) {
                tblProductos.getSelectionModel().select(fila);
                mostrarSeleccionado();
            }
        }
    }

    private int leerVisibilidad() {
        String valor = cmbVisibilidad.getValue();
        if (VISIBILIDADES[1].equals(valor)) {
            return ProductoDAO.VISIBILIDAD_EN_TIENDA;
        }
        if (VISIBILIDADES[2].equals(valor)) {
            return ProductoDAO.VISIBILIDAD_OCULTOS;
        }
        return ProductoDAO.VISIBILIDAD_TODOS;
    }

    private boolean hayFiltro() {
        boolean hayTexto = txtBuscar.getText() != null && !txtBuscar.getText().isBlank();
        boolean hayCategoria = cmbCategoria.getValue() != null && cmbCategoria.getValue().getIdCategoria() > 0;
        return hayTexto || hayCategoria || leerVisibilidad() != ProductoDAO.VISIBILIDAD_TODOS;
    }

    private void mostrarErrorConexion() {
        tblProductos.getItems().clear();
        flujoTiles.getChildren().clear();
        aplicarVista();
        mostrarDetalle(null);
        lblVacio.setText(ProductoService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
    }

    // ===================== Ficha del producto =====================

    @FXML
    private void onVistaTabla() {
        cambiarVista(false);
    }

    @FXML
    private void onVistaCuadricula() {
        cambiarVista(true);
    }

    private void cambiarVista(boolean cuadricula) {
        verCuadricula = cuadricula;
        String valor = "tabla";
        if (cuadricula) {
            valor = "cuadricula";
        }
        Preferences.userNodeForPackage(ProductosController.class).put("catalogoVista", valor);
        aplicarVista();
    }

    // Muestra la tabla o la cuadrícula. Si no hay productos se deja la tabla, que trae el mensaje de "sin datos"
    private void aplicarVista() {
        boolean hayProductos = !tblProductos.getItems().isEmpty();
        boolean mostrarTiles = verCuadricula && hayProductos;
        ApoyoVista.mostrarNodo(scrollTiles, mostrarTiles);
        ApoyoVista.mostrarNodo(tblProductos, !mostrarTiles);
        btnVistaTabla.getStyleClass().remove("activo");
        btnVistaCuadricula.getStyleClass().remove("activo");
        if (verCuadricula) {
            btnVistaCuadricula.getStyleClass().add("activo");
        } else {
            btnVistaTabla.getStyleClass().add("activo");
        }
    }

    // Tarjeta de un producto en la cuadrícula: imagen arriba y sus datos abajo
    private VBox crearTile(final FilaProducto fila) {
        Producto producto = fila.getProducto();

        ImageView imagen = new ImageView();
        imagen.setFitWidth(176);
        imagen.setFitHeight(120);
        imagen.setPreserveRatio(true);
        Label sinImagen = new Label("Sin imagen");
        sinImagen.getStyleClass().add("texto-suave");
        sinImagen.setGraphic(new FontIcon("bi-image-fill"));
        StackPane marcoImagen = new StackPane(imagen, sinImagen);
        marcoImagen.getStyleClass().add("tile-imagen");
        marcoImagen.setMinHeight(130);
        marcoImagen.setPrefHeight(130);
        ApoyoVista.cargarImagen(imagen, sinImagen, producto.getLinkFotografia());

        Label nombre = new Label(fila.getNombre());
        nombre.getStyleClass().add("tile-nombre");
        nombre.setWrapText(true);
        nombre.setMinHeight(34);
        Label categoria = new Label(fila.getCategoria());
        categoria.getStyleClass().add("texto-suave");
        Label sku = new Label("SKU " + fila.getSku());
        sku.getStyleClass().add("texto-suave");
        Label precio = new Label(fila.getPrecio());
        precio.getStyleClass().add("precio");
        Label tienda = new Label(fila.getEnTienda());
        tienda.getStyleClass().add("estado-chip");
        if (producto.isMostrarEnTienda()) {
            tienda.getStyleClass().add("estado-activo");
        } else {
            tienda.getStyleClass().add("estado-inactivo");
        }
        HBox pie = new HBox(8, precio, tienda);
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox tile = new VBox(6, marcoImagen, nombre, categoria, sku, pie);
        tile.getStyleClass().add("tile-producto");
        tile.setPrefWidth(200);
        tile.setMinWidth(200);
        tile.setMaxWidth(200);
        tile.getProperties().put("idPostre", Integer.valueOf(producto.getIdPostre()));
        tile.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                tblProductos.getSelectionModel().select(fila);
                boolean hay = mostrarSeleccionado();
                marcarTiles();
                if (hay && evento.getClickCount() == 2 && puedeEditar) {
                    onEditar();
                }
            }
        });
        return tile;
    }

    // Resalta en la cuadrícula el producto que está seleccionado
    private void marcarTiles() {
        for (javafx.scene.Node nodo : flujoTiles.getChildren()) {
            nodo.getStyleClass().remove("tile-seleccionado");
            Object id = nodo.getProperties().get("idPostre");
            if (seleccionado != null && id instanceof Integer
                    && ((Integer) id).intValue() == seleccionado.getIdPostre()) {
                nodo.getStyleClass().add("tile-seleccionado");
            }
        }
    }

    @FXML
    // RF-28 · Paso 5: el usuario selecciona un producto para ver sus características.
    private void onClicTabla(MouseEvent evento) {
        boolean hay = mostrarSeleccionado();
        if (hay && evento.getClickCount() == 2 && puedeEditar) {
            onEditar();
        }
    }

    @FXML
    private void onTeclaTabla() {
        mostrarSeleccionado();
    }

    // Pasos 6 y 7: lee el producto con sus opciones y muestra la ficha
    private boolean mostrarSeleccionado() {
        FilaProducto fila = tblProductos.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return false;
        }
        if (seleccionado != null && seleccionado.getIdPostre() == fila.getProducto().getIdPostre()) {
            return true;
        }
        Producto producto;
        try {
            producto = servicio.buscarPorId(fila.getProducto().getIdPostre());
        } catch (DAOException e) {
            // RF-28 · Excepción paso 6: sin conexión.
            Alertas.error(ProductoService.MSG_ERROR_CONEXION);
            return false;
        }
        // RF-28 · Excepción paso 6: «Producto no encontrado»; se retira del listado.
        if (producto == null) {
            // Se retira de la lista
            Alertas.aviso(ProductoService.MSG_NO_ENCONTRADO);
            tblProductos.getItems().remove(fila);
            return false;
        }
        mostrarDetalle(producto);
        return true;
    }

    // RF-28 · Paso 7: ficha completa (SKU, precio base, fotografía y características con sus opciones).
    private void mostrarDetalle(Producto producto) {
        seleccionado = producto;
        marcarTiles();
        ApoyoVista.mostrarNodo(cajaDetalle, producto != null);
        ApoyoVista.mostrarNodo(cajaBotones, producto != null);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, producto == null);
        if (producto == null) {
            return;
        }

        ApoyoVista.cargarImagen(imgProducto, lblSinImagen, producto.getLinkFotografia());
        lblDetNombre.setText(producto.getNombre());
        lblDetSku.setText(producto.getSku());
        FilaProducto fila = new FilaProducto(producto);
        lblDetCategoria.setText(fila.getCategoria());
        lblDetPrecio.setText(fila.getPrecio());
        lblDetCosto.setText(fila.getCosto());
        lblDetTienda.setText(fila.getEnTienda());
        lblDetTienda.getStyleClass().removeAll("estado-activo", "estado-inactivo");
        if (producto.isMostrarEnTienda()) {
            lblDetTienda.getStyleClass().add("estado-activo");
        } else {
            lblDetTienda.getStyleClass().add("estado-inactivo");
        }
        if (producto.getDescripcion() == null || producto.getDescripcion().isBlank()) {
            lblDetDescripcion.setText("Sin descripción");
        } else {
            lblDetDescripcion.setText(producto.getDescripcion());
        }

        // RF-28 · Excepción paso 7: marca «Sin imagen» y señala los campos incompletos.
        // Campos opcionales que faltan (paso 7)
        List<String> faltantes = servicio.camposIncompletos(producto);
        ApoyoVista.mostrarNodo(lblDetIncompletos, !faltantes.isEmpty());
        lblDetIncompletos.setText("Datos incompletos: " + ValidacionCatalogo.unir(faltantes));

        // Opciones agrupadas por característica
        listaOpciones.getChildren().clear();
        ConfiguracionPostre configuracion = null;
        try {
            configuracion = servicio.cargarConfiguracion(producto.getIdCategoria());
        } catch (DAOException e) {
            Label error = new Label(ProductoService.MSG_ERROR_CONEXION);
            error.getStyleClass().add("texto-suave");
            listaOpciones.getChildren().add(error);
            return;
        }
        for (Caracteristica caracteristica : configuracion.getCaracteristicas()) {
            String elegidas = "";
            for (OpcionElegida elegida : producto.getOpciones()) {
                if (elegida.getOpcion().getIdCaracteristica() == caracteristica.getIdCaracteristica()) {
                    if (!elegidas.isEmpty()) {
                        elegidas = elegidas + ", ";
                    }
                    elegidas = elegidas + elegida.getOpcion().getNombre();
                    if (elegida.getCantidad() > 1) {
                        elegidas = elegidas + " ×" + elegida.getCantidad();
                    }
                }
            }
            if (elegidas.isEmpty()) {
                elegidas = "—";
            }
            Label linea = new Label(caracteristica.getNombre() + ": " + elegidas);
            linea.setWrapText(true);
            linea.getStyleClass().add("funcion-item");
            listaOpciones.getChildren().add(linea);
        }
        if (producto.getOpciones().isEmpty()) {
            Label sinOpciones = new Label("Sin opciones registradas");
            sinOpciones.getStyleClass().add("texto-suave");
            listaOpciones.getChildren().add(sinOpciones);
        }
    }

    // ===================== Registro y modificación (RF-29, RF-30) =====================

    @FXML
    // RF-29 (Registro de producto): inicia aquí. Paso 1: «+ Nuevo Postre».
    private void onNuevo() {
        // Paso 2 de RF-29: debe existir al menos una categoría activa
        List<Categoria> activas;
        try {
            activas = servicio.listarCategoriasActivas();
        } catch (DAOException e) {
            // RF-29 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            Alertas.error(ProductoService.MSG_ERROR_CONEXION);
            return;
        }
        if (activas.isEmpty()) {
            // RF-29 · Excepción paso 2: no hay categorías activas; se ofrece registrar una.
            if (Alertas.confirmar(ProductoService.MSG_SIN_CATEGORIAS, "¿Quieres ir a registrar una categoría?")) {
                catalogo.irACategorias();
            }
            return;
        }
        abrirFormulario(null, activas);
    }

    @FXML
    // RF-30 (Modificación de producto): inicia aquí. Paso 1: selecciona el producto y pide «Editar detalles».
    private void onEditar() {
        if (seleccionado == null) {
            Alertas.info("Selecciona un producto de la lista");
            return;
        }
        abrirPorId(seleccionado.getIdPostre());
    }

    // Abre el formulario de edición de un producto (también se usa para "abrir el existente")
    // RF-30 · Paso 2: valida que el producto exista y recupera sus datos.
    private void abrirPorId(int idPostre) {
        Producto producto;
        List<Categoria> activas;
        try {
            producto = servicio.buscarPorId(idPostre);
            activas = servicio.listarCategoriasActivas();
        } catch (DAOException e) {
            // RF-30 · Excepción paso 2: «Error de conexión».
            Alertas.error(ProductoService.MSG_ERROR_CONEXION);
            return;
        }
        // RF-30 · Excepción paso 2: «Producto no encontrado».
        if (producto == null) {
            Alertas.aviso(ProductoService.MSG_NO_ENCONTRADO);
            cargarProductos();
            return;
        }
        abrirFormulario(producto, activas);
    }

    private void abrirFormulario(Producto producto, List<Categoria> activas) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de producto");
            return;
        }
        FormularioProductoController formulario = cargador.getController();
        String titulo = "Nuevo Postre";
        if (producto == null) {
            formulario.prepararNuevo(activas);
        } else {
            formulario.prepararEdicion(producto, activas);
            titulo = "Editar detalles del producto";
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, tblProductos.getScene().getWindow(), titulo);

        // RF-29 · Paso 8 / RF-30 · Paso 9: informa el éxito y actualiza el listado.
        if (formulario.seGuardo()) {
            Alertas.info(formulario.getMensajeExito());
        }
        if (formulario.seGuardo() || formulario.debeRecargarLista()) {
            seleccionado = null;
            cargarProductos();
        }
        // Se pidió abrir el producto que ya tiene la misma configuración
        if (formulario.getIdParaAbrir() > 0) {
            abrirPorId(formulario.getIdParaAbrir());
        }
    }

    // ===================== Baja (RF-31) =====================

    @FXML
    // RF-31 (Eliminación de producto): inicia aquí. Paso 1: selecciona el producto y pide darlo de baja.
    private void onDarDeBaja() {
        if (seleccionado == null) {
            Alertas.info("Selecciona un producto de la lista");
            return;
        }
        ResultadoCatalogo revision = servicio.prepararBaja(seleccionado.getIdPostre());
        if (!revision.isExitoso()) {
            // RF-31 · Excepciones pasos 2 y 3: no existe, sin conexión o tiene pedidos en curso.
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                cargarProductos();
            }
            return;
        }
        // RF-31 · Pasos 4 y 5: confirmación doble; si cancela, el producto queda sin cambios.
        if (!Alertas.confirmarDoble("¿Dar de baja este producto?", revision.getMensaje())) {
            return;
        }
        ResultadoCatalogo resultado = servicio.darDeBaja(seleccionado.getIdPostre());
        if (resultado.isExitoso()) {
            // RF-31 · Paso 7: informa el éxito y actualiza el listado.
            Alertas.info(resultado.getMensaje());
        } else {
            // RF-31 · Excepción paso 6: «No fue posible dar de baja el producto».
            Alertas.error(resultado.getMensaje());
        }
        seleccionado = null;
        cargarProductos();
    }
}

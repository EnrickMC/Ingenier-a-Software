package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.pedidos.controller.BarraEstados;
import com.pac.modulos.catalogo.service.CategoriaService;
import com.pac.modulos.catalogo.service.PermisosCatalogo;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pestaña Categorías (categorias.fxml).
 * - RF-15 Visualización: lista con búsqueda, filtro por estado y detalle.
 * - RF-16 Registro y RF-17 Modificación: abren formulario-categoria.fxml.
 * - RF-18 Baja: revisa productos activos, confirmación doble, inhabilita o elimina.
 */
public class CategoriasController {

    private static final String RUTA_FORMULARIO = "/com/pac/view/catalogo/formulario-categoria.fxml";

    @FXML private TextField txtBuscar;
    @FXML private BarraEstados barraEstados;
    @FXML private Button btnNueva;

    @FXML private TableView<FilaCategoria> tblCategorias;
    @FXML private Label lblConteo;
    @FXML private Label lblAviso;
    @FXML private Label lblVacio;

    @FXML private VBox cajaDetalle;
    @FXML private VBox cajaSinSeleccion;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetEstado;
    @FXML private Label lblDetProductos;
    @FXML private Label lblDetDescripcion;
    @FXML private Button btnModificar;
    @FXML private Button btnDarDeBaja;

    private CategoriaService servicio = new CategoriaService();
    private CatalogoController catalogo;
    private Categoria seleccionada;
    private boolean puedeEditar;

    // Lo llama CatalogoController al abrir el apartado
    // RF-15 · Paso 1: la Administradora o el personal entran a la pestaña Categorías (el permiso se revisa en CatalogoController, paso 2).
    public void iniciar(CatalogoController catalogo) {
        this.catalogo = catalogo;
        // Botones de estado: cada uno filtra la lista
        barraEstados.agregar(EstadoRegistro.ACTIVA.getValorBD(), "Activa", "estado-terminado", "bi-check-circle-fill");
        barraEstados.agregar(EstadoRegistro.INACTIVA.getValorBD(), "Inactiva", "estado-baja", "bi-slash-circle-fill");
        barraEstados.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onBuscar();
            }
        });

        // El personal solo consulta: se ocultan los botones de cambios
        puedeEditar = PermisosCatalogo.puedeEditarCatalogo();
        ApoyoVista.mostrarNodo(btnNueva, puedeEditar);
        ApoyoVista.mostrarNodo(btnModificar, puedeEditar);
        ApoyoVista.mostrarNodo(btnDarDeBaja, puedeEditar);
    }

    // Se llama cada vez que la pestaña queda visible
    public void alMostrar() {
        cargarCategorias();
    }

    // ===================== Lista (RF-15) =====================

    // RF-15 (Visualización de categorías): inicia aquí en la pantalla. Paso 3: consulta las categorías.
    private void cargarCategorias() {
        int idAntes = 0;
        if (seleccionada != null) {
            idAntes = seleccionada.getIdCategoria();
        }

        tblCategorias.getItems().clear();
        mostrarDetalle(null);
        ApoyoVista.mostrarNodo(lblAviso, false);

        List<Categoria> categorias;
        try {
            categorias = servicio.buscar(txtBuscar.getText(), leerFiltroEstado());
        } catch (DAOException e) {
            // RF-15 · Excepción paso 3: sin conexión, «Error de conexión, intente de nuevo» (con «Reintentar»).
            lblVacio.setText(CategoriaService.MSG_ERROR_CONEXION_LISTA);
            lblConteo.setText("");
            return;
        }

        // Paso 4: los registros incompletos no se muestran y se avisa
        int incompletas = 0;
        for (Categoria categoria : categorias) {
            if (servicio.datosCompletos(categoria)) {
                tblCategorias.getItems().add(new FilaCategoria(categoria));
            } else {
                incompletas++;
            }
        }
        // RF-15 · Excepción paso 4: «Algunas categorías no pudieron cargarse».
        if (incompletas > 0) {
            lblAviso.setText("Algunas categorías no pudieron cargarse");
            ApoyoVista.mostrarNodo(lblAviso, true);
        }
        lblConteo.setText(tblCategorias.getItems().size() + " categoría(s)");

        // RF-15 · Paso 5: despliega el listado (nombre, descripción, estado y número de productos).
        if (tblCategorias.getItems().isEmpty()) {
            if (hayFiltro()) {
                // RF-15 · Excepción paso 6: «Sin resultados para el criterio ingresado» (con «Limpiar filtro»). También RF-17/RF-18 paso 1.
                lblVacio.setText(CategoriaService.MSG_SIN_RESULTADOS);
            } else {
                // RF-15 · Excepción paso 4: «No hay categorías registradas» (con «+ Nueva categoría»).
                lblVacio.setText(CategoriaService.MSG_SIN_CATEGORIAS);
            }
        }

        // Se vuelve a seleccionar la categoría que estaba elegida
        for (FilaCategoria fila : tblCategorias.getItems()) {
            if (fila.getCategoria().getIdCategoria() == idAntes) {
                tblCategorias.getSelectionModel().select(fila);
                mostrarDetalle(fila.getCategoria());
            }
        }
    }

    private EstadoRegistro leerFiltroEstado() {
        String valor = barraEstados.getClave();
        if (valor == null) {
            return null;
        }
        return EstadoRegistro.desdeBD(valor);
    }

    private boolean hayFiltro() {
        boolean hayTexto = txtBuscar.getText() != null && !txtBuscar.getText().isBlank();
        return hayTexto || leerFiltroEstado() != null;
    }

    @FXML
    // RF-15 · Paso 6: aplica la búsqueda o el filtro.
    private void onBuscar() {
        cargarCategorias();
    }

    @FXML
    private void onLimpiarFiltro() {
        txtBuscar.clear();
        barraEstados.limpiar();
        cargarCategorias();
    }

    // ===================== Detalle =====================

    // Un clic muestra el detalle; doble clic abre "Modificar"
    @FXML
    private void onClicTabla(MouseEvent evento) {
        FilaCategoria fila = tblCategorias.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }
        mostrarDetalle(fila.getCategoria());
        if (evento.getClickCount() == 2 && puedeEditar) {
            onModificar();
        }
    }

    @FXML
    private void onTeclaTabla() {
        FilaCategoria fila = tblCategorias.getSelectionModel().getSelectedItem();
        if (fila != null) {
            mostrarDetalle(fila.getCategoria());
        }
    }

    private void mostrarDetalle(Categoria categoria) {
        seleccionada = categoria;
        ApoyoVista.mostrarNodo(cajaDetalle, categoria != null);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, categoria == null);
        if (categoria == null) {
            return;
        }
        catalogo.setIdCategoriaActual(categoria.getIdCategoria());
        lblDetNombre.setText(categoria.getNombre());
        lblDetEstado.setText(categoria.getEstado().getValorBD());
        lblDetEstado.getStyleClass().removeAll("estado-activo", "estado-inactivo");
        if (categoria.estaActiva()) {
            lblDetEstado.getStyleClass().add("estado-activo");
        } else {
            lblDetEstado.getStyleClass().add("estado-inactivo");
        }
        lblDetProductos.setText(String.valueOf(categoria.getNumeroProductos()));
        if (categoria.getDescripcion() == null || categoria.getDescripcion().isBlank()) {
            lblDetDescripcion.setText("Sin descripción");
        } else {
            lblDetDescripcion.setText(categoria.getDescripcion());
        }
    }

    @FXML
    private void onVerCaracteristicas() {
        if (seleccionada != null) {
            catalogo.irACaracteristicas(seleccionada);
        }
    }

    // ===================== Registro y modificación (RF-16, RF-17) =====================

    @FXML
    // RF-16 (Registro de categorías): inicia aquí. Paso 1: la Administradora pide una nueva categoría.
    private void onNueva() {
        abrirFormulario(null);
    }

    @FXML
    // RF-17 (Modificación de categorías): inicia aquí. Paso 1: selecciona la categoría y pide modificarla.
    private void onModificar() {
        if (seleccionada == null) {
            Alertas.info("Selecciona una categoría de la lista");
            return;
        }
        // Paso 2 de RF-17: se valida que siga existiendo y se leen sus datos actuales
        Categoria categoria;
        try {
            categoria = servicio.buscarPorId(seleccionada.getIdCategoria());
        } catch (DAOException e) {
            // RF-17 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            Alertas.error(CategoriaService.MSG_ERROR_CONEXION);
            return;
        }
        // RF-17 · Excepción paso 2: «Categoría no encontrada»; se recarga el listado.
        if (categoria == null) {
            Alertas.aviso(CategoriaService.MSG_NO_ENCONTRADA);
            cargarCategorias();
            return;
        }
        abrirFormulario(categoria);
    }

    private void abrirFormulario(Categoria categoria) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de categoría");
            return;
        }
        FormularioCategoriaController formulario = cargador.getController();
        String titulo = "Nueva categoría";
        if (categoria == null) {
            formulario.prepararNueva();
        } else {
            formulario.prepararEdicion(categoria);
            titulo = "Modificar categoría";
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, tblCategorias.getScene().getWindow(), titulo);

        // RF-16 / RF-17 · Paso 7 / Paso 9: cierra el formulario, informa el éxito y actualiza el listado.
        if (formulario.seGuardo()) {
            Alertas.info(formulario.getMensajeExito());
        }
        if (formulario.seGuardo() || formulario.debeRecargarLista()) {
            cargarCategorias();
        }
    }

    // ===================== Baja (RF-18) =====================

    @FXML
    // RF-18 (Eliminación de categorías): inicia aquí. Paso 1: selecciona la categoría y pide darla de baja.
    private void onDarDeBaja() {
        if (seleccionada == null) {
            Alertas.info("Selecciona una categoría de la lista");
            return;
        }

        // Pasos 2 y 3: existe y no tiene productos activos
        ResultadoCatalogo revision = servicio.prepararBaja(seleccionada.getIdCategoria());
        // RF-18 · Excepciones pasos 2 y 3: no existe, sin conexión o tiene productos activos.
        if (!revision.isExitoso()) {
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                cargarCategorias();
            }
            return;
        }

        // Pasos 4 y 5: confirmación con las consecuencias
        // RF-18 · Excepción paso 5: la Administradora canceló; la categoría queda sin cambios.
        if (!Alertas.confirmarDoble("¿Dar de baja esta categoría?", revision.getMensaje())) {
            return;
        }

        ResultadoCatalogo resultado = servicio.darDeBaja(seleccionada.getIdCategoria());
        // RF-18 · Paso 7: informa el éxito y actualiza el listado. Si falla, excepción paso 6.
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        seleccionada = null;
        cargarCategorias();
    }
}

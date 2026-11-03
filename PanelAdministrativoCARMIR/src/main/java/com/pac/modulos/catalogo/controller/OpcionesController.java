package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.service.CaracteristicaService;
import com.pac.modulos.catalogo.service.ExclusionService;
import com.pac.modulos.catalogo.service.OpcionService;
import com.pac.modulos.catalogo.service.PermisosCatalogo;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.controller.RecetasController;
import com.pac.modulos.inventario.service.PermisosInventario;
import com.pac.navigation.Navegador;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña Opciones (opciones.fxml).
 * Visualización, registro, modificación y baja de opciones de característica.
 */
public class OpcionesController {

    private static final String RUTA_FORMULARIO = "/com/pac/view/catalogo/formulario-opcion.fxml";

    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Caracteristica> cmbCaracteristica;
    @FXML private TextField txtBuscar;
    @FXML private Button btnNueva;

    @FXML private TableView<FilaOpcion> tblOpciones;
    @FXML private Label lblConteo;
    @FXML private Label lblAviso;
    @FXML private Label lblVacio;

    @FXML private VBox cajaSinSeleccion;
    @FXML private ScrollPane cajaDetalle;
    @FXML private VBox cajaBotones;
    @FXML private Button btnReceta;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetDescripcion;
    @FXML private ImageView imgOpcion;
    @FXML private Label lblSinImagen;
    @FXML private Label lblDetValores;
    @FXML private VBox listaExclusiones;
    @FXML private Button btnModificar;
    @FXML private Button btnDarDeBaja;

    private OpcionService servicio = new OpcionService();
    private CaracteristicaService caracteristicaService = new CaracteristicaService();
    private ExclusionService exclusionService = new ExclusionService();
    private CatalogoController catalogo;
    private Opcion seleccionada;
    private boolean puedeEditar;
    private boolean llenandoSelectores = false;

    public void iniciar(CatalogoController catalogo) {
        this.catalogo = catalogo;
        puedeEditar = PermisosCatalogo.puedeEditarCatalogo();
        ApoyoVista.mostrarNodo(btnNueva, puedeEditar);
        ApoyoVista.mostrarNodo(btnModificar, puedeEditar);
        ApoyoVista.mostrarNodo(btnDarDeBaja, puedeEditar);
    }

    // Se llama cada vez que la pestaña queda visible
    // RF «Visualización de opciones de característica»: inicia aquí. Paso 1: se abre la pestaña Opciones.
    public void alMostrar() {
        llenandoSelectores = true;
        boolean conectado = catalogo.llenarCategorias(cmbCategoria);
        if (conectado) {
            conectado = llenarCaracteristicas();
        }
        llenandoSelectores = false;
        // RF «Visualización de opciones de característica» · Excepción paso 3: sin conexión, «Error de conexión» con «Reintentar».
        if (!conectado) {
            mostrarErrorConexion();
            return;
        }
        cargarOpciones();
    }

    // Llena el selector de características de la categoría elegida; false si no hubo conexión
    private boolean llenarCaracteristicas() {
        cmbCaracteristica.getItems().clear();
        cmbCaracteristica.setValue(null);
        Categoria categoria = cmbCategoria.getValue();
        if (categoria == null) {
            return true;
        }
        List<Caracteristica> lista;
        try {
            lista = caracteristicaService.listarPorCategoria(categoria.getIdCategoria());
        } catch (DAOException e) {
            return false;
        }
        cmbCaracteristica.getItems().setAll(lista);
        for (Caracteristica caracteristica : lista) {
            if (caracteristica.getIdCaracteristica() == catalogo.getIdCaracteristicaActual()) {
                cmbCaracteristica.setValue(caracteristica);
            }
        }
        if (cmbCaracteristica.getValue() == null && !lista.isEmpty()) {
            cmbCaracteristica.setValue(lista.get(0));
            catalogo.setIdCaracteristicaActual(lista.get(0).getIdCaracteristica());
        }
        return true;
    }

    @FXML
    private void onCambioCategoria() {
        if (llenandoSelectores) {
            return;
        }
        Categoria categoria = cmbCategoria.getValue();
        if (categoria != null) {
            catalogo.setIdCategoriaActual(categoria.getIdCategoria());
        }
        llenandoSelectores = true;
        boolean conectado = llenarCaracteristicas();
        llenandoSelectores = false;
        if (!conectado) {
            mostrarErrorConexion();
            return;
        }
        cargarOpciones();
    }

    @FXML
    private void onCambioCaracteristica() {
        if (llenandoSelectores) {
            return;
        }
        Caracteristica caracteristica = cmbCaracteristica.getValue();
        if (caracteristica != null) {
            catalogo.setIdCaracteristicaActual(caracteristica.getIdCaracteristica());
        }
        cargarOpciones();
    }

    @FXML
    // RF «Visualización de opciones de característica» · Paso 1: el usuario escribe un criterio de búsqueda.
    private void onBuscar() {
        cargarOpciones();
    }

    @FXML
    // RF «Visualización de opciones de característica» · Excepción paso 6: «Limpiar filtro».
    private void onLimpiarFiltro() {
        txtBuscar.clear();
        cargarOpciones();
    }

    @FXML
    private void onReintentar() {
        alMostrar();
    }

    // ===================== Lista (Visualización de opciones) =====================

    // RF «Visualización de opciones de característica» · Pasos 2 a 6: consulta y despliega las opciones de la característica.
    private void cargarOpciones() {
        int idAntes = 0;
        if (seleccionada != null) {
            idAntes = seleccionada.getIdOpcion();
        }
        tblOpciones.getItems().clear();
        mostrarDetalle(null);
        ApoyoVista.mostrarNodo(lblAviso, false);
        lblConteo.setText("");

        Caracteristica caracteristica = cmbCaracteristica.getValue();
        if (caracteristica == null) {
            if (cmbCategoria.getValue() == null) {
                lblVacio.setText("No hay categorías registradas");
            } else {
                lblVacio.setText("Esta categoría no tiene características. Regístralas en la pestaña Características.");
            }
            return;
        }

        List<Opcion> opciones;
        List<Exclusion> exclusiones;
        try {
            // Paso 2: la característica sigue existiendo
            if (servicio.buscarCaracteristica(caracteristica.getIdCaracteristica()) == null) {
                // RF «Visualización de opciones de característica» · Excepción paso 2: «Característica no encontrada»; regresa al listado de características.
                Alertas.aviso(OpcionService.MSG_CARACTERISTICA_NO_ENCONTRADA);
                catalogo.setIdCaracteristicaActual(0);
                alMostrar();
                return;
            }
            opciones = servicio.listarPorCaracteristica(caracteristica.getIdCaracteristica());
            exclusiones = exclusionService.listarPorCategoria(caracteristica.getIdCategoria());
        } catch (DAOException e) {
            // RF «Visualización de opciones de característica» · Excepción paso 3: sin conexión.
            mostrarErrorConexion();
            return;
        }

        // RF «Visualización de opciones de característica» · Excepción paso 4: «Esta característica no tiene opciones registradas» (con «+ Nueva opción»).
        if (opciones.isEmpty()) {
            lblVacio.setText(OpcionService.MSG_SIN_OPCIONES);
            lblConteo.setText("0 opción(es)");
            return;
        }

        // Pasos 4 a 6: se quitan las incompletas y se aplica el criterio de búsqueda
        String criterio = "";
        if (txtBuscar.getText() != null) {
            criterio = txtBuscar.getText().trim().toLowerCase();
        }
        int incompletas = 0;
        for (Opcion opcion : opciones) {
            if (!servicio.datosCompletos(opcion)) {
                incompletas++;
            } else if (criterio.isEmpty() || opcion.getNombre().toLowerCase().contains(criterio)) {
                tblOpciones.getItems().add(new FilaOpcion(opcion, contarExclusiones(exclusiones, opcion)));
            }
        }
        // RF «Visualización de opciones de característica» · Excepción paso 4: «Algunas opciones no pudieron cargarse».
        if (incompletas > 0) {
            lblAviso.setText(OpcionService.MSG_INCOMPLETAS);
            ApoyoVista.mostrarNodo(lblAviso, true);
        }
        lblConteo.setText(tblOpciones.getItems().size() + " opción(es)");
        // RF «Visualización de opciones de característica» · Excepción paso 6: «Sin resultados para el criterio ingresado».
        if (tblOpciones.getItems().isEmpty() && hayFiltro()) {
            lblVacio.setText(OpcionService.MSG_SIN_RESULTADOS);
        }

        for (FilaOpcion fila : tblOpciones.getItems()) {
            if (fila.getOpcion().getIdOpcion() == idAntes) {
                tblOpciones.getSelectionModel().select(fila);
                mostrarSeleccionada();
            }
        }
    }

    private int contarExclusiones(List<Exclusion> exclusiones, Opcion opcion) {
        int total = 0;
        for (Exclusion exclusion : exclusiones) {
            if (exclusion.incluyeOpcion(opcion.getIdOpcion())) {
                total++;
            }
        }
        return total;
    }

    private boolean hayFiltro() {
        return txtBuscar.getText() != null && !txtBuscar.getText().isBlank();
    }

    private void mostrarErrorConexion() {
        tblOpciones.getItems().clear();
        mostrarDetalle(null);
        lblVacio.setText(OpcionService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
    }

    // ===================== Detalle =====================

    @FXML
    private void onClicTabla(MouseEvent evento) {
        boolean hay = mostrarSeleccionada();
        if (hay && evento.getClickCount() == 2 && puedeEditar) {
            onModificar();
        }
    }

    @FXML
    private void onTeclaTabla() {
        mostrarSeleccionada();
    }

    // Abre el apartado Recetas en la receta de la opción (o en el formulario para registrarla)
    @FXML
    private void onReceta() {
        if (seleccionada == null) {
            return;
        }
        RecetasController.abrirAlEntrar(seleccionada.getIdOpcion());
        Navegador.getInstancia().irASeccion(SeccionPanel.RECETAS);
    }

    private boolean mostrarSeleccionada() {
        FilaOpcion fila = tblOpciones.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return false;
        }
        if (seleccionada != null && seleccionada.getIdOpcion() == fila.getOpcion().getIdOpcion()) {
            return true;
        }
        mostrarDetalle(fila.getOpcion());
        return true;
    }

    private void mostrarDetalle(Opcion opcion) {
        seleccionada = opcion;
        ApoyoVista.mostrarNodo(cajaDetalle, opcion != null);
        ApoyoVista.mostrarNodo(cajaBotones, opcion != null);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, opcion == null);
        if (opcion == null) {
            return;
        }
        lblDetNombre.setText(opcion.getNombre());
        if (opcion.getDescripcion() == null || opcion.getDescripcion().isBlank()) {
            lblDetDescripcion.setText("Sin descripción");
        } else {
            lblDetDescripcion.setText(opcion.getDescripcion());
        }
        ApoyoVista.cargarImagen(imgOpcion, lblSinImagen, opcion.getLinkImagen());

        // El costo sale de la receta activa (Módulo 5); el precio base = costo × multiplicador de costo
        String costo = "Sin receta";
        String precio = "Sin receta (se calcula al registrar la receta)";
        if (opcion.isTieneReceta()) {
            costo = Formatos.moneda(opcion.getCostoReceta());
            precio = Formatos.moneda(opcion.getCostoReceta() * opcion.getMultiplicadorCosto());
        }
        lblDetValores.setText("Multiplicador de ingredientes: ×" + ValidacionCatalogo.numero(opcion.getMultiplicadorIngredientes())
                + "\nMultiplicador de costo: ×" + ValidacionCatalogo.numero(opcion.getMultiplicadorCosto())
                + "\nSe escala con el tamaño: " + ApoyoVista.siNo(opcion.isSeEscala())
                + "\nPuntos de selección: " + ValidacionCatalogo.numero(opcion.getPuntosSeleccion())
                + "\nMultiplicador de presupuesto: ×" + ValidacionCatalogo.numero(opcion.getMultiplicadorPresupuesto())
                + "\nDisponible: " + ApoyoVista.siNo(opcion.isDisponible())
                + "\nCosto de receta: " + costo
                + "\nPrecio base: " + precio);

        // Botón hacia Recetas: "Ver receta" o "Registrar receta" según tenga o no
        ApoyoVista.mostrarNodo(btnReceta, PermisosInventario.puedeVerRecetas());
        if (opcion.isTieneReceta()) {
            btnReceta.setText("Ver receta");
        } else {
            btnReceta.setText("Registrar receta");
        }

        listaExclusiones.getChildren().clear();
        List<Exclusion> exclusiones = new ArrayList<Exclusion>();
        try {
            exclusiones = servicio.listarExclusiones(opcion.getIdOpcion());
        } catch (DAOException e) {
            Label error = new Label(OpcionService.MSG_ERROR_CONEXION);
            error.getStyleClass().add("texto-suave");
            listaExclusiones.getChildren().add(error);
        }
        if (exclusiones.isEmpty()) {
            Label ninguna = new Label("Sin exclusiones");
            ninguna.getStyleClass().add("texto-suave");
            listaExclusiones.getChildren().add(ninguna);
        }
        for (Exclusion exclusion : exclusiones) {
            String otra = exclusion.getNombreCaracteristicaExcluida() + ": " + exclusion.getNombreOpcionExcluida();
            if (exclusion.getIdOpcionExcluida() == opcion.getIdOpcion()) {
                otra = exclusion.getNombreCaracteristica() + ": " + exclusion.getNombreOpcion();
            }
            Label linea = new Label("✕ " + otra + " (" + exclusion.getEstado().getValorBD() + ")");
            linea.setWrapText(true);
            linea.getStyleClass().add("funcion-item");
            listaExclusiones.getChildren().add(linea);
        }
    }

    @FXML
    // RF «Exclusiones entre opciones de características» · Paso 1: la Administradora pide administrar las exclusiones de la opción.
    private void onExclusiones() {
        if (seleccionada != null && cmbCategoria.getValue() != null) {
            catalogo.irAExclusiones(seleccionada, cmbCategoria.getValue().getIdCategoria());
        }
    }

    // ===================== Registro y modificación =====================

    @FXML
    // RF «Registro de opción de característica»: inicia aquí. Paso 1: «+ Nueva opción».
    private void onNueva() {
        Caracteristica caracteristica = cmbCaracteristica.getValue();
        if (caracteristica == null) {
            Alertas.info("Primero elige una característica");
            return;
        }
        // Paso 2: la característica sigue existiendo y disponible
        ResultadoCatalogo revision = servicio.revisarCaracteristicaParaAlta(caracteristica.getIdCaracteristica());
        // RF «Registro de opción de característica» · Excepción paso 2: la característica ya no está disponible o no hay conexión.
        if (!revision.isExitoso()) {
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                alMostrar();
            }
            return;
        }
        abrirFormulario(null, caracteristica);
    }

    @FXML
    // RF «Modificación de opción de característica»: inicia aquí. Paso 1: selecciona una opción y pide «Editar».
    private void onModificar() {
        if (seleccionada == null) {
            Alertas.info("Selecciona una opción de la lista");
            return;
        }
        Opcion opcion;
        Caracteristica caracteristica;
        // RF «Modificación de opción de característica» · Paso 2: valida que la opción exista y recupera sus datos.
        try {
            opcion = servicio.buscarPorId(seleccionada.getIdOpcion());
            caracteristica = servicio.buscarCaracteristica(seleccionada.getIdCaracteristica());
        } catch (DAOException e) {
            // RF «Modificación de opción de característica» · Excepción paso 2: «Error de conexión».
            Alertas.error(OpcionService.MSG_ERROR_CONEXION);
            return;
        }
        // RF «Modificación de opción de característica» · Excepción paso 2: «Opción no encontrada».
        if (opcion == null || caracteristica == null) {
            Alertas.aviso(OpcionService.MSG_NO_ENCONTRADA);
            cargarOpciones();
            return;
        }
        abrirFormulario(opcion, caracteristica);
    }

    private void abrirFormulario(Opcion opcion, Caracteristica caracteristica) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de opción");
            return;
        }
        FormularioOpcionController formulario = cargador.getController();
        String titulo = "Nueva opción";
        if (opcion == null) {
            formulario.prepararNueva(caracteristica);
        } else {
            formulario.prepararEdicion(opcion, caracteristica);
            titulo = "Modificar opción";
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, tblOpciones.getScene().getWindow(), titulo);

        // RF «Registro de opción de característica» · Paso 8 / RF «Modificación de opción de característica» · Paso 9: informa el éxito y actualiza el listado.
        if (formulario.seGuardo()) {
            Alertas.info(formulario.getMensajeExito());
        }
        if (formulario.seGuardo() || formulario.debeRecargarLista()) {
            seleccionada = null;
            cargarOpciones();
        }
    }

    // ===================== Baja (Eliminación de opción) =====================

    @FXML
    // RF «Eliminación de opción de característica»: inicia aquí. Paso 1: selecciona una opción y pide darla de baja.
    private void onDarDeBaja() {
        if (seleccionada == null) {
            Alertas.info("Selecciona una opción de la lista");
            return;
        }
        ResultadoCatalogo revision = servicio.prepararBaja(seleccionada.getIdOpcion());
        // RF «Eliminación de opción de característica» · Excepciones pasos 2 y 3: no existe, sin conexión o está en pedidos en curso.
        if (!revision.isExitoso()) {
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                cargarOpciones();
            }
            return;
        }
        // RF «Eliminación de opción de característica» · Pasos 5 y 6: confirmación doble; si cancela, la opción queda sin cambios.
        if (!Alertas.confirmarDoble("¿Dar de baja esta opción?", revision.getMensaje())) {
            return;
        }
        ResultadoCatalogo resultado = servicio.darDeBaja(seleccionada.getIdOpcion());
        // RF «Eliminación de opción de característica» · Paso 8: informa el éxito y actualiza el listado. Si falla, excepción paso 7.
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        seleccionada = null;
        cargarOpciones();
    }
}

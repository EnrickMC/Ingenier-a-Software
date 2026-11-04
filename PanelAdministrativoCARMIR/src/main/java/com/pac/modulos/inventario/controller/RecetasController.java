package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.modulos.inventario.model.Receta;
import com.pac.modulos.inventario.service.PermisosInventario;
import com.pac.modulos.inventario.service.RecetaService;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Apartado "Recetas" (recetas.fxml). Módulo 5.
 * Visualización (RF-55), registro (RF-56), modificación (RF-57) y baja (RF-58) de recetas.
 * Cada receta pertenece a una opción del catálogo y de ella sale el precio de esa opción.
 */
public class RecetasController implements ControladorSeccion {

    private static final String RUTA_FORMULARIO = "/com/pac/view/inventario/formulario-receta.fxml";

    // Opción que pidió abrir el catálogo ("Ver / registrar receta"); 0 = ninguna
    private static int opcionPendiente = 0;

    @FXML private Label lblModoConsulta;
    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivas;
    @FXML private Button btnNueva;

    @FXML private TableView<FilaReceta> tblRecetas;
    @FXML private Label lblConteo;
    @FXML private Label lblAviso;
    @FXML private Label lblVacio;

    @FXML private VBox cajaSinSeleccion;
    @FXML private ScrollPane cajaDetalle;
    @FXML private HBox cajaBotones;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetOpcion;
    @FXML private Label lblDetEstado;
    @FXML private Label lblDetTiempo;
    @FXML private Label lblDetAviso;
    @FXML private Label lblDetCosto;
    @FXML private VBox listaIngredientes;
    @FXML private Label lblDetPasos;
    @FXML private Button btnModificar;
    @FXML private Button btnDarDeBaja;
    @FXML private Button btnReactivar;

    private RecetaService servicio = new RecetaService();
    private FilaReceta seleccionada;
    private boolean puedeEditar;

    /**
     * El catálogo llama a este método antes de navegar a Recetas para que se abra
     * la receta de la opción (o el formulario para registrarla si no tiene).
     */
    public static void abrirAlEntrar(int idOpcion) {
        opcionPendiente = idOpcion;
    }

    @FXML
    private void initialize() {
        // Cada renglón se pinta según el estado de la receta
        // (JavaFX necesita la flecha "->" para indicar cómo crear cada renglón)
        tblRecetas.setRowFactory(tabla -> new RenglonReceta());
    }

    // El Navegador llama a este método al abrir el apartado
    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        puedeEditar = PermisosInventario.puedeEditarRecetas();
        ApoyoVista.mostrarNodo(lblModoConsulta, !puedeEditar);
        ApoyoVista.mostrarNodo(btnNueva, puedeEditar);
        cargarRecetas();

        int idOpcion = opcionPendiente;
        opcionPendiente = 0;
        if (idOpcion > 0) {
            abrirOpcion(idOpcion);
        }
    }

    // ===================== Lista (RF-55) =====================

    @FXML
    private void onBuscar() {
        cargarRecetas();
    }

    @FXML
    private void onLimpiarFiltro() {
        txtBuscar.clear();
        chkInactivas.setSelected(false);
        cargarRecetas();
    }

    @FXML
    private void onReintentar() {
        cargarRecetas();
    }

    // RF-55 (Visualización de recetas): inicia aquí en la pantalla. Pasos 1 a 3: carga la lista de opciones con receta.
    private void cargarRecetas() {
        int idAntes = 0;
        if (seleccionada != null) {
            idAntes = seleccionada.getReceta().getIdReceta();
        }
        tblRecetas.getItems().clear();
        mostrarDetalle(null);
        ApoyoVista.mostrarNodo(lblAviso, false);
        lblConteo.setText("");

        List<Receta> recetas;
        try {
            recetas = servicio.listar(chkInactivas.isSelected());
        } catch (DAOException e) {
            // RF-55 · Excepción paso 2: sin conexión, «Error de conexión» con «Reintentar».
            lblVacio.setText(RecetaService.MSG_ERROR_CONEXION);
            return;
        }

        int porRevisar = 0;
        for (Receta receta : recetas) {
            if (!servicio.coincide(receta, txtBuscar.getText())) {
                continue;
            }
            tblRecetas.getItems().add(new FilaReceta(receta));
            if (receta.isActiva() && receta.requiereRevision()) {
                porRevisar++;
            }
        }
        lblConteo.setText(tblRecetas.getItems().size() + " receta(s)");

        if (recetas.isEmpty()) {
            // RF-55 · Excepción paso 2: «Aún no hay recetas registradas».
            lblVacio.setText(RecetaService.MSG_SIN_RECETAS);
        } else if (tblRecetas.getItems().isEmpty()) {
            // RF-55 · Excepción paso 4: «No hay recetas que coincidan con la búsqueda».
            lblVacio.setText(RecetaService.MSG_SIN_RESULTADOS);
        }
        // RF-54 / RF-55: recetas con insumos dados de baja
        if (porRevisar > 0) {
            lblAviso.setText(porRevisar + " receta(s) requieren revisión: usan insumos que se dieron de baja."
                    + " Modifícalas para cambiar esos insumos.");
            ApoyoVista.mostrarNodo(lblAviso, true);
        }

        if (idAntes > 0) {
            seleccionarReceta(idAntes);
        }
    }

    private boolean hayFiltro() {
        return !txtBuscar.getText().isBlank() || chkInactivas.isSelected();
    }

    // Selecciona en la tabla la receta indicada y muestra su detalle; false si no está en la lista
    private boolean seleccionarReceta(int idReceta) {
        for (FilaReceta fila : tblRecetas.getItems()) {
            if (fila.getReceta().getIdReceta() == idReceta) {
                tblRecetas.getSelectionModel().select(fila);
                tblRecetas.scrollTo(fila);
                mostrarDetalle(fila);
                return true;
            }
        }
        return false;
    }

    @FXML
    private void onClicTabla() {
        FilaReceta fila = tblRecetas.getSelectionModel().getSelectedItem();
        if (fila != null && fila != seleccionada) {
            mostrarDetalle(fila);
        }
    }

    // ===================== Detalle (letra grande para la cocina) =====================

    // RF-55 · Pasos 5 y 6: valida la receta seleccionada y la muestra.
    private void mostrarDetalle(FilaReceta fila) {
        seleccionada = fila;
        ApoyoVista.mostrarNodo(cajaDetalle, fila != null);
        ApoyoVista.mostrarNodo(cajaBotones, fila != null && puedeEditar);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, fila == null);
        if (fila == null) {
            return;
        }

        // Se vuelve a leer con sus ingredientes (pudo cambiar desde que se listó)
        Receta receta;
        try {
            receta = servicio.buscarPorId(fila.getReceta().getIdReceta());
        } catch (DAOException e) {
            Alertas.error(RecetaService.MSG_ERROR_CONEXION);
            return;
        }
        if (receta == null) {
            // RF-55 · Excepción paso 5: «La receta ya no está disponible» y se recarga la lista.
            Alertas.aviso(RecetaService.MSG_NO_DISPONIBLE);
            seleccionada = null;
            cargarRecetas();
            return;
        }
        seleccionada = new FilaReceta(receta);

        lblDetNombre.setText(receta.getNombre());
        lblDetOpcion.setText(receta.getDescripcionOpcion());
        lblDetEstado.setText(seleccionada.getEstado());
        if (!receta.isActiva()) {
            EstilosInventario.aplicarChip(lblDetEstado, "chip-gris");
        } else if (receta.requiereRevision()) {
            EstilosInventario.aplicarChip(lblDetEstado, "chip-proximo");
        } else {
            EstilosInventario.aplicarChip(lblDetEstado, "chip-normal");
        }
        lblDetTiempo.setText(receta.getTiempoMinutos() + " min");

        // Avisos de la receta
        String aviso = null;
        if (!receta.isActiva()) {
            aviso = "Receta dada de baja: no descuenta insumos y la opción aparece «Sin receta» en el catálogo.";
        } else if (receta.requiereRevision()) {
            // RF-55 · Excepción paso 6: insumos dados de baja, «Esta receta requiere revisión».
            aviso = RecetaService.MSG_REQUIERE_REVISION + ": tiene insumos dados de baja (en rojo).";
        } else if (!receta.isOpcionDisponible()) {
            aviso = "La opción de esta receta ya no está disponible en el catálogo.";
        }
        lblDetAviso.setText(aviso);
        ApoyoVista.mostrarNodo(lblDetAviso, aviso != null);

        lblDetCosto.setText("Costo de la receta: " + Formatos.moneda(receta.getCosto())
                + "\nMultiplicador de la opción: ×" + Formatos.cantidad(receta.getMultiplicadorCosto())
                + "\nPrecio base en el catálogo: " + Formatos.moneda(receta.getPrecioBase()));

        listaIngredientes.getChildren().clear();
        for (IngredienteReceta ingrediente : receta.getIngredientes()) {
            String unidad = "";
            if (ingrediente.getUnidad() != null) {
                unidad = " " + ingrediente.getUnidad().getValorBD();
            }
            String texto = "•  " + Formatos.cantidad(ingrediente.getCantidad()) + unidad + "  " + ingrediente.getNombreInsumo();
            Label linea = new Label(texto);
            linea.setWrapText(true);
            if (ingrediente.isInsumoActivo()) {
                linea.getStyleClass().add("receta-ingrediente");
            } else {
                linea.setText(texto + "  (dado de baja)");
                linea.getStyleClass().add("receta-ingrediente-baja");
            }
            listaIngredientes.getChildren().add(linea);
        }
        lblDetPasos.setText(receta.getPasos());

        // Botones según el estado
        ApoyoVista.mostrarNodo(btnModificar, receta.isActiva());
        ApoyoVista.mostrarNodo(btnDarDeBaja, receta.isActiva());
        ApoyoVista.mostrarNodo(btnReactivar, !receta.isActiva());
    }

    // ===================== Abrir desde el catálogo =====================

    // Muestra la receta de la opción; si no tiene (o está de baja), ofrece registrarla
    private void abrirOpcion(int idOpcion) {
        Receta receta;
        try {
            receta = servicio.buscarPorOpcion(idOpcion);
        } catch (DAOException e) {
            Alertas.error(RecetaService.MSG_ERROR_CONEXION);
            return;
        }
        if (receta != null && receta.isActiva()) {
            txtBuscar.clear();
            if (!seleccionarReceta(receta.getIdReceta())) {
                cargarRecetas();
                seleccionarReceta(receta.getIdReceta());
            }
            return;
        }
        if (!puedeEditar) {
            Alertas.info("Esa opción aún no tiene receta. Solo la administradora puede registrarla.");
            return;
        }
        abrirFormularioNueva(idOpcion);
    }

    // ===================== Registro, modificación y baja =====================

    @FXML
    // RF-56 (Registro de nueva receta): inicia aquí. Paso 1: «+ Nueva receta».
    private void onNueva() {
        ResultadoInventario revision = servicio.revisarParaAlta();
        if (!revision.isExitoso()) {
            mostrarProblema(revision);
            return;
        }
        abrirFormularioNueva(0);
    }

    // Una receta dada de baja se vuelve a registrar con sus datos como punto de partida
    @FXML
    private void onReactivar() {
        if (seleccionada == null) {
            return;
        }
        abrirFormularioNueva(seleccionada.getReceta().getIdOpcion());
    }

    private void abrirFormularioNueva(int idOpcion) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de receta");
            return;
        }
        FormularioRecetaController formulario = cargador.getController();
        if (!formulario.prepararNueva(idOpcion)) {
            return;
        }
        mostrarFormulario(cargador, formulario, "Registrar receta");
    }

    @FXML
    // RF-57 (Modificación de receta): inicia aquí. Paso 1: se selecciona la receta y se pide «Editar receta».
    private void onModificar() {
        if (seleccionada == null) {
            return;
        }
        int idReceta = seleccionada.getReceta().getIdReceta();
        ResultadoInventario revision = servicio.revisarParaModificar(idReceta);
        if (!revision.isExitoso()) {
            mostrarProblema(revision);
            if (revision.isRecargarLista()) {
                seleccionada = null;
                cargarRecetas();
            }
            return;
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de receta");
            return;
        }
        FormularioRecetaController formulario = cargador.getController();
        if (!formulario.prepararEdicion(seleccionada.getReceta())) {
            return;
        }
        mostrarFormulario(cargador, formulario, "Modificar receta");
    }

    private void mostrarFormulario(FXMLLoader cargador, FormularioRecetaController formulario, String titulo) {
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, tblRecetas.getScene().getWindow(), titulo);
        if (formulario.seGuardo()) {
            // RF-56 / RF-57 · Paso 8: informa el éxito y actualiza la lista.
            Alertas.info(formulario.getMensajeExito());
            txtBuscar.clear();
            cargarRecetas();
            seleccionarReceta(formulario.getIdGuardado());
            return;
        }
        // La receta ya existía: se muestra la existente
        if (formulario.getIdExistente() > 0) {
            txtBuscar.clear();
            cargarRecetas();
            seleccionarReceta(formulario.getIdExistente());
            return;
        }
        if (formulario.debeRecargarLista()) {
            cargarRecetas();
        }
    }

    @FXML
    // RF-58 (Eliminación de receta): inicia aquí. Paso 1: se selecciona la receta y se pide darla de baja.
    private void onDarDeBaja() {
        if (seleccionada == null) {
            return;
        }
        int idReceta = seleccionada.getReceta().getIdReceta();
        ResultadoInventario revision = servicio.prepararBaja(idReceta);
        // RF-58 · Excepciones pasos 2 y 3: receta no disponible o necesaria para pedidos en curso.
        if (!revision.isExitoso()) {
            mostrarProblema(revision);
            if (revision.isRecargarLista()) {
                seleccionada = null;
                cargarRecetas();
            }
            return;
        }
        // RF-58: confirmación doble
        // RF-58 · Pasos 4 y 5: confirmación; si se cancela, la receta queda sin cambios.
        if (!Alertas.confirmarDoble("¿Dar de baja esta receta?", revision.getMensaje())) {
            return;
        }
        ResultadoInventario resultado = servicio.darDeBaja(idReceta);
        if (resultado.isExitoso()) {
            // RF-58 · Paso 7: informa el éxito.
            Alertas.info(resultado.getMensaje());
        } else {
            mostrarProblema(resultado);
        }
        seleccionada = null;
        cargarRecetas();
    }

    private void mostrarProblema(ResultadoInventario resultado) {
        if (ResultadoInventario.CAMPO_CONEXION.equals(resultado.getCampo())) {
            // RF-58 · Excepción paso 6: «No fue posible dar de baja la receta».
            Alertas.error(resultado.getMensaje());
        } else {
            Alertas.aviso(resultado.getMensaje());
        }
    }
}

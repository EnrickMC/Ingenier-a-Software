package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.service.CaracteristicaService;
import com.pac.modulos.catalogo.service.PermisosCatalogo;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pestaña Características (caracteristicas.fxml).
 * Visualización, registro, modificación, baja y orden de muestra (subir / bajar).
 */
public class CaracteristicasController {

    private static final String RUTA_FORMULARIO = "/com/pac/view/catalogo/formulario-caracteristica.fxml";

    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private Button btnNueva;

    @FXML private TableView<FilaCaracteristica> tblCaracteristicas;
    @FXML private Label lblConteo;
    @FXML private Label lblVacio;

    @FXML private VBox cajaDetalle;
    @FXML private VBox cajaSinSeleccion;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetDescripcion;
    @FXML private Label lblDetReglas;
    @FXML private VBox listaOpciones;
    @FXML private HBox cajaOrden;
    @FXML private Button btnModificar;
    @FXML private Button btnDarDeBaja;

    private CaracteristicaService servicio = new CaracteristicaService();
    private CatalogoController catalogo;
    private Caracteristica seleccionada;
    private boolean puedeEditar;
    // Evita recargar mientras se llena el selector de categorías
    private boolean llenandoSelector = false;

    public void iniciar(CatalogoController catalogo) {
        this.catalogo = catalogo;
        puedeEditar = PermisosCatalogo.puedeEditarCatalogo();
        ApoyoVista.mostrarNodo(btnNueva, puedeEditar);
        ApoyoVista.mostrarNodo(btnModificar, puedeEditar);
        ApoyoVista.mostrarNodo(btnDarDeBaja, puedeEditar);
        ApoyoVista.mostrarNodo(cajaOrden, puedeEditar);
    }

    // Se llama cada vez que la pestaña queda visible
    // RF «Visualización de características»: inicia aquí. Paso 1: se abre la pestaña Características (permiso revisado en el paso 2 al abrir el apartado).
    public void alMostrar() {
        llenandoSelector = true;
        boolean conectado = catalogo.llenarCategorias(cmbCategoria);
        llenandoSelector = false;
        // RF «Visualización de características» · Excepción paso 3: sin conexión, «Error de conexión» con «Reintentar».
        if (!conectado) {
            mostrarErrorConexion();
            return;
        }
        cargarCaracteristicas();
    }

    @FXML
    // RF «Visualización de características» · Paso 1: el usuario elige la categoría de producto.
    private void onCambioCategoria() {
        if (llenandoSelector) {
            return;
        }
        Categoria categoria = cmbCategoria.getValue();
        if (categoria != null) {
            catalogo.setIdCategoriaActual(categoria.getIdCategoria());
        }
        seleccionada = null;
        cargarCaracteristicas();
    }

    @FXML
    private void onReintentar() {
        alMostrar();
    }

    // ===================== Lista (Visualización de características) =====================

    // RF «Visualización de características» · Pasos 3 a 5: consulta y despliega las características de la categoría.
    private void cargarCaracteristicas() {
        int idAntes = catalogo.getIdCaracteristicaActual();
        tblCaracteristicas.getItems().clear();
        mostrarDetalle(null);
        lblConteo.setText("");

        Categoria categoria = cmbCategoria.getValue();
        // RF «Visualización de características» · Excepción paso 3: no hay categorías que elegir.
        if (categoria == null) {
            lblVacio.setText("No hay categorías registradas. Registra una en la pestaña Categorías.");
            return;
        }

        List<Caracteristica> lista;
        try {
            // Paso 3: la lista de categorías se acaba de leer de la BD, así que la categoría existe
            lista = servicio.listarPorCategoria(categoria.getIdCategoria());
        } catch (DAOException e) {
            // RF «Visualización de características» · Excepción paso 3: sin conexión.
            mostrarErrorConexion();
            return;
        }

        for (Caracteristica caracteristica : lista) {
            tblCaracteristicas.getItems().add(new FilaCaracteristica(caracteristica));
        }
        lblConteo.setText(lista.size() + " característica(s)");
        // RF «Visualización de características» · Excepción paso 4: «Esta categoría no tiene características registradas» (con «+ Nueva característica»).
        if (lista.isEmpty()) {
            lblVacio.setText(CaracteristicaService.MSG_SIN_CARACTERISTICAS);
        }

        // Se vuelve a seleccionar la característica que estaba elegida
        for (FilaCaracteristica fila : tblCaracteristicas.getItems()) {
            if (fila.getCaracteristica().getIdCaracteristica() == idAntes) {
                tblCaracteristicas.getSelectionModel().select(fila);
                mostrarSeleccionada();
            }
        }
    }

    private void mostrarErrorConexion() {
        tblCaracteristicas.getItems().clear();
        mostrarDetalle(null);
        lblVacio.setText(CaracteristicaService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
    }

    // ===================== Detalle =====================

    @FXML
    // RF «Visualización de características» · Paso 6: el usuario selecciona una característica para ver su detalle.
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

    // Paso 7: lee de nuevo la característica con sus opciones; false si no se pudo
    private boolean mostrarSeleccionada() {
        FilaCaracteristica fila = tblCaracteristicas.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return false;
        }
        Caracteristica caracteristica;
        try {
            caracteristica = servicio.buscarConOpciones(fila.getCaracteristica().getIdCaracteristica());
        } catch (DAOException e) {
            // RF «Visualización de características» · Excepción paso 7: sin conexión.
            Alertas.error(CaracteristicaService.MSG_ERROR_CONEXION);
            return false;
        }
        // RF «Visualización de características» · Excepción paso 7: «Característica no encontrada»; se recarga el listado.
        if (caracteristica == null) {
            Alertas.aviso(CaracteristicaService.MSG_NO_ENCONTRADA);
            cargarCaracteristicas();
            return false;
        }
        mostrarDetalle(caracteristica);
        return true;
    }

    private void mostrarDetalle(Caracteristica caracteristica) {
        seleccionada = caracteristica;
        ApoyoVista.mostrarNodo(cajaDetalle, caracteristica != null);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, caracteristica == null);
        if (caracteristica == null) {
            return;
        }
        catalogo.setIdCaracteristicaActual(caracteristica.getIdCaracteristica());
        lblDetNombre.setText(caracteristica.getNombre());
        if (caracteristica.getDescripcion() == null || caracteristica.getDescripcion().isBlank()) {
            lblDetDescripcion.setText("Sin descripción");
        } else {
            lblDetDescripcion.setText(caracteristica.getDescripcion());
        }
        lblDetReglas.setText(textoReglas(caracteristica));

        listaOpciones.getChildren().clear();
        if (caracteristica.getOpciones().isEmpty()) {
            Label sinOpciones = new Label("Sin opciones registradas");
            sinOpciones.getStyleClass().add("texto-suave");
            listaOpciones.getChildren().add(sinOpciones);
        }
        for (Opcion opcion : caracteristica.getOpciones()) {
            String texto = "• " + opcion.getNombre();
            if (caracteristica.isSeleccionMultiple()) {
                texto = texto + " (" + ValidacionCatalogo.numero(opcion.getPuntosSeleccion()) + " pts)";
            }
            if (!opcion.isDisponible()) {
                texto = texto + " · no disponible";
            }
            Label linea = new Label(texto);
            linea.getStyleClass().add("funcion-item");
            listaOpciones.getChildren().add(linea);
        }
    }

    // Explica en palabras cómo elige el cliente
    public static String textoReglas(Caracteristica c) {
        String texto = "Orden de muestra: " + c.getOrdenMostrar();
        if (c.isObligatoria()) {
            texto = texto + "\nObligatoria: el cliente debe elegir al menos una opción.";
        } else {
            texto = texto + "\nOpcional: el cliente puede no elegir nada.";
        }
        if (c.isSeleccionMultiple()) {
            texto = texto + "\nSelección múltiple con presupuesto de "
                    + ValidacionCatalogo.numero(c.getPresupuestoSeleccion()) + " puntos.";
        } else {
            texto = texto + "\nSelección única: solo una opción.";
        }
        if (!c.isDisponible()) {
            texto = texto + "\nNo disponible: no se muestra al cliente.";
        }
        return texto;
    }

    @FXML
    private void onVerOpciones() {
        if (seleccionada != null) {
            catalogo.irAOpciones(seleccionada);
        }
    }

    // ===================== Orden (subir / bajar) =====================

    @FXML
    private void onSubir() {
        mover(-1);
    }

    @FXML
    private void onBajar() {
        mover(1);
    }

    // Intercambia el orden con la característica de arriba (-1) o de abajo (+1)
    private void mover(int direccion) {
        int indice = tblCaracteristicas.getSelectionModel().getSelectedIndex();
        int vecino = indice + direccion;
        if (indice < 0 || vecino < 0 || vecino >= tblCaracteristicas.getItems().size()) {
            return;
        }
        Caracteristica actual = tblCaracteristicas.getItems().get(indice).getCaracteristica();
        Caracteristica otra = tblCaracteristicas.getItems().get(vecino).getCaracteristica();
        ResultadoCatalogo resultado = servicio.mover(actual, otra);
        if (!resultado.isExitoso()) {
            Alertas.error(resultado.getMensaje());
        }
        catalogo.setIdCaracteristicaActual(actual.getIdCaracteristica());
        cargarCaracteristicas();
    }

    // ===================== Registro y modificación =====================

    @FXML
    // RF «Registro de característica»: inicia aquí. Paso 1: «+ Nueva característica».
    private void onNueva() {
        Categoria categoria = cmbCategoria.getValue();
        if (categoria == null) {
            Alertas.info("Primero elige o registra una categoría");
            return;
        }
        // Paso 2: la categoría sigue existiendo y activa
        ResultadoCatalogo revision = servicio.revisarCategoriaParaAlta(categoria.getIdCategoria());
        // RF «Registro de característica» · Excepción paso 2: la categoría ya no está disponible o no hay conexión.
        if (!revision.isExitoso()) {
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                alMostrar();
            }
            return;
        }
        int orden;
        try {
            orden = servicio.siguienteOrden(categoria.getIdCategoria());
        } catch (DAOException e) {
            // RF «Registro de característica» · Excepción paso 2: «Error de conexión»; no abre el formulario.
            Alertas.error(CaracteristicaService.MSG_ERROR_CONEXION);
            return;
        }
        abrirFormulario(null, categoria, orden);
    }

    @FXML
    // RF «Modificación de característica»: inicia aquí. Paso 1: selecciona una característica y pide «Editar».
    private void onModificar() {
        if (seleccionada == null) {
            Alertas.info("Selecciona una característica de la lista");
            return;
        }
        Caracteristica caracteristica;
        // RF «Modificación de característica» · Paso 2: valida que la característica exista y recupera sus datos y opciones.
        try {
            caracteristica = servicio.buscarConOpciones(seleccionada.getIdCaracteristica());
        } catch (DAOException e) {
            // RF «Modificación de característica» · Excepción paso 2: «Error de conexión».
            Alertas.error(CaracteristicaService.MSG_ERROR_CONEXION);
            return;
        }
        // RF «Modificación de característica» · Excepción paso 2: «Característica no encontrada».
        if (caracteristica == null) {
            Alertas.aviso(CaracteristicaService.MSG_NO_ENCONTRADA);
            cargarCaracteristicas();
            return;
        }
        abrirFormulario(caracteristica, cmbCategoria.getValue(), caracteristica.getOrdenMostrar());
    }

    private void abrirFormulario(Caracteristica caracteristica, Categoria categoria, int orden) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de característica");
            return;
        }
        FormularioCaracteristicaController formulario = cargador.getController();
        String titulo = "Nueva característica";
        if (caracteristica == null) {
            formulario.prepararNueva(categoria, orden);
        } else {
            formulario.prepararEdicion(caracteristica, categoria);
            titulo = "Modificar característica";
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, tblCaracteristicas.getScene().getWindow(), titulo);

        // RF «Registro de característica» · Paso 8 / RF «Modificación de característica» · Paso 9: cierra el formulario, informa el éxito y actualiza el listado.
        if (formulario.seGuardo()) {
            Alertas.info(formulario.getMensajeExito());
        }
        if (formulario.seGuardo() || formulario.debeRecargarLista()) {
            cargarCaracteristicas();
        }
    }

    // ===================== Baja (Eliminación de característica) =====================

    @FXML
    // RF «Eliminación de característica»: inicia aquí. Paso 1: selecciona una característica y pide darla de baja.
    private void onDarDeBaja() {
        if (seleccionada == null) {
            Alertas.info("Selecciona una característica de la lista");
            return;
        }
        ResultadoCatalogo revision = servicio.prepararBaja(seleccionada.getIdCaracteristica());
        // RF «Eliminación de característica» · Excepciones pasos 2 y 3: no existe, sin conexión o está en pedidos en curso.
        if (!revision.isExitoso()) {
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                cargarCaracteristicas();
            }
            return;
        }
        // RF «Eliminación de característica» · Pasos 4 y 5: confirmación doble; si cancela, la característica queda sin cambios.
        if (!Alertas.confirmarDoble("¿Dar de baja esta característica?", revision.getMensaje())) {
            return;
        }
        ResultadoCatalogo resultado = servicio.darDeBaja(seleccionada.getIdCaracteristica());
        // RF «Eliminación de característica» · Paso 7: informa el éxito y actualiza el listado. Si falla, excepción paso 6.
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        cargarCaracteristicas();
    }
}

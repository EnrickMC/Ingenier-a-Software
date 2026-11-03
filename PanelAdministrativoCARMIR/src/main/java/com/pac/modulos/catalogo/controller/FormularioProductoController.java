package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.model.Producto;
import com.pac.modulos.catalogo.service.ConfiguracionPostre;
import com.pac.modulos.catalogo.service.ProductoService;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana "Nuevo Postre" / "Editar detalles del producto" (formulario-producto.fxml).
 * RF-18 (registro) y RF-19 (modificación).
 *
 * Por cada característica de la categoría (en su orden de muestra) se arman los controles
 * para elegir sus opciones. Cada cambio recalcula el precio, el costo y los avisos con
 * ConfiguracionPostre (obligatorias, presupuesto y exclusiones).
 */
public class FormularioProductoController {

    @FXML private Label lblTitulo;
    @FXML private Label lblSku;
    @FXML private Label lblPrecio;
    @FXML private Label lblCosto;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtFoto;
    @FXML private ImageView imgFoto;
    @FXML private Label lblSinImagen;
    @FXML private CheckBox chkMostrar;
    @FXML private VBox contenedorCaracteristicas;
    @FXML private VBox listaAvisos;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private ProductoService servicio = new ProductoService();

    // Producto que se modifica (null = registro nuevo)
    private Producto editado;
    // Reglas de la categoría elegida
    private ConfiguracionPostre configuracion;
    // Un selector por cada opción mostrada
    private List<SelectorOpcion> selectores = new ArrayList<SelectorOpcion>();
    // Evita reconstruir mientras se llena el selector de categorías
    private boolean preparando = false;

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";
    private int idParaAbrir = 0;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblMensaje, false);
        ApoyoVista.cargarImagen(imgFoto, lblSinImagen, null);
    }

    // ===================== Preparar la ventana =====================

    // Formulario en blanco (paso 3 de RF-18)
    // RF-18 · Paso 3: despliega el formulario de registro.
    public void prepararNuevo(List<Categoria> activas) {
        editado = null;
        lblTitulo.setText("Nuevo Postre");
        lblSku.setText("El SKU se genera al guardar.");
        preparando = true;
        cmbCategoria.getItems().setAll(activas);
        cmbCategoria.setValue(activas.get(0));
        preparando = false;
        construirOpciones(null);
    }

    // Formulario con los datos actuales (paso 3 de RF-19)
    // RF-19 · Paso 3: formulario con los datos actuales.
    public void prepararEdicion(Producto producto, List<Categoria> activas) {
        editado = producto;
        lblTitulo.setText("Editar detalles del producto");
        lblSku.setText("SKU: " + producto.getSku());
        txtNombre.setText(ApoyoVista.texto(producto.getNombre()));
        txtDescripcion.setText(ApoyoVista.texto(producto.getDescripcion()));
        txtFoto.setText(ApoyoVista.texto(producto.getLinkFotografia()));
        chkMostrar.setSelected(producto.isMostrarEnTienda());
        ApoyoVista.cargarImagen(imgFoto, lblSinImagen, producto.getLinkFotografia());

        preparando = true;
        cmbCategoria.getItems().setAll(activas);
        Categoria suya = null;
        for (Categoria categoria : activas) {
            if (categoria.getIdCategoria() == producto.getIdCategoria()) {
                suya = categoria;
            }
        }
        // Si su categoría ya está inactiva, se agrega para que se vea (al guardar se avisará)
        if (suya == null) {
            suya = new Categoria();
            suya.setIdCategoria(producto.getIdCategoria());
            suya.setNombre(producto.getNombreCategoria());
            suya.setEstado(EstadoRegistro.INACTIVA);
            cmbCategoria.getItems().add(suya);
        }
        cmbCategoria.setValue(suya);
        preparando = false;
        construirOpciones(producto.getOpciones());
    }

    // Al cambiar de categoría se vuelven a armar las opciones (las elegidas se pierden)
    @FXML
    private void onCambioCategoria() {
        if (preparando) {
            return;
        }
        construirOpciones(null);
    }

    // ===================== Controles de las opciones =====================

    // Arma los controles de cada característica; "elegidas" marca las que ya tiene el producto
    // RF-18 / RF-19 · Paso 4: características de la categoría con sus opciones para marcar.
    private void construirOpciones(List<OpcionElegida> elegidas) {
        contenedorCaracteristicas.getChildren().clear();
        selectores.clear();
        configuracion = null;

        Categoria categoria = cmbCategoria.getValue();
        if (categoria == null) {
            return;
        }
        try {
            configuracion = servicio.cargarConfiguracion(categoria.getIdCategoria());
        } catch (DAOException e) {
            Label error = new Label(ProductoService.MSG_ERROR_CONEXION);
            error.getStyleClass().add("mensaje-error");
            contenedorCaracteristicas.getChildren().add(error);
            return;
        }

        if (configuracion.getCaracteristicas().isEmpty()) {
            Label vacio = new Label("Esta categoría no tiene características. Regístralas en la pestaña Características.");
            vacio.setWrapText(true);
            vacio.getStyleClass().add("texto-suave");
            contenedorCaracteristicas.getChildren().add(vacio);
        }

        for (Caracteristica caracteristica : configuracion.getCaracteristicas()) {
            contenedorCaracteristicas.getChildren().add(crearCaja(caracteristica, elegidas));
        }
        recalcular();
    }

    // Caja de una característica: título, regla y sus opciones
    private VBox crearCaja(Caracteristica caracteristica, List<OpcionElegida> elegidas) {
        VBox caja = new VBox(6);
        caja.getStyleClass().add("caja-caracteristica");

        String titulo = caracteristica.getNombre();
        if (caracteristica.isObligatoria()) {
            titulo = titulo + " *";
        }
        Label lblTituloCaja = new Label(titulo);
        lblTituloCaja.getStyleClass().add("formulario-etiqueta");

        String regla;
        if (caracteristica.isSeleccionMultiple()) {
            regla = "Selección múltiple · presupuesto de " + ValidacionCatalogo.numero(caracteristica.getPresupuestoSeleccion())
                    + " puntos";
        } else {
            regla = "Selección única";
        }
        if (!caracteristica.isDisponible()) {
            regla = regla + " · NO DISPONIBLE";
        }
        Label lblRegla = new Label(regla);
        lblRegla.getStyleClass().add("texto-ayuda");
        caja.getChildren().addAll(lblTituloCaja, lblRegla);

        FlowPane opciones = new FlowPane(14, 6);
        ToggleGroup grupo = new ToggleGroup();

        // En selección única no obligatoria se puede dejar sin elegir
        if (!caracteristica.isSeleccionMultiple() && !caracteristica.isObligatoria()) {
            RadioButton ninguna = new RadioButton("Ninguna");
            ninguna.setToggleGroup(grupo);
            ninguna.setSelected(true);
            // Flecha necesaria: JavaFX pide una acción para el clic del control
            ninguna.setOnAction(evento -> recalcular());
            opciones.getChildren().add(ninguna);
        }

        for (Opcion opcion : caracteristica.getOpciones()) {
            int vecesElegida = buscarCantidad(elegidas, opcion.getIdOpcion());
            boolean noDisponible = !opcion.isDisponible() || !caracteristica.isDisponible();
            // Las opciones no disponibles solo se muestran si el producto ya las tenía
            if (noDisponible && vecesElegida == 0) {
                continue;
            }
            String texto = opcion.getNombre();
            if (caracteristica.isSeleccionMultiple()) {
                texto = texto + " (" + ValidacionCatalogo.numero(opcion.getPuntosSeleccion()) + " pts)";
            }
            if (noDisponible) {
                texto = texto + " · no disponible";
            }

            if (caracteristica.isSeleccionMultiple()) {
                CheckBox casilla = new CheckBox(texto);
                Spinner<Integer> cantidad = new Spinner<Integer>(1, 20, 1);
                cantidad.setPrefWidth(70);
                cantidad.setDisable(true);
                // Flechas necesarias: JavaFX avisa con un "listener" cuando cambia el valor
                casilla.setOnAction(evento -> {
                    cantidad.setDisable(!casilla.isSelected());
                    recalcular();
                });
                cantidad.valueProperty().addListener((observable, antes, ahora) -> recalcular());
                HBox fila = new HBox(6, casilla, cantidad);
                fila.setAlignment(Pos.CENTER_LEFT);
                opciones.getChildren().add(fila);
                SelectorOpcion selector = new SelectorOpcion(opcion, casilla, cantidad);
                selectores.add(selector);
                if (vecesElegida > 0) {
                    selector.elegir(vecesElegida);
                }
            } else {
                RadioButton radio = new RadioButton(texto);
                radio.setToggleGroup(grupo);
                // Flecha necesaria: JavaFX pide una acción para el clic del control
                radio.setOnAction(evento -> recalcular());
                opciones.getChildren().add(radio);
                SelectorOpcion selector = new SelectorOpcion(opcion, radio);
                selectores.add(selector);
                if (vecesElegida > 0) {
                    selector.elegir(1);
                }
            }
        }

        if (opciones.getChildren().isEmpty()) {
            Label sinOpciones = new Label("Sin opciones disponibles");
            sinOpciones.getStyleClass().add("texto-suave");
            opciones.getChildren().add(sinOpciones);
        }
        caja.getChildren().add(opciones);
        return caja;
    }

    // Cantidad con que el producto ya tenía una opción (0 = no la tenía)
    private int buscarCantidad(List<OpcionElegida> elegidas, int idOpcion) {
        if (elegidas == null) {
            return 0;
        }
        for (OpcionElegida elegida : elegidas) {
            if (elegida.getOpcion().getIdOpcion() == idOpcion) {
                return elegida.getCantidad();
            }
        }
        return 0;
    }

    // Lista de opciones marcadas en el formulario
    private List<OpcionElegida> leerElegidas() {
        List<OpcionElegida> elegidas = new ArrayList<OpcionElegida>();
        for (SelectorOpcion selector : selectores) {
            if (selector.estaElegida()) {
                elegidas.add(new OpcionElegida(selector.getOpcion(), selector.getCantidad()));
            }
        }
        return elegidas;
    }

    // Precio, costo y avisos en vivo
    // RF-18 / RF-19: precio, costo y avisos de reglas en vivo.
    private void recalcular() {
        List<OpcionElegida> elegidas = leerElegidas();
        lblPrecio.setText(Formatos.moneda(ConfiguracionPostre.calcularPrecio(elegidas)));
        lblCosto.setText("Costo: " + Formatos.moneda(ConfiguracionPostre.calcularCosto(elegidas)));

        listaAvisos.getChildren().clear();
        if (configuracion == null) {
            return;
        }
        for (String error : configuracion.validar(elegidas)) {
            Label linea = new Label("• " + error);
            linea.setWrapText(true);
            linea.getStyleClass().add("aviso-error");
            listaAvisos.getChildren().add(linea);
        }
        List<String> sinReceta = ConfiguracionPostre.opcionesSinReceta(elegidas);
        if (!sinReceta.isEmpty()) {
            Label linea = new Label("• Sin receta todavía (su costo cuenta como $0): " + ValidacionCatalogo.unir(sinReceta));
            linea.setWrapText(true);
            linea.getStyleClass().add("aviso-info");
            listaAvisos.getChildren().add(linea);
        }
    }

    // ===================== Fotografía =====================

    // Botón "Ver": muestra la vista previa (si el enlace tiene un formato permitido)
    @FXML
    private void onVerFoto() {
        ApoyoVista.desmarcar(txtFoto);
        String enlace = txtFoto.getText();
        if (enlace == null || enlace.isBlank()) {
            ApoyoVista.cargarImagen(imgFoto, lblSinImagen, null);
            return;
        }
        // RF-18 · Excepción paso 5: «Formato o tamaño de imagen no permitido».
        if (!ValidacionCatalogo.imagenValida(enlace)) {
            ApoyoVista.marcar(txtFoto);
            mostrarError(ProductoService.MSG_IMAGEN_NO_PERMITIDA);
            return;
        }
        ApoyoVista.cargarImagen(imgFoto, lblSinImagen, enlace);
    }

    // Botón "Elegir": abre el explorador de archivos, escribe la ruta y muestra la vista previa
    @FXML
    private void onElegirFoto() {
        if (ApoyoVista.elegirImagen(txtFoto)) {
            onVerFoto();
        }
    }

    // ===================== Guardar =====================

    @FXML
    // RF-18 / RF-19 · Paso 4: la Administradora eligió «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        Producto datos = new Producto();
        if (editado != null) {
            datos.setIdPostre(editado.getIdPostre());
        }
        if (cmbCategoria.getValue() != null) {
            datos.setIdCategoria(cmbCategoria.getValue().getIdCategoria());
        }
        datos.setNombre(txtNombre.getText());
        datos.setDescripcion(txtDescripcion.getText());
        datos.setLinkFotografia(txtFoto.getText());
        datos.setMostrarEnTienda(chkMostrar.isSelected());
        datos.setOpciones(leerElegidas());

        ResultadoCatalogo resultado;
        // RF-18 · Pasos 5 a 7 (registro) o RF-19 · Pasos 5 a 8 (modificación) en ProductoService.
        if (editado == null) {
            resultado = servicio.registrar(datos);
        } else {
            resultado = servicio.modificar(datos, false);
            // Paso 7: cambia el precio y hay pedidos vigentes
            if (resultado.isPideConfirmacion()) {
                // RF-19 · Excepción paso 7: no confirmó el cambio de precio; regresa al formulario.
                if (!Alertas.confirmar("Cambio de precio", resultado.getMensaje())) {
                    return;
                }
                resultado = servicio.modificar(datos, true);
            }
        }

        if (resultado.isExitoso()) {
            guardado = true;
            mensajeExito = resultado.getMensaje();
            cerrar();
            return;
        }
        if (resultado.isRecargarLista()) {
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }
        // Ya existe un producto con la misma configuración: se ofrece abrirlo
        if (resultado.isOfrecerAbrir()) {
            boolean abrir = Alertas.confirmar("Configuración repetida", resultado.getMensaje()
                    + "\n\n¿Quieres abrir ese producto? (Se descartan los cambios de esta ventana)");
            if (abrir) {
                idParaAbrir = resultado.getIdRelacionado();
                cerrar();
                return;
            }
        }

        mostrarError(resultado.getMensaje());
        Node campo = campoDe(resultado.getCampo());
        if (campo != null) {
            ApoyoVista.marcar(campo);
        }
        // La categoría ya no está disponible: se recarga la lista de categorías activas
        // RF-18 / RF-19 · Excepción paso 6: la categoría ya no está disponible; se recarga la lista.
        if (ResultadoCatalogo.CAMPO_CATEGORIA.equals(resultado.getCampo())) {
            recargarCategorias();
        }
    }

    private void recargarCategorias() {
        try {
            List<Categoria> activas = servicio.listarCategoriasActivas();
            Categoria antes = cmbCategoria.getValue();
            preparando = true;
            cmbCategoria.getItems().setAll(activas);
            if (antes != null && cmbCategoria.getItems().contains(antes)) {
                cmbCategoria.setValue(antes);
            } else {
                cmbCategoria.setValue(null);
            }
            preparando = false;
        } catch (DAOException e) {
            preparando = false;
        }
    }

    private Node campoDe(String campo) {
        if (ResultadoCatalogo.CAMPO_NOMBRE.equals(campo)) {
            return txtNombre;
        }
        if (ResultadoCatalogo.CAMPO_DESCRIPCION.equals(campo)) {
            return txtDescripcion;
        }
        if (ResultadoCatalogo.CAMPO_FOTO.equals(campo)) {
            return txtFoto;
        }
        if (ResultadoCatalogo.CAMPO_CATEGORIA.equals(campo)) {
            return cmbCategoria;
        }
        if (ResultadoCatalogo.CAMPO_OPCIONES.equals(campo)) {
            return contenedorCaracteristicas;
        }
        return null;
    }

    @FXML
    // RF-18 / RF-19 · Excepción paso 4: «Cancelar» descarta los datos.
    private void onCancelar() {
        cerrar();
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        ApoyoVista.mostrarNodo(lblMensaje, true);
    }

    private void quitarMarcas() {
        Node[] campos = {txtNombre, txtDescripcion, txtFoto, cmbCategoria, contenedorCaracteristicas};
        for (Node campo : campos) {
            ApoyoVista.desmarcar(campo);
        }
    }

    private void cerrar() {
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.close();
    }

    // ===================== Lo que lee ProductosController al cerrar =====================

    public boolean seGuardo() {
        return guardado;
    }

    public boolean debeRecargarLista() {
        return recargarLista;
    }

    public String getMensajeExito() {
        return mensajeExito;
    }

    // Id del producto con la misma configuración que se pidió abrir (0 = ninguno)
    public int getIdParaAbrir() {
        return idParaAbrir;
    }
}

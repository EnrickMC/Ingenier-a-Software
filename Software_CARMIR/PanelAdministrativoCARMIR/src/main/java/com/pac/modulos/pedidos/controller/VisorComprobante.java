package com.pac.modulos.pedidos.controller;

import com.pac.CarmirApp;
import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.pedidos.model.ArchivoComprobante;
import com.pac.modulos.pedidos.model.Pago;
import com.pac.modulos.pedidos.service.PagoService;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * RF-50 (Visualización del comprobante de pago): muestra dentro del panel la foto o el PDF del comprobante
 * que el cliente subió desde la página web (RF-14), y el botón para abrir el enlace si escribió uno.
 *
 * Se usa en la validación de un pago (formulario-pago.fxml) y en la ventana «Ver comprobante»
 * de «Validación de pagos» y de la ficha del pedido.
 */
public class VisorComprobante extends VBox {

    public static final String MSG_SIN_COMPROBANTE = "Este pago no tiene comprobante";
    public static final String MSG_ERROR_LECTURA = "Error de conexión: no fue posible leer el comprobante";
    public static final String MSG_YA_NO_EXISTE = "El comprobante ya no está disponible";
    public static final String MSG_SIN_VISTA = "No se pudo mostrar la vista previa. Usa «Abrir en el equipo» para verlo.";

    private final Label lblDatos = new Label();
    private final ImageView vista = new ImageView();
    private final Label lblSinVista = new Label();
    private final Button btnGrande = new Button("Ver en grande");
    private final Button btnEquipo = new Button("Abrir en el equipo");
    private final Button btnEnlace = new Button("Abrir enlace");

    private PagoService servicio = new PagoService();
    private Pago pago;
    private ArchivoComprobante archivo;
    private Image imagen;

    public VisorComprobante() {
        setSpacing(8);

        lblDatos.getStyleClass().add("texto-suave");
        lblDatos.setWrapText(true);
        lblDatos.setMaxWidth(Double.MAX_VALUE);

        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        vista.setCursor(Cursor.HAND);
        lblSinVista.getStyleClass().add("texto-suave");
        lblSinVista.setWrapText(true);
        lblSinVista.setTextAlignment(TextAlignment.CENTER);
        lblSinVista.setAlignment(Pos.CENTER);
        StackPane marco = new StackPane(vista, lblSinVista);
        marco.getStyleClass().add("caja-imagen");
        marco.setAlignment(Pos.CENTER);
        marco.setMinHeight(160);
        setTamanoVista(500, 320);

        btnGrande.getStyleClass().add("btn-azul");
        btnGrande.setGraphic(new FontIcon("bi-arrows-fullscreen"));
        btnEquipo.getStyleClass().add("btn-oscuro");
        btnEquipo.setGraphic(new FontIcon("bi-box-arrow-up-right"));
        btnEnlace.getStyleClass().add("btn-oscuro");
        btnEnlace.setGraphic(new FontIcon("bi-link-45deg"));
        // Flecha necesaria: JavaFX pide una acción para el clic de cada botón
        btnGrande.setOnAction(evento -> verEnGrande());
        btnEquipo.setOnAction(evento -> abrirEnElEquipo());
        btnEnlace.setOnAction(evento -> abrirEnlace());
        // Flecha necesaria: JavaFX pide una acción para el clic sobre la vista previa
        vista.setOnMouseClicked(evento -> verEnGrande());

        FlowPane botones = new FlowPane(8, 8, btnGrande, btnEquipo, btnEnlace);
        getChildren().addAll(lblDatos, marco, botones);
        mostrar(null);
    }

    // Tamaño máximo de la vista previa
    public void setTamanoVista(double ancho, double alto) {
        vista.setFitWidth(ancho);
        vista.setFitHeight(alto);
        lblSinVista.setMaxWidth(ancho - 20);
    }

    // RF-50: inicia aquí. Paso 1: se eligió un pago (o se abrió su validación) y se muestra su comprobante.
    public void mostrar(Pago elegido) {
        pago = elegido;
        archivo = null;
        imagen = null;
        vista.setImage(null);
        lblDatos.setText("");
        if (pago == null) {
            sinVista("Elige un pago para ver su comprobante");
            actualizarBotones();
            return;
        }
        if (pago.tieneArchivo()) {
            mostrarArchivo();
        } else if (pago.tieneEnlace()) {
            lblDatos.setText("El comprobante está en un enlace: " + pago.getLinkComprobante());
            sinVista("Usa «Abrir enlace» para verlo en el navegador.");
        } else {
            // RF-50 · Excepción paso 1: el pago no tiene comprobante (por ejemplo, efectivo en la tienda).
            sinVista(MSG_SIN_COMPROBANTE);
        }
        actualizarBotones();
    }

    private void mostrarArchivo() {
        // RF-50 · Paso 2: lee la foto o el PDF guardado con el pago.
        try {
            archivo = servicio.leerArchivo(pago.getIdPago());
        } catch (DAOException e) {
            // RF-50 · Excepción paso 2: «Error de conexión: no fue posible leer el comprobante».
            sinVista(MSG_ERROR_LECTURA);
            return;
        }
        if (archivo == null) {
            // RF-50 · Excepción paso 2: el pago se borró mientras se veía la lista.
            sinVista(MSG_YA_NO_EXISTE);
            return;
        }
        // RF-50 · Paso 3: datos del archivo y vista previa (de un PDF se muestra la primera página).
        String datos = archivo.getNombreArchivo() + " · " + archivo.getTipoCorto() + " · "
                + textoTamano(archivo.getTamanoBytes());
        if (archivo.getFechaSubida() != null) {
            datos = datos + " · subido el " + Formatos.fechaHoraCorta(archivo.getFechaSubida());
        }
        int paginas = ImagenComprobante.contarPaginas(archivo);
        if (paginas > 1) {
            datos = datos + "\nTiene " + paginas + " páginas: aquí se ve la primera; ábrelo en el equipo para ver las demás.";
        }
        if (pago.tieneEnlace()) {
            datos = datos + "\nTambién envió un enlace: " + pago.getLinkComprobante();
        }
        lblDatos.setText(datos);

        imagen = ImagenComprobante.crear(archivo);
        if (imagen == null) {
            // RF-50 · Excepción paso 3: el archivo no se pudo dibujar; aún se puede abrir en el equipo.
            sinVista(MSG_SIN_VISTA);
            return;
        }
        vista.setImage(imagen);
        ApoyoVista.mostrarNodo(vista, true);
        ApoyoVista.mostrarNodo(lblSinVista, false);
    }

    private void sinVista(String texto) {
        lblSinVista.setText(texto);
        ApoyoVista.mostrarNodo(vista, false);
        ApoyoVista.mostrarNodo(lblSinVista, true);
    }

    private void actualizarBotones() {
        ApoyoVista.mostrarNodo(lblDatos, !lblDatos.getText().isEmpty());
        ApoyoVista.mostrarNodo(btnGrande, imagen != null);
        ApoyoVista.mostrarNodo(btnEquipo, archivo != null);
        ApoyoVista.mostrarNodo(btnEnlace, pago != null && pago.tieneEnlace());
    }

    // ===================== Acciones =====================

    // RF-50 · Pasos 4 y 5: la imagen en una ventana del tamaño de la pantalla.
    private void verEnGrande() {
        if (imagen == null) {
            return;
        }
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        ImageView grande = new ImageView(imagen);
        grande.setPreserveRatio(true);
        grande.setSmooth(true);
        grande.setFitWidth(Math.min(imagen.getWidth(), pantalla.getWidth() - 140));
        VBox raiz = new VBox(8, grande);
        raiz.getStyleClass().add("formulario");
        raiz.setAlignment(Pos.TOP_CENTER);
        raiz.setStyle("-fx-padding: 12;");
        ApoyoVista.mostrarModal(raiz, getScene().getWindow(), "Comprobante · pedido #" + pago.getFolio());
    }

    // RF-50 · Pasos 4 y 5: guarda una copia temporal y la abre con el programa del equipo (visor de fotos o de PDF).
    private void abrirEnElEquipo() {
        if (archivo == null) {
            return;
        }
        try {
            File copia = File.createTempFile("comprobante-pedido-" + pago.getFolio() + "-", archivo.getExtension());
            copia.deleteOnExit();
            Files.write(copia.toPath(), archivo.getContenido());
            if (!CarmirApp.abrirEnlace(copia.getAbsolutePath())) {
                // RF-50 · Excepción paso 5: no hay un programa para abrirlo.
                Alertas.aviso("No se pudo abrir el comprobante en el equipo");
            }
        } catch (IOException e) {
            // RF-50 · Excepción paso 5: no se pudo guardar la copia temporal.
            Alertas.aviso("No se pudo abrir el comprobante en el equipo");
        }
    }

    // RF-50 · Pasos 4 y 5: el enlace que escribió el cliente se abre en el navegador.
    private void abrirEnlace() {
        if (pago != null && pago.tieneEnlace()) {
            DetallePedidoController.abrirComprobante(pago.getLinkComprobante());
        }
    }

    // "820 KB" o "1.4 MB"
    private static String textoTamano(int bytes) {
        if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
        int kb = Math.max(1, Math.round(bytes / 1024f));
        return kb + " KB";
    }

    // ===================== Ventana «Ver comprobante» =====================

    // RF-50: abre el comprobante del pago en su propia ventana (desde «Validación de pagos» o la ficha del pedido).
    public static void abrirVentana(Pago pago, Window duena) {
        Label titulo = new Label("Comprobante del pago · pedido #" + pago.getFolio());
        titulo.getStyleClass().add("formulario-titulo");
        Label datosPago = new Label(Formatos.moneda(pago.getMonto()) + " · " + pago.getMetodo() + " · "
                + Formatos.fechaHoraCorta(pago.getFechaPago()) + " · " + pago.getEstado());
        datosPago.getStyleClass().add("texto-suave");
        VisorComprobante visor = new VisorComprobante();
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        visor.setTamanoVista(Math.min(760, pantalla.getWidth() - 160), Math.min(620, pantalla.getHeight() - 260));
        VBox raiz = new VBox(10, titulo, datosPago, visor);
        raiz.getStyleClass().add("formulario");
        raiz.getStylesheets().add(VisorComprobante.class.getResource("/com/pac/css/usuarios.css").toExternalForm());
        raiz.getStylesheets().add(VisorComprobante.class.getResource("/com/pac/css/catalogo.css").toExternalForm());
        raiz.getStylesheets().add(VisorComprobante.class.getResource("/com/pac/css/pedidos.css").toExternalForm());
        visor.mostrar(pago);
        ApoyoVista.mostrarModal(raiz, duena, "Comprobante · pedido #" + pago.getFolio());
    }
}

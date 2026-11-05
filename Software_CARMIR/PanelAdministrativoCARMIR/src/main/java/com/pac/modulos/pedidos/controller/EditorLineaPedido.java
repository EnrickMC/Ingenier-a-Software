package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.util.Formatos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Renglón editable de un postre en el formulario de modificación:
 * nombre y personalización, cantidad, dedicatoria, comentario y los botones Personalizar y Quitar.
 */
public class EditorLineaPedido {

    private LineaPedido linea;
    private FormularioModificacionController formulario;
    private VBox caja;
    private Label lblNombre;
    private Label lblEspecificaciones;
    private Label lblSubtotal;
    private Spinner<Integer> spnCantidad;
    private TextField txtDedicatoria;
    private TextField txtComentario;

    public EditorLineaPedido(LineaPedido linea, FormularioModificacionController formulario) {
        this.linea = linea;
        this.formulario = formulario;
        construir();
    }

    private void construir() {
        lblNombre = new Label();
        lblNombre.getStyleClass().add("postre-nombre");
        lblNombre.setWrapText(true);
        lblSubtotal = new Label();
        lblSubtotal.getStyleClass().add("precio");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Button btnPersonalizar = new Button("Personalizar");
        btnPersonalizar.getStyleClass().add("btn-azul");
        // Flecha necesaria: JavaFX pide una acción para el clic del botón
        btnPersonalizar.setOnAction(evento -> formulario.personalizar(this));
        Button btnQuitar = new Button("Quitar");
        btnQuitar.getStyleClass().add("btn-rojo");
        // Flecha necesaria: JavaFX pide una acción para el clic del botón
        btnQuitar.setOnAction(evento -> formulario.quitar(this));
        HBox arriba = new HBox(8, lblNombre, espacio, lblSubtotal, btnPersonalizar, btnQuitar);
        arriba.setAlignment(Pos.CENTER_LEFT);

        lblEspecificaciones = new Label();
        lblEspecificaciones.getStyleClass().add("especificaciones");
        lblEspecificaciones.setWrapText(true);

        spnCantidad = new Spinner<Integer>(1, 99, linea.getCantidad());
        spnCantidad.setEditable(true);
        spnCantidad.setPrefWidth(80);
        // Flecha necesaria: JavaFX avisa con un "listener" cuando cambia el valor
        spnCantidad.valueProperty().addListener((observable, antes, ahora) -> cambioCantidad());
        txtDedicatoria = new TextField(texto(linea.getDedicatoria()));
        txtDedicatoria.getStyleClass().add("campo");
        txtDedicatoria.setPromptText("Dedicatoria (opcional)");
        HBox.setHgrow(txtDedicatoria, Priority.ALWAYS);
        txtComentario = new TextField(texto(linea.getComentario()));
        txtComentario.getStyleClass().add("campo");
        txtComentario.setPromptText("Comentario (opcional)");
        HBox.setHgrow(txtComentario, Priority.ALWAYS);
        Label lblCantidad = new Label("Piezas");
        lblCantidad.getStyleClass().add("formulario-etiqueta");
        HBox abajo = new HBox(8, lblCantidad, spnCantidad, txtDedicatoria, txtComentario);
        abajo.setAlignment(Pos.CENTER_LEFT);

        caja = new VBox(6, arriba, lblEspecificaciones, abajo);
        caja.getStyleClass().add("linea-postre");
        actualizar();
    }

    // Vuelve a mostrar nombre, personalización y subtotal
    public void actualizar() {
        lblNombre.setText(linea.getNombreMostrar());
        if (linea.getEspecificaciones().isEmpty()) {
            lblEspecificaciones.setText("Sin personalización");
        } else {
            lblEspecificaciones.setText(String.join(" · ", linea.getEspecificaciones()));
        }
        String subtotal = Formatos.moneda(linea.getPrecioUnitario()) + " c/u · " + Formatos.moneda(linea.getSubtotal());
        if (linea.isPersonalizacionCambiada()) {
            subtotal = subtotal + " (nuevo precio)";
        }
        lblSubtotal.setText(subtotal);
    }

    private void cambioCantidad() {
        Integer valor = spnCantidad.getValue();
        if (valor != null) {
            linea.setCantidad(valor);
        }
        actualizar();
        formulario.recalcularTotal();
    }

    // Pasa lo escrito en pantalla a la línea
    public LineaPedido leer() {
        Integer valor = spnCantidad.getValue();
        if (valor != null) {
            linea.setCantidad(valor);
        }
        linea.setDedicatoria(limpiar(txtDedicatoria.getText()));
        linea.setComentario(limpiar(txtComentario.getText()));
        return linea;
    }

    public LineaPedido getLinea() {
        return linea;
    }

    public VBox getNodo() {
        return caja;
    }

    private String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}

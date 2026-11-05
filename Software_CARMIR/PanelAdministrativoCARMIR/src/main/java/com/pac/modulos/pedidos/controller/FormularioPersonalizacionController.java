package com.pac.modulos.pedidos.controller;

import com.pac.modulos.catalogo.controller.SelectorOpcion;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.service.ConfiguracionPostre;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana para cambiar la personalización de un postre del pedido (Modificación de pedidos).
 * Arma un selector por característica, igual que el formulario de productos del catálogo,
 * y valida con ConfiguracionPostre (obligatorias, presupuesto y exclusiones).
 */
public class FormularioPersonalizacionController {

    @FXML private Label lblTitulo;
    @FXML private Label lblPrecio;
    @FXML private VBox contenedorCaracteristicas;
    @FXML private VBox listaAvisos;

    private ConfiguracionPostre configuracion;
    private List<SelectorOpcion> selectores = new ArrayList<SelectorOpcion>();
    private List<OpcionElegida> elegidas;   // null si se canceló

    public void preparar(ConfiguracionPostre configuracion, String nombreCategoria, List<OpcionElegida> actuales) {
        this.configuracion = configuracion;
        lblTitulo.setText("Personalizar: " + nombreCategoria);
        contenedorCaracteristicas.getChildren().clear();
        selectores.clear();
        if (configuracion.getCaracteristicas().isEmpty()) {
            Label vacio = new Label("Esta categoría no tiene características registradas en el catálogo.");
            vacio.getStyleClass().add("texto-suave");
            contenedorCaracteristicas.getChildren().add(vacio);
        }
        for (Caracteristica caracteristica : configuracion.getCaracteristicas()) {
            contenedorCaracteristicas.getChildren().add(crearCaja(caracteristica, actuales));
        }
        recalcular();
    }

    // Opciones elegidas; null si se cerró sin aceptar
    public List<OpcionElegida> getElegidas() {
        return elegidas;
    }

    // Caja de una característica: título, regla y sus opciones
    private VBox crearCaja(Caracteristica caracteristica, List<OpcionElegida> actuales) {
        VBox caja = new VBox(6);
        caja.getStyleClass().add("caja-caracteristica");
        String titulo = caracteristica.getNombre();
        if (caracteristica.isObligatoria()) {
            titulo = titulo + " *";
        }
        Label lblCaja = new Label(titulo);
        lblCaja.getStyleClass().add("formulario-etiqueta");
        String regla = "Selección única";
        if (caracteristica.isSeleccionMultiple()) {
            regla = "Selección múltiple · presupuesto de "
                    + ValidacionCatalogo.numero(caracteristica.getPresupuestoSeleccion()) + " puntos";
        }
        Label lblRegla = new Label(regla);
        lblRegla.getStyleClass().add("texto-ayuda");
        caja.getChildren().addAll(lblCaja, lblRegla);

        FlowPane opciones = new FlowPane(14, 6);
        ToggleGroup grupo = new ToggleGroup();
        if (!caracteristica.isSeleccionMultiple() && !caracteristica.isObligatoria()) {
            RadioButton ninguna = new RadioButton("Ninguna");
            ninguna.setToggleGroup(grupo);
            ninguna.setSelected(true);
            // Flecha necesaria: JavaFX pide una acción para el clic del control
            ninguna.setOnAction(evento -> recalcular());
            opciones.getChildren().add(ninguna);
        }

        for (Opcion opcion : caracteristica.getOpciones()) {
            int veces = buscarCantidad(actuales, opcion.getIdOpcion());
            boolean noDisponible = !opcion.isDisponible() || !caracteristica.isDisponible();
            // Las opciones no disponibles solo se muestran si el postre ya las tenía
            if (noDisponible && veces == 0) {
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
                if (veces > 0) {
                    selector.elegir(veces);
                }
            } else {
                RadioButton radio = new RadioButton(texto);
                radio.setToggleGroup(grupo);
                // Flecha necesaria: JavaFX pide una acción para el clic del control
                radio.setOnAction(evento -> recalcular());
                opciones.getChildren().add(radio);
                SelectorOpcion selector = new SelectorOpcion(opcion, radio);
                selectores.add(selector);
                if (veces > 0) {
                    selector.elegir(1);
                }
            }
        }
        caja.getChildren().add(opciones);
        return caja;
    }

    private int buscarCantidad(List<OpcionElegida> actuales, int idOpcion) {
        if (actuales == null) {
            return 0;
        }
        for (OpcionElegida elegida : actuales) {
            if (elegida.getOpcion().getIdOpcion() == idOpcion) {
                return elegida.getCantidad();
            }
        }
        return 0;
    }

    private List<OpcionElegida> leerElegidas() {
        List<OpcionElegida> lista = new ArrayList<OpcionElegida>();
        for (SelectorOpcion selector : selectores) {
            if (selector.estaElegida()) {
                lista.add(new OpcionElegida(selector.getOpcion(), selector.getCantidad()));
            }
        }
        return lista;
    }

    // Precio en vivo y errores de las reglas del catálogo
    private void recalcular() {
        List<OpcionElegida> lista = leerElegidas();
        lblPrecio.setText(Formatos.moneda(ConfiguracionPostre.calcularPrecio(lista)));
        listaAvisos.getChildren().clear();
        for (String error : configuracion.validar(lista)) {
            Label linea = new Label("• " + error);
            linea.setWrapText(true);
            linea.getStyleClass().add("aviso-error");
            listaAvisos.getChildren().add(linea);
        }
    }

    @FXML
    private void onAceptar() {
        List<OpcionElegida> lista = leerElegidas();
        List<String> errores = configuracion.validar(lista);
        if (!errores.isEmpty()) {
            Alertas.aviso("Corrige la personalización:\n• " + String.join("\n• ", errores));
            return;
        }
        elegidas = lista;
        cerrar();
    }

    @FXML
    private void onCancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage ventana = (Stage) lblTitulo.getScene().getWindow();
        ventana.close();
    }
}

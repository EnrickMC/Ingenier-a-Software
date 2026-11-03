package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.Pedido;
import com.pac.util.Formatos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Renglón de la lista de pedidos (lado izquierdo de Gestión de Pedidos):
 * "#folio - cliente [estado]" y abajo la fecha, los artículos y el total.
 */
public class CeldaPedido extends ListCell<Pedido> {

    private static final String[] CLASES_CELDA = {"celda-atrasada", "celda-inactiva"};

    // JavaFX llama a este método cada vez que la celda muestra otro pedido
    @Override
    protected void updateItem(Pedido pedido, boolean vacia) {
        super.updateItem(pedido, vacia);
        // Ancho 0: la celda se ajusta al ancho de la lista y no aparece la barra horizontal
        setPrefWidth(0);
        for (String clase : CLASES_CELDA) {
            getStyleClass().remove(clase);
        }
        if (vacia || pedido == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        Label titulo = new Label("#" + pedido.getFolio() + " - " + pedido.getCliente().getNombre());
        titulo.getStyleClass().add("celda-titulo");
        titulo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titulo, Priority.ALWAYS);
        Label estado = new Label();
        EstilosPedidos.aplicarEstadoCorto(estado, pedido);
        estado.setMinWidth(Region.USE_PREF_SIZE);
        // Nombre del cliente en su propio renglón; debajo las etiquetas de estado
        FlowPane arriba = new FlowPane(8, 4, estado);
        arriba.setAlignment(Pos.CENTER_LEFT);

        String articulos = pedido.getNumeroArticulos() + " artículo(s)";
        if (pedido.getNumeroArticulos() == 1) {
            articulos = "1 artículo";
        }
        Label datos = new Label("Solicitado " + Formatos.fechaHoraCorta(pedido.getFechaSolicitud()) + " | " + articulos);
        datos.getStyleClass().add("celda-subtitulo");
        Label entrega = new Label("Entrega " + Formatos.fecha(pedido.getFechaEntrega()));
        entrega.getStyleClass().add("celda-subtitulo");
        Label total = new Label(Formatos.moneda(pedido.getMontoTotal()));
        total.getStyleClass().add("celda-total");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox abajo = new HBox(8, entrega, espacio, total);
        abajo.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(3, titulo, arriba, datos, abajo);
        if (pedido.isActivo() && pedido.isAtrasado()) {
            Label atrasado = EstilosPedidos.crearEtiqueta("Atrasado", EstilosPedidos.ATRASADO);
            arriba.getChildren().add(atrasado);
            getStyleClass().add("celda-atrasada");
        }
        if (pedido.isCancelado() || pedido.isDadoDeBaja()) {
            getStyleClass().add("celda-inactiva");
        }
        setText(null);
        setGraphic(caja);
    }
}

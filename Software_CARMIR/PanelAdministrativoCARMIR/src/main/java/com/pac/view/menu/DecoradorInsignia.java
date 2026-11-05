package com.pac.view.menu;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * PATRÓN DECORATOR (decorador concreto).
 * Agrega un círculo con un número al botón, por ejemplo pedidos nuevos
 * o alertas de inventario. Si el número es 0, no se dibuja nada.
 */
public class DecoradorInsignia extends DecoradorElementoMenu {

    private int cantidad;
    private String claseEstilo;

    /**
     * @param cantidad    número que se muestra en la insignia
     * @param claseEstilo "insignia-alerta" (rojo) o "insignia-info" (color de acento)
     */
    public DecoradorInsignia(ElementoMenu elemento, int cantidad, String claseEstilo) {
        super(elemento);
        this.cantidad = cantidad;
        this.claseEstilo = claseEstilo;
    }

    @Override
    public Node crearNodo() {
        Node nodoOriginal = super.crearNodo();

        if (cantidad <= 0) {
            return nodoOriginal;
        }

        String texto = String.valueOf(cantidad);
        if (cantidad > 99) {
            texto = "99+";
        }

        Label insignia = new Label(texto);
        insignia.getStyleClass().add("insignia");
        insignia.getStyleClass().add(claseEstilo);
        insignia.setMouseTransparent(true);
        insignia.setTranslateX(-16);

        StackPane contenedor = new StackPane(nodoOriginal, insignia);
        StackPane.setAlignment(insignia, Pos.CENTER_RIGHT);
        return contenedor;
    }
}

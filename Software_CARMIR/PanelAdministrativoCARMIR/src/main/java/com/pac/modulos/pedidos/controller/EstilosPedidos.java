package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.Pedido;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Elige las clases CSS (pedidos.css) que colorean las etiquetas de estado de los pedidos.
 */
public class EstilosPedidos {

    public static final String ESTILO_BASE = "estado-pedido";
    public static final String CANCELADO = "estado-cancelado";
    public static final String BAJA = "estado-baja";
    public static final String ATRASADO = "estado-atrasado";

    // Todas las clases de color (para quitarlas antes de poner la nueva)
    private static final String[] CLASES = {"estado-aceptacion", "estado-aprobado", "estado-pagado",
        "estado-preparacion", "estado-terminado", "estado-entregado", CANCELADO, BAJA, ATRASADO,
        "estado-pendiente-pago", "estado-preparar"};

    // Icono (Ikonli) de cada clase de color; la posición coincide con CLASES
    private static final String[] ICONOS = {"bi-hourglass-split", "bi-check-circle-fill", "bi-wallet-fill",
        "bi-egg-fill", "bi-patch-check-fill", "bi-bag-check-fill", "bi-x-circle-fill", "bi-archive-fill",
        "bi-alarm-fill", "bi-credit-card-fill", "bi-clock-fill"};

    // Clase de apoyo: no se crean objetos de ella
    private EstilosPedidos() {
    }

    // Clase de color de un pedido: baja, cancelado o el color de su estado
    public static String claseDe(Pedido pedido) {
        if (pedido.isDadoDeBaja()) {
            return BAJA;
        }
        if (pedido.isCancelado()) {
            return CANCELADO;
        }
        return pedido.getEstado().getClaseCss();
    }

    // Pone en la etiqueta el texto y el color del estado del pedido
    public static void aplicarEstado(Label etiqueta, Pedido pedido) {
        etiqueta.setText(pedido.getTextoEstado());
        aplicarClase(etiqueta, claseDe(pedido));
        ajustarAncho(etiqueta);
    }

    // Pone en la etiqueta el texto y el color de un estado
    public static void aplicarEstado(Label etiqueta, EstadoPedido estado) {
        etiqueta.setText(estado.toString());
        aplicarClase(etiqueta, estado.getClaseCss());
    }

    // Igual que aplicarEstado, pero con el texto corto para que todas las etiquetas midan lo mismo
    public static void aplicarEstadoCorto(Label etiqueta, Pedido pedido) {
        String clase = claseDe(pedido);
        etiqueta.setText(textoCorto(pedido));
        aplicarClase(etiqueta, clase);
    }

    // Texto corto de un estado (cabe en el ancho fijo de las etiquetas)
    public static String textoCorto(EstadoPedido estado) {
        if (estado == EstadoPedido.EN_ACEPTACION) {
            return "Por revisar";
        }
        if (estado == EstadoPedido.TERMINADO) {
            return "Terminado";
        }
        return estado.toString();
    }

    private static String textoCorto(Pedido pedido) {
        if (pedido.isDadoDeBaja()) {
            return "Dado de baja";
        }
        if (pedido.isCancelado()) {
            return "Cancelado";
        }
        return textoCorto(pedido.getEstado());
    }

    // Los textos largos no caben en el ancho fijo: esas etiquetas se ajustan a su texto
    private static void ajustarAncho(Labeled etiqueta) {
        etiqueta.getStyleClass().remove("estado-libre");
        if (etiqueta.getText() != null && etiqueta.getText().length() > 15) {
            etiqueta.getStyleClass().add("estado-libre");
        }
    }

    // Icono (literal de Ikonli) que va con una clase de color
    public static String iconoDe(String clase) {
        for (int i = 0; i < CLASES.length; i++) {
            if (CLASES[i].equals(clase)) {
                return ICONOS[i];
            }
        }
        return "bi-circle-fill";
    }

    // Pone la clase indicada quitando antes las otras clases de color
    public static void aplicarClase(Node nodo, String clase) {
        for (String otra : CLASES) {
            nodo.getStyleClass().remove(otra);
        }
        // El tamaño fijo de las etiquetas solo va en etiquetas (no en las tarjetas del calendario)
        if (nodo instanceof Labeled && !nodo.getStyleClass().contains(ESTILO_BASE)) {
            nodo.getStyleClass().add(ESTILO_BASE);
        }
        if (clase != null) {
            nodo.getStyleClass().add(clase);
            // Cada etiqueta lleva el icono de su estado
            if (nodo instanceof Labeled) {
                ((Labeled) nodo).setGraphic(new FontIcon(iconoDe(clase)));
                ajustarAncho((Labeled) nodo);
            }
        }
    }

    // Etiqueta pequeña con texto y color (para la lista y la agenda)
    public static Label crearEtiqueta(String texto, String clase) {
        Label etiqueta = new Label(texto);
        aplicarClase(etiqueta, clase);
        return etiqueta;
    }
}

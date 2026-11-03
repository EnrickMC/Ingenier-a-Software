package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.Pedido;
import com.pac.util.Formatos;

/**
 * Renglón de la tabla "Últimos pedidos" del dashboard.
 * Cada getter es una columna (PropertyValueFactory del FXML).
 */
public class FilaPedido {

    private Pedido pedido;

    public FilaPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Pedido getPedidoCompleto() {
        return pedido;
    }

    public String getFolio() {
        return "#" + pedido.getFolio();
    }

    public String getCliente() {
        return pedido.getCliente().getNombre();
    }

    public String getProductos() {
        if (pedido.getResumenProductos() == null) {
            return "—";
        }
        return pedido.getResumenProductos();
    }

    public String getTotal() {
        return Formatos.moneda(pedido.getMontoTotal());
    }

    public String getEntrega() {
        return Formatos.fecha(pedido.getFechaEntrega());
    }

    public String getEstado() {
        return pedido.getTextoEstado();
    }
}

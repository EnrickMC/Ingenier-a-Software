package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.EstadoPago;
import com.pac.modulos.pedidos.model.Pago;
import com.pac.util.Formatos;

/**
 * Renglón de la tabla de "Validación de Pagos".
 * Cada getter es una columna (PropertyValueFactory del FXML).
 */
public class FilaPago {

    private Pago pago;

    public FilaPago(Pago pago) {
        this.pago = pago;
    }

    public Pago getPago() {
        return pago;
    }

    public String getFolio() {
        return "#" + pago.getFolio();
    }

    public String getCliente() {
        return pago.getNombreCliente();
    }

    public String getMonto() {
        return Formatos.moneda(pago.getMonto());
    }

    public String getFecha() {
        return Formatos.fechaHoraCorta(pago.getFechaPago());
    }

    public String getMetodo() {
        return pago.getMetodo().toString();
    }

    public String getReferencia() {
        if (pago.getReferencia() == null) {
            return "—";
        }
        return pago.getReferencia();
    }

    public String getComprobante() {
        if (pago.tieneComprobante()) {
            return "Sí";
        }
        return "No";
    }

    public String getEstado() {
        String texto = pago.getEstado().toString();
        if (pago.getEstado() == EstadoPago.RECHAZADO && pago.getMotivoRechazo() != null) {
            texto = texto + ": " + pago.getMotivoRechazo();
        }
        return texto;
    }

    public String getPedido() {
        String texto = "";
        if (pago.getEstadoPedido() != null) {
            texto = pago.getEstadoPedido().toString();
        }
        if (pago.isPedidoCancelado()) {
            texto = "Cancelado (" + texto + ")";
        }
        return texto;
    }

    public String getUsuario() {
        return pago.getNombreUsuario();
    }
}

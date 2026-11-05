package com.pac.modulos.pedidos.model;

/**
 * Cómo eligió pagar el cliente (Pedido.ModalidadPago).
 * Con "Anticipo" el pedido pasa a Pagado al cubrir el anticipo; con "Pago total", al cubrir el total.
 */
public enum ModalidadPago {

    ANTICIPO("Anticipo", "Anticipo"),
    PAGO_TOTAL("Pago total", "Pago total");

    private final String valorBD;
    private final String texto;

    ModalidadPago(String valorBD, String texto) {
        this.valorBD = valorBD;
        this.texto = texto;
    }

    public String getValorBD() {
        return valorBD;
    }

    public static ModalidadPago desdeBD(String valor) {
        for (ModalidadPago modalidad : values()) {
            if (modalidad.valorBD.equalsIgnoreCase(valor)) {
                return modalidad;
            }
        }
        throw new IllegalArgumentException("Modalidad de pago desconocida: " + valor);
    }

    @Override
    public String toString() {
        return texto;
    }
}

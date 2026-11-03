package com.pac.modulos.pedidos.model;

/**
 * Forma de pago (Pago.MetodoPago).
 */
public enum MetodoPago {

    TRANSFERENCIA("Transferencia", "Transferencia"),
    DEPOSITO("Deposito", "Depósito"),
    EFECTIVO("Efectivo", "Efectivo");

    private final String valorBD;
    private final String texto;

    MetodoPago(String valorBD, String texto) {
        this.valorBD = valorBD;
        this.texto = texto;
    }

    public String getValorBD() {
        return valorBD;
    }

    public static MetodoPago desdeBD(String valor) {
        for (MetodoPago metodo : values()) {
            if (metodo.valorBD.equalsIgnoreCase(valor)) {
                return metodo;
            }
        }
        throw new IllegalArgumentException("Método de pago desconocido: " + valor);
    }

    @Override
    public String toString() {
        return texto;
    }
}

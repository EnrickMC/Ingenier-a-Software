package com.pac.modulos.pedidos.model;

/**
 * Forma de pago (Pago.MetodoPago).
 */
public enum MetodoPago {

    TRANSFERENCIA("Transferencia", "Transferencia"),
    EFECTIVO("Efectivo", "Efectivo");

    // Valor que se quitó de la base de datos (Parte 8 de MIGRACION_COMPLETA.sql lo pasa a Transferencia)
    private static final String VALOR_QUITADO = "Deposito";

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
        // Si la base todavía no tiene la Parte 8, un pago viejo se lee como transferencia
        if (VALOR_QUITADO.equalsIgnoreCase(valor)) {
            return TRANSFERENCIA;
        }
        throw new IllegalArgumentException("Método de pago desconocido: " + valor);
    }

    @Override
    public String toString() {
        return texto;
    }
}

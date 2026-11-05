package com.pac.modulos.pedidos.model;

/**
 * Resultado de la revisión de un pago (Pago.EstadoValidacion).
 * Solo los pagos "Validado" cuentan para el saldo (vista vw_SaldoPedido).
 */
public enum EstadoPago {

    PENDIENTE("Pendiente"),
    VALIDADO("Validado"),
    RECHAZADO("Rechazado");

    private final String valorBD;

    EstadoPago(String valorBD) {
        this.valorBD = valorBD;
    }

    public String getValorBD() {
        return valorBD;
    }

    public static EstadoPago desdeBD(String valor) {
        for (EstadoPago estado : values()) {
            if (estado.valorBD.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pago desconocido: " + valor);
    }

    @Override
    public String toString() {
        return valorBD;
    }
}

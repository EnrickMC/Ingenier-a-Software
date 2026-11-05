package com.pac.modulos.pedidos.model;

/**
 * Cómo recibe el cliente su pedido (Pedido.ModalidadEntrega).
 */
public enum ModalidadEntrega {

    RECOGER("Recoger en tienda", "Recoger en tienda"),
    DOMICILIO("Envio a domicilio", "Envío a domicilio");

    private final String valorBD;
    private final String texto;

    ModalidadEntrega(String valorBD, String texto) {
        this.valorBD = valorBD;
        this.texto = texto;
    }

    public String getValorBD() {
        return valorBD;
    }

    public static ModalidadEntrega desdeBD(String valor) {
        for (ModalidadEntrega modalidad : values()) {
            if (modalidad.valorBD.equalsIgnoreCase(valor)) {
                return modalidad;
            }
        }
        throw new IllegalArgumentException("Modalidad de entrega desconocida: " + valor);
    }

    @Override
    public String toString() {
        return texto;
    }
}

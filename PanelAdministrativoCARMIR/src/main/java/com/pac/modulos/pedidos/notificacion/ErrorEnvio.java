package com.pac.modulos.pedidos.notificacion;

/**
 * Error que lanza un canal cuando no pudo entregar el mensaje.
 * sinRed = true si fue por falta de internet (se marca "Pendiente de reenvío").
 */
public class ErrorEnvio extends Exception {

    private boolean sinRed;

    public ErrorEnvio(String mensaje, boolean sinRed) {
        super(mensaje);
        this.sinRed = sinRed;
    }

    public boolean isSinRed() {
        return sinRed;
    }
}

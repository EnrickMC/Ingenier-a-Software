package com.pac.modulos.catalogo.model;

/**
 * Una opción que forma parte de un producto, con las veces que se eligió.
 * Corresponde a una fila de la tabla PostreOpcion.
 */
public class OpcionElegida {

    private Opcion opcion;
    private int cantidad;

    public OpcionElegida(Opcion opcion, int cantidad) {
        this.opcion = opcion;
        this.cantidad = cantidad;
    }

    public Opcion getOpcion() {
        return opcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}

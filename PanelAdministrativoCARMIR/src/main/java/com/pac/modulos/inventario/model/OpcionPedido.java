package com.pac.modulos.inventario.model;

/**
 * Una opción elegida en un postre de un pedido (DetallePedido + PostreOpcion),
 * con los datos que necesita el descuento automático de insumos (RF-63).
 */
public class OpcionPedido {

    private int idPostre;
    private int cantidadPostres;          // DetallePedido.Cantidad
    private int idOpcion;
    private String nombreOpcion;
    private int cantidadElegida;          // PostreOpcion.CantidadElegida
    private boolean seEscala;
    private double multiplicadorIngredientes = 1;
    private int idRecetaActiva;           // 0 si la opción no tiene receta activa

    public int getIdPostre() {
        return idPostre;
    }

    public void setIdPostre(int idPostre) {
        this.idPostre = idPostre;
    }

    public int getCantidadPostres() {
        return cantidadPostres;
    }

    public void setCantidadPostres(int cantidadPostres) {
        this.cantidadPostres = cantidadPostres;
    }

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public String getNombreOpcion() {
        return nombreOpcion;
    }

    public void setNombreOpcion(String nombreOpcion) {
        this.nombreOpcion = nombreOpcion;
    }

    public int getCantidadElegida() {
        return cantidadElegida;
    }

    public void setCantidadElegida(int cantidadElegida) {
        this.cantidadElegida = cantidadElegida;
    }

    public boolean isSeEscala() {
        return seEscala;
    }

    public void setSeEscala(boolean seEscala) {
        this.seEscala = seEscala;
    }

    public double getMultiplicadorIngredientes() {
        return multiplicadorIngredientes;
    }

    public void setMultiplicadorIngredientes(double multiplicadorIngredientes) {
        this.multiplicadorIngredientes = multiplicadorIngredientes;
    }

    public int getIdRecetaActiva() {
        return idRecetaActiva;
    }

    public void setIdRecetaActiva(int idRecetaActiva) {
        this.idRecetaActiva = idRecetaActiva;
    }

    public boolean tieneRecetaActiva() {
        return idRecetaActiva > 0;
    }
}

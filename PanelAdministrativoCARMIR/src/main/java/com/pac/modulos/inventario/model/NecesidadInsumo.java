package com.pac.modulos.inventario.model;

/**
 * Cantidad total de un insumo que necesita un pedido (RF-63),
 * junto con la existencia que hay en el inventario.
 */
public class NecesidadInsumo {

    private int idInsumo;
    private String nombreInsumo;
    private UnidadMedida unidad;
    private double cantidadNecesaria;
    private double existencia;

    public NecesidadInsumo(int idInsumo, String nombreInsumo, UnidadMedida unidad) {
        this.idInsumo = idInsumo;
        this.nombreInsumo = nombreInsumo;
        this.unidad = unidad;
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public String getNombreInsumo() {
        return nombreInsumo;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public double getCantidadNecesaria() {
        return cantidadNecesaria;
    }

    // Suma una cantidad a lo que se necesita
    public void sumar(double cantidad) {
        cantidadNecesaria = cantidadNecesaria + cantidad;
    }

    // Deja la cantidad con 3 decimales, igual que la columna DECIMAL(10,3)
    public void redondear() {
        cantidadNecesaria = Math.round(cantidadNecesaria * 1000) / 1000.0;
    }

    public double getExistencia() {
        return existencia;
    }

    public void setExistencia(double existencia) {
        this.existencia = existencia;
    }

    // true si la existencia no alcanza
    public boolean falta() {
        return existencia + 0.0005 < cantidadNecesaria;
    }

    // Cuánto falta para completar
    public double getFaltante() {
        if (!falta()) {
            return 0;
        }
        return cantidadNecesaria - existencia;
    }
}

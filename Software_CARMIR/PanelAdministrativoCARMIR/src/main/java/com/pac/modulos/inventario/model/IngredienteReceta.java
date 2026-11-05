package com.pac.modulos.inventario.model;

/**
 * Representa una fila de la tabla RecetaInsumo: un insumo de la receta y su cantidad.
 * También trae los datos del insumo que se necesitan para mostrarlo y calcular el costo.
 */
public class IngredienteReceta {

    private int idInsumo;
    private double cantidad;

    // Datos del insumo (no son columnas de RecetaInsumo)
    private String nombreInsumo;
    private UnidadMedida unidad;
    private double costoUnitario;
    private boolean insumoActivo = true;

    public IngredienteReceta() {
    }

    // Crea el ingrediente a partir del insumo elegido en el formulario
    public IngredienteReceta(Insumo insumo, double cantidad) {
        this.idInsumo = insumo.getIdInsumo();
        this.cantidad = cantidad;
        this.nombreInsumo = insumo.getNombre();
        this.unidad = insumo.getUnidad();
        this.costoUnitario = insumo.getCostoUnitario();
        this.insumoActivo = insumo.isActivo();
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(int idInsumo) {
        this.idInsumo = idInsumo;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public String getNombreInsumo() {
        return nombreInsumo;
    }

    public void setNombreInsumo(String nombreInsumo) {
        this.nombreInsumo = nombreInsumo;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public void setUnidad(UnidadMedida unidad) {
        this.unidad = unidad;
    }

    public double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public boolean isInsumoActivo() {
        return insumoActivo;
    }

    public void setInsumoActivo(boolean insumoActivo) {
        this.insumoActivo = insumoActivo;
    }

    // Costo de este ingrediente = cantidad × costo unitario del insumo
    public double getCosto() {
        return cantidad * costoUnitario;
    }
}

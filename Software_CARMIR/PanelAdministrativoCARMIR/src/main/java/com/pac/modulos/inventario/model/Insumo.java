package com.pac.modulos.inventario.model;

import java.time.LocalDate;

/**
 * Representa una fila de la tabla Insumo (por ejemplo "Harina de trigo").
 *
 * La existencia no se guarda en la tabla: es la suma de la existencia de sus lotes
 * (igual que la vista vw_ExistenciaInsumo). El DAO la llena al leer el insumo.
 * La tabla no tiene columna de clave; la clave se arma con el id (por ejemplo INS-0007).
 */
public class Insumo {

    private int idInsumo;
    private String nombre;
    private String categoria;
    private UnidadMedida unidad;
    private double costoUnitario;
    private double stockMinimo;
    private boolean perecedero;
    private boolean activo = true;

    // Datos calculados (no son columnas)
    private double existencia;
    private LocalDate caducidadMasProxima;   // del lote con existencia que caduca antes (null si ninguno)

    public int getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(int idInsumo) {
        this.idInsumo = idInsumo;
    }

    // Clave que se muestra en pantalla, por ejemplo "INS-0007"
    public String getClave() {
        return String.format("INS-%04d", idInsumo);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
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

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isPerecedero() {
        return perecedero;
    }

    public void setPerecedero(boolean perecedero) {
        this.perecedero = perecedero;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public double getExistencia() {
        return existencia;
    }

    public void setExistencia(double existencia) {
        this.existencia = existencia;
    }

    public LocalDate getCaducidadMasProxima() {
        return caducidadMasProxima;
    }

    public void setCaducidadMasProxima(LocalDate caducidadMasProxima) {
        this.caducidadMasProxima = caducidadMasProxima;
    }

    // true si tiene stock mínimo configurado (mayor a cero)
    public boolean tieneStockMinimo() {
        return stockMinimo > 0;
    }

    // RF-61: hay alerta si la existencia es igual o menor al stock mínimo
    public boolean estaBajoMinimo() {
        if (!tieneStockMinimo()) {
            return false;
        }
        return existencia <= stockMinimo + 0.0005;
    }

    // Así se muestra en los selectores: "Harina de trigo (kg)"
    @Override
    public String toString() {
        String texto = nombre;
        if (unidad != null) {
            texto = texto + " (" + unidad.getValorBD() + ")";
        }
        if (!activo) {
            texto = texto + " · dado de baja";
        }
        return texto;
    }
}

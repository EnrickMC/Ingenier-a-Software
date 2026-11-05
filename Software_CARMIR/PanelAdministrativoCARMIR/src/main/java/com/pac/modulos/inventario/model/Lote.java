package com.pac.modulos.inventario.model;

import java.time.LocalDate;

/**
 * Representa una fila de la tabla Lote: cada compra de un insumo,
 * con su propia existencia y su fecha de caducidad.
 */
public class Lote {

    private int idLote;
    private int idInsumo;
    private double cantidadComprada;
    private double existencia;
    private double costoCompra;
    private LocalDate fechaCompra;
    private String proveedor;
    private LocalDate fechaCaducidad;

    // Datos del insumo (no son columnas de Lote)
    private String nombreInsumo;
    private UnidadMedida unidad;

    public int getIdLote() {
        return idLote;
    }

    public void setIdLote(int idLote) {
        this.idLote = idLote;
    }

    // Clave que se muestra en pantalla, por ejemplo "L-0012"
    public String getClave() {
        return String.format("L-%04d", idLote);
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(int idInsumo) {
        this.idInsumo = idInsumo;
    }

    public double getCantidadComprada() {
        return cantidadComprada;
    }

    public void setCantidadComprada(double cantidadComprada) {
        this.cantidadComprada = cantidadComprada;
    }

    public double getExistencia() {
        return existencia;
    }

    public void setExistencia(double existencia) {
        this.existencia = existencia;
    }

    public double getCostoCompra() {
        return costoCompra;
    }

    public void setCostoCompra(double costoCompra) {
        this.costoCompra = costoCompra;
    }

    public LocalDate getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDate fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public LocalDate getFechaCaducidad() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(LocalDate fechaCaducidad) {
        this.fechaCaducidad = fechaCaducidad;
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

    // Costo de una unidad de este lote = costo de compra / cantidad comprada
    public double getCostoPorUnidad() {
        if (cantidadComprada <= 0) {
            return 0;
        }
        return costoCompra / cantidadComprada;
    }

    // Así se muestra en los selectores: "L-0012 · quedan 2.5 kg · caduca 10/11/2026"
    @Override
    public String toString() {
        // El lote con id 0 es la opción "automático" del formulario de merma
        if (idLote == 0) {
            return "Automático: primero el que caduca antes (PEPS)";
        }
        String texto = getClave() + " · quedan " + formatoCantidad(existencia);
        if (unidad != null) {
            texto = texto + " " + unidad.getValorBD();
        }
        if (fechaCaducidad != null) {
            texto = texto + " · caduca " + fechaCaducidad.getDayOfMonth() + "/" + fechaCaducidad.getMonthValue()
                    + "/" + fechaCaducidad.getYear();
        }
        return texto;
    }

    // Muestra la cantidad sin ceros de más (2.5 en lugar de 2.500)
    private String formatoCantidad(double valor) {
        double redondeado = Math.round(valor * 1000) / 1000.0;
        if (redondeado == Math.floor(redondeado)) {
            return String.valueOf((long) redondeado);
        }
        return String.valueOf(redondeado);
    }
}

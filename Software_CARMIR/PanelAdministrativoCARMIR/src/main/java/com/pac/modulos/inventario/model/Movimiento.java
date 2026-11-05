package com.pac.modulos.inventario.model;

import java.time.LocalDateTime;

/**
 * Representa una fila de la tabla MovimientoInventario: cada entrada o salida de un insumo.
 *
 * - cantidad: positiva en compras, negativa en mermas y producción.
 * - idUsuario: 0 cuando lo registró la Administradora o el sistema (en la BD queda NULL).
 * - folio: 0 cuando no lo provocó un pedido (en la BD queda NULL).
 */
public class Movimiento {

    private int idMovimiento;
    private int idInsumo;
    private int idUsuario;
    private int folio;
    private TipoMovimiento tipo;
    private double cantidad;
    private String motivo;
    private LocalDateTime fecha;
    private double costo;

    // Datos relacionados (no son columnas de MovimientoInventario)
    private String nombreInsumo;
    private UnidadMedida unidad;
    private String nombreUsuario;

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(int idInsumo) {
        this.idInsumo = idInsumo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public double getCosto() {
        return costo;
    }

    public void setCosto(double costo) {
        this.costo = costo;
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

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}

package com.pac.modulos.inventario.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una fila de la tabla AlertaInventario.
 * Si la alerta es de caducidad, también trae el lote de la tabla AlertaCaducidad
 * y los días que le faltan (vista vw_AlertaCaducidad).
 */
public class AlertaInventario {

    private int idAlerta;
    private int idInsumo;
    private TipoAlerta tipo;
    private LocalDateTime fecha;
    private double existenciaEnAlerta;
    private boolean pendiente = true;

    // Datos relacionados (no son columnas de AlertaInventario)
    private String nombreInsumo;
    private UnidadMedida unidad;
    private int idLote;                  // 0 si no es de caducidad
    private LocalDate fechaCaducidad;    // del lote
    private double existenciaLote;       // lo que queda del lote

    public int getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(int idAlerta) {
        this.idAlerta = idAlerta;
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(int idInsumo) {
        this.idInsumo = idInsumo;
    }

    public TipoAlerta getTipo() {
        return tipo;
    }

    public void setTipo(TipoAlerta tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public double getExistenciaEnAlerta() {
        return existenciaEnAlerta;
    }

    public void setExistenciaEnAlerta(double existenciaEnAlerta) {
        this.existenciaEnAlerta = existenciaEnAlerta;
    }

    public boolean isPendiente() {
        return pendiente;
    }

    public void setPendiente(boolean pendiente) {
        this.pendiente = pendiente;
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

    public int getIdLote() {
        return idLote;
    }

    public void setIdLote(int idLote) {
        this.idLote = idLote;
    }

    public LocalDate getFechaCaducidad() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(LocalDate fechaCaducidad) {
        this.fechaCaducidad = fechaCaducidad;
    }

    public double getExistenciaLote() {
        return existenciaLote;
    }

    public void setExistenciaLote(double existenciaLote) {
        this.existenciaLote = existenciaLote;
    }

    // RF-39: una alerta de existencia baja con existencia en cero se muestra como "Agotado"
    public boolean estaAgotado() {
        return tipo == TipoAlerta.EXISTENCIA_BAJA && existenciaEnAlerta <= 0.0005;
    }
}

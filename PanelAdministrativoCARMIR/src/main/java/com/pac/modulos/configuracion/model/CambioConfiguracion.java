package com.pac.modulos.configuracion.model;

import java.time.LocalDateTime;

/**
 * Un cambio de la configuración o de la cobertura de envío (tabla CambioConfiguracion).
 * Guarda qué cambió, el valor de antes, el de después, quién y cuándo.
 */
public class CambioConfiguracion {

    private int idCambio;
    private String clave;
    private String valorAnterior;     // null si el registro es nuevo
    private String valorNuevo;        // null si se eliminó
    private int idUsuario;            // 0 = Administradora
    private String nombreUsuario;
    private LocalDateTime fecha;

    public CambioConfiguracion(String clave, String valorAnterior, String valorNuevo, int idUsuario) {
        this.clave = clave;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.idUsuario = idUsuario;
    }

    public int getIdCambio() {
        return idCambio;
    }

    public void setIdCambio(int idCambio) {
        this.idCambio = idCambio;
    }

    public String getClave() {
        return clave;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}

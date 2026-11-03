package com.pac.modulos.pedidos.model;

import java.time.LocalDateTime;

/**
 * Cancelación de un pedido (tabla Cancelacion). No es un estado: el pedido conserva el último.
 */
public class Cancelacion {

    // Valores del ENUM Cancelacion.EstadoReembolso
    public static final String REEMBOLSO_NO_APLICA = "No aplica";
    public static final String REEMBOLSO_PENDIENTE = "Pendiente";
    public static final String REEMBOLSO_REALIZADO = "Realizado";

    private int folio;
    private String motivo;
    private LocalDateTime fecha;
    private double montoReembolso;
    private String estadoReembolso;
    private String nombreUsuario;

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
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

    public double getMontoReembolso() {
        return montoReembolso;
    }

    public void setMontoReembolso(double montoReembolso) {
        this.montoReembolso = montoReembolso;
    }

    public String getEstadoReembolso() {
        return estadoReembolso;
    }

    public void setEstadoReembolso(String estadoReembolso) {
        this.estadoReembolso = estadoReembolso;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}

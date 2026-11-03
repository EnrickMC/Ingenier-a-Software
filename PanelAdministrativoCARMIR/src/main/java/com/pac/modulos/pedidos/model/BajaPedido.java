package com.pac.modulos.pedidos.model;

import java.time.LocalDateTime;

/**
 * Baja de una solicitud registrada por error (tabla BajaPedido, migración V4).
 * El pedido se conserva en el historial pero sale de las listas de trabajo.
 */
public class BajaPedido {

    private int folio;
    private String motivo;
    private LocalDateTime fecha;
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

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}

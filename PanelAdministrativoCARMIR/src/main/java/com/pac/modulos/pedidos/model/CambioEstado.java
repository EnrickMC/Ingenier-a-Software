package com.pac.modulos.pedidos.model;

import java.time.LocalDateTime;

/**
 * Un renglón de la bitácora de estados del pedido (tabla HistorialEstado).
 * También se usa para mostrar en la misma lista las modificaciones del pedido.
 */
public class CambioEstado {

    private int folio;
    private String estadoAnterior;   // null en el primer registro
    private String estadoNuevo;
    private String nombreUsuario;
    private LocalDateTime fecha;

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
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

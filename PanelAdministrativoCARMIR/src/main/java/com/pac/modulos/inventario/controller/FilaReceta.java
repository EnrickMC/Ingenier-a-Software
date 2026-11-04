package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.Receta;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de recetas (RF-55).
 */
public class FilaReceta {

    public static final String ESTADO_ACTIVA = "Activa";
    public static final String ESTADO_REVISION = "Requiere revisión";
    public static final String ESTADO_INACTIVA = "Dada de baja";

    private Receta receta;

    public FilaReceta(Receta receta) {
        this.receta = receta;
    }

    public Receta getReceta() {
        return receta;
    }

    public String getNombre() {
        return receta.getNombre();
    }

    public String getOpcion() {
        return receta.getDescripcionOpcion();
    }

    public String getTiempo() {
        return receta.getTiempoMinutos() + " min";
    }

    public String getCosto() {
        return Formatos.moneda(receta.getCosto());
    }

    // Precio base que toma el catálogo para la opción
    public String getPrecio() {
        return Formatos.moneda(receta.getPrecioBase());
    }

    public String getEstado() {
        if (!receta.isActiva()) {
            return ESTADO_INACTIVA;
        }
        if (receta.requiereRevision()) {
            return ESTADO_REVISION;
        }
        return ESTADO_ACTIVA;
    }

    // Clase CSS del renglón: ámbar si requiere revisión, gris si está dada de baja
    public String getClaseEstilo() {
        if (!receta.isActiva()) {
            return EstilosInventario.FILA_INACTIVA;
        }
        if (receta.requiereRevision()) {
            return EstilosInventario.FILA_PROXIMA;
        }
        return null;
    }
}

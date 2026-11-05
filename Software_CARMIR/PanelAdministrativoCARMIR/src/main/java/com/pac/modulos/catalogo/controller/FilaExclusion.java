package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Exclusion;

/**
 * Una fila de la tabla de exclusiones (exclusiones.fxml).
 */
public class FilaExclusion {

    private Exclusion exclusion;

    public FilaExclusion(Exclusion exclusion) {
        this.exclusion = exclusion;
    }

    public Exclusion getExclusion() {
        return exclusion;
    }

    public String getOpcionA() {
        return exclusion.getNombreCaracteristica() + ": " + exclusion.getNombreOpcion();
    }

    public String getOpcionB() {
        return exclusion.getNombreCaracteristicaExcluida() + ": " + exclusion.getNombreOpcionExcluida();
    }

    public String getMotivo() {
        return ApoyoVista.texto(exclusion.getMotivo());
    }

    public String getEstado() {
        return exclusion.getEstado().getValorBD();
    }
}

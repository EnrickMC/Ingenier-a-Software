package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;

/**
 * Una fila de la tabla de características (caracteristicas.fxml).
 */
public class FilaCaracteristica {

    private Caracteristica caracteristica;

    public FilaCaracteristica(Caracteristica caracteristica) {
        this.caracteristica = caracteristica;
    }

    public Caracteristica getCaracteristica() {
        return caracteristica;
    }

    public int getOrden() {
        return caracteristica.getOrdenMostrar();
    }

    public String getNombre() {
        return caracteristica.getNombre();
    }

    public String getSeleccion() {
        if (caracteristica.isSeleccionMultiple()) {
            return "Múltiple";
        }
        return "Única";
    }

    public String getObligatoria() {
        return ApoyoVista.siNo(caracteristica.isObligatoria());
    }

    // El presupuesto solo aplica a la selección múltiple
    public String getPresupuesto() {
        if (!caracteristica.isSeleccionMultiple()) {
            return "—";
        }
        return ValidacionCatalogo.numero(caracteristica.getPresupuestoSeleccion()) + " pts";
    }

    public String getDisponible() {
        return ApoyoVista.siNo(caracteristica.isDisponible());
    }

    public int getOpciones() {
        return caracteristica.getNumeroOpciones();
    }
}

package com.pac.modulos.configuracion.controller;

import com.pac.modulos.configuracion.model.CodigoPostalCobertura;
import com.pac.util.Formatos;

/**
 * Renglón de la tabla de cobertura de envío (textos listos para mostrar).
 */
public class FilaCobertura {

    private final CodigoPostalCobertura cobertura;

    public FilaCobertura(CodigoPostalCobertura cobertura) {
        this.cobertura = cobertura;
    }

    public CodigoPostalCobertura getCobertura() {
        return cobertura;
    }

    public String getCodigoPostal() {
        return cobertura.getCodigoPostal();
    }

    public String getCosto() {
        if (cobertura.getCostoEnvio() == 0) {
            return "Gratis";
        }
        return Formatos.moneda(cobertura.getCostoEnvio());
    }
}

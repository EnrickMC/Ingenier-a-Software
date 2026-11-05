package com.pac.modulos.configuracion.model;

/**
 * Un código postal al que se hace envío a domicilio y su costo (tabla CodigoPostalCobertura).
 */
public class CodigoPostalCobertura {

    private String codigoPostal;
    private double costoEnvio;

    public CodigoPostalCobertura(String codigoPostal, double costoEnvio) {
        this.codigoPostal = codigoPostal;
        this.costoEnvio = costoEnvio;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public double getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(double costoEnvio) {
        this.costoEnvio = costoEnvio;
    }
}

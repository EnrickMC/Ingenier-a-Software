package com.pac.modulos.configuracion.model;

/**
 * Un parámetro del negocio (una fila de la tabla Configuracion).
 * El valor siempre se guarda como texto; el servicio revisa que tenga el formato correcto.
 */
public class Parametro {

    private String clave;
    private String valor;
    private String descripcion;

    public Parametro(String clave, String valor, String descripcion) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public String getClave() {
        return clave;
    }

    public String getValor() {
        if (valor == null) {
            return "";
        }
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

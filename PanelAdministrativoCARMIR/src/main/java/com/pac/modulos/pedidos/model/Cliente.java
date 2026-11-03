package com.pac.modulos.pedidos.model;

/**
 * Persona que hace pedidos desde la tienda en línea (tabla Cliente).
 */
public class Cliente {

    private int idCliente;
    private String nombre;
    private String telefono;   // TelefonoWhatsApp, 10 dígitos
    private String correo;     // puede ser null

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}

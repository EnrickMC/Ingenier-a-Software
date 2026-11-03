package com.pac.modulos.pedidos.model;

/**
 * Dirección de entrega de un cliente (tabla Direccion).
 */
public class Direccion {

    private int idDireccion;
    private int idCliente;
    private String calle;
    private String numeroExterior;
    private String numeroInterior;   // puede ser null
    private String colonia;
    private String codigoPostal;
    private String referencias;      // puede ser null

    // Dirección en una sola línea, por ejemplo "Av. Juárez 120 int. 3, Centro, C.P. 64000"
    public String getTexto() {
        String texto = calle + " " + numeroExterior;
        if (numeroInterior != null && !numeroInterior.isBlank()) {
            texto = texto + " int. " + numeroInterior;
        }
        texto = texto + ", " + colonia + ", C.P. " + codigoPostal;
        return texto;
    }

    // true si tiene los mismos datos que otra dirección (sin contar el id)
    public boolean mismosDatos(Direccion otra) {
        if (otra == null) {
            return false;
        }
        return igual(calle, otra.calle) && igual(numeroExterior, otra.numeroExterior)
                && igual(numeroInterior, otra.numeroInterior) && igual(colonia, otra.colonia)
                && igual(codigoPostal, otra.codigoPostal) && igual(referencias, otra.referencias);
    }

    // Compara dos textos tomando null y vacío como iguales
    private boolean igual(String uno, String dos) {
        String a = "";
        String b = "";
        if (uno != null) {
            a = uno.trim();
        }
        if (dos != null) {
            b = dos.trim();
        }
        return a.equalsIgnoreCase(b);
    }

    public int getIdDireccion() {
        return idDireccion;
    }

    public void setIdDireccion(int idDireccion) {
        this.idDireccion = idDireccion;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getNumeroExterior() {
        return numeroExterior;
    }

    public void setNumeroExterior(String numeroExterior) {
        this.numeroExterior = numeroExterior;
    }

    public String getNumeroInterior() {
        return numeroInterior;
    }

    public void setNumeroInterior(String numeroInterior) {
        this.numeroInterior = numeroInterior;
    }

    public String getColonia() {
        return colonia;
    }

    public void setColonia(String colonia) {
        this.colonia = colonia;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getReferencias() {
        return referencias;
    }

    public void setReferencias(String referencias) {
        this.referencias = referencias;
    }
}

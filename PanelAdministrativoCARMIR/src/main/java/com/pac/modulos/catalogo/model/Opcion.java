package com.pac.modulos.catalogo.model;

/**
 * Representa una fila de la tabla OpcionCaracteristica (por ejemplo "Grande" o "Moño").
 *
 * - multiplicadorIngredientes: escala las opciones que "se escalan" (tamaño grande = 2).
 * - multiplicadorCosto: convierte el costo de la opción en precio de venta.
 * - seEscala: si su costo crece con el tamaño (glaseado sí, velas no).
 * - puntosSeleccion: puntos que gasta del presupuesto de su característica.
 * - multiplicadorPresupuesto: amplía el presupuesto de las características múltiples.
 */
public class Opcion {

    private int idOpcion;
    private int idCaracteristica;
    private String nombre;
    private String descripcion;
    private String linkImagen;
    private double multiplicadorIngredientes = 1;
    private double multiplicadorCosto = 3;
    private boolean seEscala;
    private boolean disponible = true;
    private double puntosSeleccion = 1;
    private double multiplicadorPresupuesto = 1;

    // Datos calculados (no son columnas)
    private String nombreCaracteristica;
    private boolean tieneReceta;
    private double costoReceta;

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public int getIdCaracteristica() {
        return idCaracteristica;
    }

    public void setIdCaracteristica(int idCaracteristica) {
        this.idCaracteristica = idCaracteristica;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getLinkImagen() {
        return linkImagen;
    }

    public void setLinkImagen(String linkImagen) {
        this.linkImagen = linkImagen;
    }

    public double getMultiplicadorIngredientes() {
        return multiplicadorIngredientes;
    }

    public void setMultiplicadorIngredientes(double multiplicadorIngredientes) {
        this.multiplicadorIngredientes = multiplicadorIngredientes;
    }

    public double getMultiplicadorCosto() {
        return multiplicadorCosto;
    }

    public void setMultiplicadorCosto(double multiplicadorCosto) {
        this.multiplicadorCosto = multiplicadorCosto;
    }

    public boolean isSeEscala() {
        return seEscala;
    }

    public void setSeEscala(boolean seEscala) {
        this.seEscala = seEscala;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public double getPuntosSeleccion() {
        return puntosSeleccion;
    }

    public void setPuntosSeleccion(double puntosSeleccion) {
        this.puntosSeleccion = puntosSeleccion;
    }

    public double getMultiplicadorPresupuesto() {
        return multiplicadorPresupuesto;
    }

    public void setMultiplicadorPresupuesto(double multiplicadorPresupuesto) {
        this.multiplicadorPresupuesto = multiplicadorPresupuesto;
    }

    public String getNombreCaracteristica() {
        return nombreCaracteristica;
    }

    public void setNombreCaracteristica(String nombreCaracteristica) {
        this.nombreCaracteristica = nombreCaracteristica;
    }

    public boolean isTieneReceta() {
        return tieneReceta;
    }

    public void setTieneReceta(boolean tieneReceta) {
        this.tieneReceta = tieneReceta;
    }

    public double getCostoReceta() {
        return costoReceta;
    }

    public void setCostoReceta(double costoReceta) {
        this.costoReceta = costoReceta;
    }

    // Así se muestra dentro de los ComboBox: "Sabor: Tres leches"
    @Override
    public String toString() {
        String texto = nombre;
        if (nombreCaracteristica != null) {
            texto = nombreCaracteristica + ": " + nombre;
        }
        if (!disponible) {
            texto = texto + " (no disponible)";
        }
        return texto;
    }
}

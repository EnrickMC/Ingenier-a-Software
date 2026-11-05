package com.pac.modulos.inventario.model;

/**
 * Opción de característica del catálogo vista desde el módulo de recetas.
 * Se usa en el formulario de "Nueva receta" para elegir la opción.
 */
public class OpcionReceta {

    private int idOpcion;
    private String nombreOpcion;
    private String nombreCaracteristica;
    private String nombreCategoria;
    private boolean disponible = true;
    // Si la característica de la opción admite recetas
    private boolean permiteReceta = true;
    private double multiplicadorCosto = 1;
    private int idRecetaInactiva;      // receta dada de baja que tenía la opción (0 = ninguna)

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public String getNombreOpcion() {
        return nombreOpcion;
    }

    public void setNombreOpcion(String nombreOpcion) {
        this.nombreOpcion = nombreOpcion;
    }

    public String getNombreCaracteristica() {
        return nombreCaracteristica;
    }

    public void setNombreCaracteristica(String nombreCaracteristica) {
        this.nombreCaracteristica = nombreCaracteristica;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public boolean isPermiteReceta() {
        return permiteReceta;
    }

    public void setPermiteReceta(boolean permiteReceta) {
        this.permiteReceta = permiteReceta;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public double getMultiplicadorCosto() {
        return multiplicadorCosto;
    }

    public void setMultiplicadorCosto(double multiplicadorCosto) {
        this.multiplicadorCosto = multiplicadorCosto;
    }

    public int getIdRecetaInactiva() {
        return idRecetaInactiva;
    }

    public void setIdRecetaInactiva(int idRecetaInactiva) {
        this.idRecetaInactiva = idRecetaInactiva;
    }

    // Así se muestra en el selector: "Pastel · Sabor: Chocolate"
    @Override
    public String toString() {
        String texto = nombreCategoria + " · " + nombreCaracteristica + ": " + nombreOpcion;
        if (idRecetaInactiva > 0) {
            texto = texto + " (tenía una receta dada de baja)";
        }
        return texto;
    }
}

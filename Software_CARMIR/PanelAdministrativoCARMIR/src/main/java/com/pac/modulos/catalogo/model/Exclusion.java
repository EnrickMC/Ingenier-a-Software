package com.pac.modulos.catalogo.model;

/**
 * Representa una fila de la tabla ExclusionOpciones:
 * dos opciones que no se pueden elegir juntas (por ejemplo tres leches con frambuesa).
 * A-B y B-A son la misma regla.
 */
public class Exclusion {

    private int idExclusion;
    private int idOpcion;
    private int idOpcionExcluida;
    private String motivo;
    private EstadoRegistro estado = EstadoRegistro.ACTIVA;

    // Textos para mostrar (vienen de un JOIN, no son columnas)
    private String nombreOpcion;
    private String nombreCaracteristica;
    private String nombreOpcionExcluida;
    private String nombreCaracteristicaExcluida;

    public int getIdExclusion() {
        return idExclusion;
    }

    public void setIdExclusion(int idExclusion) {
        this.idExclusion = idExclusion;
    }

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public int getIdOpcionExcluida() {
        return idOpcionExcluida;
    }

    public void setIdOpcionExcluida(int idOpcionExcluida) {
        this.idOpcionExcluida = idOpcionExcluida;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistro estado) {
        this.estado = estado;
    }

    public boolean estaActiva() {
        return estado == EstadoRegistro.ACTIVA;
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

    public String getNombreOpcionExcluida() {
        return nombreOpcionExcluida;
    }

    public void setNombreOpcionExcluida(String nombreOpcionExcluida) {
        this.nombreOpcionExcluida = nombreOpcionExcluida;
    }

    public String getNombreCaracteristicaExcluida() {
        return nombreCaracteristicaExcluida;
    }

    public void setNombreCaracteristicaExcluida(String nombreCaracteristicaExcluida) {
        this.nombreCaracteristicaExcluida = nombreCaracteristicaExcluida;
    }

    // true si la regla involucra a esa opción (en cualquiera de los dos lados)
    public boolean incluyeOpcion(int id) {
        return idOpcion == id || idOpcionExcluida == id;
    }

    // Texto corto: "Sabor: Tres leches  ✕  Relleno: Frambuesa"
    public String getDescripcionPar() {
        return nombreCaracteristica + ": " + nombreOpcion + "  ✕  "
                + nombreCaracteristicaExcluida + ": " + nombreOpcionExcluida;
    }
}

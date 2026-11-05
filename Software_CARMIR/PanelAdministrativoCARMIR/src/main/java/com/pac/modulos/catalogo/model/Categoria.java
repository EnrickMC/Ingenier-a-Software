package com.pac.modulos.catalogo.model;

/**
 * Representa una fila de la tabla CategoriaPostre (por ejemplo Pastel o Gelatina).
 */
public class Categoria {

    private int idCategoria;
    private String nombre;
    private String descripcion;
    private EstadoRegistro estado = EstadoRegistro.ACTIVA;

    // Dato calculado (no es columna): cuántos productos del catálogo tiene
    private int numeroProductos;

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
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

    public EstadoRegistro getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistro estado) {
        this.estado = estado;
    }

    public boolean estaActiva() {
        return estado == EstadoRegistro.ACTIVA;
    }

    public int getNumeroProductos() {
        return numeroProductos;
    }

    public void setNumeroProductos(int numeroProductos) {
        this.numeroProductos = numeroProductos;
    }

    // Así se muestra dentro de los ComboBox
    @Override
    public String toString() {
        if (estaActiva()) {
            return nombre;
        }
        return nombre + " (inactiva)";
    }
}

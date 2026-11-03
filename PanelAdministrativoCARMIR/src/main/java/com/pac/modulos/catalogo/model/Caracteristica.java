package com.pac.modulos.catalogo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una fila de la tabla Caracteristica (Tipo, Tamaño, Sabor, Decoración...).
 * Guarda las reglas con las que el cliente elige sus opciones.
 */
public class Caracteristica {

    private int idCaracteristica;
    private int idCategoria;
    private String nombre;
    private String descripcion;
    private boolean obligatoria;
    private boolean seleccionMultiple;
    private double presupuestoSeleccion = 1;
    private int ordenMostrar = 1;
    private boolean disponible = true;
    // Indica si las opciones de esta característica pueden tener receta
    private boolean permiteReceta = true;

    // Datos calculados (no son columnas)
    private int numeroOpciones;
    private List<Opcion> opciones = new ArrayList<Opcion>();

    public int getIdCaracteristica() {
        return idCaracteristica;
    }

    public void setIdCaracteristica(int idCaracteristica) {
        this.idCaracteristica = idCaracteristica;
    }

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

    public boolean isObligatoria() {
        return obligatoria;
    }

    public void setObligatoria(boolean obligatoria) {
        this.obligatoria = obligatoria;
    }

    public boolean isSeleccionMultiple() {
        return seleccionMultiple;
    }

    public void setSeleccionMultiple(boolean seleccionMultiple) {
        this.seleccionMultiple = seleccionMultiple;
    }

    public double getPresupuestoSeleccion() {
        return presupuestoSeleccion;
    }

    public void setPresupuestoSeleccion(double presupuestoSeleccion) {
        this.presupuestoSeleccion = presupuestoSeleccion;
    }

    public int getOrdenMostrar() {
        return ordenMostrar;
    }

    public void setOrdenMostrar(int ordenMostrar) {
        this.ordenMostrar = ordenMostrar;
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

    public int getNumeroOpciones() {
        return numeroOpciones;
    }

    public void setNumeroOpciones(int numeroOpciones) {
        this.numeroOpciones = numeroOpciones;
    }

    public List<Opcion> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<Opcion> opciones) {
        this.opciones = opciones;
    }

    // Así se muestra dentro de los ComboBox
    @Override
    public String toString() {
        if (disponible) {
            return nombre;
        }
        return nombre + " (no disponible)";
    }
}

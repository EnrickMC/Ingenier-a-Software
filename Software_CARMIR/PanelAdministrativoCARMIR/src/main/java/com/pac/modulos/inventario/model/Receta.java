package com.pac.modulos.inventario.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una fila de la tabla Receta: la receta de una opción de característica
 * (por ejemplo el sabor "Chocolate"), con sus ingredientes de la tabla RecetaInsumo.
 *
 * Cada opción tiene como máximo una receta (uq_receta_idopcion).
 * El costo no se guarda: es la suma de cantidad × costo unitario de sus ingredientes
 * (igual que la vista vw_CostoReceta).
 */
public class Receta {

    private int idReceta;
    private int idOpcion;
    private String nombre;
    private String pasos;
    private int tiempoMinutos;
    private boolean activa = true;
    private List<IngredienteReceta> ingredientes = new ArrayList<IngredienteReceta>();

    // Datos de la opción (no son columnas de Receta)
    private String nombreOpcion;
    private String nombreCaracteristica;
    private String nombreCategoria;
    private boolean opcionDisponible = true;
    private double multiplicadorCosto = 1;

    // Datos calculados al listar (cuando no se leen los ingredientes)
    private double costoListado;
    private int insumosDadosDeBaja;

    public int getIdReceta() {
        return idReceta;
    }

    public void setIdReceta(int idReceta) {
        this.idReceta = idReceta;
    }

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPasos() {
        return pasos;
    }

    public void setPasos(String pasos) {
        this.pasos = pasos;
    }

    public int getTiempoMinutos() {
        return tiempoMinutos;
    }

    public void setTiempoMinutos(int tiempoMinutos) {
        this.tiempoMinutos = tiempoMinutos;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public List<IngredienteReceta> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(List<IngredienteReceta> ingredientes) {
        this.ingredientes = ingredientes;
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

    public boolean isOpcionDisponible() {
        return opcionDisponible;
    }

    public void setOpcionDisponible(boolean opcionDisponible) {
        this.opcionDisponible = opcionDisponible;
    }

    public double getMultiplicadorCosto() {
        return multiplicadorCosto;
    }

    public void setMultiplicadorCosto(double multiplicadorCosto) {
        this.multiplicadorCosto = multiplicadorCosto;
    }

    public double getCostoListado() {
        return costoListado;
    }

    public void setCostoListado(double costoListado) {
        this.costoListado = costoListado;
    }

    public int getInsumosDadosDeBaja() {
        return insumosDadosDeBaja;
    }

    public void setInsumosDadosDeBaja(int insumosDadosDeBaja) {
        this.insumosDadosDeBaja = insumosDadosDeBaja;
    }

    // Costo de la receta: si se leyeron los ingredientes se suman; si no, se usa el del listado
    public double getCosto() {
        if (ingredientes.isEmpty()) {
            return costoListado;
        }
        double total = 0;
        for (IngredienteReceta ingrediente : ingredientes) {
            total = total + ingrediente.getCosto();
        }
        return Math.round(total * 100) / 100.0;
    }

    // Precio base de la opción = costo de la receta × multiplicador de costo de la opción
    public double getPrecioBase() {
        return Math.round(getCosto() * multiplicadorCosto * 100) / 100.0;
    }

    // RF-54 / RF-55: la receta "requiere revisión" si alguno de sus insumos se dio de baja
    public boolean requiereRevision() {
        if (ingredientes.isEmpty()) {
            return insumosDadosDeBaja > 0;
        }
        for (IngredienteReceta ingrediente : ingredientes) {
            if (!ingrediente.isInsumoActivo()) {
                return true;
            }
        }
        return false;
    }

    // Texto de la opción: "Pastel · Sabor: Chocolate"
    public String getDescripcionOpcion() {
        String texto = "";
        if (nombreCategoria != null) {
            texto = nombreCategoria + " · ";
        }
        if (nombreCaracteristica != null) {
            texto = texto + nombreCaracteristica + ": ";
        }
        return texto + nombreOpcion;
    }
}

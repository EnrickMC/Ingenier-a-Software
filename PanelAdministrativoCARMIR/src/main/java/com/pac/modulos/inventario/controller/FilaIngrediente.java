package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de ingredientes del formulario de receta.
 */
public class FilaIngrediente {

    private IngredienteReceta ingrediente;

    public FilaIngrediente(IngredienteReceta ingrediente) {
        this.ingrediente = ingrediente;
    }

    public IngredienteReceta getIngrediente() {
        return ingrediente;
    }

    public String getInsumo() {
        if (!ingrediente.isInsumoActivo()) {
            return ingrediente.getNombreInsumo() + " (dado de baja)";
        }
        return ingrediente.getNombreInsumo();
    }

    public String getCantidad() {
        String unidad = "";
        if (ingrediente.getUnidad() != null) {
            unidad = " " + ingrediente.getUnidad().getValorBD();
        }
        return Formatos.cantidad(ingrediente.getCantidad()) + unidad;
    }

    public String getCosto() {
        return Formatos.moneda(ingrediente.getCosto());
    }
}

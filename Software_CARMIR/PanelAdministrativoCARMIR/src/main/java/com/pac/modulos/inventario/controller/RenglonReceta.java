package com.pac.modulos.inventario.controller;

import javafx.scene.control.TableRow;

/**
 * Renglón de la tabla de recetas: ámbar = requiere revisión, gris = dada de baja.
 */
public class RenglonReceta extends TableRow<FilaReceta> {

    // JavaFX llama a este método cada vez que el renglón muestra otra fila
    @Override
    protected void updateItem(FilaReceta fila, boolean vacio) {
        super.updateItem(fila, vacio);
        if (vacio || fila == null) {
            EstilosInventario.aplicarClaseFila(this, null);
            return;
        }
        EstilosInventario.aplicarClaseFila(this, fila.getClaseEstilo());
    }
}

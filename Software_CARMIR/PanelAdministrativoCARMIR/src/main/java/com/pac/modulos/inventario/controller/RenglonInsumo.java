package com.pac.modulos.inventario.controller;

import javafx.scene.control.TableRow;

/**
 * Renglón de la tabla de insumos: se pinta de color según el estado del insumo
 * (durazno = stock bajo, rojo = agotado o caducado, ámbar = próximo a caducar, gris = dado de baja).
 */
public class RenglonInsumo extends TableRow<FilaInsumo> {

    // JavaFX llama a este método cada vez que el renglón muestra otra fila
    @Override
    protected void updateItem(FilaInsumo fila, boolean vacio) {
        super.updateItem(fila, vacio);
        if (vacio || fila == null) {
            EstilosInventario.aplicarClaseFila(this, null);
            return;
        }
        EstilosInventario.aplicarClaseFila(this, fila.getClaseEstilo());
    }
}

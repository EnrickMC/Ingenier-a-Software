package com.pac.modulos.inventario.controller;

import javafx.scene.control.TableRow;

/**
 * Renglón de la tabla de alertas: rojo = agotado o caducado, durazno = stock bajo,
 * ámbar = próximo a caducar, gris = ya atendida.
 */
public class RenglonAlerta extends TableRow<FilaAlerta> {

    // JavaFX llama a este método cada vez que el renglón muestra otra fila
    @Override
    protected void updateItem(FilaAlerta fila, boolean vacio) {
        super.updateItem(fila, vacio);
        if (vacio || fila == null) {
            EstilosInventario.aplicarClaseFila(this, null);
            return;
        }
        EstilosInventario.aplicarClaseFila(this, EstilosInventario.claseFilaAlerta(fila.getAlerta()));
    }
}

package com.pac.modulos.pedidos.controller;

import com.pac.modulos.inventario.controller.EstilosInventario;
import com.pac.modulos.pedidos.model.EstadoPago;
import javafx.scene.control.TableRow;

/**
 * Renglón de la tabla de pagos: ámbar = pendiente de validar, rojo claro = rechazado
 * o de un pedido cancelado (usa las clases de inventario.css).
 */
public class RenglonPago extends TableRow<FilaPago> {

    // JavaFX llama a este método cada vez que el renglón muestra otra fila
    @Override
    protected void updateItem(FilaPago fila, boolean vacio) {
        super.updateItem(fila, vacio);
        if (vacio || fila == null) {
            EstilosInventario.aplicarClaseFila(this, null);
            return;
        }
        String clase = null;
        if (fila.getPago().getEstado() == EstadoPago.PENDIENTE) {
            clase = EstilosInventario.FILA_PROXIMA;
        }
        if (fila.getPago().getEstado() == EstadoPago.RECHAZADO || fila.getPago().isPedidoCancelado()) {
            clase = EstilosInventario.FILA_CRITICA;
        }
        EstilosInventario.aplicarClaseFila(this, clase);
    }
}

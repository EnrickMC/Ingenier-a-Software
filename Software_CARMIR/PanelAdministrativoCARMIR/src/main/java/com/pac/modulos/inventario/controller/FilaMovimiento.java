package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.Movimiento;
import com.pac.util.Formatos;

/**
 * Una fila del historial de movimientos (pestaña Lotes y movimientos).
 */
public class FilaMovimiento {

    private Movimiento movimiento;

    public FilaMovimiento(Movimiento movimiento) {
        this.movimiento = movimiento;
    }

    public Movimiento getMovimiento() {
        return movimiento;
    }

    public String getFecha() {
        return Formatos.fechaHoraCorta(movimiento.getFecha());
    }

    public String getInsumo() {
        return movimiento.getNombreInsumo();
    }

    public String getTipo() {
        return movimiento.getTipo().toString();
    }

    // Cantidad con signo y unidad: "+5 kg" o "-0.25 kg"
    public String getCantidad() {
        String signo = "";
        if (movimiento.getCantidad() > 0) {
            signo = "+";
        }
        return signo + Formatos.cantidad(movimiento.getCantidad()) + " " + movimiento.getUnidad().getValorBD();
    }

    public String getCosto() {
        return Formatos.moneda(movimiento.getCosto());
    }

    public String getFolio() {
        if (movimiento.getFolio() <= 0) {
            return "—";
        }
        return "#" + movimiento.getFolio();
    }

    public String getMotivo() {
        if (movimiento.getMotivo() == null) {
            return "";
        }
        return movimiento.getMotivo();
    }

    public String getUsuario() {
        return movimiento.getNombreUsuario();
    }
}

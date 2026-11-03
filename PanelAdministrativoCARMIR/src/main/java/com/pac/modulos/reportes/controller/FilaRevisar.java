package com.pac.modulos.reportes.controller;

import com.pac.modulos.reportes.model.MovimientoRevisar;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla "Movimientos por revisar" (pestaña Ingresos y gastos).
 */
public class FilaRevisar {

    private MovimientoRevisar movimiento;

    public FilaRevisar(MovimientoRevisar movimiento) {
        this.movimiento = movimiento;
    }

    public String getFecha() {
        return Formatos.fechaHoraCorta(movimiento.getFecha());
    }

    public String getOrigen() {
        return movimiento.getOrigen();
    }

    public String getReferencia() {
        return movimiento.getReferencia();
    }

    public String getMonto() {
        return Formatos.moneda(movimiento.getMonto());
    }

    public String getMotivo() {
        return movimiento.getMotivo();
    }
}

package com.pac.modulos.reportes.controller;

import com.pac.modulos.reportes.model.RenglonDia;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla "Detalle por fecha" (pestaña Ingresos y gastos).
 */
public class FilaDia {

    private RenglonDia dia;

    public FilaDia(RenglonDia dia) {
        this.dia = dia;
    }

    public RenglonDia getDia() {
        return dia;
    }

    public String getFecha() {
        return Formatos.fecha(dia.getFecha());
    }

    public String getCobros() {
        return Formatos.moneda(dia.getCobros());
    }

    public String getReembolsos() {
        if (dia.getReembolsos() == 0) {
            return "—";
        }
        return "-" + Formatos.moneda(dia.getReembolsos());
    }

    public String getCompras() {
        return Formatos.moneda(dia.getCompras());
    }

    public String getMermas() {
        return Formatos.moneda(dia.getMermas());
    }

    public String getUtilidad() {
        return Formatos.moneda(dia.getUtilidad());
    }
}

package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.service.InventarioService;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de lotes (pestaña Lotes y movimientos).
 */
public class FilaLote {

    private Lote lote;

    public FilaLote(Lote lote) {
        this.lote = lote;
    }

    public Lote getLote() {
        return lote;
    }

    public String getClave() {
        return lote.getClave();
    }

    public String getInsumo() {
        return lote.getNombreInsumo();
    }

    // Lo que queda del lote, con su unidad
    public String getExistencia() {
        return Formatos.cantidad(lote.getExistencia()) + " " + lote.getUnidad().getValorBD();
    }

    public String getCompra() {
        return Formatos.fecha(lote.getFechaCompra());
    }

    // Fecha de caducidad y días que faltan, por ejemplo "05/11/2026 (4 días)"
    public String getCaducidad() {
        if (lote.getFechaCaducidad() == null) {
            return "No caduca";
        }
        int dias = InventarioService.diasPara(lote.getFechaCaducidad());
        String texto = Formatos.fecha(lote.getFechaCaducidad());
        if (dias < 0) {
            return texto + " (caducado)";
        }
        if (dias == 0) {
            return texto + " (hoy)";
        }
        return texto + " (" + dias + " días)";
    }
}

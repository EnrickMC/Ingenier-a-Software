package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.service.InsumoService;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de insumos (insumos.fxml).
 */
public class FilaInsumo {

    private Insumo insumo;
    private String estado;

    public FilaInsumo(Insumo insumo, String estado) {
        this.insumo = insumo;
        this.estado = estado;
    }

    public Insumo getInsumo() {
        return insumo;
    }

    public String getClave() {
        return insumo.getClave();
    }

    public String getNombre() {
        return insumo.getNombre();
    }

    public String getCategoria() {
        return insumo.getCategoria();
    }

    public String getExistencia() {
        return Formatos.cantidad(insumo.getExistencia());
    }

    public String getUnidad() {
        return insumo.getUnidad().getValorBD();
    }

    public String getStockMinimo() {
        if (!insumo.tieneStockMinimo()) {
            return "—";
        }
        return Formatos.cantidad(insumo.getStockMinimo());
    }

    public String getCosto() {
        return Formatos.moneda(insumo.getCostoUnitario());
    }

    public String getPerecedero() {
        if (insumo.isPerecedero()) {
            return "Sí";
        }
        return "No";
    }

    public String getEstado() {
        return estado;
    }

    // Clase CSS del renglón según el estado (durazno, rojo, ámbar o gris)
    public String getClaseEstilo() {
        return EstilosInventario.claseFilaInsumo(estado);
    }

    // Clase CSS de la etiqueta del estado en el detalle
    public String getClaseChip() {
        return EstilosInventario.claseChipInsumo(estado);
    }

    // true si el estado es el de un insumo que necesita atención
    public boolean necesitaAtencion() {
        return !InsumoService.ESTADO_NORMAL.equals(estado) && !InsumoService.ESTADO_SIN_MINIMO.equals(estado)
                && !InsumoService.ESTADO_INACTIVO.equals(estado);
    }
}

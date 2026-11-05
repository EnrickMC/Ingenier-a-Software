package com.pac.modulos.reportes.controller;

import com.pac.modulos.reportes.model.RenglonVenta;
import com.pac.util.Formatos;

import java.util.Locale;

/**
 * Una fila de las tablas de la pestaña Más vendidos (productos, categorías u opciones).
 */
public class FilaVenta {

    private RenglonVenta renglon;
    private int posicion;
    private boolean esProducto;

    public FilaVenta(RenglonVenta renglon, int posicion, boolean esProducto) {
        this.renglon = renglon;
        this.posicion = posicion;
        this.esProducto = esProducto;
    }

    public RenglonVenta getRenglon() {
        return renglon;
    }

    public String getPosicion() {
        return String.valueOf(posicion);
    }

    public String getNombre() {
        return renglon.getNombre();
    }

    public String getGrupo() {
        return renglon.getGrupo();
    }

    public String getUnidades() {
        return String.valueOf(renglon.getUnidades());
    }

    public String getIngresos() {
        return Formatos.moneda(renglon.getIngresos());
    }

    public String getUtilidad() {
        return Formatos.moneda(renglon.getUtilidad());
    }

    public String getMargen() {
        return String.format(Locale.US, "%.1f %%", renglon.getMargen());
    }

    public String getPorcentaje() {
        return String.format(Locale.US, "%.1f %%", renglon.getPorcentajeIngresos());
    }

    // Productos: «Producto inactivo», «Oculto en tienda» o «Activo»; categorías y opciones: Activa / Inactiva
    public String getEstado() {
        if (esProducto) {
            return renglon.getEstadoProducto();
        }
        return renglon.getEstado();
    }
}

package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Producto;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de productos (productos.fxml).
 */
public class FilaProducto {

    private Producto producto;

    public FilaProducto(Producto producto) {
        this.producto = producto;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getSku() {
        return producto.getSku();
    }

    public String getNombre() {
        return producto.getNombre();
    }

    public String getCategoria() {
        if (producto.isCategoriaActiva()) {
            return producto.getNombreCategoria();
        }
        return producto.getNombreCategoria() + " (inactiva)";
    }

    // Precio calculado con vw_CostoPrecioPostre; 0 = sus opciones aún no tienen receta
    public String getPrecio() {
        if (producto.getPrecioVenta() <= 0) {
            return "Sin receta";
        }
        return Formatos.moneda(producto.getPrecioVenta());
    }

    public String getCosto() {
        if (producto.getCostoIngredientes() <= 0) {
            return "—";
        }
        return Formatos.moneda(producto.getCostoIngredientes());
    }

    public String getEnTienda() {
        if (producto.isMostrarEnTienda()) {
            return "Visible";
        }
        return "Oculto";
    }
}

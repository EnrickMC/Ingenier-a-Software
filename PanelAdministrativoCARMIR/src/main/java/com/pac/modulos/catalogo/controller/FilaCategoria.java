package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Categoria;

/**
 * Una fila de la tabla de categorías.
 * La tabla lee estos getters por su nombre (PropertyValueFactory en categorias.fxml).
 */
public class FilaCategoria {

    private Categoria categoria;

    public FilaCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getNombre() {
        return categoria.getNombre();
    }

    public String getDescripcion() {
        return ApoyoVista.texto(categoria.getDescripcion());
    }

    public String getEstado() {
        return categoria.getEstado().getValorBD();
    }

    public int getProductos() {
        return categoria.getNumeroProductos();
    }
}

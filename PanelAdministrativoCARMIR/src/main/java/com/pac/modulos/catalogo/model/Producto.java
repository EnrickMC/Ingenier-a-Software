package com.pac.modulos.catalogo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un producto del catálogo: una fila de la tabla Postre con nombre comercial,
 * más las opciones que lo forman (tabla PostreOpcion).
 *
 * El precio no se guarda: se calcula con las vistas vw_CostoReceta y vw_CostoPrecioPostre.
 * La tabla no tiene columna SKU; el SKU se arma con el id (por ejemplo PST-0007).
 */
public class Producto {

    private int idPostre;
    private int idCategoria;
    private String claveConfiguracion;
    private String nombre;
    private String descripcion;
    private String linkFotografia;
    private boolean mostrarEnTienda;

    // Datos calculados (no son columnas)
    private String nombreCategoria;
    private boolean categoriaActiva = true;
    private double costoIngredientes;
    private double precioVenta;
    private List<OpcionElegida> opciones = new ArrayList<OpcionElegida>();

    public int getIdPostre() {
        return idPostre;
    }

    public void setIdPostre(int idPostre) {
        this.idPostre = idPostre;
    }

    // SKU para mostrar, armado con el id: PST-0001
    public String getSku() {
        if (idPostre <= 0) {
            return "Nuevo";
        }
        return String.format("PST-%04d", idPostre);
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getClaveConfiguracion() {
        return claveConfiguracion;
    }

    public void setClaveConfiguracion(String claveConfiguracion) {
        this.claveConfiguracion = claveConfiguracion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getLinkFotografia() {
        return linkFotografia;
    }

    public void setLinkFotografia(String linkFotografia) {
        this.linkFotografia = linkFotografia;
    }

    public boolean tieneFotografia() {
        return linkFotografia != null && !linkFotografia.isBlank();
    }

    public boolean isMostrarEnTienda() {
        return mostrarEnTienda;
    }

    public void setMostrarEnTienda(boolean mostrarEnTienda) {
        this.mostrarEnTienda = mostrarEnTienda;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public boolean isCategoriaActiva() {
        return categoriaActiva;
    }

    public void setCategoriaActiva(boolean categoriaActiva) {
        this.categoriaActiva = categoriaActiva;
    }

    public double getCostoIngredientes() {
        return costoIngredientes;
    }

    public void setCostoIngredientes(double costoIngredientes) {
        this.costoIngredientes = costoIngredientes;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public List<OpcionElegida> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<OpcionElegida> opciones) {
        this.opciones = opciones;
    }
}

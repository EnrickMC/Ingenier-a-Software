package com.pac.modulos.pedidos.model;

import com.pac.modulos.catalogo.model.OpcionElegida;

import java.util.ArrayList;
import java.util.List;

/**
 * Un postre dentro de un pedido (tabla DetallePedido) con su personalización.
 * El precio y el costo están congelados al momento del pedido.
 */
public class LineaPedido {

    private int folio;
    private int idPostre;
    private int idCategoria;
    private String nombreCategoria;
    private String nombrePostre;        // null si es un postre personalizado (sin nombre de catálogo)
    private String linkFotografia;      // foto del postre del catálogo (puede ser null)
    private int cantidad;
    private String dedicatoria;
    private String comentario;
    private double precioUnitario;
    private double costoUnitario;

    // "Característica: Opción" de cada opción elegida, para mostrar la personalización
    private List<String> especificaciones = new ArrayList<String>();
    // Enlaces a las imágenes de referencia de las opciones
    private List<String> imagenes = new ArrayList<String>();
    // Opciones con todos sus datos; solo se cargan al modificar el pedido
    private List<OpcionElegida> opciones = new ArrayList<OpcionElegida>();
    // Al modificar: true si la personalización cambió (se toma el precio actual)
    private boolean personalizacionCambiada = false;

    // Nombre que se muestra: el del catálogo o "Pastel personalizado"
    public String getNombreMostrar() {
        if (nombrePostre != null && !nombrePostre.isBlank()) {
            return nombrePostre;
        }
        if (nombreCategoria != null) {
            return nombreCategoria + " personalizado";
        }
        return "Postre personalizado";
    }

    public double getSubtotal() {
        return Math.round(cantidad * precioUnitario * 100) / 100.0;
    }

    // Personalización en una sola línea, separada por " · "
    public String getTextoEspecificaciones() {
        String texto = "";
        for (int i = 0; i < especificaciones.size(); i++) {
            if (i > 0) {
                texto = texto + " · ";
            }
            texto = texto + especificaciones.get(i);
        }
        return texto;
    }

    // Copia de la línea (para editarla sin tocar la original)
    public LineaPedido copiar() {
        LineaPedido copia = new LineaPedido();
        copia.folio = folio;
        copia.idPostre = idPostre;
        copia.idCategoria = idCategoria;
        copia.nombreCategoria = nombreCategoria;
        copia.nombrePostre = nombrePostre;
        copia.linkFotografia = linkFotografia;
        copia.cantidad = cantidad;
        copia.dedicatoria = dedicatoria;
        copia.comentario = comentario;
        copia.precioUnitario = precioUnitario;
        copia.costoUnitario = costoUnitario;
        copia.especificaciones = new ArrayList<String>(especificaciones);
        copia.imagenes = new ArrayList<String>(imagenes);
        copia.opciones = new ArrayList<OpcionElegida>(opciones);
        copia.personalizacionCambiada = personalizacionCambiada;
        return copia;
    }

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
    }

    public int getIdPostre() {
        return idPostre;
    }

    public void setIdPostre(int idPostre) {
        this.idPostre = idPostre;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getNombrePostre() {
        return nombrePostre;
    }

    public void setNombrePostre(String nombrePostre) {
        this.nombrePostre = nombrePostre;
    }

    public String getLinkFotografia() {
        return linkFotografia;
    }

    public void setLinkFotografia(String linkFotografia) {
        this.linkFotografia = linkFotografia;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getDedicatoria() {
        return dedicatoria;
    }

    public void setDedicatoria(String dedicatoria) {
        this.dedicatoria = dedicatoria;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public List<String> getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(List<String> especificaciones) {
        this.especificaciones = especificaciones;
    }

    public List<String> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<String> imagenes) {
        this.imagenes = imagenes;
    }

    public List<OpcionElegida> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<OpcionElegida> opciones) {
        this.opciones = opciones;
    }

    public boolean isPersonalizacionCambiada() {
        return personalizacionCambiada;
    }

    public void setPersonalizacionCambiada(boolean personalizacionCambiada) {
        this.personalizacionCambiada = personalizacionCambiada;
    }
}

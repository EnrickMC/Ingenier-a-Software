package com.pac.modulos.reportes.model;

/**
 * Un renglón del reporte de productos más vendidos (RF-66):
 * un producto, una categoría o una opción de personalización.
 */
public class RenglonVenta {

    private String nombre;          // producto, categoría u opción
    private String grupo;           // categoría del producto o característica de la opción
    private int unidades;           // piezas vendidas
    private double ingresos;        // Σ PrecioUnitario × Cantidad
    private double costo;           // Σ CostoUnitario × Cantidad
    private boolean inactivo;       // ya no se puede vender (categoría inactiva u opción no disponible)
    private boolean oculto;         // producto del catálogo que no se muestra en la tienda
    private double porcentajeIngresos;  // parte de los ingresos totales del periodo (0 a 100)

    public RenglonVenta(String nombre, String grupo, int unidades, double ingresos, double costo) {
        this.nombre = nombre;
        this.grupo = grupo;
        this.unidades = unidades;
        this.ingresos = ingresos;
        this.costo = costo;
    }

    // Utilidad = ingresos − costo
    public double getUtilidad() {
        return ingresos - costo;
    }

    // Margen = utilidad / ingresos × 100 (0 si no hubo ingresos)
    public double getMargen() {
        if (ingresos <= 0) {
            return 0;
        }
        return getUtilidad() / ingresos * 100;
    }

    public String getNombre() {
        return nombre;
    }

    public String getGrupo() {
        return grupo;
    }

    public int getUnidades() {
        return unidades;
    }

    public double getIngresos() {
        return ingresos;
    }

    public double getCosto() {
        return costo;
    }

    public boolean isInactivo() {
        return inactivo;
    }

    public void setInactivo(boolean inactivo) {
        this.inactivo = inactivo;
    }

    public boolean isOculto() {
        return oculto;
    }

    public void setOculto(boolean oculto) {
        this.oculto = oculto;
    }

    // Texto del estado de un producto: «Producto inactivo» (RF-66), «Oculto en tienda» o «Activo»
    public String getEstadoProducto() {
        if (inactivo) {
            return "Producto inactivo";
        }
        if (oculto) {
            return "Oculto en tienda";
        }
        return "Activo";
    }

    // Texto del estado de una categoría u opción
    public String getEstado() {
        if (inactivo) {
            return "Inactiva";
        }
        return "Activa";
    }

    public double getPorcentajeIngresos() {
        return porcentajeIngresos;
    }

    public void setPorcentajeIngresos(double porcentajeIngresos) {
        this.porcentajeIngresos = porcentajeIngresos;
    }
}

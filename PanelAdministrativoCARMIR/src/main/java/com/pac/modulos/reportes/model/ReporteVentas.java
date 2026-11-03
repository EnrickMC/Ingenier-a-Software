package com.pac.modulos.reportes.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de productos más vendidos de un periodo (RF-47).
 * Solo cuenta pedidos pagados (Pagado o un estado posterior) que no están cancelados
 * ni dados de baja. La fecha que se usa es la de la solicitud del pedido.
 */
public class ReporteVentas {

    private LocalDate desde;
    private LocalDate hasta;
    private int idCategoria;            // 0 = todas las categorías
    private String nombreCategoria;     // texto del filtro, por ejemplo "Todas las categorías"
    private String orden;               // texto del orden elegido
    private LocalDateTime fechaGeneracion;
    private int lineasIncompletas;      // ventas que no se incluyeron por datos incompletos
    private int pedidos;                // pedidos que entraron al reporte

    private List<RenglonVenta> porProducto = new ArrayList<RenglonVenta>();
    private List<RenglonVenta> porCategoria = new ArrayList<RenglonVenta>();
    private List<RenglonVenta> porOpcion = new ArrayList<RenglonVenta>();

    public ReporteVentas(LocalDate desde, LocalDate hasta) {
        this.desde = desde;
        this.hasta = hasta;
        this.fechaGeneracion = LocalDateTime.now();
    }

    // ----- Totales (se suman los productos) -----

    public int getUnidadesTotales() {
        int total = 0;
        for (RenglonVenta renglon : porProducto) {
            total = total + renglon.getUnidades();
        }
        return total;
    }

    public double getIngresosTotales() {
        double total = 0;
        for (RenglonVenta renglon : porProducto) {
            total = total + renglon.getIngresos();
        }
        return total;
    }

    public double getCostoTotal() {
        double total = 0;
        for (RenglonVenta renglon : porProducto) {
            total = total + renglon.getCosto();
        }
        return total;
    }

    public double getUtilidadTotal() {
        return getIngresosTotales() - getCostoTotal();
    }

    public double getMargenTotal() {
        if (getIngresosTotales() <= 0) {
            return 0;
        }
        return getUtilidadTotal() / getIngresosTotales() * 100;
    }

    public boolean tieneDatos() {
        return !porProducto.isEmpty();
    }

    // ----- Getters y setters -----

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
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

    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = orden;
    }

    public int getLineasIncompletas() {
        return lineasIncompletas;
    }

    public void setLineasIncompletas(int lineasIncompletas) {
        this.lineasIncompletas = lineasIncompletas;
    }

    public int getPedidos() {
        return pedidos;
    }

    public void setPedidos(int pedidos) {
        this.pedidos = pedidos;
    }

    public List<RenglonVenta> getPorProducto() {
        return porProducto;
    }

    public void setPorProducto(List<RenglonVenta> porProducto) {
        this.porProducto = porProducto;
    }

    public List<RenglonVenta> getPorCategoria() {
        return porCategoria;
    }

    public void setPorCategoria(List<RenglonVenta> porCategoria) {
        this.porCategoria = porCategoria;
    }

    public List<RenglonVenta> getPorOpcion() {
        return porOpcion;
    }

    public void setPorOpcion(List<RenglonVenta> porOpcion) {
        this.porOpcion = porOpcion;
    }
}

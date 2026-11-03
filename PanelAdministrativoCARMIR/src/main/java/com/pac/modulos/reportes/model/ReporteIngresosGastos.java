package com.pac.modulos.reportes.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de ingresos y gastos de un periodo (RF-46).
 *
 * Ingresos = pagos validados (anticipos + liquidaciones + pagos totales) − reembolsos realizados.
 * Gastos   = compras de insumos + mermas.
 * Utilidad bruta = ingresos − gastos (si es negativa es «Pérdida en el periodo»).
 * El costo de producción (insumos usados en pedidos) solo se muestra como dato informativo.
 */
public class ReporteIngresosGastos {

    private LocalDate desde;
    private LocalDate hasta;
    private LocalDateTime fechaGeneracion;

    private double anticipos;
    private double liquidaciones;
    private double pagosTotales;
    private double reembolsos;
    private double compras;
    private double mermas;
    private double produccion;          // informativo: no se suma a los gastos
    private double porReembolsar;       // pagos de pedidos cancelados con reembolso pendiente (aparte)
    private int numeroPagos;
    private int numeroCompras;
    private int numeroMermas;

    private List<RenglonDia> dias = new ArrayList<RenglonDia>();
    private List<MovimientoRevisar> porRevisar = new ArrayList<MovimientoRevisar>();

    public ReporteIngresosGastos(LocalDate desde, LocalDate hasta) {
        this.desde = desde;
        this.hasta = hasta;
        this.fechaGeneracion = LocalDateTime.now();
    }

    // ----- Totales calculados -----

    // Cobros del periodo (antes de restar reembolsos)
    public double getCobros() {
        return anticipos + liquidaciones + pagosTotales;
    }

    public double getIngresos() {
        return getCobros() - reembolsos;
    }

    public double getGastos() {
        return compras + mermas;
    }

    public double getUtilidad() {
        return getIngresos() - getGastos();
    }

    public boolean esPerdida() {
        return getUtilidad() < 0;
    }

    // true si en el periodo hubo pagos o reembolsos
    public boolean hayIngresos() {
        return numeroPagos > 0 || reembolsos > 0;
    }

    // true si en el periodo hubo compras o mermas
    public boolean hayGastos() {
        return numeroCompras > 0 || numeroMermas > 0;
    }

    // true si no hubo ninguna transacción en el periodo (ni siquiera por revisar)
    public boolean estaVacio() {
        return !hayIngresos() && !hayGastos() && produccion == 0 && porReembolsar == 0 && porRevisar.isEmpty();
    }

    // true si hay algo que exportar
    public boolean tieneDatos() {
        return !estaVacio();
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

    public double getAnticipos() {
        return anticipos;
    }

    public void setAnticipos(double anticipos) {
        this.anticipos = anticipos;
    }

    public double getLiquidaciones() {
        return liquidaciones;
    }

    public void setLiquidaciones(double liquidaciones) {
        this.liquidaciones = liquidaciones;
    }

    public double getPagosTotales() {
        return pagosTotales;
    }

    public void setPagosTotales(double pagosTotales) {
        this.pagosTotales = pagosTotales;
    }

    public double getReembolsos() {
        return reembolsos;
    }

    public void setReembolsos(double reembolsos) {
        this.reembolsos = reembolsos;
    }

    public double getCompras() {
        return compras;
    }

    public void setCompras(double compras) {
        this.compras = compras;
    }

    public double getMermas() {
        return mermas;
    }

    public void setMermas(double mermas) {
        this.mermas = mermas;
    }

    public double getProduccion() {
        return produccion;
    }

    public void setProduccion(double produccion) {
        this.produccion = produccion;
    }

    public double getPorReembolsar() {
        return porReembolsar;
    }

    public void setPorReembolsar(double porReembolsar) {
        this.porReembolsar = porReembolsar;
    }

    public int getNumeroPagos() {
        return numeroPagos;
    }

    public void setNumeroPagos(int numeroPagos) {
        this.numeroPagos = numeroPagos;
    }

    public int getNumeroCompras() {
        return numeroCompras;
    }

    public void setNumeroCompras(int numeroCompras) {
        this.numeroCompras = numeroCompras;
    }

    public int getNumeroMermas() {
        return numeroMermas;
    }

    public void setNumeroMermas(int numeroMermas) {
        this.numeroMermas = numeroMermas;
    }

    public List<RenglonDia> getDias() {
        return dias;
    }

    public void setDias(List<RenglonDia> dias) {
        this.dias = dias;
    }

    public List<MovimientoRevisar> getPorRevisar() {
        return porRevisar;
    }

    public void setPorRevisar(List<MovimientoRevisar> porRevisar) {
        this.porRevisar = porRevisar;
    }
}

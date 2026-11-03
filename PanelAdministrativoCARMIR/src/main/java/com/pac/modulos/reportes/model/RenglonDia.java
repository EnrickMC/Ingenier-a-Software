package com.pac.modulos.reportes.model;

import java.time.LocalDate;

/**
 * Totales de un día dentro del reporte de ingresos y gastos (RF-46).
 * También se usa para las barras de la gráfica (una semana o un mes), con su etiqueta.
 */
public class RenglonDia {

    private LocalDate fecha;
    private String etiqueta;      // texto de la barra de la gráfica, por ejemplo "Sem. 03/11"
    private double cobros;        // pagos validados (anticipos, liquidaciones y pagos totales)
    private double reembolsos;    // reembolsos realizados de pedidos cancelados
    private double compras;       // compras de insumos
    private double mermas;        // mermas (pérdidas de insumo)

    public RenglonDia(LocalDate fecha) {
        this.fecha = fecha;
    }

    // Ingresos del día = cobros − reembolsos
    public double getIngresos() {
        return cobros - reembolsos;
    }

    // Gastos del día = compras + mermas
    public double getGastos() {
        return compras + mermas;
    }

    // Utilidad del día = ingresos − gastos
    public double getUtilidad() {
        return getIngresos() - getGastos();
    }

    // Suma los importes de otro renglón (se usa para armar semanas y meses)
    public void sumar(RenglonDia otro) {
        cobros = cobros + otro.cobros;
        reembolsos = reembolsos + otro.reembolsos;
        compras = compras + otro.compras;
        mermas = mermas + otro.mermas;
    }

    // ----- Getters y setters -----

    public LocalDate getFecha() {
        return fecha;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public double getCobros() {
        return cobros;
    }

    public void setCobros(double cobros) {
        this.cobros = cobros;
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
}

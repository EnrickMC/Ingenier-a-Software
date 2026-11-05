package com.pac.modulos.reportes.model;

import java.time.LocalDateTime;

/**
 * Movimiento que no es consistente y por eso NO se suma en el reporte (RF-65,
 * excepción «Movimientos por revisar»). Por ejemplo: un pago validado de un pedido
 * dado de baja o una compra con costo en cero.
 */
public class MovimientoRevisar {

    private LocalDateTime fecha;
    private String origen;        // "Pago", "Compra" o "Merma"
    private String referencia;    // pedido (#12) o insumo
    private double monto;
    private String motivo;        // por qué se debe revisar

    public MovimientoRevisar(LocalDateTime fecha, String origen, String referencia, double monto, String motivo) {
        this.fecha = fecha;
        this.origen = origen;
        this.referencia = referencia;
        this.monto = monto;
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getOrigen() {
        return origen;
    }

    public String getReferencia() {
        return referencia;
    }

    public double getMonto() {
        return monto;
    }

    public String getMotivo() {
        return motivo;
    }
}

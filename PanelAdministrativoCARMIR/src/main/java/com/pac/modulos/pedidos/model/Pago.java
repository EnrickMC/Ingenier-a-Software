package com.pac.modulos.pedidos.model;

import java.time.LocalDateTime;

/**
 * Un pago o comprobante del cliente (tabla Pago).
 */
public class Pago {

    private int idPago;
    private int folio;
    private int idUsuario;            // 0 = sin usuario (pendiente o Administradora)
    private String nombreUsuario;     // quién lo validó o rechazó
    private double monto;
    private LocalDateTime fechaPago;
    private MetodoPago metodo;
    private String referencia;        // puede ser null
    private String linkComprobante;   // puede ser null
    private EstadoPago estado;
    private String motivoRechazo;     // puede ser null
    // Tipo del archivo que subió el cliente desde la web (image/jpeg, application/pdf...); null si no subió archivo
    private String tipoArchivo;

    // Datos del pedido, para la lista de pagos por validar
    private String nombreCliente;
    private EstadoPedido estadoPedido;
    private boolean pedidoCancelado;

    // true si el pago tiene enlace o archivo de comprobante
    public boolean tieneComprobante() {
        if (tieneEnlace()) {
            return true;
        }
        return tieneArchivo();
    }

    // Enlace que se escribió para el comprobante (Pago.LinkComprobante)
    public boolean tieneEnlace() {
        return linkComprobante != null && !linkComprobante.isBlank();
    }

    // Foto o PDF que el cliente subió desde la página web (tabla ComprobanteArchivo, RF-14)
    public boolean tieneArchivo() {
        return tipoArchivo != null;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getLinkComprobante() {
        return linkComprobante;
    }

    public void setLinkComprobante(String linkComprobante) {
        this.linkComprobante = linkComprobante;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public EstadoPedido getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(EstadoPedido estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    public boolean isPedidoCancelado() {
        return pedidoCancelado;
    }

    public void setPedidoCancelado(boolean pedidoCancelado) {
        this.pedidoCancelado = pedidoCancelado;
    }
}

package com.pac.modulos.pedidos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un pedido (tabla Pedido) con su cliente, su saldo (vista vw_SaldoPedido)
 * y, cuando se abre el detalle, sus postres, pagos, historial y notificaciones.
 */
public class Pedido {

    private int folio;
    private Cliente cliente = new Cliente();
    private LocalDateTime fechaSolicitud;
    private LocalDate fechaEntrega;
    private LocalTime horarioEntrega;
    private ModalidadEntrega modalidadEntrega;
    private ModalidadPago modalidadPago;
    private double montoTotal;
    private double anticipoRequerido;
    private EstadoPedido estado;
    private double totalPagado;          // suma de pagos validados
    private int pagosPendientes;         // pagos que subió el cliente y nadie ha revisado
    private int numeroArticulos;         // suma de las cantidades de sus postres
    private String resumenProductos;     // nombres de los postres en una línea
    private String version;              // marca para saber si alguien más lo cambió

    private Direccion direccion;         // solo si es envío a domicilio
    private Cancelacion cancelacion;     // null si no está cancelado
    private BajaPedido baja;             // null si no está dado de baja

    // Se llenan al abrir el detalle
    private List<LineaPedido> lineas = new ArrayList<LineaPedido>();
    private List<Pago> pagos = new ArrayList<Pago>();
    private List<CambioEstado> historial = new ArrayList<CambioEstado>();
    private List<Modificacion> modificaciones = new ArrayList<Modificacion>();
    private List<Notificacion> notificaciones = new ArrayList<Notificacion>();

    // ===================== Datos calculados =====================

    public boolean isCancelado() {
        return cancelacion != null;
    }

    public boolean isDadoDeBaja() {
        return baja != null;
    }

    // Suma de los postres (cantidad × precio de cada línea); solo sirve con el detalle abierto
    public double getSubtotalProductos() {
        double suma = 0;
        for (LineaPedido linea : lineas) {
            suma = suma + linea.getSubtotal();
        }
        return redondear(suma);
    }

    /*
     * Costo de envío de un pedido hecho en la página web (Módulo 2).
     * La tabla Pedido no tiene columna para el envío: el Portal de Pedidos lo guarda dentro de
     * MontoTotal, así que el envío es lo que el total tiene de más sobre los postres.
     * Solo aplica con envío a domicilio y con las líneas cargadas; en otro caso es 0.
     */
    public double getCostoEnvio() {
        if (modalidadEntrega != ModalidadEntrega.DOMICILIO || lineas.isEmpty()) {
            return 0;
        }
        double envio = redondear(montoTotal - getSubtotalProductos());
        if (envio < 0.005) {
            return 0;
        }
        return envio;
    }

    // Saldo pendiente = total − pagos validados (negativo = saldo a favor del cliente)
    public double getSaldo() {
        return redondear(montoTotal - totalPagado);
    }

    // Monto que debe estar pagado para pasar a "Pagado": el anticipo o, con "Pago total", el total
    public double getMontoParaIniciar() {
        if (modalidadPago == ModalidadPago.PAGO_TOTAL) {
            return montoTotal;
        }
        return anticipoRequerido;
    }

    // Cuánto falta para cubrir el anticipo (0 si ya está cubierto)
    public double getFaltaParaIniciar() {
        double falta = redondear(getMontoParaIniciar() - totalPagado);
        if (falta < 0) {
            return 0;
        }
        return falta;
    }

    public boolean anticipoCubierto() {
        return totalPagado + 0.005 >= getMontoParaIniciar();
    }

    // Porcentaje del total que ya se pagó (0 a 100)
    public double getPorcentajePagado() {
        if (montoTotal <= 0) {
            return 0;
        }
        return Math.round(totalPagado * 1000 / montoTotal) / 10.0;
    }

    // true si sigue en las listas de trabajo: no cancelado, no dado de baja y no entregado
    public boolean isActivo() {
        return !isCancelado() && !isDadoDeBaja() && estado != EstadoPedido.ENTREGADO;
    }

    // Fecha y hora de entrega juntas (null si falta alguna)
    public LocalDateTime getFechaHoraEntrega() {
        if (fechaEntrega == null || horarioEntrega == null) {
            return null;
        }
        return LocalDateTime.of(fechaEntrega, horarioEntrega);
    }

    // RF-45: la entrega ya pasó y el pedido no se ha entregado
    public boolean isAtrasado() {
        LocalDateTime entrega = getFechaHoraEntrega();
        if (entrega == null || estado == EstadoPedido.ENTREGADO) {
            return false;
        }
        return entrega.isBefore(LocalDateTime.now());
    }

    // RF-36 / RF-33: un pedido necesita postres, teléfono de contacto y fecha y hora de entrega
    public boolean isDatosCompletos() {
        boolean conTelefono = cliente.getTelefono() != null && cliente.getTelefono().matches("[0-9]{10}");
        boolean conDomicilio = modalidadEntrega != ModalidadEntrega.DOMICILIO || direccion != null;
        return numeroArticulos > 0 && conTelefono && fechaEntrega != null && horarioEntrega != null && conDomicilio;
    }

    // Texto que se muestra como estado: el estado y, si aplica, la marca de cancelado o baja
    public String getTextoEstado() {
        if (isDadoDeBaja()) {
            return "Dado de baja";
        }
        if (isCancelado()) {
            return "Cancelado (" + estado + ")";
        }
        return estado.toString();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    // ===================== Getters y setters =====================

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalTime getHorarioEntrega() {
        return horarioEntrega;
    }

    public void setHorarioEntrega(LocalTime horarioEntrega) {
        this.horarioEntrega = horarioEntrega;
    }

    public ModalidadEntrega getModalidadEntrega() {
        return modalidadEntrega;
    }

    public void setModalidadEntrega(ModalidadEntrega modalidadEntrega) {
        this.modalidadEntrega = modalidadEntrega;
    }

    public ModalidadPago getModalidadPago() {
        return modalidadPago;
    }

    public void setModalidadPago(ModalidadPago modalidadPago) {
        this.modalidadPago = modalidadPago;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }

    public double getAnticipoRequerido() {
        return anticipoRequerido;
    }

    public void setAnticipoRequerido(double anticipoRequerido) {
        this.anticipoRequerido = anticipoRequerido;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public double getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(double totalPagado) {
        this.totalPagado = totalPagado;
    }

    public int getPagosPendientes() {
        return pagosPendientes;
    }

    public void setPagosPendientes(int pagosPendientes) {
        this.pagosPendientes = pagosPendientes;
    }

    public int getNumeroArticulos() {
        return numeroArticulos;
    }

    public void setNumeroArticulos(int numeroArticulos) {
        this.numeroArticulos = numeroArticulos;
    }

    public String getResumenProductos() {
        return resumenProductos;
    }

    public void setResumenProductos(String resumenProductos) {
        this.resumenProductos = resumenProductos;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public Cancelacion getCancelacion() {
        return cancelacion;
    }

    public void setCancelacion(Cancelacion cancelacion) {
        this.cancelacion = cancelacion;
    }

    public BajaPedido getBaja() {
        return baja;
    }

    public void setBaja(BajaPedido baja) {
        this.baja = baja;
    }

    public List<LineaPedido> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaPedido> lineas) {
        this.lineas = lineas;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }

    public List<CambioEstado> getHistorial() {
        return historial;
    }

    public void setHistorial(List<CambioEstado> historial) {
        this.historial = historial;
    }

    public List<Modificacion> getModificaciones() {
        return modificaciones;
    }

    public void setModificaciones(List<Modificacion> modificaciones) {
        this.modificaciones = modificaciones;
    }

    public List<Notificacion> getNotificaciones() {
        return notificaciones;
    }

    public void setNotificaciones(List<Notificacion> notificaciones) {
        this.notificaciones = notificaciones;
    }
}

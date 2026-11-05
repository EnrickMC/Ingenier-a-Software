package com.pac.modulos.pedidos.model;

import com.pac.modulos.inventario.model.NecesidadInsumo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que muestra la Agenda de Producción para un periodo (RF-45, RF-46):
 * pedidos programados, los que tienen datos por revisar, los atrasados y los insumos estimados.
 */
public class AgendaPeriodo {

    private LocalDate desde;
    private LocalDate hasta;
    private List<Pedido> pedidos = new ArrayList<Pedido>();          // programados en el periodo (completos)
    private List<Pedido> porRevisar = new ArrayList<Pedido>();       // con datos incompletos
    private List<Pedido> atrasados = new ArrayList<Pedido>();        // entrega vencida sin entregar (de cualquier fecha)
    private List<NecesidadInsumo> insumos = new ArrayList<NecesidadInsumo>(); // estimado de los que faltan preparar

    // Cuántos pedidos del periodo siguen esperando el anticipo
    public int contarPendientesDePago() {
        int total = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.APROBADO) {
                total++;
            }
        }
        return total;
    }

    // Pedidos del periodo en una fecha
    public List<Pedido> pedidosDelDia(LocalDate fecha) {
        List<Pedido> lista = new ArrayList<Pedido>();
        for (Pedido pedido : pedidos) {
            if (fecha.equals(pedido.getFechaEntrega())) {
                lista.add(pedido);
            }
        }
        return lista;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public List<Pedido> getPorRevisar() {
        return porRevisar;
    }

    public List<Pedido> getAtrasados() {
        return atrasados;
    }

    public List<NecesidadInsumo> getInsumos() {
        return insumos;
    }

    public void setInsumos(List<NecesidadInsumo> insumos) {
        this.insumos = insumos;
    }
}

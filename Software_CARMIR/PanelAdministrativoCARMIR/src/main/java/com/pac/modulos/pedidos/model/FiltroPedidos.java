package com.pac.modulos.pedidos.model;

import java.time.LocalDate;

/**
 * Criterios de búsqueda de la lista de pedidos (RF-36, RF-37).
 */
public class FiltroPedidos {

    // Qué grupo de pedidos se consulta
    public static final int ACTIVOS = 0;      // no cancelados, no dados de baja y no entregados
    public static final int TODOS = 1;        // historial completo (solo la Administradora)
    public static final int CANCELADOS = 2;
    public static final int DADOS_DE_BAJA = 3;

    private int grupo = ACTIVOS;
    private EstadoPedido estado;    // null = cualquier estado
    private String texto;           // folio, nombre o teléfono del cliente
    private LocalDate desde;        // fecha de solicitud desde (puede ser null)
    private LocalDate hasta;        // fecha de solicitud hasta (puede ser null)

    public int getGrupo() {
        return grupo;
    }

    public void setGrupo(int grupo) {
        this.grupo = grupo;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
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
}

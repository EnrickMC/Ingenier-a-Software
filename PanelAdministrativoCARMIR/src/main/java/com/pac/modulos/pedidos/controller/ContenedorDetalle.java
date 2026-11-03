package com.pac.modulos.pedidos.controller;

/**
 * La implementa la pantalla que muestra el detalle de un pedido (la lista de Gestión de Pedidos).
 * El detalle le avisa cuando el pedido cambió o ya no existe, para que recargue su lista.
 */
public interface ContenedorDetalle {

    // folio = pedido que cambió; recargarLista = true si la lista debe volver a consultarse
    void pedidoCambiado(int folio, boolean recargarLista);
}

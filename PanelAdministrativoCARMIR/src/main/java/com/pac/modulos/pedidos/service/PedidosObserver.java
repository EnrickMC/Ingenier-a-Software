package com.pac.modulos.pedidos.service;

/**
 * PATRÓN OBSERVER (observador).
 * Lo implementan las pantallas que muestran contadores de pedidos: el menú lateral
 * y la lista de pedidos (que avisa cuando llega uno nuevo de la web).
 */
public interface PedidosObserver {

    // porRevisar = solicitudes "En proceso de aceptación"; pagosPendientes = comprobantes sin validar;
    // folioNuevo = folio del pedido nuevo que llegó desde la última revisión (0 = ninguno)
    void pedidosActualizados(int porRevisar, int pagosPendientes, int folioNuevo);
}

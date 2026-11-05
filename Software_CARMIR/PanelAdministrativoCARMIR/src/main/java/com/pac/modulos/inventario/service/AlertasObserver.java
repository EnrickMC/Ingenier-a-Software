package com.pac.modulos.inventario.service;

/**
 * PATRÓN OBSERVER (observador).
 * Lo implementan las pantallas que muestran el número de alertas pendientes
 * (por ejemplo el menú lateral del panel).
 */
public interface AlertasObserver {

    // Se llama cada vez que se revisan las alertas
    void alertasActualizadas(int pendientes);
}

package com.pac.navigation;

/**
 * PATRÓN OBSERVER (observador).
 * Las clases que quieran saber qué apartado se está mostrando implementan
 * esta interfaz y se registran en el Navegador.
 */
public interface NavegacionObserver {

    // Se llama cada vez que se muestra otro apartado del panel
    void seccionCambiada(SeccionPanel seccion);
}

package com.pac.view.menu;

import com.pac.navigation.SeccionPanel;
import javafx.scene.Node;
import javafx.scene.control.Button;

/**
 * PATRÓN DECORATOR (componente).
 * Representa un botón del menú lateral. Los decoradores le agregan cosas
 * (candado de permiso, contador) sin modificar la clase base.
 */
public interface ElementoMenu {

    // Devuelve lo que se dibuja en el menú lateral
    Node crearNodo();

    // Devuelve el botón interno, para poder marcarlo como activo
    Button getBoton();

    // Apartado al que lleva este botón
    SeccionPanel getSeccion();
}

package com.pac.view.menu;

import com.pac.navigation.SeccionPanel;
import javafx.scene.Node;
import javafx.scene.control.Button;

/**
 * PATRÓN DECORATOR (decorador abstracto).
 * Envuelve otro ElementoMenu y le pasa todas las llamadas.
 * Los decoradores concretos heredan de aquí y solo cambian lo que necesitan.
 */
public abstract class DecoradorElementoMenu implements ElementoMenu {

    // Elemento que se está decorando
    protected ElementoMenu elementoEnvuelto;

    public DecoradorElementoMenu(ElementoMenu elemento) {
        elementoEnvuelto = elemento;
    }

    @Override
    public Node crearNodo() {
        return elementoEnvuelto.crearNodo();
    }

    @Override
    public Button getBoton() {
        return elementoEnvuelto.getBoton();
    }

    @Override
    public SeccionPanel getSeccion() {
        return elementoEnvuelto.getSeccion();
    }
}

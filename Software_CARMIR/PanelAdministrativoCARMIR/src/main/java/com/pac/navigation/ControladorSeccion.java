package com.pac.navigation;

/**
 * La implementan los controladores de los apartados que se cargan dentro del panel.
 * El Navegador llama a este método justo después de cargar el FXML.
 */
public interface ControladorSeccion {

    void inicializarSeccion(SeccionPanel seccion);
}

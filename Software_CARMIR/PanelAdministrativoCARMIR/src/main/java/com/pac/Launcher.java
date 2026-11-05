package com.pac;

import javafx.application.Application;

/**
 * Punto de entrada para ejecutar desde el IDE o desde un JAR.
 * Evita el error "JavaFX runtime components are missing".
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(CarmirApp.class, args);
    }
}

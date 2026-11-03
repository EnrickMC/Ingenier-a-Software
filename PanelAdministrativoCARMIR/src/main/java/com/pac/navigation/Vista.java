package com.pac.navigation;

/**
 * Pantallas completas de la aplicación. Cada una ocupa toda la ventana.
 */
public enum Vista {

    BLOQUEO("usuarios/bloqueo.fxml"),
    // Ya no hay una pantalla aparte para el Personal: esta vista abre el mismo inicio de sesión único
    LOGIN_PERSONAL("usuarios/bloqueo.fxml"),
    PANEL_PRINCIPAL("panel-principal.fxml");

    private final String archivoFxml;

    Vista(String archivoFxml) {
        this.archivoFxml = archivoFxml;
    }

    // Ruta del archivo FXML dentro de resources
    public String getRutaFxml() {
        return "/com/pac/view/" + archivoFxml;
    }
}

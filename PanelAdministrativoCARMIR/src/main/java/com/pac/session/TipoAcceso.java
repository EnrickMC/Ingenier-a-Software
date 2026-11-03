package com.pac.session;

/**
 * Forma en que el usuario entró al panel.
 * La base de datos no guarda un rol; la Administradora entra por la pantalla de bloqueo
 * y el Personal por el inicio de sesión.
 */
public enum TipoAcceso {

    ADMINISTRADORA("Administradora"),
    PERSONAL("Personal");

    private final String etiqueta;

    TipoAcceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

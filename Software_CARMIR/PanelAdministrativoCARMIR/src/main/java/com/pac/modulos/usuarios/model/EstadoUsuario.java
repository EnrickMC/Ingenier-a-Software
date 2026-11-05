package com.pac.modulos.usuarios.model;

/**
 * Estados posibles de un usuario.
 * Son los mismos valores del ENUM EstadoUsuario de la tabla Usuario.
 */
public enum EstadoUsuario {

    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    BLOQUEADO("Bloqueado");

    // Texto tal como se guarda en la base de datos
    private final String valorBD;

    EstadoUsuario(String valorBD) {
        this.valorBD = valorBD;
    }

    public String getValorBD() {
        return valorBD;
    }

    // Convierte el texto de la base de datos al valor del enum
    public static EstadoUsuario desdeBD(String valor) {
        for (EstadoUsuario estado : values()) {
            if (estado.valorBD.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de usuario desconocido: " + valor);
    }
}

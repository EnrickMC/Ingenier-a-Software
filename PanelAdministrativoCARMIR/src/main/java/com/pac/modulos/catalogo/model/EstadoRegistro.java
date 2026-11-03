package com.pac.modulos.catalogo.model;

/**
 * Estado de una categoría o de una exclusión.
 * Son los mismos valores de los ENUM EstadoCategoria y EstadoExclusion ('Activa', 'Inactiva').
 */
public enum EstadoRegistro {

    ACTIVA("Activa"),
    INACTIVA("Inactiva");

    // Texto tal como se guarda en la base de datos
    private final String valorBD;

    EstadoRegistro(String valorBD) {
        this.valorBD = valorBD;
    }

    public String getValorBD() {
        return valorBD;
    }

    // Convierte el texto de la base de datos al valor del enum
    public static EstadoRegistro desdeBD(String valor) {
        for (EstadoRegistro estado : values()) {
            if (estado.valorBD.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado desconocido: " + valor);
    }
}

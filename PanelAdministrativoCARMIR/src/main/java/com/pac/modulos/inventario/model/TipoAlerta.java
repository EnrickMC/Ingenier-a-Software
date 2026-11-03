package com.pac.modulos.inventario.model;

/**
 * Motivo de una alerta de inventario.
 * Son los mismos valores del ENUM AlertaInventario.TipoAlerta.
 */
public enum TipoAlerta {

    EXISTENCIA_BAJA("Existencia baja", "Existencia baja"),
    PROXIMA_CADUCIDAD("Proxima caducidad", "Próxima caducidad"),
    CADUCADO("Caducado", "Caducado");

    // Texto tal como se guarda en la base de datos (sin acento)
    private final String valorBD;
    // Texto que se muestra en pantalla
    private final String texto;

    TipoAlerta(String valorBD, String texto) {
        this.valorBD = valorBD;
        this.texto = texto;
    }

    public String getValorBD() {
        return valorBD;
    }

    // Convierte el texto de la base de datos al valor del enum
    public static TipoAlerta desdeBD(String valor) {
        for (TipoAlerta tipo : values()) {
            if (tipo.valorBD.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de alerta desconocido: " + valor);
    }

    // true si la alerta es de un lote (próxima caducidad o caducado)
    public boolean esDeCaducidad() {
        return this == PROXIMA_CADUCIDAD || this == CADUCADO;
    }

    @Override
    public String toString() {
        return texto;
    }
}

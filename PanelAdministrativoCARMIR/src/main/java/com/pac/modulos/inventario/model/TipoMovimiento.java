package com.pac.modulos.inventario.model;

/**
 * Clase de movimiento de inventario.
 * Son los mismos valores del ENUM MovimientoInventario.TipoMovimiento.
 */
public enum TipoMovimiento {

    COMPRA("Compra", "Compra"),
    PRODUCCION("Produccion", "Producción"),
    MERMA("Merma", "Merma"),
    AJUSTE("Ajuste", "Ajuste");

    // Texto tal como se guarda en la base de datos (sin acento)
    private final String valorBD;
    // Texto que se muestra en pantalla
    private final String texto;

    TipoMovimiento(String valorBD, String texto) {
        this.valorBD = valorBD;
        this.texto = texto;
    }

    public String getValorBD() {
        return valorBD;
    }

    // Convierte el texto de la base de datos al valor del enum
    public static TipoMovimiento desdeBD(String valor) {
        for (TipoMovimiento tipo : values()) {
            if (tipo.valorBD.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de movimiento desconocido: " + valor);
    }

    @Override
    public String toString() {
        return texto;
    }
}

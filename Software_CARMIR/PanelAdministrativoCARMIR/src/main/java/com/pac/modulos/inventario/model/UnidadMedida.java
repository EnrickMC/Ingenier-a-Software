package com.pac.modulos.inventario.model;

/**
 * Unidades en que se mide un insumo.
 * Son los mismos valores del ENUM Insumo.UnidadMedida ('kg', 'g', 'L', 'ml', 'pieza').
 */
public enum UnidadMedida {

    KILOGRAMO("kg", "Kilogramos (kg)"),
    GRAMO("g", "Gramos (g)"),
    LITRO("L", "Litros (L)"),
    MILILITRO("ml", "Mililitros (ml)"),
    PIEZA("pieza", "Piezas");

    // Texto tal como se guarda en la base de datos
    private final String valorBD;
    // Texto que se muestra en los selectores
    private final String descripcion;

    UnidadMedida(String valorBD, String descripcion) {
        this.valorBD = valorBD;
        this.descripcion = descripcion;
    }

    public String getValorBD() {
        return valorBD;
    }

    // Convierte el texto de la base de datos al valor del enum
    public static UnidadMedida desdeBD(String valor) {
        for (UnidadMedida unidad : values()) {
            if (unidad.valorBD.equalsIgnoreCase(valor)) {
                return unidad;
            }
        }
        throw new IllegalArgumentException("Unidad desconocida: " + valor);
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

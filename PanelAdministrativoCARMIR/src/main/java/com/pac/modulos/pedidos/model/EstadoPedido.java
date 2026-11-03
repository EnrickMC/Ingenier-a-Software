package com.pac.modulos.pedidos.model;

/**
 * Los 6 estados de un pedido (RF-26), en el orden en que avanza.
 * Son los mismos valores del ENUM Pedido.EstadoPedido.
 * La cancelación y la baja NO son estados: el pedido conserva el último estado que alcanzó.
 */
public enum EstadoPedido {

    EN_ACEPTACION("En proceso de aceptacion", "En proceso de aceptación", "estado-aceptacion"),
    APROBADO("Aprobado", "Aprobado", "estado-aprobado"),
    PAGADO("Pagado", "Pagado", "estado-pagado"),
    EN_PREPARACION("En preparacion", "En preparación", "estado-preparacion"),
    TERMINADO("Pedido terminado", "Pedido terminado", "estado-terminado"),
    ENTREGADO("Entregado", "Entregado", "estado-entregado");

    // Texto tal como se guarda en la base de datos (sin acentos)
    private final String valorBD;
    // Texto que se muestra en pantalla
    private final String texto;
    // Clase CSS (pedidos.css) con el color del estado
    private final String claseCss;

    EstadoPedido(String valorBD, String texto, String claseCss) {
        this.valorBD = valorBD;
        this.texto = texto;
        this.claseCss = claseCss;
    }

    public String getValorBD() {
        return valorBD;
    }

    public String getClaseCss() {
        return claseCss;
    }

    // Siguiente estado de la secuencia oficial; null si ya es el último (Entregado)
    public EstadoPedido siguiente() {
        EstadoPedido[] todos = values();
        int posicion = ordinal() + 1;
        if (posicion >= todos.length) {
            return null;
        }
        return todos[posicion];
    }

    // true si este estado va antes que el otro en la secuencia
    public boolean esAnteriorA(EstadoPedido otro) {
        return ordinal() < otro.ordinal();
    }

    // Convierte el texto de la base de datos al valor del enum; null si no lo reconoce
    public static EstadoPedido desdeBD(String valor) {
        if (valor == null) {
            return null;
        }
        for (EstadoPedido estado : values()) {
            if (estado.valorBD.equalsIgnoreCase(valor.trim())) {
                return estado;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return texto;
    }
}

package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.TipoAlerta;
import com.pac.modulos.inventario.service.InsumoService;
import javafx.scene.Node;

/**
 * Elige las clases CSS (inventario.css) que colorean renglones y etiquetas
 * según el estado de un insumo, una alerta o una receta.
 */
public class EstilosInventario {

    public static final String FILA_BAJO_MINIMO = "fila-bajo-minimo";
    public static final String FILA_CRITICA = "fila-critica";
    public static final String FILA_PROXIMA = "fila-proxima";
    public static final String FILA_INACTIVA = "fila-inactiva";

    // Todas las clases de renglón (para quitarlas antes de poner la nueva)
    public static final String[] CLASES_FILA = {FILA_BAJO_MINIMO, FILA_CRITICA, FILA_PROXIMA, FILA_INACTIVA};

    // Clase de apoyo: no se crean objetos de ella
    private EstilosInventario() {
    }

    // Renglón de la tabla de insumos; null = sin color
    public static String claseFilaInsumo(String estado) {
        if (InsumoService.ESTADO_STOCK_BAJO.equals(estado)) {
            return FILA_BAJO_MINIMO;
        }
        if (InsumoService.ESTADO_AGOTADO.equals(estado) || InsumoService.ESTADO_CADUCADO.equals(estado)) {
            return FILA_CRITICA;
        }
        if (InsumoService.ESTADO_PROXIMO_CADUCAR.equals(estado) || InsumoService.ESTADO_REVISAR.equals(estado)) {
            return FILA_PROXIMA;
        }
        if (InsumoService.ESTADO_INACTIVO.equals(estado)) {
            return FILA_INACTIVA;
        }
        return null;
    }

    // Etiqueta de estado del insumo en el detalle
    public static String claseChipInsumo(String estado) {
        if (InsumoService.ESTADO_STOCK_BAJO.equals(estado)) {
            return "chip-bajo";
        }
        if (InsumoService.ESTADO_AGOTADO.equals(estado) || InsumoService.ESTADO_CADUCADO.equals(estado)) {
            return "chip-critico";
        }
        if (InsumoService.ESTADO_PROXIMO_CADUCAR.equals(estado) || InsumoService.ESTADO_REVISAR.equals(estado)) {
            return "chip-proximo";
        }
        if (InsumoService.ESTADO_INACTIVO.equals(estado) || InsumoService.ESTADO_SIN_MINIMO.equals(estado)) {
            return "chip-gris";
        }
        return "chip-normal";
    }

    // Renglón de la tabla de alertas
    public static String claseFilaAlerta(AlertaInventario alerta) {
        if (!alerta.isPendiente()) {
            return FILA_INACTIVA;
        }
        if (alerta.getTipo() == TipoAlerta.CADUCADO || alerta.estaAgotado()) {
            return FILA_CRITICA;
        }
        if (alerta.getTipo() == TipoAlerta.EXISTENCIA_BAJA) {
            return FILA_BAJO_MINIMO;
        }
        return FILA_PROXIMA;
    }

    // Pone la clase indicada en el nodo, quitando antes cualquier otra clase de renglón
    public static void aplicarClaseFila(Node nodo, String clase) {
        for (String otra : CLASES_FILA) {
            nodo.getStyleClass().remove(otra);
        }
        if (clase != null) {
            nodo.getStyleClass().add(clase);
        }
    }

    // Pone la clase de la etiqueta de estado, quitando las anteriores
    public static void aplicarChip(Node nodo, String clase) {
        String[] chips = {"chip-normal", "chip-bajo", "chip-critico", "chip-proximo", "chip-gris", "chip-info"};
        for (String otra : chips) {
            nodo.getStyleClass().remove(otra);
        }
        nodo.getStyleClass().add(clase);
    }
}

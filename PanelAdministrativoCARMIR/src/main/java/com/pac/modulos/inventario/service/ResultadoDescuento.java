package com.pac.modulos.inventario.service;

import com.pac.modulos.inventario.model.NecesidadInsumo;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta del descuento automático de insumos de un pedido (RF-63).
 * El Gestor de Pedidos la revisa para decidir si el pedido pasa a "En preparación".
 */
public class ResultadoDescuento {

    // Posibles resultados del descuento
    public static final int DESCONTADO = 1;            // se descontaron los insumos
    public static final int YA_DESCONTADO = 2;         // ya se había descontado antes: no se repite
    public static final int SIN_RECETA = 3;            // hay opciones sin receta activa: preguntar si se continúa
    public static final int FALTAN_INSUMOS = 4;        // la existencia no alcanza: no se descontó nada
    public static final int PEDIDO_NO_DISPONIBLE = 5;  // no existe o está cancelado
    public static final int ESTADO_INVALIDO = 6;       // el pedido no está en "Pagado"
    public static final int ERROR = 7;                 // falló la base de datos: hay que deshacer todo

    private int resultado;
    private String mensaje;
    private List<String> opcionesSinReceta = new ArrayList<String>();
    private List<NecesidadInsumo> insumos = new ArrayList<NecesidadInsumo>();

    public ResultadoDescuento(int resultado, String mensaje) {
        this.resultado = resultado;
        this.mensaje = mensaje;
    }

    public int getResultado() {
        return resultado;
    }

    public String getMensaje() {
        return mensaje;
    }

    // true si el pedido puede terminar de cambiar a "En preparación"
    public boolean permiteContinuar() {
        return resultado == DESCONTADO || resultado == YA_DESCONTADO;
    }

    // Nombres de las opciones elegidas que no tienen receta activa
    public List<String> getOpcionesSinReceta() {
        return opcionesSinReceta;
    }

    public void setOpcionesSinReceta(List<String> opcionesSinReceta) {
        this.opcionesSinReceta = opcionesSinReceta;
    }

    // Insumos calculados (los descontados, o los que faltan si el resultado es FALTAN_INSUMOS)
    public List<NecesidadInsumo> getInsumos() {
        return insumos;
    }

    public void setInsumos(List<NecesidadInsumo> insumos) {
        this.insumos = insumos;
    }
}

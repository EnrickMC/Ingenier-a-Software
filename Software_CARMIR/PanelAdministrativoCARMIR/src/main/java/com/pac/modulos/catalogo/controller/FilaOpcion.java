package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de opciones (opciones.fxml).
 */
public class FilaOpcion {

    private Opcion opcion;
    private int exclusiones;

    public FilaOpcion(Opcion opcion, int exclusiones) {
        this.opcion = opcion;
        this.exclusiones = exclusiones;
    }

    public Opcion getOpcion() {
        return opcion;
    }

    public String getNombre() {
        return opcion.getNombre();
    }

    public String getMultIngredientes() {
        return "×" + ValidacionCatalogo.numero(opcion.getMultiplicadorIngredientes());
    }

    public String getMultCosto() {
        return "×" + ValidacionCatalogo.numero(opcion.getMultiplicadorCosto());
    }

    public String getSeEscala() {
        return ApoyoVista.siNo(opcion.isSeEscala());
    }

    public String getPuntos() {
        return ValidacionCatalogo.numero(opcion.getPuntosSeleccion());
    }

    public String getMultPresupuesto() {
        return "×" + ValidacionCatalogo.numero(opcion.getMultiplicadorPresupuesto());
    }

    public String getDisponible() {
        return ApoyoVista.siNo(opcion.isDisponible());
    }

    // Costo de su receta (vw_CostoReceta) o "Sin receta"
    public String getCostoReceta() {
        if (!opcion.isTieneReceta()) {
            return "Sin receta";
        }
        return Formatos.moneda(opcion.getCostoReceta());
    }

    // Precio base de la opción = costo de receta × multiplicador de costo (igual que vw_CostoPrecioPostre)
    public String getPrecioBase() {
        if (!opcion.isTieneReceta()) {
            return "Sin receta";
        }
        return Formatos.moneda(opcion.getCostoReceta() * opcion.getMultiplicadorCosto());
    }

    public int getExclusiones() {
        return exclusiones;
    }
}

package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Opcion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;

/**
 * Los controles con que se elige una opción en el formulario de productos.
 * - Selección única: un RadioButton.
 * - Selección múltiple: un CheckBox y un Spinner con la cantidad.
 */
public class SelectorOpcion {

    private Opcion opcion;
    private RadioButton radio;
    private CheckBox casilla;
    private Spinner<Integer> cantidad;

    // Para características de selección única
    public SelectorOpcion(Opcion opcion, RadioButton radio) {
        this.opcion = opcion;
        this.radio = radio;
    }

    // Para características de selección múltiple
    public SelectorOpcion(Opcion opcion, CheckBox casilla, Spinner<Integer> cantidad) {
        this.opcion = opcion;
        this.casilla = casilla;
        this.cantidad = cantidad;
    }

    public Opcion getOpcion() {
        return opcion;
    }

    // true si la opción está marcada
    public boolean estaElegida() {
        if (radio != null) {
            return radio.isSelected();
        }
        return casilla.isSelected();
    }

    // Veces que se eligió (1 en selección única)
    public int getCantidad() {
        if (cantidad == null) {
            return 1;
        }
        return cantidad.getValue();
    }

    // Marca la opción con una cantidad (al cargar un producto existente)
    public void elegir(int veces) {
        if (radio != null) {
            radio.setSelected(true);
            return;
        }
        casilla.setSelected(true);
        cantidad.getValueFactory().setValue(veces);
        cantidad.setDisable(false);
    }
}

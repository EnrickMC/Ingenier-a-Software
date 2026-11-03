package com.pac.modulos.pedidos.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Fila de botones de estado que sirven como filtro (reemplaza al ComboBox de estados).
 * Solo uno puede estar activo; si se vuelve a pulsar el activo, se regresa a "Todos".
 */
public class BarraEstados extends FlowPane {

    private final ToggleGroup grupo = new ToggleGroup();
    private ToggleButton botonTodos;
    private Runnable alCambiar;
    private boolean cambiandoSolo = false;
    private boolean compacto = false;

    public BarraEstados() {
        setHgap(8);
        setVgap(8);
        botonTodos = crearBoton("Todos", "bi-grid-fill", null);
        botonTodos.setUserData(null);
        botonTodos.setSelected(true);
        getChildren().add(botonTodos);

        // Si se quita la selección (clic sobre el botón activo) se vuelve a "Todos"
        // (JavaFX necesita este escuchador para enterarse del cambio de botón)
        grupo.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> valor, Toggle antes, Toggle ahora) {
                if (cambiandoSolo) {
                    return;
                }
                if (ahora == null) {
                    cambiandoSolo = true;
                    botonTodos.setSelected(true);
                    cambiandoSolo = false;
                }
                if (alCambiar != null) {
                    alCambiar.run();
                }
            }
        });
    }

    // Quita todos los botones menos "Todos" (para volver a llenar la barra)
    public void vaciar() {
        getChildren().retainAll(botonTodos);
    }

    // Agrega un botón de estado: la clave es lo que devuelve getClave() al estar activo
    public void agregar(String clave, String texto, String clase) {
        ToggleButton boton = crearBoton(texto, EstilosPedidos.iconoDe(clase), clase);
        boton.setUserData(clave);
        getChildren().add(boton);
    }

    // Igual, pero con el icono indicado (para filtros que no son de pedidos)
    public void agregar(String clave, String texto, String clase, String icono) {
        ToggleButton boton = crearBoton(texto, icono, clase);
        boton.setUserData(clave);
        getChildren().add(boton);
    }

    // Deja activo el botón de esa clave sin avisar del cambio
    public void seleccionar(String clave) {
        for (javafx.scene.Node nodo : getChildren()) {
            if (nodo instanceof ToggleButton && clave.equals(nodo.getUserData())) {
                cambiandoSolo = true;
                ((ToggleButton) nodo).setSelected(true);
                cambiandoSolo = false;
            }
        }
    }

    private ToggleButton crearBoton(String texto, String icono, String clase) {
        ToggleButton boton = new ToggleButton(texto);
        boton.setGraphic(new FontIcon(icono));
        boton.setToggleGroup(grupo);
        boton.getStyleClass().add("filtro-estado");
        boton.getStyleClass().add("estado-pedido");
        if (compacto) {
            boton.getStyleClass().add("filtro-compacto");
        }
        if (clase != null) {
            boton.getStyleClass().add(clase);
        } else {
            boton.getStyleClass().add("filtro-todos");
        }
        return boton;
    }

    // Botones más angostos, para las barras que comparten renglón con otros controles
    public void setCompacto(boolean compacto) {
        this.compacto = compacto;
        setPrefWrapLength(560);
        // El botón "Todos" ya existe: también se hace angosto
        botonTodos.getStyleClass().add("filtro-compacto");
    }

    public boolean isCompacto() {
        return compacto;
    }

    // Acción que se ejecuta cada vez que cambia el filtro
    public void setAlCambiar(Runnable accion) {
        this.alCambiar = accion;
    }

    // Clave del estado activo; null si está en "Todos"
    public String getClave() {
        Toggle activo = grupo.getSelectedToggle();
        if (activo == null) {
            return null;
        }
        Object clave = activo.getUserData();
        if (clave == null) {
            return null;
        }
        return clave.toString();
    }

    // Regresa a "Todos" sin avisar del cambio
    public void limpiar() {
        cambiandoSolo = true;
        botonTodos.setSelected(true);
        cambiandoSolo = false;
    }
}

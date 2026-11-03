package com.pac.view.menu;

import com.pac.session.SesionUsuario;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * PATRÓN DECORATOR (decorador concreto).
 * Si el usuario de la sesión no tiene el permiso del apartado, desactiva el botón
 * y le pone un candado. (Requerimiento: ocultar o bloquear los módulos sin permiso).
 */
public class DecoradorPermiso extends DecoradorElementoMenu {

    public DecoradorPermiso(ElementoMenu elemento) {
        super(elemento);
    }

    @Override
    public Node crearNodo() {
        Node nodoOriginal = super.crearNodo();
        String permiso = getSeccion().getPermisoRequerido();

        // Si tiene permiso, el botón se queda igual
        if (SesionUsuario.getInstancia().tienePermiso(permiso)) {
            return nodoOriginal;
        }

        // RNF-15: el menú muestra con candado los apartados sin permiso.
        // Si no tiene permiso: se desactiva el botón y se agrega un candado a la derecha
        getBoton().setDisable(true);
        getBoton().getStyleClass().add("menu-item-bloqueado");

        FontIcon candado = new FontIcon("bi-lock-fill");
        candado.getStyleClass().add("menu-candado");
        candado.setMouseTransparent(true);
        candado.setTranslateX(-18);

        StackPane contenedor = new StackPane(nodoOriginal, candado);
        StackPane.setAlignment(candado, Pos.CENTER_RIGHT);
        Tooltip.install(contenedor, new Tooltip("Sin permiso: " + permiso));
        return contenedor;
    }
}

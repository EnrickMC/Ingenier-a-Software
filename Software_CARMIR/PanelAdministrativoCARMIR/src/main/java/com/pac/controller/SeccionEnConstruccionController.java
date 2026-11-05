package com.pac.controller;

import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controlador de la pantalla temporal (seccion-en-construccion.fxml).
 * Se usa en los apartados que todavía no tienen funcionalidad y muestra
 * a qué módulo pertenecen y qué funciones tendrán.
 */
public class SeccionEnConstruccionController implements ControladorSeccion {

    @FXML private FontIcon icoSeccion;
    @FXML private Label lblNombre;
    @FXML private Label lblModulo;
    @FXML private VBox listaFunciones;

    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        icoSeccion.setIconLiteral(seccion.getIcono());
        lblNombre.setText(seccion.getTextoMenu());
        lblModulo.setText(seccion.getModulo());

        // Una línea con palomita por cada función prevista
        for (String funcion : seccion.getFunciones()) {
            FontIcon palomita = new FontIcon("bi-check2-circle");
            palomita.getStyleClass().add("funcion-icono");

            Label linea = new Label(funcion, palomita);
            linea.getStyleClass().add("funcion-item");
            linea.setGraphicTextGap(10);
            listaFunciones.getChildren().add(linea);
        }
    }
}

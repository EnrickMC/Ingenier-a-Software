package com.pac.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Ícono de signo de interrogación que va junto al nombre de un campo.
 * La explicación se muestra SOLO mientras el mouse está encima del ícono:
 * al quitar el mouse desaparece de inmediato (no se queda fija con el clic).
 *
 * Se usa desde los FXML así:
 *   <Label text="Stock mínimo *" contentDisplay="RIGHT">
 *       <graphic><IconoAyuda texto="Cantidad por debajo de la cual..."/></graphic>
 *   </Label>
 *
 * También se puede crear desde Java: new IconoAyuda("explicación").
 */
public class IconoAyuda extends Label {

    // Ancho máximo del globo con la explicación
    private static final double ANCHO_GLOBO = 320;

    private Tooltip globo;

    // Constructor que usa el FXML (después se llama a setTexto)
    public IconoAyuda() {
        this("");
    }

    public IconoAyuda(String texto) {
        FontIcon icono = new FontIcon("bi-question-circle-fill");
        setGraphic(icono);
        getStyleClass().add("icono-ayuda");
        setCursor(Cursor.HAND);
        setFocusTraversable(false);

        // El globo lo muestran y lo quitan los eventos del mouse de esta clase (no el Tooltip.install)
        globo = new Tooltip(texto);
        globo.getStyleClass().add("globo-ayuda");
        globo.setWrapText(true);
        globo.setMaxWidth(ANCHO_GLOBO);
        globo.setAutoHide(false);
        globo.setHideDelay(Duration.ZERO);

        setOnMouseEntered(new AlEntrar());
        setOnMouseExited(new AlSalir());
        // Si el ícono se oculta o cambia de ventana, el globo también se quita
        visibleProperty().addListener(new AlCambiarVisibilidad());
    }

    public String getTexto() {
        return globo.getText();
    }

    // Cambia la explicación que se muestra
    public void setTexto(String texto) {
        globo.setText(texto);
    }

    // Mouse encima: se muestra el globo junto al ícono
    private class AlEntrar implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent evento) {
            Bounds limites = localToScreen(getBoundsInLocal());
            if (limites != null && !globo.isShowing()) {
                globo.show(IconoAyuda.this, limites.getMaxX() + 6, limites.getMaxY() + 6);
            }
        }
    }

    // Mouse fuera: el globo desaparece
    private class AlSalir implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent evento) {
            globo.hide();
        }
    }

    // Se quita el globo si el ícono deja de verse
    private class AlCambiarVisibilidad implements ChangeListener<Boolean> {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable,
                            Boolean antes, Boolean ahora) {
            if (!ahora) {
                globo.hide();
            }
        }
    }
}

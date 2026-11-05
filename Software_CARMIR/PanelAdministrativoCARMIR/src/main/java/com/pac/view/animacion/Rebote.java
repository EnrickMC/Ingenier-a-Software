package com.pac.view.animacion;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Animación corta de "rebote": el elemento crece un poco, se inclina y vuelve a su forma.
 * Se usa cuando el mouse pasa por el logo o por los iconos del menú.
 *
 * Uso: elemento.setOnMouseEntered(new Rebote(icono, 1.25, 12));
 * (el elemento que recibe el mouse y el que se anima pueden ser distintos).
 */
public class Rebote implements EventHandler<MouseEvent> {

    private Node objetivo;
    private double aumento;
    private double grados;
    private Timeline animacion;

    public Rebote(Node objetivo, double aumento, double grados) {
        this.objetivo = objetivo;
        this.aumento = aumento;
        this.grados = grados;
    }

    @Override
    public void handle(MouseEvent evento) {
        if (animacion != null) {
            animacion.stop();
        }
        // Cuadros clave: sube, se inclina al otro lado, y regresa a lo normal
        KeyFrame inicio = new KeyFrame(Duration.ZERO,
                new KeyValue(objetivo.scaleXProperty(), 1.0),
                new KeyValue(objetivo.scaleYProperty(), 1.0),
                new KeyValue(objetivo.rotateProperty(), 0.0));
        KeyFrame subir = new KeyFrame(Duration.millis(140),
                new KeyValue(objetivo.scaleXProperty(), aumento, Interpolator.EASE_OUT),
                new KeyValue(objetivo.scaleYProperty(), aumento, Interpolator.EASE_OUT),
                new KeyValue(objetivo.rotateProperty(), grados, Interpolator.EASE_OUT));
        KeyFrame contrario = new KeyFrame(Duration.millis(300),
                new KeyValue(objetivo.rotateProperty(), -grados, Interpolator.EASE_BOTH));
        KeyFrame final0 = new KeyFrame(Duration.millis(460),
                new KeyValue(objetivo.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                new KeyValue(objetivo.scaleYProperty(), 1.0, Interpolator.EASE_IN),
                new KeyValue(objetivo.rotateProperty(), 0.0, Interpolator.EASE_BOTH));
        animacion = new Timeline(inicio, subir, contrario, final0);
        animacion.play();
    }
}

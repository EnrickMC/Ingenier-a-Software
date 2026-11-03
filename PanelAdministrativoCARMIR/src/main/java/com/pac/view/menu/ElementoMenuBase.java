package com.pac.view.menu;

import com.pac.navigation.Navegador;
import com.pac.navigation.SeccionPanel;
import com.pac.view.animacion.IconoLottie;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

/**
 * PATRÓN DECORATOR (componente concreto).
 * Es el botón normal del menú: icono animado + texto. Al hacer clic abre su apartado.
 */
public class ElementoMenuBase implements ElementoMenu {

    // Lado del icono animado, en pixeles
    private static final double TAMANO_ICONO = 40;

    private SeccionPanel seccion;
    private Button boton;
    private IconoLottie icono;

    public ElementoMenuBase(SeccionPanel seccionDelBoton) {
        seccion = seccionDelBoton;

        // Icono animado (Lottie4J); si Lottie falla se ve el icono normal
        icono = new IconoLottie(seccion.getArchivoLottie(), TAMANO_ICONO, seccion.getCuadroReposo(), seccion.getIcono());

        boton = new Button(seccion.getTextoMenu(), icono);
        boton.getStyleClass().add("menu-item");
        boton.setAlignment(Pos.CENTER_LEFT);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setGraphicTextGap(8);

        // Acción del clic (JavaFX necesita la flecha "->" para indicar qué hacer)
        boton.setOnAction(evento -> abrirSeccion());

        // La animación del icono solo corre mientras el mouse está encima del botón
        boton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                icono.reproducir();
            }
        });
        boton.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                icono.detener();
            }
        });
    }

    // Muestra el apartado de este botón en el panel
    private void abrirSeccion() {
        Navegador.getInstancia().irASeccion(seccion);
    }

    @Override
    public Node crearNodo() {
        return boton;
    }

    @Override
    public Button getBoton() {
        return boton;
    }

    @Override
    public SeccionPanel getSeccion() {
        return seccion;
    }
}

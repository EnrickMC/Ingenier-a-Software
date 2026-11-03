package com.pac.view.animacion;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Adornos de colores pastel para el fondo del menú lateral (corazones, estrellas,
 * chispas de azúcar y "donas"). Están quietos y, cuando el mouse pasa por el menú,
 * flotan y giran suavemente. Al quitar el mouse regresan a su lugar.
 *
 * No recibe clics (mouseTransparent) para no estorbar al menú que está encima.
 */
public class DecoracionMenu extends Pane {

    // Colores pastel
    private static final Color ROSA = Color.web("#f8c8dc");
    private static final Color MENTA = Color.web("#b8e6d0");
    private static final Color LAVANDA = Color.web("#d4c5f0");
    private static final Color DURAZNO = Color.web("#ffd6b0");
    private static final Color MANTEQUILLA = Color.web("#fff1b8");
    private static final Color CIELO = Color.web("#bde0fe");

    // Posición de cada adorno como fracción del ancho y del alto (0 a 1)
    private List<Node> adornos = new ArrayList<Node>();
    private List<Double> posicionesX = new ArrayList<Double>();
    private List<Double> posicionesY = new ArrayList<Double>();
    private List<Double> duraciones = new ArrayList<Double>();
    private List<Double> subidas = new ArrayList<Double>();
    private List<Double> giros = new ArrayList<Double>();
    private List<Double> fases = new ArrayList<Double>();

    private double tiempo = 0;
    private double intensidad = 0;
    private boolean activo = false;
    private Reloj reloj = new Reloj();

    public DecoracionMenu() {
        setMouseTransparent(true);
        // Los adornos no deben salirse del menú
        setMinSize(0, 0);

        agregarIcono("bi-heart-fill", ROSA, 26, 0.80, 0.10);
        agregarIcono("bi-star-fill", MANTEQUILLA, 22, 0.14, 0.24);
        agregarIcono("bi-stars", CIELO, 30, 0.72, 0.36);
        agregarDona(MENTA, 15, 0.20, 0.47);
        agregarIcono("bi-heart-fill", LAVANDA, 18, 0.46, 0.58);
        agregarIcono("bi-star-fill", ROSA, 17, 0.84, 0.64);
        agregarChispa(DURAZNO, 0.12, 0.70, 35);
        agregarChispa(MENTA, 0.62, 0.78, -30);
        agregarChispa(LAVANDA, 0.30, 0.86, 60);
        agregarDona(MANTEQUILLA, 12, 0.78, 0.88);
        agregarIcono("bi-stars", DURAZNO, 24, 0.18, 0.95);
    }

    // ---------- Creación de adornos ----------

    private void agregarIcono(String codigo, Color color, double tamano, double x, double y) {
        FontIcon icono = new FontIcon(codigo);
        icono.setIconSize((int) tamano);
        icono.setIconColor(color);
        registrar(icono, x, y);
    }

    // "Dona": círculo de color con hueco
    private void agregarDona(Color color, double radio, double x, double y) {
        Circle dona = new Circle(radio);
        dona.setFill(Color.TRANSPARENT);
        dona.setStroke(color);
        dona.setStrokeWidth(radio / 2.2);
        registrar(dona, x, y);
    }

    // Chispa de azúcar: rectángulo redondeado inclinado
    private void agregarChispa(Color color, double x, double y, double grados) {
        Rectangle chispa = new Rectangle(20, 6);
        chispa.setArcWidth(6);
        chispa.setArcHeight(6);
        chispa.setFill(color);
        chispa.setRotate(grados);
        chispa.getProperties().put("anguloBase", Double.valueOf(grados));
        registrar(chispa, x, y);
    }

    private void registrar(Node adorno, double x, double y) {
        adorno.setOpacity(0.26);
        // Se dibuja una sola vez como imagen: al mover, girar o escalar no tiembla el trazo
        adorno.setCache(true);
        adorno.setCacheHint(CacheHint.QUALITY);
        getChildren().add(adorno);
        adornos.add(adorno);
        posicionesX.add(Double.valueOf(x));
        posicionesY.add(Double.valueOf(y));
        int numero = adornos.size();
        // Cada adorno tiene su propio ritmo
        duraciones.add(Double.valueOf(2.6 + (numero % 4) * 0.7));
        subidas.add(Double.valueOf(10 + (numero % 3) * 5));
        double gira = 14 + (numero % 3) * 8;
        if (numero % 2 == 0) {
            gira = -gira;
        }
        giros.add(Double.valueOf(gira));
        fases.add(Double.valueOf(numero * 1.3));
    }

    // Reloj de la animación: calcula la posición exacta en cada cuadro con senos,
    // así el movimiento es continuo (sin saltos de píxel) y arranca/termina con suavidad
    private class Reloj extends AnimationTimer {
        private long anterior = 0;

        @Override
        public void handle(long ahora) {
            if (anterior == 0) {
                anterior = ahora;
            }
            double dt = (ahora - anterior) / 1.0e9;
            anterior = ahora;
            if (dt > 0.1) {
                dt = 0.1;
            }
            tiempo = tiempo + dt;

            // La intensidad sube a 1 al entrar el mouse y baja a 0 al salir
            double meta = 0;
            if (activo) {
                meta = 1;
            }
            double paso = dt / 0.6;
            if (intensidad < meta) {
                intensidad = Math.min(meta, intensidad + paso);
            } else {
                intensidad = Math.max(meta, intensidad - paso);
            }
            // Curva suave (smoothstep) para evitar tirones en el arranque y el final
            double fuerza = intensidad * intensidad * (3 - 2 * intensidad);

            for (int i = 0; i < adornos.size(); i++) {
                mover(adornos.get(i), i, fuerza);
            }
            if (!activo && intensidad <= 0) {
                anterior = 0;
                stop();
            }
        }
    }

    // Coloca un adorno según el tiempo (valores con decimales, sin redondear)
    private void mover(Node adorno, int i, double fuerza) {
        double vuelta = 2 * Math.PI * tiempo / duraciones.get(i).doubleValue() + fases.get(i).doubleValue();
        double onda = Math.sin(vuelta);
        double sube = subidas.get(i).doubleValue();
        adorno.setTranslateY(-sube * (0.5 + 0.5 * onda) * fuerza);
        adorno.setTranslateX(sube * 0.5 * Math.sin(vuelta * 0.5) * fuerza);
        adorno.setRotate(base(adorno) + giros.get(i).doubleValue() * Math.sin(vuelta * 0.77) * fuerza);
        double latido = 1 + 0.2 * (0.5 + 0.5 * Math.sin(vuelta * 1.3)) * fuerza;
        adorno.setScaleX(latido);
        adorno.setScaleY(latido);
    }

    // ---------- Posiciones ----------

    @Override
    protected void layoutChildren() {
        for (int i = 0; i < adornos.size(); i++) {
            Node adorno = adornos.get(i);
            double x = getWidth() * posicionesX.get(i).doubleValue();
            double y = getHeight() * posicionesY.get(i).doubleValue();
            // Se centra el adorno en su posición
            adorno.relocate(x - adorno.getLayoutBounds().getWidth() / 2,
                    y - adorno.getLayoutBounds().getHeight() / 2);
        }
    }

    // ---------- Control de la animación ----------

    // Mouse sobre el menú: los adornos se mueven
    public void animar() {
        activo = true;
        reloj.start();
    }

    // Mouse fuera del menú: el movimiento se apaga poco a poco y regresan a su lugar
    public void detener() {
        activo = false;
    }

    // Ángulo inicial del adorno (las chispas empiezan inclinadas)
    private double base(Node adorno) {
        Object inicial = adorno.getProperties().get("anguloBase");
        if (inicial instanceof Double) {
            return ((Double) inicial).doubleValue();
        }
        return 0;
    }

    // Manejadores listos para poner en el menú lateral
    public EventHandler<MouseEvent> alEntrar() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                animar();
            }
        };
    }

    public EventHandler<MouseEvent> alSalir() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                detener();
            }
        };
    }
}

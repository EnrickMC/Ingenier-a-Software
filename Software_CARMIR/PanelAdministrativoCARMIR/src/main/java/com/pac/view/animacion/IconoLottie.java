package com.pac.view.animacion;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Icono animado hecho con Lottie4J.
 * Lee un archivo JSON de Lottie de la carpeta resources/com/pac/lottie/ y lo dibuja en un
 * cuadro pequeño. Se queda quieto en un cuadro de reposo y solo se anima cuando se llama a
 * reproducir() (por ejemplo, al pasar el mouse); detener() lo regresa al reposo.
 *
 * Si el archivo no se puede cargar, muestra en su lugar un icono normal de Bootstrap Icons,
 * así el menú nunca se queda sin icono.
 */
public class IconoLottie extends StackPane {

    // Carpeta donde se guardan TODOS los JSON de Lottie que usa el panel
    private static final String CARPETA = "/com/pac/lottie/";

    // Los JSON ya leídos se guardan para no leerlos otra vez cuando se vuelve a armar el menú
    private static Map<String, Animation> animaciones = new HashMap<String, Animation>();

    private LottiePlayer reproductor;
    private Animation animacion;
    private double cuadroReposo;

    // Datos del modo "en ciclo" (ver iniciarCiclo)
    private boolean enCiclo = false;
    private double cuadroDePausa = 0;
    private double segundosDePausa = 0;
    private PauseTransition temporizador;
    private boolean estuvoEnPantalla = false;

    // archivo: nombre del JSON (por ejemplo "gear.json"); tamano: lado del cuadro en pixeles
    // cuadroReposo: cuadro que se ve cuando el mouse no está encima
    // respaldo: código del icono normal que se usa si falla Lottie
    public IconoLottie(String archivo, double tamano, double cuadroReposo, String respaldo) {
        this.cuadroReposo = cuadroReposo;
        getStyleClass().add("icono-lottie");
        setMinSize(tamano, tamano);
        setPrefSize(tamano, tamano);
        setMaxSize(tamano, tamano);
        // El icono no recibe el mouse: lo recibe el botón del menú que lo contiene
        setMouseTransparent(true);

        Node contenido = crearContenido(archivo, tamano, respaldo);
        getChildren().add(contenido);
    }

    // Intenta crear el reproductor; si algo falla devuelve el icono de respaldo
    private Node crearContenido(String archivo, double tamano, String respaldo) {
        try {
            animacion = cargar(archivo);
            int lado = (int) Math.round(tamano);
            reproductor = new LottiePlayer(animacion, lado, lado);
            // Fondo transparente: así se ve el color del menú detrás del icono
            reproductor.setBackgroundColor(Color.TRANSPARENT);
            reproductor.seekToFrame(cuadroReposo);
            return reproductor;
        } catch (Exception error) {
            System.err.println("No se pudo cargar el icono animado " + archivo + ": " + error.getMessage());
            reproductor = null;
            FontIcon icono = new FontIcon(respaldo);
            icono.getStyleClass().add("menu-icono");
            return icono;
        }
    }

    // Lee el JSON de resources (Lottie4J necesita un archivo, así que se copia a una carpeta temporal)
    private static Animation cargar(String archivo) throws Exception {
        Animation guardada = animaciones.get(archivo);
        if (guardada != null) {
            return guardada;
        }
        InputStream entrada = IconoLottie.class.getResourceAsStream(CARPETA + archivo);
        if (entrada == null) {
            throw new IllegalStateException("No existe " + CARPETA + archivo);
        }
        File temporal = File.createTempFile("carmir-lottie-", ".json");
        temporal.deleteOnExit();
        try {
            Files.copy(entrada, temporal.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            entrada.close();
        }
        Animation animacion = LottieFileLoader.load(temporal);
        animaciones.put(archivo, animacion);
        return animacion;
    }

    // Empieza la animación (se repite mientras el mouse siga encima)
    public void reproducir() {
        if (reproductor != null && !reproductor.isPlaying()) {
            // Siempre empieza desde el primer cuadro
            reproductor.seekToFrame(0);
            reproductor.play();
        }
    }

    // Detiene la animación y regresa al cuadro de reposo
    public void detener() {
        if (reproductor != null) {
            reproductor.stop();
            reproductor.seekToFrame(cuadroReposo);
        }
    }

    // ---------- Modo "en ciclo" (no depende del mouse) ----------

    /**
     * La animación corre sola una y otra vez. Se detiene "segundos" en el cuadro "cuadroDePausa"
     * (por ejemplo, cuando el dibujo ya está completo) y luego sigue hasta el final y vuelve a empezar.
     */
    public void iniciarCiclo(double segundos, double cuadro) {
        if (reproductor == null || enCiclo) {
            return;
        }
        enCiclo = true;
        segundosDePausa = segundos;
        cuadroDePausa = cuadro;
        primeraParte();
    }

    // true si el ciclo debe seguir; si el icono ya salió de la pantalla (se cambió de apartado) se apaga solo
    private boolean debeSeguir() {
        if (getScene() != null) {
            estuvoEnPantalla = true;
        }
        if (enCiclo && estuvoEnPantalla && getScene() == null) {
            detenerCiclo();
        }
        return enCiclo;
    }

    // Del cuadro 0 hasta el cuadro de pausa
    private void primeraParte() {
        if (!debeSeguir()) {
            return;
        }
        reproductor.seekToFrame(0);
        reproductor.play();
        esperar(cuadroDePausa / cuadrosPorSegundo(), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent evento) {
                pausa();
            }
        });
    }

    // Se queda quieta en el cuadro de pausa
    private void pausa() {
        if (!debeSeguir()) {
            return;
        }
        reproductor.stop();
        reproductor.seekToFrame(cuadroDePausa);
        esperar(segundosDePausa, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent evento) {
                segundaParte();
            }
        });
    }

    // Del cuadro de pausa hasta el final de la animación
    private void segundaParte() {
        if (!debeSeguir()) {
            return;
        }
        reproductor.seekToFrame(cuadroDePausa);
        reproductor.play();
        double faltan = ultimoCuadro() - cuadroDePausa;
        esperar(faltan / cuadrosPorSegundo(), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent evento) {
                primeraParte();
            }
        });
    }

    // Detiene el modo en ciclo y deja el icono en su cuadro de reposo
    public void detenerCiclo() {
        enCiclo = false;
        if (temporizador != null) {
            temporizador.stop();
        }
        detener();
    }

    private void esperar(double segundos, EventHandler<ActionEvent> alTerminar) {
        temporizador = new PauseTransition(Duration.seconds(segundos));
        temporizador.setOnFinished(alTerminar);
        temporizador.play();
    }

    private double cuadrosPorSegundo() {
        if (animacion == null || animacion.framesPerSecond() == null || animacion.framesPerSecond() <= 0) {
            return 30;
        }
        return animacion.framesPerSecond();
    }

    private double ultimoCuadro() {
        if (animacion == null || animacion.outPoint() == null) {
            return 150;
        }
        return animacion.outPoint();
    }

    // Hace que el icono se anime mientras el mouse esté sobre el nodo "anfitrion"
    // (sirve cuando el icono va dentro de otro componente, como un botón o una tarjeta)
    public void animarAlPasarMouse(Node anfitrion) {
        anfitrion.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                reproducir();
            }
        });
        anfitrion.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent evento) {
                detener();
            }
        });
    }

    // true si el archivo se pudo cargar con Lottie4J
    public boolean usaLottie() {
        return reproductor != null;
    }

    // Cuadro actual (útil para pruebas)
    public double getCuadroActual() {
        if (reproductor == null) {
            return 0;
        }
        return reproductor.getCurrentFrame();
    }
}

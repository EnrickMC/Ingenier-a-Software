package com.pac.controller;

import com.pac.session.TipoAcceso;
import com.pac.modulos.inventario.service.AlertasObserver;
import com.pac.modulos.inventario.service.MonitorAlertas;
import com.pac.modulos.pedidos.notificacion.NotificacionService;
import com.pac.modulos.pedidos.service.MonitorPedidos;
import com.pac.modulos.pedidos.service.PedidosObserver;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.navigation.NavegacionObserver;
import com.pac.navigation.Navegador;
import com.pac.navigation.SeccionPanel;
import com.pac.navigation.Vista;
import com.pac.modulos.configuracion.service.ConfiguracionService;
import com.pac.session.MonitorInactividad;
import com.pac.session.SesionObserver;
import com.pac.session.SesionUsuario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import com.pac.util.TemaVisual;
import com.pac.view.menu.ElementoMenu;
import com.pac.view.menu.FabricaMenu;
import com.pac.view.animacion.DecoracionIconos;
import com.pac.view.animacion.IconoLottie;
import com.pac.view.animacion.Rebote;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador del panel principal (panel-principal.fxml).
 * Tiene el menú lateral, la barra superior y el área donde se cargan los apartados.
 *
 * Es observador de la sesión (para mostrar quién entró), de la navegación
 * (para marcar en el menú el apartado activo) y de las alertas de inventario
 * (para actualizar el contador del menú).
 */
public class PanelPrincipalController implements SesionObserver, NavegacionObserver, AlertasObserver, PedidosObserver {

    // RF-39 / RF-40: cada cuánto se revisan existencias y caducidades mientras el panel está abierto
    private static final double HORAS_ENTRE_REVISIONES = 1;
    // RF-21: cada cuánto se revisa si entraron pedidos o pagos nuevos desde la web
    private static final double SEGUNDOS_ENTRE_PEDIDOS = 60;
    // Cada cuánto se revisa si la sesión lleva demasiado tiempo sin actividad
    private static final double SEGUNDOS_ENTRE_INACTIVIDAD = 20;

    @FXML private VBox contenedorMenu;
    @FXML private Label lblSubtituloSidebar;
    @FXML private Label lblTitulo;
    @FXML private StackPane iconoTitulo;
    @FXML private Label lblUsuario;
    @FXML private StackPane contenedorContenido;
    @FXML private VBox barraLateral;
    @FXML private ImageView imgLogo;
    @FXML private Button btnTema;
    @FXML private FontIcon iconoTema;

    private List<ElementoMenu> elementosMenu;
    private Timeline revisionAlertas;
    private Timeline revisionPedidos;
    private Timeline revisionInactividad;

    // JavaFX llama a este método al cargar la pantalla
    @FXML
    private void initialize() {
        // Parámetros de la tabla Configuracion (si falta la migración se usan los de app.properties)
        new ConfiguracionService().cargarEnAplicacion();

        // Se registra como observador de la sesión y de la navegación
        SesionUsuario sesion = SesionUsuario.getInstancia();
        Navegador navegador = Navegador.getInstancia();
        sesion.agregarObservador(this);
        navegador.agregarObservador(this);

        // RF-39 / RF-40: al iniciar sesión se revisan las alertas de inventario
        MonitorAlertas monitor = MonitorAlertas.getInstancia();
        monitor.revisar();
        monitor.agregarObservador(this);
        iniciarRevisionPeriodica();

        // Módulo 4: contadores de pedidos por revisar y pagos por validar, y avisos pendientes de reenvío
        MonitorPedidos monitorPedidos = MonitorPedidos.getInstancia();
        monitorPedidos.revisar();
        monitorPedidos.agregarObservador(this);
        iniciarRevisionPedidos();
        NotificacionService.getInstancia().reintentarPendientes();

        // Cada vez que se carga un apartado se ponen los iconos animados de adorno
        // (cajita en las tablas vacías, iconos en las tarjetas del Dashboard)
        final Runnable decorarPantalla = new Runnable() {
            @Override
            public void run() {
                for (Node pantalla : contenedorContenido.getChildren()) {
                    DecoracionIconos.aplicar(pantalla);
                }
            }
        };
        contenedorContenido.getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> cambio) {
                Platform.runLater(decorarPantalla);
            }
        });

        // Cierre de sesión por inactividad (parámetro "Minutos de inactividad")
        MonitorInactividad.getInstancia().registrarActividad();
        iniciarRevisionInactividad();

        // Se arma el menú lateral (cada botón ya viene con sus decoradores)
        armarMenu();

        // Animaciones del menú: los adornos pastel se mueven mientras el mouse está sobre el menú
        // y el logo rebota al pasar el mouse
        imgLogo.setOnMouseEntered(new Rebote(imgLogo, 1.12, 8));

        // El botón de la esquina muestra el modo al que se puede cambiar
        actualizarBotonTema();

        // Se muestra el usuario que ya inició sesión
        if (sesion.estaActiva()) {
            sesionIniciada(sesion.getUsuario(), sesion.getTipoAcceso());
        }

        // Se abre el dashboard como primer apartado
        navegador.registrarContenedor(contenedorContenido);
        navegador.irASeccion(SeccionPanel.DASHBOARD);
    }

    // Botón de la esquina superior derecha: alterna entre modo claro y semi oscuro
    @FXML
    private void onCambiarTema() {
        TemaVisual.alternar();
        actualizarBotonTema();
    }

    // Texto e ícono del botón: dicen a qué modo se va a cambiar
    private void actualizarBotonTema() {
        if (TemaVisual.esOscuro()) {
            btnTema.setText("Modo claro");
            iconoTema.setIconLiteral("bi-sun-fill");
        } else {
            btnTema.setText("Modo oscuro");
            iconoTema.setIconLiteral("bi-moon-stars-fill");
        }
    }

    // Crea los botones del menú; se vuelve a llamar cuando cambia algún contador
    private void armarMenu() {
        contenedorMenu.getChildren().clear();
        elementosMenu = FabricaMenu.crearMenu();
        for (ElementoMenu elemento : elementosMenu) {
            contenedorMenu.getChildren().add(elemento.crearNodo());
        }
        SeccionPanel actual = Navegador.getInstancia().getSeccionActual();
        if (actual != null) {
            seccionCambiada(actual);
        }
    }

    // Revisa las alertas cada hora mientras el panel esté abierto
    private void iniciarRevisionPeriodica() {
        // (JavaFX necesita la flecha "->" para indicar qué hacer cada vez que pasa el tiempo)
        KeyFrame cadaHora = new KeyFrame(Duration.hours(HORAS_ENTRE_REVISIONES),
                evento -> MonitorAlertas.getInstancia().revisar());
        revisionAlertas = new Timeline(cadaHora);
        revisionAlertas.setCycleCount(Animation.INDEFINITE);
        revisionAlertas.play();
    }

    // Revisa cada minuto si hay pedidos o pagos nuevos (para los contadores del menú)
    private void iniciarRevisionPedidos() {
        // (JavaFX necesita la flecha "->" para indicar qué hacer cada vez que pasa el tiempo)
        KeyFrame cadaMinuto = new KeyFrame(Duration.seconds(SEGUNDOS_ENTRE_PEDIDOS),
                evento -> revisarPedidos());
        revisionPedidos = new Timeline(cadaMinuto);
        revisionPedidos.setCycleCount(Animation.INDEFINITE);
        revisionPedidos.play();
    }

    // Cada minuto: contadores de pedidos y pagos, y avisos de pedidos nuevos de la página web (Módulo 2)
    private void revisarPedidos() {
        MonitorPedidos.getInstancia().revisar();
        NotificacionService.getInstancia().enviarAvisosDeLaWeb();
    }

    // RNF-16 · Excepción paso 7: cierre de sesión por inactividad.
    // Revisa cada 20 segundos si la sesión lleva demasiado tiempo sin usarse
    private void iniciarRevisionInactividad() {
        // (JavaFX necesita la flecha "->" para indicar qué hacer cada vez que pasa el tiempo)
        KeyFrame cadaRato = new KeyFrame(Duration.seconds(SEGUNDOS_ENTRE_INACTIVIDAD),
                evento -> revisarInactividad());
        revisionInactividad = new Timeline(cadaRato);
        revisionInactividad.setCycleCount(Animation.INDEFINITE);
        revisionInactividad.play();
    }

    private void revisarInactividad() {
        // Si este panel ya no está en pantalla, deja de revisar
        if (contenedorMenu.getScene() == null) {
            revisionInactividad.stop();
            return;
        }
        if (!MonitorInactividad.getInstancia().seVencio()) {
            return;
        }
        revisionInactividad.stop();
        SesionUsuario.getInstancia().cerrar();
        Navegador.getInstancia().mostrar(Vista.LOGIN_PERSONAL);
        // El aviso se muestra después: JavaFX no permite abrir ventanas durante una animación
        Platform.runLater(new AvisoInactividad());
    }

    // Muestra el aviso de sesión cerrada por inactividad
    private static class AvisoInactividad implements Runnable {
        @Override
        public void run() {
            Alertas.aviso(MonitorInactividad.MSG_SESION_INACTIVA);
        }
    }

    // ----- Observer de pedidos (Módulo 4) -----

    // Cambió el número de pedidos por revisar o de pagos por validar: se rearma el menú
    @Override
    public void pedidosActualizados(int porRevisar, int pagosPendientes, int folioNuevo) {
        if (contenedorMenu.getScene() == null && elementosMenu != null) {
            MonitorPedidos.getInstancia().quitarObservador(this);
            revisionPedidos.stop();
            return;
        }
        armarMenu();
    }

    // ----- Observer de alertas de inventario -----

    // Cambió el número de alertas pendientes: se vuelve a armar el menú con el nuevo contador
    @Override
    public void alertasActualizadas(int pendientes) {
        // Si este panel ya no está en pantalla (se abrió otro), deja de escuchar
        if (contenedorMenu.getScene() == null && elementosMenu != null) {
            MonitorAlertas.getInstancia().quitarObservador(this);
            revisionAlertas.stop();
            return;
        }
        armarMenu();
    }

    // ----- Observer de navegación -----

    // Cambia el título y marca el botón activo del menú
    @Override
    public void seccionCambiada(SeccionPanel seccion) {
        lblTitulo.setText(seccion.getTituloPantalla());
        ponerIconoTitulo(seccion);
        for (ElementoMenu elemento : elementosMenu) {
            Button boton = elemento.getBoton();
            boton.getStyleClass().remove("activo");
            if (elemento.getSeccion() == seccion) {
                boton.getStyleClass().add("activo");
            }
        }
    }

    // Icono animado del apartado junto al título: se mueve un momento al entrar y luego se queda quieto
    private void ponerIconoTitulo(SeccionPanel seccion) {
        final IconoLottie icono = new IconoLottie("enc_" + seccion.getArchivoLottie(), 36,
                59, seccion.getIcono());
        iconoTitulo.getChildren().setAll(icono);
        icono.reproducir();
        PauseTransition espera = new PauseTransition(Duration.seconds(2));
        espera.setOnFinished(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent evento) {
                icono.detener();
            }
        });
        espera.play();
    }

    // ----- Observer de sesión -----

    // Muestra en la barra superior quién está usando el panel
    @Override
    public void sesionIniciada(Usuario usuario, TipoAcceso tipoAcceso) {
        String quien;
        if (tipoAcceso == TipoAcceso.ADMINISTRADORA) {
            quien = "Administración";
        } else {
            quien = usuario.getNombreUsuario();
        }
        String fecha = Formatos.fechaLarga(LocalDateTime.now());
        lblUsuario.setText("Usuario: " + quien + " | " + fecha);
    }

    // Al cerrar sesión el panel deja de escuchar, porque se va a cerrar
    @Override
    public void sesionCerrada() {
        SesionUsuario.getInstancia().quitarObservador(this);
        Navegador.getInstancia().quitarObservador(this);
        MonitorAlertas.getInstancia().quitarObservador(this);
        MonitorPedidos.getInstancia().quitarObservador(this);
        if (revisionAlertas != null) {
            revisionAlertas.stop();
        }
        if (revisionPedidos != null) {
            revisionPedidos.stop();
        }
        if (revisionInactividad != null) {
            revisionInactividad.stop();
        }
    }

    // ----- Botones del pie del menú -----

    // "Bloquear pantalla": vuelve a la pantalla de bloqueo
    @FXML
    private void onBloquear() {
        SesionUsuario.getInstancia().cerrar();
        Navegador.getInstancia().mostrar(Vista.BLOQUEO);
    }

    // "Cerrar sesión": vuelve al inicio de sesión del Personal
    @FXML
    private void onCerrarSesion() {
        SesionUsuario.getInstancia().cerrar();
        Navegador.getInstancia().mostrar(Vista.LOGIN_PERSONAL);
    }
}

package com.pac.navigation;

import com.pac.modulos.usuarios.service.ControlAcceso;
import com.pac.modulos.usuarios.service.ResultadoAcceso;
import com.pac.session.MonitorInactividad;
import com.pac.session.SesionUsuario;
import com.pac.util.Alertas;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN SINGLETON + PATRÓN OBSERVER (sujeto).
 * Se encarga de cambiar de pantalla (bloqueo, login, panel) y de cargar
 * los apartados dentro del panel. Avisa a los observadores qué apartado quedó activo.
 *
 * Antes de abrir un apartado revisa los permisos con ControlAcceso (RNF-15).
 */
public class Navegador {

    private static final double ANCHO_VENTANA = 1200;
    private static final double ALTO_VENTANA = 750;

    // La única instancia de la clase
    private static Navegador instancia;

    private List<NavegacionObserver> observadores;
    private Stage ventana;
    private Scene escena;
    private Pane contenedorSecciones;
    private SeccionPanel seccionActual;

    // Constructor privado
    private Navegador() {
        observadores = new ArrayList<NavegacionObserver>();
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized Navegador getInstancia() {
        if (instancia == null) {
            instancia = new Navegador();
        }
        return instancia;
    }

    // Se llama una vez al iniciar la aplicación
    public void inicializar(Stage ventanaPrincipal) {
        ventana = ventanaPrincipal;
    }

    // Reemplaza todo el contenido de la ventana por la vista indicada
    public void mostrar(Vista vista) {
        // Los observadores pertenecen a la pantalla anterior; la nueva se registra sola
        observadores.clear();
        contenedorSecciones = null;
        seccionActual = null;

        FXMLLoader cargador = cargarFxml(vista.getRutaFxml());
        Parent raiz = cargador.getRoot();

        if (escena == null) {
            escena = new Scene(raiz, ANCHO_VENTANA, ALTO_VENTANA);
            // Cualquier movimiento del ratón o tecla cuenta como actividad (cierre por inactividad)
            escena.addEventFilter(MouseEvent.MOUSE_MOVED, MonitorInactividad.getInstancia());
            escena.addEventFilter(MouseEvent.MOUSE_PRESSED, MonitorInactividad.getInstancia());
            escena.addEventFilter(KeyEvent.KEY_PRESSED, MonitorInactividad.getInstancia());
            escena.addEventFilter(ScrollEvent.SCROLL, MonitorInactividad.getInstancia());
            ventana.setScene(escena);
        } else {
            escena.setRoot(raiz);
        }
    }

    // El panel principal registra aquí el área donde se cargan los apartados
    public void registrarContenedor(Pane contenedor) {
        contenedorSecciones = contenedor;
    }

    // Revisa los permisos y, si puede entrar, carga el apartado dentro del panel
    // RNF-15 (Restricción de acceso a módulos): inicia aquí. Paso 2 de las visualizaciones (RF-13, RF-17, RF-21, RF-30, RF-33, RF-41, RF-46...): valida la sesión y el permiso antes de abrir el apartado.
    public void irASeccion(SeccionPanel seccion) {
        if (contenedorSecciones == null) {
            System.out.println("El panel principal todavía no registra su contenedor");
            return;
        }

        ResultadoAcceso acceso = ControlAcceso.revisar(seccion.getPermisoRequerido());

        // RNF-15: tiene permiso; se abre el apartado (paso 3 del RF correspondiente).
        if (acceso == ResultadoAcceso.PERMITIDO) {
            cargarSeccion(seccion);
            return;
        }

        // RNF-15 · Excepción: los permisos cambiaron; se avisa y se vuelve a armar el menú.
        if (acceso == ResultadoAcceso.PERMISOS_CAMBIARON) {
            // Se vuelve a armar el panel para que el menú muestre los permisos nuevos
            Alertas.aviso(ControlAcceso.MSG_PERMISOS_CAMBIARON);
            mostrar(Vista.PANEL_PRINCIPAL);
            return;
        }

        // RNF-15 · Excepción: la cuenta está inactiva o la sesión venció; regresa al inicio de sesión.
        if (acceso == ResultadoAcceso.CUENTA_INACTIVA || acceso == ResultadoAcceso.SESION_VENCIDA) {
            String mensaje = ControlAcceso.MSG_SESION_VENCIDA;
            if (acceso == ResultadoAcceso.CUENTA_INACTIVA) {
                mensaje = ControlAcceso.MSG_CUENTA_INACTIVA;
            }
            SesionUsuario.getInstancia().cerrar();
            Alertas.aviso(mensaje);
            mostrar(Vista.LOGIN_PERSONAL);
            return;
        }

        // DENEGADO o SIN_VERIFICAR: no se abre y se queda donde estaba
        // RNF-15 · Excepción: no se pudieron verificar los permisos (sin conexión).
        if (acceso == ResultadoAcceso.SIN_VERIFICAR) {
            Alertas.error(ControlAcceso.MSG_SIN_VERIFICAR);
        } else {
            // Excepción paso 2 (RF-13, RF-17, RF-21, RF-33...): «Acceso denegado»; el apartado no se abre.
            Alertas.error(ControlAcceso.MSG_ACCESO_DENEGADO);
        }
        if (seccionActual == null) {
            cargarSeccion(SeccionPanel.DASHBOARD);
        }
    }

    // Carga el FXML del apartado y avisa a los observadores
    private void cargarSeccion(SeccionPanel seccion) {
        FXMLLoader cargador = cargarFxml(seccion.getRutaFxml());
        Object controlador = cargador.getController();
        if (controlador instanceof ControladorSeccion) {
            ControladorSeccion controladorSeccion = (ControladorSeccion) controlador;
            controladorSeccion.inicializarSeccion(seccion);
        }

        Parent vistaSeccion = cargador.getRoot();
        contenedorSecciones.getChildren().clear();
        contenedorSecciones.getChildren().add(vistaSeccion);
        seccionActual = seccion;

        // Se copia la lista para que un observador pueda quitarse sin causar errores
        List<NavegacionObserver> copia = new ArrayList<NavegacionObserver>(observadores);
        for (NavegacionObserver observador : copia) {
            observador.seccionCambiada(seccion);
        }
    }

    public SeccionPanel getSeccionActual() {
        return seccionActual;
    }

    // ----- Manejo de observadores -----

    public void agregarObservador(NavegacionObserver observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void quitarObservador(NavegacionObserver observador) {
        observadores.remove(observador);
    }

    // Lee un archivo FXML y devuelve el cargador con la vista y su controlador
    private FXMLLoader cargarFxml(String ruta) {
        FXMLLoader cargador = new FXMLLoader(Navegador.class.getResource(ruta));
        try {
            cargador.load();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar " + ruta, e);
        }
        return cargador;
    }
}

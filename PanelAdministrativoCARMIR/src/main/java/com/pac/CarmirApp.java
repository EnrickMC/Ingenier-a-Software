package com.pac;

import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.configuracion.service.ConfiguracionService;
import com.pac.navigation.Navegador;
import com.pac.navigation.Vista;
import com.pac.util.SelectorImagen;
import com.pac.util.TemaVisual;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;

/**
 * Clase principal del Panel Administrativo CARMIR.
 * Abre la ventana y muestra primero la pantalla de bloqueo.
 */
public class CarmirApp extends Application {

    // Sirve para abrir enlaces en el navegador (por ejemplo, el comprobante de un pago)
    private static HostServices servicios;

    @Override
    public void start(Stage ventana) {
        servicios = getHostServices();
        ventana.setTitle("CARMIR - Panel Administrativo");
        ventana.setMinWidth(1000);
        ventana.setMinHeight(650);

        // Parámetros guardados en la tabla Configuracion (si no se puede, se usan los de app.properties)
        new ConfiguracionService().cargarEnAplicacion();

        // Modo claro u oscuro (queda guardado entre aperturas)
        TemaVisual.iniciar();

        Navegador navegador = Navegador.getInstancia();
        navegador.inicializar(ventana);
        navegador.mostrar(Vista.BLOQUEO);

        ventana.show();
    }

    @Override
    public void stop() {
        // Al cerrar la ventana se libera la conexión a MySQL (si se abrió)
        ConexionBD.getInstancia().cerrar();
    }

    // Abre un enlace en el navegador del equipo; devuelve False si no se pudo
    public static boolean abrirEnlace(String enlace) {
        if (servicios == null || enlace == null || enlace.isBlank()) {
            return false;
        }
        servicios.showDocument(SelectorImagen.comoUrl(enlace));
        return true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

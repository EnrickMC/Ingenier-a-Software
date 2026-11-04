package com.pac.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * RNF-11 (Modo oscuro del panel administrativo): controla el modo claro y el modo semi oscuro del panel
 * (los dos con colores pastel).
 *
 * El modo oscuro consiste en ponerle la clase "tema-oscuro" a la raíz de cada ventana
 * y agregar la hoja tema-oscuro.css, que solo cambia las variables de color.
 * Se aplica a TODAS las ventanas: la principal, las flotantes, los avisos y las listas desplegables.
 * La elección se guarda en el equipo para que la próxima vez abra igual.
 */
public class TemaVisual {

    private static final String CLASE_OSCURO = "tema-oscuro";
    // Clase de la raíz de las pantallas que no cambian con el tema (inicio de sesión)
    private static final String CLASE_PANTALLA_SESION = "pantalla-sesion";
    private static final String HOJA_OSCURO = "/com/pac/css/tema-oscuro.css";
    private static final String LLAVE_PREFERENCIA = "modoOscuro";
    private static final String MARCA_ESCENA = "carmir.tema.vinculada";

    private static boolean oscuro = false;
    private static boolean iniciado = false;

    private TemaVisual() {
    }

    // Se llama una sola vez al abrir la aplicación
    public static void iniciar() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        oscuro = leerPreferencia();

        // Cada ventana que se abre (modales, avisos, listas desplegables) recibe el tema actual
        Window.getWindows().addListener(new ListChangeListener<Window>() {
            @Override
            public void onChanged(Change<? extends Window> cambio) {
                while (cambio.next()) {
                    if (cambio.wasAdded()) {
                        for (Window ventana : cambio.getAddedSubList()) {
                            vincular(ventana);
                        }
                    }
                }
            }
        });
        List<Window> abiertas = new ArrayList<Window>(Window.getWindows());
        for (Window ventana : abiertas) {
            vincular(ventana);
        }
    }

    public static boolean esOscuro() {
        return oscuro;
    }

    // Cambia entre claro y oscuro y lo aplica a todas las ventanas abiertas
    public static void alternar() {
        establecer(!oscuro);
    }

    public static void establecer(boolean valor) {
        oscuro = valor;
        // RNF-11 · Paso 3: guarda la elección en el equipo
        guardarPreferencia(valor);
        // RNF-11 · Paso 2: cambia los colores de todas las ventanas abiertas
        List<Window> abiertas = new ArrayList<Window>(Window.getWindows());
        for (Window ventana : abiertas) {
            if (ventana.getScene() != null) {
                aplicar(ventana.getScene());
            }
        }
    }

    // ---------- Aplicación del tema ----------

    // Aplica el tema a la escena de la ventana (o espera a que tenga una)
    private static void vincular(Window ventana) {
        if (ventana.getScene() != null) {
            vincularEscena(ventana.getScene());
            return;
        }
        // Todavía no tiene escena: se espera a que la reciba
        ventana.sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> valor, Scene anterior, Scene nueva) {
                if (nueva != null) {
                    vincularEscena(nueva);
                }
            }
        });
    }

    // Aplica el tema y vuelve a aplicarlo si la escena cambia de raíz (el Navegador cambia de pantalla así)
    private static void vincularEscena(Scene escena) {
        aplicar(escena);
        if (escena.getProperties().containsKey(MARCA_ESCENA)) {
            return;
        }
        escena.getProperties().put(MARCA_ESCENA, Boolean.TRUE);
        escena.rootProperty().addListener(new ChangeListener<Parent>() {
            @Override
            public void changed(ObservableValue<? extends Parent> valor, Parent anterior, Parent nueva) {
                if (nueva != null) {
                    aplicar(escena);
                }
            }
        });
    }

    // Pone o quita la clase y la hoja de estilos del modo oscuro
    private static void aplicar(Scene escena) {
        Parent raiz = escena.getRoot();
        String hoja = TemaVisual.class.getResource(HOJA_OSCURO).toExternalForm();
        // La pantalla de inicio de sesión siempre se ve igual: el modo oscuro no le afecta
        boolean usarOscuro = oscuro;
        if (raiz != null && raiz.getStyleClass().contains(CLASE_PANTALLA_SESION)) {
            usarOscuro = false;
        }
        if (usarOscuro) {
            if (raiz != null && !raiz.getStyleClass().contains(CLASE_OSCURO)) {
                raiz.getStyleClass().add(CLASE_OSCURO);
            }
            if (!escena.getStylesheets().contains(hoja)) {
                escena.getStylesheets().add(hoja);
            }
        } else {
            if (raiz != null) {
                raiz.getStyleClass().remove(CLASE_OSCURO);
            }
            escena.getStylesheets().remove(hoja);
        }
    }

    // ---------- Preferencia guardada ----------

    private static boolean leerPreferencia() {
        try {
            return Preferences.userNodeForPackage(TemaVisual.class).getBoolean(LLAVE_PREFERENCIA, false);
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static void guardarPreferencia(boolean valor) {
        try {
            Preferences.userNodeForPackage(TemaVisual.class).putBoolean(LLAVE_PREFERENCIA, valor);
        } catch (RuntimeException error) {
            // RNF-11 · Excepción paso 3: si no se puede guardar, el modo solo dura mientras la aplicación esté abierta
        }
    }
}

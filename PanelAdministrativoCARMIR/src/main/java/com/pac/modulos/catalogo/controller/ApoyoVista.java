package com.pac.modulos.catalogo.controller;

import com.pac.util.SelectorImagen;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Métodos de apoyo que comparten las pantallas del catálogo.
 */
public class ApoyoVista {

    // Clase de apoyo: no se crean objetos de ella
    private ApoyoVista() {
    }

    // Muestra u oculta un elemento sin dejar el hueco
    public static void mostrarNodo(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    // Pone el borde rojo a un campo
    public static void marcar(Node nodo) {
        if (!nodo.getStyleClass().contains("campo-error")) {
            nodo.getStyleClass().add("campo-error");
        }
    }

    // Quita el borde rojo de un campo
    public static void desmarcar(Node nodo) {
        nodo.getStyleClass().remove("campo-error");
    }

    // Lee un archivo FXML; devuelve null si no se pudo
    public static FXMLLoader cargar(String ruta) {
        FXMLLoader cargador = new FXMLLoader(ApoyoVista.class.getResource(ruta));
        try {
            cargador.load();
        } catch (IOException e) {
            System.out.println("No se pudo cargar " + ruta + ": " + e.getMessage());
            return null;
        }
        return cargador;
    }

    // Abre una ventana modal y espera a que se cierre.
    // El contenido va dentro de un ScrollPane: si no cabe en la pantalla, se puede deslizar hacia abajo.
    public static void mostrarModal(Parent raiz, Window duena, String titulo) {
        ScrollPane deslizador = new ScrollPane(raiz);
        deslizador.setFitToWidth(true);
        deslizador.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        deslizador.getStyleClass().add("scroll-modal");

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initOwner(duena);
        ventana.setTitle(titulo);
        ventana.setScene(new Scene(deslizador));
        // Estilo del deslizador (los formularios cargan el suyo)
        ventana.getScene().getStylesheets().add(ApoyoVista.class.getResource("/com/pac/css/carmir-base.css").toExternalForm());
        ventana.setResizable(true);

        // Tamaño inicial: el que pide el contenido, sin pasar del tamaño de la pantalla
        // (primero se aplican los estilos para que el tamaño incluya márgenes y letra)
        deslizador.applyCss();
        raiz.applyCss();
        double ancho = raiz.prefWidth(-1);
        double alto = raiz.prefHeight(ancho);
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        if (alto > area.getHeight() - 60) {
            alto = area.getHeight() - 60;
            ancho = ancho + 16;
        }
        deslizador.setPrefViewportWidth(ancho);
        deslizador.setPrefViewportHeight(alto);

        // La ventana nunca es más alta que la pantalla
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        ventana.setMaxHeight(pantalla.getHeight() - 20);
        ventana.setMaxWidth(pantalla.getWidth() - 20);
        ventana.showAndWait();
    }

    // Texto "Sí" / "No"
    public static String siNo(boolean valor) {
        if (valor) {
            return "Sí";
        }
        return "No";
    }

    // Texto vacío en lugar de null
    public static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }

    // Carga una imagen (de internet o de un archivo local) en segundo plano.
    // Si no hay enlace o la imagen falla, se muestra la etiqueta "Sin imagen".
    public static void cargarImagen(ImageView visor, Label sinImagen, String enlace) {
        if (enlace == null || enlace.isBlank()) {
            visor.setImage(null);
            mostrarNodo(visor, false);
            mostrarNodo(sinImagen, true);
            return;
        }
        Image imagen = new Image(SelectorImagen.comoUrl(enlace), true);
        visor.setImage(imagen);
        mostrarNodo(visor, true);
        mostrarNodo(sinImagen, false);

        // Flecha necesaria: JavaFX avisa por un "listener" cuando la descarga falla
        imagen.errorProperty().addListener((observable, antes, conError) -> {
            if (conError) {
                mostrarNodo(visor, false);
                mostrarNodo(sinImagen, true);
            }
        });
    }

    // Botón "Elegir imagen": abre el explorador de archivos y, si se eligió una imagen,
    // escribe su ruta en el campo. Devuelve true si se eligió una.
    public static boolean elegirImagen(TextField campo) {
        String ruta = SelectorImagen.elegir(campo.getScene().getWindow(), campo.getText());
        if (ruta == null) {
            return false;
        }
        campo.setText(ruta);
        desmarcar(campo);
        return true;
    }
}

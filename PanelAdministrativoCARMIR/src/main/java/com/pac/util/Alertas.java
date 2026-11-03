package com.pac.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Ventanas de mensaje que usan todos los módulos del panel.
 * - error / aviso / info: solo muestran un mensaje.
 * - confirmar: pregunta Sí / No.
 * - confirmarDoble: pregunta dos veces, para acciones críticas (RNF-5).
 */
public class Alertas {

    // Muestra un mensaje de error
    public static void error(String mensaje) {
        mostrar(Alert.AlertType.ERROR, "Error", mensaje);
    }

    // Muestra un mensaje de advertencia
    public static void aviso(String mensaje) {
        mostrar(Alert.AlertType.WARNING, "Aviso", mensaje);
    }

    // Muestra un mensaje informativo
    public static void info(String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, "Información", mensaje);
    }

    // Pregunta Sí / No; devuelve true si eligió "Sí"
    public static boolean confirmar(String titulo, String mensaje) {
        ButtonType botonSi = new ButtonType("Sí", ButtonBar.ButtonData.OK_DONE);
        ButtonType botonNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.getButtonTypes().setAll(botonSi, botonNo);
        aplicarEstilo(alerta);
        alerta.showAndWait();

        // getResult() devuelve el botón que se presionó
        return alerta.getResult() == botonSi;
    }

    // Pide la confirmación dos veces; devuelve true solo si aceptó ambas
    public static boolean confirmarDoble(String titulo, String mensaje) {
        if (!confirmar(titulo, mensaje)) {
            return false;
        }
        return confirmar("Confirma de nuevo", "Esta acción es importante. ¿Seguro que deseas continuar?");
    }

    // Arma y muestra una alerta de un solo botón
    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        aplicarEstilo(alerta);
        alerta.showAndWait();
    }

    // Les pone los colores y la letra del panel (las ventanas de aviso no heredan los estilos)
    private static void aplicarEstilo(Alert alerta) {
        String hoja = Alertas.class.getResource("/com/pac/css/carmir-base.css").toExternalForm();
        alerta.getDialogPane().getStylesheets().add(hoja);
        // Se evita el recorte del texto largo
        alerta.getDialogPane().setMinWidth(380);
    }
}

package com.pac.modulos.usuarios.controller;

import com.pac.navigation.Navegador;
import com.pac.navigation.Vista;
import com.pac.modulos.usuarios.service.AutenticacionService;
import com.pac.modulos.usuarios.service.ResultadoAutenticacion;
import com.pac.session.SesionUsuario;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * Controlador de la pantalla de bloqueo (bloqueo.fxml).
 * RNF-14: pantalla de inicio de sesión única; entran la Administradora y el Personal con su usuario y contraseña.
 */
public class BloqueoController {

    @FXML private Label lblHora;
    @FXML private Label lblFecha;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwdContrasena;
    @FXML private Label lblMensaje;
    @FXML private Label lblNegocio;

    private AutenticacionService autenticacion = new AutenticacionService();
    private Timeline reloj;

    // JavaFX llama a este método al cargar la pantalla
    @FXML
    private void initialize() {
        txtUsuario.clear();
        lblNegocio.setText(ConfiguracionApp.getInstancia().getNombreNegocio());
        ocultarMensaje();
        iniciarReloj();
    }

    // Actualiza la hora cada segundo
    private void iniciarReloj() {
        actualizarReloj();
        // La flecha "->" es necesaria para decirle al Timeline qué hacer cada segundo
        KeyFrame cadaSegundo = new KeyFrame(Duration.seconds(1), evento -> actualizarReloj());
        reloj = new Timeline(cadaSegundo);
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
    }

    private void actualizarReloj() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(Formatos.hora(ahora));
        lblFecha.setText(Formatos.fechaBloqueo(ahora));
    }

    // Botón "Desbloquear" o Enter en la contraseña
    @FXML
    // RNF-14 · Paso 1: inicio de sesión desde la pantalla de bloqueo (Administradora o Personal).
    private void onDesbloquear() {
        ocultarMensaje();
        String contrasena = pwdContrasena.getText();
        // Un solo inicio de sesión: el sistema decide si es la Administradora o el Personal
        ResultadoAutenticacion resultado = autenticacion.iniciarSesion(txtUsuario.getText(), contrasena);

        // RNF-14 · Excepciones pasos 2 a 6: datos vacíos, credenciales incorrectas, cuenta bloqueada o inactiva,
        // sin módulos asignados o sin conexión.
        if (!resultado.isExitoso()) {
            mostrarMensaje(resultado.getMensaje());
            pwdContrasena.clear();
            return;
        }

        // RNF-14 · Pasos 7 y 8: abre la sesión y muestra el panel principal con los módulos permitidos (RNF-12).
        reloj.stop();
        SesionUsuario.getInstancia().iniciar(resultado.getUsuario(), resultado.getTipoAcceso());
        Navegador.getInstancia().mostrar(Vista.PANEL_PRINCIPAL);
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }
}

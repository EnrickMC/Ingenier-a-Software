package com.pac.modulos.usuarios.controller;

import com.pac.session.TipoAcceso;
import com.pac.navigation.Navegador;
import com.pac.navigation.Vista;
import com.pac.modulos.usuarios.service.AutenticacionService;
import com.pac.modulos.usuarios.service.ResultadoAutenticacion;
import com.pac.session.SesionUsuario;
import com.pac.util.ConfiguracionApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controlador del inicio de sesión del Personal (login-personal.fxml).
 */
public class LoginPersonalController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwdContrasena;
    @FXML private Label lblMensaje;
    @FXML private Label lblNegocio;

    private AutenticacionService autenticacion = new AutenticacionService();

    // JavaFX llama a este método al cargar la pantalla
    @FXML
    private void initialize() {
        lblNegocio.setText(ConfiguracionApp.getInstancia().getNombreNegocio());
        ocultarMensaje();
    }

    // Botón "Ingresar" o Enter en la contraseña
    @FXML
    // RNF-14 · Paso 1: el Personal inicia sesión con usuario y contraseña.
    // (Pantalla anterior: Vista.LOGIN_PERSONAL ahora abre bloqueo.fxml, el inicio de sesión único.)
    private void onIngresar() {
        ocultarMensaje();
        String usuario = txtUsuario.getText();
        String contrasena = pwdContrasena.getText();
        ResultadoAutenticacion resultado = autenticacion.iniciarSesionPersonal(usuario, contrasena);

        // RNF-14 · Excepciones pasos 2 a 6: credenciales, cuenta inactiva o bloqueada, sin permisos o sin conexión.
        if (!resultado.isExitoso()) {
            mostrarMensaje(resultado.getMensaje());
            pwdContrasena.clear();
            return;
        }

        SesionUsuario.getInstancia().iniciar(resultado.getUsuario(), TipoAcceso.PERSONAL);
        Navegador.getInstancia().mostrar(Vista.PANEL_PRINCIPAL);
    }

    // Enlace "Volver a la pantalla de bloqueo"
    @FXML
    private void onVolverABloqueo() {
        Navegador.getInstancia().mostrar(Vista.BLOQUEO);
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

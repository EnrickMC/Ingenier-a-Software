package com.pac.modulos.configuracion.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.service.IntegracionesService;
import com.pac.modulos.configuracion.service.ResultadoConfiguracion;
import com.pac.modulos.pedidos.notificacion.ConfiguracionNotificaciones;
import com.pac.modulos.pedidos.notificacion.DatosNotificaciones;
import com.pac.util.Alertas;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

/**
 * Tarjeta "Notificaciones al cliente" de la pestaña Respaldos y sistema (integraciones.fxml).
 * RF-71 (Configuración de las cuentas de notificaciones): aquí se escriben los datos de WhatsApp
 * Business Cloud API y del correo SMTP (los usan los avisos de RF-47)
 * con los que el panel avisa a los clientes, se elige el modo (simulado o real) y se envían pruebas.
 *
 * El token y la contraseña nunca se muestran: el campo queda vacío y solo se indica si hay uno guardado.
 * Si se deja vacío al guardar, se conserva el que ya estaba.
 */
public class IntegracionesController {

    private static final String TEXTO_STARTTLS = "STARTTLS (587)";
    private static final String TEXTO_SSL = "SSL (465)";

    @FXML private Label lblError;
    @FXML private Label lblResultado;
    @FXML private Button btnGuardar;
    @FXML private FlowPane panelModo;
    @FXML private RadioButton rbSimulado;
    @FXML private RadioButton rbReal;
    @FXML private TextField txtReintentos;
    @FXML private TextField txtSegundos;

    @FXML private CheckBox chkWhatsApp;
    @FXML private CheckBox chkCorreo;
    @FXML private TextField txtWhatsAppId;
    @FXML private PasswordField txtWhatsAppToken;
    @FXML private Label lblEstadoToken;
    @FXML private Button btnQuitarToken;
    @FXML private TextField txtLada;
    @FXML private TextField txtVersionApi;
    @FXML private TextField txtPruebaWhatsApp;
    @FXML private Button btnProbarWhatsApp;

    @FXML private TextField txtSmtpServidor;
    @FXML private TextField txtSmtpPuerto;
    @FXML private ComboBox<String> cmbSeguridad;
    @FXML private TextField txtSmtpUsuario;
    @FXML private PasswordField txtSmtpContrasena;
    @FXML private Label lblEstadoContrasena;
    @FXML private Button btnQuitarContrasena;
    @FXML private TextField txtSmtpRemitente;
    @FXML private TextField txtPruebaCorreo;
    @FXML private Button btnProbarCorreo;

    private IntegracionesService servicio = new IntegracionesService();

    // true = la persona pidió borrar el secreto guardado (se borra al guardar)
    private boolean quitarToken = false;
    private boolean quitarContrasena = false;

    // false = falta la tabla ConfiguracionNotificacion: se puede probar pero no guardar
    private boolean hayTabla = true;

    @FXML
    private void initialize() {
        cmbSeguridad.getItems().add(TEXTO_STARTTLS);
        cmbSeguridad.getItems().add(TEXTO_SSL);
        ApoyoVista.mostrarNodo(lblError, false);
        ApoyoVista.mostrarNodo(lblResultado, false);
    }

    // RF-71: inicia aquí. Paso 1: muestra los datos guardados de WhatsApp y correo (sin el token ni la contraseña).
    public void cargar() {
        limpiarMarcas();
        ApoyoVista.mostrarNodo(lblError, false);
        quitarToken = false;
        quitarContrasena = false;

        hayTabla = servicio.cargarEnAplicacion();
        // RF-71 · Excepción paso 1: no se pudo leer la tabla; se usan los valores de notificaciones.properties.
        if (!hayTabla) {
            lblError.setText("No se pueden guardar estos datos: " + servicio.getMotivoError()
                    + ". Mientras tanto se usan los de config/notificaciones.properties.");
            ApoyoVista.mostrarNodo(lblError, true);
        }
        btnGuardar.setDisable(!hayTabla);

        DatosNotificaciones datos = ConfiguracionNotificaciones.getInstancia().getDatos();
        if (datos.isSimulado()) {
            rbSimulado.setSelected(true);
        } else {
            rbReal.setSelected(true);
        }
        txtReintentos.setText(String.valueOf(datos.getReintentos()));
        txtSegundos.setText(String.valueOf(datos.getSegundosEntreReintentos()));

        chkWhatsApp.setSelected(datos.isWhatsAppHabilitado());
        chkCorreo.setSelected(datos.isCorreoHabilitado());
        txtWhatsAppId.setText(datos.getWhatsAppIdNumero());
        txtLada.setText(datos.getWhatsAppLada());
        txtVersionApi.setText(datos.getWhatsAppVersionApi());

        txtSmtpServidor.setText(datos.getSmtpServidor());
        txtSmtpPuerto.setText(String.valueOf(datos.getSmtpPuerto()));
        if (DatosNotificaciones.SEGURIDAD_SSL.equals(datos.getSmtpSeguridad())) {
            cmbSeguridad.setValue(TEXTO_SSL);
        } else {
            cmbSeguridad.setValue(TEXTO_STARTTLS);
        }
        txtSmtpUsuario.setText(datos.getSmtpUsuario());
        txtSmtpRemitente.setText(datos.getSmtpRemitente());

        // Los secretos nunca se ponen en el campo
        txtWhatsAppToken.clear();
        txtSmtpContrasena.clear();
        mostrarEstadoSecretos();
    }

    // ===================== Botones =====================

    // RF-71 · Paso 2: el usuario modificó los datos y eligió «Guardar».
    @FXML
    private void onGuardar() {
        limpiarMarcas();
        ApoyoVista.mostrarNodo(lblError, false);
        ApoyoVista.mostrarNodo(lblResultado, false);
        // RF-71 · Paso 3: la sesión sigue activa y con el permiso «Configurar sistema».
        if (!AccesoConfiguracion.revisar()) {
            return;
        }
        DatosNotificaciones datos = leerFormulario();
        if (datos == null) {
            return;
        }
        // RF-71 · Pasos 3, 5 y 6: valida, guarda y aplica los datos nuevos (IntegracionesService).
        ResultadoConfiguracion resultado = servicio.guardar(datos, quitarToken, quitarContrasena);
        if (!resultado.isExitoso()) {
            mostrarError(resultado);
            return;
        }
        Alertas.info(resultado.getMensaje());
        cargar();
    }

    // RF-71 · Paso 2: pide quitar el token guardado (se borra al guardar).
    @FXML
    private void onQuitarToken() {
        quitarToken = true;
        txtWhatsAppToken.clear();
        mostrarEstadoSecretos();
    }

    @FXML
    private void onQuitarContrasena() {
        quitarContrasena = true;
        txtSmtpContrasena.clear();
        mostrarEstadoSecretos();
    }

    @FXML
    private void onProbarWhatsApp() {
        iniciarPrueba(true);
    }

    @FXML
    private void onProbarCorreo() {
        iniciarPrueba(false);
    }

    // ===================== Pruebas en segundo plano =====================

    // RF-71 · Paso 2: el usuario eligió «Enviar prueba»; RF-71 · Paso 4: la prueba usa lo que está escrito
    // en el formulario (no hace falta guardar antes) y se envía en segundo plano
    private void iniciarPrueba(boolean esWhatsApp) {
        limpiarMarcas();
        ApoyoVista.mostrarNodo(lblError, false);
        if (!AccesoConfiguracion.revisar()) {
            return;
        }
        DatosNotificaciones datos = leerFormulario();
        if (datos == null) {
            return;
        }
        String destino;
        if (esWhatsApp) {
            destino = txtPruebaWhatsApp.getText();
        } else {
            destino = txtPruebaCorreo.getText();
        }
        TareaPrueba tarea = new TareaPrueba(servicio, datos, esWhatsApp, destino, quitarToken, quitarContrasena);
        tarea.setOnSucceeded(new AlTerminarPrueba(tarea));
        tarea.setOnFailed(new AlTerminarPrueba(tarea));

        bloquearBotones(true);
        lblResultado.setText("Enviando mensaje de prueba…");
        ApoyoVista.mostrarNodo(lblResultado, true);
        Thread hilo = new Thread(tarea, "carmir-prueba-notificacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    // RF-71 · Paso 4 y Excepción paso 4: muestra el resultado de la prueba (ya en el hilo de la pantalla)
    private class AlTerminarPrueba implements EventHandler<WorkerStateEvent> {
        private final TareaPrueba tarea;

        AlTerminarPrueba(TareaPrueba tarea) {
            this.tarea = tarea;
        }

        @Override
        public void handle(WorkerStateEvent evento) {
            bloquearBotones(false);
            ApoyoVista.mostrarNodo(lblResultado, false);
            ResultadoConfiguracion resultado = tarea.getValue();
            if (resultado == null) {
                String detalle = "";
                if (tarea.getException() != null) {
                    detalle = ": " + tarea.getException().getMessage();
                }
                Alertas.error("No se pudo enviar la prueba" + detalle);
                return;
            }
            if (resultado.isExitoso()) {
                Alertas.info(resultado.getMensaje());
            } else {
                mostrarError(resultado);
            }
        }
    }

    // Envía la prueba fuera del hilo de la pantalla (puede tardar varios segundos)
    private static class TareaPrueba extends Task<ResultadoConfiguracion> {
        private final IntegracionesService servicio;
        private final DatosNotificaciones datos;
        private final boolean esWhatsApp;
        private final String destino;
        private final boolean quitarToken;
        private final boolean quitarContrasena;

        TareaPrueba(IntegracionesService servicio, DatosNotificaciones datos, boolean esWhatsApp, String destino,
                    boolean quitarToken, boolean quitarContrasena) {
            this.servicio = servicio;
            this.datos = datos;
            this.esWhatsApp = esWhatsApp;
            this.destino = destino;
            this.quitarToken = quitarToken;
            this.quitarContrasena = quitarContrasena;
        }

        @Override
        protected ResultadoConfiguracion call() {
            if (esWhatsApp) {
                return servicio.probarWhatsApp(datos, quitarToken, destino);
            }
            return servicio.probarCorreo(datos, quitarContrasena, destino);
        }
    }

    // ===================== Apoyo =====================

    // RF-71 · Excepción paso 3: arma los datos con lo escrito; devuelve null (y marca el campo) si un número no es válido
    private DatosNotificaciones leerFormulario() {
        DatosNotificaciones datos = new DatosNotificaciones();
        if (rbReal.isSelected()) {
            datos.setModo(DatosNotificaciones.MODO_REAL);
        } else {
            datos.setModo(DatosNotificaciones.MODO_SIMULADO);
        }
        int reintentos = leerNumero(txtReintentos, "Los reintentos deben ser un número entero");
        if (reintentos < 0) {
            return null;
        }
        int segundos = leerNumero(txtSegundos, "Los segundos entre reintentos deben ser un número entero");
        if (segundos < 0) {
            return null;
        }
        int puerto = leerNumero(txtSmtpPuerto, "El puerto del correo debe ser un número (normalmente 587 o 465)");
        if (puerto < 0) {
            return null;
        }
        datos.setReintentos(reintentos);
        datos.setSegundosEntreReintentos(segundos);

        datos.setWhatsAppHabilitado(chkWhatsApp.isSelected());
        datos.setCorreoHabilitado(chkCorreo.isSelected());
        datos.setWhatsAppIdNumero(txtWhatsAppId.getText());
        datos.setWhatsAppToken(txtWhatsAppToken.getText());
        datos.setWhatsAppLada(txtLada.getText());
        datos.setWhatsAppVersionApi(txtVersionApi.getText());

        datos.setSmtpServidor(txtSmtpServidor.getText());
        datos.setSmtpPuerto(puerto);
        if (TEXTO_SSL.equals(cmbSeguridad.getValue())) {
            datos.setSmtpSeguridad(DatosNotificaciones.SEGURIDAD_SSL);
        } else {
            datos.setSmtpSeguridad(DatosNotificaciones.SEGURIDAD_STARTTLS);
        }
        datos.setSmtpUsuario(txtSmtpUsuario.getText());
        datos.setSmtpContrasena(txtSmtpContrasena.getText());
        datos.setSmtpRemitente(txtSmtpRemitente.getText());
        return datos;
    }

    // Lee un número entero del campo; si no es válido marca el campo y devuelve -1
    private int leerNumero(TextField campo, String mensajeError) {
        String texto = "";
        if (campo.getText() != null) {
            texto = campo.getText().trim();
        }
        try {
            int numero = Integer.parseInt(texto);
            if (numero >= 0) {
                return numero;
            }
        } catch (NumberFormatException e) {
            // se avisa abajo
        }
        ApoyoVista.marcar(campo);
        campo.requestFocus();
        lblError.setText(mensajeError);
        ApoyoVista.mostrarNodo(lblError, true);
        return -1;
    }

    // RF-71 · Paso 1: texto junto al token y la contraseña, si hay uno guardado (sin mostrarlo)
    // RF-71 · Excepción paso 1: si se guardó en otra computadora no se puede leer y hay que escribirlo de nuevo
    private void mostrarEstadoSecretos() {
        String[] textosToken = {
                "Todavía no hay un token guardado.",
                "Hay un token guardado. Escribe otro solo si quieres cambiarlo.",
                "No se pudo leer el token guardado (se guardó en otra computadora). Escríbelo de nuevo."};
        String[] textosContrasena = {
                "Todavía no hay una contraseña guardada.",
                "Hay una contraseña guardada. Escribe otra solo si quieres cambiarla.",
                "No se pudo leer la contraseña guardada (se guardó en otra computadora). Escríbela de nuevo."};
        mostrarEstado(DatosNotificaciones.CLAVE_WHATSAPP_TOKEN, quitarToken, lblEstadoToken, btnQuitarToken,
                txtWhatsAppToken, textosToken);
        mostrarEstado(DatosNotificaciones.CLAVE_SMTP_CONTRASENA, quitarContrasena, lblEstadoContrasena,
                btnQuitarContrasena, txtSmtpContrasena, textosContrasena);
    }

    // textos = {sin guardar, guardado, no se puede leer}
    private void mostrarEstado(String clave, boolean seQuita, Label etiqueta, Button botonQuitar,
                               PasswordField campo, String[] textos) {
        if (seQuita) {
            etiqueta.setText("Se quitará al guardar.");
            campo.setPromptText("");
            ApoyoVista.mostrarNodo(botonQuitar, false);
            return;
        }
        int estado = servicio.estadoSecreto(clave);
        if (estado == IntegracionesService.SECRETO_GUARDADO) {
            etiqueta.setText(textos[1]);
            campo.setPromptText("•••••••• (guardado)");
            ApoyoVista.mostrarNodo(botonQuitar, true);
        } else if (estado == IntegracionesService.SECRETO_ILEGIBLE) {
            etiqueta.setText(textos[2]);
            campo.setPromptText("Escríbelo de nuevo");
            ApoyoVista.mostrarNodo(botonQuitar, true);
        } else {
            etiqueta.setText(textos[0]);
            campo.setPromptText("Pégalo aquí");
            ApoyoVista.mostrarNodo(botonQuitar, false);
        }
    }

    private void mostrarError(ResultadoConfiguracion resultado) {
        Node campo = nodoDe(resultado.getCampo());
        if (campo != null) {
            ApoyoVista.marcar(campo);
            campo.requestFocus();
        }
        lblError.setText(resultado.getMensaje());
        ApoyoVista.mostrarNodo(lblError, true);
    }

    private void bloquearBotones(boolean bloquear) {
        btnProbarWhatsApp.setDisable(bloquear);
        btnProbarCorreo.setDisable(bloquear);
        if (bloquear) {
            btnGuardar.setDisable(true);
        } else {
            btnGuardar.setDisable(!hayTabla);
        }
    }

    // Campo del formulario que corresponde a cada clave (para marcarlo en rojo)
    private Node nodoDe(String clave) {
        if (clave == null) {
            return null;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_MODO)) {
            return panelModo;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_REINTENTOS)) {
            return txtReintentos;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SEGUNDOS_REINTENTO)) {
            return txtSegundos;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO)) {
            return txtWhatsAppId;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_WHATSAPP_TOKEN)) {
            return txtWhatsAppToken;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_WHATSAPP_LADA)) {
            return txtLada;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_WHATSAPP_VERSION)) {
            return txtVersionApi;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SMTP_SERVIDOR)) {
            return txtSmtpServidor;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SMTP_PUERTO)) {
            return txtSmtpPuerto;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SMTP_SEGURIDAD)) {
            return cmbSeguridad;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SMTP_USUARIO)) {
            return txtSmtpUsuario;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SMTP_CONTRASENA)) {
            return txtSmtpContrasena;
        }
        if (clave.equals(DatosNotificaciones.CLAVE_SMTP_REMITENTE)) {
            return txtSmtpRemitente;
        }
        if (clave.equals(IntegracionesService.CAMPO_PRUEBA_WHATSAPP)) {
            return txtPruebaWhatsApp;
        }
        if (clave.equals(IntegracionesService.CAMPO_PRUEBA_CORREO)) {
            return txtPruebaCorreo;
        }
        return null;
    }

    private void limpiarMarcas() {
        Node[] campos = {panelModo, txtReintentos, txtSegundos, txtWhatsAppId, txtWhatsAppToken, txtLada,
                txtVersionApi, txtPruebaWhatsApp, txtSmtpServidor, txtSmtpPuerto, cmbSeguridad, txtSmtpUsuario,
                txtSmtpContrasena, txtSmtpRemitente, txtPruebaCorreo};
        for (Node campo : campos) {
            ApoyoVista.desmarcar(campo);
        }
    }
}

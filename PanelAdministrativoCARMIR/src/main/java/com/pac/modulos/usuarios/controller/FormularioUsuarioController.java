package com.pac.modulos.usuarios.controller;

import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Permisos;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.modulos.usuarios.service.ResultadoOperacion;
import com.pac.modulos.usuarios.service.UsuarioService;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la ventana "Nuevo usuario" / "Modificar usuario" (usuario-formulario.fxml).
 * RNF-11 (registro) y RNF-14 (modificación).
 */
public class FormularioUsuarioController {

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtUsuario;
    @FXML private VBox cajaEstado;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private CheckBox chkRestablecer;
    @FXML private PasswordField pwdContrasena;
    @FXML private PasswordField pwdConfirmacion;
    @FXML private Label lblAyudaContrasena;
    @FXML private GridPane contenedorPermisos;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private UsuarioService servicio = new UsuarioService();

    // Usuario que se está modificando (null = registro nuevo)
    private Usuario usuarioEditado;

    // Una casilla por permiso; cada casilla guarda su Permiso en setUserData
    private List<CheckBox> casillasPermisos = new ArrayList<CheckBox>();

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";

    // JavaFX llama a este método al cargar la ventana
    @FXML
    private void initialize() {
        for (EstadoUsuario estado : EstadoUsuario.values()) {
            cmbEstado.getItems().add(estado.getValorBD());
        }
        ocultarMensaje();
    }

    // ===================== Preparar la ventana =====================

    // Formulario vacío para registrar un usuario nuevo
    // RNF-11 · Paso 2: formulario vacío (nombre, usuario, contraseña, confirmación y permisos).
    public void prepararNuevo(List<Permiso> permisos) {
        usuarioEditado = null;
        lblTitulo.setText("Nuevo usuario");
        lblSubtitulo.setText("El usuario se guardará como «Activo».");
        mostrarNodo(cajaEstado, false);
        mostrarNodo(chkRestablecer, false);
        activarContrasena(true);
        crearCasillas(permisos, null);
    }

    // Formulario con los datos del usuario (sin la contraseña)
    // RNF-14 · Paso 3: formulario con los datos actuales (sin la contraseña).
    public void prepararEdicion(Usuario usuario, List<Permiso> permisos) {
        usuarioEditado = usuario;
        lblTitulo.setText("Modificar usuario");
        lblSubtitulo.setText("La contraseña actual no se muestra. Márcala solo si quieres restablecerla.");
        txtNombre.setText(usuario.getNombreCompleto());
        txtUsuario.setText(usuario.getNombreUsuario());
        cmbEstado.setValue(usuario.getEstado().getValorBD());
        mostrarNodo(cajaEstado, true);
        mostrarNodo(chkRestablecer, true);
        chkRestablecer.setSelected(false);
        activarContrasena(false);
        crearCasillas(permisos, usuario);
    }

    // Crea una casilla por permiso, en dos columnas, y marca las que ya tiene el usuario
    private void crearCasillas(List<Permiso> permisos, Usuario usuario) {
        contenedorPermisos.getChildren().clear();
        casillasPermisos.clear();

        for (int i = 0; i < permisos.size(); i++) {
            Permiso permiso = permisos.get(i);
            CheckBox casilla = new CheckBox(permiso.getNombrePermiso());
            casilla.setUserData(permiso);
            casilla.getStyleClass().add("casilla-permiso");
            if (usuario != null && usuario.tienePermiso(permiso.getNombrePermiso())) {
                casilla.setSelected(true);
            }

            int columna = i % 2;
            int fila = i / 2;
            contenedorPermisos.add(casilla, columna, fila);
            casillasPermisos.add(casilla);
        }
    }

    // ===================== Botones =====================

    // Casilla "Restablecer contraseña" (solo al modificar)
    @FXML
    private void onCambiarRestablecer() {
        activarContrasena(chkRestablecer.isSelected());
    }

    // Botón "Guardar"
    @FXML
    // RNF-11 · Paso 3 / RNF-14 · Paso 4: la Administradora eligió «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ocultarMensaje();

        Usuario datos = leerFormulario();
        String contrasena = pwdContrasena.getText();
        String confirmacion = pwdConfirmacion.getText();
        ResultadoOperacion resultado;

        if (usuarioEditado == null) {
            resultado = servicio.registrar(datos, contrasena, confirmacion);

            // El nombre de usuario es de una cuenta dada de baja: se ofrece reactivarla
            // RNF-11 · Excepción paso 5: cuenta dada de baja con ese usuario; se ofrece reactivarla.
            if (resultado.isOfrecerReactivar()) {
                boolean reactivar = Alertas.confirmar("Cuenta dada de baja", resultado.getMensaje());
                if (!reactivar) {
                    marcar(txtUsuario);
                    return;
                }
                resultado = servicio.reactivar(resultado.getIdUsuarioExistente(), datos, contrasena);
            }
        } else {
            resultado = servicio.modificar(datos, chkRestablecer.isSelected(), contrasena, confirmacion);
        }

        if (resultado.isExitoso()) {
            guardado = true;
            mensajeExito = resultado.getMensaje();
            cerrarVentana();
            return;
        }

        // El usuario ya no existe: se avisa y se recarga la lista
        // RNF-14 · Excepción paso 2: el usuario ya no existe.
        if (resultado.isRecargarLista()) {
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrarVentana();
            return;
        }

        mostrarError(resultado);
    }

    // Botón "Cancelar": se descartan los datos y la lista no cambia
    @FXML
    // RNF-11 · Excepción paso 3 / RNF-14 · Excepción paso 4: «Cancelar» descarta los datos.
    private void onCancelar() {
        cerrarVentana();
    }

    // ===================== Lectura y errores =====================

    // Arma un Usuario con lo que se escribió en el formulario
    private Usuario leerFormulario() {
        Usuario datos = new Usuario();
        if (usuarioEditado != null) {
            datos.setIdUsuario(usuarioEditado.getIdUsuario());
        }
        datos.setNombreCompleto(txtNombre.getText().trim());
        datos.setNombreUsuario(txtUsuario.getText().trim());

        if (usuarioEditado != null && cmbEstado.getValue() != null) {
            datos.setEstado(EstadoUsuario.desdeBD(cmbEstado.getValue()));
        } else {
            datos.setEstado(EstadoUsuario.ACTIVO);
        }

        for (CheckBox casilla : casillasPermisos) {
            if (casilla.isSelected()) {
                Permiso permiso = (Permiso) casilla.getUserData();
                datos.agregarPermiso(permiso);
            }
        }
        return datos;
    }

    // Muestra el mensaje y resalta el campo que tiene el problema
    // RNF-11 / RNF-14 · Excepciones pasos 4 a 6: resalta el campo con error.
    private void mostrarError(ResultadoOperacion resultado) {
        mostrarMensaje(resultado.getMensaje());
        String campo = resultado.getCampo();
        if (campo == null) {
            return;
        }

        if (campo.equals(ResultadoOperacion.CAMPO_NOMBRE)) {
            marcar(txtNombre);
        } else if (campo.equals(ResultadoOperacion.CAMPO_USUARIO)) {
            marcar(txtUsuario);
        } else if (campo.equals(ResultadoOperacion.CAMPO_CONTRASENA)) {
            // Se indica el problema sin mostrar la contraseña y se limpian ambos campos
            marcar(pwdContrasena);
            marcar(pwdConfirmacion);
            limpiarContrasenas();
        } else if (campo.equals(ResultadoOperacion.CAMPO_PERMISOS)) {
            marcar(contenedorPermisos);
        } else if (campo.equals(ResultadoOperacion.CAMPO_PERMISO_PROPIO)) {
            // Se vuelve a marcar el permiso "Gestionar usuarios"
            restablecerPermisoPropio();
            marcar(contenedorPermisos);
        } else if (campo.equals(ResultadoOperacion.CAMPO_ESTADO)) {
            cmbEstado.setValue(EstadoUsuario.ACTIVO.getValorBD());
            marcar(cmbEstado);
        } else if (campo.equals(ResultadoOperacion.CAMPO_CONEXION)) {
            // Se conserva el formulario, pero sin la contraseña
            limpiarContrasenas();
        }
    }

    // Vuelve a marcar la casilla "Gestionar usuarios"
    private void restablecerPermisoPropio() {
        for (CheckBox casilla : casillasPermisos) {
            Permiso permiso = (Permiso) casilla.getUserData();
            if (permiso.getNombrePermiso().equalsIgnoreCase(Permisos.GESTIONAR_USUARIOS)) {
                casilla.setSelected(true);
            }
        }
    }

    // Activa o desactiva los campos de contraseña
    private void activarContrasena(boolean activa) {
        pwdContrasena.setDisable(!activa);
        pwdConfirmacion.setDisable(!activa);
        lblAyudaContrasena.setDisable(!activa);
        limpiarContrasenas();
    }

    private void limpiarContrasenas() {
        pwdContrasena.clear();
        pwdConfirmacion.clear();
    }

    // Pone el borde rojo a un campo
    private void marcar(Node nodo) {
        if (!nodo.getStyleClass().contains("campo-error")) {
            nodo.getStyleClass().add("campo-error");
        }
    }

    // Quita el borde rojo de todos los campos
    private void quitarMarcas() {
        txtNombre.getStyleClass().remove("campo-error");
        txtUsuario.getStyleClass().remove("campo-error");
        cmbEstado.getStyleClass().remove("campo-error");
        pwdContrasena.getStyleClass().remove("campo-error");
        pwdConfirmacion.getStyleClass().remove("campo-error");
        contenedorPermisos.getStyleClass().remove("campo-error");
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
        mostrarNodo(lblMensaje, true);
        // La ventana crece para que el mensaje no tape los botones
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.sizeToScene();
    }

    private void ocultarMensaje() {
        mostrarNodo(lblMensaje, false);
    }

    private void mostrarNodo(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    private void cerrarVentana() {
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.close();
    }

    // ===================== Lo que lee UsuariosController al cerrar =====================

    public boolean seGuardo() {
        return guardado;
    }

    public boolean debeRecargarLista() {
        return recargarLista;
    }

    public String getMensajeExito() {
        return mensajeExito;
    }
}

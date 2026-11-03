package com.pac.modulos.usuarios.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.pedidos.controller.BarraEstados;
import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.modulos.usuarios.service.ResultadoOperacion;
import com.pac.modulos.usuarios.service.UsuarioService;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

/**
 * Controlador del apartado "Usuarios y Permisos" (usuarios.fxml). Módulo 7.
 *
 * - RNF-13 Visualización: lista con búsqueda, filtro por estado y detalle.
 * - RNF-11 Registro y RNF-14 Modificación: abren usuario-formulario.fxml.
 * - RNF-12 Baja: confirmación doble y baja lógica.
 */
public class UsuariosController implements ControladorSeccion {

    private static final String RUTA_FORMULARIO = "/com/pac/view/usuarios/usuario-formulario.fxml";

    // Barra de búsqueda
    @FXML private TextField txtBuscar;
    @FXML private BarraEstados barraEstados;

    // Tabla
    @FXML private TableView<FilaUsuario> tblUsuarios;
    @FXML private Label lblConteo;
    @FXML private Label lblVacio;

    // Detalle del usuario seleccionado
    @FXML private VBox cajaDetalle;
    @FXML private VBox cajaSinSeleccion;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetUsuario;
    @FXML private Label lblDetEstado;
    @FXML private Label lblDetUltimoAcceso;
    @FXML private Label lblDetIntentos;
    @FXML private VBox listaDetPermisos;
    @FXML private Button btnDarDeBaja;

    private UsuarioService servicio = new UsuarioService();

    // Usuario que se muestra en el detalle (null si no hay ninguno)
    private Usuario usuarioSeleccionado;

    // El Navegador llama a este método al abrir el apartado
    @Override
    // RNF-13 (Visualización de usuarios): inicia aquí. Paso 1: se abre el apartado (el permiso se revisó en el paso 2, Navegador).
    public void inicializarSeccion(SeccionPanel seccion) {
        // Botones de estado: cada uno filtra la lista
        barraEstados.agregar(EstadoUsuario.ACTIVO.getValorBD(), "Activo", "estado-terminado", "bi-check-circle-fill");
        barraEstados.agregar(EstadoUsuario.INACTIVO.getValorBD(), "Inactivo", "estado-baja", "bi-slash-circle-fill");
        barraEstados.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onBuscar();
            }
        });

        cargarUsuarios();
    }

    // ===================== Lista de usuarios (RNF-13) =====================

    // Consulta la base de datos con el texto y el estado elegidos y llena la tabla
    // RNF-13 · Pasos 3 a 5: consulta y muestra la lista (sin contraseñas).
    private void cargarUsuarios() {
        String texto = txtBuscar.getText();
        EstadoUsuario estado = leerFiltroEstado();

        tblUsuarios.getItems().clear();
        mostrarDetalle(null);

        List<Usuario> usuarios;
        try {
            usuarios = servicio.buscar(texto, estado);
        } catch (DAOException e) {
            // RNF-13 · Excepción paso 3: «Error de conexión» con «Reintentar», sin información parcial.
            // Sin conexión no se muestra información parcial
            lblVacio.setText("Error de conexión");
            lblConteo.setText("");
            return;
        }

        for (Usuario usuario : usuarios) {
            tblUsuarios.getItems().add(new FilaUsuario(usuario));
        }
        lblConteo.setText(usuarios.size() + " usuario(s)");

        // Mensajes cuando la tabla queda vacía
        if (usuarios.isEmpty()) {
            if (hayFiltro()) {
                // RNF-13 · Excepción paso 4 / RNF-12 y RNF-14 · Excepción paso 1: «No se encontraron usuarios con ese criterio».
                lblVacio.setText("No se encontraron usuarios con ese criterio");
            } else {
                // RNF-13 · Excepción paso 4: «Aún no hay usuarios del personal registrados» (con «+ Nuevo usuario»).
                lblVacio.setText("Aún no hay usuarios del personal registrados");
            }
        }
    }

    // Convierte lo elegido en el ComboBox al estado del enum (null = todos)
    private EstadoUsuario leerFiltroEstado() {
        String valor = barraEstados.getClave();
        if (valor == null) {
            return null;
        }
        return EstadoUsuario.desdeBD(valor);
    }

    // true si se escribió algo en la búsqueda o se eligió un estado
    private boolean hayFiltro() {
        boolean hayTexto = txtBuscar.getText() != null && !txtBuscar.getText().isBlank();
        return hayTexto || leerFiltroEstado() != null;
    }

    // Botón "Buscar", Enter en la búsqueda o cambio de estado
    @FXML
    private void onBuscar() {
        cargarUsuarios();
    }

    // Botón "Limpiar filtro"
    @FXML
    private void onLimpiarFiltro() {
        txtBuscar.clear();
        barraEstados.limpiar();
        cargarUsuarios();
    }

    // Botón "Reintentar" cuando no hubo conexión
    @FXML
    private void onReintentar() {
        cargarUsuarios();
    }

    // ===================== Detalle =====================

    // Clic sobre la tabla: un clic muestra el detalle, doble clic abre "Modificar"
    @FXML
    // RNF-13 · Paso 6: la Administradora selecciona un usuario.
    private void onClicTabla(MouseEvent evento) {
        boolean seleccionado = mostrarSeleccionado();
        if (seleccionado && evento.getClickCount() == 2) {
            onEditar();
        }
    }

    // Al moverse con las flechas del teclado también se actualiza el detalle
    @FXML
    private void onTeclaTabla() {
        mostrarSeleccionado();
    }

    // Lee de nuevo el usuario seleccionado en la base y lo muestra; false si no se pudo
    private boolean mostrarSeleccionado() {
        FilaUsuario fila = tblUsuarios.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return false;
        }
        int id = fila.getUsuario().getIdUsuario();
        if (usuarioSeleccionado != null && usuarioSeleccionado.getIdUsuario() == id) {
            return true;
        }

        Usuario usuario;
        try {
            usuario = servicio.buscarPorId(id);
        } catch (DAOException e) {
            // RNF-13 · Excepción paso 6: sin conexión.
            Alertas.error(UsuarioService.MSG_ERROR_CONEXION);
            return false;
        }
        // RNF-13 · Excepción paso 6: «Usuario no encontrado»; se recarga la lista.
        if (usuario == null) {
            Alertas.aviso(UsuarioService.MSG_NO_ENCONTRADO);
            cargarUsuarios();
            return false;
        }
        mostrarDetalle(usuario);
        return true;
    }

    // Llena la tarjeta de detalle (o la oculta si usuario es null)
    private void mostrarDetalle(Usuario usuario) {
        usuarioSeleccionado = usuario;
        mostrarNodo(cajaDetalle, usuario != null);
        mostrarNodo(cajaSinSeleccion, usuario == null);
        if (usuario == null) {
            return;
        }

        lblDetNombre.setText(usuario.getNombreCompleto());
        lblDetUsuario.setText("@" + usuario.getNombreUsuario());
        lblDetEstado.setText(usuario.getEstado().getValorBD());
        lblDetEstado.getStyleClass().removeAll("estado-activo", "estado-inactivo", "estado-bloqueado");
        lblDetEstado.getStyleClass().add("estado-" + usuario.getEstado().getValorBD().toLowerCase());
        lblDetUltimoAcceso.setText(Formatos.fechaHoraCorta(usuario.getUltimoAcceso()));
        lblDetIntentos.setText(String.valueOf(usuario.getIntentosFallidos()));

        // Una línea por permiso
        listaDetPermisos.getChildren().clear();
        if (usuario.getPermisos().isEmpty()) {
            Label sinPermisos = new Label("Sin permisos asignados");
            sinPermisos.getStyleClass().add("detalle-sin-permisos");
            listaDetPermisos.getChildren().add(sinPermisos);
        }
        for (Permiso permiso : usuario.getPermisos()) {
            FontIcon palomita = new FontIcon("bi-check2-circle");
            palomita.getStyleClass().add("funcion-icono");
            Label linea = new Label(permiso.getNombrePermiso(), palomita);
            linea.getStyleClass().add("funcion-item");
            linea.setGraphicTextGap(8);
            listaDetPermisos.getChildren().add(linea);
        }

        // Una cuenta inactiva ya no se puede dar de baja
        btnDarDeBaja.setDisable(usuario.getEstado() == EstadoUsuario.INACTIVO);
    }

    // ===================== Registro y modificación (RNF-11, RNF-14) =====================

    // Botón "+ Nuevo usuario"
    @FXML
    // RNF-11 (Registro de usuarios): inicia aquí. Paso 1: «+ Nuevo usuario».
    private void onNuevo() {
        abrirFormulario(null);
    }

    // Botón "Modificar" o doble clic en la tabla
    @FXML
    // RNF-14 (Modificación de usuarios): inicia aquí. Paso 1: selecciona al usuario y pide modificarlo.
    private void onEditar() {
        if (usuarioSeleccionado == null) {
            Alertas.info("Selecciona un usuario de la lista");
            return;
        }

        // Paso 2 de RNF-14: se valida que siga existiendo y se leen sus datos actuales
        Usuario usuario;
        try {
            usuario = servicio.buscarPorId(usuarioSeleccionado.getIdUsuario());
        } catch (DAOException e) {
            // RNF-14 · Excepción paso 2: «Error de conexión»; no se abre el formulario.
            Alertas.error(UsuarioService.MSG_ERROR_CONEXION);
            return;
        }
        // RNF-14 · Excepción paso 2: «Usuario no encontrado».
        if (usuario == null) {
            Alertas.aviso(UsuarioService.MSG_NO_ENCONTRADO);
            cargarUsuarios();
            return;
        }
        abrirFormulario(usuario);
    }

    // Abre la ventana del formulario; usuario null = registro nuevo
    private void abrirFormulario(Usuario usuario) {
        List<Permiso> permisos;
        try {
            permisos = servicio.listarPermisos();
        } catch (DAOException e) {
            Alertas.error(UsuarioService.MSG_ERROR_CONEXION);
            return;
        }

        FXMLLoader cargador = new FXMLLoader(UsuariosController.class.getResource(RUTA_FORMULARIO));
        Parent raiz;
        try {
            raiz = cargador.load();
        } catch (IOException e) {
            Alertas.error("No se pudo abrir el formulario de usuario");
            return;
        }

        FormularioUsuarioController formulario = cargador.getController();
        String titulo;
        if (usuario == null) {
            formulario.prepararNuevo(permisos);
            titulo = "Nuevo usuario";
        } else {
            formulario.prepararEdicion(usuario, permisos);
            titulo = "Modificar usuario";
        }

        // Ventana modal: mientras esté abierta no se puede usar el panel
        ApoyoVista.mostrarModal(raiz, tblUsuarios.getScene().getWindow(), titulo);

        // RNF-11 · Paso 7 / RNF-14 · Paso 8: informa el éxito y actualiza la lista.
        if (formulario.seGuardo()) {
            Alertas.info(formulario.getMensajeExito());
            cargarUsuarios();
        } else if (formulario.debeRecargarLista()) {
            cargarUsuarios();
        }
    }

    // ===================== Baja (RNF-12) =====================

    // Botón "Dar de baja"
    @FXML
    // RNF-12 (Eliminación de usuarios): inicia aquí. Paso 1: selecciona al usuario y pide darlo de baja.
    private void onDarDeBaja() {
        if (usuarioSeleccionado == null) {
            Alertas.info("Selecciona un usuario de la lista");
            return;
        }

        // Paso 2: revisar que exista, que esté activo y que no sea la propia cuenta
        ResultadoOperacion revision = servicio.prepararBaja(usuarioSeleccionado.getIdUsuario());
        // RNF-12 · Excepciones paso 2: sin conexión, no existe, ya inactiva o es la propia cuenta.
        if (!revision.isExitoso()) {
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                cargarUsuarios();
            }
            return;
        }

        // Pasos 3 y 4: confirmación doble con los datos de la persona
        Usuario usuario = revision.getUsuario();
        String mensaje = "Nombre: " + usuario.getNombreCompleto()
                + "\nUsuario: " + usuario.getNombreUsuario()
                + "\nPermisos: " + usuario.getTextoPermisos()
                + "\n\nYa no podrá entrar al sistema. Su historial de pedidos y movimientos se conserva.";
        // RNF-12 · Excepción paso 4: canceló; el usuario conserva su acceso y permisos.
        if (!Alertas.confirmarDoble("¿Dar de baja a este usuario?", mensaje)) {
            return;
        }

        // Paso 5: baja lógica
        ResultadoOperacion resultado = servicio.darDeBaja(usuario);
        // RNF-12 · Paso 6: informa el éxito y actualiza la lista. Si falla, excepción paso 5.
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        cargarUsuarios();
    }

    // ===================== Apoyo =====================

    // Muestra u oculta un elemento sin dejar el hueco
    private void mostrarNodo(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}

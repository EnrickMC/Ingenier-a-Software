package com.pac.modulos.pedidos.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.service.ConfiguracionPostre;
import com.pac.modulos.catalogo.service.ProductoService;
import com.pac.modulos.pedidos.model.Cliente;
import com.pac.modulos.pedidos.model.Direccion;
import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.modulos.pedidos.model.ModalidadEntrega;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.ApoyoTransaccion;
import com.pac.modulos.pedidos.service.ModificacionPedidoService;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.ResultadoPedido;
import com.pac.util.Alertas;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario de Modificación de pedidos (solo la Administradora).
 * Primero revisa los cambios con ModificacionPedidoService.revisarCambios (muestra "antes / después"
 * con la confirmación doble) y después los guarda con modificar.
 */
public class FormularioModificacionController {

    private static final String RUTA_PERSONALIZACION = "/com/pac/view/pedidos/formulario-personalizacion.fxml";

    @FXML private Label lblTitulo;
    @FXML private Label lblTotal;
    @FXML private Label lblDiferencia;
    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private ComboBox<ModalidadEntrega> cmbModalidad;
    @FXML private Label lblCarga;
    @FXML private VBox cajaDireccion;
    @FXML private TextField txtCalle;
    @FXML private TextField txtNumExt;
    @FXML private TextField txtNumInt;
    @FXML private TextField txtCodigoPostal;
    @FXML private TextField txtColonia;
    @FXML private TextField txtReferencias;
    @FXML private ComboBox<Categoria> cmbCategoriaNueva;
    @FXML private VBox listaLineas;
    @FXML private Label lblMensaje;

    private ModificacionPedidoService servicio = new ModificacionPedidoService();
    private PedidoService pedidoService = new PedidoService();
    private ProductoService productoService = new ProductoService();
    private Pedido original;
    private List<EditorLineaPedido> editores = new ArrayList<EditorLineaPedido>();
    private ResultadoPedido resultado;

    @FXML
    private void initialize() {
        cmbModalidad.getItems().setAll(ModalidadEntrega.values());
        // Al cambiar la fecha se muestra cuántos pedidos hay ese día
        // (Flecha necesaria: JavaFX avisa con un "listener" cuando cambia el valor)
        dpFecha.valueProperty().addListener((observable, antes, ahora) -> mostrarCarga());
    }

    // RF-34 · Paso 3: muestra el formulario con los datos actuales.
    public void preparar(Pedido pedido) {
        original = pedido;
        lblTitulo.setText("Modificar pedido #" + pedido.getFolio() + " · " + pedido.getTextoEstado());

        Cliente cliente = pedido.getCliente();
        txtNombre.setText(ApoyoVista.texto(cliente.getNombre()));
        txtTelefono.setText(ApoyoVista.texto(cliente.getTelefono()));
        txtCorreo.setText(ApoyoVista.texto(cliente.getCorreo()));

        dpFecha.setValue(pedido.getFechaEntrega());
        if (pedido.getHorarioEntrega() != null) {
            txtHora.setText(pedido.getHorarioEntrega().toString());
        }
        cmbModalidad.setValue(pedido.getModalidadEntrega());
        Direccion direccion = pedido.getDireccion();
        if (direccion != null) {
            txtCalle.setText(ApoyoVista.texto(direccion.getCalle()));
            txtNumExt.setText(ApoyoVista.texto(direccion.getNumeroExterior()));
            txtNumInt.setText(ApoyoVista.texto(direccion.getNumeroInterior()));
            txtCodigoPostal.setText(ApoyoVista.texto(direccion.getCodigoPostal()));
            txtColonia.setText(ApoyoVista.texto(direccion.getColonia()));
            txtReferencias.setText(ApoyoVista.texto(direccion.getReferencias()));
        }
        onCambioModalidad();

        // Copia de cada postre para editarla sin tocar el original
        listaLineas.getChildren().clear();
        editores.clear();
        for (LineaPedido linea : pedido.getLineas()) {
            agregarEditor(linea.copiar());
        }

        try {
            cmbCategoriaNueva.getItems().setAll(productoService.listarCategoriasActivas());
        } catch (DAOException e) {
            cmbCategoriaNueva.setPromptText(ApoyoTransaccion.MSG_ERROR_CONEXION);
        }
        recalcularTotal();
        mostrarCarga();
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    // null si se cerró sin guardar
    public ResultadoPedido getResultado() {
        return resultado;
    }

    @FXML
    private void onCambioModalidad() {
        ApoyoVista.mostrarNodo(cajaDireccion, cmbModalidad.getValue() == ModalidadEntrega.DOMICILIO);
        recalcularTotal();
    }

    private void mostrarCarga() {
        if (original == null || dpFecha.getValue() == null) {
            lblCarga.setText("");
            return;
        }
        try {
            int carga = pedidoService.cargaDelDia(dpFecha.getValue(), original.getFolio());
            String texto = "Ese día hay " + carga + " pedido(s) más por entregar (capacidad: "
                    + ConfiguracionApp.getInstancia().getCapacidadDiaria() + ").";
            if (carga >= ConfiguracionApp.getInstancia().getCapacidadDiaria()) {
                texto = texto + " " + PedidoService.MSG_AGENDA_SATURADA + ".";
            }
            lblCarga.setText(texto);
        } catch (DAOException e) {
            lblCarga.setText("");
        }
    }

    // ===================== Postres =====================

    private void agregarEditor(LineaPedido linea) {
        EditorLineaPedido editor = new EditorLineaPedido(linea, this);
        editores.add(editor);
        listaLineas.getChildren().add(editor.getNodo());
    }

    // Abre la personalización de un postre del pedido
    // RF-34 · Paso 4: cambiar la personalización de un postre.
    public void personalizar(EditorLineaPedido editor) {
        LineaPedido linea = editor.getLinea();
        List<OpcionElegida> elegidas = elegirOpciones(linea.getIdCategoria(), linea.getNombreCategoria(),
                linea.getOpciones());
        if (elegidas == null) {
            return;
        }
        aplicarPersonalizacion(linea, elegidas);
        editor.actualizar();
        recalcularTotal();
    }

    public void quitar(EditorLineaPedido editor) {
        if (editores.size() == 1) {
            Alertas.aviso(ModificacionPedidoService.MSG_SIN_POSTRES);
            return;
        }
        if (!Alertas.confirmar("¿Quitar este postre?", editor.getLinea().getNombreMostrar())) {
            return;
        }
        editores.remove(editor);
        listaLineas.getChildren().remove(editor.getNodo());
        recalcularTotal();
    }

    @FXML
    private void onAgregarPostre() {
        Categoria categoria = cmbCategoriaNueva.getValue();
        if (categoria == null) {
            Alertas.info("Elige primero la categoría del postre");
            return;
        }
        List<OpcionElegida> elegidas = elegirOpciones(categoria.getIdCategoria(), categoria.getNombre(), null);
        if (elegidas == null) {
            return;
        }
        LineaPedido nueva = new LineaPedido();
        nueva.setFolio(original.getFolio());
        nueva.setIdCategoria(categoria.getIdCategoria());
        nueva.setNombreCategoria(categoria.getNombre());
        nueva.setCantidad(1);
        aplicarPersonalizacion(nueva, elegidas);
        agregarEditor(nueva);
        recalcularTotal();
    }

    // Abre la ventana de personalización; devuelve las opciones o null si se canceló
    private List<OpcionElegida> elegirOpciones(int idCategoria, String nombreCategoria, List<OpcionElegida> actuales) {
        ConfiguracionPostre configuracion;
        try {
            configuracion = servicio.cargarConfiguracion(idCategoria);
        } catch (DAOException e) {
            Alertas.error(ApoyoTransaccion.MSG_ERROR_CONEXION);
            return null;
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_PERSONALIZACION);
        if (cargador == null) {
            Alertas.error("No se pudo abrir la personalización");
            return null;
        }
        FormularioPersonalizacionController ventana = cargador.getController();
        ventana.preparar(configuracion, nombreCategoria, actuales);
        ApoyoVista.mostrarModal(cargador.getRoot(), lblTitulo.getScene().getWindow(), "Personalizar postre");
        return ventana.getElegidas();
    }

    // Pone las opciones nuevas en la línea con su precio y su descripción
    private void aplicarPersonalizacion(LineaPedido linea, List<OpcionElegida> elegidas) {
        linea.setOpciones(elegidas);
        linea.setPersonalizacionCambiada(true);
        linea.setPrecioUnitario(ConfiguracionPostre.calcularPrecio(elegidas));
        linea.setCostoUnitario(ConfiguracionPostre.calcularCosto(elegidas));
        linea.setNombrePostre(null);
        linea.setLinkFotografia(null);
        List<String> textos = new ArrayList<String>();
        for (OpcionElegida elegida : elegidas) {
            String texto = elegida.getOpcion().getNombre();
            if (elegida.getCantidad() > 1) {
                texto = texto + " ×" + elegida.getCantidad();
            }
            textos.add(texto);
        }
        linea.setEspecificaciones(textos);
        linea.setImagenes(new ArrayList<String>());
    }

    // Total en vivo y diferencia con el original
    // RF-34 · Paso 7: recalcula el total.
    public void recalcularTotal() {
        double total = 0;
        for (EditorLineaPedido editor : editores) {
            total = total + editor.getLinea().getSubtotal();
        }
        // Mismo cálculo que ModificacionPedidoService: el envío web se conserva si sigue a domicilio
        if (cmbModalidad.getValue() == ModalidadEntrega.DOMICILIO) {
            total = total + original.getCostoEnvio();
        }
        total = PedidoService.redondear(total);
        lblTotal.setText(Formatos.moneda(total));
        double diferencia = PedidoService.redondear(total - original.getMontoTotal());
        if (Math.abs(diferencia) < 0.005) {
            lblDiferencia.setText("Sin cambio en el total");
        } else if (diferencia > 0) {
            lblDiferencia.setText("+" + Formatos.moneda(diferencia) + " vs. actual");
        } else {
            lblDiferencia.setText("−" + Formatos.moneda(-diferencia) + " vs. actual");
        }
    }

    // ===================== Guardar =====================

    @FXML
    // RF-34 · Paso 4: la administradora elige «Guardar».
    private void onGuardar() {
        quitarMarcas();
        Pedido cambios = leerCambios();
        if (cambios == null) {
            return;
        }

        // 1. Se revisan los cambios y se muestra "antes / después"
        ResultadoPedido revision = servicio.revisarCambios(original, cambios);
        // RF-34 · Excepciones pasos 5 y 6: datos inválidos; se resalta el campo.
        if (!revision.isExitoso()) {
            mostrarError(revision.getMensaje(), campoDe(revision.getCampo()));
            return;
        }
        // RF-34 · Paso 8: confirma los cambios con la diferencia del total.
        if (!Alertas.confirmarDoble("¿Guardar los cambios del pedido #" + original.getFolio() + "?",
                revision.getMensajeCompleto())) {
            return;
        }

        // 2. Se guardan en una sola transacción
        ResultadoPedido respuesta = servicio.modificar(original, cambios);
        // RF-34 · Excepción paso 9: se muestra el error y no se guarda.
        if (!respuesta.isExitoso() && !respuesta.isRecargar()) {
            mostrarError(respuesta.getMensaje(), campoDe(respuesta.getCampo()));
            return;
        }
        resultado = respuesta;
        cerrar();
    }

    // Arma el pedido con lo que hay en pantalla; null si la hora no tiene formato válido
    private Pedido leerCambios() {
        Pedido cambios = new Pedido();
        cambios.setFolio(original.getFolio());
        Cliente cliente = new Cliente();
        cliente.setIdCliente(original.getCliente().getIdCliente());
        cliente.setNombre(limpiar(txtNombre.getText()));
        cliente.setTelefono(limpiar(txtTelefono.getText()));
        cliente.setCorreo(limpiar(txtCorreo.getText()));
        cambios.setCliente(cliente);

        cambios.setFechaEntrega(dpFecha.getValue());
        String hora = limpiar(txtHora.getText());
        if (hora != null) {
            try {
                cambios.setHorarioEntrega(LocalTime.parse(hora));
            } catch (DateTimeParseException e) {
                mostrarError("Indica la hora de entrega con el formato HH:mm (por ejemplo 14:30)", txtHora);
                return null;
            }
        }
        cambios.setModalidadEntrega(cmbModalidad.getValue());
        if (cmbModalidad.getValue() == ModalidadEntrega.DOMICILIO) {
            Direccion direccion = new Direccion();
            direccion.setCalle(limpiar(txtCalle.getText()));
            direccion.setNumeroExterior(limpiar(txtNumExt.getText()));
            direccion.setNumeroInterior(limpiar(txtNumInt.getText()));
            direccion.setCodigoPostal(limpiar(txtCodigoPostal.getText()));
            direccion.setColonia(limpiar(txtColonia.getText()));
            direccion.setReferencias(limpiar(txtReferencias.getText()));
            cambios.setDireccion(direccion);
        }

        List<LineaPedido> lineas = new ArrayList<LineaPedido>();
        for (EditorLineaPedido editor : editores) {
            lineas.add(editor.leer());
        }
        cambios.setLineas(lineas);
        return cambios;
    }

    private Node campoDe(String campo) {
        if (ResultadoPedido.CAMPO_NOMBRE.equals(campo)) {
            return txtNombre;
        }
        if (ResultadoPedido.CAMPO_TELEFONO.equals(campo)) {
            return txtTelefono;
        }
        if (ResultadoPedido.CAMPO_CORREO.equals(campo)) {
            return txtCorreo;
        }
        if (ResultadoPedido.CAMPO_FECHA.equals(campo)) {
            return dpFecha;
        }
        if (ResultadoPedido.CAMPO_HORARIO.equals(campo)) {
            return txtHora;
        }
        if (ResultadoPedido.CAMPO_DIRECCION.equals(campo)) {
            return cajaDireccion;
        }
        if (ResultadoPedido.CAMPO_PRODUCTOS.equals(campo)) {
            return listaLineas;
        }
        return null;
    }

    @FXML
    // RF-34 · Excepción paso 4: «Cancelar» descarta los cambios.
    private void onCancelar() {
        cerrar();
    }

    private void mostrarError(String mensaje, Node campo) {
        lblMensaje.setText(mensaje);
        ApoyoVista.mostrarNodo(lblMensaje, true);
        if (campo != null) {
            ApoyoVista.marcar(campo);
        }
    }

    private void quitarMarcas() {
        Node[] campos = {txtNombre, txtTelefono, txtCorreo, dpFecha, txtHora, cajaDireccion, listaLineas};
        for (Node campo : campos) {
            ApoyoVista.desmarcar(campo);
        }
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    private String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private void cerrar() {
        Stage ventana = (Stage) lblTitulo.getScene().getWindow();
        ventana.close();
    }
}

package com.pac.modulos.pedidos.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.pedidos.model.EstadoPago;
import com.pac.modulos.pedidos.model.Pago;
import com.pac.modulos.pedidos.service.ApoyoTransaccion;
import com.pac.modulos.pedidos.service.MonitorPedidos;
import com.pac.modulos.pedidos.service.PagoService;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.ResultadoPedido;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.util.List;

/**
 * Controlador de "Validación de Pagos" (pagos.fxml). Módulo 4, RF-42 y RF-43.
 * Lista los pagos (por defecto los pendientes que subieron los clientes) para validarlos o rechazarlos.
 */
public class PagosController implements ControladorSeccion {

    private static final String RUTA_PAGO = "/com/pac/view/pedidos/formulario-pago.fxml";

    @FXML private TextField txtBuscar;
    @FXML private BarraEstados barraEstados;
    @FXML private Label lblConteo;
    @FXML private TableView<FilaPago> tblPagos;
    @FXML private Label lblVacio;
    @FXML private Button btnComprobante;
    @FXML private Button btnVerPedido;
    @FXML private Button btnValidar;

    private PagoService servicio = new PagoService();
    private PedidoService pedidoService = new PedidoService();
    private boolean llenandoFiltros = false;

    @FXML
    private void initialize() {
        // Cada renglón se pinta según el estado del pago
        // (JavaFX necesita la flecha "->" para indicar cómo crear cada renglón)
        tblPagos.setRowFactory(tabla -> new RenglonPago());
        // Al pulsar un botón de estado se vuelve a consultar
        barraEstados.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onBuscar();
            }
        });
    }

    // El Navegador llama a este método al abrir el apartado
    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        llenandoFiltros = true;
        // Botones de estado: cada uno filtra los pagos (se abre en "Pendiente")
        barraEstados.vaciar();
        barraEstados.agregar(EstadoPago.PENDIENTE.toString(), "Pendiente", "estado-aceptacion", "bi-hourglass-split");
        barraEstados.agregar(EstadoPago.VALIDADO.toString(), "Validado", "estado-terminado", "bi-check-circle-fill");
        barraEstados.agregar(EstadoPago.RECHAZADO.toString(), "Rechazado", "estado-cancelado", "bi-x-circle-fill");
        barraEstados.seleccionar(EstadoPago.PENDIENTE.toString());
        llenandoFiltros = false;
        cargarPagos();
    }

    @FXML
    private void onBuscar() {
        if (llenandoFiltros) {
            return;
        }
        cargarPagos();
    }

    private EstadoPago estadoElegido() {
        for (EstadoPago estado : EstadoPago.values()) {
            if (estado.toString().equals(barraEstados.getClave())) {
                return estado;
            }
        }
        return null;
    }

    // RF-42 (Validación de pagos): lista de pagos por validar.
    private void cargarPagos() {
        List<Pago> pagos;
        try {
            pagos = servicio.listar(estadoElegido(), txtBuscar.getText());
        } catch (DAOException e) {
            tblPagos.getItems().clear();
            // RF-42 · Excepción paso 2: sin conexión, «Error de conexión».
            lblVacio.setText(ApoyoTransaccion.MSG_ERROR_CONEXION);
            lblConteo.setText("");
            actualizarBotones();
            return;
        }
        tblPagos.getItems().clear();
        for (Pago pago : pagos) {
            tblPagos.getItems().add(new FilaPago(pago));
        }
        lblConteo.setText(pagos.size() + " pago(s)");
        if (pagos.isEmpty()) {
            if (estadoElegido() == EstadoPago.PENDIENTE) {
                lblVacio.setText("No hay pagos por validar");
            } else {
                lblVacio.setText("No se encontraron pagos con ese criterio");
            }
        }
        actualizarBotones();
    }

    @FXML
    private void onClicTabla(MouseEvent evento) {
        actualizarBotones();
        FilaPago fila = tblPagos.getSelectionModel().getSelectedItem();
        if (fila != null && evento.getClickCount() == 2) {
            if (fila.getPago().getEstado() == EstadoPago.PENDIENTE) {
                onValidar();
            } else {
                onVerPedido();
            }
        }
    }

    private void actualizarBotones() {
        FilaPago fila = tblPagos.getSelectionModel().getSelectedItem();
        btnVerPedido.setDisable(fila == null);
        btnComprobante.setDisable(fila == null || !fila.getPago().tieneComprobante());
        btnValidar.setDisable(fila == null || fila.getPago().getEstado() != EstadoPago.PENDIENTE);
    }

    // RF-50 · Paso 1: «Ver comprobante» del pago elegido (foto, PDF o enlace)
    @FXML
    private void onAbrirComprobante() {
        FilaPago fila = tblPagos.getSelectionModel().getSelectedItem();
        if (fila != null && fila.getPago().tieneComprobante()) {
            VisorComprobante.abrirVentana(fila.getPago(), tblPagos.getScene().getWindow());
        }
    }

    @FXML
    private void onVerPedido() {
        FilaPago fila = tblPagos.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }
        if (DetallePedidoController.abrirEnVentana(fila.getPago().getFolio(), tblPagos.getScene().getWindow())) {
            cargarPagos();
        }
    }

    // RF-42: validar o rechazar el pago pendiente elegido
    @FXML
    // RF-42: inicia aquí la validación de un pago pendiente desde la sección de pagos.
    private void onValidar() {
        FilaPago fila = tblPagos.getSelectionModel().getSelectedItem();
        if (fila == null || fila.getPago().getEstado() != EstadoPago.PENDIENTE) {
            Alertas.info("Selecciona un pago pendiente de validar");
            return;
        }
        ResultadoPedido lectura = pedidoService.cargarDetalle(fila.getPago().getFolio());
        if (!lectura.isExitoso()) {
            // RF-42 · Excepción paso 2: el pedido no permite pagos.
            Alertas.aviso(lectura.getMensaje());
            cargarPagos();
            return;
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_PAGO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir la validación del pago");
            return;
        }
        FormularioPagoController formulario = cargador.getController();
        formulario.prepararValidacion(fila.getPago(), lectura.getPedido());
        ApoyoVista.mostrarModal(cargador.getRoot(), tblPagos.getScene().getWindow(),
                "Validar pago · pedido #" + fila.getPago().getFolio());

        ResultadoPedido resultado = formulario.getResultado();
        if (resultado == null) {
            return;
        }
        if (resultado.isExitoso()) {
            // RF-42 · Paso 9: informa que el pago se validó y el saldo actualizado.
            Alertas.info(resultado.getMensajeCompleto());
        } else if (resultado.isRecargar()) {
            Alertas.aviso(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        MonitorPedidos.getInstancia().revisar();
        cargarPagos();
    }
}

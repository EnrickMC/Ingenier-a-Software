package com.pac.modulos.pedidos.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.pedidos.model.Cancelacion;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.ResultadoPedido;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Ventana para cancelar un pedido (RF-39) o dar de baja una solicitud (solo la Administradora).
 * Pide el motivo (con descripción obligatoria si es "Otro") y, al cancelar con pagos validados,
 * el monto y el estado del reembolso.
 */
public class FormularioMotivoController {

    @FXML private Label lblTitulo;
    @FXML private Label lblExplicacion;
    @FXML private Label lblResumen;
    @FXML private ComboBox<String> cmbMotivo;
    @FXML private Label lblDetalle;
    @FXML private TextArea txtDetalle;
    @FXML private VBox cajaReembolso;
    @FXML private Label lblPagado;
    @FXML private TextField txtReembolso;
    @FXML private ComboBox<String> cmbEstadoReembolso;
    @FXML private Label lblMensaje;
    @FXML private Button btnNo;
    @FXML private Button btnConfirmar;

    private PedidoService servicio = new PedidoService();
    private Pedido pedido;
    private boolean esCancelacion;
    private ResultadoPedido resultado;

    // RF-39 · Paso 3: pide el motivo e informa si el pedido tiene pagos validados.
    public void prepararCancelacion(Pedido pedido) {
        this.pedido = pedido;
        esCancelacion = true;
        lblTitulo.setText("Cancelar pedido #" + pedido.getFolio());
        lblExplicacion.setText("El pedido conserva su estado actual y queda marcado como cancelado. "
                + "Ya no admitirá cambios ni pagos.");
        cmbMotivo.getItems().setAll(PedidoService.MOTIVOS_CANCELACION);
        btnNo.setText("No cancelar");
        btnConfirmar.setText("Cancelar pedido");
        llenarResumen();

        // Reembolso: solo si hay pagos validados
        boolean conPagos = pedido.getTotalPagado() > 0;
        ApoyoVista.mostrarNodo(cajaReembolso, conPagos);
        if (conPagos) {
            lblPagado.setText("El cliente ya pagó " + Formatos.moneda(pedido.getTotalPagado())
                    + ". Indica cuánto se le devolverá.");
            txtReembolso.setText(ValidacionCatalogo.numero(pedido.getTotalPagado()));
            cmbEstadoReembolso.getItems().setAll(Cancelacion.REEMBOLSO_PENDIENTE, Cancelacion.REEMBOLSO_REALIZADO);
            cmbEstadoReembolso.setValue(Cancelacion.REEMBOLSO_PENDIENTE);
        }
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    // RF-35 · Paso 4: solicita el motivo de la baja.
    public void prepararBaja(Pedido pedido) {
        this.pedido = pedido;
        esCancelacion = false;
        lblTitulo.setText("Dar de baja la solicitud #" + pedido.getFolio());
        lblExplicacion.setText("Para solicitudes registradas por error (duplicadas, de prueba o con datos falsos). "
                + "El registro se conserva en el historial, pero sale de las listas de trabajo.");
        cmbMotivo.getItems().setAll(PedidoService.MOTIVOS_BAJA);
        btnNo.setText("No dar de baja");
        btnConfirmar.setText("Dar de baja");
        llenarResumen();
        ApoyoVista.mostrarNodo(cajaReembolso, false);
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    private void llenarResumen() {
        lblResumen.setText("Cliente: " + pedido.getCliente().getNombre()
                + "\nEstado: " + pedido.getTextoEstado()
                + "\nTotal: " + Formatos.moneda(pedido.getMontoTotal())
                + " · Entrega: " + Formatos.fecha(pedido.getFechaEntrega()));
    }

    // null si se cerró sin confirmar
    public ResultadoPedido getResultado() {
        return resultado;
    }

    // Con "Otro" la descripción es obligatoria
    @FXML
    private void onCambioMotivo() {
        if (PedidoService.MOTIVO_OTRO.equals(cmbMotivo.getValue())) {
            lblDetalle.setText("Descripción *");
            txtDetalle.setPromptText("Escribe el motivo");
        } else {
            lblDetalle.setText("Descripción");
            txtDetalle.setPromptText("Opcional. Detalles del motivo");
        }
    }

    @FXML
    // RF-39 paso 4 / RF-35 (Eliminación de pedidos) paso 5: la administradora confirma.
    private void onConfirmar() {
        quitarMarcas();
        double reembolso = 0;
        if (esCancelacion && cajaReembolso.isVisible()) {
            if (!ValidacionCatalogo.esDecimal(txtReembolso.getText())) {
                mostrarError(PedidoService.MSG_REEMBOLSO_INVALIDO, txtReembolso);
                return;
            }
            reembolso = ValidacionCatalogo.aDecimal(txtReembolso.getText());
        }
        // Primero se revisa el motivo para no pedir la confirmación doble en vano
        String motivo = cmbMotivo.getValue();
        boolean faltaMotivo = motivo == null;
        boolean faltaDetalle = PedidoService.MOTIVO_OTRO.equals(motivo) && txtDetalle.getText().isBlank();
        if (faltaMotivo || faltaDetalle) {
            // RF-39 · Excepción paso 5 / RF-35 · Excepción paso 6: falta el motivo.
            String mensaje = PedidoService.MSG_INDICA_MOTIVO_BAJA;
            if (esCancelacion) {
                mensaje = PedidoService.MSG_INDICA_MOTIVO_CANCELACION;
            }
            if (faltaMotivo) {
                mostrarError(mensaje, cmbMotivo);
            } else {
                mostrarError(mensaje, txtDetalle);
            }
            return;
        }

        String pregunta = "¿Dar de baja la solicitud #" + pedido.getFolio() + "?";
        String detalle = "La solicitud dejará de aparecer en las listas de trabajo y se avisará al cliente.";
        if (esCancelacion) {
            pregunta = "¿Cancelar el pedido #" + pedido.getFolio() + "?";
            detalle = "El pedido ya no admitirá cambios y se avisará al cliente.";
        }
        // Confirmación doble; si no confirma, el pedido queda sin cambios.
        if (!Alertas.confirmarDoble(pregunta, detalle)) {
            return;
        }

        ResultadoPedido respuesta;
        if (esCancelacion) {
            respuesta = servicio.cancelar(pedido, motivo, txtDetalle.getText(), reembolso, cmbEstadoReembolso.getValue());
        } else {
            respuesta = servicio.darDeBaja(pedido, motivo, txtDetalle.getText());
        }
        if (!respuesta.isExitoso() && !respuesta.isRecargar()) {
            if (ResultadoPedido.CAMPO_REEMBOLSO.equals(respuesta.getCampo())) {
                mostrarError(respuesta.getMensaje(), txtReembolso);
            } else {
                mostrarError(respuesta.getMensaje(), null);
            }
            return;
        }
        resultado = respuesta;
        cerrar();
    }

    @FXML
    // RF-39 · Excepción paso 4 / RF-35 · Excepción paso 5: «No cancelar» o «No dar de baja».
    private void onNo() {
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
        ApoyoVista.desmarcar(cmbMotivo);
        ApoyoVista.desmarcar(txtDetalle);
        ApoyoVista.desmarcar(txtReembolso);
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    private void cerrar() {
        Stage ventana = (Stage) lblTitulo.getScene().getWindow();
        ventana.close();
    }
}

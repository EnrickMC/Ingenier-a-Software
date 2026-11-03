package com.pac.modulos.pedidos.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.pedidos.model.MetodoPago;
import com.pac.modulos.pedidos.model.Pago;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.PagoService;
import com.pac.modulos.pedidos.service.ResultadoPedido;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Ventana de pagos (RF-28, RF-29).
 * - Registrar: pago en sucursal o recibido por otro medio; queda «Validado».
 * - Validar: revisa un pago «Pendiente» que subió el cliente; se puede validar (corrigiendo
 *   monto, método o referencia) o rechazar con motivo.
 */
public class FormularioPagoController {

    @FXML private Label lblTitulo;
    @FXML private Label lblExplicacion;
    @FXML private Label lblDesglose;
    @FXML private Label lblSituacion;
    @FXML private TextField txtMonto;
    @FXML private ComboBox<MetodoPago> cmbMetodo;
    @FXML private Label lblReferencia;
    @FXML private TextField txtReferencia;
    @FXML private VBox cajaEnlace;
    @FXML private TextField txtComprobante;
    @FXML private VBox cajaRevision;
    @FXML private Label lblComprobante;
    @FXML private Button btnAbrirComprobante;
    @FXML private TextField txtMotivoRechazo;
    @FXML private Label lblMensaje;
    @FXML private Button btnRechazar;
    @FXML private Button btnGuardar;

    private PagoService servicio = new PagoService();
    private Pedido pedido;
    private Pago pendiente;          // null al registrar un pago nuevo
    private ResultadoPedido resultado;

    @FXML
    private void initialize() {
        cmbMetodo.getItems().setAll(MetodoPago.values());
    }

    // RF-28 · Paso 3: muestra total, anticipo, lo pagado, el saldo y los pagos anteriores (pago en sucursal).
    public void prepararRegistro(Pedido pedido) {
        this.pedido = pedido;
        lblTitulo.setText("Registrar pago · pedido #" + pedido.getFolio());
        lblExplicacion.setText("Para pagos recibidos en sucursal o revisados por otro medio. Quedará validado con tu usuario.");
        llenarDesglose();
        // Monto sugerido: lo que falta para el anticipo o, si ya está cubierto, el saldo
        double sugerido = pedido.getFaltaParaIniciar();
        if (sugerido <= 0) {
            sugerido = pedido.getSaldo();
        }
        txtMonto.setText(ValidacionCatalogo.numero(sugerido));
        cmbMetodo.setValue(MetodoPago.EFECTIVO);
        onCambioMetodo();
        btnGuardar.setText("Registrar pago");
        ApoyoVista.mostrarNodo(cajaRevision, false);
        ApoyoVista.mostrarNodo(btnRechazar, false);
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    // RF-28 · Paso 3: lo mismo para validar un pago que subió el cliente con su comprobante.
    public void prepararValidacion(Pago pago, Pedido pedido) {
        this.pedido = pedido;
        this.pendiente = pago;
        lblTitulo.setText("Validar pago · pedido #" + pedido.getFolio());
        lblExplicacion.setText("Pago que subió el cliente el " + Formatos.fechaHoraCorta(pago.getFechaPago())
                + ". Revisa el comprobante y corrige los datos si hace falta.");
        llenarDesglose();
        txtMonto.setText(ValidacionCatalogo.numero(pago.getMonto()));
        cmbMetodo.setValue(pago.getMetodo());
        txtReferencia.setText(ApoyoVista.texto(pago.getReferencia()));
        onCambioMetodo();
        ApoyoVista.mostrarNodo(cajaEnlace, false);
        if (pago.tieneComprobante()) {
            lblComprobante.setText("Comprobante: " + pago.getLinkComprobante());
        } else {
            lblComprobante.setText("El cliente no subió el enlace del comprobante.");
        }
        ApoyoVista.mostrarNodo(btnAbrirComprobante, pago.tieneComprobante());
        btnGuardar.setText("Validar pago");
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    private void llenarDesglose() {
        lblDesglose.setText("Total: " + Formatos.moneda(pedido.getMontoTotal())
                + "\nAnticipo requerido: " + Formatos.moneda(pedido.getMontoParaIniciar())
                + "\nPagado (validado): " + Formatos.moneda(pedido.getTotalPagado())
                + "\nSaldo pendiente: " + Formatos.moneda(pedido.getSaldo()));
        lblSituacion.setText(PagoService.situacion(pedido));
    }

    // null si se cerró sin guardar
    public ResultadoPedido getResultado() {
        return resultado;
    }

    // Transferencia y depósito piden referencia; en efectivo es opcional
    @FXML
    private void onCambioMetodo() {
        if (cmbMetodo.getValue() == MetodoPago.EFECTIVO) {
            lblReferencia.setText("Referencia (opcional)");
        } else {
            lblReferencia.setText("Referencia *");
        }
    }

    @FXML
    private void onAbrirComprobante() {
        if (pendiente != null) {
            DetallePedidoController.abrirComprobante(pendiente.getLinkComprobante());
        }
    }

    @FXML
    // RF-28 · Paso 4: la administradora captura el pago y elige validarlo.
    private void onGuardar() {
        quitarMarcas();
        if (!ValidacionCatalogo.esDecimal(txtMonto.getText())) {
            // RF-28 · Excepción paso 5: el monto debe ser mayor a $0.00.
            mostrarError(PagoService.MSG_MONTO_INVALIDO, txtMonto);
            return;
        }
        double monto = ValidacionCatalogo.aDecimal(txtMonto.getText());
        ResultadoPedido respuesta;
        if (pendiente == null) {
            respuesta = servicio.registrar(pedido.getFolio(), monto, cmbMetodo.getValue(), txtReferencia.getText(),
                    txtComprobante.getText());
        } else {
            if (!Alertas.confirmar("¿Validar el pago?", "Monto: " + Formatos.moneda(monto) + "\nMétodo: "
                    + cmbMetodo.getValue())) {
                return;
            }
            respuesta = servicio.validar(pendiente, monto, cmbMetodo.getValue(), txtReferencia.getText());
        }
        terminar(respuesta);
    }

    @FXML
    // RF-28 · Excepción paso 4: el comprobante no es válido; se rechaza el pago con su motivo.
    private void onRechazar() {
        quitarMarcas();
        if (txtMotivoRechazo.getText().isBlank()) {
            mostrarError(PagoService.MSG_INDICA_MOTIVO_RECHAZO, txtMotivoRechazo);
            return;
        }
        if (!Alertas.confirmar("¿Rechazar el pago?", "No cambiará el saldo ni el estado del pedido. "
                + "Se avisará al cliente con el motivo.")) {
            return;
        }
        terminar(servicio.rechazar(pendiente, txtMotivoRechazo.getText()));
    }

    // Si hubo error de un campo se queda abierta; si no, se cierra con el resultado
    private void terminar(ResultadoPedido respuesta) {
        // RF-28 · Excepciones pasos 5 y 6: se resalta el campo con error y no se registra.
        if (!respuesta.isExitoso() && !respuesta.isRecargar() && !respuesta.isOfrecerRevision()) {
            mostrarError(respuesta.getMensaje(), campoDe(respuesta.getCampo()));
            return;
        }
        resultado = respuesta;
        cerrar();
    }

    private Node campoDe(String campo) {
        if (ResultadoPedido.CAMPO_MONTO.equals(campo)) {
            return txtMonto;
        }
        if (ResultadoPedido.CAMPO_METODO.equals(campo)) {
            return cmbMetodo;
        }
        if (ResultadoPedido.CAMPO_REFERENCIA.equals(campo)) {
            return txtReferencia;
        }
        if (ResultadoPedido.CAMPO_COMPROBANTE.equals(campo)) {
            return txtComprobante;
        }
        if (ResultadoPedido.CAMPO_MOTIVO.equals(campo)) {
            return txtMotivoRechazo;
        }
        return null;
    }

    @FXML
    // RF-28 · Excepción paso 4: la administradora canceló la captura; nada cambia.
    private void onCerrar() {
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
        ApoyoVista.desmarcar(txtMonto);
        ApoyoVista.desmarcar(cmbMetodo);
        ApoyoVista.desmarcar(txtReferencia);
        ApoyoVista.desmarcar(txtComprobante);
        ApoyoVista.desmarcar(txtMotivoRechazo);
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    private void cerrar() {
        Stage ventana = (Stage) lblTitulo.getScene().getWindow();
        ventana.close();
    }
}

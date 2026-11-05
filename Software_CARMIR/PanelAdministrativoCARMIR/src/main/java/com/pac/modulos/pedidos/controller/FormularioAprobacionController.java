package com.pac.modulos.pedidos.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.pedidos.model.ModalidadPago;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.ResultadoPedido;
import com.pac.util.Alertas;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Ventana de aprobación (RF-38): muestra folio, cliente, total, fecha de entrega y la carga del día,
 * y deja ajustar el anticipo requerido antes de aprobar.
 */
public class FormularioAprobacionController {

    @FXML private Label lblResumen;
    @FXML private Label lblCarga;
    @FXML private Label lblModalidad;
    @FXML private TextField txtAnticipo;
    @FXML private Label lblAyudaAnticipo;
    @FXML private Label lblMensaje;

    private PedidoService servicio = new PedidoService();
    private Pedido pedido;
    private ResultadoPedido resultado;

    // RF-38 · Paso 4: muestra la confirmación con folio, cliente, total, fecha y anticipo requerido.
    public void preparar(Pedido pedido, int cargaDelDia) {
        this.pedido = pedido;
        String hora = "";
        if (pedido.getHorarioEntrega() != null) {
            hora = " a las " + pedido.getHorarioEntrega().toString();
        }
        lblResumen.setText("Folio: #" + pedido.getFolio()
                + "\nCliente: " + pedido.getCliente().getNombre() + " · " + pedido.getCliente().getTelefono()
                + "\nTotal: " + Formatos.moneda(pedido.getMontoTotal())
                + "\nEntrega: " + Formatos.fecha(pedido.getFechaEntrega()) + hora + " · " + pedido.getModalidadEntrega());

        // Carga del día de entrega (RF-33)
        int capacidad = ConfiguracionApp.getInstancia().getCapacidadDiaria();
        String carga = "Ese día hay " + cargaDelDia + " pedido(s) más por entregar (capacidad: " + capacidad + ").";
        if (cargaDelDia >= capacidad) {
            // RF-33 · Excepción paso 5: «Agenda saturada para esa fecha» (no bloquea la aprobación).
            carga = carga + "\n" + PedidoService.MSG_AGENDA_SATURADA + ": puedes aprobarlo, pero revisa la producción.";
            lblCarga.getStyleClass().setAll("aviso-amarillo");
        }
        lblCarga.setText(carga);

        lblModalidad.setText("Modalidad de pago: " + pedido.getModalidadPago());
        txtAnticipo.setText(ValidacionCatalogo.numero(pedido.getAnticipoRequerido()));
        if (pedido.getModalidadPago() == ModalidadPago.PAGO_TOTAL) {
            txtAnticipo.setText(ValidacionCatalogo.numero(pedido.getMontoTotal()));
            txtAnticipo.setDisable(true);
            lblAyudaAnticipo.setText("Con «Pago total» se debe cubrir el total antes de iniciar la preparación.");
        } else {
            lblAyudaAnticipo.setText("Monto mínimo que el cliente debe pagar para que el pedido pase a «Pagado».");
        }
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    // null si se cerró sin aprobar
    public ResultadoPedido getResultado() {
        return resultado;
    }

    @FXML
    // RF-38 · Paso 5: la administradora confirma la aprobación.
    private void onAprobar() {
        ApoyoVista.desmarcar(txtAnticipo);
        String texto = txtAnticipo.getText();
        if (!ValidacionCatalogo.esDecimal(texto)) {
            mostrarError(PedidoService.MSG_ANTICIPO_INVALIDO);
            ApoyoVista.marcar(txtAnticipo);
            return;
        }
        double anticipo = ValidacionCatalogo.aDecimal(texto);
        if (anticipo < 0 || anticipo > pedido.getMontoTotal()) {
            mostrarError(PedidoService.MSG_ANTICIPO_INVALIDO);
            ApoyoVista.marcar(txtAnticipo);
            return;
        }

        // Confirmación doble
        // RF-38 · Excepción paso 5: si no confirma, el pedido sigue «En proceso de aceptación».
        if (!Alertas.confirmarDoble("¿Aprobar el pedido #" + pedido.getFolio() + "?",
                "Total: " + Formatos.moneda(pedido.getMontoTotal()) + "\nAnticipo requerido: " + Formatos.moneda(anticipo)
                        + "\nSe avisará al cliente por WhatsApp y correo.")) {
            return;
        }
        ResultadoPedido respuesta = servicio.aprobar(pedido, anticipo);
        if (!respuesta.isExitoso() && !respuesta.isRecargar() && ResultadoPedido.CAMPO_ANTICIPO.equals(respuesta.getCampo())) {
            mostrarError(respuesta.getMensaje());
            ApoyoVista.marcar(txtAnticipo);
            return;
        }
        // RF-38 · Excepciones pasos 2, 3 y 6: se muestra el motivo y no se aprueba.
        if (!respuesta.isExitoso() && !respuesta.isRecargar()) {
            mostrarError(respuesta.getMensaje());
            return;
        }
        resultado = respuesta;
        cerrar();
    }

    @FXML
    // RF-38 · Excepción paso 5: «Cancelar» cierra la ventana sin cambios.
    private void onCancelar() {
        cerrar();
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        ApoyoVista.mostrarNodo(lblMensaje, true);
    }

    private void cerrar() {
        Stage ventana = (Stage) txtAnticipo.getScene().getWindow();
        ventana.close();
    }
}

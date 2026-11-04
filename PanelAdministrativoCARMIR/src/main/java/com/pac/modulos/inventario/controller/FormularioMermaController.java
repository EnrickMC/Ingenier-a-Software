package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.service.InsumoService;
import com.pac.modulos.inventario.service.InventarioService;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana "Registrar merma" (RF-60) (formulario-merma.fxml).
 * Se puede descontar de un lote en particular o dejar que el sistema use primero el que caduca antes.
 */
public class FormularioMermaController {

    @FXML private ComboBox<Insumo> cmbInsumo;
    @FXML private Label lblExistencia;
    @FXML private ComboBox<Lote> cmbLote;
    @FXML private Label lblCantidad;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cmbMotivo;
    @FXML private Label lblDetalle;
    @FXML private TextField txtDetalle;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private InventarioService servicio = new InventarioService();
    private Lote automatico;             // opción "por PEPS" del selector de lotes (id 0)
    private boolean guardado = false;
    private boolean recargarLista = false;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblMensaje, false);
        automatico = new Lote();
        for (String motivo : InventarioService.MOTIVOS_MERMA) {
            cmbMotivo.getItems().add(motivo);
        }
        lblExistencia.setText("");
    }

    /**
     * Prepara el formulario. Todos los parámetros pueden ser null:
     * desde una alerta de caducidad llegan el lote y el motivo "Caducidad".
     * Devuelve false si no hubo conexión o no hay insumos con existencia.
     */
    // RF-60 (Registro de salida de insumos / merma): inicia aquí en la pantalla. Paso 3: muestra el formulario de merma.
    public boolean preparar(Insumo insumo, Lote lote, String motivo) {
        List<Insumo> conExistencia = new ArrayList<>();
        try {
            for (Insumo activo : new InsumoService().listar(false)) {
                if (activo.getExistencia() > 0) {
                    conExistencia.add(activo);
                }
            }
        } catch (DAOException e) {
            // RF-60 · Excepción paso 2: sin conexión, «Error de conexión».
            Alertas.error(InventarioService.MSG_ERROR_CONEXION);
            return false;
        }
        if (conExistencia.isEmpty()) {
            // RF-60 · Excepción paso 2: no hay insumos con existencia para registrar una merma.
            Alertas.aviso("No hay insumos con existencia para registrar una merma");
            return false;
        }
        cmbInsumo.getItems().setAll(conExistencia);

        // Si viene un lote, el insumo es el del lote
        int idInsumo = 0;
        if (insumo != null) {
            idInsumo = insumo.getIdInsumo();
        }
        if (lote != null) {
            idInsumo = lote.getIdInsumo();
        }
        for (Insumo opcion : conExistencia) {
            if (opcion.getIdInsumo() == idInsumo) {
                cmbInsumo.setValue(opcion);
            }
        }
        if (!llenarLotes()) {
            return false;
        }

        if (lote != null) {
            for (Lote opcion : cmbLote.getItems()) {
                if (opcion.getIdLote() == lote.getIdLote()) {
                    cmbLote.setValue(opcion);
                }
            }
            onCambioLote();
        }
        if (motivo != null) {
            cmbMotivo.setValue(motivo);
            onCambioMotivo();
        }
        return true;
    }

    @FXML
    private void onCambioInsumo() {
        if (!llenarLotes()) {
            Alertas.error(InventarioService.MSG_ERROR_CONEXION);
        }
    }

    // Lotes con existencia del insumo elegido; el primero siempre es "automático (PEPS)"
    private boolean llenarLotes() {
        cmbLote.getItems().clear();
        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            lblCantidad.setText("Cantidad *");
            lblExistencia.setText("");
            cmbLote.setDisable(true);
            return true;
        }
        List<Lote> lotes;
        try {
            lotes = servicio.listarLotes(insumo.getIdInsumo(), true);
        } catch (DAOException e) {
            return false;
        }
        cmbLote.getItems().add(automatico);
        cmbLote.getItems().addAll(lotes);
        cmbLote.setValue(automatico);
        cmbLote.setDisable(false);

        String unidad = insumo.getUnidad().getValorBD();
        lblCantidad.setText("Cantidad (" + unidad + ") *");
        lblExistencia.setText("Existencia disponible: " + Formatos.cantidad(insumo.getExistencia()) + " " + unidad);
        return true;
    }

    // Si se elige un lote, la existencia que se muestra es la de ese lote
    @FXML
    private void onCambioLote() {
        Insumo insumo = cmbInsumo.getValue();
        Lote lote = cmbLote.getValue();
        if (insumo == null || lote == null) {
            return;
        }
        String unidad = insumo.getUnidad().getValorBD();
        if (lote.getIdLote() == 0) {
            lblExistencia.setText("Existencia disponible: " + Formatos.cantidad(insumo.getExistencia()) + " " + unidad);
        } else {
            lblExistencia.setText("Existencia en el lote " + lote.getClave() + ": "
                    + Formatos.cantidad(lote.getExistencia()) + " " + unidad);
        }
    }

    // Con el motivo "Otro" el detalle es obligatorio
    @FXML
    private void onCambioMotivo() {
        if (InventarioService.MOTIVO_OTRO.equals(cmbMotivo.getValue())) {
            lblDetalle.setText("Detalle *");
            txtDetalle.setPromptText("Explica el motivo de la merma");
        } else {
            lblDetalle.setText("Detalle");
            txtDetalle.setPromptText("Opcional. Ej. Se cayó la bolsa al descargar");
        }
    }

    @FXML
    // RF-60 · Paso 4: la persona elige «Registrar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            mostrarError("Elige el insumo", cmbInsumo);
            return;
        }
        if (!ValidacionCatalogo.esDecimal(txtCantidad.getText())) {
            // RF-60 · Excepción paso 5: la cantidad debe ser mayor a cero.
            mostrarError(InventarioService.MSG_CANTIDAD_INVALIDA, txtCantidad);
            return;
        }
        int idLote = 0;
        if (cmbLote.getValue() != null) {
            idLote = cmbLote.getValue().getIdLote();
        }
        double cantidad = ValidacionCatalogo.aDecimal(txtCantidad.getText());

        ResultadoInventario resultado = servicio.registrarMerma(insumo.getIdInsumo(), cantidad,
                cmbMotivo.getValue(), txtDetalle.getText(), idLote);
        if (resultado.isExitoso()) {
            guardado = true;
            // RF-60 · Paso 9: informa el éxito y la nueva existencia.
            Alertas.info(resultado.getMensaje());
            cerrar();
            return;
        }
        if (resultado.isRecargarLista()) {
            // RF-60 · Excepción pasos 5 y 6: se muestra el error y no se registra la merma.
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }
        // El lote se quedó sin existencia: se vuelve a leer la lista
        if (ResultadoInventario.CAMPO_LOTE.equals(resultado.getCampo())) {
            llenarLotes();
        }
        mostrarError(resultado.getMensaje(), campoDe(resultado.getCampo()));
    }

    private Node campoDe(String campo) {
        if (ResultadoInventario.CAMPO_CANTIDAD.equals(campo)) {
            return txtCantidad;
        }
        if (ResultadoInventario.CAMPO_MOTIVO.equals(campo)) {
            return cmbMotivo;
        }
        if (ResultadoInventario.CAMPO_DETALLE.equals(campo)) {
            return txtDetalle;
        }
        if (ResultadoInventario.CAMPO_LOTE.equals(campo)) {
            return cmbLote;
        }
        return null;
    }

    @FXML
    // RF-60 · Excepción paso 4: «Cancelar» descarta los datos; la existencia no cambia.
    private void onCancelar() {
        cerrar();
    }

    private void mostrarError(String mensaje, Node campo) {
        lblMensaje.setText(mensaje);
        ApoyoVista.mostrarNodo(lblMensaje, true);
        if (campo != null) {
            ApoyoVista.marcar(campo);
        }
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.sizeToScene();
    }

    private void quitarMarcas() {
        Node[] campos = {cmbInsumo, cmbLote, txtCantidad, cmbMotivo, txtDetalle};
        for (Node campo : campos) {
            ApoyoVista.desmarcar(campo);
        }
    }

    private void cerrar() {
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.close();
    }

    public boolean seGuardo() {
        return guardado;
    }

    public boolean debeRecargarLista() {
        return recargarLista;
    }
}

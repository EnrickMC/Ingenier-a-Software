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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

/**
 * Ventana "Registrar compra / entrada" (RF-37) (formulario-entrada.fxml).
 */
public class FormularioEntradaController {

    @FXML private ComboBox<Insumo> cmbInsumo;
    @FXML private Label lblExistencia;
    @FXML private Label lblCantidad;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCosto;
    @FXML private Label lblCostoUnidad;
    @FXML private DatePicker dpCompra;
    @FXML private Label lblCaducidad;
    @FXML private DatePicker dpCaducidad;
    @FXML private TextField txtProveedor;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private InventarioService servicio = new InventarioService();
    private boolean guardado = false;
    private boolean recargarLista = false;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblMensaje, false);
        dpCompra.setValue(LocalDate.now());
        lblCostoUnidad.setText("");
        lblExistencia.setText("");
    }

    /**
     * Llena el selector con los insumos activos y elige el indicado (puede ser null).
     * Devuelve false si no hubo conexión o no hay insumos.
     */
    // RF-37 (Registro de entrada de insumos): inicia aquí en la pantalla. Paso 3: muestra el formulario de compra.
    public boolean preparar(Insumo insumo) {
        List<Insumo> activos;
        try {
            activos = new InsumoService().listar(false);
        } catch (DAOException e) {
            // RF-37 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            Alertas.error(InventarioService.MSG_ERROR_CONEXION);
            return false;
        }
        if (activos.isEmpty()) {
            Alertas.aviso("Primero registra un insumo en la pestaña Insumos");
            return false;
        }
        cmbInsumo.getItems().setAll(activos);
        if (insumo != null) {
            for (Insumo activo : activos) {
                if (activo.getIdInsumo() == insumo.getIdInsumo()) {
                    cmbInsumo.setValue(activo);
                }
            }
        }
        onCambioInsumo();
        return true;
    }

    // Al elegir el insumo se muestran su unidad, existencia y si pide caducidad
    @FXML
    private void onCambioInsumo() {
        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            lblCantidad.setText("Cantidad comprada *");
            lblCaducidad.setText("Fecha de caducidad");
            lblExistencia.setText("");
            onCambioCosto();
            return;
        }
        String unidad = insumo.getUnidad().getValorBD();
        lblCantidad.setText("Cantidad comprada (" + unidad + ") *");
        lblExistencia.setText("Existencia actual: " + Formatos.cantidad(insumo.getExistencia()) + " " + unidad
                + "  ·  Costo registrado: " + Formatos.moneda(insumo.getCostoUnitario()) + " por " + unidad);
        if (insumo.isPerecedero()) {
            lblCaducidad.setText("Fecha de caducidad *");
            dpCaducidad.setPromptText("Obligatoria");
        } else {
            lblCaducidad.setText("Fecha de caducidad");
            dpCaducidad.setPromptText("Sin caducidad");
        }
        onCambioCosto();
    }

    // Muestra en vivo cuánto cuesta cada unidad de esta compra
    @FXML
    private void onCambioCosto() {
        Insumo insumo = cmbInsumo.getValue();
        boolean numerosValidos = ValidacionCatalogo.esDecimal(txtCantidad.getText())
                && ValidacionCatalogo.esDecimal(txtCosto.getText());
        if (insumo == null || !numerosValidos) {
            lblCostoUnidad.setText("");
            return;
        }
        double cantidad = ValidacionCatalogo.aDecimal(txtCantidad.getText());
        double costo = ValidacionCatalogo.aDecimal(txtCosto.getText());
        if (cantidad <= 0) {
            lblCostoUnidad.setText("");
            return;
        }
        lblCostoUnidad.setText("= " + Formatos.moneda(costo / cantidad) + " por " + insumo.getUnidad().getValorBD());
    }

    @FXML
    // RF-37 · Paso 4: la persona elige «Registrar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            mostrarError("Elige el insumo que se compró", cmbInsumo);
            return;
        }
        if (!ValidacionCatalogo.esDecimal(txtCantidad.getText())) {
            // RF-37 · Excepción paso 5: cantidad o costo sin capturar o no mayores a cero.
            mostrarError(InventarioService.MSG_CANTIDAD_INVALIDA, txtCantidad);
            return;
        }
        if (!ValidacionCatalogo.esDecimal(txtCosto.getText())) {
            mostrarError(InventarioService.MSG_COSTO_INVALIDO, txtCosto);
            return;
        }

        Lote datos = new Lote();
        datos.setIdInsumo(insumo.getIdInsumo());
        datos.setCantidadComprada(ValidacionCatalogo.aDecimal(txtCantidad.getText()));
        datos.setCostoCompra(ValidacionCatalogo.aDecimal(txtCosto.getText()));
        datos.setFechaCompra(dpCompra.getValue());
        datos.setFechaCaducidad(dpCaducidad.getValue());
        datos.setProveedor(txtProveedor.getText());

        ResultadoInventario resultado = servicio.registrarEntrada(datos);
        // RF-37 · Paso 9: informa el éxito y la nueva existencia.
        if (resultado.isExitoso()) {
            guardado = true;
            String mensaje = resultado.getMensaje();
            if (resultado.getAviso() != null) {
                // RF-37 · Excepción paso 8: el lote está próximo a caducar, se muestra el aviso.
                Alertas.aviso(mensaje + "\n\n" + resultado.getAviso());
            } else {
                Alertas.info(mensaje);
            }
            ofrecerActualizarCosto(resultado, datos, insumo);
            cerrar();
            return;
        }
        if (resultado.isRecargarLista()) {
            // RF-37 · Excepción pasos 5 y 6: se muestra el error y no se registra la entrada.
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }
        mostrarError(resultado.getMensaje(), campoDe(resultado.getCampo()));
    }

    // La Administradora puede pasar el costo de esta compra al costo registrado del insumo
    private void ofrecerActualizarCosto(ResultadoInventario resultado, Lote datos, Insumo insumo) {
        if (!resultado.isOfrecerActualizarCosto()) {
            return;
        }
        String unidad = insumo.getUnidad().getValorBD();
        String pregunta = "Esta compra salió a " + Formatos.moneda(datos.getCostoPorUnidad()) + " por " + unidad
                + " y el costo registrado es " + Formatos.moneda(insumo.getCostoUnitario()) + " por " + unidad
                + ".\n¿Actualizar el costo del insumo? Cambiará el costo de sus recetas y el precio de las opciones del catálogo.";
        if (!Alertas.confirmar("Actualizar costo del insumo", pregunta)) {
            return;
        }
        ResultadoInventario cambio = servicio.actualizarCostoUnitario(insumo.getIdInsumo(), datos.getCostoPorUnidad());
        if (cambio.isExitoso()) {
            Alertas.info(cambio.getMensaje());
        } else {
            Alertas.error(cambio.getMensaje());
        }
    }

    private Node campoDe(String campo) {
        if (ResultadoInventario.CAMPO_CANTIDAD.equals(campo)) {
            return txtCantidad;
        }
        if (ResultadoInventario.CAMPO_COSTO.equals(campo)) {
            return txtCosto;
        }
        if (ResultadoInventario.CAMPO_FECHA_COMPRA.equals(campo)) {
            return dpCompra;
        }
        if (ResultadoInventario.CAMPO_CADUCIDAD.equals(campo)) {
            return dpCaducidad;
        }
        if (ResultadoInventario.CAMPO_PROVEEDOR.equals(campo)) {
            return txtProveedor;
        }
        return null;
    }

    @FXML
    // RF-37 · Excepción paso 4: «Cancelar» descarta los datos; la existencia no cambia.
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
        Node[] campos = {cmbInsumo, txtCantidad, txtCosto, dpCompra, dpCaducidad, txtProveedor};
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

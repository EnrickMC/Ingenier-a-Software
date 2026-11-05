package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.UnidadMedida;
import com.pac.modulos.inventario.service.InsumoService;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Ventana "Registrar insumo" (RF-52) / "Modificar insumo" (RF-53) (formulario-insumo.fxml).
 */
public class FormularioInsumoController {

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<UnidadMedida> cmbUnidad;
    @FXML private Label lblCosto;
    @FXML private Label lblStock;
    @FXML private TextField txtCosto;
    @FXML private TextField txtStockMinimo;
    @FXML private CheckBox chkPerecedero;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private InsumoService servicio = new InsumoService();
    private Insumo editado;

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";
    private int idGuardado = 0;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblMensaje, false);
        cmbUnidad.getItems().setAll(UnidadMedida.values());
        try {
            cmbCategoria.getItems().setAll(servicio.listarCategorias());
        } catch (DAOException e) {
            // Sin conexión el campo sigue funcionando: se escribe la categoría a mano
            cmbCategoria.getItems().clear();
        }
    }

    // Formulario en blanco
    // RF-52 · Paso 2: despliega el formulario vacío para registrar.
    public void prepararNuevo() {
        editado = null;
        lblTitulo.setText("Registrar insumo");
        txtStockMinimo.setText("0");
    }

    // Formulario con los datos actuales (las existencias no se cambian aquí)
    // RF-53 · Paso 3: muestra el formulario con los datos actuales del insumo.
    public void prepararEdicion(Insumo insumo) {
        editado = insumo;
        lblTitulo.setText("Modificar insumo");
        lblSubtitulo.setText(insumo.getClave() + " · Existencia actual: " + Formatos.cantidad(insumo.getExistencia())
                + " " + insumo.getUnidad().getValorBD() + ". Las existencias solo cambian con compras, mermas y producción.");
        txtNombre.setText(insumo.getNombre());
        cmbCategoria.setValue(insumo.getCategoria());
        cmbCategoria.getEditor().setText(insumo.getCategoria());
        cmbUnidad.setValue(insumo.getUnidad());
        txtCosto.setText(ValidacionCatalogo.numero(insumo.getCostoUnitario()));
        txtStockMinimo.setText(Formatos.cantidad(insumo.getStockMinimo()).replace(",", ""));
        chkPerecedero.setSelected(insumo.isPerecedero());
        onCambioUnidad();
    }

    // Las etiquetas muestran la unidad elegida: "Costo por kg", "Stock mínimo (kg)"
    @FXML
    private void onCambioUnidad() {
        UnidadMedida unidad = cmbUnidad.getValue();
        if (unidad == null) {
            lblCosto.setText("Costo por unidad *");
            lblStock.setText("Stock mínimo *");
            return;
        }
        lblCosto.setText("Costo por " + unidad.getValorBD() + " *");
        lblStock.setText("Stock mínimo (" + unidad.getValorBD() + ") *");
    }

    @FXML
    // RF-52 · Paso 3 / RF-53 · Paso 4: la administradora elige «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        // RF-52 · Excepción paso 4: costo o stock con formato inválido; se resalta el campo.
        if (!ValidacionCatalogo.esDecimal(txtCosto.getText())) {
            mostrarError("Escribe el costo por unidad (por ejemplo 28.50)", txtCosto);
            return;
        }
        if (!ValidacionCatalogo.esDecimal(txtStockMinimo.getText())) {
            mostrarError("Escribe el stock mínimo (0 si no quieres alerta)", txtStockMinimo);
            return;
        }

        Insumo datos = new Insumo();
        if (editado != null) {
            datos.setIdInsumo(editado.getIdInsumo());
        }
        datos.setNombre(txtNombre.getText());
        // En un ComboBox editable lo escrito está en el editor
        datos.setCategoria(cmbCategoria.getEditor().getText());
        datos.setUnidad(cmbUnidad.getValue());
        datos.setCostoUnitario(ValidacionCatalogo.aDecimal(txtCosto.getText()));
        datos.setStockMinimo(ValidacionCatalogo.aDecimal(txtStockMinimo.getText()));
        datos.setPerecedero(chkPerecedero.isSelected());

        ResultadoInventario resultado;
        if (editado == null) {
            resultado = servicio.registrar(datos);
            // RF-52: el nombre es de un insumo dado de baja → se ofrece reactivarlo
            // RF-52 · Excepción paso 5: el nombre es de un insumo dado de baja; se ofrece reactivarlo.
            if (resultado.isOfrecerReactivar()) {
                if (Alertas.confirmar("Insumo dado de baja", resultado.getMensaje())) {
                    resultado = servicio.reactivar(resultado.getIdRelacionado());
                } else {
                    mostrarError("Cambia el nombre para registrar un insumo distinto", txtNombre);
                    return;
                }
            }
        } else {
            resultado = servicio.modificar(datos);
        }

        if (resultado.isExitoso()) {
            guardado = true;
            mensajeExito = resultado.getMensaje();
            idGuardado = resultado.getIdRelacionado();
            if (idGuardado == 0 && editado != null) {
                idGuardado = editado.getIdInsumo();
            }
            cerrar();
            return;
        }
        if (resultado.isRecargarLista()) {
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }
        // RF-53: no se permite cambiar la unidad; se restablece la original
        // RF-53 · Excepción paso 6: cambio de unidad no permitido; se restablece la unidad original.
        if (ResultadoInventario.CAMPO_UNIDAD.equals(resultado.getCampo()) && editado != null) {
            cmbUnidad.setValue(editado.getUnidad());
            onCambioUnidad();
        }
        mostrarError(resultado.getMensaje(), campoDe(resultado.getCampo()));
    }

    // Convierte el nombre del campo del resultado en el control del formulario
    private Node campoDe(String campo) {
        if (ResultadoInventario.CAMPO_NOMBRE.equals(campo)) {
            return txtNombre;
        }
        if (ResultadoInventario.CAMPO_CATEGORIA.equals(campo)) {
            return cmbCategoria;
        }
        if (ResultadoInventario.CAMPO_UNIDAD.equals(campo)) {
            return cmbUnidad;
        }
        if (ResultadoInventario.CAMPO_COSTO.equals(campo)) {
            return txtCosto;
        }
        if (ResultadoInventario.CAMPO_STOCK.equals(campo)) {
            return txtStockMinimo;
        }
        return null;
    }

    @FXML
    // RF-52 · Excepción paso 3 / RF-53 · Excepción paso 4: «Cancelar» descarta los datos y cierra el formulario.
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
        Node[] campos = {txtNombre, cmbCategoria, cmbUnidad, txtCosto, txtStockMinimo};
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

    public String getMensajeExito() {
        return mensajeExito;
    }

    public int getIdGuardado() {
        return idGuardado;
    }
}

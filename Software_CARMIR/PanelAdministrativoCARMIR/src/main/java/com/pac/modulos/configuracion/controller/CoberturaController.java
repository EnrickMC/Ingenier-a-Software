package com.pac.modulos.configuracion.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.model.CodigoPostalCobertura;
import com.pac.modulos.configuracion.service.CoberturaService;
import com.pac.modulos.configuracion.service.ConfiguracionService;
import com.pac.modulos.configuracion.service.ResultadoConfiguracion;
import com.pac.util.Alertas;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * RF-69 (Administración de la cobertura de envío): pestaña "Cobertura de envío", códigos postales a los
 * que se entrega y el costo de cada uno. La página web usa esta lista en RF-10 · Paso 7.
 */
public class CoberturaController {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtCosto;
    @FXML private Button btnAgregar;
    @FXML private Button btnCambiarCosto;
    @FXML private Label lblError;
    @FXML private Label lblConteo;
    @FXML private TextField txtBuscar;
    @FXML private Button btnEliminar;
    @FXML private TableView<FilaCobertura> tblCobertura;

    private CoberturaService servicio = new CoberturaService();
    private boolean disponible = true;
    private boolean cargada = false;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblError, false);
        btnCambiarCosto.setDisable(true);
        btnEliminar.setDisable(true);
        // Al elegir un renglón se copian sus datos al formulario
        tblCobertura.getSelectionModel().selectedItemProperty().addListener(new AlElegirRenglon());
    }

    // RF-69 · Excepción paso 1: sin las tablas de configuración el formulario queda desactivado.
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
        txtCodigo.setDisable(!disponible);
        txtCosto.setDisable(!disponible);
        btnAgregar.setDisable(!disponible);
        txtBuscar.setDisable(!disponible);
    }

    // RF-69: inicia aquí. Paso 1: se consulta la primera vez que se abre la pestaña (y después de cada cambio)
    public void cargar() {
        if (!disponible) {
            return;
        }
        buscar();
        cargada = true;
    }

    private void buscar() {
        String inicio = txtBuscar.getText();
        if (inicio == null) {
            inicio = "";
        }
        List<CodigoPostalCobertura> lista;
        try {
            lista = servicio.buscar(inicio.trim());
        } catch (DAOException e) {
            // RF-69 · Excepción paso 1: «Error de conexión: no fue posible leer la configuración».
            mostrarError(ConfiguracionService.MSG_ERROR_CONEXION + "\n" + e.getMessage());
            return;
        }
        ObservableList<FilaCobertura> filas = FXCollections.observableArrayList();
        for (CodigoPostalCobertura cobertura : lista) {
            filas.add(new FilaCobertura(cobertura));
        }
        tblCobertura.setItems(filas);
        lblConteo.setText(filas.size() + " código(s) postal(es)");
    }

    // ===================== Botones =====================

    // RF-69 · Paso 1: búsqueda por los primeros dígitos del código postal.
    @FXML
    private void onBuscar() {
        if (cargada) {
            buscar();
        }
    }

    // RF-69 · Paso 2: el usuario capturó un código postal nuevo con su costo y eligió «Agregar».
    @FXML
    private void onAgregar() {
        limpiarMensajes();
        // RF-69 · Paso 3: la sesión sigue activa y con el permiso «Configurar sistema».
        if (!AccesoConfiguracion.revisar()) {
            return;
        }
        ResultadoConfiguracion resultado = servicio.agregar(txtCodigo.getText(), txtCosto.getText());
        terminar(resultado);
    }

    // RF-69 · Paso 2: el usuario eligió un código de la lista y «Cambiar costo».
    @FXML
    private void onCambiarCosto() {
        limpiarMensajes();
        FilaCobertura elegida = tblCobertura.getSelectionModel().getSelectedItem();
        if (elegida == null) {
            return;
        }
        if (!AccesoConfiguracion.revisar()) {
            return;
        }
        ResultadoConfiguracion resultado = servicio.cambiarCosto(elegida.getCodigoPostal(), txtCosto.getText());
        terminar(resultado);
    }

    // RF-69 · Paso 2: el usuario eligió un código de la lista y «Quitar».
    @FXML
    private void onEliminar() {
        limpiarMensajes();
        FilaCobertura elegida = tblCobertura.getSelectionModel().getSelectedItem();
        if (elegida == null) {
            return;
        }
        boolean seguro = Alertas.confirmar("Quitar código postal",
                "¿Quitar el código postal " + elegida.getCodigoPostal() + " de la cobertura de envío?");
        // RF-69 · Excepción paso 2: no confirmó; RF-69 · Excepción paso 3: sesión o permiso.
        if (!seguro || !AccesoConfiguracion.revisar()) {
            return;
        }
        ResultadoConfiguracion resultado = servicio.eliminar(elegida.getCodigoPostal());
        terminar(resultado);
    }

    @FXML
    private void onLimpiar() {
        limpiarMensajes();
        tblCobertura.getSelectionModel().clearSelection();
        txtCodigo.clear();
        txtCosto.clear();
        txtCodigo.setEditable(true);
    }

    // ===================== Apoyo =====================

    // RF-69 · Paso 7: muestra el resultado de agregar, cambiar o quitar, limpia el formulario y actualiza la lista
    // RF-69 · Excepciones pasos 4 a 6: se marca el campo con error y no cambia nada
    private void terminar(ResultadoConfiguracion resultado) {
        if (!resultado.isExitoso()) {
            if (ResultadoConfiguracion.CAMPO_CODIGO_POSTAL.equals(resultado.getCampo())) {
                ApoyoVista.marcar(txtCodigo);
            }
            if (ResultadoConfiguracion.CAMPO_COSTO.equals(resultado.getCampo())) {
                ApoyoVista.marcar(txtCosto);
            }
            mostrarError(resultado.getMensaje());
            return;
        }
        onLimpiar();
        buscar();
        Alertas.info(resultado.getMensaje());
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        ApoyoVista.mostrarNodo(lblError, true);
    }

    private void limpiarMensajes() {
        ApoyoVista.mostrarNodo(lblError, false);
        ApoyoVista.desmarcar(txtCodigo);
        ApoyoVista.desmarcar(txtCosto);
    }

    // Copia el renglón elegido al formulario; el código postal no se edita (es la llave)
    private class AlElegirRenglon implements ChangeListener<FilaCobertura> {
        @Override
        public void changed(ObservableValue<? extends FilaCobertura> valor, FilaCobertura anterior, FilaCobertura nueva) {
            boolean hayRenglon = nueva != null;
            btnCambiarCosto.setDisable(!hayRenglon || !disponible);
            btnEliminar.setDisable(!hayRenglon || !disponible);
            btnAgregar.setDisable(hayRenglon || !disponible);
            if (hayRenglon) {
                txtCodigo.setText(nueva.getCodigoPostal());
                txtCodigo.setEditable(false);
                txtCosto.setText(String.format("%.2f", nueva.getCobertura().getCostoEnvio()));
            } else {
                txtCodigo.setEditable(true);
            }
        }
    }
}

package com.pac.modulos.configuracion.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.configuracion.service.ConfiguracionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/**
 * Pestaña "Historial de cambios": los últimos cambios a los parámetros y a la cobertura.
 */
public class HistorialController {

    @FXML private Label lblConteo;
    @FXML private Label lblError;
    @FXML private TableView<FilaCambio> tblHistorial;

    private ConfiguracionService servicio = new ConfiguracionService();
    private boolean disponible = true;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblError, false);
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public void cargar() {
        ApoyoVista.mostrarNodo(lblError, false);
        if (!disponible) {
            lblError.setText(ConfiguracionController.MSG_SIN_TABLAS);
            ApoyoVista.mostrarNodo(lblError, true);
            return;
        }
        ObservableList<FilaCambio> filas = FXCollections.observableArrayList();
        try {
            for (CambioConfiguracion cambio : servicio.historial()) {
                filas.add(new FilaCambio(cambio));
            }
        } catch (DAOException e) {
            lblError.setText(ConfiguracionService.MSG_ERROR_CONEXION + "\n" + e.getMessage());
            ApoyoVista.mostrarNodo(lblError, true);
        }
        tblHistorial.setItems(filas);
        lblConteo.setText("Últimos " + filas.size() + " cambio(s), del más reciente al más antiguo");
    }

    @FXML
    private void onActualizar() {
        cargar();
    }
}

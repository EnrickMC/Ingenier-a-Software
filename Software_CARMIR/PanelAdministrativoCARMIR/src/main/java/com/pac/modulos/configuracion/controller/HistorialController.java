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
 * RF-70 (Visualización del historial de cambios de configuración): pestaña "Historial de cambios",
 * los últimos cambios a los parámetros, a la cobertura y a los datos de notificaciones.
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

    // RF-70: inicia aquí. Paso 1: se abrió la pestaña o se eligió «Actualizar».
    public void cargar() {
        ApoyoVista.mostrarNodo(lblError, false);
        // RF-70 · Paso 2 y Excepción paso 2: sin las tablas de configuración se avisa de la migración pendiente.
        if (!disponible) {
            lblError.setText(ConfiguracionController.MSG_SIN_TABLAS);
            ApoyoVista.mostrarNodo(lblError, true);
            return;
        }
        // RF-70 · Paso 3: consulta los últimos 300 cambios.
        ObservableList<FilaCambio> filas = FXCollections.observableArrayList();
        try {
            for (CambioConfiguracion cambio : servicio.historial()) {
                filas.add(new FilaCambio(cambio));
            }
        } catch (DAOException e) {
            // RF-70 · Excepción paso 3: «Error de conexión: no fue posible leer la configuración».
            lblError.setText(ConfiguracionService.MSG_ERROR_CONEXION + "\n" + e.getMessage());
            ApoyoVista.mostrarNodo(lblError, true);
        }
        // RF-70 · Paso 4: tabla del más reciente al más antiguo.
        tblHistorial.setItems(filas);
        lblConteo.setText("Últimos " + filas.size() + " cambio(s), del más reciente al más antiguo");
    }

    @FXML
    private void onActualizar() {
        cargar();
    }
}

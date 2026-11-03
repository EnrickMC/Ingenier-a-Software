package com.pac.modulos.configuracion.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.service.ConfiguracionService;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;

/**
 * Controlador del apartado "Configuración" (configuracion.fxml).
 * Tiene cuatro pestañas, cada una con su propio controlador:
 * Parámetros, Cobertura de envío, Historial de cambios y Respaldos y sistema.
 */
public class ConfiguracionController implements ControladorSeccion {

    public static final String MSG_SIN_TABLAS = "La base de datos todavía no tiene las tablas de configuración. "
            + "El panel usa los valores de app.properties. Ejecuta DBCarmir/migraciones/MIGRACION_COMPLETA.sql "
            + "para poder guardar cambios.";

    @FXML private Label lblAvisoMigracion;
    @FXML private Tab tabParametros;
    @FXML private Tab tabCobertura;
    @FXML private Tab tabHistorial;
    @FXML private Tab tabRespaldos;

    // JavaFX inyecta el controlador de cada <fx:include> como "<fx:id>Controller"
    @FXML private ParametrosController parametrosController;
    @FXML private CoberturaController coberturaController;
    @FXML private HistorialController historialController;
    @FXML private RespaldosController respaldosController;

    private boolean iniciado = false;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblAvisoMigracion, false);
    }

    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        // Si la tabla Configuracion no existe se avisa y solo se puede consultar
        boolean hayTablas = true;
        String motivo = null;
        try {
            new ConfiguracionService().listar();
        } catch (DAOException e) {
            hayTablas = false;
            motivo = e.getMessage();
        }
        if (!hayTablas) {
            lblAvisoMigracion.setText(MSG_SIN_TABLAS + "\n(" + motivo + ")");
            ApoyoVista.mostrarNodo(lblAvisoMigracion, true);
        }
        parametrosController.cargar(hayTablas);
        coberturaController.setDisponible(hayTablas);
        historialController.setDisponible(hayTablas);
        respaldosController.cargar();
        iniciado = true;
    }

    // Cada pestaña se actualiza al abrirla (por ejemplo, el historial después de guardar)
    @FXML
    private void onCambioPestana(Event evento) {
        if (!iniciado) {
            return;
        }
        Tab pestana = (Tab) evento.getSource();
        if (!pestana.isSelected()) {
            return;
        }
        if (pestana == tabCobertura) {
            coberturaController.cargar();
        } else if (pestana == tabHistorial) {
            historialController.cargar();
        } else if (pestana == tabRespaldos) {
            respaldosController.cargar();
        }
    }
}

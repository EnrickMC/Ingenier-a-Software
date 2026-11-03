package com.pac.modulos.configuracion.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.model.ClavesConfiguracion;
import com.pac.modulos.configuracion.model.Parametro;
import com.pac.modulos.configuracion.service.ConfiguracionService;
import com.pac.modulos.configuracion.service.ResultadoConfiguracion;
import com.pac.util.Alertas;
import com.pac.util.ConfiguracionApp;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña "Parámetros": un campo por cada valor de la tabla Configuracion.
 * Al guardar se revisan todos los valores; solo se guardan los que cambiaron y cada cambio
 * queda en el historial con el usuario que lo hizo.
 */
public class ParametrosController {

    public static final String MSG_ORIGEN_BD = "Valores guardados en la base de datos. Los cambios se aplican en cuanto se guardan.";
    public static final String MSG_ORIGEN_ARCHIVO = "Mostrando los valores de app.properties (solo consulta).";

    @FXML private Label lblOrigen;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;
    @FXML private Button btnDescartar;
    @FXML private TextField txtNombreNegocio;
    @FXML private TextField txtCapacidadDiaria;
    @FXML private TextField txtDiasParaPreparar;
    @FXML private TextField txtDiasMaximosAgenda;
    @FXML private TextField txtDiasAnticipacion;
    @FXML private TextField txtHoraApertura;
    @FXML private TextField txtHoraCierre;
    @FXML private CheckBox chkLunes;
    @FXML private CheckBox chkMartes;
    @FXML private CheckBox chkMiercoles;
    @FXML private CheckBox chkJueves;
    @FXML private CheckBox chkViernes;
    @FXML private CheckBox chkSabado;
    @FXML private CheckBox chkDomingo;
    @FXML private TextField txtPorcentajeAnticipo;
    @FXML private TextArea txtDatosBancarios;
    @FXML private TextField txtDiasAvisoCaducidad;
    @FXML private TextField txtIntentosLogin;
    @FXML private TextField txtMinutosInactividad;

    private ConfiguracionService servicio = new ConfiguracionService();
    private boolean hayTablas = true;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblError, false);
    }

    // ===================== Cargar =====================

    // Llena los campos con lo guardado; si no hay tablas, con lo que usa el panel ahora
    public void cargar(boolean tablasDisponibles) {
        hayTablas = tablasDisponibles;
        limpiarMarcas();
        if (hayTablas) {
            try {
                mostrar(servicio.listar());
                lblOrigen.setText(MSG_ORIGEN_BD);
            } catch (DAOException e) {
                hayTablas = false;
            }
        }
        if (!hayTablas) {
            mostrarValoresDelArchivo();
            lblOrigen.setText(MSG_ORIGEN_ARCHIVO);
        }
        btnGuardar.setDisable(!hayTablas);
        btnDescartar.setDisable(!hayTablas);
    }

    private void mostrar(List<Parametro> parametros) {
        for (Parametro parametro : parametros) {
            TextInputControl campo = campoDe(parametro.getClave());
            if (campo != null) {
                campo.setText(parametro.getValor());
            }
        }
        String dias = ConfiguracionService.valorDe(parametros, ClavesConfiguracion.DIAS_LABORABLES);
        marcarDias(dias);
    }

    // Sin la tabla Configuracion: se muestran los valores con los que trabaja el panel
    private void mostrarValoresDelArchivo() {
        ConfiguracionApp app = ConfiguracionApp.getInstancia();
        txtNombreNegocio.setText(app.getNombreNegocio());
        txtCapacidadDiaria.setText(String.valueOf(app.getCapacidadDiaria()));
        txtDiasParaPreparar.setText(String.valueOf(app.getDiasParaPreparar()));
        txtDiasMaximosAgenda.setText(String.valueOf(app.getDiasMaximosAgenda()));
        txtDiasAvisoCaducidad.setText(String.valueOf(app.getDiasAvisoCaducidad()));
        txtIntentosLogin.setText(String.valueOf(app.getIntentosMaximosLogin()));
        txtMinutosInactividad.setText(String.valueOf(app.getMinutosInactividadSesion()));
        // Estos valores solo existen en la base de datos: se muestran los de fábrica
        txtDiasAnticipacion.setText("3");
        txtHoraApertura.setText("09:00");
        txtHoraCierre.setText("19:00");
        txtPorcentajeAnticipo.setText("50");
        txtDatosBancarios.setText("");
        marcarDias("L,M,X,J,V,S");
    }

    // ===================== Botones =====================

    @FXML
    private void onDescartar() {
        cargar(hayTablas);
    }

    @FXML
    private void onGuardar() {
        limpiarMarcas();
        if (!AccesoConfiguracion.revisar()) {
            return;
        }
        ResultadoConfiguracion resultado = servicio.guardar(leerFormulario());
        if (!resultado.isExitoso()) {
            // Se marca el campo con el problema (si es de un campo)
            Node campo = nodoDe(resultado.getCampo());
            if (campo != null) {
                ApoyoVista.marcar(campo);
                campo.requestFocus();
            }
            lblError.setText(resultado.getMensaje());
            ApoyoVista.mostrarNodo(lblError, true);
            return;
        }
        Alertas.info(resultado.getMensaje());
        cargar(true);
    }

    // ===================== Apoyo =====================

    // Arma la lista de parámetros con lo escrito en el formulario
    private List<Parametro> leerFormulario() {
        List<Parametro> parametros = new ArrayList<Parametro>();
        String[] claves = {
                ClavesConfiguracion.NOMBRE_NEGOCIO, ClavesConfiguracion.CAPACIDAD_DIARIA,
                ClavesConfiguracion.DIAS_PARA_PREPARAR, ClavesConfiguracion.DIAS_MAXIMOS_AGENDA,
                ClavesConfiguracion.DIAS_ANTICIPACION_MINIMA, ClavesConfiguracion.HORA_APERTURA,
                ClavesConfiguracion.HORA_CIERRE, ClavesConfiguracion.PORCENTAJE_ANTICIPO,
                ClavesConfiguracion.DATOS_BANCARIOS, ClavesConfiguracion.DIAS_AVISO_CADUCIDAD,
                ClavesConfiguracion.INTENTOS_MAXIMOS_LOGIN, ClavesConfiguracion.MINUTOS_INACTIVIDAD};
        for (String clave : claves) {
            parametros.add(new Parametro(clave, campoDe(clave).getText(), null));
        }
        parametros.add(new Parametro(ClavesConfiguracion.DIAS_LABORABLES, leerDias(), null));
        return parametros;
    }

    // Campo de texto de cada clave
    private TextInputControl campoDe(String clave) {
        if (ClavesConfiguracion.NOMBRE_NEGOCIO.equals(clave)) {
            return txtNombreNegocio;
        }
        if (ClavesConfiguracion.CAPACIDAD_DIARIA.equals(clave)) {
            return txtCapacidadDiaria;
        }
        if (ClavesConfiguracion.DIAS_PARA_PREPARAR.equals(clave)) {
            return txtDiasParaPreparar;
        }
        if (ClavesConfiguracion.DIAS_MAXIMOS_AGENDA.equals(clave)) {
            return txtDiasMaximosAgenda;
        }
        if (ClavesConfiguracion.DIAS_ANTICIPACION_MINIMA.equals(clave)) {
            return txtDiasAnticipacion;
        }
        if (ClavesConfiguracion.HORA_APERTURA.equals(clave)) {
            return txtHoraApertura;
        }
        if (ClavesConfiguracion.HORA_CIERRE.equals(clave)) {
            return txtHoraCierre;
        }
        if (ClavesConfiguracion.PORCENTAJE_ANTICIPO.equals(clave)) {
            return txtPorcentajeAnticipo;
        }
        if (ClavesConfiguracion.DATOS_BANCARIOS.equals(clave)) {
            return txtDatosBancarios;
        }
        if (ClavesConfiguracion.DIAS_AVISO_CADUCIDAD.equals(clave)) {
            return txtDiasAvisoCaducidad;
        }
        if (ClavesConfiguracion.INTENTOS_MAXIMOS_LOGIN.equals(clave)) {
            return txtIntentosLogin;
        }
        if (ClavesConfiguracion.MINUTOS_INACTIVIDAD.equals(clave)) {
            return txtMinutosInactividad;
        }
        return null;
    }

    // Nodo que se marca en rojo según la clave con error
    private Node nodoDe(String clave) {
        if (ClavesConfiguracion.DIAS_LABORABLES.equals(clave)) {
            return chkLunes.getParent();
        }
        return campoDe(clave);
    }

    // Casillas en el mismo orden que ClavesConfiguracion.LETRAS_DIAS (L M X J V S D)
    private CheckBox[] casillasDias() {
        return new CheckBox[] {chkLunes, chkMartes, chkMiercoles, chkJueves, chkViernes, chkSabado, chkDomingo};
    }

    // "L,M,X" → marca lunes, martes y miércoles
    private void marcarDias(String dias) {
        String texto = "";
        if (dias != null) {
            texto = "," + dias.replace(" ", "") + ",";
        }
        CheckBox[] casillas = casillasDias();
        for (int i = 0; i < casillas.length; i++) {
            casillas[i].setSelected(texto.contains("," + ClavesConfiguracion.LETRAS_DIAS[i] + ","));
        }
    }

    // Casillas marcadas → "L,M,X"
    private String leerDias() {
        StringBuilder dias = new StringBuilder();
        CheckBox[] casillas = casillasDias();
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i].isSelected()) {
                if (dias.length() > 0) {
                    dias.append(",");
                }
                dias.append(ClavesConfiguracion.LETRAS_DIAS[i]);
            }
        }
        return dias.toString();
    }

    private void limpiarMarcas() {
        ApoyoVista.mostrarNodo(lblError, false);
        String[] claves = {
                ClavesConfiguracion.NOMBRE_NEGOCIO, ClavesConfiguracion.CAPACIDAD_DIARIA,
                ClavesConfiguracion.DIAS_PARA_PREPARAR, ClavesConfiguracion.DIAS_MAXIMOS_AGENDA,
                ClavesConfiguracion.DIAS_ANTICIPACION_MINIMA, ClavesConfiguracion.HORA_APERTURA,
                ClavesConfiguracion.HORA_CIERRE, ClavesConfiguracion.PORCENTAJE_ANTICIPO,
                ClavesConfiguracion.DATOS_BANCARIOS, ClavesConfiguracion.DIAS_AVISO_CADUCIDAD,
                ClavesConfiguracion.INTENTOS_MAXIMOS_LOGIN, ClavesConfiguracion.MINUTOS_INACTIVIDAD,
                ClavesConfiguracion.DIAS_LABORABLES};
        for (String clave : claves) {
            ApoyoVista.desmarcar(nodoDe(clave));
        }
    }
}

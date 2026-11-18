package com.pac.modulos.configuracion.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.service.ConexionAwsService;
import com.pac.modulos.configuracion.service.ResultadoConfiguracion;
import com.pac.util.ConfiguracionAws;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.File;
import java.util.List;

/**
 * Tarjeta "Conexión con AWS" de la pestaña "Respaldos y sistema".
 * Muestra la configuración de AWS de esta PC (archivo .carmir\aws.properties y perfil de AWS CLI).
 * Solo lectura: los datos se cambian con 1_INSTALAR_PRIMERA_VEZ.bat. Las claves secretas nunca se muestran.
 */
public class ConexionAwsController {

    private static final String SIN_DATO = "(sin configurar)";

    @FXML private Label lblModo;
    @FXML private Label lblArchivo;
    @FXML private Label lblPerfil;
    @FXML private Label lblRegion;
    @FXML private Label lblAccessKey;
    @FXML private Label lblSecreta;
    @FXML private Label lblCli;
    @FXML private Label lblInstancia;
    @FXML private Label lblServidorRds;
    @FXML private Label lblTunel;
    @FXML private Label lblBaseAws;
    @FXML private Label lblUsuarioAws;
    @FXML private Label lblContrasenaAws;
    @FXML private Label lblBucket;
    @FXML private Label lblDominio;
    @FXML private Button btnProbarAws;
    @FXML private Label lblResultadoAws;

    private ConexionAwsService servicio = new ConexionAwsService();

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblResultadoAws, false);
    }

    // Llena la tarjeta (la llama RespaldosController al abrir la pestaña)
    public void cargar() {
        ConfiguracionAws aws = ConfiguracionAws.getInstancia();

        if (aws.isActivo()) {
            lblModo.setText("Conectado a AWS (abierto con INICIAR_CARMIR_AWS.bat)");
        } else {
            lblModo.setText("Base de datos local (abierto con 2_INICIAR_SISTEMA.bat)");
        }

        File archivo = aws.getArchivo();
        if (!aws.existeArchivo()) {
            lblArchivo.setText(archivo.getPath() + " no existe. Ejecuta 1_INSTALAR_PRIMERA_VEZ.bat y elige la opción AWS.");
        } else {
            List<String> faltan = aws.clavesFaltantes();
            if (faltan.isEmpty()) {
                lblArchivo.setText(archivo.getPath());
            } else {
                lblArchivo.setText(archivo.getPath() + "  (faltan: " + String.join(", ", faltan) + ")");
            }
        }

        lblPerfil.setText(dato(aws.getPerfil()));
        lblRegion.setText(dato(aws.getRegion()));

        // Llaves de AWS CLI: la Access Key ID se muestra tapada y la secreta solo dice si existe
        String llave = aws.getAccessKeyId();
        if (llave.isEmpty()) {
            lblAccessKey.setText("No hay llaves para este perfil en " + aws.getArchivoCredenciales().getPath());
        } else {
            lblAccessKey.setText(servicio.ocultarLlave(llave));
        }
        if (aws.tieneClaveSecreta()) {
            lblSecreta.setText("Guardada (oculta)");
        } else {
            lblSecreta.setText("No guardada");
        }

        File cli = aws.buscarAwsCli();
        if (cli == null) {
            lblCli.setText("No se encontró aws.exe en esta PC");
        } else {
            lblCli.setText(cli.getAbsolutePath());
        }

        lblInstancia.setText(dato(aws.getInstanciaEc2()));
        if (aws.getServidorRds().isEmpty()) {
            lblServidorRds.setText(SIN_DATO);
        } else {
            lblServidorRds.setText(aws.getServidorRds() + " : " + dato(aws.getPuertoRds()));
        }
        if (aws.getPuertoLocal().isEmpty()) {
            lblTunel.setText(SIN_DATO);
        } else if (servicio.tunelAbierto(aws.getPuertoLocal())) {
            lblTunel.setText("127.0.0.1:" + aws.getPuertoLocal() + " (abierto)");
        } else {
            lblTunel.setText("127.0.0.1:" + aws.getPuertoLocal() + " (cerrado)");
        }

        lblBaseAws.setText(dato(aws.getBaseDatos()));
        lblUsuarioAws.setText(dato(aws.getUsuarioBD()));
        if (aws.getContrasenaBD().isEmpty()) {
            lblContrasenaAws.setText("No guardada");
        } else {
            lblContrasenaAws.setText("Guardada (oculta)");
        }
        lblBucket.setText(dato(aws.getBucketImagenes()));
        lblDominio.setText(dato(aws.getDominioImagenes()));
    }

    // Texto para mostrar: el valor o "(sin configurar)"
    private String dato(String valor) {
        if (valor == null || valor.isEmpty()) {
            return SIN_DATO;
        }
        return valor;
    }

    // ===================== Botones =====================

    // Vuelve a leer aws.properties y las llaves (por si se corrió otra vez el instalador)
    @FXML
    private void onRecargar() {
        ConfiguracionAws.getInstancia().recargar();
        ApoyoVista.mostrarNodo(lblResultadoAws, false);
        cargar();
    }

    // Pregunta a AWS si las llaves del perfil funcionan (en segundo plano para no congelar la pantalla)
    @FXML
    private void onProbarCredenciales() {
        btnProbarAws.setDisable(true);
        lblResultadoAws.setText("Probando credenciales con AWS…");
        ApoyoVista.mostrarNodo(lblResultadoAws, true);

        TareaProbar tarea = new TareaProbar(servicio);
        tarea.setOnSucceeded(new AlTerminar(tarea));
        tarea.setOnFailed(new AlTerminar(tarea));
        Thread hilo = new Thread(tarea, "carmir-probar-aws");
        hilo.setDaemon(true);
        hilo.start();
    }

    // Tarea en segundo plano: ejecuta aws sts get-caller-identity
    private static class TareaProbar extends Task<ResultadoConfiguracion> {
        private final ConexionAwsService servicio;

        TareaProbar(ConexionAwsService servicio) {
            this.servicio = servicio;
        }

        @Override
        protected ResultadoConfiguracion call() {
            return servicio.probarCredenciales();
        }
    }

    // Muestra el resultado al terminar (ya en el hilo de la pantalla)
    private class AlTerminar implements EventHandler<WorkerStateEvent> {
        private final TareaProbar tarea;

        AlTerminar(TareaProbar tarea) {
            this.tarea = tarea;
        }

        @Override
        public void handle(WorkerStateEvent evento) {
            btnProbarAws.setDisable(false);
            ResultadoConfiguracion resultado = tarea.getValue();
            if (resultado == null) {
                lblResultadoAws.setText("No se pudo probar la conexión con AWS");
                return;
            }
            lblResultadoAws.setText(resultado.getMensaje());
            cargar();
        }
    }
}

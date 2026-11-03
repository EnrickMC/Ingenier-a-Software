package com.pac.modulos.configuracion.controller;

import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.configuracion.service.PermisosConfiguracion;
import com.pac.modulos.configuracion.service.RespaldoService;
import com.pac.modulos.configuracion.service.ResultadoConfiguracion;
import com.pac.modulos.pedidos.notificacion.ConfiguracionNotificaciones;
import com.pac.util.Alertas;
import com.pac.util.ConfiguracionApp;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Pestaña "Respaldos y sistema": datos de la conexión, cuentas de WhatsApp y correo de las
 * notificaciones (IntegracionesController, RF-32) y respaldos de la base de datos (RNF-8).
 * Generar un respaldo requiere «Configurar sistema»; restaurarlo, ser la administradora.
 */
public class RespaldosController {

    @FXML private Label lblServidor;
    @FXML private Label lblBaseDatos;
    @FXML private Label lblUsuarioBD;
    @FXML private Label lblEstadoConexion;
    @FXML private Label lblOrigenParametros;
    @FXML private Label lblNotificaciones;
    @FXML private TextField txtCarpetaMysql;
    @FXML private Label lblProgramas;
    @FXML private Button btnGenerar;
    @FXML private Button btnRestaurar;
    @FXML private Label lblSoloAdministradora;
    @FXML private Label lblResultado;

    // Tarjeta "Notificaciones al cliente" (integraciones.fxml): JavaFX la inyecta como "<fx:id>Controller"
    @FXML private IntegracionesController integracionesController;

    private RespaldoService servicio = new RespaldoService();

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblResultado, false);
    }

    // Muestra los datos del sistema (se llama al abrir el apartado y al volver a la pestaña)
    public void cargar() {
        ConexionBD conexion = ConexionBD.getInstancia();
        lblServidor.setText(servicio.getServidor() + " : " + servicio.getPuerto());
        lblBaseDatos.setText(servicio.getBaseDeDatos());
        lblUsuarioBD.setText(conexion.getUsuario());
        lblEstadoConexion.setText("Sin probar");

        if (ConfiguracionApp.getInstancia().usaValoresBD()) {
            lblOrigenParametros.setText("Se leen de la tabla Configuracion de la base de datos");
        } else {
            lblOrigenParametros.setText("Se leen de config/app.properties (falta la migración o no hay conexión)");
        }

        // Datos de WhatsApp y correo (lee la tabla ConfiguracionNotificacion y llena la tarjeta)
        integracionesController.cargar();
        if (ConfiguracionNotificaciones.getInstancia().isSimulado()) {
            lblNotificaciones.setText("Modo simulado: los mensajes no se envían, solo se registran");
        } else {
            lblNotificaciones.setText("Envío real por WhatsApp y correo");
        }

        boolean esAdministradora = PermisosConfiguracion.puedeRestaurar();
        btnRestaurar.setDisable(!esAdministradora);
        ApoyoVista.mostrarNodo(lblSoloAdministradora, !esAdministradora);
        mostrarProgramas();
    }

    // Indica si se encontraron mysqldump y mysql
    private void mostrarProgramas() {
        File mysqldump = servicio.buscarPrograma("mysqldump");
        File mysql = servicio.buscarPrograma("mysql");
        if (mysqldump != null && mysql != null) {
            lblProgramas.setText("Programas encontrados en: " + mysqldump.getParent());
            if (txtCarpetaMysql.getText() == null || txtCarpetaMysql.getText().isEmpty()) {
                txtCarpetaMysql.setText(mysqldump.getParent());
            }
        } else {
            lblProgramas.setText("No se encontraron mysqldump y mysql. Elige la carpeta \"bin\" de MySQL "
                    + "(por ejemplo C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin) o escríbela en "
                    + "app.properties como respaldos.carpetaMysql.");
        }
    }

    // ===================== Botones =====================

    @FXML
    private void onProbarConexion() {
        if (ConexionBD.getInstancia().probarConexion()) {
            lblEstadoConexion.setText("Conectado correctamente");
        } else {
            lblEstadoConexion.setText("Sin conexión: revisa que MySQL esté encendido y los datos de db.properties");
        }
    }

    @FXML
    private void onElegirCarpeta() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Carpeta bin de MySQL");
        File carpeta = selector.showDialog(txtCarpetaMysql.getScene().getWindow());
        if (carpeta == null) {
            return;
        }
        RespaldoService.setCarpetaElegida(carpeta.getAbsolutePath());
        txtCarpetaMysql.setText(carpeta.getAbsolutePath());
        mostrarProgramas();
    }

    @FXML
    private void onGenerarRespaldo() {
        ApoyoVista.mostrarNodo(lblResultado, false);
        if (!AccesoConfiguracion.revisar()) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar respaldo");
        selector.setInitialFileName(servicio.nombreSugerido());
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo SQL (*.sql)", "*.sql"));
        File destino = selector.showSaveDialog(btnGenerar.getScene().getWindow());
        if (destino == null) {
            return;
        }
        if (!destino.getName().toLowerCase().endsWith(".sql")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".sql");
        }
        ejecutarEnSegundoPlano(new TareaRespaldo(servicio, destino, false), "Generando respaldo…");
    }

    @FXML
    private void onRestaurarRespaldo() {
        ApoyoVista.mostrarNodo(lblResultado, false);
        if (!PermisosConfiguracion.puedeRestaurar()) {
            Alertas.error(PermisosConfiguracion.MSG_SOLO_ADMINISTRADORA);
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir respaldo");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo SQL (*.sql)", "*.sql"));
        File origen = selector.showOpenDialog(btnRestaurar.getScene().getWindow());
        if (origen == null) {
            return;
        }
        if (!servicio.esRespaldoValido(origen)) {
            Alertas.error(RespaldoService.MSG_ARCHIVO_NO_VALIDO);
            return;
        }
        // Doble confirmación: la restauración reemplaza todos los datos actuales
        boolean seguro = Alertas.confirmarDoble("Restaurar respaldo",
                "Se reemplazarán TODOS los datos actuales de " + servicio.getBaseDeDatos()
                        + " por los del archivo:\n" + origen.getName()
                        + "\n\nLo que se haya registrado después del respaldo se perderá.");
        if (!seguro) {
            return;
        }
        ejecutarEnSegundoPlano(new TareaRespaldo(servicio, origen, true), "Restaurando respaldo…");
    }

    // ===================== Segundo plano =====================

    // mysqldump y mysql pueden tardar: se ejecutan sin congelar la pantalla
    private void ejecutarEnSegundoPlano(TareaRespaldo tarea, String mensaje) {
        btnGenerar.setDisable(true);
        btnRestaurar.setDisable(true);
        lblResultado.setText(mensaje);
        ApoyoVista.mostrarNodo(lblResultado, true);
        tarea.setOnSucceeded(new AlTerminar(tarea));
        tarea.setOnFailed(new AlTerminar(tarea));
        Thread hilo = new Thread(tarea, "carmir-respaldo");
        hilo.setDaemon(true);
        hilo.start();
    }

    // Muestra el resultado al terminar (ya en el hilo de la pantalla)
    private class AlTerminar implements EventHandler<WorkerStateEvent> {
        private final TareaRespaldo tarea;

        AlTerminar(TareaRespaldo tarea) {
            this.tarea = tarea;
        }

        @Override
        public void handle(WorkerStateEvent evento) {
            btnGenerar.setDisable(false);
            btnRestaurar.setDisable(!PermisosConfiguracion.puedeRestaurar());
            ApoyoVista.mostrarNodo(lblResultado, false);
            ResultadoConfiguracion resultado = tarea.getValue();
            if (resultado == null) {
                String detalle = "";
                if (tarea.getException() != null) {
                    detalle = "\n" + tarea.getException().getMessage();
                }
                Alertas.error(RespaldoService.MSG_RESPALDO_ERROR + detalle);
                return;
            }
            if (resultado.isExitoso()) {
                Alertas.info(resultado.getMensaje());
            } else {
                Alertas.error(resultado.getMensaje());
            }
            cargar();
        }
    }

    // Genera o restaura el respaldo fuera del hilo de la pantalla
    private static class TareaRespaldo extends Task<ResultadoConfiguracion> {
        private final RespaldoService servicio;
        private final File archivo;
        private final boolean restaurar;

        TareaRespaldo(RespaldoService servicio, File archivo, boolean restaurar) {
            this.servicio = servicio;
            this.archivo = archivo;
            this.restaurar = restaurar;
        }

        @Override
        protected ResultadoConfiguracion call() {
            if (restaurar) {
                return servicio.restaurarRespaldo(archivo);
            }
            return servicio.generarRespaldo(archivo);
        }
    }
}

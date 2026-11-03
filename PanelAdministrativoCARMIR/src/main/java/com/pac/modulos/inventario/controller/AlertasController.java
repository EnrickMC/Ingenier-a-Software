package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.model.TipoAlerta;
import com.pac.modulos.inventario.service.AlertaService;
import com.pac.modulos.inventario.service.MonitorAlertas;
import com.pac.modulos.inventario.service.PermisosInventario;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pestaña Alertas (alertas.fxml): existencia baja / agotado (RF-39) y caducidad (RF-40).
 * Desde aquí se atiende la alerta: registrar compra, merma del lote o marcarla como atendida.
 */
public class AlertasController {

    @FXML private CheckBox chkAtendidas;
    @FXML private Label lblUltimaRevision;
    @FXML private Label lblConteo;
    @FXML private TableView<FilaAlerta> tblAlertas;
    @FXML private Label lblVacio;
    @FXML private Label lblSeleccion;
    @FXML private Button btnCompra;
    @FXML private Button btnMermaLote;
    @FXML private Button btnAtendida;

    private AlertaService servicio = new AlertaService();
    private InventarioController inventario;
    private boolean puedeMover;

    @FXML
    private void initialize() {
        // Cada renglón se pinta según el tipo de alerta
        // (JavaFX necesita la flecha "->" para indicar cómo crear cada renglón)
        tblAlertas.setRowFactory(tabla -> new RenglonAlerta());
        lblUltimaRevision.setText("");
    }

    public void iniciar(InventarioController inventario) {
        this.inventario = inventario;
        puedeMover = PermisosInventario.puedeRegistrarMovimientos();
    }

    // Se llama cada vez que la pestaña queda visible
    // RF-39 / RF-40 · Paso 7 (RF-39) y paso 6 (RF-40): muestra las alertas de stock bajo y de caducidad.
    public void alMostrar() {
        cargar();
    }

    @FXML
    private void onFiltro() {
        cargar();
    }

    // Vuelve a revisar existencias y caducidades en este momento
    @FXML
    private void onRevisar() {
        // RF-39 / RF-40 · Paso 1: revisión manual de existencias y caducidades.
        boolean revisado = MonitorAlertas.getInstancia().revisar();
        if (!revisado) {
            mostrarErrorConexion();
            return;
        }
        lblUltimaRevision.setText("Revisado a las " + Formatos.hora(LocalDateTime.now()));
        cargar();
    }

    private void cargar() {
        tblAlertas.getItems().clear();
        List<AlertaInventario> alertas;
        try {
            alertas = servicio.listar(!chkAtendidas.isSelected());
        } catch (DAOException e) {
            mostrarErrorConexion();
            return;
        }
        int pendientes = 0;
        for (AlertaInventario alerta : alertas) {
            tblAlertas.getItems().add(new FilaAlerta(alerta));
            if (alerta.isPendiente()) {
                pendientes = pendientes + 1;
            }
        }
        lblConteo.setText(pendientes + " pendiente(s)");
        if (chkAtendidas.isSelected()) {
            lblVacio.setText("No hay alertas registradas");
        } else {
            lblVacio.setText("No hay alertas pendientes. El inventario está en orden.");
        }
        actualizarBotones();
    }

    private void mostrarErrorConexion() {
        tblAlertas.getItems().clear();
        // RF-40 · Excepción paso 2: sin conexión no se pudo revisar.
        lblVacio.setText(AlertaService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
        actualizarBotones();
    }

    @FXML
    private void onClicTabla() {
        actualizarBotones();
    }

    // Los botones dependen del tipo de la alerta elegida
    private void actualizarBotones() {
        FilaAlerta fila = tblAlertas.getSelectionModel().getSelectedItem();
        boolean pendiente = fila != null && fila.getAlerta().isPendiente();
        boolean deCaducidad = pendiente && fila.getAlerta().getTipo().esDeCaducidad();

        ApoyoVista.mostrarNodo(btnCompra, puedeMover && pendiente && !deCaducidad);
        ApoyoVista.mostrarNodo(btnMermaLote, puedeMover && deCaducidad);
        ApoyoVista.mostrarNodo(btnAtendida, puedeMover && pendiente);

        if (fila == null) {
            lblSeleccion.setText("Selecciona una alerta para atenderla");
        } else if (!pendiente) {
            lblSeleccion.setText("Esta alerta ya fue atendida");
        } else if (!puedeMover) {
            lblSeleccion.setText(PermisosInventario.MSG_SOLO_CONSULTA);
        } else if (deCaducidad) {
            lblSeleccion.setText("Si el lote ya no sirve, regístralo como merma por caducidad");
        } else {
            lblSeleccion.setText("Registra una compra para reponer «" + fila.getInsumo() + "»");
        }
    }

    // Existencia baja: abre la compra con el insumo ya elegido
    @FXML
    private void onCompra() {
        FilaAlerta fila = tblAlertas.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(fila.getAlerta().getIdInsumo());
        if (inventario.abrirEntrada(insumo)) {
            cargar();
        }
    }

    // Caducidad: abre la merma con el lote y el motivo "Caducidad" ya elegidos
    @FXML
    // RF-40 · Excepción paso 4: lote caducado; se sugiere registrarlo como merma (RF-38).
    private void onMermaLote() {
        FilaAlerta fila = tblAlertas.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }
        AlertaInventario alerta = fila.getAlerta();
        Lote lote = new Lote();
        lote.setIdLote(alerta.getIdLote());
        lote.setIdInsumo(alerta.getIdInsumo());
        String motivo = null;
        if (alerta.getTipo() == TipoAlerta.CADUCADO) {
            motivo = "Caducidad";
        }
        if (inventario.abrirMerma(null, lote, motivo)) {
            cargar();
        }
    }

    @FXML
    private void onAtendida() {
        FilaAlerta fila = tblAlertas.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }
        String pregunta = "¿Marcar como atendida la alerta de «" + fila.getInsumo() + "»?\n"
                + "Si el problema sigue, se volverá a generar en la siguiente revisión.";
        if (!Alertas.confirmar("Marcar alerta como atendida", pregunta)) {
            return;
        }
        ResultadoInventario resultado = servicio.marcarAtendida(fila.getAlerta().getIdAlerta());
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else if (ResultadoInventario.CAMPO_CONEXION.equals(resultado.getCampo())) {
            Alertas.error(resultado.getMensaje());
        } else {
            Alertas.aviso(resultado.getMensaje());
        }
        cargar();
    }
}

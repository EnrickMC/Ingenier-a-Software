package com.pac.modulos.inventario.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.service.AlertasObserver;
import com.pac.modulos.inventario.service.InventarioService;
import com.pac.modulos.inventario.service.MonitorAlertas;
import com.pac.modulos.inventario.service.PermisosInventario;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Controlador del apartado "Inventario de Insumos" (inventario.fxml). Módulo 5.
 *
 * La pantalla tiene tres pestañas, cada una con su propio controlador:
 * Insumos, Lotes y movimientos, y Alertas. Esta clase las coordina, abre los
 * formularios de compra y merma (se usan desde varias pestañas) y muestra en
 * la pestaña Alertas cuántas hay pendientes (observador de MonitorAlertas).
 */
public class InventarioController implements ControladorSeccion, AlertasObserver {

    private static final String RUTA_ENTRADA = "/com/pac/view/inventario/formulario-entrada.fxml";
    private static final String RUTA_MERMA = "/com/pac/view/inventario/formulario-merma.fxml";

    @FXML private TabPane pestanas;
    @FXML private Tab tabInsumos;
    @FXML private Tab tabMovimientos;
    @FXML private Tab tabAlertas;
    @FXML private Label lblModoConsulta;

    // JavaFX inyecta el controlador de cada <fx:include> como "<fx:id>Controller"
    @FXML private InsumosController insumosController;
    @FXML private MovimientosController movimientosController;
    @FXML private AlertasController alertasController;

    private boolean iniciado = false;

    // El Navegador llama a este método al abrir el apartado
    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        insumosController.iniciar(this);
        movimientosController.iniciar(this);
        alertasController.iniciar(this);

        // Aviso para el personal: puede registrar compras y mermas, pero no cambiar insumos
        ApoyoVista.mostrarNodo(lblModoConsulta, !PermisosInventario.puedeAdministrarInsumos());

        // RF-62: al abrir el inventario se revisan existencias y caducidades
        MonitorAlertas monitor = MonitorAlertas.getInstancia();
        monitor.agregarObservador(this);
        boolean revisado = monitor.revisar();
        if (!revisado) {
            alertasActualizadas(monitor.getPendientes());
        }

        iniciado = true;
        insumosController.alMostrar();
    }

    // Al cambiar de pestaña se recarga la que quedó visible
    @FXML
    private void onCambioPestana(Event evento) {
        if (!iniciado) {
            return;
        }
        Tab pestana = (Tab) evento.getSource();
        if (!pestana.isSelected()) {
            return;
        }
        if (pestana == tabInsumos) {
            insumosController.alMostrar();
        } else if (pestana == tabMovimientos) {
            movimientosController.alMostrar();
        } else if (pestana == tabAlertas) {
            alertasController.alMostrar();
        }
    }

    // ----- Observer de alertas: número de pendientes en el título de la pestaña -----

    @Override
    public void alertasActualizadas(int pendientes) {
        // Si la pantalla ya se cerró (se abrió otro apartado), deja de escuchar
        if (pestanas.getScene() == null && iniciado) {
            MonitorAlertas.getInstancia().quitarObservador(this);
            return;
        }
        if (pendientes > 0) {
            tabAlertas.setText("Alertas (" + pendientes + ")");
        } else {
            tabAlertas.setText("Alertas");
        }
    }

    // ===================== Saltos entre pestañas =====================

    // Desde Insumos: ver todos los movimientos de un insumo
    public void irAMovimientos(Insumo insumo) {
        movimientosController.filtrarPorInsumo(insumo.getIdInsumo());
        pestanas.getSelectionModel().select(tabMovimientos);
    }

    // ===================== Formularios compartidos =====================

    // Abre "Registrar compra / entrada" (RF-59); devuelve true si se registró
    public boolean abrirEntrada(Insumo insumo) {
        if (insumo != null) {
            ResultadoInventario revision = new InventarioService().revisarParaEntrada(insumo.getIdInsumo());
            if (!revision.isExitoso()) {
                mostrarProblema(revision);
                return revision.isRecargarLista();
            }
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_ENTRADA);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de compra");
            return false;
        }
        FormularioEntradaController formulario = cargador.getController();
        if (!formulario.preparar(insumo)) {
            return false;
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, pestanas.getScene().getWindow(), "Registrar compra / entrada");
        return formulario.seGuardo() || formulario.debeRecargarLista();
    }

    // Abre "Registrar merma" (RF-60); lote y motivo pueden ser null. Devuelve true si se registró
    public boolean abrirMerma(Insumo insumo, Lote lote, String motivo) {
        if (insumo != null) {
            ResultadoInventario revision = new InventarioService().revisarParaMerma(insumo.getIdInsumo());
            if (!revision.isExitoso()) {
                mostrarProblema(revision);
                return revision.isRecargarLista();
            }
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_MERMA);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de merma");
            return false;
        }
        FormularioMermaController formulario = cargador.getController();
        if (!formulario.preparar(insumo, lote, motivo)) {
            return false;
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, pestanas.getScene().getWindow(), "Registrar merma");
        return formulario.seGuardo() || formulario.debeRecargarLista();
    }

    private void mostrarProblema(ResultadoInventario resultado) {
        if (ResultadoInventario.CAMPO_CONEXION.equals(resultado.getCampo())) {
            Alertas.error(resultado.getMensaje());
        } else {
            Alertas.aviso(resultado.getMensaje());
        }
    }
}

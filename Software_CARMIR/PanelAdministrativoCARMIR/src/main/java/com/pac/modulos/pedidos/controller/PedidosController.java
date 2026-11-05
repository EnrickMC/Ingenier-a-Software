package com.pac.modulos.pedidos.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.FiltroPedidos;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.ApoyoTransaccion;
import com.pac.modulos.pedidos.service.MonitorPedidos;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.PedidosObserver;
import com.pac.modulos.pedidos.service.PermisosPedidos;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Controlador de "Gestión de Pedidos" (pedidos.fxml). Módulo 4.
 *
 * - Lista de pedidos con filtros por grupo, estado, texto y fechas (RF-36, RF-37).
 * - El detalle de la derecha es detalle-pedido.fxml (DetallePedidoController).
 * - Cada 60 segundos revisa si entró un pedido nuevo de la web y lo avisa (observador de MonitorPedidos).
 */
public class PedidosController implements ControladorSeccion, PedidosObserver, ContenedorDetalle {

    private static final int SEGUNDOS_ENTRE_REVISIONES = 60;

    private static final String GRUPO_ACTIVOS = "Pedidos activos";
    private static final String GRUPO_TODOS = "Historial completo";
    private static final String GRUPO_CANCELADOS = "Solo cancelados";
    private static final String GRUPO_BAJAS = "Dados de baja";

    // Folio que se debe abrir al entrar (por ejemplo, desde el dashboard)
    private static int folioPorAbrir = 0;

    @FXML private HBox cajaNuevo;
    @FXML private Label lblNuevo;
    @FXML private Label lblModoPersonal;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbGrupo;
    @FXML private BarraEstados barraEstados;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblTituloLista;
    @FXML private Label lblConteo;
    @FXML private Label lblAviso;
    @FXML private ListView<Pedido> lstPedidos;
    @FXML private Label lblVacio;

    // JavaFX inyecta el controlador del <fx:include> como "<fx:id>Controller"
    @FXML private DetallePedidoController detalleController;

    private PedidoService servicio = new PedidoService();
    private Timeline revisionPeriodica;
    private int folioNuevo = 0;
    private boolean llenandoFiltros = false;

    // Pide abrir un pedido la próxima vez que se entre a Gestión de Pedidos
    public static void abrirAlEntrar(int folio) {
        folioPorAbrir = folio;
    }

    @FXML
    private void initialize() {
        // Cada renglón de la lista se dibuja con CeldaPedido
        // (JavaFX necesita la flecha "->" para indicar cómo crear cada celda)
        lstPedidos.setCellFactory(lista -> new CeldaPedido());
        // Al pulsar un botón de estado se vuelve a consultar la lista
        barraEstados.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onBuscar();
            }
        });
    }

    // El Navegador llama a este método al abrir el apartado
    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        boolean esAdministradora = PermisosPedidos.esAdministradora();
        ApoyoVista.mostrarNodo(lblModoPersonal, !esAdministradora);
        ApoyoVista.mostrarNodo(cajaNuevo, false);
        llenarFiltros(esAdministradora);
        detalleController.iniciar(this);

        // RF-36: se refresca cada 60 segundos y se avisa si entró un pedido nuevo
        MonitorPedidos monitor = MonitorPedidos.getInstancia();
        monitor.agregarObservador(this);
        monitor.revisar();
        iniciarRevisionPeriodica();

        cargarPedidos();
        if (folioPorAbrir > 0) {
            int folio = folioPorAbrir;
            folioPorAbrir = 0;
            abrirFolio(folio);
        }
    }

    // RF-37 (Filtrar pedidos por estados): opciones del filtro (estados, «Cancelados» y «Dados de baja»).
    private void llenarFiltros(boolean esAdministradora) {
        llenandoFiltros = true;
        cmbGrupo.getItems().clear();
        cmbGrupo.getItems().add(GRUPO_ACTIVOS);
        if (esAdministradora) {
            cmbGrupo.getItems().add(GRUPO_TODOS);
            cmbGrupo.getItems().add(GRUPO_CANCELADOS);
            cmbGrupo.getItems().add(GRUPO_BAJAS);
        }
        cmbGrupo.setValue(GRUPO_ACTIVOS);
        ApoyoVista.mostrarNodo(cmbGrupo, esAdministradora);

        // Botones de estado: cada uno filtra la lista
        barraEstados.vaciar();
        for (EstadoPedido estado : EstadoPedido.values()) {
            barraEstados.agregar(estado.toString(), EstilosPedidos.textoCorto(estado), estado.getClaseCss());
        }
        llenandoFiltros = false;
    }

    private void iniciarRevisionPeriodica() {
        // (JavaFX necesita la flecha "->" para indicar qué hacer cada vez que pasa el tiempo)
        KeyFrame cadaMinuto = new KeyFrame(Duration.seconds(SEGUNDOS_ENTRE_REVISIONES), evento -> revisarPeriodicamente());
        revisionPeriodica = new Timeline(cadaMinuto);
        revisionPeriodica.setCycleCount(Animation.INDEFINITE);
        revisionPeriodica.play();
    }

    // Cada minuto: si la pantalla sigue abierta se revisan los contadores y se refresca la lista
    private void revisarPeriodicamente() {
        if (lstPedidos.getScene() == null) {
            dejarDeEscuchar();
            return;
        }
        MonitorPedidos.getInstancia().revisar();
        cargarPedidos();
    }

    private void dejarDeEscuchar() {
        revisionPeriodica.stop();
        MonitorPedidos.getInstancia().quitarObservador(this);
    }

    // ----- Observer de MonitorPedidos -----

    @Override
    // RF-33 · Paso 1: llegó una solicitud nueva; se avisa en la lista.
    public void pedidosActualizados(int porRevisar, int pagosPendientes, int folioNuevo) {
        if (lstPedidos.getScene() == null) {
            dejarDeEscuchar();
            return;
        }
        if (folioNuevo > 0) {
            this.folioNuevo = folioNuevo;
            lblNuevo.setText("Entró un pedido nuevo desde la tienda en línea (#" + folioNuevo + "). Hay "
                    + porRevisar + " solicitud(es) por revisar.");
            ApoyoVista.mostrarNodo(cajaNuevo, true);
        }
    }

    @FXML
    private void onVerNuevo() {
        ApoyoVista.mostrarNodo(cajaNuevo, false);
        if (folioNuevo > 0) {
            onLimpiarFiltros();
            abrirFolio(folioNuevo);
        }
    }

    @FXML
    private void onCerrarAvisoNuevo() {
        ApoyoVista.mostrarNodo(cajaNuevo, false);
    }

    // ===================== Lista y filtros (RF-36, RF-37) =====================

    @FXML
    // RF-36 / RF-37 · Paso 1: la persona aplica la búsqueda o el filtro.
    private void onBuscar() {
        if (llenandoFiltros) {
            return;
        }
        cargarPedidos();
    }

    @FXML
    private void onActualizar() {
        cargarPedidos();
    }

    // Deja todos los filtros en su valor inicial (ya no hay botón; lo usa el código)
    private void onLimpiarFiltros() {
        llenandoFiltros = true;
        txtBuscar.clear();
        cmbGrupo.setValue(GRUPO_ACTIVOS);
        barraEstados.limpiar();
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        llenandoFiltros = false;
        cargarPedidos();
    }

    // Arma el filtro con lo que está en pantalla
    private FiltroPedidos leerFiltro() {
        FiltroPedidos filtro = new FiltroPedidos();
        String grupo = cmbGrupo.getValue();
        if (GRUPO_TODOS.equals(grupo)) {
            filtro.setGrupo(FiltroPedidos.TODOS);
        } else if (GRUPO_CANCELADOS.equals(grupo)) {
            filtro.setGrupo(FiltroPedidos.CANCELADOS);
        } else if (GRUPO_BAJAS.equals(grupo)) {
            filtro.setGrupo(FiltroPedidos.DADOS_DE_BAJA);
        } else {
            filtro.setGrupo(FiltroPedidos.ACTIVOS);
        }
        for (EstadoPedido estado : EstadoPedido.values()) {
            if (estado.toString().equals(barraEstados.getClave())) {
                filtro.setEstado(estado);
            }
        }
        filtro.setTexto(txtBuscar.getText());
        filtro.setDesde(dpDesde.getValue());
        filtro.setHasta(dpHasta.getValue());
        return filtro;
    }

    // RF-36 (Historial de pedidos) y RF-37: inicia aquí en la pantalla la consulta de la lista.
    private void cargarPedidos() {
        FiltroPedidos filtro = leerFiltro();

        // Rango de fechas inválido: se conserva la lista anterior
        String error = servicio.validarFiltro(filtro);
        if (error != null) {
            ApoyoVista.marcar(dpDesde);
            ApoyoVista.marcar(dpHasta);
            // RF-36 · Excepción paso 2: «Rango de fechas inválido»; se mantiene la lista anterior.
            Alertas.aviso(error);
            return;
        }
        ApoyoVista.desmarcar(dpDesde);
        ApoyoVista.desmarcar(dpHasta);

        List<Pedido> pedidos;
        try {
            pedidos = servicio.buscar(filtro);
        } catch (DAOException e) {
            // Error de conexión: se conserva la lista y se avisa
            // RF-36 · Excepción paso 3: sin conexión, «Error de conexión».
            lblAviso.setText(ApoyoTransaccion.MSG_ERROR_CONEXION + ". La lista puede no estar actualizada.");
            ApoyoVista.mostrarNodo(lblAviso, true);
            if (lstPedidos.getItems().isEmpty()) {
                lblVacio.setText(ApoyoTransaccion.MSG_ERROR_CONEXION);
            }
            return;
        }

        // Se conserva el pedido elegido si sigue en la lista
        int folioAntes = 0;
        Pedido elegido = lstPedidos.getSelectionModel().getSelectedItem();
        if (elegido != null) {
            folioAntes = elegido.getFolio();
        }
        lstPedidos.getItems().setAll(pedidos);
        lblTituloLista.setText(cmbGrupo.getValue());
        lblConteo.setText(pedidos.size() + " pedido(s)");
        if (pedidos.isEmpty()) {
            lblVacio.setText(servicio.mensajeSinResultados(filtro));
        }

        // Aviso de pedidos incompletos (RF-36)
        int incompletos = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.isActivo() && !servicio.datosFaltantes(pedido).isEmpty()) {
                incompletos++;
            }
        }
        if (incompletos > 0) {
            lblAviso.setText("Hay pedidos con información incompleta (" + incompletos + "). Revísalos antes de aprobarlos.");
        }
        ApoyoVista.mostrarNodo(lblAviso, incompletos > 0);

        if (folioAntes > 0) {
            seleccionarFolio(folioAntes, false);
        }
    }

    private boolean hayFiltro() {
        boolean conTexto = txtBuscar.getText() != null && !txtBuscar.getText().isBlank();
        boolean conEstado = barraEstados.getClave() != null;
        boolean conGrupo = !GRUPO_ACTIVOS.equals(cmbGrupo.getValue());
        return conTexto || conEstado || conGrupo || dpDesde.getValue() != null || dpHasta.getValue() != null;
    }

    // ===================== Selección =====================

    @FXML
    private void onClicLista(MouseEvent evento) {
        mostrarSeleccionado();
    }

    @FXML
    private void onTeclaLista() {
        mostrarSeleccionado();
    }

    // RF-36 paso 6 / RF-33 paso 1: se selecciona un pedido y se abre su detalle.
    private void mostrarSeleccionado() {
        Pedido pedido = lstPedidos.getSelectionModel().getSelectedItem();
        if (pedido == null) {
            return;
        }
        if (detalleController.getFolio() == pedido.getFolio()) {
            return;
        }
        detalleController.mostrar(pedido.getFolio());
    }

    // Selecciona un folio en la lista; si mostrar es true también abre su detalle
    private boolean seleccionarFolio(int folio, boolean mostrar) {
        for (Pedido pedido : lstPedidos.getItems()) {
            if (pedido.getFolio() == folio) {
                lstPedidos.getSelectionModel().select(pedido);
                lstPedidos.scrollTo(pedido);
                if (mostrar) {
                    detalleController.mostrar(folio);
                }
                return true;
            }
        }
        return false;
    }

    // Abre un pedido aunque no esté en la lista filtrada
    private void abrirFolio(int folio) {
        if (!seleccionarFolio(folio, true)) {
            lstPedidos.getSelectionModel().clearSelection();
            detalleController.mostrar(folio);
        }
    }

    // ----- Avisos del detalle -----

    @Override
    public void pedidoCambiado(int folio, boolean recargarLista) {
        cargarPedidos();
        if (folio > 0 && !seleccionarFolio(folio, false)) {
            lstPedidos.getSelectionModel().clearSelection();
        }
    }
}

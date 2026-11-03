package com.pac.modulos.pedidos.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.inventario.model.NecesidadInsumo;
import com.pac.modulos.pedidos.model.AgendaPeriodo;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.AgendaService;
import com.pac.modulos.pedidos.service.ApoyoTransaccion;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.PermisosPedidos;
import com.pac.navigation.Navegador;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la "Agenda de Producción" (agenda.fxml). Módulo 4, RF-30 y RF-31.
 *
 * Vistas: día, semana, mes (por defecto) y rango de fechas, con anterior / hoy / siguiente.
 * Cada pedido es una tarjeta con el color de su estado; los atrasados van en rojo y se marcan
 * los que deben pasar a preparación pronto. Al hacer clic se abre ese pedido en Gestión de Pedidos.
 */
public class AgendaController implements ControladorSeccion {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", ESPANOL);
    private static final int TARJETAS_POR_DIA_MES = 3;
    private static final String MARCA_ATRASADO = "Atrasado";
    private static final String MARCA_PREPARAR = "Preparar pronto";

    @FXML private ToggleButton btnDia;
    @FXML private ToggleButton btnSemana;
    @FXML private ToggleButton btnMes;
    @FXML private ToggleButton btnRango;
    @FXML private HBox cajaNavegacion;
    @FXML private HBox cajaRango;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private BarraEstados barraEstados;
    @FXML private Label lblFiltro;
    @FXML private Label lblPeriodo;
    @FXML private Label lblAviso;
    @FXML private ScrollPane scrollCalendario;
    @FXML private VBox contenedorCalendario;
    @FXML private VBox cajaVacia;
    @FXML private Label lblVacio;
    @FXML private Label lblResumen;
    @FXML private VBox listaAtrasados;
    @FXML private VBox listaPorRevisar;
    @FXML private VBox listaInsumos;

    private AgendaService servicio = new AgendaService();
    private PedidoService pedidoService = new PedidoService();

    // Periodo que se está mostrando (se conserva si la siguiente consulta falla)
    private int vista = AgendaService.VISTA_MES;
    private LocalDate referencia = LocalDate.now();
    private LocalDate desde;
    private LocalDate hasta;
    private AgendaPeriodo agenda;

    // El Navegador llama a este método al abrir el apartado
    @Override
    // RF-30 (Visualización de agenda de producción): inicia aquí. Paso 1: se entra a la agenda (semana actual).
    public void inicializarSeccion(SeccionPanel seccion) {
        // La primera vista es el mes completo
        btnMes.setSelected(true);
        ApoyoVista.mostrarNodo(cajaRango, false);
        ApoyoVista.mostrarNodo(lblAviso, false);

        // Botones de estado: cada uno filtra el calendario (otra pulsación vuelve a "Todos")
        barraEstados.agregar(EstadoPedido.APROBADO.toString(), EstilosPedidos.textoCorto(EstadoPedido.APROBADO),
                EstadoPedido.APROBADO.getClaseCss());
        barraEstados.agregar(EstadoPedido.PAGADO.toString(), EstilosPedidos.textoCorto(EstadoPedido.PAGADO),
                EstadoPedido.PAGADO.getClaseCss());
        barraEstados.agregar(EstadoPedido.EN_PREPARACION.toString(), EstilosPedidos.textoCorto(EstadoPedido.EN_PREPARACION),
                EstadoPedido.EN_PREPARACION.getClaseCss());
        barraEstados.agregar(EstadoPedido.TERMINADO.toString(), EstilosPedidos.textoCorto(EstadoPedido.TERMINADO),
                EstadoPedido.TERMINADO.getClaseCss());
        barraEstados.agregar(MARCA_ATRASADO, "Atrasado", EstilosPedidos.ATRASADO);
        barraEstados.agregar(MARCA_PREPARAR, "Preparar pronto", "estado-preparar");
        barraEstados.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onFiltrar();
            }
        });
        mostrarPeriodo(AgendaService.VISTA_MES, LocalDate.now());
    }

    // ===================== Filtros =====================

    @FXML
    private void onFiltrar() {
        if (agenda != null) {
            dibujar();
        }
    }

    // true si el pedido cumple el botón de estado que está activo
    private boolean pasaFiltro(Pedido pedido) {
        String clave = barraEstados.getClave();
        if (clave == null) {
            return true;
        }
        if (MARCA_ATRASADO.equals(clave)) {
            return pedido.isAtrasado();
        }
        if (MARCA_PREPARAR.equals(clave)) {
            return AgendaService.porPreparar(pedido);
        }
        return clave.equals(pedido.getEstado().toString());
    }

    private boolean hayFiltro() {
        return barraEstados.getClave() != null;
    }

    // Pedidos de un día que cumplen los filtros
    private List<Pedido> pedidosFiltrados(LocalDate dia) {
        List<Pedido> resultado = new java.util.ArrayList<Pedido>();
        for (Pedido pedido : agenda.pedidosDelDia(dia)) {
            if (pasaFiltro(pedido)) {
                resultado.add(pedido);
            }
        }
        return resultado;
    }

    private int contarFiltrados() {
        int total = 0;
        for (Pedido pedido : agenda.getPedidos()) {
            if (pasaFiltro(pedido)) {
                total++;
            }
        }
        return total;
    }

    // ===================== Navegación =====================

    @FXML
    // RF-31 (Filtrar vistas de la agenda por periodo): inicia aquí. Paso 1: elige Día, Semana, Mes o Rango.
    private void onCambioVista() {
        int nueva = vistaElegida();
        boolean esRango = nueva == AgendaService.VISTA_RANGO;
        ApoyoVista.mostrarNodo(cajaRango, esRango);
        ApoyoVista.mostrarNodo(cajaNavegacion, !esRango);
        if (esRango) {
            // Se propone el periodo que se está viendo
            dpDesde.setValue(desde);
            dpHasta.setValue(hasta);
            vista = nueva;
            return;
        }
        mostrarPeriodo(nueva, referencia);
    }

    private int vistaElegida() {
        if (btnDia.isSelected()) {
            return AgendaService.VISTA_DIA;
        }
        if (btnMes.isSelected()) {
            return AgendaService.VISTA_MES;
        }
        if (btnRango.isSelected()) {
            return AgendaService.VISTA_RANGO;
        }
        // Si se desmarcó el botón activo se queda en semana
        btnSemana.setSelected(true);
        return AgendaService.VISTA_SEMANA;
    }

    @FXML
    private void onAnterior() {
        mostrarPeriodo(vista, AgendaService.mover(vista, referencia, -1));
    }

    @FXML
    private void onSiguiente() {
        mostrarPeriodo(vista, AgendaService.mover(vista, referencia, 1));
    }

    @FXML
    private void onHoy() {
        mostrarPeriodo(vista, LocalDate.now());
    }

    @FXML
    // RF-31 · Paso 2: aplica el rango personalizado.
    private void onAplicarRango() {
        String error = servicio.validarRango(dpDesde.getValue(), dpHasta.getValue());
        if (error != null) {
            // RF-31 · Excepción paso 3: fechas inválidas o rango demasiado amplio; no se aplica el filtro.
            Alertas.aviso(error);
            return;
        }
        cargar(AgendaService.VISTA_RANGO, dpDesde.getValue(), dpDesde.getValue(), dpHasta.getValue());
    }

    @FXML
    private void onActualizar() {
        if (desde == null) {
            mostrarPeriodo(vista, referencia);
            return;
        }
        cargar(vista, referencia, desde, hasta);
    }

    private void mostrarPeriodo(int nuevaVista, LocalDate fecha) {
        cargar(nuevaVista, fecha, AgendaService.inicioPeriodo(nuevaVista, fecha), AgendaService.finPeriodo(nuevaVista, fecha));
    }

    // Lee la agenda; si falla se conserva el periodo anterior (no se muestra información parcial)
    // RF-30 pasos 3 y 4 / RF-31 paso 4: consulta los pedidos del periodo.
    private void cargar(int nuevaVista, LocalDate nuevaReferencia, LocalDate nuevoDesde, LocalDate nuevoHasta) {
        AgendaPeriodo leida;
        try {
            leida = servicio.cargar(nuevoDesde, nuevoHasta);
        } catch (DAOException e) {
            System.out.println("Agenda: " + e.getMessage());
            if (agenda == null) {
                contenedorCalendario.getChildren().clear();
                // RF-30 · Excepción paso 3: «No fue posible cargar la agenda» con «Reintentar».
                lblVacio.setText(AgendaService.MSG_NO_CARGADA);
                ApoyoVista.mostrarNodo(cajaVacia, true);
            } else {
                // RF-31 · Excepción paso 4: sin conexión; se conserva el periodo anterior.
                Alertas.error(ApoyoTransaccion.MSG_ERROR_CONEXION + ". Se conserva el periodo anterior.");
            }
            seleccionarBoton(vista);
            return;
        }
        vista = nuevaVista;
        referencia = nuevaReferencia;
        desde = nuevoDesde;
        hasta = nuevoHasta;
        agenda = leida;
        seleccionarBoton(vista);
        dibujar();
    }

    private void seleccionarBoton(int vistaActual) {
        btnDia.setSelected(vistaActual == AgendaService.VISTA_DIA);
        btnSemana.setSelected(vistaActual == AgendaService.VISTA_SEMANA);
        btnMes.setSelected(vistaActual == AgendaService.VISTA_MES);
        btnRango.setSelected(vistaActual == AgendaService.VISTA_RANGO);
        ApoyoVista.mostrarNodo(cajaRango, vistaActual == AgendaService.VISTA_RANGO);
        ApoyoVista.mostrarNodo(cajaNavegacion, vistaActual != AgendaService.VISTA_RANGO);
    }

    // ===================== Dibujar =====================

    // RF-30 paso 7 / RF-31 paso 6: dibuja el calendario en la vista elegida.
    private void dibujar() {
        lblPeriodo.setText(textoPeriodo());
        contenedorCalendario.getChildren().clear();

        if (vista == AgendaService.VISTA_SEMANA) {
            contenedorCalendario.getChildren().add(crearSemana());
        } else if (vista == AgendaService.VISTA_MES) {
            contenedorCalendario.getChildren().add(crearMes());
        } else {
            crearLista();
        }

        // RF-30 · Excepción paso 4 / RF-31 · Excepción paso 5: «Sin pedidos programados para este periodo».
        boolean vacia = contarFiltrados() == 0;
        lblVacio.setText(AgendaService.MSG_SIN_PEDIDOS);
        if (hayFiltro()) {
            lblFiltro.setText(contarFiltrados() + " de " + agenda.getPedidos().size() + " pedido(s) con el filtro");
            if (vacia) {
                lblVacio.setText("Ningún pedido cumple el filtro de este periodo");
            }
        } else {
            lblFiltro.setText(agenda.getPedidos().size() + " pedido(s) en el periodo");
        }
        // El mensaje de "sin pedidos" ya no ocupa el calendario: se muestra junto al periodo, en la barra
        ApoyoVista.mostrarNodo(cajaVacia, false);
        if (vacia) {
            lblFiltro.setText(lblVacio.getText());
        }

        // Avisos y panel lateral
        // RF-30 · Excepción paso 5: pedidos en «Pedidos con datos por revisar».
        if (!agenda.getPorRevisar().isEmpty()) {
            lblAviso.setText(AgendaService.MSG_INFO_INCOMPLETA + " (" + agenda.getPorRevisar().size()
                    + "). Están en «Pedidos con datos por revisar».");
        }
        ApoyoVista.mostrarNodo(lblAviso, !agenda.getPorRevisar().isEmpty());
        llenarResumen();
    }

    private String textoPeriodo() {
        if (vista == AgendaService.VISTA_DIA) {
            return nombreDia(desde) + " " + Formatos.fecha(desde);
        }
        if (vista == AgendaService.VISTA_MES) {
            String mes = FORMATO_MES.format(desde);
            return mes.substring(0, 1).toUpperCase() + mes.substring(1);
        }
        return Formatos.fecha(desde) + " – " + Formatos.fecha(hasta);
    }

    // Vista semanal: 7 columnas de lunes a domingo
    private GridPane crearSemana() {
        GridPane rejilla = crearRejilla();
        LocalDate dia = desde;
        for (int columna = 0; columna < 7; columna++) {
            VBox celda = crearCeldaDia(dia, true, Integer.MAX_VALUE);
            celda.setMinHeight(380);
            rejilla.add(celda, columna, 0);
            dia = dia.plusDays(1);
        }
        return rejilla;
    }

    // Vista mensual: semanas completas (de lunes a domingo) que cubren el mes
    private VBox crearMes() {
        VBox caja = new VBox(4);
        GridPane encabezado = crearRejilla();
        LocalDate lunes = AgendaService.inicioPeriodo(AgendaService.VISTA_SEMANA, desde);
        for (int columna = 0; columna < 7; columna++) {
            Label nombre = new Label(nombreDia(lunes.plusDays(columna)));
            nombre.getStyleClass().add("agenda-encabezado");
            encabezado.add(nombre, columna, 0);
        }
        caja.getChildren().add(encabezado);

        GridPane rejilla = crearRejilla();
        LocalDate dia = lunes;
        int fila = 0;
        while (!dia.isAfter(hasta)) {
            for (int columna = 0; columna < 7; columna++) {
                VBox celda = crearCeldaDia(dia, false, TARJETAS_POR_DIA_MES);
                celda.setMinHeight(110);
                if (dia.getMonthValue() != desde.getMonthValue()) {
                    celda.getStyleClass().add("agenda-fuera-mes");
                }
                rejilla.add(celda, columna, fila);
                dia = dia.plusDays(1);
            }
            fila++;
        }
        caja.getChildren().add(rejilla);
        return caja;
    }

    // Vista de día o de rango: lista de tarjetas agrupadas por fecha
    private void crearLista() {
        LocalDate dia = desde;
        while (!dia.isAfter(hasta)) {
            List<Pedido> delDia = pedidosFiltrados(dia);
            if (!delDia.isEmpty() || vista == AgendaService.VISTA_DIA) {
                Label fecha = new Label(nombreDia(dia) + " " + Formatos.fecha(dia) + " · " + delDia.size() + " pedido(s)");
                fecha.getStyleClass().add("agenda-fecha");
                contenedorCalendario.getChildren().add(fecha);
                FlowPane tarjetas = new FlowPane(8, 8);
                for (Pedido pedido : delDia) {
                    VBox tarjeta = crearTarjeta(pedido, true);
                    tarjeta.setPrefWidth(260);
                    tarjetas.getChildren().add(tarjeta);
                }
                contenedorCalendario.getChildren().add(tarjetas);
            }
            dia = dia.plusDays(1);
        }
    }

    private GridPane crearRejilla() {
        GridPane rejilla = new GridPane();
        rejilla.setHgap(6);
        rejilla.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(100.0 / 7);
            rejilla.getColumnConstraints().add(columna);
        }
        return rejilla;
    }

    // Celda de un día con sus tarjetas (en el mes solo se ven algunas y "+n más")
    private VBox crearCeldaDia(LocalDate dia, boolean conNombre, int maximo) {
        VBox celda = new VBox(4);
        celda.getStyleClass().add("agenda-dia");
        if (dia.equals(LocalDate.now())) {
            celda.getStyleClass().add("agenda-hoy");
        }
        String titulo = String.valueOf(dia.getDayOfMonth());
        if (conNombre) {
            titulo = nombreDia(dia) + " " + dia.getDayOfMonth();
        }
        Label fecha = new Label(titulo);
        fecha.getStyleClass().add("agenda-fecha");
        celda.getChildren().add(fecha);

        List<Pedido> delDia = pedidosFiltrados(dia);
        int mostrados = 0;
        for (Pedido pedido : delDia) {
            if (mostrados >= maximo) {
                break;
            }
            celda.getChildren().add(crearTarjeta(pedido, conNombre));
            mostrados++;
        }
        if (delDia.size() > mostrados) {
            Button mas = new Button("+" + (delDia.size() - mostrados) + " más");
            mas.getStyleClass().add("enlace");
            // Flecha necesaria: JavaFX pide una acción para el clic del botón
            mas.setOnAction(evento -> verDia(dia));
            celda.getChildren().add(mas);
        }
        return celda;
    }

    // Abre la vista diaria de una fecha
    private void verDia(LocalDate dia) {
        mostrarPeriodo(AgendaService.VISTA_DIA, dia);
    }

    // Tarjeta de un pedido: hora, folio, cliente, postres y marcas
    private VBox crearTarjeta(Pedido pedido, boolean completa) {
        VBox tarjeta = new VBox(2);
        tarjeta.getStyleClass().add("agenda-tarjeta");
        // RF-30 · Paso 6: color por estado del pedido.
        EstilosPedidos.aplicarClase(tarjeta, pedido.getEstado().getClaseCss());
        tarjeta.getStyleClass().remove(EstilosPedidos.ESTILO_BASE);

        String hora = "";
        if (pedido.getHorarioEntrega() != null) {
            hora = pedido.getHorarioEntrega().toString() + " · ";
        }
        Label titulo = new Label(hora + "#" + pedido.getFolio() + " " + pedido.getCliente().getNombre());
        titulo.setStyle("-fx-font-weight: bold;");
        titulo.setWrapText(true);
        tarjeta.getChildren().add(titulo);
        if (completa) {
            Label postres = new Label(ApoyoVista.texto(pedido.getResumenProductos()));
            postres.setWrapText(true);
            tarjeta.getChildren().add(postres);
            Label estado = new Label(pedido.getEstado().toString());
            tarjeta.getChildren().add(estado);
        }

        FlowPane marcas = new FlowPane(4, 4);
        // RF-30 · Excepción paso 6: entrega vencida, se resalta en rojo como «Atrasado».
        if (pedido.isAtrasado()) {
            tarjeta.getStyleClass().add("tarjeta-atrasada");
            marcas.getChildren().add(EstilosPedidos.crearEtiqueta("Atrasado", EstilosPedidos.ATRASADO));
        }
        if (AgendaService.pendienteDePago(pedido)) {
            marcas.getChildren().add(EstilosPedidos.crearEtiqueta("Pago pendiente", "estado-pendiente-pago"));
        }
        if (AgendaService.porPreparar(pedido)) {
            marcas.getChildren().add(EstilosPedidos.crearEtiqueta("Preparar pronto", "estado-preparar"));
        }
        if (!marcas.getChildren().isEmpty()) {
            tarjeta.getChildren().add(marcas);
        }
        Tooltip.install(tarjeta, new Tooltip("Clic para abrir el pedido #" + pedido.getFolio()));
        // Flecha necesaria: JavaFX pide una acción para el clic del ratón
        tarjeta.setOnMouseClicked(evento -> abrirPedido(pedido.getFolio()));
        return tarjeta;
    }

    // Resumen: número de pedidos, atrasados, datos por revisar e insumos estimados
    // RF-30 · Paso 8: resumen del pedido seleccionado.
    private void llenarResumen() {
        lblResumen.setText(agenda.getPedidos().size() + " pedido(s) programado(s)\n"
                + agenda.contarPendientesDePago() + " pendiente(s) de pago\n"
                + agenda.getAtrasados().size() + " atrasado(s) en total");

        listaAtrasados.getChildren().clear();
        for (Pedido pedido : agenda.getAtrasados()) {
            listaAtrasados.getChildren().add(crearEnlace(pedido, "#" + pedido.getFolio() + " " + pedido.getCliente().getNombre()
                    + " · " + Formatos.fecha(pedido.getFechaEntrega()) + " · " + pedido.getEstado()));
        }
        if (agenda.getAtrasados().isEmpty()) {
            agregarTexto(listaAtrasados, "Sin pedidos atrasados", "texto-suave");
        }

        listaPorRevisar.getChildren().clear();
        for (Pedido pedido : agenda.getPorRevisar()) {
            String faltan = String.join(", ", pedidoService.datosFaltantes(pedido));
            listaPorRevisar.getChildren().add(crearEnlace(pedido, "#" + pedido.getFolio() + " "
                    + pedido.getCliente().getNombre() + " · falta " + faltan));
        }
        if (agenda.getPorRevisar().isEmpty()) {
            agregarTexto(listaPorRevisar, "Todos los pedidos tienen sus datos completos", "texto-suave");
        }

        listaInsumos.getChildren().clear();
        for (NecesidadInsumo insumo : agenda.getInsumos()) {
            String unidad = insumo.getUnidad().getValorBD();
            String texto = insumo.getNombreInsumo() + ": " + Formatos.cantidad(insumo.getCantidadNecesaria()) + " " + unidad
                    + " (hay " + Formatos.cantidad(insumo.getExistencia()) + ")";
            String clase = "insumo-alcanza";
            if (insumo.falta()) {
                texto = texto + " · faltan " + Formatos.cantidad(insumo.getFaltante()) + " " + unidad;
                clase = "insumo-falta";
            }
            agregarTexto(listaInsumos, texto, clase);
        }
        if (agenda.getInsumos().isEmpty()) {
            agregarTexto(listaInsumos, "Sin insumos por estimar", "texto-suave");
        }
    }

    private Node crearEnlace(Pedido pedido, String texto) {
        Button enlace = new Button(texto);
        enlace.getStyleClass().add("enlace");
        enlace.setWrapText(true);
        enlace.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(enlace, Priority.ALWAYS);
        // Flecha necesaria: JavaFX pide una acción para el clic del botón
        enlace.setOnAction(evento -> abrirPedido(pedido.getFolio()));
        return enlace;
    }

    private void agregarTexto(VBox lista, String texto, String clase) {
        Label linea = new Label(texto);
        linea.setWrapText(true);
        linea.getStyleClass().add(clase);
        lista.getChildren().add(linea);
    }

    // Abre el detalle; si el pedido ya no existe o fue cancelado se avisa y se actualiza la agenda
    private void abrirPedido(int folio) {
        try {
            if (!pedidoService.sigueDisponible(folio)) {
                // RF-30 · Excepción paso 8: «El pedido ya no está disponible» y se actualiza el calendario.
                Alertas.aviso(PedidoService.MSG_YA_NO_DISPONIBLE);
                onActualizar();
                return;
            }
        } catch (DAOException e) {
            Alertas.error(ApoyoTransaccion.MSG_ERROR_CONEXION);
            return;
        }
        // Quien puede gestionar pedidos va directo a ese pedido en "Gestión de Pedidos"
        if (PermisosPedidos.puedeGestionar()) {
            PedidosController.abrirAlEntrar(folio);
            Navegador.getInstancia().irASeccion(SeccionPanel.PEDIDOS);
            return;
        }
        // Si no tiene ese permiso, se abre el detalle en una ventana
        boolean cambio = DetallePedidoController.abrirEnVentana(folio, scrollCalendario.getScene().getWindow());
        if (cambio) {
            onActualizar();
        }
    }

    private String nombreDia(LocalDate dia) {
        String nombre = dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ESPANOL);
        nombre = nombre.replace(".", "");
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }
}

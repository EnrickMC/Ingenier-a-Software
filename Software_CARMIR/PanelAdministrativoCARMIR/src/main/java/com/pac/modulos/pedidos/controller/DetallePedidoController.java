package com.pac.modulos.pedidos.controller;

import com.pac.CarmirApp;
import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.pedidos.model.CambioEstado;
import com.pac.modulos.pedidos.model.EstadoPago;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.modulos.pedidos.model.ModalidadEntrega;
import com.pac.modulos.pedidos.model.Modificacion;
import com.pac.modulos.pedidos.model.Notificacion;
import com.pac.modulos.pedidos.model.Pago;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.notificacion.NotificacionService;
import com.pac.modulos.pedidos.service.ApoyoTransaccion;
import com.pac.modulos.pedidos.service.EstadoPedidoService;
import com.pac.modulos.pedidos.service.ModificacionPedidoService;
import com.pac.modulos.pedidos.service.PagoService;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.PermisosPedidos;
import com.pac.modulos.pedidos.service.ResultadoPedido;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import com.pac.view.animacion.IconoLottie;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalle de un pedido (detalle-pedido.fxml).
 *
 * Muestra cliente, postres con su personalización e imágenes, entrega, desglose de pagos (RF-43),
 * historial de estados, modificaciones y notificaciones. Tiene los botones de las acciones:
 * Aprobar (RF-38), Avanzar estado (RF-40, RF-41, RF-44), Registrar pago (RF-42), Modificar,
 * Cancelar (RF-39), Dar de baja e Imprimir ticket.
 */
public class DetallePedidoController {

    private static final String RUTA_DETALLE = "/com/pac/view/pedidos/detalle-pedido.fxml";
    private static final String RUTA_APROBACION = "/com/pac/view/pedidos/formulario-aprobacion.fxml";
    private static final String RUTA_MOTIVO = "/com/pac/view/pedidos/formulario-motivo.fxml";
    private static final String RUTA_PAGO = "/com/pac/view/pedidos/formulario-pago.fxml";
    private static final String RUTA_MODIFICACION = "/com/pac/view/pedidos/formulario-modificacion.fxml";

    @FXML private VBox cajaSinSeleccion;
    @FXML private StackPane decoracionPastel;
    @FXML private Label lblSinSeleccion;
    @FXML private VBox cajaContenido;
    @FXML private Label lblFolio;
    @FXML private HBox cajaEtiquetas;
    @FXML private Label lblEstado;
    @FXML private Label lblFechas;
    @FXML private Label lblBanner;
    @FXML private HBox cajaPasos;
    @FXML private VBox listaAvisos;
    @FXML private Label lblCliente;
    @FXML private VBox listaPostres;
    @FXML private Label lblEntrega;
    @FXML private Label lblDesglose;
    @FXML private ProgressBar barraPagado;
    @FXML private Label lblSituacion;
    @FXML private Label lblTotal;
    @FXML private VBox listaPagos;
    @FXML private VBox listaHistorial;
    @FXML private Label lblTituloModificaciones;
    @FXML private VBox listaModificaciones;
    @FXML private VBox listaNotificaciones;
    @FXML private Label lblBloqueo;
    @FXML private Button btnAprobar;
    @FXML private Button btnAvanzar;
    @FXML private Button btnPago;
    @FXML private Button btnModificar;
    @FXML private Button btnCancelar;
    @FXML private Button btnBaja;
    @FXML private Button btnImprimir;

    private PedidoService pedidoService = new PedidoService();
    private EstadoPedidoService estadoService = new EstadoPedidoService();
    private PagoService pagoService = new PagoService();
    private ModificacionPedidoService modificacionService = new ModificacionPedidoService();

    // Pastel animado del panel vacío
    private static final double TAMANO_PASTEL = 456;
    private static final double CUADRO_PASTEL_COMPLETO = 136;
    private static final double SEGUNDOS_PAUSA_PASTEL = 5;
    private IconoLottie pastel;

    private ContenedorDetalle contenedor;   // null cuando el detalle se abre en una ventana
    private Pedido pedido;
    private int folioPedido = 0;
    private boolean huboCambios = false;

    @FXML
    private void initialize() {
        // Pastel animado de adorno (Lottie4J): se arma, se queda completo 5 segundos y se vuelve a armar
        pastel = new IconoLottie("pastel_animado.json", TAMANO_PASTEL, CUADRO_PASTEL_COMPLETO, "bi-basket-fill");
        decoracionPastel.getChildren().add(pastel);

        mostrarVacio("Selecciona un pedido de la lista para ver su detalle", false);
    }

    public void iniciar(ContenedorDetalle contenedor) {
        this.contenedor = contenedor;
    }

    public int getFolio() {
        return folioPedido;
    }

    // true si en la ventana se cambió algo del pedido (para que quien la abrió recargue)
    public boolean huboCambios() {
        return huboCambios;
    }

    /**
     * Abre el detalle de un pedido en una ventana aparte (desde la Agenda o la Validación de pagos).
     * Devuelve true si se cambió algo del pedido.
     */
    public static boolean abrirEnVentana(int folio, Window duena) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_DETALLE);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el detalle del pedido");
            return false;
        }
        DetallePedidoController detalle = cargador.getController();
        if (!detalle.mostrar(folio)) {
            return false;
        }
        Parent raiz = cargador.getRoot();
        raiz.setStyle("-fx-pref-width: 820px; -fx-pref-height: 720px;");
        ApoyoVista.mostrarModal(raiz, duena, "Pedido #" + folio);
        return detalle.huboCambios();
    }

    // ===================== Mostrar =====================

    /**
     * Lee el pedido y lo muestra. Devuelve false si no se pudo
     * (no existe o error de conexión; ya se avisó al usuario).
     */
    // RF-36 paso 6 y RF-33 (Visualización de pedidos) pasos 2 a 6: muestra la ficha del pedido.
    public boolean mostrar(int folio) {
        folioPedido = folio;
        ResultadoPedido lectura = pedidoService.cargarDetalle(folio);
        if (!lectura.isExitoso()) {
            pedido = null;
            if (lectura.esErrorConexion()) {
                mostrarVacio(lectura.getMensaje(), true);
                if (contenedor == null) {
                    // RF-33 · Excepción paso 2: sin conexión, «Error de conexión».
                    Alertas.error(lectura.getMensaje());
                }
            } else {
                mostrarVacio("Selecciona un pedido de la lista para ver su detalle", false);
                // RF-33 · Excepción paso 2: «Pedido no encontrado» o ya atendido; se recarga la lista.
                Alertas.aviso(lectura.getMensaje());
                avisarCambio(true);
            }
            return false;
        }
        pedido = lectura.getPedido();
        pastel.detenerCiclo();
        ApoyoVista.mostrarNodo(cajaSinSeleccion, false);
        ApoyoVista.mostrarNodo(cajaContenido, true);
        llenarEncabezado();
        llenarAvisos();
        llenarCliente();
        llenarPostres();
        llenarEntregaYPago();
        llenarPagos();
        llenarHistorial();
        llenarNotificaciones();
        configurarBotones();
        return true;
    }

    private void mostrarVacio(String mensaje, boolean conReintentar) {
        pedido = null;
        lblSinSeleccion.setText(mensaje);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, true);
        ApoyoVista.mostrarNodo(cajaContenido, false);
        pastel.iniciarCiclo(SEGUNDOS_PAUSA_PASTEL, CUADRO_PASTEL_COMPLETO);
    }

    @FXML
    private void onReintentar() {
        if (folioPedido > 0) {
            mostrar(folioPedido);
        }
    }

    // Vuelve a leer el pedido y avisa a la lista
    private void recargar(boolean recargarLista) {
        huboCambios = true;
        mostrar(folioPedido);
        avisarCambio(recargarLista);
    }

    private void avisarCambio(boolean recargarLista) {
        if (recargarLista) {
            huboCambios = true;
        }
        if (contenedor != null) {
            contenedor.pedidoCambiado(folioPedido, recargarLista);
        }
    }

    private void llenarEncabezado() {
        lblFolio.setText("Pedido #" + pedido.getFolio());
        EstilosPedidos.aplicarEstado(lblEstado, pedido);
        cajaEtiquetas.getChildren().setAll(lblEstado);
        if (pedido.isActivo() && pedido.isAtrasado()) {
            cajaEtiquetas.getChildren().add(EstilosPedidos.crearEtiqueta("Atrasado", EstilosPedidos.ATRASADO));
        }
        if (pedido.getPagosPendientes() > 0 && pedido.isActivo()) {
            cajaEtiquetas.getChildren().add(EstilosPedidos.crearEtiqueta(pedido.getPagosPendientes()
                    + " pago(s) por validar", "estado-pendiente-pago"));
        }

        String horario = "";
        if (pedido.getHorarioEntrega() != null) {
            horario = " a las " + pedido.getHorarioEntrega().toString();
        }
        lblFechas.setText("Ingresado: " + Formatos.fechaHoraCorta(pedido.getFechaSolicitud())
                + "\nEntrega: " + Formatos.fecha(pedido.getFechaEntrega()) + horario);

        // Cancelado o dado de baja: motivo y fecha
        String banner = null;
        if (pedido.isDadoDeBaja()) {
            banner = "Esta solicitud fue dada de baja el " + Formatos.fechaHoraCorta(pedido.getBaja().getFecha())
                    + " por " + pedido.getBaja().getNombreUsuario() + ".\nMotivo: " + pedido.getBaja().getMotivo();
        } else if (pedido.isCancelado()) {
            banner = "Pedido cancelado el " + Formatos.fechaHoraCorta(pedido.getCancelacion().getFecha())
                    + " por " + pedido.getCancelacion().getNombreUsuario() + ".\nMotivo: "
                    + pedido.getCancelacion().getMotivo() + "\nReembolso: "
                    + Formatos.moneda(pedido.getCancelacion().getMontoReembolso()) + " ("
                    + pedido.getCancelacion().getEstadoReembolso() + ")";
        }
        if (banner != null) {
            lblBanner.setText(banner);
        }
        ApoyoVista.mostrarNodo(lblBanner, banner != null);

        // Barra de los 6 estados; si está cancelado se detiene en el último
        cajaPasos.getChildren().clear();
        boolean detenido = pedido.isCancelado() || pedido.isDadoDeBaja();
        for (EstadoPedido estado : EstadoPedido.values()) {
            Label paso = new Label(estado.toString());
            paso.setWrapText(true);
            paso.setMaxWidth(Double.MAX_VALUE);
            paso.setMaxHeight(Double.MAX_VALUE);
            HBox.setHgrow(paso, Priority.ALWAYS);
            paso.getStyleClass().add("paso-estado");
            if (estado == pedido.getEstado()) {
                if (detenido) {
                    paso.getStyleClass().add("paso-detenido");
                } else {
                    paso.getStyleClass().add("paso-actual");
                }
            } else if (estado.esAnteriorA(pedido.getEstado())) {
                paso.getStyleClass().add("paso-hecho");
            }
            cajaPasos.getChildren().add(paso);
        }
    }

    // RF-33: avisos de la ficha
    // RF-33 · Excepciones pasos 4 y 5: avisos de la ficha (datos incompletos, fecha vencida, agenda saturada).
    private void llenarAvisos() {
        listaAvisos.getChildren().clear();
        List<String> avisos;
        try {
            avisos = pedidoService.avisosDeRevision(pedido);
        } catch (DAOException e) {
            avisos = new ArrayList<String>();
        }
        for (String aviso : avisos) {
            Label linea = new Label(aviso);
            linea.setWrapText(true);
            linea.setMaxWidth(Double.MAX_VALUE);
            linea.getStyleClass().add("aviso-amarillo");
            listaAvisos.getChildren().add(linea);
        }
        ApoyoVista.mostrarNodo(listaAvisos, !avisos.isEmpty());
    }

    private void llenarCliente() {
        String texto = "Nombre: " + pedido.getCliente().getNombre()
                + "\nTeléfono (WhatsApp): " + ApoyoVista.texto(pedido.getCliente().getTelefono());
        if (pedido.getCliente().getCorreo() != null && !pedido.getCliente().getCorreo().isBlank()) {
            texto = texto + "\nCorreo: " + pedido.getCliente().getCorreo();
        }
        lblCliente.setText(texto);
    }

    // Postres con su personalización, dedicatoria, comentario e imágenes de referencia
    private void llenarPostres() {
        listaPostres.getChildren().clear();
        if (pedido.getLineas().isEmpty()) {
            agregarLinea(listaPostres, "El pedido no tiene postres registrados", "aviso-rojo");
            return;
        }
        for (LineaPedido linea : pedido.getLineas()) {
            VBox caja = new VBox(3);
            caja.getStyleClass().add("linea-postre");

            Label nombre = new Label(linea.getCantidad() + " × " + linea.getNombreMostrar());
            nombre.getStyleClass().add("postre-nombre");
            nombre.setWrapText(true);
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            Label subtotal = new Label(Formatos.moneda(linea.getSubtotal()));
            subtotal.getStyleClass().add("precio");
            Label unitario = new Label("(" + Formatos.moneda(linea.getPrecioUnitario()) + " c/u)");
            unitario.getStyleClass().add("texto-ayuda");
            HBox arriba = new HBox(8, nombre, espacio, unitario, subtotal);
            arriba.setAlignment(Pos.CENTER_LEFT);
            caja.getChildren().add(arriba);

            if (!linea.getEspecificaciones().isEmpty()) {
                agregarLinea(caja, "- " + String.join("\n- ", linea.getEspecificaciones()), "especificaciones");
            }
            if (linea.getDedicatoria() != null && !linea.getDedicatoria().isBlank()) {
                agregarLinea(caja, "Dedicatoria: \"" + linea.getDedicatoria() + "\"", "dedicatoria");
            }
            if (linea.getComentario() != null && !linea.getComentario().isBlank()) {
                agregarLinea(caja, "Comentario: " + linea.getComentario(), "especificaciones");
            }

            listaPostres.getChildren().add(caja);
        }
    }

    private void llenarEntregaYPago() {
        String entrega = "Método: " + pedido.getModalidadEntrega();
        if (pedido.getModalidadEntrega() == ModalidadEntrega.DOMICILIO) {
            if (pedido.getDireccion() != null) {
                entrega = entrega + "\nDirección: " + pedido.getDireccion().getTexto();
                String referencias = pedido.getDireccion().getReferencias();
                if (referencias != null && !referencias.isBlank()) {
                    entrega = entrega + "\nReferencias: " + referencias;
                }
            } else {
                entrega = entrega + "\nDirección: (falta registrarla)";
            }
        }
        String hora = "—";
        if (pedido.getHorarioEntrega() != null) {
            hora = pedido.getHorarioEntrega().toString();
        }
        entrega = entrega + "\nFecha acordada: " + Formatos.fecha(pedido.getFechaEntrega()) + "\nHora: " + hora;
        lblEntrega.setText(entrega);

        // RF-43: desglose
        if (pedido.getEstado() == EstadoPedido.EN_ACEPTACION && !pedido.isCancelado()) {
            lblDesglose.setText("Modalidad: " + pedido.getModalidadPago());
            ApoyoVista.mostrarNodo(barraPagado, false);
        } else {
            lblDesglose.setText("Modalidad: " + pedido.getModalidadPago()
                    + "\nAnticipo requerido: " + Formatos.moneda(pedido.getMontoParaIniciar())
                    + "\nPagado: " + Formatos.moneda(pedido.getTotalPagado()) + " (" + pedido.getPorcentajePagado() + " %)"
                    + "\nSaldo pendiente: " + Formatos.moneda(pedido.getSaldo()));
            barraPagado.setProgress(pedido.getPorcentajePagado() / 100.0);
            ApoyoVista.mostrarNodo(barraPagado, true);
        }
        lblSituacion.setText(PagoService.situacion(pedido));
        lblSituacion.getStyleClass().removeAll("saldo-pendiente", "saldo-cero");
        if (pedido.getSaldo() <= 0.004 && pedido.getSaldo() >= -0.004) {
            lblSituacion.getStyleClass().add("saldo-cero");
        } else {
            lblSituacion.getStyleClass().add("saldo-pendiente");
        }
        // Pedidos de la página web: el envío viene incluido en el total
        String total = "Total: " + Formatos.moneda(pedido.getMontoTotal()) + " MXN";
        if (pedido.getCostoEnvio() > 0) {
            total = total + " (incluye envío " + Formatos.moneda(pedido.getCostoEnvio()) + ")";
        }
        lblTotal.setText(total);
    }

    // Pagos con su estado, comprobante y, si están pendientes, el botón para revisarlos
    // RF-43 (Seguimiento de saldos y pagos de pedidos) · Paso 5: desglose de pagos, anticipo, lo pagado y el saldo.
    private void llenarPagos() {
        listaPagos.getChildren().clear();
        if (pedido.getPagos().isEmpty()) {
            agregarLinea(listaPagos, "Sin pagos registrados", "texto-suave");
            return;
        }
        for (Pago pago : pedido.getPagos()) {
            String texto = Formatos.moneda(pago.getMonto()) + " · " + Formatos.fechaHoraCorta(pago.getFechaPago())
                    + " · " + pago.getMetodo();
            if (pago.getReferencia() != null) {
                texto = texto + " · Ref. " + pago.getReferencia();
            }
            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                texto = texto + " · Revisó: " + pago.getNombreUsuario();
            }
            if (pago.getEstado() == EstadoPago.RECHAZADO && pago.getMotivoRechazo() != null) {
                texto = texto + "\nMotivo del rechazo: " + pago.getMotivoRechazo();
            }
            Label estado = EstilosPedidos.crearEtiqueta(pago.getEstado().toString(), claseEstadoPago(pago.getEstado()));
            estado.setMinWidth(Region.USE_PREF_SIZE);
            Label datos = new Label(texto);
            datos.setWrapText(true);
            datos.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(datos, Priority.ALWAYS);
            HBox fila = new HBox(8, estado, datos);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("linea-bitacora");

            if (pago.tieneComprobante()) {
                // RF-50 · Paso 1: el comprobante (foto, PDF o enlace) se ve en su propia ventana
                Button comprobante = new Button("Ver comprobante");
                comprobante.getStyleClass().add("enlace");
                // Flecha necesaria: JavaFX pide una acción para el clic del botón
                comprobante.setOnAction(evento -> VisorComprobante.abrirVentana(pago, listaPagos.getScene().getWindow()));
                fila.getChildren().add(comprobante);
            }
            if (pago.getEstado() == EstadoPago.PENDIENTE && PermisosPedidos.puedeValidarPagos()) {
                Button revisar = new Button("Validar / rechazar");
                revisar.getStyleClass().add("btn-azul");
                // Flecha necesaria: JavaFX pide una acción para el clic del botón
                revisar.setOnAction(evento -> abrirValidacion(pago));
                fila.getChildren().add(revisar);
            }
            listaPagos.getChildren().add(fila);
        }
    }

    public static String claseEstadoPago(EstadoPago estado) {
        if (estado == EstadoPago.VALIDADO) {
            return "estado-terminado";
        }
        if (estado == EstadoPago.RECHAZADO) {
            return EstilosPedidos.CANCELADO;
        }
        return "estado-pendiente-pago";
    }

    public static void abrirComprobante(String enlace) {
        if (!CarmirApp.abrirEnlace(enlace)) {
            Alertas.aviso("No se pudo abrir el comprobante. Enlace: " + enlace);
        }
    }

    private void llenarHistorial() {
        listaHistorial.getChildren().clear();
        for (CambioEstado cambio : pedido.getHistorial()) {
            String anterior = "Registro";
            EstadoPedido estadoAnterior = EstadoPedido.desdeBD(cambio.getEstadoAnterior());
            if (estadoAnterior != null) {
                anterior = estadoAnterior.toString();
            }
            String nuevo = cambio.getEstadoNuevo();
            EstadoPedido estadoNuevo = EstadoPedido.desdeBD(cambio.getEstadoNuevo());
            if (estadoNuevo != null) {
                nuevo = estadoNuevo.toString();
            }
            agregarLinea(listaHistorial, Formatos.fechaHoraCorta(cambio.getFecha()) + " · " + anterior + " → " + nuevo
                    + " · " + cambio.getNombreUsuario(), "linea-bitacora");
        }
        if (pedido.getHistorial().isEmpty()) {
            agregarLinea(listaHistorial, "Sin cambios registrados", "texto-suave");
        }

        // Modificaciones: solo la Administradora
        boolean verModificaciones = PermisosPedidos.esAdministradora();
        ApoyoVista.mostrarNodo(lblTituloModificaciones, verModificaciones);
        ApoyoVista.mostrarNodo(listaModificaciones, verModificaciones);
        listaModificaciones.getChildren().clear();
        if (!verModificaciones) {
            return;
        }
        for (Modificacion modificacion : pedido.getModificaciones()) {
            agregarLinea(listaModificaciones, Formatos.fechaHoraCorta(modificacion.getFecha()) + " · "
                    + modificacion.getNombreUsuario() + "\nAntes:\n" + modificacion.getDatosAnteriores()
                    + "\nDespués:\n" + modificacion.getDatosNuevos(), "linea-bitacora");
        }
        if (pedido.getModificaciones().isEmpty()) {
            agregarLinea(listaModificaciones, "Sin modificaciones", "texto-suave");
        }
    }

    // RF-47: avisos enviados; los fallidos se pueden reenviar
    // RF-47 · Paso 7: bitácora de avisos del pedido (con «Reenviar» si falló).
    private void llenarNotificaciones() {
        listaNotificaciones.getChildren().clear();
        if (pedido.getNotificaciones().isEmpty()) {
            agregarLinea(listaNotificaciones, "Sin notificaciones", "texto-suave");
            return;
        }
        for (Notificacion aviso : pedido.getNotificaciones()) {
            String texto = Formatos.fechaHoraCorta(aviso.getFecha()) + " · " + aviso.getTipoEvento() + " · "
                    + aviso.getCanal() + " → " + aviso.getDestino() + " · " + aviso.getResultado();
            if (aviso.getMotivoFallo() != null && !aviso.getMotivoFallo().isBlank()) {
                texto = texto + "\n" + aviso.getMotivoFallo();
            }
            Label datos = new Label(texto);
            datos.setWrapText(true);
            datos.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(datos, Priority.ALWAYS);
            HBox fila = new HBox(8, datos);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("linea-bitacora");
            if (aviso.isFallida()) {
                fila.getStyleClass().add("linea-fallida");
                if (PermisosPedidos.puedeGestionar()) {
                    Button reenviar = new Button("Reenviar");
                    reenviar.getStyleClass().add("btn-naranja");
                    // Flecha necesaria: JavaFX pide una acción para el clic del botón
                    reenviar.setOnAction(evento -> reenviar(aviso));
                    fila.getChildren().add(reenviar);
                }
            }
            listaNotificaciones.getChildren().add(fila);
        }
    }

    // RF-47 · Excepción paso 6: «Reenviar» un aviso «Fallido».
    private void reenviar(Notificacion aviso) {
        if (NotificacionService.getInstancia().reenviar(aviso.getIdNotificacion())) {
            Alertas.info("Se volverá a enviar el aviso en segundo plano. Actualiza el detalle en unos segundos.");
        } else {
            Alertas.error("No se pudo reenviar el aviso");
        }
        mostrar(folioPedido);
    }

    private void agregarLinea(VBox lista, String texto, String claseEstilo) {
        Label linea = new Label(texto);
        linea.setWrapText(true);
        linea.setMaxWidth(Double.MAX_VALUE);
        linea.getStyleClass().add(claseEstilo);
        lista.getChildren().add(linea);
    }

    // ===================== Botones según el pedido y los permisos =====================

    // RF-33 · Paso 6: botones «Aprobar», «Modificar», «Cancelar», «Dar de baja» e «Imprimir ticket».
    private void configurarBotones() {
        boolean gestionar = PermisosPedidos.puedeGestionar();
        boolean administradora = PermisosPedidos.esAdministradora();
        boolean activo = pedido.isActivo();

        // Aprobar: solo «En proceso de aceptación»; se deshabilita con el motivo si no se puede
        boolean verAprobar = gestionar && activo && pedido.getEstado() == EstadoPedido.EN_ACEPTACION;
        ApoyoVista.mostrarNodo(btnAprobar, verAprobar);
        String motivoAprobar = null;
        if (verAprobar) {
            try {
                motivoAprobar = pedidoService.motivoNoAprobable(pedido);
            } catch (DAOException e) {
                motivoAprobar = null;
            }
        }
        btnAprobar.setDisable(motivoAprobar != null);
        ponerAyuda(btnAprobar, motivoAprobar);

        // Avanzar estado: muestra el siguiente estado y por qué está bloqueado (RF-44)
        EstadoPedido siguiente = pedido.getEstado().siguiente();
        boolean verAvanzar = gestionar && activo && pedido.getEstado() != EstadoPedido.EN_ACEPTACION && siguiente != null;
        ApoyoVista.mostrarNodo(btnAvanzar, verAvanzar);
        String bloqueo = null;
        if (verAvanzar) {
            btnAvanzar.setText("Pasar a «" + siguiente + "»");
            ResultadoPedido revision = estadoService.revisarAvance(pedido);
            if (revision != null) {
                bloqueo = revision.getMensaje();
            }
        }
        if (bloqueo != null) {
            lblBloqueo.setText(bloqueo);
        } else if (motivoAprobar != null) {
            lblBloqueo.setText("No se puede aprobar: " + motivoAprobar);
        }
        ApoyoVista.mostrarNodo(lblBloqueo, bloqueo != null || motivoAprobar != null);

        // Registrar pago
        ApoyoVista.mostrarNodo(btnPago, PermisosPedidos.puedeValidarPagos() && pagoService.revisarRegistro(pedido) == null);

        // Modificar, cancelar y dar de baja
        ApoyoVista.mostrarNodo(btnModificar, administradora && modificacionService.motivoNoModificable(pedido) == null);
        ApoyoVista.mostrarNodo(btnCancelar, gestionar && pedidoService.motivoNoCancelable(pedido) == null);
        ApoyoVista.mostrarNodo(btnBaja, administradora && pedidoService.motivoNoBaja(pedido) == null);
        ApoyoVista.mostrarNodo(btnImprimir, true);
    }

    private void ponerAyuda(Button boton, String texto) {
        if (texto == null) {
            boton.setTooltip(null);
        } else {
            boton.setTooltip(new Tooltip(texto));
        }
    }

    // ===================== Acciones =====================

    // RF-38: aprobar
    @FXML
    // RF-38 (Aprobación de pedidos): inicia aquí. Paso 1: la persona pide «Aprobar».
    private void onAprobar() {
        if (pedido == null) {
            return;
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_APROBACION);
        if (cargador == null) {
            Alertas.error("No se pudo abrir la aprobación");
            return;
        }
        FormularioAprobacionController formulario = cargador.getController();
        int carga = 0;
        try {
            carga = pedidoService.cargaDelDia(pedido.getFechaEntrega(), pedido.getFolio());
        } catch (DAOException e) {
            Alertas.error(ApoyoTransaccion.MSG_ERROR_CONEXION);
            return;
        }
        formulario.preparar(pedido, carga);
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, ventana(), "Aprobar pedido #" + pedido.getFolio());
        atenderResultado(formulario.getResultado());
    }

    // RF-40, RF-41, RF-44: avanzar al siguiente estado
    @FXML
    // RF-41 (Modificación de estado del pedido): inicia aquí. Paso 1: la persona pide cambiar el estado.
    private void onAvanzar() {
        if (pedido == null) {
            return;
        }
        EstadoPedido siguiente = pedido.getEstado().siguiente();
        ResultadoPedido bloqueo = estadoService.revisarAvance(pedido);
        if (bloqueo != null) {
            atenderBloqueo(bloqueo);
            return;
        }
        String detalle = "Estado actual: " + pedido.getEstado() + "\nSiguiente estado: " + siguiente;
        if (siguiente == EstadoPedido.EN_PREPARACION) {
            detalle = detalle + "\n\nSe descontarán del inventario los insumos de las recetas del pedido.";
        }
        // RF-41 · Pasos 4 y 5: confirmación; si se cancela, el pedido conserva su estado.
        if (!Alertas.confirmar("¿Cambiar el estado del pedido #" + pedido.getFolio() + "?", detalle)) {
            return;
        }
        ResultadoPedido resultado = estadoService.avanzar(pedido, siguiente, false);
        // RF-63: hay opciones sin receta; se pregunta si se continúa sin descontarlas
        // RF-63 · Excepción paso 3: opciones sin receta; la administradora decide si continúa.
        if (!resultado.isExitoso() && resultado.isPreguntarSinReceta()) {
            if (!Alertas.confirmar("Opciones sin receta", resultado.getMensaje())) {
                return;
            }
            resultado = estadoService.avanzar(pedido, siguiente, true);
        }
        if (!resultado.isExitoso() && !resultado.isRecargar()) {
            atenderBloqueo(resultado);
            return;
        }
        atenderResultado(resultado);
    }

    // Muestra por qué no se puede avanzar y ofrece registrar el pago o abrir la revisión
    // RF-41 · Excepciones paso 3 / RF-44: se ofrece registrar el pago o abrir la aprobación.
    private void atenderBloqueo(ResultadoPedido bloqueo) {
        if (bloqueo.isOfrecerPago()) {
            if (Alertas.confirmar("No se puede avanzar", bloqueo.getMensaje() + "\n\n¿Registrar o validar un pago ahora?")) {
                onRegistrarPago();
            }
            return;
        }
        if (bloqueo.isOfrecerRevision() && PermisosPedidos.puedeGestionar()
                && pedido.getEstado() == EstadoPedido.EN_ACEPTACION && !btnAprobar.isDisabled()) {
            if (Alertas.confirmar("No se puede avanzar", bloqueo.getMensaje() + "\n\n¿Abrir la aprobación del pedido?")) {
                onAprobar();
            }
            return;
        }
        Alertas.aviso(bloqueo.getMensaje());
    }

    // RF-42: registrar un pago (o validar el pendiente si el cliente ya subió uno)
    @FXML
    // RF-42 (Validación de pagos): inicia aquí. Paso 1: la persona pide «Registrar pago». También RF-43 paso 6.
    private void onRegistrarPago() {
        if (pedido == null) {
            return;
        }
        ResultadoPedido revision = pagoService.revisarRegistro(pedido);
        if (revision != null) {
            atenderBloqueo(revision);
            return;
        }
        // Si hay un pago pendiente se ofrece revisarlo primero
        for (Pago pago : pedido.getPagos()) {
            if (pago.getEstado() == EstadoPago.PENDIENTE) {
                if (Alertas.confirmar("Hay un pago por validar", "El cliente subió un pago de "
                        + Formatos.moneda(pago.getMonto()) + ". ¿Quieres revisarlo ahora?")) {
                    abrirValidacion(pago);
                    return;
                }
                break;
            }
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_PAGO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el registro de pago");
            return;
        }
        FormularioPagoController formulario = cargador.getController();
        formulario.prepararRegistro(pedido);
        ApoyoVista.mostrarModal(cargador.getRoot(), ventana(), "Registrar pago · pedido #" + pedido.getFolio());
        atenderResultado(formulario.getResultado());
    }

    private void abrirValidacion(Pago pago) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_PAGO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir la validación del pago");
            return;
        }
        FormularioPagoController formulario = cargador.getController();
        formulario.prepararValidacion(pago, pedido);
        ApoyoVista.mostrarModal(cargador.getRoot(), ventana(), "Validar pago · pedido #" + pedido.getFolio());
        atenderResultado(formulario.getResultado());
    }

    // Modificar (solo la Administradora)
    @FXML
    // RF-34 (Modificación de pedidos): inicia aquí. Paso 1: la persona pide «Modificar».
    private void onModificar() {
        if (pedido == null) {
            return;
        }
        ResultadoPedido lectura = modificacionService.prepararModificacion(pedido.getFolio());
        // RF-34 · Excepción paso 2: no se puede modificar (no encontrado, cancelado o en producción).
        if (!lectura.isExitoso()) {
            if (lectura.esErrorConexion()) {
                Alertas.error(lectura.getMensaje());
            } else {
                Alertas.aviso(lectura.getMensaje());
                recargar(true);
            }
            return;
        }
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_MODIFICACION);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de modificación");
            return;
        }
        FormularioModificacionController formulario = cargador.getController();
        formulario.preparar(lectura.getPedido());
        ApoyoVista.mostrarModal(cargador.getRoot(), ventana(), "Modificar pedido #" + pedido.getFolio());
        atenderResultado(formulario.getResultado());
    }

    // RF-39: cancelar
    @FXML
    // RF-39 (Cancelación de pedidos): inicia aquí. Paso 1: la persona pide cancelar el pedido.
    private void onCancelar() {
        if (pedido == null) {
            return;
        }
        String impedimento = pedidoService.motivoNoCancelable(pedido);
        if (impedimento != null) {
            // RF-39 · Excepción paso 2: ya cancelado o ya en producción.
            Alertas.aviso(impedimento);
            return;
        }
        abrirMotivo(true);
    }

    // Dar de baja (solo la Administradora)
    @FXML
    // RF-35 (Eliminación de pedidos): inicia aquí. Paso 1: la persona pide «Dar de baja».
    private void onDarDeBaja() {
        if (pedido == null) {
            return;
        }
        String impedimento = pedidoService.motivoNoBaja(pedido);
        if (impedimento != null) {
            // RF-35 · Excepciones pasos 2 y 3: ya dado de baja o ya aprobado.
            Alertas.aviso(impedimento);
            return;
        }
        abrirMotivo(false);
    }

    private void abrirMotivo(boolean esCancelacion) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_MOTIVO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario");
            return;
        }
        FormularioMotivoController formulario = cargador.getController();
        String titulo;
        if (esCancelacion) {
            formulario.prepararCancelacion(pedido);
            titulo = "Cancelar pedido #" + pedido.getFolio();
        } else {
            formulario.prepararBaja(pedido);
            titulo = "Dar de baja la solicitud #" + pedido.getFolio();
        }
        ApoyoVista.mostrarModal(cargador.getRoot(), ventana(), titulo);
        atenderResultado(formulario.getResultado());
    }

    // Lo que regresa un formulario: mensaje y recarga
    // RF-38 / RF-39 / RF-35 (Eliminación de pedidos) / RF-41 · Paso final: informa el resultado y actualiza la vista.
    private void atenderResultado(ResultadoPedido resultado) {
        if (resultado == null) {
            return;
        }
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensajeCompleto());
            recargar(true);
            if (resultado.isOfrecerPago() && pedido != null) {
                if (Alertas.confirmar("Registrar pago", "¿Quieres registrar o validar el pago del anticipo ahora?")) {
                    onRegistrarPago();
                }
            }
            return;
        }
        if (resultado.isRecargar()) {
            Alertas.aviso(resultado.getMensaje());
            recargar(true);
            return;
        }
        Alertas.error(resultado.getMensaje());
    }

    // ===================== Ticket (RF-48) =====================

    // RF-48 (Impresión del ticket del pedido): inicia aquí. Paso 1: se eligió «Imprimir ticket» en la ficha del pedido.
    @FXML
    private void onImprimir() {
        if (pedido == null) {
            return;
        }
        // RF-48 · Paso 2: arma el ticket de cocina.
        Node ticket = TicketPedido.crear(pedido);
        // RF-48 · Paso 3: busca una impresora configurada en el equipo.
        PrinterJob trabajo = PrinterJob.createPrinterJob();
        // RF-48 · Excepción paso 3: «No hay una impresora configurada en este equipo».
        if (trabajo == null) {
            Alertas.error("No hay una impresora configurada en este equipo");
            return;
        }
        // RF-48 · Paso 4: el usuario elige la impresora y confirma (Excepción paso 4: si cancela no se imprime).
        if (!trabajo.showPrintDialog(ventana())) {
            return;
        }
        // RF-48 · Paso 5: envía el ticket a la impresora (Excepción paso 5: «No se pudo imprimir el ticket»).
        boolean impreso = trabajo.printPage(ticket);
        if (impreso) {
            trabajo.endJob();
        } else {
            Alertas.error("No se pudo imprimir el ticket");
        }
    }

    private Window ventana() {
        return cajaContenido.getScene().getWindow();
    }
}

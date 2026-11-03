package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.model.Movimiento;
import com.pac.modulos.inventario.service.InsumoService;
import com.pac.modulos.pedidos.controller.BarraEstados;
import com.pac.modulos.inventario.service.InventarioService;
import com.pac.modulos.inventario.service.PermisosInventario;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pestaña Insumos (insumos.fxml).
 * Visualización (RF-33), registro (RF-34), modificación (RF-35) y baja (RF-36) de insumos.
 */
public class InsumosController {

    private static final String RUTA_FORMULARIO = "/com/pac/view/inventario/formulario-insumo.fxml";
    private static final String TODAS_CATEGORIAS = "Todas las categorías";
    private static final int MOVIMIENTOS_DETALLE = 5;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private CheckBox chkInactivos;
    @FXML private BarraEstados barraStock;
    @FXML private Button btnNuevo;

    @FXML private TableView<FilaInsumo> tblInsumos;
    @FXML private Label lblConteo;
    @FXML private Label lblAviso;
    @FXML private Label lblVacio;

    @FXML private VBox cajaSinSeleccion;
    @FXML private ScrollPane cajaDetalle;
    @FXML private VBox cajaBotones;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetClave;
    @FXML private Label lblDetEstado;
    @FXML private Label lblDetExistencia;
    @FXML private Label lblDetValores;
    @FXML private VBox listaLotes;
    @FXML private VBox listaMovimientos;
    @FXML private Button btnEntrada;
    @FXML private Button btnMerma;
    @FXML private Button btnModificar;
    @FXML private Button btnDarDeBaja;
    @FXML private Button btnReactivar;

    private InsumoService servicio = new InsumoService();
    private InventarioService inventarioService = new InventarioService();
    private InventarioController inventario;
    private FilaInsumo seleccionada;
    private boolean puedeAdministrar;
    private boolean puedeMover;
    private boolean llenandoFiltros = false;

    // Claves de los botones de estado del stock
    private static final String FILTRO_BAJO = "bajo";
    private static final String FILTRO_CRITICO = "critico";
    private static final String FILTRO_PROXIMO = "proximo";
    private static final String FILTRO_BAJA = "baja";

    @FXML
    private void initialize() {
        // Cada renglón se pinta según el estado del insumo
        // (JavaFX necesita la flecha "->" para indicar cómo crear cada renglón)
        tblInsumos.setRowFactory(tabla -> new RenglonInsumo());

        // Botones de estado del stock: cada uno filtra la tabla
        barraStock.agregar(FILTRO_BAJO, "Stock bajo", "estado-preparacion", "bi-exclamation-triangle-fill");
        barraStock.agregar(FILTRO_CRITICO, "Crítico", "estado-cancelado", "bi-x-octagon-fill");
        barraStock.agregar(FILTRO_PROXIMO, "Por caducar", "estado-aceptacion", "bi-hourglass-split");
        barraStock.agregar(FILTRO_BAJA, "Dado de baja", "estado-baja", "bi-archive-fill");
        barraStock.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onBuscar();
            }
        });
    }

    public void iniciar(InventarioController inventario) {
        this.inventario = inventario;
        puedeAdministrar = PermisosInventario.puedeAdministrarInsumos();
        puedeMover = PermisosInventario.puedeRegistrarMovimientos();
        ApoyoVista.mostrarNodo(btnNuevo, puedeAdministrar);
        ApoyoVista.mostrarNodo(btnModificar, puedeAdministrar);
        ApoyoVista.mostrarNodo(btnDarDeBaja, puedeAdministrar);
        ApoyoVista.mostrarNodo(btnEntrada, puedeMover);
        ApoyoVista.mostrarNodo(btnMerma, puedeMover);
        ApoyoVista.mostrarNodo(btnReactivar, false);
    }

    // Se llama cada vez que la pestaña queda visible
    public void alMostrar() {
        llenarCategorias();
        cargarInsumos();
    }

    // Llena el filtro de categorías conservando la que estaba elegida
    private void llenarCategorias() {
        String antes = cmbCategoria.getValue();
        List<String> categorias;
        try {
            categorias = servicio.listarCategorias();
        } catch (DAOException e) {
            return;
        }
        llenandoFiltros = true;
        cmbCategoria.getItems().clear();
        cmbCategoria.getItems().add(TODAS_CATEGORIAS);
        cmbCategoria.getItems().addAll(categorias);
        if (antes != null && cmbCategoria.getItems().contains(antes)) {
            cmbCategoria.setValue(antes);
        } else {
            cmbCategoria.setValue(TODAS_CATEGORIAS);
        }
        llenandoFiltros = false;
    }

    @FXML
    private void onBuscar() {
        if (llenandoFiltros) {
            return;
        }
        cargarInsumos();
    }

    @FXML
    private void onLimpiarFiltro() {
        llenandoFiltros = true;
        txtBuscar.clear();
        cmbCategoria.setValue(TODAS_CATEGORIAS);
        chkInactivos.setSelected(false);
        barraStock.limpiar();
        llenandoFiltros = false;
        cargarInsumos();
    }

    @FXML
    private void onReintentar() {
        alMostrar();
    }

    // ===================== Lista (Visualización RF-33) =====================

    // RF-33 (Visualización de datos de insumos): inicia aquí en la pantalla. Paso 1: la persona entra a Insumos y puede buscar o filtrar.
    private void cargarInsumos() {
        int idAntes = 0;
        if (seleccionada != null) {
            idAntes = seleccionada.getInsumo().getIdInsumo();
        }
        tblInsumos.getItems().clear();
        mostrarDetalle(null);
        ApoyoVista.mostrarNodo(lblAviso, false);
        lblConteo.setText("");

        // Paso 3: se consultan los insumos (con su existencia calculada con los lotes)
        List<Insumo> insumos;
        try {
            insumos = servicio.listar(chkInactivos.isSelected() || FILTRO_BAJA.equals(barraStock.getClave()));
        } catch (DAOException e) {
            // RF-33 · Excepción paso 3: sin conexión, «Error de conexión» con «Reintentar».
            mostrarErrorConexion();
            return;
        }

        // Paso 4: no hay insumos
        if (insumos.isEmpty()) {
            // RF-33 · Excepción paso 4: «No hay insumos registrados» (con «+ Registrar insumo» para la administradora).
            lblVacio.setText(InsumoService.MSG_SIN_INSUMOS);
            lblConteo.setText("0 insumo(s)");
            return;
        }

        // Pasos 4 a 6: filtro, datos incompletos y estado de cada insumo
        String categoria = cmbCategoria.getValue();
        if (TODAS_CATEGORIAS.equals(categoria)) {
            categoria = null;
        }
        int incompletos = 0;
        int atencion = 0;
        for (Insumo insumo : insumos) {
            if (!servicio.coincide(insumo, txtBuscar.getText(), categoria)) {
                continue;
            }
            FilaInsumo fila = new FilaInsumo(insumo, servicio.estadoDe(insumo));
            if (!pasaFiltroStock(fila)) {
                continue;
            }
            tblInsumos.getItems().add(fila);
            if (insumo.isActivo() && !servicio.datosCompletos(insumo)) {
                incompletos++;
            }
            if (fila.necesitaAtencion()) {
                atencion++;
            }
        }

        String conteo = tblInsumos.getItems().size() + " insumo(s)";
        if (atencion > 0) {
            conteo = conteo + " · " + atencion + " requieren atención";
        }
        lblConteo.setText(conteo);
        if (incompletos > 0) {
            lblAviso.setText(incompletos + " insumo(s) tienen datos incompletos y se marcaron «Revisar datos»"
                    + " (falta categoría o costo).");
            ApoyoVista.mostrarNodo(lblAviso, true);
        }
        if (tblInsumos.getItems().isEmpty()) {
            // RF-33 · Excepción paso 4: «No se encontraron insumos con ese criterio» con «Limpiar filtro».
            lblVacio.setText(InsumoService.MSG_SIN_RESULTADOS);
        }

        // Se vuelve a seleccionar el insumo que estaba abierto
        for (FilaInsumo fila : tblInsumos.getItems()) {
            if (fila.getInsumo().getIdInsumo() == idAntes) {
                tblInsumos.getSelectionModel().select(fila);
                mostrarSeleccionada();
            }
        }
    }

    private boolean hayFiltro() {
        boolean conTexto = txtBuscar.getText() != null && !txtBuscar.getText().isBlank();
        boolean conCategoria = cmbCategoria.getValue() != null && !TODAS_CATEGORIAS.equals(cmbCategoria.getValue());
        return conTexto || conCategoria || chkInactivos.isSelected() || barraStock.getClave() != null;
    }

    // true si el insumo cumple el botón de estado del stock que está activo
    private boolean pasaFiltroStock(FilaInsumo fila) {
        String clave = barraStock.getClave();
        if (clave == null) {
            return true;
        }
        String clase = EstilosInventario.claseChipInsumo(fila.getEstado());
        if (FILTRO_BAJO.equals(clave)) {
            return "chip-bajo".equals(clase);
        }
        if (FILTRO_CRITICO.equals(clave)) {
            return "chip-critico".equals(clase);
        }
        if (FILTRO_PROXIMO.equals(clave)) {
            return "chip-proximo".equals(clase);
        }
        return InsumoService.ESTADO_INACTIVO.equals(fila.getEstado());
    }

    private void mostrarErrorConexion() {
        tblInsumos.getItems().clear();
        mostrarDetalle(null);
        lblVacio.setText(InsumoService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
    }

    // ===================== Detalle (RF-33 paso 7) =====================

    @FXML
    private void onClicTabla(MouseEvent evento) {
        boolean hay = mostrarSeleccionada();
        if (hay && evento.getClickCount() == 2 && puedeAdministrar && seleccionada.getInsumo().isActivo()) {
            onModificar();
        }
    }

    @FXML
    private void onTeclaTabla() {
        mostrarSeleccionada();
    }

    private boolean mostrarSeleccionada() {
        FilaInsumo fila = tblInsumos.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return false;
        }
        if (seleccionada != null && seleccionada.getInsumo().getIdInsumo() == fila.getInsumo().getIdInsumo()) {
            return true;
        }
        mostrarDetalle(fila);
        return true;
    }

    // RF-33 · Paso 7: muestra los lotes y últimos movimientos del insumo seleccionado.
    private void mostrarDetalle(FilaInsumo fila) {
        seleccionada = fila;
        ApoyoVista.mostrarNodo(cajaDetalle, fila != null);
        ApoyoVista.mostrarNodo(cajaBotones, fila != null);
        ApoyoVista.mostrarNodo(cajaSinSeleccion, fila == null);
        if (fila == null) {
            return;
        }
        Insumo insumo = fila.getInsumo();
        String unidad = insumo.getUnidad().getValorBD();

        // Paso 7: se valida que el insumo siga existiendo y se leen sus lotes y movimientos
        List<Lote> lotes;
        List<Movimiento> movimientos;
        try {
            Insumo actual = servicio.buscarPorId(insumo.getIdInsumo());
            if (actual == null) {
                // RF-33 · Excepción paso 7: «Insumo no encontrado» y se recarga la tabla.
                Alertas.aviso(InsumoService.MSG_NO_ENCONTRADO);
                seleccionada = null;
                cargarInsumos();
                return;
            }
            lotes = inventarioService.listarLotes(insumo.getIdInsumo(), true);
            movimientos = inventarioService.listarUltimosMovimientos(insumo.getIdInsumo(), MOVIMIENTOS_DETALLE);
        } catch (DAOException e) {
            Alertas.error(InsumoService.MSG_ERROR_CONEXION);
            return;
        }

        lblDetNombre.setText(insumo.getNombre());
        lblDetClave.setText(insumo.getClave() + " · " + insumo.getCategoria());
        lblDetEstado.setText(fila.getEstado());
        EstilosInventario.aplicarChip(lblDetEstado, fila.getClaseChip());
        lblDetExistencia.setText(Formatos.cantidad(insumo.getExistencia()) + " " + unidad);

        String stock = "Sin stock mínimo (no genera alertas)";
        if (insumo.tieneStockMinimo()) {
            stock = Formatos.cantidad(insumo.getStockMinimo()) + " " + unidad;
        }
        lblDetValores.setText("Unidad: " + insumo.getUnidad()
                + "\nCosto por unidad: " + Formatos.moneda(insumo.getCostoUnitario()) + " / " + unidad
                + "\nStock mínimo: " + stock
                + "\nPerecedero: " + ApoyoVista.siNo(insumo.isPerecedero())
                + "\nValor en inventario: " + Formatos.moneda(insumo.getExistencia() * insumo.getCostoUnitario()));

        listaLotes.getChildren().clear();
        if (lotes.isEmpty()) {
            agregarLinea(listaLotes, "Sin existencias", "texto-suave");
        }
        for (Lote lote : lotes) {
            FilaLote filaLote = new FilaLote(lote);
            agregarLinea(listaLotes, lote.getClave() + " · " + filaLote.getExistencia() + "\nCaduca: "
                    + filaLote.getCaducidad(), "linea-lote");
        }

        listaMovimientos.getChildren().clear();
        if (movimientos.isEmpty()) {
            agregarLinea(listaMovimientos, "Sin movimientos", "texto-suave");
        }
        for (Movimiento movimiento : movimientos) {
            FilaMovimiento filaMov = new FilaMovimiento(movimiento);
            agregarLinea(listaMovimientos, filaMov.getFecha() + " · " + filaMov.getTipo() + " " + filaMov.getCantidad(),
                    "linea-movimiento");
        }

        // Botones según el estado del insumo
        boolean activo = insumo.isActivo();
        ApoyoVista.mostrarNodo(btnEntrada, puedeMover && activo);
        ApoyoVista.mostrarNodo(btnMerma, puedeMover && activo);
        ApoyoVista.mostrarNodo(btnModificar, puedeAdministrar && activo);
        ApoyoVista.mostrarNodo(btnDarDeBaja, puedeAdministrar && activo);
        ApoyoVista.mostrarNodo(btnReactivar, puedeAdministrar && !activo);
    }

    private void agregarLinea(VBox lista, String texto, String claseEstilo) {
        Label linea = new Label(texto);
        linea.setWrapText(true);
        linea.setMaxWidth(Double.MAX_VALUE);
        linea.getStyleClass().add(claseEstilo);
        lista.getChildren().add(linea);
    }

    // ===================== Compras y mermas =====================

    @FXML
    private void onEntrada() {
        if (seleccionada == null) {
            return;
        }
        if (inventario.abrirEntrada(seleccionada.getInsumo())) {
            recargarConSeleccion();
        }
    }

    @FXML
    private void onMerma() {
        if (seleccionada == null) {
            return;
        }
        if (inventario.abrirMerma(seleccionada.getInsumo(), null, null)) {
            recargarConSeleccion();
        }
    }

    @FXML
    private void onVerMovimientos() {
        if (seleccionada != null) {
            inventario.irAMovimientos(seleccionada.getInsumo());
        }
    }

    // Recarga la tabla y vuelve a mostrar el detalle del insumo que estaba elegido
    private void recargarConSeleccion() {
        FilaInsumo antes = seleccionada;
        cargarInsumos();
        if (antes != null && seleccionada == null) {
            for (FilaInsumo fila : tblInsumos.getItems()) {
                if (fila.getInsumo().getIdInsumo() == antes.getInsumo().getIdInsumo()) {
                    tblInsumos.getSelectionModel().select(fila);
                    mostrarDetalle(fila);
                }
            }
        }
    }

    // ===================== Registro y modificación (RF-34, RF-35) =====================

    @FXML
    // RF-34 (Registro de datos de insumos): inicia aquí. Paso 1: la administradora pide registrar un insumo.
    private void onNuevo() {
        abrirFormulario(null);
    }

    @FXML
    // RF-35 (Modificación de datos de insumos): inicia aquí. Paso 1: se selecciona el insumo y se pide modificarlo.
    private void onModificar() {
        if (seleccionada == null) {
            Alertas.info("Selecciona un insumo de la lista");
            return;
        }
        // Paso 2: el insumo existe y está activo
        ResultadoInventario revision = servicio.revisarParaModificar(seleccionada.getInsumo().getIdInsumo());
        if (!revision.isExitoso()) {
            // RF-35 · Excepción paso 2: «Insumo no encontrado» o «Error de conexión»; no se abre el formulario.
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                seleccionada = null;
                cargarInsumos();
            }
            return;
        }
        Insumo actual;
        try {
            actual = servicio.buscarPorId(seleccionada.getInsumo().getIdInsumo());
        } catch (DAOException e) {
            Alertas.error(InsumoService.MSG_ERROR_CONEXION);
            return;
        }
        abrirFormulario(actual);
    }

    private void abrirFormulario(Insumo insumo) {
        FXMLLoader cargador = ApoyoVista.cargar(RUTA_FORMULARIO);
        if (cargador == null) {
            Alertas.error("No se pudo abrir el formulario de insumo");
            return;
        }
        FormularioInsumoController formulario = cargador.getController();
        String titulo = "Registrar insumo";
        if (insumo == null) {
            formulario.prepararNuevo();
        } else {
            formulario.prepararEdicion(insumo);
            titulo = "Modificar insumo";
        }
        Parent raiz = cargador.getRoot();
        ApoyoVista.mostrarModal(raiz, tblInsumos.getScene().getWindow(), titulo);

        if (formulario.seGuardo()) {
            // RF-34 / RF-35 · Paso final: informa el éxito y actualiza la tabla.
            Alertas.info(formulario.getMensajeExito());
        }
        if (formulario.seGuardo() || formulario.debeRecargarLista()) {
            llenarCategorias();
            cargarInsumos();
            seleccionarPorId(formulario.getIdGuardado());
        }
    }

    // Deja seleccionado el insumo con ese id (por ejemplo, el que se acaba de registrar)
    private void seleccionarPorId(int idInsumo) {
        if (idInsumo <= 0) {
            return;
        }
        for (FilaInsumo fila : tblInsumos.getItems()) {
            if (fila.getInsumo().getIdInsumo() == idInsumo) {
                tblInsumos.getSelectionModel().select(fila);
                tblInsumos.scrollTo(fila);
                mostrarDetalle(fila);
            }
        }
    }

    // ===================== Baja (RF-36) =====================

    @FXML
    // RF-36 (Eliminación de insumo del inventario): inicia aquí. Paso 1: se selecciona el insumo y se pide darlo de baja.
    private void onDarDeBaja() {
        if (seleccionada == null) {
            Alertas.info("Selecciona un insumo de la lista");
            return;
        }
        int idInsumo = seleccionada.getInsumo().getIdInsumo();
        ResultadoInventario revision = servicio.prepararBaja(idInsumo);
        if (!revision.isExitoso()) {
            // RF-36 · Excepción paso 2: insumo no encontrado o ya dado de baja.
            Alertas.aviso(revision.getMensaje());
            if (revision.isRecargarLista()) {
                seleccionada = null;
                cargarInsumos();
            }
            return;
        }
        // RF-36 · Pasos 4 y 5: confirmación; si la administradora cancela, el insumo queda sin cambios.
        if (!Alertas.confirmarDoble("¿Dar de baja este insumo?", revision.getMensaje())) {
            return;
        }
        ResultadoInventario resultado = servicio.darDeBaja(idInsumo);
        if (resultado.isExitoso()) {
            // RF-36 · Paso 7: informa el éxito y actualiza la tabla.
            Alertas.info(resultado.getMensaje());
        } else {
            // RF-36 · Excepción paso 6: «No fue posible dar de baja el insumo».
            Alertas.error(resultado.getMensaje());
        }
        seleccionada = null;
        cargarInsumos();
    }

    @FXML
    // RF-34 · Excepción paso 5: reactivar el insumo dado de baja en lugar de crear uno nuevo.
    private void onReactivar() {
        if (seleccionada == null) {
            return;
        }
        if (!Alertas.confirmar("¿Reactivar este insumo?", "«" + seleccionada.getNombre()
                + "» volverá a aparecer en compras, mermas y recetas.")) {
            return;
        }
        ResultadoInventario resultado = servicio.reactivar(seleccionada.getInsumo().getIdInsumo());
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        int id = seleccionada.getInsumo().getIdInsumo();
        seleccionada = null;
        cargarInsumos();
        seleccionarPorId(id);
    }
}
